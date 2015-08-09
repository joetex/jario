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
import java.util.Arrays;

public class ColorWindow implements java.io.Serializable
{
	public boolean one_enable;
	public boolean one_invert;
	public boolean two_enable;
	public boolean two_invert;

	public int mask;

	public int main_mask;
	public int sub_mask;

	public int[] main = new int[256];
	public int[] sub = new int[256];

	//adding helper method for serialization to save/initialize super class state
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException{
        ois.defaultReadObject();
         
        int len1 = ois.readInt();
        for(int i=0; i<len1; i++)
        {
        	main[i] = ois.readInt();
        }
        
        int len2 = ois.readInt();
        for(int i=0; i<len2; i++)
        {
        	sub[i] = ois.readInt();
        }
    }
     
    private void writeObject(ObjectOutputStream oos) throws IOException
    {
        oos.defaultWriteObject();
         
        int len1 = main.length;
        oos.writeInt(len1);
        for(int i=0; i<len1; i++)
        {
        	oos.writeInt(main[i]);
        }
        
        int len2 = sub.length;
        oos.writeInt(len2);
        for(int i=0; i<len2; i++)
        {
        	oos.writeInt(sub[i]);
        }
    }
    
    
	public void render(boolean screen)
	{
		int[] output = (screen == false ? main : sub);
		boolean set = true, clr = false;

		switch (screen == false ? main_mask : sub_mask)
		{
		case 0:
			Arrays.fill(output, 1);
			return; // always
		case 1:
			set = true;
			clr = false;
			break; // inside window only
		case 2:
			set = false;
			clr = true;
			break; // outside window only
		case 3:
			Arrays.fill(output, 0);
			return; // never
		}

		if (one_enable == false && two_enable == false)
		{
			Arrays.fill(output, (clr ? 1 : 0));
			return;
		}

		if (one_enable == true && two_enable == false)
		{
			if (one_invert)
			{
				set ^= true;
				clr ^= true;
			}
			for (int x = 0; x < 256; x++)
			{
				output[x] = (((x >= PPU.ppu.regs.window_one_left && x <= PPU.ppu.regs.window_one_right) ? set : clr) ? 1 : 0);
			}
			return;
		}

		if (one_enable == false && two_enable == true)
		{
			if (two_invert)
			{
				set ^= true;
				clr ^= true;
			}
			for (int x = 0; x < 256; x++)
			{
				output[x] = (((x >= PPU.ppu.regs.window_two_left && x <= PPU.ppu.regs.window_two_right) ? set : clr) ? 1 : 0);
			}
			return;
		}

		for (int x = 0; x < 256; x++)
		{
			boolean one_mask = (x >= PPU.ppu.regs.window_one_left && x <= PPU.ppu.regs.window_one_right) ^ one_invert;
			boolean two_mask = (x >= PPU.ppu.regs.window_two_left && x <= PPU.ppu.regs.window_two_right) ^ two_invert;
			switch (mask)
			{
			case 0:
				output[x] = ((one_mask | two_mask == true ? set : clr) ? 1 : 0);
				break;
			case 1:
				output[x] = ((one_mask & two_mask == true ? set : clr) ? 1 : 0);
				break;
			case 2:
				output[x] = ((one_mask ^ two_mask == true ? set : clr) ? 1 : 0);
				break;
			case 3:
				output[x] = ((one_mask ^ two_mask == false ? set : clr) ? 1 : 0);
				break;
			}
		}
	}
}
