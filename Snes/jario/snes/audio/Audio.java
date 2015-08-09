/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.audio;

import jario.hardware.Bus32bit;
import jario.hardware.BusDMA;
import jario.hardware.Hardware;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

public class Audio implements Hardware, Bus32bit, java.io.Serializable
{
	private BusDMA output;
	private byte[] buffer0 = new byte[8192];
	private transient ByteBuffer outputBuffer;
	private int bufferIndex;

	//adding helper method for serialization to save/initialize super class state
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException{
        ois.defaultReadObject();
         
        int len = ois.readInt();
        int pos = ois.readInt();
        
        ois.read(buffer0, 0, len);
        outputBuffer = ByteBuffer.wrap(buffer0);
        outputBuffer.position(pos);
    }
     
    private void writeObject(ObjectOutputStream oos) throws IOException{
        oos.defaultWriteObject();
         
       byte[] buff = outputBuffer.array();
       oos.writeInt(buff.length);
       oos.writeInt(outputBuffer.position());
       oos.write(buff);
    }
    
	public Audio()
	{
		outputBuffer = ByteBuffer.wrap(buffer0);
		reset();
	}

	@Override
	public void connect(int port, Hardware hw)
	{
		switch (port)
		{
		case 0:
			output = (BusDMA) hw;
			break;
		}
	}

	@Override
	public void reset()
	{
		bufferIndex = 0;
	}

	@Override
	public int read32bit(int address)
	{
		switch (address)
		{
		case 0:
			int index = bufferIndex;
			output.writeDMA(0, outputBuffer, 0, index);
			bufferIndex = 0;
			return index;
		default:
			return 0;
		}
	}

	@Override
	public void write32bit(int address, int sample)
	{
		if (bufferIndex < buffer0.length)
		{
			buffer0[bufferIndex++] = (byte) (sample >> 24); // left
			buffer0[bufferIndex++] = (byte) (sample >> 16); // left
			buffer0[bufferIndex++] = (byte) (sample >> 8); // right
			buffer0[bufferIndex++] = (byte) (sample >> 0); // right
		}
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
}
