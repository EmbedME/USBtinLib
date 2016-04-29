/*
 * This file is part of USBtinLib.
 *
 * Copyright (C) 2014-2015  Thomas Fischl 
 *
 * USBtinLib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * USBtinLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with USBtinLib.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.fischl.usbtin;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;

/**
 * Represents an USBtin device.
 * Provide access to an USBtin device (http://www.fischl.de/usbtin) over virtual
 * serial port (CDC).
 *
 * @author Thomas Fischl <tfischl@gmx.de>
 */
public class USBtin implements SerialPortEventListener {

    /** Serial port (virtual) to which USBtin is connected */
    protected SerialPort serialPort;
    
    /** Characters coming from USBtin are collected in this StringBuilder */
    protected StringBuilder incomingMessage = new StringBuilder();
    
    /** Listener for CAN messages */
    protected ArrayList<CANMessageListener> listeners = new ArrayList<CANMessageListener>();
    
    /** Transmit fifo */
    protected LinkedList<CANMessage> fifoTX = new LinkedList<CANMessage>();
    
    /** USBtin firmware version */
    protected String firmwareVersion;
    
    /** USBtin hardware version */
    protected String hardwareVersion;

    /** USBtin serial number */
    protected String serialNumber;

    /** Timeout for response from USBtin */
    protected static final int TIMEOUT = 1000;

    /** Modes for opening a CAN channel */
    public enum OpenMode {
        /** Send and receive on CAN bus */
        ACTIVE,
        /** Listen only, sending messages is not possible */
        LISTENONLY,
        /** Loop back the sent CAN messages. Disconnected from physical CAN bus */
        LOOPBACK
    }

    /**
     * Get firmware version string.
     * During connect() the firmware version is requested from USBtin.
     * 
     * @return Firmware version
     */
    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    /**
     * Get hardware version string.
     * During connect() the hardware version is requested from USBtin.
     * 
     * @return Hardware version
     */
    public String getHardwareVersion() {
        return hardwareVersion;
    }

    /**
     * Get serial number string.
     * During connect() the serial number is requested from USBtin.
     *
     * @return Serial number
     */
    public String getSerialNumber() {
        return serialNumber;
    }

    /**
     * Connect to USBtin on given port.
     * Opens the serial port, clears pending characters and send close command
     * to make sure that we are in configuration mode.
     * 
     * @param portName Name of virtual serial port
     * @throws USBtinException Error while connecting to USBtin
     */
    public void connect(String portName) throws USBtinException {
        
        try {
            
            // create serial port object
            serialPort = new SerialPort(portName);

            // open serial port and initialize it
            serialPort.openPort();
            serialPort.setParams(115200, 8, 1, 0);

            // clear port and make sure we are in configuration mode (close cmd)
            serialPort.writeBytes("\rC\r".getBytes());
            Thread.sleep(100);
            serialPort.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
            serialPort.writeBytes("C\r".getBytes());
            byte b;
            do {
                byte[] buffer = serialPort.readBytes(1, TIMEOUT);
                b = buffer[0];            
            } while ((b != '\r') && (b != 7));
            
            // get version strings
            this.firmwareVersion = this.transmit("v").substring(1);
            this.hardwareVersion = this.transmit("V").substring(1);
            this.serialNumber = this.transmit("N").substring(1);

            // reset overflow error flags
            this.transmit("W2D00");

        } catch (SerialPortException e) {
            throw new USBtinException(e.getPortName() + " - " + e.getExceptionType());
        } catch (SerialPortTimeoutException e) {
            throw new USBtinException("Timeout! USBtin doesn't answer. Right port?");
        } catch (InterruptedException e) {
            throw new USBtinException(e);
        }                                
    }
    
    /**
     * Disconnect.
     * Close serial port connection
     * 
     * @throws USBtinException Error while closing connection
     */
    public void disconnect() throws USBtinException {
        
        try {
            serialPort.closePort();
        } catch (SerialPortException e) {
            throw new USBtinException(e.getExceptionType());
        }    
    }
    
    /**
     * Open CAN channel.
     * Set given baudrate and open the CAN channel in given mode.
     * 
     * @param baudrate Baudrate in bits/second
     * @param mode CAN bus accessing mode
     * @throws USBtinException Error while opening CAN channel
     */
    public void openCANChannel(int baudrate, OpenMode mode) throws USBtinException {

        try {
            
            // set baudrate
            char baudCh = ' ';
            switch (baudrate) {
                case 10000: baudCh = '0'; break;
                case 20000: baudCh = '1'; break;
                case 50000: baudCh = '2'; break;
                case 100000: baudCh = '3'; break;
                case 125000: baudCh = '4'; break;
                case 250000: baudCh = '5'; break;
                case 500000: baudCh = '6'; break;
                case 800000: baudCh = '7'; break;
                case 1000000: baudCh = '8'; break;
            }
            
            if (baudCh != ' ') {
                
                // use preset baudrate
                
                this.transmit("S" + baudCh);
                
            } else {
                
                // calculate baudrate register settings

                final int FOSC = 24000000;
                int xdesired = FOSC / baudrate;
                int xopt = 0;
                int diffopt = 0;
                int brpopt = 0;

                // walk through possible can bit length (in TQ)
                for (int x = 11; x <= 23; x++) {

                    // get next even value for baudrate factor
                    int xbrp = (xdesired * 10) / x;
                    int m = xbrp % 20;
                    if (m >= 10) xbrp += 20;
                    xbrp -= m;
                    xbrp /= 10;

                    // check bounds
                    if (xbrp < 2) xbrp = 2;
                    if (xbrp > 128) xbrp = 128;

                    // calculate diff
                    int xist = x * xbrp;
                    int diff = xdesired - xist;
                    if (diff < 0) diff = -diff;

                    // use this clock option if it is better than previous
                    if ((xopt == 0) || (diff <= diffopt)) { xopt = x; diffopt = diff; brpopt = xbrp / 2 - 1;};
                }

                // mapping for CNF register values
                int cnfvalues[] = {0x9203, 0x9303, 0x9B03, 0x9B04, 0x9C04, 0xA404, 0xA405, 0xAC05, 0xAC06, 0xAD06, 0xB506, 0xB507, 0xBD07};

                this.transmit("s" + String.format("%02x", brpopt | 0xC0) + String.format("%04x", cnfvalues[xopt - 11]));
                
                System.out.println("No preset for given baudrate " + baudrate + ". Set baudrate to " + (FOSC / ((brpopt + 1) * 2) / xopt));
                
            }

            // open can channel
            char modeCh;
            switch (mode) {
                default:
                    System.err.println("Mode " + mode + " not supported. Opening listen only.");
                case LISTENONLY: modeCh = 'L'; break;
                case LOOPBACK: modeCh = 'l'; break;
                case ACTIVE: modeCh = 'O'; break;
            }
            this.transmit(modeCh + "");

            // register serial port event listener
            serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
            serialPort.addEventListener(this);
            
        } catch (SerialPortException e) {
            throw new USBtinException(e);
        } catch (SerialPortTimeoutException e) {
            throw new USBtinException("Timeout! USBtin doesn't answer. Right port?");            
        }
    }

    /**
     * Close CAN channel.
     * 
     * @throws USBtinException Error while closing CAN channel
     */
    public void closeCANChannel() throws USBtinException {
        try {
            serialPort.removeEventListener();
            serialPort.writeBytes("C\r".getBytes());
        } catch (SerialPortException e) {
            throw new USBtinException(e.getExceptionType());
        }
        
        firmwareVersion = null;
        hardwareVersion = null;
    }

    /**
     * Read response from USBtin
     *
     * @return Response from USBtin
     * @throws SerialPortException Error while accessing USBtin
     * @throws SerialPortTimeoutException Timeout of serial port
     */
    protected String readResponse() throws SerialPortException, SerialPortTimeoutException {
        StringBuilder response = new StringBuilder();
        while (true) {
            byte[] buffer = serialPort.readBytes(1, 1000);
            if (buffer[0] == '\r') {
                return response.toString();
            } else if (buffer[0] == 7) {
                throw new SerialPortException(serialPort.getPortName(), "transmit", "BELL signal");
            } else {
                response.append((char) buffer[0]);
            }
        }
    }

    /**
     * Transmit given command to USBtin
     *
     * @param cmd Command
     * @return Response from USBtin
     * @throws SerialPortException Error while talking to USBtin
     * @throws SerialPortTimeoutException Timeout of serial port
     */
    public String transmit(String cmd) throws SerialPortException, SerialPortTimeoutException {

        String cmdline = cmd + "\r";
        serialPort.writeBytes(cmdline.getBytes());

        return this.readResponse();
    }


    /**
     * Handle serial port event.
     * Read single byte and check for end of line character.
     * If end of line is reached, parse command and dispatch it.
     * 
     * @param event Serial port event
     */
    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR() && event.getEventValue() > 0) {
            try {
                byte buffer[] = serialPort.readBytes();
                for (byte b : buffer) {
                    if ((b == '\r') && incomingMessage.length() > 0) {

                        String message = incomingMessage.toString();
                        char cmd = message.charAt(0);

                        // check if this is a CAN message
                        if (cmd == 't' || cmd == 'T' || cmd == 'r' || cmd == 'R') {
                            
                            // create CAN message from message string
                            CANMessage canmsg = new CANMessage(message);
                        
                            // give the CAN message to the listeners
                            for (CANMessageListener listener : listeners) {
                                listener.receiveCANMessage(canmsg);
                            }
                            
                        } else if ((cmd == 'z') || (cmd == 'Z')) {
                            
                            // remove first message from transmit fifo and send next one
                            
                            fifoTX.removeFirst();
                         
                            try {
                                sendFirstTXFifoMessage();
                            } catch (USBtinException ex) {
                                System.err.println(ex);
                            }
                            
                            
                        }
                        
                        incomingMessage.setLength(0);
                        
                    } else if (b == 0x07) {
                        
                        // resend first element from tx fifo
                        try {
                            sendFirstTXFifoMessage();
                        } catch (USBtinException ex) {
                            System.err.println(ex);
                        }
                        
                    } else if (b != '\r') {
                        
                        incomingMessage.append((char) b);
                    }
                }
            } catch (SerialPortException ex) {
                System.err.println(ex);
            }
        }
    }
    
    /**
     * Add message listener
     * 
     * @param listener Listener object
     */
    public void addMessageListener(CANMessageListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove message listener.
     * 
     * @param listener Listener object
     */
    public void removeMessageListener(CANMessageListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Send first message in tx fifo
     * 
     * @throws USBtinException On serial port errors
     */
    protected void sendFirstTXFifoMessage() throws USBtinException {
        
        if (fifoTX.size() == 0) {
            return;
        }

        CANMessage canmsg = fifoTX.getFirst();

        try {
            serialPort.writeBytes((canmsg.toString() + "\r").getBytes());
        } catch (SerialPortException e) {
            throw new USBtinException(e);
        }
    }
    
    /**
     * Send given can message.
     * 
     * @param canmsg Can message to send
     * @throws USBtinException  On serial port errors
     */
    public void send(CANMessage canmsg) throws USBtinException {
        
        fifoTX.add(canmsg);
        
        if (fifoTX.size() > 1) return;
        
        sendFirstTXFifoMessage();
    }
    
    /**
     * Write given register of MCP2515
     * 
     * @param register Register address
     * @param value Value to write
     * @throws USBtinException On serial port errors
     */
    public void writeMCPRegister(int register, byte value) throws USBtinException {
        
        try {
            
            String cmd = "W" + String.format("%02x", register) + String.format("%02x", value);
            transmit(cmd);
            
        } catch (SerialPortException e) {
            throw new USBtinException(e);
        } catch (SerialPortTimeoutException e) {
            throw new USBtinException("Timeout! USBtin doesn't answer. Right port?");            
        }               
    }
    
    /**
     * Write given mask registers to MCP2515
     * 
     * @param maskid Mask identifier (0 = RXM0, 1 = RXM1)
     * @param registers Register values to write
     * @throws USBtinException On serial port errors
     */
    protected void writeMCPFilterMaskRegisters(int maskid, byte[] registers) throws USBtinException {
        
        for (int i = 0; i < 4; i++) {
            writeMCPRegister(0x20 + maskid * 4 + i, registers[i]);
        }                
    }
    
    /**
     * Write given filter registers to MCP2515
     * 
     * @param filterid Filter identifier (0 = RXF0, ... 5 = RXF5)
     * @param registers Register values to write
     * @throws USBtinException On serial port errors
     */
    protected void writeMCPFilterRegisters(int filterid, byte[] registers) throws USBtinException {

        int startregister[] = {0x00, 0x04, 0x08, 0x10, 0x14, 0x18};
        
        for (int i = 0; i < 4; i++) {
            writeMCPRegister(startregister[filterid] + i, registers[i]);
        }        
    }
    
    /**
     * Set hardware filters.
     * Call this function after connect() and before openCANChannel()!
     * 
     * @param fc Filter chains (USBtin supports maximum 2 hardware filter chains)
     * @throws USBtinException On serial port errors
     */
    public void setFilter(FilterChain[] fc) throws USBtinException {

        /*
         * The MCP2515 offers two filter chains. Each chain consists of one mask
         * and a set of filters:
         * 
         * RXM0         RXM1
         *   |            |
         * RXF0         RXF2
         * RXF1         RXF3
         *              RXF4
         *              RXF5
         */
        
        // if no filter chain given, accept all messages
        if ((fc == null) || (fc.length == 0)) {

            byte[] registers = {0, 0, 0, 0};
            writeMCPFilterMaskRegisters(0, registers);
            writeMCPFilterMaskRegisters(1, registers);

            return;
        }
        
        // check maximum filter channels
        if (fc.length > 2) {
            throw new USBtinException("Too many filter chains: " + fc.length + " (maximum is 2)!");
        }
        
        // swap channels if necessary and check filter chain length
        if (fc.length == 2) {
            
            if (fc[0].getFilters().length > fc[1].getFilters().length) {
                FilterChain temp = fc[0];
                fc[0] = fc[1];
                fc[1] = temp;
            }
            
            if ((fc[0].getFilters().length > 2) || (fc[1].getFilters().length > 4)) {
                throw new USBtinException("Filter chain too long: " + fc[0].getFilters().length + "/" + fc[1].getFilters().length + " (maximum is 2/4)!");
            }
            
        } else if (fc.length == 1) {
            
            if ((fc[0].getFilters().length > 4)) {
                throw new USBtinException("Filter chain too long: " + fc[0].getFilters().length + " (maximum is 4)!");
            }
        }
        
        // set MCP2515 filter/mask registers; walk through filter channels
        int filterid = 0;
        int fcidx = 0;
        for (int channel = 0; channel < 2; channel++) {
                                    
            // set mask
            writeMCPFilterMaskRegisters(channel, fc[fcidx].getMask().getRegisters());
           
            // set filters
            byte[] registers = {0, 0, 0, 0};
            for (int i = 0; i < (channel == 0 ? 2 : 4); i++) {

                if (fc[fcidx].getFilters().length > i) {
                    registers = fc[fcidx].getFilters()[i].getRegisters();
                }

                writeMCPFilterRegisters(filterid, registers);
                filterid++;
            }
            
            // go to next filter chain if available
            if (fc.length - 1 > fcidx) {
                fcidx++;
            }
        }
    }
}
