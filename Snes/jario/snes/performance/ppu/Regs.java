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

public class Regs implements java.io.Serializable
{
	//adding helper method for serialization to save/initialize super class state
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException{
        ois.defaultReadObject();
         
    }
     
    private void writeObject(ObjectOutputStream oos) throws IOException{
        oos.defaultWriteObject();
         
    }
    
	// internal
	public int ppu1_mdr;
	public int ppu2_mdr;

	public int vram_readbuffer;
	public int oam_latchdata;
	public int cgram_latchdata;
	public int bgofs_latchdata;
	public int mode7_latchdata;

	public boolean counters_latched;
	public boolean latch_hcounter;
	public boolean latch_vcounter;

	// $2100
	public boolean display_disable;
	public int display_brightness;

	// $2102-$2103
	public int oam_baseaddr;
	public int oam_addr;
	public boolean oam_priority;

	// $2105
	public boolean bg3_priority;
	public int bgmode;

	// $210d
	public int mode7_hoffset;

	// $210e
	public int mode7_voffset;

	// $2115
	public boolean vram_incmode;
	public int vram_mapping;
	public int vram_incsize;

	// $2116-$2117
	public int vram_addr;

	// $211a
	public int mode7_repeat;
	public boolean mode7_vflip;
	public boolean mode7_hflip;

	// $211b-$2120
	public int m7a;
	public int m7b;
	public int m7c;
	public int m7d;
	public int m7x;
	public int m7y;

	// $2121
	public int cgram_addr;

	// $2126-$212a
	public int window_one_left;
	public int window_one_right;
	public int window_two_left;
	public int window_two_right;

	// $2133
	public boolean mode7_extbg;
	public boolean pseudo_hires;
	public boolean overscan;
	public boolean interlace;

	// $213c
	public int hcounter;

	// $213d
	public int vcounter;
}
