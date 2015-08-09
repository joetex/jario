/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.memory;

import jario.hardware.Bus8bit;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

public class UnmappedMemory implements Hardware, Bus8bit, Configurable, java.io.Serializable
{
	private byte dummy = 0;
	
	@Override
	public void connect(int port, Hardware hw)
	{
	}

	@Override
	public void reset()
	{
	}

	@Override
	public byte read8bit(int addr)
	{
		return MemoryBus.cpu.read8bit(0x430c); // cpu regs.mdr
	}

	@Override
	public void write8bit(int addr, byte data)
	{
	}

	@Override
	public Object readConfig(String key)
	{
		if (key.equals("size")) return 16 * 1024 * 1024;
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
}
