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
 * Exception regarding USBtin
 *
 * @author Thomas Fischl <tfischl@gmx.de>
 */
public class USBtinException extends Exception {

    /**
     * Standard constructor
     */
    public USBtinException() {
        super();
    }

    /**
     * Construct exception
     * 
     * @param message Message string
     */
    public USBtinException(String message) {
        super(message);
    }

    /**
     * Construct exception
     * 
     * @param message Message string
     * @param cause Cause of exception
     */
    public USBtinException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct exception
     * 
     * @param cause Cause of exception
     */
    public USBtinException(Throwable cause) {
        super(cause);
    }
}
