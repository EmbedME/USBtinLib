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

/**
 * Represents a CAN message.
 *
 * @author Thomas Fischl <tfischl@gmx.de>
 */
public class CANMessage {

    /** CAN message ID */
    protected int id;
    
    /** CAN message payload data */
    protected byte[] data;
    
    /** Marks frames with extended message id */
    protected boolean extended;
    
    /** Marks request for transmition frames */
    protected boolean rtr;

    /**
     * Get CAN message identifier
     * 
     * @return CAN message identifier
     */
    public int getId() {
        return id;
    }

    /**
     * Set CAN message identifier
     * 
     * @param id CAN message identifier
     */
    public void setId(int id) {
        
        if (id > (0x1fffffff))
            id = 0x1fffffff;
        
        if (id > 0x7ff)
            extended = true;
        
        this.id = id;
    }

    /**
     * Get CAN message payload data
     * 
     * @return CAN message payload data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Set CAN message payload data
     * 
     * @param data 
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Determine if CAN message id is extended
     * 
     * @return true if extended CAN message
     */
    public boolean isExtended() {
        return extended;
    }

    /**
     * Determine if CAN message is a request for transmission
     * 
     * @return true if RTR message
     */
    public boolean isRtr() {
        return rtr;
    }    
    
    /**
     * Create message with given id and data.
     * Depending on Id, the extended flag is set.
     * 
     * @param id Message identifier
     * @param data Payload data
     */
    public CANMessage(int id, byte[] data) {        
        this.data = data;
        this.extended = false;
        setId(id);
        this.rtr = false;
    }
    
    /**
     * Create message with given message properties.
     * 
     * @param id Message identifier
     * @param data Payload data
     * @param extended Marks messages with extended identifier
     * @param rtr Marks RTR messages
     */
    public CANMessage(int id, byte[] data, boolean extended, boolean rtr) {
        setId(id);
        this.data = data;
        this.extended = extended;
        this.rtr = rtr;
    }    
    
    /**
     * Create message with given message string.
     * The message string is parsed. On errors, the corresponding value is
     * set to zero. 
     * 
     * Example message strings:
     * t1230        id: 123h        dlc: 0      data: --
     * t00121122    id: 001h        dlc: 2      data: 11 22
     * T12345678197 id: 12345678h   dlc: 1      data: 97
     * r0037        id: 003h        dlc: 7      RTR
     * 
     * @param msg Message string
     */
    public CANMessage(String msg) {
        
        this.rtr = false;
        int index = 1;
        char type;
        if (msg.length() > 0) type = msg.charAt(0);
        else type = 't';
        
        switch (type) {
            case 'r':
                this.rtr = true;
            default:
            case 't':
                try {
                   this.id = Integer.parseInt(msg.substring(index, index + 3), 16);
                } catch (java.lang.StringIndexOutOfBoundsException e) {
                    this.id = 0;
                } catch (java.lang.NumberFormatException e) {
                    this.id = 0;
                }
                this.extended = false;
                index += 3;
                break;
            case 'R':
                this.rtr = true;                
            case 'T':
                try {
                    this.id = Integer.parseInt(msg.substring(index, index + 8), 16);
                } catch (java.lang.StringIndexOutOfBoundsException e) {
                    this.id = 0;
                } catch (java.lang.NumberFormatException e) {
                    this.id = 0;
                }
                this.extended = true;
                index += 8;
                break;
        }
        
        int length;
        try {
            length = Integer.parseInt(msg.substring(index, index + 1), 16);
            if (length > 8) length = 8;
        } catch (java.lang.StringIndexOutOfBoundsException e) {
            length = 0;
        } catch (java.lang.NumberFormatException e) {
            length = 0;
        }
        index += 1;        
        
        this.data = new byte[length];        
        if (!this.rtr) {
            for (int i = 0; i < length; i++) {
                try {
                    this.data[i] = (byte) Integer.parseInt(msg.substring(index, index + 2), 16);
                } catch (java.lang.StringIndexOutOfBoundsException e) {
                    this.data[i] = 0;
                } catch (java.lang.NumberFormatException e) {
                    this.data[i] = 0;
                }
                index += 2;
            }
        }
    }
        
    /**
     * Get string representation of CAN message
     * 
     * @return CAN message as string representation
     */
    @Override
    public String toString(){
        String s;
        if (this.extended) {
            if (this.rtr) s = "R";
            else s = "T";
            s = s + String.format("%08x", this.id);
        }
        else {
            if (this.rtr) s = "r";
            else s = "t";
            s = s + String.format("%03x", this.id);
        }
        s = s + String.format("%01x", this.data.length);
        
        if (!this.rtr) {
            for (int i = 0; i < this.data.length; i++) {
                s = s + String.format("%02x", this.data[i]);
            }
        }
        return s;
    } 
}