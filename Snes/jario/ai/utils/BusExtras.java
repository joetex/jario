package jario.ai.utils;

import jario.hardware.Bus8bit;

public class BusExtras 
{
	//little-endian 16 bit 
	public static int read16bit(Bus8bit bus, int address)
	{
		int l = (bus.read8bit(address) & 0xFF);
		int h = (bus.read8bit(address+1) & 0xFF);
		int result = ((h<<8) | l);
		return (result & 0xFFFF);
	}
	
	public static void write16bit(Bus8bit bus, int address, int value)
	{
		byte low = (byte)(value & 0xFF);
		byte high = (byte)((value & 0xFF00) >> 8);
		
		bus.write8bit(address, low);
		bus.write8bit(address+1, high);
	}
}
