/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.nioneo.store;

import java.util.LinkedList;
import java.util.List;

public class PropertyBlock
{
    private final List<DynamicRecord> valueRecords = new LinkedList<DynamicRecord>();
    private long[] valueBlocks;
    private boolean inUse;
    private boolean isCreated;

    public PropertyType getType()
    {
        return PropertyType.getPropertyType( valueBlocks[0], false );
    }

    public int getKeyIndexId()
    {
        // [kkkk,kkkk][kkkk,kkkk][kkkk,kkk ],[][][][][]
        return (int) ( valueBlocks[0] >> 40 );
    }

    public void setSingleBlock( long value )
    {
        valueBlocks = new long[1];
        valueBlocks[0] = value;
    }

    public void addValueRecord( DynamicRecord record )
    {
        valueRecords.add( record );
    }

    public List<DynamicRecord> getValueRecords()
    {
        return valueRecords;
    }

    public long getSingleValueBlock()
    {
        return valueBlocks[0];
    }

    public long[] getValueBlocks()
    {
        return valueBlocks;
    }

    public boolean isLight()
    {
        return valueRecords.size() == 0;
    }

    public PropertyData newPropertyData( PropertyRecord parent )
    {
        return newPropertyData( parent, null );
    }

    public PropertyData newPropertyData( PropertyRecord parent,
            Object extractedValue )
    {
        return getType().newPropertyData( this, parent.getId(), extractedValue );
    }

    public void setValueBlocks( long[] blocks )
    {
        this.valueBlocks = blocks;
    }

    public boolean inUse()
    {
        return inUse;
    }

    public void setInUse( boolean inUse )
    {
        this.inUse = inUse;
    }

    public boolean isCreated()
    {
        return isCreated;
    }

    public void setCreated()
    {
        isCreated = true;
    }

    private boolean changed;

    public boolean isChanged()
    {
        return changed;
    }

    public void setChanged()
    {
        changed = true;
    }

    /**
     * A property block can take a variable size of bytes in a property record.
     * This method returns the size of this block in bytes, including the header
     * size.
     *
     * @return The size of this block in bytes, including the header.
     */
    public int getSize()
    {
        // Currently each block is a multiple of 8 in size
        return valueBlocks.length * 8;
    }

    @Override
    public String toString()
    {
        StringBuffer result = new StringBuffer("PropertyBlock[");
        result.append( getKeyIndexId() ).append( ", " ).append( getType() );
        result.append( ", " ).append( valueBlocks ).append( "]" );

        return result.toString();
    }
}