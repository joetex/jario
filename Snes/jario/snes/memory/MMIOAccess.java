/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.memory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import jario.hardware.Bus8bit;
import jario.hardware.Configurable;
import jario.hardware.Hardware;
import jario.snes.memory.Bus.MapMode;

public class MMIOAccess implements Hardware, Bus8bit, Configurable, java.io.Serializable
{
	private transient Bus8bit[] mmio = new Bus8bit[0x8000];
	private byte dummy = 0;
	
	public MMIOAccess(UnmappedMMIO mmio_unmapped)
	{
		for (int i = 0; i < 0x8000; i++)
		{
			mmio[i] = mmio_unmapped;
		}
	}
	
	
	//adding helper method for serialization to save/initialize super class state
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException{
    	
    	ois.defaultReadObject();
    	
    	mmio = (Bus8bit[])ois.readObject();
    	/*mmio = new Bus8bit[0x8000];
    	
    	int pos = -1;
    	for(int i=0; i<mmio.length; i++)
    	{
    		if( i > pos)
    			pos = ois.readInt();
    		
    		if( i != pos )
    		{
    			mmio[i] = MemoryBus.mmio_unmapped_static;
    			continue;
    		}
    		
    		
    		mmio[i] = (Bus8bit)ois.readObject();
    	}*/
    }
     
    private void writeObject(ObjectOutputStream oos) throws IOException{
        
    	oos.defaultWriteObject();
    	
    	oos.writeObject(mmio);
       /*
    	//First count how many classes we found
    	int len = mmio.length;
    	int cnt = 0;
    	int firstPos = -1;
    	
    	for(int i=0; i<len; i++)
    	{
    		try
    		{
    			Class c = mmio[i].getClass();
    			if( c.getName().equals("jario.snes.memory.UnmappedMMIO") ) {
    				System.out.println("Index: " + i + " is not a class");
    				continue;
    			}
    			
    			//if( firstPos == -1 )
    			//	firstPos = i;
    			cnt++;
    		}
    		catch(Exception e)
    		{
    			
    		}
    	}
    	
    	//write out the class count
    	//oos.writeInt(firstPos);
    	
    	//now write the index and object
    	for(int i=0; i<len; i++)
    	{
    		try
    		{
    			Class c = mmio[i].getClass();
    			if( c.getName().equals("jario.snes.memory.UnmappedMMIO") ) {
    				System.out.println("Index: " + i + " is not a class");
    				continue;
    			}
    			oos.writeInt(i);
    			oos.writeObject(mmio[i]);
    		}
    		catch(Exception e)
    		{
    		}
    	}
 */
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
	public final byte read8bit(int addr)
	{
		return mmio[addr & 0x7fff].read8bit(addr);
	}

	@Override
	public final void write8bit(int addr, byte data)
	{
		mmio[addr & 0x7fff].write8bit(addr, data);
	}
	
	@Override
	public Object readConfig(String key)
	{
		if (key.equals("size")) return 0;
		// last
		try { return handle(Integer.parseInt(key, 16)); } catch (NumberFormatException e) { return null; }
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		// last
		try { map(Integer.parseInt(key, 16), (Bus8bit)value); } catch (NumberFormatException e) { }
	}
	
	Bus8bit handle(int addr)
	{
		return mmio[addr & 0x7fff];
	}
	
	void map(int addr, Bus8bit access)
	{
		mmio[addr & 0x7fff] = access;
	}


	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		for (int i = 0; i < 0x8000; i++)
		{
			mmio[i] = null;
		}
		mmio = null;
	}
}
