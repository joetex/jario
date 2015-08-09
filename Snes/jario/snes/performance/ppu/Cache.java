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

public class Cache implements java.io.Serializable
{
	private transient ByteBuffer tiledata0;
	private transient ByteBuffer tiledata1;
	private transient ByteBuffer tiledata2;
	transient byte[][] tilevalid = new byte[3][];

	
	//adding helper method for serialization to save/initialize super class state
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException
    {
        ois.defaultReadObject();
         
        tilevalid = new byte[3][];
        tilevalid[0] = new byte[4096];
		tilevalid[1] = new byte[2048];
		tilevalid[2] = new byte[1024];
		
		int len1 = ois.readInt();
        for(int i=0; i<len1; i++)
        {
        	int len2 = ois.readInt();
        	for(int j=0;j<len2; j++)
        	{
        		tilevalid[i][j] = ois.readByte();
        	}
        }
		
        tiledata0 = ByteBuffer.allocate(262144);
		tiledata1 = ByteBuffer.allocate(131072);
		tiledata2 = ByteBuffer.allocate(65536);
		
        int len = ois.readInt();
        int pos = ois.readInt();
        int limit = ois.readInt();
        byte[] buff = new byte[len];
        for(int i=0; i<len; i++)
        {
        	buff[i] = ois.readByte();
        }
        tiledata0.put(buff);
        tiledata0.position(pos);
        tiledata0.limit(limit);
        
        len = ois.readInt();
        pos = ois.readInt();
        limit = ois.readInt();
        buff = new byte[len];
        for(int i=0; i<len; i++)
        {
        	buff[i] = ois.readByte();
        }
        tiledata1.put(buff);
        tiledata1.position(pos);
        tiledata1.limit(limit);
        
        len = ois.readInt();
        pos = ois.readInt();
        limit = ois.readInt();
        buff = new byte[len];
        for(int i=0; i<len; i++)
        {
        	buff[i] = ois.readByte();
        }
        tiledata2.put(buff);
        tiledata2.position(pos);
        tiledata2.limit(limit);
    }
     
    private void writeObject(ObjectOutputStream oos) throws IOException{
        oos.defaultWriteObject();
         
        oos.writeInt(tilevalid.length);
        for(int i=0; i<tilevalid.length; i++)
        {
        	oos.writeInt(tilevalid[i].length);
        	for(int j=0;j<tilevalid[i].length; j++)
        	{
        		oos.writeByte(tilevalid[i][j]);
        	}
        }
        
        byte[] buff = tiledata0.array();
        oos.writeInt(buff.length);
        oos.writeInt(tiledata0.position());
        oos.writeInt(tiledata0.limit());
        for(int i=0; i<buff.length; i++)
        {
        	oos.writeByte(buff[i]);
        }
        
        
        buff = tiledata1.array();
        oos.writeInt(buff.length);
        oos.writeInt(tiledata1.position());
        oos.writeInt(tiledata1.limit());
        for(int i=0; i<buff.length; i++)
        {
        	oos.writeByte(buff[i]);
        }
        
        buff = tiledata2.array();
        oos.writeInt(buff.length);
        oos.writeInt(tiledata2.position());
        oos.writeInt(tiledata2.limit());
        for(int i=0; i<buff.length; i++)
        {
        	oos.writeByte(buff[i]);
        }
        
        
        
    }
    
    
	private static final void render_line_2(byte[] output, int output_offset, int d0, int d1, int mask)
	{
		int color = (((d0 & mask) != 0) ? 1 : 0) << 0;
		color |= (((d1 & mask) != 0) ? 1 : 0) << 1;
		output[output_offset] = (byte) color;
	}

	private static final void render_line_4(byte[] output, int output_offset, int d0, int d1, int d2, int d3, int mask)
	{
		int color = (((d0 & mask) != 0) ? 1 : 0) << 0;
		color |= (((d1 & mask) != 0) ? 1 : 0) << 1;
		color |= (((d2 & mask) != 0) ? 1 : 0) << 2;
		color |= (((d3 & mask) != 0) ? 1 : 0) << 3;
		output[output_offset] = (byte) color;
	}

	private static final void render_line_8(byte[] output, int output_offset, int d0, int d1, int d2, int d3, int d4, int d5, int d6, int d7, int mask)
	{
		int color = (((d0 & mask) != 0) ? 1 : 0) << 0;
		color |= (((d1 & mask) != 0) ? 1 : 0) << 1;
		color |= (((d2 & mask) != 0) ? 1 : 0) << 2;
		color |= (((d3 & mask) != 0) ? 1 : 0) << 3;
		color |= (((d4 & mask) != 0) ? 1 : 0) << 4;
		color |= (((d5 & mask) != 0) ? 1 : 0) << 5;
		color |= (((d6 & mask) != 0) ? 1 : 0) << 6;
		color |= (((d7 & mask) != 0) ? 1 : 0) << 7;
		output[output_offset] = (byte) color;
	}

	ByteBuffer tile_2bpp(int tile)
	{
		if (tilevalid[0][tile] == 0)
		{
			tilevalid[0][tile] = 1;
			byte[] output = tiledata0.array();
			int output_offset = tile << 6;
			int offset = tile << 4;
			int y = 8;
			int d0, d1;
			while ((y--) != 0)
			{
				d0 = PPU.ppu.vram[offset + 0];
				d1 = PPU.ppu.vram[offset + 1];
				render_line_2(output, output_offset++, d0, d1, 0x80);
				render_line_2(output, output_offset++, d0, d1, 0x40);
				render_line_2(output, output_offset++, d0, d1, 0x20);
				render_line_2(output, output_offset++, d0, d1, 0x10);
				render_line_2(output, output_offset++, d0, d1, 0x08);
				render_line_2(output, output_offset++, d0, d1, 0x04);
				render_line_2(output, output_offset++, d0, d1, 0x02);
				render_line_2(output, output_offset++, d0, d1, 0x01);
				offset += 2;
			}
		}
		tiledata0.position(tile << 6);
		return tiledata0;
	}

	ByteBuffer tile_4bpp(int tile)
	{
		if (tilevalid[1][tile] == 0)
		{
			tilevalid[1][tile] = 1;
			byte[] output = tiledata1.array();
			int output_offset = tile << 6;
			int offset = tile << 5;
			int y = 8;
			int d0, d1, d2, d3;
			while ((y--) != 0)
			{
				d0 = PPU.ppu.vram[offset + 0];
				d1 = PPU.ppu.vram[offset + 1];
				d2 = PPU.ppu.vram[offset + 16];
				d3 = PPU.ppu.vram[offset + 17];
				render_line_4(output, output_offset++, d0, d1, d2, d3, 0x80);
				render_line_4(output, output_offset++, d0, d1, d2, d3, 0x40);
				render_line_4(output, output_offset++, d0, d1, d2, d3, 0x20);
				render_line_4(output, output_offset++, d0, d1, d2, d3, 0x10);
				render_line_4(output, output_offset++, d0, d1, d2, d3, 0x08);
				render_line_4(output, output_offset++, d0, d1, d2, d3, 0x04);
				render_line_4(output, output_offset++, d0, d1, d2, d3, 0x02);
				render_line_4(output, output_offset++, d0, d1, d2, d3, 0x01);
				offset += 2;
			}
		}
		tiledata1.position(tile << 6);
		return tiledata1;
	}

	ByteBuffer tile_8bpp(int tile)
	{
		if (tilevalid[2][tile] == 0)
		{
			tilevalid[2][tile] = 1;
			byte[] output = tiledata2.array();
			int output_offset = tile << 6;
			int offset = tile << 6;
			int y = 8;
			int d0, d1, d2, d3, d4, d5, d6, d7;
			while ((y--) != 0)
			{
				d0 = PPU.ppu.vram[offset + 0];
				d1 = PPU.ppu.vram[offset + 1];
				d2 = PPU.ppu.vram[offset + 16];
				d3 = PPU.ppu.vram[offset + 17];
				d4 = PPU.ppu.vram[offset + 32];
				d5 = PPU.ppu.vram[offset + 33];
				d6 = PPU.ppu.vram[offset + 48];
				d7 = PPU.ppu.vram[offset + 49];
				render_line_8(output, output_offset++, d0, d1, d2, d3, d4, d5, d6, d7, 0x80);
				render_line_8(output, output_offset++, d0, d1, d2, d3, d4, d5, d6, d7, 0x40);
				render_line_8(output, output_offset++, d0, d1, d2, d3, d4, d5, d6, d7, 0x20);
				render_line_8(output, output_offset++, d0, d1, d2, d3, d4, d5, d6, d7, 0x10);
				render_line_8(output, output_offset++, d0, d1, d2, d3, d4, d5, d6, d7, 0x08);
				render_line_8(output, output_offset++, d0, d1, d2, d3, d4, d5, d6, d7, 0x04);
				render_line_8(output, output_offset++, d0, d1, d2, d3, d4, d5, d6, d7, 0x02);
				render_line_8(output, output_offset++, d0, d1, d2, d3, d4, d5, d6, d7, 0x01);
				offset += 2;
			}
		}
		tiledata2.position(tile << 6);
		return tiledata2;
	}

	ByteBuffer tile(int bpp, int tile)
	{
		switch (bpp)
		{
		case 0:
			return tile_2bpp(tile);
		case 1:
			return tile_4bpp(tile);
		case 2:
		default:
			return tile_8bpp(tile);
		}
	}

	public Cache(PPU self)
	{
		//this.self = self;
		tiledata0 = ByteBuffer.allocate(262144);
		tiledata1 = ByteBuffer.allocate(131072);
		tiledata2 = ByteBuffer.allocate(65536);
		tilevalid[0] = new byte[4096];
		tilevalid[1] = new byte[2048];
		tilevalid[2] = new byte[1024];
	}

	//public transient PPU self;
}
