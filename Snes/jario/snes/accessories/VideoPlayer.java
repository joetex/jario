/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.accessories;

import jario.hardware.BusDMA;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import javax.swing.JFrame;

public class VideoPlayer implements Hardware, BusDMA, Configurable, java.io.Serializable
{
	private transient JFrame window;
	private transient Graphics2D graphics;
	private transient BufferedImage bufferedImage;
	private int[] colorConvert = new int[65536];
	private long currentTime;
	private long previousTime;
	private int frames;
	private Boolean enabled = true;
	
	public VideoPlayer()
	{
		for (int color = 0; color < colorConvert.length; color++)
		{
			colorConvert[color] = convertColor(color);
		}
		bufferedImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
		previousTime = currentTime = System.currentTimeMillis();
	}

	@Override
	public void connect(int port, Hardware hw)
	{
	}

	@Override
	public void reset()
	{
	}

	@Override
	public void readDMA(int address, ByteBuffer data, int offset, int length)
	{
	}

	@Override
	public void writeDMA(int address, ByteBuffer data, int offset, int length)
	{
		if(enabled)
		{
			final int width = (length >> 16) & 0xFFFF;
			final int height = length & 0xFFFF;
			final int pitch = (height >= 240) ? 512 : 1024;
	
			ShortBuffer buffer = data.asShortBuffer();
	
			for (int y = 0; y < height; y++)
			{
				int a = y * pitch;
				int x = 0;
				while (x < width)
				{
					bufferedImage.setRGB(x, y, colorConvert[buffer.get(a + x++) & 0xFFFF]);
					bufferedImage.setRGB(x, y, colorConvert[buffer.get(a + x++) & 0xFFFF]);
					bufferedImage.setRGB(x, y, colorConvert[buffer.get(a + x++) & 0xFFFF]);
					bufferedImage.setRGB(x, y, colorConvert[buffer.get(a + x++) & 0xFFFF]);
					bufferedImage.setRGB(x, y, colorConvert[buffer.get(a + x++) & 0xFFFF]);
					bufferedImage.setRGB(x, y, colorConvert[buffer.get(a + x++) & 0xFFFF]);
					bufferedImage.setRGB(x, y, colorConvert[buffer.get(a + x++) & 0xFFFF]);
					bufferedImage.setRGB(x, y, colorConvert[buffer.get(a + x++) & 0xFFFF]);
				}
			}
	
			graphics.drawImage(bufferedImage, 0, 0, 512, 448, 0, 0, width, height, null);
			
			//updateFPS();
		}
		
	}

	public void updateFPS()
	{
		currentTime = System.currentTimeMillis();
		if (++frames % 20 == 0)
		{
			if( frames % 100 == 0)
				window.setTitle("FPS " + Integer.toString((int) ((20 * 1000.0f) / (currentTime - previousTime))));
			previousTime = currentTime;
		}
	}
	@Override
	public Object readConfig(String key)
	{
		if (key.equals("enable")) return enabled;
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("enable")) enabled = (Boolean) value;
		if (key.equals("window"))
		{
			window = (JFrame) value;
			graphics = (Graphics2D) window.getContentPane().getComponent(0).getGraphics();
		}
	}

	private int convertColor(int color)
	{
		int b = ((color >> 10) & 31) * 8;
		int red = b + b / 35;
		b = ((color >> 5) & 31) * 8;
		int green = b + b / 35;
		b = ((color >> 0) & 31) * 8;
		int blue = b + b / 35;

		return (255 << 24) | (red << 16) | (green << 8) | blue;
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		window = null;
		graphics = null;
		bufferedImage = null;
	}
}
