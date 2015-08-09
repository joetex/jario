/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.cpu;

import jario.hardware.Bus8bit;
import jario.hardware.Hardware;

public class BusB extends Status implements Hardware, Bus8bit, java.io.Serializable
{
	@Override
	public void connect(int port, Hardware hw)
	{
	}

	@Override
	public void reset()
	{
	}

	@Override
	public byte read8bit(int address)
	{
		return (byte) CPU.cpu.port_data[address & 0x3];
	}

	@Override
	public void write8bit(int address, byte data)
	{
		CPU.cpu.port_data[address & 0x3] = data & 0xFF;
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
}
