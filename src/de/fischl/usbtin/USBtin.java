/*
 * This file is part of USBtinLib.
 *
 * Copyright (C) 2014  Thomas Fischl 
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
    
    /** USBtin firmware version */
    protected String firmwareVersion;
    
    /** USBtin hardware version */
    protected String hardwareVersion;

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
            char baudCh;
            switch (baudrate) {
                default:
                    System.err.println("Baudrate " + baudrate + " not supported. Using 10k.");
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
            this.transmit("S" + baudCh);

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
            throw new USBtinException("Timeout! USBtin doesn't answer. Right port?");
        } catch (SerialPortTimeoutException e) {
            throw new USBtinException(e);
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
                        }
                        
                        incomingMessage.setLength(0);
                    } else {
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
     * Send given can message.
     * 
     * @param canmsg Can message to send
     * @throws USBtinException  On serial port errors
     */
    public void send(CANMessage canmsg) throws USBtinException {
        try {
            serialPort.writeBytes((canmsg.toString() + "\r").getBytes());            
        } catch (SerialPortException e) {
            throw new USBtinException(e);
        }
    }
}
