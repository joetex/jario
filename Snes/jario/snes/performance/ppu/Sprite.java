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
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Sprite implements java.io.Serializable
{
	//adding helper method for serialization to save/initialize super class state
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException{
        ois.defaultReadObject();
         
        int len=ois.readInt();
        for(int i=0; i<len; i++)
        {
        	list[i] = (List)ois.readObject();
        }
        
        len=ois.readInt();
        for(int i=0; i<len; i++)
        {
        	itemlist[i] = ois.readInt();
        }
        
        len=ois.readInt();
        for(int i=0; i<len; i++)
        {
        	tilelist[i] = (TileList)ois.readObject();
        }
        
        len=ois.readInt();
        for(int i=0; i<len; i++)
        {
        	priority_table[i] = ois.readInt();
        }
    }
     
    private void writeObject(ObjectOutputStream oos) throws IOException{
        oos.defaultWriteObject();
         
        int len=list.length;
        oos.writeInt(len);
        for(int i=0; i<len; i++)
        {
        	oos.writeObject(list[i]);
        }
        
        len=itemlist.length;
        oos.writeInt(len);
        for(int i=0; i<len; i++)
        {
        	oos.writeInt(itemlist[i]);
        }
        
        
        len=tilelist.length;
        oos.writeInt(len);
        for(int i=0; i<len; i++)
        {
        	oos.writeObject(tilelist[i]);
        }
        
        len=priority_table.length;
        oos.writeInt(len);
        for(int i=0; i<len; i++)
        {
        	oos.writeInt(priority_table[i]);
        }
    }
    
	class Regs implements java.io.Serializable
	{
		public int priority0;
		public int priority1;
		public int priority2;
		public int priority3;
		public int base_size;
		public int nameselect;
		public int tiledata_addr;
		public int first_sprite;
		public boolean main_enable;
		public boolean sub_enable;
		public boolean interlace;
		public boolean time_over;
		public boolean range_over;
	}

	class Output implements java.io.Serializable
	{
		public int[] palette = new int[256];
		public int[] priority = new int[256];
		
		//adding helper method for serialization to save/initialize super class state
	    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException{
	        ois.defaultReadObject();
	         
	        int len=ois.readInt();
	        for(int i=0; i<len; i++)
	        {
	        	palette[i] = ois.readInt();
	        }
	        
	        len=ois.readInt();
	        for(int i=0; i<len; i++)
	        {
	        	priority[i] = ois.readInt();
	        }
	        
	    }
	     
	    private void writeObject(ObjectOutputStream oos) throws IOException{
	        oos.defaultWriteObject();
	         
	        int len=palette.length;
	        oos.writeInt(len);
	        for(int i=0; i<len; i++)
	        {
	        	oos.writeInt(palette[i]);
	        }
	        
	        len=priority.length;
	        oos.writeInt(len);
	        for(int i=0; i<len; i++)
	        {
	        	oos.writeInt(priority[i]);
	        }
	        
	    }
	}

	public boolean priority0_enable;
	public boolean priority1_enable;
	public boolean priority2_enable;
	public boolean priority3_enable;

	public Regs regs = new Regs();
	public List[] list = new List[128];
	public boolean list_valid;

	public int[] itemlist = new int[32];
	public TileList[] tilelist = new TileList[34];
	public Output output = new Output();
	public LayerWindow window = new LayerWindow();

	public void frame()
	{
		regs.time_over = false;
		regs.range_over = false;
	}

	public void update_list(int addr, int data)
	{
		if (addr < 0x0200)
		{
			int i = addr >> 2;
			switch (addr & 3)
			{
			case 0:
				list[i].x = (list[i].x & 0x0100) | data;
				break;
			case 1:
				list[i].y = (data + 1) & 0xff;
				break;
			case 2:
				list[i].character = data;
				break;
			case 3:
				list[i].vflip = (data & 0x80) != 0;
				list[i].hflip = (data & 0x40) != 0;
				list[i].priority = ((data >> 4) & 3);
				list[i].palette = ((data >> 1) & 7);
				list[i].use_nameselect = (data & 0x01) != 0;
				break;
			}
		}
		else
		{
			int i = (addr & 0x1f) << 2;
			list[i + 0].x = ((data & 0x01) << 8) | (list[i + 0].x & 0xff);
			list[i + 0].size = (data & 0x02) != 0;
			list[i + 1].x = ((data & 0x04) << 6) | (list[i + 1].x & 0xff);
			list[i + 1].size = (data & 0x08) != 0;
			list[i + 2].x = ((data & 0x10) << 4) | (list[i + 2].x & 0xff);
			list[i + 2].size = (data & 0x20) != 0;
			list[i + 3].x = ((data & 0x40) << 2) | (list[i + 3].x & 0xff);
			list[i + 3].size = (data & 0x80) != 0;
			list_valid = false;
		}
	}

	public void address_reset()
	{
		PPU.ppu.regs.oam_addr = (PPU.ppu.regs.oam_baseaddr << 1) & 0xFFFF;
		set_first();
	}

	public void set_first()
	{
		regs.first_sprite = (PPU.ppu.regs.oam_priority == false ? 0 : (PPU.ppu.regs.oam_addr >> 2) & 127);
	}

	public boolean on_scanline(int sprite)
	{
		List s = list[sprite];
		if (s.x > 256 && (s.x + s.width - 1) < 512) { return false; }
		int height = (regs.interlace == false ? s.height : s.height >> 1);
		if (PPU.ppu.ppuCounter.vcounter() >= s.y && PPU.ppu.ppuCounter.vcounter() < (s.y + height)) { return true; }
		if ((s.y + height) >= 256 && PPU.ppu.ppuCounter.vcounter() < ((s.y + height) & 255)) { return true; }
		return false;
	}

	private static int[] width1 = { 8, 8, 8, 16, 16, 32, 16, 16 };
	private static int[] height1 = { 8, 8, 8, 16, 16, 32, 32, 32 };
	private static int[] width2 = { 16, 32, 64, 32, 64, 64, 32, 32 };
	private static int[] height2 = { 16, 32, 64, 32, 64, 64, 64, 32 };

	private int[] priority_table = new int[4];

	public void render()
	{
		if (list_valid == false)
		{
			list_valid = true;
			for (int i = 0; i < 128; i++)
			{
				if (list[i].size == false)
				{
					list[i].width = width1[regs.base_size];
					list[i].height = height1[regs.base_size];
				}
				else
				{
					list[i].width = width2[regs.base_size];
					list[i].height = height2[regs.base_size];
					if (regs.interlace && regs.base_size >= 6)
					{
						list[i].height = 16;
					}
				}
			}
		}

		int itemcount = 0;
		int tilecount = 0;
		Arrays.fill(output.priority, 0xff);
		Arrays.fill(itemlist, 0xff);
		for (int i = 0; i < 34; i++)
		{
			tilelist[i].tile = 0xffff;
		}

		for (int i = 0; i < 128; i++)
		{
			int s = (regs.first_sprite + i) & 127;
			if (on_scanline(s) == false)
			{
				continue;
			}
			if (itemcount++ >= 32)
			{
				break;
			}
			itemlist[itemcount - 1] = s;
		}

		for (int i = 31; i >= 0; i--)
		{
			if (itemlist[i] == 0xff)
			{
				continue;
			}
			List s = list[itemlist[i]];
			int tile_width = s.width >> 3;
			int x = s.x;
			int y = ((PPU.ppu.ppuCounter.vcounter() - s.y) & 0xff);
			if (regs.interlace)
			{
				y <<= 1;
			}

			if (s.vflip)
			{
				if (s.width == s.height)
				{
					y = ((s.height - 1) - y);
				}
				else
				{
					y = ((y < s.width) ? ((s.width - 1) - y) : (s.width + ((s.width - 1) - (y - s.width))));
				}
			}

			if (regs.interlace)
			{
				y = (s.vflip == false) ? (y + (PPU.ppu.ppuCounter.field() ? 1 : 0)) : (y - (PPU.ppu.ppuCounter.field() ? 1 : 0));
			}

			x &= 511;
			y &= 255;

			int tdaddr = (regs.tiledata_addr) & 0xFFFF;
			int chrx = ((s.character >> 0) & 15);
			int chry = ((s.character >> 4) & 15);
			if (s.use_nameselect)
			{
				tdaddr = (tdaddr + ((256 * 32) + (regs.nameselect << 13))) & 0xFFFF;
			}
			chry = (chry + ((y >> 3))) & 0xFFFF;
			chry &= 15;
			chry <<= 4;

			for (int tx = 0; tx < tile_width; tx++)
			{
				int sx = ((x + (tx << 3)) & 511);
				if (x != 256 && sx >= 256 && (sx + 7) < 512)
				{
					continue;
				}
				if (tilecount++ >= 34)
				{
					break;
				}

				int n = tilecount - 1;
				tilelist[n].x = sx;
				tilelist[n].y = y;
				tilelist[n].priority = s.priority;
				tilelist[n].palette = 128 + (s.palette << 4);
				tilelist[n].hflip = s.hflip;

				int mx = (s.hflip == false) ? tx : ((tile_width - 1) - tx);
				int pos = tdaddr + ((chry + ((chrx + mx) & 15)) << 5);
				tilelist[n].tile = (pos >> 5) & 0x07ff;
			}
		}

		regs.time_over |= (tilecount > 34);
		regs.range_over |= (itemcount > 32);

		if (regs.main_enable == false && regs.sub_enable == false) { return; }

		for (int i = 0; i < 34; i++)
		{
			if (tilelist[i].tile == 0xffff)
			{
				continue;
			}

			TileList t = tilelist[i];
			ByteBuffer tiledata = PPU.ppu.cache.tile_4bpp(t.tile);
			int tiledata_offset = tiledata.position() + ((t.y & 7) << 3);
			int sx = t.x;
			for (int x = 0; x < 8; x++)
			{
				sx &= 511;
				if (sx < 256)
				{
					int color = tiledata.array()[tiledata_offset + (t.hflip == false ? x : 7 - x)] & 0xFF;
					if ((color) != 0)
					{
						color += t.palette;
						output.palette[sx] = color & 0xFF;
						output.priority[sx] = t.priority;
					}
				}
				sx++;
			}
		}

		if (regs.main_enable)
		{
			window.render(false);
		}
		if (regs.sub_enable)
		{
			window.render(true);
		}

		priority_table[0] = (priority0_enable ? regs.priority0 : 0);
		priority_table[1] = (priority1_enable ? regs.priority1 : 0);
		priority_table[2] = (priority2_enable ? regs.priority2 : 0);
		priority_table[3] = (priority3_enable ? regs.priority3 : 0);
		if (priority_table[0] + priority_table[1] + priority_table[2] + priority_table[3] == 0) { return; }

		for (int x = 0; x < 256; x++)
		{
			if (output.priority[x] == 0xff)
			{
				continue;
			}
			int priority = priority_table[output.priority[x]];
			int palette = output.palette[x];
			int color = PPU.ppu.screen.get_palette(output.palette[x]);
			if (regs.main_enable && (window.main[x] == 0))
			{
				PPU.ppu.screen.output.plot_main(x, color, priority, 4 + ((palette < 192) ? 1 : 0));
			}
			if (regs.sub_enable && (window.sub[x] == 0))
			{
				PPU.ppu.screen.output.plot_sub(x, color, priority, 4 + ((palette < 192) ? 1 : 0));
			}
		}
	}

	public Sprite(PPU self)
	{
		//this.self = self;
		priority0_enable = true;
		priority1_enable = true;
		priority2_enable = true;
		priority3_enable = true;

		for (int i = 0; i < list.length; i++)
		{
			list[i] = new List();
		}
		for (int i = 0; i < tilelist.length; i++)
		{
			tilelist[i] = new TileList();
		}
	}

	//public PPU self;
}
