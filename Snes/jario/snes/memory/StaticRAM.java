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

import java.util.Arrays;

public class StaticRAM implements Hardware, Bus8bit, Configurable, java.io.Serializable
{
	private byte[] data_;
	private int size_;

	public StaticRAM(int n)
	{
		size_ = n;
		data_ = new byte[size_];
	}

	@Override
	public void connect(int port, Hardware hw)
	{
	}

	@Override
	public void reset()
	{
		Arrays.fill(data_, (byte) 0);
	}

	@Override
	public byte read8bit(int addr)
	{
		return data_[addr];
	}

	@Override
	public void write8bit(int addr, byte n)
	{
		data_[addr] = n;
	}

	@Override
	public Object readConfig(String key)
	{
		if (key.equals("size")) return size_;
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
