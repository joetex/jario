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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class AudioPlayer implements Hardware, BusDMA, Configurable, java.io.Serializable
{
	private transient AudioFormat audioFormat = null;
	private transient SourceDataLine audioDataLine = null;

	private boolean enable = true;
	private float sampleRate;

	public AudioPlayer()
	{
		sampleRate = 33334; // 62.5 fps
		initAudio(sampleRate);
	}

	//adding helper method for serialization to save/initialize super class state
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException{
        ois.defaultReadObject();
        
        sampleRate = 33334; // 62.5 fps
        initAudio(sampleRate);
    }
     
    private void writeObject(ObjectOutputStream oos) throws IOException{
        oos.defaultWriteObject();
         
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
	public void readDMA(int address, ByteBuffer b, int offset, int length)
	{
	}

	@Override
	public void writeDMA(int address, ByteBuffer buffer, int offset, int length)
	{
		if (enable && audioDataLine == null)
		{
			initAudio(sampleRate);
		}
		if (!enable && audioDataLine != null)
		{
			closeAudio();
		}
		if (enable && audioDataLine != null)
		{
			audioDataLine.write(buffer.array(), offset, length);
		}
	}

	@Override
	public Object readConfig(String key)
	{
		if (key.equals("enable")) return enable;
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("enable")) enable = (Boolean) value;
		else if (key.equals("samplerate"))
		{
			sampleRate = (Integer) value;
			initAudio(sampleRate);
		}
	}

	private boolean initAudio(float sampleRate)
	{
		closeAudio(); // Release just in case...
		audioFormat = new AudioFormat(sampleRate, 16, 2, true, true);
		SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
		try
		{
			audioDataLine = (SourceDataLine) AudioSystem.getLine(info);
			audioDataLine.open(audioFormat);
			audioDataLine.start();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void closeAudio()
	{
		if (audioDataLine != null)
		{
			audioDataLine.flush();
			audioDataLine.stop();
			audioDataLine.close();
			audioDataLine = null;
		}
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
}
