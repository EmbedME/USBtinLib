/*
 * This file is part of USBtinLib.
 *
 * Copyright (C) 2015  Thomas Fischl 
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
 * Represents the CAN filter registers.
 *
 * @author Thomas Fischl <tfischl@gmx.de>
 */
public class FilterValue extends FilterMask {
    
    /**
     * Create filter for extended CAN messages.
     * 
     * @param extid Filter for extended identifier
     */
    public FilterValue(int extid) {
        super(extid);
        registers[1] |= 0x08;
    }
    
    /**
     * Create filter for standard CAN message.
     * @param sid Filter for standard identifier
     * @param d0 Filter for first data byte
     * @param d1 Filter for second data byte
     */
    public FilterValue(int sid, byte d0, byte d1) {
        super(sid, d0, d1);        
    }
    
}
