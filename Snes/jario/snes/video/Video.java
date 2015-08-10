/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.video;

import jario.hardware.Bus1bit;
import jario.hardware.Bus32bit;
import jario.hardware.BusDMA;
import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;
import jario.snes.accessories.VideoPlayer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Video implements Hardware, Clockable, Bus32bit, Configurable, java.io.Serializable
{
	private int FRAMES_PER_SECOND = 60;
	private int SKIP_TICKS = 1000 / FRAMES_PER_SECOND;

	public transient BusDMA output;
	private transient BusDMA ppuDma;
	private transient Bus1bit ppu1bit;
	private transient Bus32bit audio;

	private transient ExecutorService executor;
	private long currentTime;
	private long next_game_tick;
	private long sleep_time;
	private boolean limit = true;

	private boolean enable = true;
	
	class AudioVideoThread implements Runnable, java.io.Serializable
	{
		private int width, height;

		public AudioVideoThread(int width, int height)
		{
			this.width = width;
			this.height = height;
		}

		public void run()
		{
			output.writeDMA(0, data_output, 1024 * 2, ((width & 0xFFFF) << 16) | (height & 0xFFFF));
		}
	}

	private boolean frame_hires;
	private boolean frame_interlace;
	private int[] line_width = new int[240];
	transient ByteBuffer data_output = ByteBuffer.allocate(1024 * 1024 * 2);

	//adding helper method for serialization to save/initialize super class state
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException{
        ois.defaultReadObject();

        data_output = ByteBuffer.allocate(1024 * 1024 * 2);
        int len = ois.readInt();
        int pos = ois.readInt();
        byte[] buff = new byte[len];
        ois.read(buff, 0, len);
        
        data_output.put(buff);
        data_output.position(pos);
    }
     
    private void writeObject(ObjectOutputStream oos) throws IOException{
        oos.defaultWriteObject();
         
       byte[] buff = data_output.array();
       oos.writeInt(buff.length);
       oos.writeInt(data_output.position());
       oos.write(buff);
    }
    
	public Video()
	{
		executor = Executors.newSingleThreadExecutor();
		frame_hires = false;
		frame_interlace = false;
		for (int i = 0; i < 240; i++)
		{
			line_width[i] = 256;
		}
		next_game_tick = System.currentTimeMillis();
	}

	@Override
	public void connect(int port, Hardware hw)
	{
		switch (port)
		{
		case 0:
			output = (BusDMA) hw;
			break;
		case 1:
			ppuDma = (BusDMA) hw;
			ppu1bit = (Bus1bit) hw;
			break;
		case 2:
			audio = (Bus32bit) hw;
			break;
		}
	}

	@Override
	public void reset()
	{
	}

	@Override
	public void clock(long clocks)
	{
		if(enable)
		{
			ppuDma.readDMA(0, data_output, 0, 512 * 478 * 2);
	
			ShortBuffer data = data_output.asShortBuffer();
			int data_offset = 0;
	
			if (ppu1bit.read1bit(0) && ppu1bit.read1bit(3))
			{
				data_offset += 512;
			}
			int width = 256;
			int height = !ppu1bit.read1bit(1) ? 224 : 239;
	
			if (frame_hires)
			{
				width <<= 1;
				// normalize line widths
				for (int y = 0; y < 240; y++)
				{
					if (line_width[y] == 512)
					{
						continue;
					}
					ShortBuffer buffer = data;
					int buffer_offset = data_offset + (y * 1024);
					for (int x = 255; x >= 0; x--)
					{
						short s = buffer.get(buffer_offset + x);
						buffer.put(buffer_offset + ((x * 2) + 1), s);
						buffer.put(buffer_offset + ((x * 2) + 0), s);
					}
				}
			}
	
			if (frame_interlace)
			{
				height <<= 1;
			}
	
			//executor.execute(new AudioVideoThread(width, height));
			output.writeDMA(0, data_output, 1024 * 2, ((width & 0xFFFF) << 16) | (height & 0xFFFF));
			audio.read32bit(0); // play audio for this frame
	
			frame_hires = false;
			frame_interlace = false;
		}

		//((VideoPlayer)output).updateFPS();
		
		currentTime = System.currentTimeMillis();

		if (limit)
		{
			next_game_tick += SKIP_TICKS;
			sleep_time = next_game_tick - currentTime;
			if (sleep_time >= 0)
			{
				try
				{
					Thread.sleep(sleep_time);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
		next_game_tick = System.currentTimeMillis();
	}

	@Override
	public int read32bit(int address)
	{
		return 0;
	}

	@Override
	public void write32bit(int address, int data)
	{   // scanline
		int y = data;
		if (y >= 240) { return; }

		frame_hires |= ppu1bit.read1bit(2);
		frame_interlace |= ppu1bit.read1bit(0);
		int width = !ppu1bit.read1bit(2) ? 256 : 512;
		line_width[y] = width;
	}

	@Override
	public Object readConfig(String key)
	{
		if (key.equals("enable")) return enable;
		if (key.equals("fps")) return FRAMES_PER_SECOND;
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("enable")) 
		{
			enable = (Boolean) value;
			((Configurable)ppuDma).writeConfig("enablerender", value);
			((Configurable)ppu1bit).writeConfig("enablerender", value);
		}
		if (key.equals("fps"))
		{
			FRAMES_PER_SECOND = (Integer) value;
			SKIP_TICKS = 1000 / FRAMES_PER_SECOND;
			
			limit = (FRAMES_PER_SECOND <= 60);
			
		}
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		output = null;
		ppuDma = null;
		ppu1bit = null;
		audio = null;
		
	}
}
