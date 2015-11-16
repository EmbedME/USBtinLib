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
 * Represents a CAN filter chain.
 * 
 * @author Thomas Fischl <tfischl@gmx.de>
 */
public class FilterChain {
    
    /** Filter mask */
    FilterMask mask;
    
    /** Filters */
    FilterValue[] filters;
    
    /**
     * Create filter chain with one mask and filters.
     * 
     * @param mask Mask
     * @param filters Filters
     */
    public FilterChain(FilterMask mask, FilterValue[] filters) {
        this.mask = mask;
        this.filters = filters;
    }
    
    /**
     * Get mask of this filter chain.
     * 
     * @return Mask
     */
    public FilterMask getMask() {
        return mask;
    }
    
    /**
     * Get filters of this filter chain.
     * 
     * @return Filters
     */
    public FilterValue[] getFilters() {
        return filters;
    }
    
}
