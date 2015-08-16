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
}
