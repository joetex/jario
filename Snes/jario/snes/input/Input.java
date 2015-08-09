/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.input;

import jario.hardware.Bus16bit;
import jario.hardware.Bus8bit;
import jario.hardware.Clockable;
import jario.hardware.Hardware;

public class Input implements Hardware, Clockable, Bus8bit, java.io.Serializable
{
	private class Stub implements Bus16bit, java.io.Serializable
	{
		@Override
		public short read16bit(int address) { return 0; }

		@Override
		public void write16bit(int address, short data) { }
	}
	
	private Stub stub = new Stub();

	@Override
	public void connect(int port, Hardware hw)
	{
		if (port >= 2) return;
		this.port[port].bus = hw != null ? (Bus16bit) hw : stub;
		port_set_device(port, Device.Joypad);
	}

	@Override
	public void reset()
	{
		joypad_strobe_latch = false;
	}

	public enum Device
	{
		None, Joypad, Multitap, Mouse, SuperScope, Justifier, Justifiers
	}

	public static final int MouseID_X = 0;
	public static final int MouseID_Y = 1;
	public static final int MouseID_Left = 2;
	public static final int MouseID_Right = 3;

	public static final int SuperScopeID_X = 0;
	public static final int SuperScopeID_Y = 1;
	public static final int SuperScopeID_Trigger = 2;
	public static final int SuperScopeID_Cursor = 3;
	public static final int SuperScopeID_Turbo = 4;
	public static final int SuperScopeID_Pause = 5;

	public static final int JustifierID_X = 0;
	public static final int JustifierID_Y = 1;
	public static final int JustifierID_Trigger = 2;
	public static final int JustifierID_Start = 3;

	@Override
	public byte read8bit(int portnumber)
	{
		// if (portnumber < 0)
		// {
		// if (portnumber == -1) return (byte)latchx;
		// else if (portnumber == -2) return (byte)latchy;
		// }

		Port p = port[portnumber];

		switch (p.device)
		{
		case Joypad:
		{
			if (!joypad_strobe_latch)
			{
				if (p.counter0 >= 16) { return 1; }
				return (byte) (((p.bus.read16bit(portnumber * 4) & (1 << p.counter0++)) != 0) ? 1 : 0);
			}
			else
			{
				return (byte) (((p.bus.read16bit(portnumber * 4) & (1 << 0)) != 0) ? 1 : 0);
			}
		}
		case Multitap:
		{
		// //if (CPU.cpu.joylatch())
		// if (joypad_strobe_latch)
		// {
		// return 2; //when latch is high -- data2 = 1, data1 = 0
		// }
		//
		// int deviceidx, deviceindex0, deviceindex1;
		// int mask = (portnumber == 0 ? 0x40 : 0x80);
		//
		// if ((CPU.cpu.pio() & mask) != 0)
		// {
		// deviceidx = p.counter0;
		// if (deviceidx >= 16)
		// {
		// return 3;
		// }
		// p.counter0++;
		//
		// deviceindex0 = 0; //controller 1
		// deviceindex1 = 1; //controller 2
		// }
		// else
		// {
		// deviceidx = p.counter1;
		// if (deviceidx >= 16)
		// {
		// return 3;
		// }
		// p.counter1++;
		//
		// deviceindex0 = 2; //controller 3
		// deviceindex1 = 3; //controller 4
		// }
		//
		// return (byte)((SnesSystem.system.getInterface().input_poll(portnumber, p.device.ordinal(), deviceindex0, deviceidx) << 0)
		// | (SnesSystem.system.getInterface().input_poll(portnumber, p.device.ordinal(), deviceindex1, deviceidx) << 1));
		} //case Device::Multitap
		case Mouse:
		{
		// if (p.counter0 >= 32)
		// {
		// return 1;
		// }
		//
		// int position_x = SnesSystem.system.getInterface().input_poll(portnumber, p.device.ordinal(), 0, MouseID_X); //-n = left, 0 = center, +n = right
		// int position_y = SnesSystem.system.getInterface().input_poll(portnumber, p.device.ordinal(), 0, MouseID_Y); //-n = up, 0 = center, +n = right
		//
		// boolean direction_x = position_x < 0; //0 = right, 1 = left
		// boolean direction_y = position_y < 0; //0 = down, 1 = up
		//
		// if (position_x < 0)
		// {
		// position_x = -position_x; //abs(position_x)
		// }
		// if (position_y < 0)
		// {
		// position_y = -position_y; //abs(position_x)
		// }
		//
		// position_x = Math.min(127, position_x); //range = 0 - 127
		// position_y = Math.min(127, position_y); //range = 0 - 127
		//
		// switch (p.counter0++)
		// {
		// default:
		// case 0:
		return 0;
		// case 1:
		// return 0;
		// case 2:
		// return 0;
		// case 3:
		// return 0;
		// case 4:
		// return 0;
		// case 5:
		// return 0;
		// case 6:
		// return 0;
		// case 7:
		// return 0;
		//
		// case 8:
		// return (byte)SnesSystem.system.getInterface().input_poll(portnumber, p.device.ordinal(), 0, MouseID_Right);
		// case 9:
		// return (byte)SnesSystem.system.getInterface().input_poll(portnumber, p.device.ordinal(), 0, MouseID_Left);
		// case 10:
		// return 0; //speed (0 = slow, 1 = normal, 2 = fast, 3 = unused)
		// case 11:
		// return 0; // ||
		//
		// case 12:
		// return 0; //signature
		// case 13:
		// return 0; // ||
		// case 14:
		// return 0; // ||
		// case 15:
		// return 1; // ||
		//
		// case 16:
		// return (byte)((direction_y ? 1 : 0) & 1);
		// case 17:
		// return (byte)((position_y >> 6) & 1);
		// case 18:
		// return (byte)((position_y >> 5) & 1);
		// case 19:
		// return (byte)((position_y >> 4) & 1);
		// case 20:
		// return (byte)((position_y >> 3) & 1);
		// case 21:
		// return (byte)((position_y >> 2) & 1);
		// case 22:
		// return (byte)((position_y >> 1) & 1);
		// case 23:
		// return (byte)((position_y >> 0) & 1);
		//
		// case 24:
		// return (byte)((direction_x ? 1 : 0) & 1);
		// case 25:
		// return (byte)((position_x >> 6) & 1);
		// case 26:
		// return (byte)((position_x >> 5) & 1);
		// case 27:
		// return (byte)((position_x >> 4) & 1);
		// case 28:
		// return (byte)((position_x >> 3) & 1);
		// case 29:
		// return (byte)((position_x >> 2) & 1);
		// case 30:
		// return (byte)((position_x >> 1) & 1);
		// case 31:
		// return (byte)((position_x >> 0) & 1);
		// }
		} //case Device::Mouse
		case SuperScope:
		{
		// if (portnumber == 0)
		// {
		// break; //Super Scope in port 1 not supported ...
		// }
		// if (p.counter0 >= 8)
		// {
		// return 1;
		// }
		//
		// if (p.counter0 == 0)
		// {
		// //turbo is a switch; toggle is edge sensitive
		// boolean turbo = (SnesSystem.system.getInterface().input_poll(portnumber, p.device.ordinal(), 0, SuperScopeID_Turbo)) != 0;
		// if (turbo && !p.superscope.turbolock)
		// {
		// p.superscope.turbo = !p.superscope.turbo; //toggle state
		// p.superscope.turbolock = true;
		// }
		// else if (!turbo)
		// {
		// p.superscope.turbolock = false;
		// }
		//
		// //trigger is a button
		// //if turbo is active, trigger is level sensitive, otherwise it is edge sensitive
		// p.superscope.trigger = false;
		// boolean trigger = (SnesSystem.system.getInterface().input_poll(portnumber, p.device.ordinal(), 0, SuperScopeID_Trigger)) != 0;
		// if (trigger && (p.superscope.turbo || !p.superscope.triggerlock))
		// {
		// p.superscope.trigger = true;
		// p.superscope.triggerlock = true;
		// }
		// else if (!trigger)
		// {
		// p.superscope.triggerlock = false;
		// }
		//
		// //cursor is a button; it is always level sensitive
		// p.superscope.cursor = (SnesSystem.system.getInterface().input_poll(portnumber, p.device.ordinal(), 0, SuperScopeID_Cursor)) != 0;
		//
		// //pause is a button; it is always edge sensitive
		// p.superscope.pause = false;
		// boolean pause = (SnesSystem.system.getInterface().input_poll(portnumber, p.device.ordinal(), 0, SuperScopeID_Pause)) != 0;
		// if (pause && !p.superscope.pauselock)
		// {
		// p.superscope.pause = true;
		// p.superscope.pauselock = true;
		// }
		// else if (!pause)
		// {
		// p.superscope.pauselock = false;
		// }
		//
		// p.superscope.offscreen =
		// p.superscope.x < 0 || p.superscope.x >= 256
		// || p.superscope.y < 0 || p.superscope.y >= (PPU.ppu.overscan() ? 240 : 225);
		// }
		//
		// switch (p.counter0++)
		// {
		// case 0:
		// return (byte)(p.superscope.trigger ? 1 : 0);
		// case 1:
		// return (byte)(p.superscope.cursor ? 1 : 0);
		// case 2:
		// return (byte)(p.superscope.turbo ? 1 : 0);
		// case 3:
		// return (byte)(p.superscope.pause ? 1 : 0);
		// case 4:
		// return 0;
		// case 5:
		// return 0;
		// case 6:
		// return (byte)(p.superscope.offscreen ? 1 : 0);
		// case 7:
		// return 0; //noise (1 = yes)
		// default:
		return 0;
		// }
		} //case Device::SuperScope
		case Justifier:
		case Justifiers:
		{
		// if (portnumber == 0)
		// {
		// break; //Justifier in port 1 not supported ...
		// }
		// if (p.counter0 >= 32)
		// {
		// return 1;
		// }
		//
		// if (p.counter0 == 0)
		// {
		// p.justifier.trigger1 = (SnesSystem.system.getInterface().input_poll(portnumber, p.device.ordinal(), 0, JustifierID_Trigger)) != 0;
		// p.justifier.start1 = (SnesSystem.system.getInterface().input_poll(portnumber, p.device.ordinal(), 0, JustifierID_Start)) != 0;
		//
		// if (p.device == Device.Justifiers)
		// {
		// p.justifier.trigger2 = (SnesSystem.system.getInterface().input_poll(portnumber, p.device.ordinal(), 1, JustifierID_Trigger)) != 0;
		// p.justifier.start2 = (SnesSystem.system.getInterface().input_poll(portnumber, p.device.ordinal(), 1, JustifierID_Start)) != 0;
		// }
		// else
		// {
		// p.justifier.x2 = -1;
		// p.justifier.y2 = -1;
		//
		// p.justifier.trigger2 = false;
		// p.justifier.start2 = false;
		// }
		// }
		//
		// switch (p.counter0++)
		// {
		// case 0:
		// return 0;
		// case 1:
		// return 0;
		// case 2:
		// return 0;
		// case 3:
		// return 0;
		// case 4:
		// return 0;
		// case 5:
		// return 0;
		// case 6:
		// return 0;
		// case 7:
		// return 0;
		// case 8:
		// return 0;
		// case 9:
		// return 0;
		// case 10:
		// return 0;
		// case 11:
		// return 0;
		//
		// case 12:
		// return 1; //signature
		// case 13:
		// return 1; // ||
		// case 14:
		// return 1; // ||
		// case 15:
		// return 0; // ||
		//
		// case 16:
		// return 0;
		// case 17:
		// return 1;
		// case 18:
		// return 0;
		// case 19:
		// return 1;
		// case 20:
		// return 0;
		// case 21:
		// return 1;
		// case 22:
		// return 0;
		// case 23:
		// return 1;
		//
		// case 24:
		// return (byte)(p.justifier.trigger1 ? 1 : 0);
		// case 25:
		// return (byte)(p.justifier.trigger2 ? 1 : 0);
		// case 26:
		// return (byte)(p.justifier.start1 ? 1 : 0);
		// case 27:
		// return (byte)(p.justifier.start2 ? 1 : 0);
		// case 28:
		// return (byte)(p.justifier.active ? 1 : 0);
		//
		// case 29:
		// return 0;
		// case 30:
		// return 0;
		// case 31:
		// return 0;
		// default:
		return 0;
		// }
		} //case Device::Justifier(s)
		case None: return 0;
		} // switch(p.device)

		// no device connected
		return 0;
	}

	@Override
	public void write8bit(int port, byte data)
	{
		if (port == 0)
		{
			boolean old_latch = joypad_strobe_latch;
			boolean new_latch = (data & 1) != 0;
			joypad_strobe_latch = new_latch;
			if (old_latch != new_latch)
			{
				poll();
			}
		}
		else
		{
			poll();
		}
	}

	private void port_set_device(int portnumber, Device device)
	{
		Port p = port[portnumber];

		p.device = device;
		p.counter0 = 0;
		p.counter1 = 0;

		// set iobit to true if device is capable of latching PPU counters
		// iobit = port[1].device == Device.SuperScope
		// || port[1].device == Device.Justifier
		// || port[1].device == Device.Justifiers;
		// latchx = -1;
		// latchy = -1;
		//
		// if (device == Device.SuperScope)
		// {
		// p.superscope.x = 256 / 2;
		// p.superscope.y = 240 / 2;
		//
		// p.superscope.trigger = false;
		// p.superscope.cursor = false;
		// p.superscope.turbo = false;
		// p.superscope.pause = false;
		// p.superscope.offscreen = false;
		//
		// p.superscope.turbolock = false;
		// p.superscope.triggerlock = false;
		// p.superscope.pauselock = false;
		// }
		// else if (device == Device.Justifier)
		// {
		// p.justifier.active = false;
		// p.justifier.x1 = 256 / 2;
		// p.justifier.y1 = 240 / 2;
		// p.justifier.x2 = -1;
		// p.justifier.y2 = -1;
		//
		// p.justifier.trigger1 = false;
		// p.justifier.trigger2 = false;
		// p.justifier.start1 = false;
		// p.justifier.start2 = false;
		// }
		// else if (device == Device.Justifiers)
		// {
		// p.justifier.active = false;
		// p.justifier.x1 = 256 / 2 - 16;
		// p.justifier.y1 = 240 / 2;
		// p.justifier.x2 = 256 / 2 + 16;
		// p.justifier.y2 = 240 / 2;
		//
		// p.justifier.trigger1 = false;
		// p.justifier.trigger2 = false;
		// p.justifier.start1 = false;
		// p.justifier.start2 = false;
		// }
	}

	private void init()
	{
	}

	private void poll()
	{
		port[0].counter0 = 0;
		port[0].counter1 = 0;
		port[1].counter0 = 0;
		port[1].counter1 = 0;

		port[1].justifier.active = !port[1].justifier.active;
	}

	@Override
	public void clock(long clocks)
	{
		// SnesSystem.system.getInterface().input_poll();
		// Port p = port[1];
		//
		// switch (p.device)
		// {
		// case SuperScope:
		// {
		// int x = SnesSystem.system.getInterface().input_poll(1, p.device.ordinal(), 0, SuperScopeID_X);
		// int y = SnesSystem.system.getInterface().input_poll(1, p.device.ordinal(), 0, SuperScopeID_Y);
		// x += p.superscope.x;
		// y += p.superscope.y;
		// p.superscope.x = Math.max(-16, Math.min(256 + 16, x));
		// p.superscope.y = Math.max(-16, Math.min(240 + 16, y));
		//
		// latchx = p.superscope.x & 0xFFFF;
		// latchy = p.superscope.y & 0xFFFF;
		// }
		// break;
		// case Justifier:
		// case Justifiers:
		// {
		// int x1 = SnesSystem.system.getInterface().input_poll(1, p.device.ordinal(), 0, JustifierID_X);
		// int y1 = SnesSystem.system.getInterface().input_poll(1, p.device.ordinal(), 0, JustifierID_Y);
		// x1 += p.justifier.x1;
		// y1 += p.justifier.y1;
		// p.justifier.x1 = Math.max(-16, Math.min(256 + 16, x1));
		// p.justifier.y1 = Math.max(-16, Math.min(240 + 16, y1));
		//
		// int x2 = SnesSystem.system.getInterface().input_poll(1, p.device.ordinal(), 1, JustifierID_X);
		// int y2 = SnesSystem.system.getInterface().input_poll(1, p.device.ordinal(), 1, JustifierID_Y);
		// x2 += p.justifier.x2;
		// y2 += p.justifier.y2;
		// p.justifier.x2 = Math.max(-16, Math.min(256 + 16, x2));
		// p.justifier.y2 = Math.max(-16, Math.min(240 + 16, y2));
		//
		// if ((p.justifier.active ? 1 : 0) == 0)
		// {
		// latchx = p.justifier.x1 & 0xFFFF;
		// latchy = p.justifier.y1 & 0xFFFF;
		// }
		// else
		// {
		// latchx = (p.device == Device.Justifiers ? (p.justifier.x2 & 0xFFFF) : -1);
		// latchy = (p.device == Device.Justifiers ? (p.justifier.y2 & 0xFFFF) : -1);
		// }
		// }
		// break;
		// }

		// if (latchy < 0 || latchy >= (display_bus.read8bit(1) != 0 ? 240 : 225) || latchx < 0 || latchx >= 256)
		// {
		// //cursor is offscreen, set to invalid position so counters are not latched
		// latchx = ~0;
		// latchy = ~0;
		// }
		// else
		// {
		// //cursor is onscreen
		// latchx += 40; //offset trigger position to simulate hardware latching delay
		// latchx <<= 2; //dot -> clock conversion
		// latchx += 2; //align trigger on half-dot ala interrupts (speed optimization for sCPU::add_clocks)
		// }
	}

	// light guns (Super Scope, Justifier(s)) strobe IOBit whenever the CRT
	// beam cannon is detected. this needs to be tested at the cycle level
	// (hence inlining here for speed) to avoid 'dead space' during DRAM refresh.
	// iobit is updated during port_set_device(),
	// latchx, latchy are updated during update() (once per frame)

	// public void tick()
	// { //only test if Super Scope or Justifier is connected
	// if (iobit && CPU.cpu.getPPUCounter().vcounter() == latchy && CPU.cpu.getPPUCounter().hcounter() == latchx)
	// {
	// PPU.ppu.latch_counters();
	// }
	// }

	// private boolean iobit;
	// private int latchx, latchy;

	private boolean joypad_strobe_latch;

	private Port[] port = new Port[2];

	public Input()
	{
		for (int i = 0; i < port.length; i++)
		{
			port[i] = new Port();
		}
		init();
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
}
