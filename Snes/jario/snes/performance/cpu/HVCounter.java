/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.cpu;

import jario.hardware.Bus1bit;

public class HVCounter implements java.io.Serializable
{
	class Status implements java.io.Serializable
	{
		public boolean interlace;
		public boolean field;
		public int vcounter;
		public int hcounter;
	}
	
	Bus1bit ppu1bit;
	private Status status = new Status();
	int region;
	transient Runnable scanline;
	
	public HVCounter()
	{
	}

	final void tick(int clocks)
	{
		status.hcounter += clocks;
		if (status.hcounter >= lineclocks())
		{
			status.hcounter -= lineclocks();
			vcounter_tick();
		}
	}
	
	final boolean field()
	{
		return status.field;
	}

	final int vcounter()
	{
		return status.vcounter;
	}

	final int hcounter()
	{
		return status.hcounter;
	}

	final int hdot()
	{
		if (region == CPU.NTSC && !status.interlace && status.vcounter == 240 && status.field)
		{
			return (status.hcounter >> 2);
		}
		else
		{
			return ((status.hcounter - (((status.hcounter > 1292) ? 1 : 0) << 1) - (((status.hcounter > 1310) ? 1 : 0) << 1)) >> 2);
		}
	}

	final int lineclocks()
	{
		if (region == CPU.NTSC && !status.interlace && status.vcounter == 240 && status.field) { return 1360; }
		return 1364;
	}

	void reset()
	{
		status.interlace = false;
		status.field = false;
		status.vcounter = 0;
		status.hcounter = 0;
	}

	private void vcounter_tick()
	{
		if (++status.vcounter == 128)
		{
			status.interlace = ppu1bit.read1bit(0);
		}

		if ((region == CPU.NTSC && status.interlace == false && status.vcounter == 262)
				|| (region == CPU.NTSC && status.interlace == true && status.vcounter == 263)
				|| (region == CPU.NTSC && status.interlace == true && status.vcounter == 262 && status.field == true)
				|| (region == CPU.PAL && status.interlace == false && status.vcounter == 312)
				|| (region == CPU.PAL && status.interlace == true && status.vcounter == 313)
				|| (region == CPU.PAL && status.interlace == true && status.vcounter == 312 && status.field == true))
		{
			status.vcounter = 0;
			status.field = !status.field;
		}
		if (scanline != null)
		{
			scanline.run();
		}
	}
}
