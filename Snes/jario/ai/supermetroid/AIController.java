package jario.ai.supermetroid;



import jario.hardware.Bus16bit;
import jario.hardware.Bus8bit;
import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;
import jario.snes.system.SnesSystem;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class AIController implements Hardware, Clockable, Bus16bit, Serializable, Configurable
{
	private static final int Joypad_B = 1 << 0;
	private static final int Joypad_Y = 1 << 1;
	private static final int Joypad_Select = 1 << 2;
	private static final int Joypad_Start = 1 << 3;
	private static final int Joypad_Up = 1 << 4;
	private static final int Joypad_Down = 1 << 5;
	private static final int Joypad_Left = 1 << 6;
	private static final int Joypad_Right = 1 << 7;
	private static final int Joypad_A = 1 << 8;
	private static final int Joypad_X = 1 << 9;
	private static final int Joypad_L = 1 << 10;
	private static final int Joypad_R = 1 << 11;

	private static final int Joypad_LeftRight = Joypad_Left | Joypad_Right;
	private static final int Joypad_UpDown = Joypad_Up | Joypad_Down;

	
	public boolean isRecording = false;
	public boolean isPlayback = false;
	
	public Queue recordedKeys;
	
	public static Bus8bit memorybus;
	
	public AIPlayerInfo playerInfo = null;
	
	public int clockcnt=0;
	@Override
	public void clock(long time) {
		// TODO Auto-generated method stub
		int health = playerInfo.getEnergy();
		
		//TileInfo[][] tiles = TileInfo.getTileMap2D();
		//int width = tiles.length;
		clockcnt++;
		if( clockcnt > 100 )
		{
			
			/*
			int prevData = 0;
			
			for(int i=0x7E0000; i<0x7FFFFF; i++)
			{
				int data = memorybus.read8bit(i) & 0xFF;
				if( data == 0x0D && prevData == 0x99 )
				{
					System.out.println("Found Door$00 - 0x990D at : " + (i-1));
				}
				
				if( data == 0xF8 && prevData == 0x91 )
				{
					System.out.println("Found Door$01 - 0x91F8 at : " + (i-1));
				}
				
				if( data == 0xE2 && prevData == 0x98 )
				{
					System.out.println("Found Door$02 - 0x98E2 at : " + (i-1));
				}
				
				if( data == 0x79 && prevData == 0x98 )
				{
					System.out.println("Found Door$03 - 0x9879 at : " + (i-1));
				}
				
				if( data == 0xBA && prevData == 0x96 )
				{
					System.out.println("Found Door$04 - 0x96BA at : " + (i-1));
				}
				
				if( data == 0xD5 && prevData == 0x93 )
				{
					System.out.println("Found Door$05 - 0x93D5 at : " + (i-1));
				}
				
				if( data == 0x44 && prevData == 0x9A )
				{
					System.out.println("Found Door$06 - 0x9A44 at : " + (i-1));
				}
				prevData = data;
			}*/
		}
		
		//int height = tiles[0].length;
		//if( health != 0 )
		//	System.out.println("Health = " + health);
	}
	
	@Override
	public void connect(int port, Hardware hw)
	{
		switch(port)
		{
		case 0:
			playerInfo.connect(port, hw);
			memorybus = (Bus8bit)hw;
			break;
		}
	}
	
	
	
	private void SetInputState(int port, int index, int buttonStates, int x, int y)
	{
		if ((buttonStates & Joypad_LeftRight) == Joypad_LeftRight)
		{
			buttonStates &= ~Joypad_LeftRight;
		}

		if ((buttonStates & Joypad_UpDown) == Joypad_UpDown)
		{
			buttonStates &= ~Joypad_UpDown;
		}

		int i = port * 4 + index;
		inputButtons[i] = buttonStates;
	}

	private int ParseInput(int playerIndex)
	{
		int snesButtonStates = 0;

		for (int i = 0; i < keys.length; i++)
		{
			if ((keyboardState & keys[i].value) != 0)
			{
				snesButtonStates |= keys[i].value;
			}
		}

		return snesButtonStates;
	}
	
	
	private class Key implements Serializable
	{
		public int vkey;
		public int value;

		public Key(int value, int vkey)
		{
			this.value = value;
			this.vkey = vkey;
		}
	}

	private class ControllerListener implements AWTEventListener
	{
		public void eventDispatched(AWTEvent event)
		{
			KeyEvent kevt = (KeyEvent) event;
			if (kevt.getID() == KeyEvent.KEY_PRESSED)
			{
				for (int i = 0; i < keys.length; i++)
				{
					if (keys[i].vkey == kevt.getKeyCode())
					{
						keyboardState |= keys[i].value;
						return;
					}
				}
			}
			else if (kevt.getID() == KeyEvent.KEY_RELEASED)
			{
				for (int i = 0; i < keys.length; i++)
				{
					if (keys[i].vkey == kevt.getKeyCode())
					{
						keyboardState &= ~keys[i].value;
						return;
					}
				}
			}
		}
	}
	
	

	private int keyboardState;
	private int[] inputButtons = new int[8];
	private Key[] keys = {
			new Key(Joypad_B, KeyEvent.VK_Z),
			new Key(Joypad_Y, KeyEvent.VK_A),
			new Key(Joypad_Select, KeyEvent.VK_QUOTE),
			new Key(Joypad_Start, KeyEvent.VK_ENTER),
			new Key(Joypad_Up, KeyEvent.VK_UP),
			new Key(Joypad_Down, KeyEvent.VK_DOWN),
			new Key(Joypad_Left, KeyEvent.VK_LEFT),
			new Key(Joypad_Right, KeyEvent.VK_RIGHT),
			new Key(Joypad_A, KeyEvent.VK_X),
			new Key(Joypad_X, KeyEvent.VK_S),
			new Key(Joypad_L, KeyEvent.VK_D),
			new Key(Joypad_R, KeyEvent.VK_C),
	};

	public AIController()
	{
		Toolkit.getDefaultToolkit().addAWTEventListener(new ControllerListener(), AWTEvent.KEY_EVENT_MASK);
		
		playerInfo = new AIPlayerInfo();
	}

	

	@Override
	public void reset()
	{
	}

	@Override
	public short read16bit(int address)
	{
		SetInputState(0, 0, ParseInput(0), 0, 0);
		if( isRecording )
		{
			recordedKeys.add((short)inputButtons[0]);
		}
		
		if( isPlayback && !recordedKeys.isEmpty())
		{
			short keys = (short)recordedKeys.remove();
			return keys;
		}
			
		
		return (short) inputButtons[0];
	}

	@Override
	public void write16bit(int address, short data)
	{
	}

	

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object readConfig(String key) {
		// TODO Auto-generated method stub
		if(key.equals("recordedkeys"))
		{
			return recordedKeys;
		}
		return null;
	}

	
	@Override
	public void writeConfig(String key, Object value) {
		// TODO Auto-generated method stub
		if( key.equals("record") ) 
		{
			if( isRecording )
				return;
			
			recordedKeys = new LinkedList();
			isRecording = true;
		}
		else if(key.equals("replay") ) 
		{
			if( isPlayback )
				return;
			
			isPlayback = true;
			recordedKeys = new LinkedList();
			
			int count = 0;
			try
			{
				ObjectInputStream inputStream = (ObjectInputStream)value;
				while(true)
				{
					short keys = inputStream.readShort();
					recordedKeys.add(keys);
					count++;
				}
			}
			catch(Exception e)
			{
				System.out.println("Finished reading " + count + " recorded keys.");
			}
		}
		else if(key.equals("stop"))
		{
			isPlayback = false;
			isRecording = false;
		}
	}

	
}

