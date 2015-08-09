/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.memory;

import jario.hardware.Bus8bit;

public class UnmappedMMIO implements Bus8bit, java.io.Serializable
{
	byte dummy = 0;
	
	@Override
	public byte read8bit(int addr)
	{
		return MemoryBus.cpu.read8bit(0x430c); // cpu regs.mdr
	}

	@Override
	public void write8bit(int addr, byte data)
	{
	}
}
