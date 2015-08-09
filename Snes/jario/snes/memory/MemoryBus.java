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
import java.util.concurrent.Executors;

import jario.hardware.Bus1bit;
import jario.hardware.Bus8bit;
import jario.hardware.Configurable;
import jario.hardware.Hardware;
import jario.snes.configuration.Configuration;
import jario.snes.console.Console.Region;

public class MemoryBus extends Bus implements Hardware, Bus1bit, Configurable, java.io.Serializable
{
	private transient StaticRAM wram;
	private transient MMIOAccess mmio;
	private transient UnmappedMMIO mmio_unmapped;
	//public static UnmappedMMIO mmio_unmapped_static;
	
	static Bus8bit cpu;
	private transient Bus8bit ppu;
	private transient Configurable cartridge;

	private byte wram_init_value;

	public MemoryBus()
	{
		wram = new StaticRAM(128 * 1024);
		mmio_unmapped = new UnmappedMMIO();
		//mmio_unmapped_static = mmio_unmapped;
		mmio = new MMIOAccess(mmio_unmapped);
		map_reset();
	}

	
	//adding helper method for serialization to save/initialize super class state
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException{
    	
    	ois.defaultReadObject();

    	wram = (StaticRAM)ois.readObject();
    	
    	mmio_unmapped = (UnmappedMMIO)ois.readObject();
    	//mmio_unmapped_static = mmio_unmapped;
    	mmio = (MMIOAccess)ois.readObject();
    	//ppu = (Bus8bit)ois.readObject();
    	//cartridge = (Configurable)ois.readObject();
    	//mmio_unmapped = new UnmappedMMIO();
		//mmio = new MMIOAccess(mmio_unmapped);
		
    	//map_reset();
    }
     
    private void writeObject(ObjectOutputStream oos) throws IOException{
        
    	oos.defaultWriteObject();
       
    	oos.writeObject(wram);
    	
    	oos.writeObject(mmio_unmapped);
    	oos.writeObject(mmio);
    	//oos.writeObject(ppu);
    	//oos.writeObject(cartridge);
    	
    	
    }
    
    
	@Override
	public void connect(int port, Hardware hw)
	{
		switch (port)
		{
		case 0:
			cpu = (Bus8bit) hw;
			break;
		case 1:
			ppu = (Bus8bit) hw;
			break;
		case 2:
			cartridge = (Configurable) hw;
			load_cart();
			for (int i = 0x2100; i <= 0x213f; i++)
			{
				mmio.map(i, ppu);
			}
			for (int i = 0x2140; i <= 0x217f; i++)
			{
				mmio.map(i, cpu);
			}
			for (int i = 0x2180; i <= 0x2183; i++)
			{
				mmio.map(i, cpu);
			}
			for (int i = 0x4016; i <= 0x4017; i++)
			{
				mmio.map(i, cpu);
			}
			for (int i = 0x4200; i <= 0x421f; i++)
			{
				mmio.map(i, cpu);
			}
			for (int i = 0x4300; i <= 0x437f; i++)
			{
				mmio.map(i, cpu);
			}
			break;
		case 3:
			cartridge = (Configurable) hw;
			for (int i = 0x2100; i <= 0x213f; i++)
			{
				mmio.map(i, ppu);
			}
			for (int i = 0x2140; i <= 0x217f; i++)
			{
				mmio.map(i, cpu);
			}
			for (int i = 0x2180; i <= 0x2183; i++)
			{
				mmio.map(i, cpu);
			}
			for (int i = 0x4016; i <= 0x4017; i++)
			{
				mmio.map(i, cpu);
			}
			for (int i = 0x4200; i <= 0x421f; i++)
			{
				mmio.map(i, cpu);
			}
			for (int i = 0x4300; i <= 0x437f; i++)
			{
				mmio.map(i, cpu);
			}
			break;
		}
	}

	@Override
	public void reset()
	{
	}

	@Override
	public Object readConfig(String key)
	{
		if (key.equals("mmio")) return mmio;
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("wram init value"))
		{
			wram_init_value = (Byte) value;
			power();
		}
		else if (key.equals("map"))
		{
			Object[] params = (Object[])value;
			map(MapMode.values()[(Integer)params[0]], (Integer)params[1], (Integer)params[2], (Integer)params[3],
					(Integer)params[4], (Bus8bit)params[5], (Integer)params[6], (Integer)params[7]);
		}
		else if(key.equals("save")) {
			ObjectOutputStream out = (ObjectOutputStream)value;
			
			try
			{
				out.writeObject(wram);
				out.writeByte(wram_init_value);
			}
			catch(Exception e) {}
		}
		
		else if(key.equals("load")) {
			ObjectInputStream in = (ObjectInputStream)value;
			
			try
			{
				wram = (StaticRAM)in.readObject();
				wram_init_value = in.readByte();
			}
			catch(Exception e) {}
			
			map_xml();
			map_system();
			
			
		}
		else if(key.equals("loadcart"))
		{
			load_cart();
		}
	}
	
	private void load_cart()
	{
		map_reset();
		map_xml();
		map_system();
	}

	private void power()
	{
		for (int n = 0; n < (128 * 1024); n++)
		{
			wram.write8bit(n, wram_init_value);
		}
	}

	private void map_reset()
	{
		map(MapMode.Direct, 0x00, 0xff, 0x0000, 0xffff, memory_unmapped);
		map(MapMode.Direct, 0x00, 0x3f, 0x2000, 0x5fff, mmio);
		map(MapMode.Direct, 0x80, 0xbf, 0x2000, 0x5fff, mmio);
		for (int i = 0x2000; i <= 0x5fff; i++)
		{
			mmio.map(i, mmio_unmapped);
		}
	}

	private void map_xml()
	{
		if (cartridge != null)
		{
			cartridge.writeConfig("mapmem", this);
		}
	}

	private void map_system()
	{
		map(MapMode.Linear, 0x00, 0x3f, 0x0000, 0x1fff, wram, 0x000000, 0x002000);
		map(MapMode.Linear, 0x80, 0xbf, 0x0000, 0x1fff, wram, 0x000000, 0x002000);
		map(MapMode.Linear, 0x7e, 0x7f, 0x0000, 0xffff, wram);
	}

	@Override
	public boolean read1bit(int address)
	{
		return false;
	}

	@Override
	public void write1bit(int address, boolean data)
	{
		if (address == 0) ((Bus1bit)cpu).write1bit(0, data); // irq
	}


	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		cpu = null;
		ppu = null;
		cartridge = null;
	}
}
