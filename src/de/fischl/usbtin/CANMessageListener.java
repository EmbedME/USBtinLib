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
 * Listener for CAN messages.
 *
 * @author Thomas Fischl <tfischl@gmx.de>
 */
public interface CANMessageListener {
    
    /**
     * This method is called on incoming CAN messages
     * 
     * @param canmsg Received CAN message
     */
    public void receiveCANMessage(CANMessage canmsg);
}