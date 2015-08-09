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

public class Background implements java.io.Serializable
{
	//adding helper method for serialization to save/initialize super class state
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException{
        ois.defaultReadObject();
         
        int len1 = ois.readInt();
        for(int i=0; i<len1; i++)
        {
        	int len2 = ois.readInt();
        	for(int j=0; j<len2; j++)
        	{
        		mosaic_table[i][j] = ois.readInt();
        	}
        }
    }
     
    private void writeObject(ObjectOutputStream oos) throws IOException{
        oos.defaultWriteObject();
         
        int len1 = mosaic_table.length;
        oos.writeInt(len1);
        for(int i=0; i<len1; i++)
        {
        	int len2 = mosaic_table[i].length;
        	oos.writeInt(len2);
        	for(int j=0; j<len2; j++)
        	{
        		oos.writeInt(mosaic_table[i][j]);
        	}
        }
    }
    
	class Regs implements java.io.Serializable
	{
		public int mode;
		public int priority0;
		public int priority1;
		public boolean tile_size;
		public int mosaic;
		public int screen_addr;
		public int screen_size;
		public int tiledata_addr;
		public int hoffset;
		public int voffset;
		public boolean main_enable;
		public boolean sub_enable;
	}

	public static final int ID_BG1 = 0;
	public static final int ID_BG2 = 1;
	public static final int ID_BG3 = 2;
	public static final int ID_BG4 = 3;

	public static final int Mode_BPP2 = 0;
	public static final int Mode_BPP4 = 1;
	public static final int Mode_BPP8 = 2;
	public static final int Mode_Mode7 = 3;
	public static final int Mode_Inactive = 4;

	public enum ScreenSize
	{
		Size32x32, Size32x64, Size64x32, Size64x64
	}

	public enum TileSize
	{
		Size8x8, Size16x16
	}

	public boolean priority0_enable;
	public boolean priority1_enable;

	public Regs regs = new Regs();

	public int[][] mosaic_table;

	public int id;
	public int opt_valid_bit;

	public boolean hires;
	public int width;

	public int tile_width;
	public int tile_height;

	public int mask_x;
	public int mask_y;

	public int scx;
	public int scy;

	public int hscroll;
	public int vscroll;

	public int mosaic_vcounter;
	public int mosaic_voffset;

	public LayerWindow window = new LayerWindow();

	public int get_tile(int hoffset, int voffset)
	{
		int tile_x = (hoffset & mask_x) >> (int) tile_width;
		int tile_y = (voffset & mask_y) >> (int) tile_height;

		int tile_pos = ((tile_y & 0x1f) << 5) + (tile_x & 0x1f);
		if ((tile_y & 0x20) != 0)
		{
			tile_pos += scy;
		}
		if ((tile_x & 0x20) != 0)
		{
			tile_pos += scx;
		}

		int tiledata_addr = (regs.screen_addr + (tile_pos << 1)) & 0xFFFF;
		return (PPU.ppu.vram[tiledata_addr + 0] << 0) + (PPU.ppu.vram[tiledata_addr + 1] << 8);
	}

	public void offset_per_tile(int x, int y, int[] hoffset, int[] voffset)
	{
		int opt_x = (x + (hscroll & 7)), hval, vval = 0;
		if (opt_x >= 8)
		{
			hval = PPU.ppu.bg3.get_tile((int) ((opt_x - 8) + (PPU.ppu.bg3.regs.hoffset & ~7)), PPU.ppu.bg3.regs.voffset + 0);
			if (PPU.ppu.regs.bgmode != 4)
			{
				vval = PPU.ppu.bg3.get_tile((int) ((opt_x - 8) + (PPU.ppu.bg3.regs.hoffset & ~7)), PPU.ppu.bg3.regs.voffset + 8);
			}

			if (PPU.ppu.regs.bgmode == 4)
			{
				if ((hval & opt_valid_bit) != 0)
				{
					if ((hval & 0x8000) == 0)
					{
						hoffset[0] = (int) (opt_x + (hval & ~7));
					}
					else
					{
						voffset[0] = y + hval;
					}
				}
			}
			else
			{
				if ((hval & opt_valid_bit) != 0)
				{
					hoffset[0] = (int) (opt_x + (hval & ~7));
				}
				if ((vval & opt_valid_bit) != 0)
				{
					voffset[0] = y + vval;
				}
			}
		}
	}

	public void scanline()
	{
		if (PPU.ppu.ppuCounter.vcounter() == 1)
		{
			mosaic_vcounter = regs.mosaic + 1;
			mosaic_voffset = 1;
		}
		else if (--mosaic_vcounter == 0)
		{
			mosaic_vcounter = regs.mosaic + 1;
			mosaic_voffset += regs.mosaic + 1;
		}
		if (PPU.ppu.regs.display_disable) { return; }

		hires = (PPU.ppu.regs.bgmode == 5 || PPU.ppu.regs.bgmode == 6);
		width = !hires ? 256 : 512;

		tile_height = regs.tile_size ? 4 : 3;
		tile_width = hires ? 4 : tile_height;

		mask_x = (tile_height == 4 ? width << 1 : width);
		mask_y = mask_x;
		if ((regs.screen_size & 1) != 0)
		{
			mask_x <<= 1;
		}
		if ((regs.screen_size & 2) != 0)
		{
			mask_y <<= 1;
		}
		mask_x--;
		mask_y--;

		scy = ((regs.screen_size & 2) != 0 ? 32 << 5 : 0);
		scx = ((regs.screen_size & 1) != 0 ? 32 << 5 : 0);
		if (regs.screen_size == 3)
		{
			scy <<= 1;
		}
	}

	int[] hoffset = { 0 }, voffset = { 0 };

	public void render()
	{
		if (regs.mode == Mode_Inactive) { return; }
		if (regs.main_enable == false && regs.sub_enable == false) { return; }

		if (regs.main_enable)
		{
			window.render(false);
		}
		if (regs.sub_enable)
		{
			window.render(true);
		}
		if (regs.mode == Mode_Mode7)
		{
			render_mode7();
			return;
		}

		int priority0 = (priority0_enable ? regs.priority0 : 0);
		int priority1 = (priority1_enable ? regs.priority1 : 0);
		if (priority0 + priority1 == 0) { return; }

		int mosaic_hcounter = 1;
		int mosaic_palette = 0;
		int mosaic_priority = 0;
		int mosaic_color = 0;

		int bgpal_index = (PPU.ppu.regs.bgmode == 0 ? id << 5 : 0);
		int pal_size = 2 << regs.mode;
		int tile_mask = 0x0fff >> regs.mode;
		int tiledata_index = regs.tiledata_addr >> (4 + regs.mode);

		hscroll = regs.hoffset;
		vscroll = regs.voffset;

		int y = (regs.mosaic == 0 ? PPU.ppu.ppuCounter.vcounter() : mosaic_voffset);
		if (hires)
		{
			hscroll <<= 1;
			if (PPU.ppu.regs.interlace)
			{
				y = (y << 1) + (PPU.ppu.ppuCounter.field() ? 1 : 0);
			}
		}

		int tile_pri, tile_num;
		int pal_index, pal_num;
		boolean mirror_x, mirror_y;

		boolean is_opt_mode = (PPU.ppu.regs.bgmode == 2 || PPU.ppu.regs.bgmode == 4 || PPU.ppu.regs.bgmode == 6);
		boolean is_direct_color_mode = (PPU.ppu.screen.regs.direct_color == true && id == ID_BG1 && (PPU.ppu.regs.bgmode == 3 || PPU.ppu.regs.bgmode == 4));

		int x = (0 - (hscroll & 7));
		while (x < width)
		{
			hoffset[0] = x + hscroll;
			voffset[0] = y + vscroll;
			if (is_opt_mode)
			{
				offset_per_tile(x, y, hoffset, voffset);
			}
			hoffset[0] &= mask_x;
			voffset[0] &= mask_y;

			tile_num = get_tile(hoffset[0], voffset[0]);
			mirror_y = (tile_num & 0x8000) != 0;
			mirror_x = (tile_num & 0x4000) != 0;
			tile_pri = (tile_num & 0x2000) != 0 ? priority1 : priority0;
			pal_num = (tile_num >> 10) & 7;
			pal_index = (bgpal_index + (pal_num << pal_size)) & 0xff;

			if (tile_width == 4 && ((hoffset[0] & 8) != 0) != mirror_x)
			{
				tile_num += 1;
			}
			if (tile_height == 4 && ((voffset[0] & 8) != 0) != mirror_y)
			{
				tile_num += 16;
			}
			tile_num = ((tile_num & 0x03ff) + tiledata_index) & tile_mask;

			if (mirror_y)
			{
				voffset[0] ^= 7;
			}
			int mirror_xmask = !mirror_x ? 0 : 7;

			ByteBuffer tiledata = PPU.ppu.cache.tile(regs.mode, tile_num);
			int tiledata_offset = tiledata.position() + ((voffset[0] & 7) * 8);

			for (int n = 0; n < 8; n++, x++)
			{
				if ((x & width) != 0)
				{
					continue;
				}
				if (--mosaic_hcounter == 0)
				{
					mosaic_hcounter = regs.mosaic + 1;
					mosaic_palette = tiledata.array()[tiledata_offset + (n ^ mirror_xmask)] & 0xFF;
					mosaic_priority = tile_pri;
					if (is_direct_color_mode)
					{
						mosaic_color = PPU.ppu.screen.get_direct_color(pal_num, mosaic_palette);
					}
					else
					{
						mosaic_color = PPU.ppu.screen.get_palette(pal_index + mosaic_palette);
					}
				}
				if (mosaic_palette == 0)
				{
					continue;
				}

				if (hires == false)
				{
					if (regs.main_enable && (window.main[x] == 0))
					{
						PPU.ppu.screen.output.plot_main(x, mosaic_color, mosaic_priority, id);
					}
					if (regs.sub_enable && (window.sub[x] == 0))
					{
						PPU.ppu.screen.output.plot_sub(x, mosaic_color, mosaic_priority, id);
					}
				}
				else
				{
					int half_x = x >> 1;
					if ((x & 1) != 0)
					{
						if (regs.main_enable && (window.main[half_x] == 0))
						{
							PPU.ppu.screen.output.plot_main(half_x, mosaic_color, mosaic_priority, id);
						}
					}
					else
					{
						if (regs.sub_enable && (window.sub[half_x] == 0))
						{
							PPU.ppu.screen.output.plot_sub(half_x, mosaic_color, mosaic_priority, id);
						}
					}
				}
			}
		}
	}

	private static final int Clip(int x)
	{
		return ((x & 0x2000) != 0) ? (x | ~0x03ff) : (x & 0x03ff);
	}

	public void render_mode7()
	{
		int px, py;
		int tx, ty, tile, palette = 0;

		int a = sclip(16, PPU.ppu.regs.m7a);
		int b = sclip(16, PPU.ppu.regs.m7b);
		int c = sclip(16, PPU.ppu.regs.m7c);
		int d = sclip(16, PPU.ppu.regs.m7d);

		int cx = sclip(13, PPU.ppu.regs.m7x);
		int cy = sclip(13, PPU.ppu.regs.m7y);
		int hofs = sclip(13, PPU.ppu.regs.mode7_hoffset);
		int vofs = sclip(13, PPU.ppu.regs.mode7_voffset);

		int y = (PPU.ppu.regs.mode7_vflip == false ? PPU.ppu.ppuCounter.vcounter() : 255 - PPU.ppu.ppuCounter.vcounter());

		int[] mosaic_x, mosaic_y;
		if (id == ID_BG1)
		{
			mosaic_x = mosaic_table[PPU.ppu.bg1.regs.mosaic];
			mosaic_y = mosaic_table[PPU.ppu.bg1.regs.mosaic];
		}
		else
		{
			mosaic_x = mosaic_table[PPU.ppu.bg2.regs.mosaic];
			mosaic_y = mosaic_table[PPU.ppu.bg1.regs.mosaic];
		}

		int priority0 = (priority0_enable ? regs.priority0 : 0);
		int priority1 = (priority1_enable ? regs.priority1 : 0);
		if (priority0 + priority1 == 0) { return; }

		int psx = ((a * Clip(hofs - cx)) & ~63) + ((b * Clip(vofs - cy)) & ~63) + ((b * mosaic_y[y]) & ~63) + (cx << 8);
		int psy = ((c * Clip(hofs - cx)) & ~63) + ((d * Clip(vofs - cy)) & ~63) + ((d * mosaic_y[y]) & ~63) + (cy << 8);
		for (int x = 0; x < 256; x++)
		{
			px = (psx + (a * mosaic_x[x])) >> 8;
			py = (psy + (c * mosaic_x[x])) >> 8;

			switch (PPU.ppu.regs.mode7_repeat)
			{
			case 0:
			case 1:
			{
				px &= 1023;
				py &= 1023;
				tx = ((px >> 3) & 127);
				ty = ((py >> 3) & 127);
				tile = PPU.ppu.vram[(ty * 128 + tx) << 1];
				palette = PPU.ppu.vram[(((tile << 6) + ((py & 7) << 3) + (px & 7)) << 1) + 1];
				break;
			}

			case 2:
			{
				if (((px | py) & ~1023) != 0)
				{
					palette = 0;
				}
				else
				{
					px &= 1023;
					py &= 1023;
					tx = ((px >> 3) & 127);
					ty = ((py >> 3) & 127);
					tile = PPU.ppu.vram[(ty * 128 + tx) << 1];
					palette = PPU.ppu.vram[(((tile << 6) + ((py & 7) << 3) + (px & 7)) << 1) + 1];
				}
				break;
			}

			case 3:
			{
				if (((px | py) & ~1023) != 0)
				{
					tile = 0;
				}
				else
				{
					px &= 1023;
					py &= 1023;
					tx = ((px >> 3) & 127);
					ty = ((py >> 3) & 127);
					tile = PPU.ppu.vram[(ty * 128 + tx) << 1];
				}
				palette = PPU.ppu.vram[(((tile << 6) + ((py & 7) << 3) + (px & 7)) << 1) + 1];
				break;
			}
			}

			int priority;
			if (id == ID_BG1)
			{
				priority = priority0;
			}
			else
			{
				priority = ((palette & 0x80) != 0 ? priority1 : priority0);
				palette &= 0x7f;
			}

			if (palette == 0)
			{
				continue;
			}
			int plot_x = (PPU.ppu.regs.mode7_hflip == false ? x : 255 - x);

			int color;
			if (PPU.ppu.screen.regs.direct_color && id == ID_BG1)
			{
				color = PPU.ppu.screen.get_direct_color(0, palette);
			}
			else
			{
				color = PPU.ppu.screen.get_palette(palette);
			}

			if (regs.main_enable && (window.main[plot_x] == 0))
			{
				PPU.ppu.screen.output.plot_main(plot_x, color, priority, id);
			}
			if (regs.sub_enable && (window.sub[plot_x] == 0))
			{
				PPU.ppu.screen.output.plot_sub(plot_x, color, priority, id);
			}
		}
	}

	public Background(PPU self, int id)
	{
		//this.self = self;
		this.id = id;

		priority0_enable = true;
		priority1_enable = true;

		opt_valid_bit = (id == ID_BG1 ? 0x2000 : id == ID_BG2 ? 0x4000 : 0x0000);

		mosaic_table = new int[16][];
		for (int m = 0; m < 16; m++)
		{
			mosaic_table[m] = new int[4096];
			for (int x = 0; x < 4096; x++)
			{
				mosaic_table[m][x] = ((x / (m + 1)) * (m + 1)) & 0xFFFF;
			}
		}
	}

	private static final int sclip(int bits, int x)
	{
		int b = 1 << (bits - 1);
		int m = (1 << bits) - 1;
		return (((x & m) ^ b) - b);
	}

	//public transient PPU self;
}
