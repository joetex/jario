/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.memory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import jario.hardware.Bus8bit;
import jario.hardware.Configurable;

public class Bus implements Bus8bit, java.io.Serializable
{
	public transient UnmappedMemory memory_unmapped;
	
	//adding helper method for serialization to save/initialize super class state
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException{
    	
    	

    	memory_unmapped = (UnmappedMemory)ois.readObject();
    	
    	//page = new Page[65536];
    	//for (int i = 0; i < page.length; i++)
		//{
		//	page[i] = new Page();
		//}
    	
    	//int cnt = ois.readInt();
    	/*page = new Page[65536];
    	for(int i=0; i<65536; i++)
    	{
    		page[i] = (Page)ois.readObject();
    	}*/
    	page = (Page[])ois.readObject();
    	
    	ois.defaultReadObject();
    }
     
    private void writeObject(ObjectOutputStream oos) throws IOException{
        
    	oos.writeObject(memory_unmapped);
    	
    	oos.writeObject(page);
    	//oos.writeInt(page.length);
    	/*for(int i=0; i<page.length; i++)
    	{
    		oos.writeObject(page[i]);
    	}*/
    	oos.defaultWriteObject();
    	
    }
    
	public enum MapMode
	{
		Direct, Linear, Shadow
	}

	public transient Page[] page = new Page[65536];

	public Bus()
	{
		for (int i = 0; i < page.length; i++)
		{
			page[i] = new Page();
		}
		
		memory_unmapped = new UnmappedMemory();
	}

	@Override
	public byte read8bit(int addr)
	{
		addr &= 0x00FFFFFF;
		Page p = page[addr >> 8];
		return p.access.read8bit(p.offset + addr);
	}

	@Override
	public void write8bit(int addr, byte data)
	{
		addr &= 0x00FFFFFF;
		Page p = page[addr >> 8];
		p.access.write8bit(p.offset + addr, data);
	}

	protected int mirror(int addr, int size)
	{
		int base_ = 0;
		if (size != 0)
		{
			int mask = 1 << 23;
			while (addr >= size)
			{
				while ((addr & mask) == 0)
				{
					mask >>= 1;
				}
				addr -= mask;
				if (size > mask)
				{
					size -= mask;
					base_ += mask;
				}
				mask >>= 1;
			}
			base_ += addr;
		}
		return base_;
	}

	protected void map(int addr, Bus8bit access, int offset)
	{
		Page p = page[addr >> 8];
		p.access = access;
		p.offset = offset - addr;
	}

	protected void map(MapMode mode, int bank_lo, int bank_hi, int addr_lo, int addr_hi, Bus8bit access)
	{
		map(mode, bank_lo, bank_hi, addr_lo, addr_hi, access, 0, 0);
	}

	public void map(MapMode mode, int bank_lo, int bank_hi, int addr_lo, int addr_hi, Bus8bit access, int offset, int size)
	{
		assert bank_lo <= bank_hi;
		assert addr_lo <= addr_hi;
		
		if (access instanceof Configurable
				&& ((Configurable)access).readConfig("size") != null
				&& (Integer)((Configurable)access).readConfig("size")  == -1) { Thread.dumpStack(); return; }

		int page_lo = (addr_lo >> 8) & 0xFF;
		int page_hi = (addr_hi >> 8) & 0xFF;
		int index = 0;

		switch (mode)
		{
		case Direct:
		{
			for (int bank = bank_lo; bank <= bank_hi; bank++)
			{
				for (int page = page_lo; page <= page_hi; page++)
				{
					map((bank << 16) + (page << 8), access, (bank << 16) + (page << 8));
				}
			}
		}
			break;
		case Linear:
		{
			for (int bank = bank_lo; bank <= bank_hi; bank++)
			{
				for (int page = page_lo; page <= page_hi; page++)
				{
					map((bank << 16) + (page << 8), access, mirror(offset + index, (Integer)((Configurable)access).readConfig("size")));
					index += 256;
					if (size != 0)
					{
						index %= size;
					}
				}
			}
		}
			break;
		case Shadow:
		{
			for (int bank = bank_lo; bank <= bank_hi; bank++)
			{
				index += (page_lo * 256);
				if (size != 0)
				{
					index %= size;
				}

				for (int page = page_lo; page <= page_hi; page++)
				{
					map((bank << 16) + (page << 8), access, mirror(offset + index, (Integer)((Configurable)access).readConfig("size")));
					index += 256;
					if (size != 0)
					{
						index %= size;
					}
				}

				index += ((255 - page_hi) * 256);
				if (size != 0)
				{
					index %= size;
				}
			}
		}
			break;
		}
	}
}
