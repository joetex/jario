/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.accessories;

import jario.hardware.Bus16bit;
import jario.hardware.Hardware;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;

public class Rumblepad2 implements Hardware, Bus16bit
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

	private class Key
	{
		public int vkey;
		public int value;

		public Key(int value, int vkey)
		{
			this.value = value;
			this.vkey = vkey;
		}
	}

	Runnable poll = new Runnable()
	{
		public void run()
		{
			while (true)
			{
				try
				{
					Thread.sleep(100);
				}
				catch (Exception e)
				{
				}

				Controllers.poll();

				update();
			}
		}
	};

	private int keyboardState;
	private int[] inputButtons = new int[8];
	private Key[] keys = {
			new Key(Joypad_B, 1),
			new Key(Joypad_Y, 0),
			new Key(Joypad_Select, 8),
			new Key(Joypad_Start, 9),
			new Key(Joypad_Up, -1),
			new Key(Joypad_Down, -1),
			new Key(Joypad_Left, -1),
			new Key(Joypad_Right, -1),
			new Key(Joypad_A, 2),
			new Key(Joypad_X, 3),
			new Key(Joypad_L, 4),
			new Key(Joypad_R, 5),
	};

	Controller controller;
	int buttonCount;
	private ExecutorService executor = Executors.newSingleThreadExecutor();

	public Rumblepad2()
	{
		// System.out.println("Joypad init");

		try
		{
			Controllers.create();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		int count = Controllers.getControllerCount();

		for (int i = 0; i < count; i++)
		{
			controller = Controllers.getController(i);
			if (controller.getAxisCount() == 4 && controller.getButtonCount() >= 8)
			{
				buttonCount = controller.getButtonCount();
				break;
			}
		}

		executor.execute(poll);
	}

	private void update()
	{
		for (int i = 0; i < keys.length; i++)
		{
			if (keys[i].vkey >= 0)
			{
				if (controller.isButtonPressed(keys[i].vkey))
				{
					keyboardState |= keys[i].value;
				}
				else
				{
					keyboardState &= ~keys[i].value;
				}
			}
		}
		for (int i = buttonCount; i < buttonCount + controller.getAxisCount(); i++)
		{
			if (controller.getAxisName(i - buttonCount).equals("X Axis"))
			{
				float value = controller.getAxisValue(i - buttonCount);

				if (value > 0.2f && value <= 1.0f)
				{
					keyboardState |= Joypad_Right;
				}
				else if (value < -0.2f && value >= -1.0f)
				{
					keyboardState |= Joypad_Left;
				}
				else
				{
					keyboardState &= ~Joypad_Right;
					keyboardState &= ~Joypad_Left;
				}
			}
			if (controller.getAxisName(i - buttonCount).equals("Y Axis"))
			{
				float value = controller.getAxisValue(i - buttonCount);

				if (value > 0.2f && value <= 1.0f)
				{
					keyboardState |= Joypad_Down;
				}
				else if (value < -0.2f && value >= -1.0f)
				{
					keyboardState |= Joypad_Up;
				}
				else
				{
					keyboardState &= ~Joypad_Down;
					keyboardState &= ~Joypad_Up;
				}
			}
		}
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
