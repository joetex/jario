/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.accessories;

import jario.hardware.Bus16bit;
import jario.hardware.Hardware;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.io.Serializable;

public class Keyboard implements Hardware, Bus16bit, Serializable
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

	public Keyboard()
	{
		Toolkit.getDefaultToolkit().addAWTEventListener(new ControllerListener(), AWTEvent.KEY_EVENT_MASK);
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
	public short read16bit(int address)
	{
		SetInputState(0, 0, ParseInput(0), 0, 0);
		return (short) inputButtons[0];
	}

	@Override
	public void write16bit(int address, short data)
	{
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

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
}
