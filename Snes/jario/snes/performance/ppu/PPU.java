/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.ppu;

import jario.hardware.Bus1bit;
import jario.hardware.Bus8bit;
import jario.hardware.BusDMA;
import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class PPU implements Hardware, Clockable, Bus1bit, Bus8bit, BusDMA, Configurable, java.io.Serializable
{
	public static final int NTSC = 0;
	public static final int PAL = 1;

	public static PPU ppu;

	private long clock;
	private int region;
	
	private transient Bus8bit bus;

	int[] vram;
	int[] oam;
	int[] cgram;

	private boolean enablerender = true;
	
	@Override
	public void connect(int port, Hardware hw)
	{
		switch (port)
		{
		case 0: bus = (Bus8bit) hw; break;
		}
	}

	private void step(int clocks)
	{
		clock += clocks;
	}

	private void synchronize_cpu()
	{

	}

	private void latch_counters()
	{
		regs.hcounter = cpuCounter.hdot() & 0xFFFF;
		regs.vcounter = cpuCounter.vcounter() & 0xFFFF;
		regs.counters_latched = true;
	}
	
	@Override
	public boolean read1bit(int address)
	{
		switch (address)
		{
		case 0: return interlace();
		case 1: return overscan();
		case 2: return hires();
		case 3: return ppuCounter.field();
		default: return false;
		}
	}
	
	@Override
	public void write1bit(int address, boolean data)
	{
		switch (address)
		{
		case 29:
			if (display.latch != 0 && data == false)
			{
				latch_counters();
			}
			display.latch = data ? 1 : 0;
			break;
		}
	}

	boolean interlace()
	{
		return display.interlace;
	}

	boolean overscan()
	{
		return display.overscan;
	}

	boolean hires()
	{
		return regs.pseudo_hires || regs.bgmode == 5 || regs.bgmode == 6;
	}

	@Override
	public void clock(long clocks)
	{
		clock -= clocks;
		cpuCounter.tick((int) clocks);
		while (clock < 0L)
		{
			scanline();
			if (ppuCounter.vcounter() < display.height && (ppuCounter.vcounter() != 0))
			{
				add_clocks(512);
				render_scanline();
				add_clocks(ppuCounter.lineclocks() - 512);
			}
			else
			{
				add_clocks(ppuCounter.lineclocks());
			}
		}
	}

	private void power()
	{
		Arrays.fill(vram, 0);
		Arrays.fill(oam, 0);
		Arrays.fill(cgram, 0);
		reset();
	}

	@Override
	public void reset()
	{
		clock = 0;
		ppuCounter.reset();
		cpuCounter.reset();
		Arrays.fill(output.array(), (byte) 0);
		mmio_reset();
		if (display != null)
		{
			display.interlace = false;
			display.overscan = false;
			display.latch = 1;
		}
	}

	private void scanline()
	{
		if(!enablerender)
			return;
		display.width = !hires() ? 256 : 512;
		display.height = !overscan() ? 225 : 240;
		if (ppuCounter.vcounter() == 0)
		{
			frame();
		}
		if (ppuCounter.vcounter() == display.height && regs.display_disable == false)
		{
			sprite.address_reset();
		}
	}

	private void frame()
	{
		sprite.frame();
		display.interlace = regs.interlace;
		display.overscan = regs.overscan;
		// display.framecounter = display.frameskip == 0 ? 0 : (display.framecounter + 1) % display.frameskip;
	}

//	public void layer_enable(int layer, int priority, boolean enable)
//	{
//		switch (layer * 4 + priority)
//		{
//		case 0:
//			bg1.priority0_enable = enable;
//			break;
//		case 1:
//			bg1.priority1_enable = enable;
//			break;
//		case 4:
//			bg2.priority0_enable = enable;
//			break;
//		case 5:
//			bg2.priority1_enable = enable;
//			break;
//		case 8:
//			bg3.priority0_enable = enable;
//			break;
//		case 9:
//			bg3.priority1_enable = enable;
//			break;
//		case 12:
//			bg4.priority0_enable = enable;
//			break;
//		case 13:
//			bg4.priority1_enable = enable;
//			break;
//		case 16:
//			sprite.priority0_enable = enable;
//			break;
//		case 17:
//			sprite.priority1_enable = enable;
//			break;
//		case 18:
//			sprite.priority2_enable = enable;
//			break;
//		case 19:
//			sprite.priority3_enable = enable;
//			break;
//		}
//	}

	// public void set_frameskip(int frameskip)
	// {
	// display.frameskip = frameskip;
	// display.framecounter = 0;
	// }
	transient Cache cache;
	transient Background bg1;
	transient Background bg2;
	transient Background bg3;
	transient Background bg4;
	transient Sprite sprite;
	transient Screen screen;
	transient Display display;

	
	public PPU()
	{
		ppu = this;
		vram = new int[64 * 1024];
		oam = new int[544];
		cgram = new int[512];
		cache = new Cache(this);
		bg1 = new Background(this, Background.ID_BG1);
		bg2 = new Background(this, Background.ID_BG2);
		bg3 = new Background(this, Background.ID_BG3);
		bg4 = new Background(this, Background.ID_BG4);
		sprite = new Sprite(this);
		screen = new Screen(this);
		output = ByteBuffer.allocate(1024 * 1024 * 2);
		
		display = new Display();
		display.width = 256;
		display.height = 224;
		display.interlace = false;
		display.overscan = false;
		display.latch = 1;
		
		cpuCounter = new HVCounter(this);
		ppuCounter = new HVCounter(this);

		power();
	}

	transient ByteBuffer output;
	transient Regs regs = new Regs();
	
	//adding helper method for serialization to save/initialize super class state
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException{
        ois.defaultReadObject();
        
        cache = (Cache)ois.readObject();
        bg1 = (Background)ois.readObject();
        bg2 = (Background)ois.readObject();
        bg3 = (Background)ois.readObject();
        bg4 = (Background)ois.readObject();
        sprite = (Sprite)ois.readObject();
        screen = (Screen)ois.readObject();
        display = (Display)ois.readObject();
        
        regs = (Regs)ois.readObject();
        
        
        cpuCounter = new HVCounter(this);
		ppuCounter = new HVCounter(this);
		
		output = ByteBuffer.allocate(1024 * 1024 * 2);
		
        int len = ois.readInt();
        int pos = ois.readInt();
        byte[] buff = new byte[len];
        ois.read(buff, 0, len);
        output.put(buff);
        output.position(pos);
        ppu = this;
        //output.put(buff);
    }
     
    private void writeObject(ObjectOutputStream oos) throws IOException
    {
       oos.defaultWriteObject();
         
       oos.writeObject(cache);
       oos.writeObject(bg1);
       oos.writeObject(bg2);
       oos.writeObject(bg3);
       oos.writeObject(bg4);
       oos.writeObject(sprite);
       oos.writeObject(screen);
       oos.writeObject(display);
       
       oos.writeObject(regs);
       
       byte[] buff = output.array();
       oos.writeInt(buff.length);
       oos.writeInt(output.position());
       oos.write(buff);
    }
    
	private int get_vram_addr()
	{
		int addr = regs.vram_addr;
		switch (regs.vram_mapping)
		{
		case 0:
			break;
		case 1:
			addr = ((addr & 0xff00) | ((addr & 0x001f) << 3) | ((addr >> 5) & 7));
			break;
		case 2:
			addr = ((addr & 0xfe00) | ((addr & 0x003f) << 3) | ((addr >> 6) & 7));
			break;
		case 3:
			addr = ((addr & 0xfc00) | ((addr & 0x007f) << 3) | ((addr >> 7) & 7));
			break;
		}
		return (addr << 1) & 0xFFFF;
	}

	private int vram_read(int addr)
	{
		if (regs.display_disable) { return vram[addr] & 0xFF; }
		if (cpuCounter.vcounter() >= display.height) { return vram[addr] & 0xFF; }
		return 0x00;
	}

	private void vram_write(int addr, int data)
	{
		if (regs.display_disable || cpuCounter.vcounter() >= display.height)
		{
			vram[addr] = data & 0xFF;
			cache.tilevalid[0][addr >> 4] = 0;
			cache.tilevalid[1][addr >> 5] = 0;
			cache.tilevalid[2][addr >> 6] = 0;
			return;
		}
	}

	private int oam_read(int addr)
	{
		if ((addr & 0x0200) != 0)
		{
			addr &= 0x021f;
		}
		if (regs.display_disable) { return oam[addr] & 0xFF; }
		if (cpuCounter.vcounter() >= display.height) { return oam[addr] & 0xFF; }
		return oam[0x0218] & 0xFF;
	}

	private void oam_write(int addr, int data)
	{
		if ((addr & 0x0200) != 0)
		{
			addr &= 0x021f;
		}
		if (!regs.display_disable && cpuCounter.vcounter() < display.height)
		{
			addr = 0x0218;
		}
		oam[addr] = data & 0xFF;
		sprite.update_list(addr, data & 0xFF);
	}

	private int cgram_read(int addr)
	{
		return cgram[addr] & 0xFF;
	}

	private void cgram_write(int addr, int data)
	{
		cgram[addr] = data & 0xFF;
	}

	private void mmio_update_video_mode()
	{
		switch (regs.bgmode)
		{
		case 0:
		{
			bg1.regs.mode = Background.Mode_BPP2;
			bg1.regs.priority0 = 8;
			bg1.regs.priority1 = 11;
			bg2.regs.mode = Background.Mode_BPP2;
			bg2.regs.priority0 = 7;
			bg2.regs.priority1 = 10;
			bg3.regs.mode = Background.Mode_BPP2;
			bg3.regs.priority0 = 2;
			bg3.regs.priority1 = 5;
			bg4.regs.mode = Background.Mode_BPP2;
			bg4.regs.priority0 = 1;
			bg4.regs.priority1 = 4;
			sprite.regs.priority0 = 3;
			sprite.regs.priority1 = 6;
			sprite.regs.priority2 = 9;
			sprite.regs.priority3 = 12;
		}
			break;

		case 1:
		{
			bg1.regs.mode = Background.Mode_BPP4;
			bg2.regs.mode = Background.Mode_BPP4;
			bg3.regs.mode = Background.Mode_BPP2;
			bg4.regs.mode = Background.Mode_Inactive;
			if (regs.bg3_priority)
			{
				bg1.regs.priority0 = 5;
				bg1.regs.priority1 = 8;
				bg2.regs.priority0 = 4;
				bg2.regs.priority1 = 7;
				bg3.regs.priority0 = 1;
				bg3.regs.priority1 = 10;
				sprite.regs.priority0 = 2;
				sprite.regs.priority1 = 3;
				sprite.regs.priority2 = 6;
				sprite.regs.priority3 = 9;
			}
			else
			{
				bg1.regs.priority0 = 6;
				bg1.regs.priority1 = 9;
				bg2.regs.priority0 = 5;
				bg2.regs.priority1 = 8;
				bg3.regs.priority0 = 1;
				bg3.regs.priority1 = 3;
				sprite.regs.priority0 = 2;
				sprite.regs.priority1 = 4;
				sprite.regs.priority2 = 7;
				sprite.regs.priority3 = 10;
			}
		}
			break;

		case 2:
		{
			bg1.regs.mode = Background.Mode_BPP4;
			bg2.regs.mode = Background.Mode_BPP4;
			bg3.regs.mode = Background.Mode_Inactive;
			bg4.regs.mode = Background.Mode_Inactive;
			bg1.regs.priority0 = 3;
			bg1.regs.priority1 = 7;
			bg2.regs.priority0 = 1;
			bg2.regs.priority1 = 5;
			sprite.regs.priority0 = 2;
			sprite.regs.priority1 = 4;
			sprite.regs.priority2 = 6;
			sprite.regs.priority3 = 8;
		}
			break;

		case 3:
		{
			bg1.regs.mode = Background.Mode_BPP8;
			bg2.regs.mode = Background.Mode_BPP4;
			bg3.regs.mode = Background.Mode_Inactive;
			bg4.regs.mode = Background.Mode_Inactive;
			bg1.regs.priority0 = 3;
			bg1.regs.priority1 = 7;
			bg2.regs.priority0 = 1;
			bg2.regs.priority1 = 5;
			sprite.regs.priority0 = 2;
			sprite.regs.priority1 = 4;
			sprite.regs.priority2 = 6;
			sprite.regs.priority3 = 8;
		}
			break;

		case 4:
		{
			bg1.regs.mode = Background.Mode_BPP8;
			bg2.regs.mode = Background.Mode_BPP2;
			bg3.regs.mode = Background.Mode_Inactive;
			bg4.regs.mode = Background.Mode_Inactive;
			bg1.regs.priority0 = 3;
			bg1.regs.priority1 = 7;
			bg2.regs.priority0 = 1;
			bg2.regs.priority1 = 5;
			sprite.regs.priority0 = 2;
			sprite.regs.priority1 = 4;
			sprite.regs.priority2 = 6;
			sprite.regs.priority3 = 8;
		}
			break;

		case 5:
		{
			bg1.regs.mode = Background.Mode_BPP4;
			bg2.regs.mode = Background.Mode_BPP2;
			bg3.regs.mode = Background.Mode_Inactive;
			bg4.regs.mode = Background.Mode_Inactive;
			bg1.regs.priority0 = 3;
			bg1.regs.priority1 = 7;
			bg2.regs.priority0 = 1;
			bg2.regs.priority1 = 5;
			sprite.regs.priority0 = 2;
			sprite.regs.priority1 = 4;
			sprite.regs.priority2 = 6;
			sprite.regs.priority3 = 8;
		}
			break;

		case 6:
		{
			bg1.regs.mode = Background.Mode_BPP4;
			bg2.regs.mode = Background.Mode_Inactive;
			bg3.regs.mode = Background.Mode_Inactive;
			bg4.regs.mode = Background.Mode_Inactive;
			bg1.regs.priority0 = 2;
			bg1.regs.priority1 = 5;
			sprite.regs.priority0 = 1;
			sprite.regs.priority1 = 3;
			sprite.regs.priority2 = 4;
			sprite.regs.priority3 = 6;
		}
			break;

		case 7:
		{
			if (regs.mode7_extbg == false)
			{
				bg1.regs.mode = Background.Mode_Mode7;
				bg2.regs.mode = Background.Mode_Inactive;
				bg3.regs.mode = Background.Mode_Inactive;
				bg4.regs.mode = Background.Mode_Inactive;
				bg1.regs.priority0 = 2;
				bg1.regs.priority1 = 2;
				sprite.regs.priority0 = 1;
				sprite.regs.priority1 = 3;
				sprite.regs.priority2 = 4;
				sprite.regs.priority3 = 5;
			}
			else
			{
				bg1.regs.mode = Background.Mode_Mode7;
				bg2.regs.mode = Background.Mode_Mode7;
				bg3.regs.mode = Background.Mode_Inactive;
				bg4.regs.mode = Background.Mode_Inactive;
				bg1.regs.priority0 = 3;
				bg1.regs.priority1 = 3;
				bg2.regs.priority0 = 1;
				bg2.regs.priority1 = 5;
				sprite.regs.priority0 = 2;
				sprite.regs.priority1 = 4;
				sprite.regs.priority2 = 6;
				sprite.regs.priority3 = 7;
			}
		}
			break;
		}
	}

	@Override
	public byte read8bit(int addr)
	{
		// CPU.cpu.synchronize_ppu();

		switch (addr & 0xffff)
		{
		
		case 0x2104: case 0x2105: case 0x2106: case 0x2108: case 0x2109: case 0x210a:
	    case 0x2114: case 0x2115: case 0x2116: case 0x2118: case 0x2119: case 0x211a:
	    case 0x2124: case 0x2125: case 0x2126: case 0x2128: case 0x2129: case 0x212a: {
	      return (byte) regs.ppu1_mdr;
	    }
		
		case 0x2134:
		{ // MPYL
			int result = (int) ((short) regs.m7a * (byte) (regs.m7b >> 8));
			regs.ppu1_mdr = (result >> 0) & 0xFF;
			return (byte) regs.ppu1_mdr;
		}
		case 0x2135:
		{ // MPYM
			int result = (int) ((short) regs.m7a * (byte) (regs.m7b >> 8));
			regs.ppu1_mdr = (result >> 8) & 0xFF;
			return (byte) regs.ppu1_mdr;
		}
		case 0x2136:
		{ // MPYH
			int result = (int) ((short) regs.m7a * (byte) (regs.m7b >> 8));
			regs.ppu1_mdr = (result >> 16) & 0xFF;
			return (byte) regs.ppu1_mdr;
		}
		case 0x2137:
		{ // SLHV
			if (display.latch != 0)
			{
				latch_counters();
			}
			return (byte) regs.ppu1_mdr; // CPU.cpu.regs.mdr;
		}
		case 0x2138:
		{ // OAMDATAREAD
			regs.ppu1_mdr = oam_read(regs.oam_addr) & 0xFF;
			regs.oam_addr = ((regs.oam_addr + 1) & 0x03ff);
			sprite.set_first();
			return (byte) regs.ppu1_mdr;
		}
		case 0x2139:
		{ // VMDATALREAD
			regs.ppu1_mdr = (regs.vram_readbuffer >> 0) & 0xFF;
			if (regs.vram_incmode == false)
			{
				addr = get_vram_addr();
				regs.vram_readbuffer = (vram_read(addr + 0) << 0);
				regs.vram_readbuffer |= (vram_read(addr + 1) << 8);
				regs.vram_addr = (regs.vram_addr + regs.vram_incsize) & 0xFFFF;
			}
			return (byte) regs.ppu1_mdr;
		}
		case 0x213a:
		{ // VMDATAHREAD
			regs.ppu1_mdr = (regs.vram_readbuffer >> 8) & 0xFF;
			if (regs.vram_incmode == true)
			{
				addr = get_vram_addr();
				regs.vram_readbuffer = (vram_read(addr + 0) << 0);
				regs.vram_readbuffer |= (vram_read(addr + 1) << 8);
				regs.vram_addr = (regs.vram_addr + regs.vram_incsize) & 0xFFFF;
			}
			return (byte) regs.ppu1_mdr;
		}
		case 0x213b:
		{ // CGDATAREAD
			if ((regs.cgram_addr & 1) == 0)
			{
				regs.ppu2_mdr = cgram_read(regs.cgram_addr) & 0xFF;
			}
			else
			{
				regs.ppu2_mdr = ((regs.ppu2_mdr & 0x80) | (cgram_read(regs.cgram_addr) & 0x7f));
			}
			regs.cgram_addr = ((regs.cgram_addr + 1) & 0x01ff);
			return (byte) regs.ppu2_mdr;
		}
		case 0x213c:
		{ // OPHCT
			if (regs.latch_hcounter == false)
			{
				regs.ppu2_mdr = (regs.hcounter & 0xff);
			}
			else
			{
				regs.ppu2_mdr = ((regs.ppu2_mdr & 0xfe) | (regs.hcounter >> 8)) & 0xFF;
			}
			regs.latch_hcounter ^= true;
			return (byte) regs.ppu2_mdr;
		}
		case 0x213d:
		{ // OPVCT
			if (regs.latch_vcounter == false)
			{
				regs.ppu2_mdr = (regs.vcounter & 0xff);
			}
			else
			{
				regs.ppu2_mdr = ((regs.ppu2_mdr & 0xfe) | (regs.vcounter >> 8)) & 0xFF;
			}
			regs.latch_vcounter ^= true;
			return (byte) regs.ppu2_mdr;
		}
		case 0x213e:
		{ // STAT77
			regs.ppu1_mdr &= 0x10;
			regs.ppu1_mdr |= ((sprite.regs.time_over ? 1 : 0) << 7);
			regs.ppu1_mdr |= ((sprite.regs.range_over ? 1 : 0) << 6);
			regs.ppu1_mdr |= 0x01; // version
			return (byte) regs.ppu1_mdr;
		}
		case 0x213f:
		{ // STAT78
			regs.latch_hcounter = false;
			regs.latch_vcounter = false;

			regs.ppu2_mdr &= 0x20;
			regs.ppu2_mdr |= ((cpuCounter.field() ? 1 : 0) << 7);
			if (display.latch == 0)
			{
				regs.ppu2_mdr |= 0x40;
			}
			else if (regs.counters_latched)
			{
				regs.ppu2_mdr |= 0x40;
				regs.counters_latched = false;
			}
			regs.ppu2_mdr |= ((region == NTSC ? 0 : 1) << 4);
			regs.ppu2_mdr |= 0x03; // version
			return (byte) regs.ppu2_mdr;
		}
		}

		//return (byte) regs.ppu1_mdr; // CPU.cpu.regs.mdr;
		return (byte) bus.read8bit(0x430c);
	}

	@Override
	public void write8bit(int addr, byte data_)
	{
		// CPU.cpu.synchronize_ppu();

		int data = data_ & 0xFF;

		switch (addr & 0xffff)
		{
		case 0x2100:
		{ // INIDISP
			if (regs.display_disable && cpuCounter.vcounter() == display.height)
			{
				sprite.address_reset();
			}
			regs.display_disable = (data & 0x80) != 0;
			regs.display_brightness = (int) (data & 0x0f);
			return;
		}
		case 0x2101:
		{ // OBSEL
			sprite.regs.base_size = (int) ((data >> 5) & 7);
			sprite.regs.nameselect = (int) ((data >> 3) & 3);
			sprite.regs.tiledata_addr = (int) ((data & 3) << 14);
			sprite.list_valid = false;
			return;
		}
		case 0x2102:
		{ // OAMADDL
			regs.oam_baseaddr = ((regs.oam_baseaddr & 0x0100) | (data << 0));
			sprite.address_reset();
			return;
		}
		case 0x2103:
		{ // OAMADDH
			regs.oam_priority = (data & 0x80) != 0;
			regs.oam_baseaddr = (((data & 1) << 8) | (regs.oam_baseaddr & 0x00ff));
			sprite.address_reset();
			return;
		}
		case 0x2104:
		{ // OAMDATA
			if ((regs.oam_addr & 1) == 0)
			{
				regs.oam_latchdata = data;
			}
			if ((regs.oam_addr & 0x0200) != 0)
			{
				oam_write(regs.oam_addr, data);
			}
			else if ((regs.oam_addr & 1) == 1)
			{
				oam_write((int) ((regs.oam_addr & ~1) + 0), regs.oam_latchdata);
				oam_write((int) ((regs.oam_addr & ~1) + 1), data);
			}
			regs.oam_addr = ((regs.oam_addr + 1) & 0x03ff);
			sprite.set_first();
			return;
		}
		case 0x2105:
		{ // BGMODE
			bg4.regs.tile_size = (data & 0x80) != 0;
			bg3.regs.tile_size = (data & 0x40) != 0;
			bg2.regs.tile_size = (data & 0x20) != 0;
			bg1.regs.tile_size = (data & 0x10) != 0;
			regs.bg3_priority = (data & 0x08) != 0;
			regs.bgmode = (int) (data & 0x07);
			mmio_update_video_mode();
			return;
		}
		case 0x2106:
		{ // MOSAIC
			int mosaic_size = (int) ((data >> 4) & 15);
			bg4.regs.mosaic = ((data & 0x08) != 0 ? mosaic_size : 0);
			bg3.regs.mosaic = ((data & 0x04) != 0 ? mosaic_size : 0);
			bg2.regs.mosaic = ((data & 0x02) != 0 ? mosaic_size : 0);
			bg1.regs.mosaic = ((data & 0x01) != 0 ? mosaic_size : 0);
			return;
		}
		case 0x2107:
		{ // BG1SC
			bg1.regs.screen_addr = (int) ((data & 0x7c) << 9);
			bg1.regs.screen_size = (int) (data & 3);
			return;
		}
		case 0x2108:
		{ // BG2SC
			bg2.regs.screen_addr = (int) ((data & 0x7c) << 9);
			bg2.regs.screen_size = (int) (data & 3);
			return;
		}
		case 0x2109:
		{ // BG3SC
			bg3.regs.screen_addr = (int) ((data & 0x7c) << 9);
			bg3.regs.screen_size = (int) (data & 3);
			return;
		}
		case 0x210a:
		{ // BG4SC
			bg4.regs.screen_addr = (int) ((data & 0x7c) << 9);
			bg4.regs.screen_size = (int) (data & 3);
			return;
		}
		case 0x210b:
		{ // BG12NBA
			bg1.regs.tiledata_addr = (int) ((data & 0x07) << 13);
			bg2.regs.tiledata_addr = (int) ((data & 0x70) << 9);
			return;
		}
		case 0x210c:
		{ // BG34NBA
			bg3.regs.tiledata_addr = (int) ((data & 0x07) << 13);
			bg4.regs.tiledata_addr = (int) ((data & 0x70) << 9);
			return;
		}
		case 0x210d:
		{ // BG1HOFS
			regs.mode7_hoffset = ((data << 8) | regs.mode7_latchdata) & 0xFFFF;
			regs.mode7_latchdata = data;

			bg1.regs.hoffset = (int) (data << 8) | (int) (regs.bgofs_latchdata & ~7) | ((bg1.regs.hoffset >> 8) & 7);
			regs.bgofs_latchdata = data;
			return;
		}
		case 0x210e:
		{ // BG1VOFS
			regs.mode7_voffset = ((data << 8) | regs.mode7_latchdata) & 0xFFFF;
			regs.mode7_latchdata = data;

			bg1.regs.voffset = ((data << 8) | regs.bgofs_latchdata);
			regs.bgofs_latchdata = data;
			return;
		}
		case 0x210f:
		{ // BG2HOFS
			bg2.regs.hoffset = (int) (data << 8) | (int) (regs.bgofs_latchdata & ~7) | ((bg2.regs.hoffset >> 8) & 7);
			regs.bgofs_latchdata = data;
			return;
		}
		case 0x2110:
		{ // BG2VOFS
			bg2.regs.voffset = (int) ((data << 8) | regs.bgofs_latchdata);
			regs.bgofs_latchdata = data;
			return;
		}
		case 0x2111:
		{ // BG3HOFS
			bg3.regs.hoffset = (int) (data << 8) | (int) (regs.bgofs_latchdata & ~7) | ((bg3.regs.hoffset >> 8) & 7);
			regs.bgofs_latchdata = data;
			return;
		}
		case 0x2112:
		{ // BG3VOFS
			bg3.regs.voffset = (int) ((data << 8) | regs.bgofs_latchdata);
			regs.bgofs_latchdata = data;
			return;
		}
		case 0x2113:
		{ // BG4HOFS
			bg4.regs.hoffset = (int) (data << 8) | (int) (regs.bgofs_latchdata & ~7) | ((bg4.regs.hoffset >> 8) & 7);
			regs.bgofs_latchdata = data;
			return;
		}
		case 0x2114:
		{ // BG4VOFS
			bg4.regs.voffset = (int) ((data << 8) | regs.bgofs_latchdata);
			regs.bgofs_latchdata = data;
			return;
		}
		case 0x2115:
		{ // VMAIN
			regs.vram_incmode = (data & 0x80) != 0;
			regs.vram_mapping = (int) ((data >> 2) & 3);
			switch (data & 3)
			{
			case 0:
				regs.vram_incsize = 1;
				break;
			case 1:
				regs.vram_incsize = 32;
				break;
			case 2:
				regs.vram_incsize = 128;
				break;
			case 3:
				regs.vram_incsize = 128;
				break;
			}
			return;
		}
		case 0x2116:
		{ // VMADDL
			regs.vram_addr = ((regs.vram_addr & 0xff00) | (data << 0));
			addr = get_vram_addr();
			regs.vram_readbuffer = (vram_read(addr + 0) << 0);
			regs.vram_readbuffer |= (vram_read(addr + 1) << 8);
			return;
		}
		case 0x2117:
		{ // VMADDH
			regs.vram_addr = ((data << 8) | (regs.vram_addr & 0x00ff));
			addr = get_vram_addr();
			regs.vram_readbuffer = (vram_read(addr + 0) << 0);
			regs.vram_readbuffer |= (vram_read(addr + 1) << 8);
			return;
		}
		case 0x2118:
		{ // VMDATAL
			vram_write(get_vram_addr() + 0, data);
			if (regs.vram_incmode == false)
			{
				regs.vram_addr = (regs.vram_addr + regs.vram_incsize) & 0xFFFF;
			}
			return;
		}
		case 0x2119:
		{ // VMDATAH
			vram_write(get_vram_addr() + 1, data);
			if (regs.vram_incmode == true)
			{
				regs.vram_addr = (regs.vram_addr + regs.vram_incsize) & 0xFFFF;
			}
			return;
		}
		case 0x211a:
		{ // M7SEL
			regs.mode7_repeat = (int) ((data >> 6) & 3);
			regs.mode7_vflip = (data & 0x02) != 0;
			regs.mode7_hflip = (data & 0x01) != 0;
			return;
		}
		case 0x211b:
		{ // M7A
			regs.m7a = ((data << 8) | regs.mode7_latchdata) & 0xFFFF;
			regs.mode7_latchdata = data;
			return;
		}
		case 0x211c:
		{ // M7B
			regs.m7b = ((data << 8) | regs.mode7_latchdata) & 0xFFFF;
			regs.mode7_latchdata = data;
			return;
		}
		case 0x211d:
		{ // M7C
			regs.m7c = ((data << 8) | regs.mode7_latchdata) & 0xFFFF;
			regs.mode7_latchdata = data;
			return;
		}
		case 0x211e:
		{ // M7D
			regs.m7d = ((data << 8) | regs.mode7_latchdata) & 0xFFFF;
			regs.mode7_latchdata = data;
			return;
		}
		case 0x211f:
		{ // M7X
			regs.m7x = ((data << 8) | regs.mode7_latchdata) & 0xFFFF;
			regs.mode7_latchdata = data;
			return;
		}
		case 0x2120:
		{ // M7Y
			regs.m7y = ((data << 8) | regs.mode7_latchdata) & 0xFFFF;
			regs.mode7_latchdata = data;
			return;
		}
		case 0x2121:
		{ // CGADD
			regs.cgram_addr = (data << 1);
			return;
		}
		case 0x2122:
		{ // CGDATA
			if ((regs.cgram_addr & 1) == 0)
			{
				regs.cgram_latchdata = data;
			}
			else
			{
				cgram_write((int) ((regs.cgram_addr & ~1) + 0), regs.cgram_latchdata);
				cgram_write((int) ((regs.cgram_addr & ~1) + 1), (data & 0x7f));
			}
			regs.cgram_addr = ((regs.cgram_addr + 1) & 0x01ff);
			return;
		}
		case 0x2123:
		{ // W12SEL
			bg2.window.two_enable = (data & 0x80) != 0;
			bg2.window.two_invert = (data & 0x40) != 0;
			bg2.window.one_enable = (data & 0x20) != 0;
			bg2.window.one_invert = (data & 0x10) != 0;
			bg1.window.two_enable = (data & 0x08) != 0;
			bg1.window.two_invert = (data & 0x04) != 0;
			bg1.window.one_enable = (data & 0x02) != 0;
			bg1.window.one_invert = (data & 0x01) != 0;
			return;
		}
		case 0x2124:
		{ // W34SEL
			bg4.window.two_enable = (data & 0x80) != 0;
			bg4.window.two_invert = (data & 0x40) != 0;
			bg4.window.one_enable = (data & 0x20) != 0;
			bg4.window.one_invert = (data & 0x10) != 0;
			bg3.window.two_enable = (data & 0x08) != 0;
			bg3.window.two_invert = (data & 0x04) != 0;
			bg3.window.one_enable = (data & 0x02) != 0;
			bg3.window.one_invert = (data & 0x01) != 0;
			return;
		}
		case 0x2125:
		{ // WOBJSEL
			screen.window.two_enable = (data & 0x80) != 0;
			screen.window.two_invert = (data & 0x40) != 0;
			screen.window.one_enable = (data & 0x20) != 0;
			screen.window.one_invert = (data & 0x10) != 0;
			sprite.window.two_enable = (data & 0x08) != 0;
			sprite.window.two_invert = (data & 0x04) != 0;
			sprite.window.one_enable = (data & 0x02) != 0;
			sprite.window.one_invert = (data & 0x01) != 0;
			return;
		}
		case 0x2126:
		{ // WH0
			regs.window_one_left = data;
			return;
		}
		case 0x2127:
		{ // WH1
			regs.window_one_right = data;
			return;
		}
		case 0x2128:
		{ // WH2
			regs.window_two_left = data;
			return;
		}
		case 0x2129:
		{ // WH3
			regs.window_two_right = data;
			return;
		}
		case 0x212a:
		{ // WBGLOG
			bg4.window.mask = (int) ((data >> 6) & 3);
			bg3.window.mask = (int) ((data >> 4) & 3);
			bg2.window.mask = (int) ((data >> 2) & 3);
			bg1.window.mask = (int) ((data >> 0) & 3);
			return;
		}
		case 0x212b:
		{ // WOBJLOG
			screen.window.mask = (int) ((data >> 2) & 3);
			sprite.window.mask = (int) ((data >> 0) & 3);
			return;
		}
		case 0x212c:
		{ // TM
			sprite.regs.main_enable = (data & 0x10) != 0;
			bg4.regs.main_enable = (data & 0x08) != 0;
			bg3.regs.main_enable = (data & 0x04) != 0;
			bg2.regs.main_enable = (data & 0x02) != 0;
			bg1.regs.main_enable = (data & 0x01) != 0;
			return;
		}
		case 0x212d:
		{ // TS
			sprite.regs.sub_enable = (data & 0x10) != 0;
			bg4.regs.sub_enable = (data & 0x08) != 0;
			bg3.regs.sub_enable = (data & 0x04) != 0;
			bg2.regs.sub_enable = (data & 0x02) != 0;
			bg1.regs.sub_enable = (data & 0x01) != 0;
			return;
		}
		case 0x212e:
		{ // TMW
			sprite.window.main_enable = (data & 0x10) != 0;
			bg4.window.main_enable = (data & 0x08) != 0;
			bg3.window.main_enable = (data & 0x04) != 0;
			bg2.window.main_enable = (data & 0x02) != 0;
			bg1.window.main_enable = (data & 0x01) != 0;
			return;
		}
		case 0x212f:
		{ // TSW
			sprite.window.sub_enable = (data & 0x10) != 0;
			bg4.window.sub_enable = (data & 0x08) != 0;
			bg3.window.sub_enable = (data & 0x04) != 0;
			bg2.window.sub_enable = (data & 0x02) != 0;
			bg1.window.sub_enable = (data & 0x01) != 0;
			return;
		}
		case 0x2130:
		{ // CGWSEL
			screen.window.main_mask = (int) ((data >> 6) & 3);
			screen.window.sub_mask = (int) ((data >> 4) & 3);
			screen.regs.addsub_mode = (data & 0x02) != 0;
			screen.regs.direct_color = (data & 0x01) != 0;
			return;
		}
		case 0x2131:
		{ // CGADDSUB
			screen.regs.color_mode = (data & 0x80) != 0;
			screen.regs.color_halve = (data & 0x40) != 0;
			screen.regs.color_enable[6] = (data & 0x20) != 0;
			screen.regs.color_enable[5] = (data & 0x10) != 0;
			screen.regs.color_enable[4] = (data & 0x10) != 0;
			screen.regs.color_enable[3] = (data & 0x08) != 0;
			screen.regs.color_enable[2] = (data & 0x04) != 0;
			screen.regs.color_enable[1] = (data & 0x02) != 0;
			screen.regs.color_enable[0] = (data & 0x01) != 0;
			return;
		}
		case 0x2132:
		{ // COLDATA
			if ((data & 0x80) != 0)
			{
				screen.regs.color_b = (int) (data & 0x1f);
			}
			if ((data & 0x40) != 0)
			{
				screen.regs.color_g = (int) (data & 0x1f);
			}
			if ((data & 0x20) != 0)
			{
				screen.regs.color_r = (int) (data & 0x1f);
			}
			screen.regs.color = (screen.regs.color_b << 10) | (screen.regs.color_g << 5) | (screen.regs.color_r << 0);
			return;
		}
		case 0x2133:
		{ // SETINI
			regs.mode7_extbg = (data & 0x40) != 0;
			regs.pseudo_hires = (data & 0x08) != 0;
			regs.overscan = (data & 0x04) != 0;
			sprite.regs.interlace = (data & 0x02) != 0;
			regs.interlace = (data & 0x01) != 0;
			mmio_update_video_mode();
			sprite.list_valid = false;
			return;
		}
		}
	}

	private void mmio_reset()
	{ // internal
		regs.ppu1_mdr = 0;
		regs.ppu2_mdr = 0;

		regs.vram_readbuffer = 0;
		regs.oam_latchdata = 0;
		regs.cgram_latchdata = 0;
		regs.bgofs_latchdata = 0;
		regs.mode7_latchdata = 0;

		regs.counters_latched = false;
		regs.latch_hcounter = false;
		regs.latch_vcounter = false;

		sprite.regs.first_sprite = 0;
		sprite.list_valid = false;

		// $2100
		regs.display_disable = true;
		regs.display_brightness = 0;

		// $2101
		sprite.regs.base_size = 0;
		sprite.regs.nameselect = 0;
		sprite.regs.tiledata_addr = 0;

		// $2102-$2103
		regs.oam_baseaddr = 0;
		regs.oam_addr = 0;
		regs.oam_priority = false;

		// $2105
		bg4.regs.tile_size = false;
		bg3.regs.tile_size = false;
		bg2.regs.tile_size = false;
		bg1.regs.tile_size = false;
		regs.bg3_priority = false;
		regs.bgmode = 0;

		// $2106
		bg4.regs.mosaic = 0;
		bg3.regs.mosaic = 0;
		bg2.regs.mosaic = 0;
		bg1.regs.mosaic = 0;

		// $2107-$210a
		bg1.regs.screen_addr = 0;
		bg1.regs.screen_size = 0;
		bg2.regs.screen_addr = 0;
		bg2.regs.screen_size = 0;
		bg3.regs.screen_addr = 0;
		bg3.regs.screen_size = 0;
		bg4.regs.screen_addr = 0;
		bg4.regs.screen_size = 0;

		// $210b-$210c
		bg1.regs.tiledata_addr = 0;
		bg2.regs.tiledata_addr = 0;
		bg3.regs.tiledata_addr = 0;
		bg4.regs.tiledata_addr = 0;

		// $210d-$2114
		regs.mode7_hoffset = 0;
		regs.mode7_voffset = 0;
		bg1.regs.hoffset = 0;
		bg1.regs.voffset = 0;
		bg2.regs.hoffset = 0;
		bg2.regs.voffset = 0;
		bg3.regs.hoffset = 0;
		bg3.regs.voffset = 0;
		bg4.regs.hoffset = 0;
		bg4.regs.voffset = 0;

		// $2115
		regs.vram_incmode = false;
		regs.vram_mapping = 0;
		regs.vram_incsize = 1;

		// $2116-$2117
		regs.vram_addr = 0;

		// $211a
		regs.mode7_repeat = 0;
		regs.mode7_vflip = false;
		regs.mode7_hflip = false;

		// $211b-$2120
		regs.m7a = 0;
		regs.m7b = 0;
		regs.m7c = 0;
		regs.m7d = 0;
		regs.m7x = 0;
		regs.m7y = 0;

		// $2121
		regs.cgram_addr = 0;

		// $2123-$2125
		bg1.window.one_enable = false;
		bg1.window.one_invert = false;
		bg1.window.two_enable = false;
		bg1.window.two_invert = false;

		bg2.window.one_enable = false;
		bg2.window.one_invert = false;
		bg2.window.two_enable = false;
		bg2.window.two_invert = false;

		bg3.window.one_enable = false;
		bg3.window.one_invert = false;
		bg3.window.two_enable = false;
		bg3.window.two_invert = false;

		bg4.window.one_enable = false;
		bg4.window.one_invert = false;
		bg4.window.two_enable = false;
		bg4.window.two_invert = false;

		sprite.window.one_enable = false;
		sprite.window.one_invert = false;
		sprite.window.two_enable = false;
		sprite.window.two_invert = false;

		screen.window.one_enable = false;
		screen.window.one_invert = false;
		screen.window.two_enable = false;
		screen.window.two_invert = false;

		// $2126-$2129
		regs.window_one_left = 0;
		regs.window_one_right = 0;
		regs.window_two_left = 0;
		regs.window_two_right = 0;

		// $212a-$212b
		bg1.window.mask = 0;
		bg2.window.mask = 0;
		bg3.window.mask = 0;
		bg4.window.mask = 0;
		sprite.window.mask = 0;
		screen.window.mask = 0;

		// $212c
		bg1.regs.main_enable = false;
		bg2.regs.main_enable = false;
		bg3.regs.main_enable = false;
		bg4.regs.main_enable = false;
		sprite.regs.main_enable = false;

		// $212d
		bg1.regs.sub_enable = false;
		bg2.regs.sub_enable = false;
		bg3.regs.sub_enable = false;
		bg4.regs.sub_enable = false;
		sprite.regs.sub_enable = false;

		// $212e
		bg1.window.main_enable = false;
		bg2.window.main_enable = false;
		bg3.window.main_enable = false;
		bg4.window.main_enable = false;
		sprite.window.main_enable = false;

		// $212f
		bg1.window.sub_enable = false;
		bg2.window.sub_enable = false;
		bg3.window.sub_enable = false;
		bg4.window.sub_enable = false;
		sprite.window.sub_enable = false;

		// $2130
		screen.window.main_mask = 0;
		screen.window.sub_mask = 0;
		screen.regs.addsub_mode = false;
		screen.regs.direct_color = false;

		// $2131
		screen.regs.color_mode = false;
		screen.regs.color_halve = false;
		screen.regs.color_enable[6] = false;
		screen.regs.color_enable[5] = false;
		screen.regs.color_enable[4] = false;
		screen.regs.color_enable[3] = false;
		screen.regs.color_enable[2] = false;
		screen.regs.color_enable[1] = false;
		screen.regs.color_enable[0] = false;

		// $2132
		screen.regs.color_b = 0;
		screen.regs.color_g = 0;
		screen.regs.color_r = 0;
		screen.regs.color = 0;

		// $2133
		regs.mode7_extbg = false;
		regs.pseudo_hires = false;
		regs.overscan = false;
		sprite.regs.interlace = false;
		regs.interlace = false;

		// $213e
		sprite.regs.time_over = false;
		sprite.regs.range_over = false;

		mmio_update_video_mode();
	}

	
	private final void add_clocks(int clocks)
	{
		ppuCounter.tick(clocks);
		step(clocks);
		synchronize_cpu();
	}

	private final void render_scanline()
	{
		if(!enablerender)
			return;
		// if ((display.framecounter != 0))
		// {
		// return; //skip this frame?
		// }
		bg1.scanline();
		bg2.scanline();
		bg3.scanline();
		bg4.scanline();
		if (regs.display_disable)
		{
			screen.render_black();
			return;
		}
		screen.scanline();
		bg1.render(); //MAP TILES
		bg2.render(); //SROLLING BACKGROUND
		bg3.render(); //ATMOSPHERE
		bg4.render(); //UNKNOWN
		sprite.render();
		screen.render();
	}

	private transient HVCounter cpuCounter;
	transient HVCounter ppuCounter;

	@Override
	public void readDMA(int address, ByteBuffer data, int offset, int length)
	{
		System.arraycopy(output.array(), 0, data.array(), offset, length);
	}

	@Override
	public void writeDMA(int address, ByteBuffer data, int offset, int length)
	{
	}

	@Override
	public Object readConfig(String key)
	{
		if (key.equals("enablerender")) return enablerender;
		if (key.equals("accuracy")) return false;
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("enablerender")) enablerender = (Boolean) value;
		if (key.equals("region"))
		{
			region = value.toString().equals("ntsc") ? NTSC : PAL;
			cpuCounter.region = region;
			ppuCounter.region = region;
		}
		
		else if(key.equals("save")) {
			ObjectOutputStream out = (ObjectOutputStream)value;
			
			try
			{
				out.writeObject(cache);
				out.writeObject(bg1);
				out.writeObject(bg2);
				out.writeObject(bg3);
				out.writeObject(bg4);
				out.writeObject(sprite);
				out.writeObject(screen);
				//out.writeObject(display);
				out.writeObject(regs);
				
				out.writeInt(vram.length);
				for(int i=0; i<vram.length; i++)
				{
					out.writeInt(vram[i]);
				}
		        
		        
		        out.writeInt(oam.length);
		        for(int i=0; i<oam.length; i++)
				{
					out.writeInt(oam[i]);
				}
		        
		        out.writeInt(cgram.length);
		        for(int i=0; i<cgram.length; i++)
				{
					out.writeInt(cgram[i]);
				}
		        /*
		        byte[] buff = output.array();
		        out.writeInt(buff.length);
		        out.writeInt(output.position());
		        out.writeInt(output.limit());
		        for(int i=0; i<buff.length; i++)
		        {
		        	out.writeByte(buff[i]);
		        }
			       */
		        //out.writeObject(cpuCounter.status);
		        //out.writeInt(cpuCounter.region);
		        
		       // out.writeObject(ppuCounter.status);
		       // out.writeInt(ppuCounter.region);
		     
			}
			catch(Exception e) { e.printStackTrace(); }
			
			
			
		}
		else if(key.equals("load")) {
			ObjectInputStream in = (ObjectInputStream)value;
			try
			{
				cache = (Cache)in.readObject();
		        bg1 = (Background)in.readObject();
		        bg2 = (Background)in.readObject();
		        bg3 = (Background)in.readObject();
		        bg4 = (Background)in.readObject();
		        sprite = (Sprite)in.readObject();
		        screen = (Screen)in.readObject();
		        //display = (Display)in.readObject();
		        regs = (Regs)in.readObject();
		        
		        //screen.build_lighttable();
		        
		        int len = in.readInt();
		        for(int i=0; i<len; i++)
		        {
		        	vram[i] = in.readInt();
		        }
		        
		        len = in.readInt();
		        for(int i=0; i<len; i++)
		        {
		        	oam[i] = in.readInt();
		        }
		        
		        len = in.readInt();
		        for(int i=0; i<len; i++)
		        {
		        	cgram[i] = in.readInt();
		        }
		        
		        /*
		        output = ByteBuffer.allocate(1024 * 1024 * 2);
				
		        len = in.readInt();
		        int pos = in.readInt();
		        int limit = in.readInt();
		        byte[] buff = new byte[len];
		        for(int i=0; i<len; i++)
		        {
		        	buff[i] = in.readByte();
		        }
		        
		        output.put(buff);
		        output.position(pos);
		        output.limit(limit);
		        */
		        
		        
		        //cpuCounter.status = (HVCounter.Status)in.readObject();
		        //cpuCounter.region = in.readInt();
		        
		        //ppuCounter.status = (HVCounter.Status)in.readObject();
		        //ppuCounter.region = in.readInt();
		        
			}
			catch(Exception e) { e.printStackTrace(); }
			
		}
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		ppu = null;
		bus = null;
		cache = null;
		bg1 = null;
		bg2 = null;
		bg3 = null;
		bg4 = null;
		sprite = null;
		screen = null;
		display = null;
		regs = null;
		cpuCounter = null;
		ppuCounter = null;
	}
}
