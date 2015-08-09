/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.ppu;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class HVCounter implements java.io.Serializable
{
	class Status implements java.io.Serializable
	{
		public boolean interlace;
		public boolean field;
		public int vcounter;
		public int hcounter;
	}
	
	//public transient PPU ppu;
	public Status status = new Status();
	int region;
	
	//adding helper method for serialization to save/initialize super class state
    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException{
        in.defaultReadObject();
         
        //this.ppu = PPU.ppu;
    }
     
    private void writeObject(ObjectOutputStream out) throws IOException{
        out.defaultWriteObject();
         
    }
    
	public HVCounter(PPU ppu)
	{
		//this.ppu = ppu;
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
		if (region == PPU.NTSC && !status.interlace && status.vcounter == 240 && status.field)
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
		if (region == PPU.NTSC && !status.interlace && status.vcounter == 240 && status.field) { return 1360; }
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
			status.interlace = PPU.ppu.display.interlace;
		}

		if ((region == PPU.NTSC && status.interlace == false && status.vcounter == 262)
				|| (region == PPU.NTSC && status.interlace == true && status.vcounter == 263)
				|| (region == PPU.NTSC && status.interlace == true && status.vcounter == 262 && status.field == true)
				|| (region == PPU.PAL && status.interlace == false && status.vcounter == 312)
				|| (region == PPU.PAL && status.interlace == true && status.vcounter == 313)
				|| (region == PPU.PAL && status.interlace == true && status.vcounter == 312 && status.field == true))
		{
			status.vcounter = 0;
			status.field = !status.field;
		}
	}
}
