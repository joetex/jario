/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.dsp;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import jario.hardware.Bus32bit;
import jario.hardware.Bus8bit;
import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

public class DSP implements Hardware, Clockable, Bus8bit, Configurable, java.io.Serializable
{
	private long clock;

	private byte[] apuram = new byte[64 * 1024];
	private transient Bus32bit output;

	private SPCDSP spc_dsp = new SPCDSP();
	private short[] samplebuffer = new short[8192];
	boolean[] channel_enabled = new boolean[8];
	
	public boolean enableaudio = true;
	
	private Bus8bit sram = new Bus8bit()
	{
		@Override
		public byte read8bit(int address)
		{
			return apuram[address];
		}

		@Override
		public void write8bit(int address, byte data)
		{
			apuram[address] = data;
		}
	};

	public DSP()
	{
		for (int i = 0; i < 8; i++)
		{
			channel_enabled[i] = true;
		}
		clock = 0;
		power();
	}

	@Override
	public void connect(int port, Hardware hw)
	{
		switch (port)
		{
		case 0:
			output = (Bus32bit) hw;
			break;
		}
	}

	@Override
	public byte read8bit(int addr)
	{
		return spc_dsp.read(addr);
	}

	@Override
	public void write8bit(int addr, byte data)
	{
		spc_dsp.write(addr, data);
	}

	@Override
	public void clock(long clocks)
	{
		clock -= clocks;
		while (clock < 0L)
		{
			spc_dsp.run(1);
			clock += 24L;

			int count = spc_dsp.sample_count();
			if (count > 0)
			{
				if(enableaudio)
					for (int n = 0; n < count; n += 2)
					{
						output.write32bit(0, ((samplebuffer[n + 0] & 0xFFFF) << 16) | (samplebuffer[n + 1] & 0xFFFF));
					}
				spc_dsp.set_output(samplebuffer, 8192);
			}
		}
	}

	@Override
	public void reset()
	{
		spc_dsp.soft_reset();
		spc_dsp.set_output(samplebuffer, 8192);
		clock = 0;
	}
	
	@Override
	public Object readConfig(String key)
	{
		if (key.equals("enableaudio")) return enableaudio;
		if (key.equalsIgnoreCase("SRAM")) return sram;
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("enableaudio")) enableaudio = (Boolean) value;
		else if(key.equals("save")) {
			ObjectOutputStream out = (ObjectOutputStream)value;
			
			try
			{
				out.writeInt(apuram.length);
				for(int i=0;i<apuram.length;i++)
				{
					out.writeByte(apuram[i]);
				}
				
				out.writeObject(spc_dsp);
				
				spc_dsp.m.ram = apuram;
				
				out.writeObject(samplebuffer);
				out.writeObject(channel_enabled);
				
			}
			catch(Exception e) { e.printStackTrace(); }
			
			
			
		}
		else if(key.equals("load")) {
			ObjectInputStream in = (ObjectInputStream)value;
			try
			{
				int len = in.readInt();
				for(int i=0;i<len;i++)
				{
					apuram[i]=in.readByte();
				}
				
				spc_dsp = (SPCDSP)in.readObject();
				samplebuffer = (short[])in.readObject();
				channel_enabled = (boolean[])in.readObject();
			}
			catch(Exception e) { e.printStackTrace(); }
			
		}
	}

	private void power()
	{
		spc_dsp.init(apuram);
		spc_dsp.reset();
		spc_dsp.set_output(samplebuffer, 8192);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		spc_dsp = null;
		sram = null;
	}

	// private void channel_enable(int channel, boolean enable)
	// {
	// channel_enabled[channel & 7] = enable;
	// int mask = 0;
	// for (int i = 0; i < 8; i++)
	// {
	// if (channel_enabled[i] == false)
	// {
	// mask |= (1 << i);
	// }
	// }
	// spc_dsp.mute_voices(mask);
	// }
}
