/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.cpu;

public class Regs implements java.io.Serializable
{
	public Reg24 pc = new Reg24();
	public Reg16[] r = new Reg16[6];
	public Reg16 a, x, y, z, s, d;
	public Flag p = new Flag();
	public int db;
	public boolean e;

	public boolean irq; // IRQ pin (0 = low, 1 = trigger)
	public boolean wai; // raised during wai, cleared after interrupt triggered
	public byte mdr; // memory data register

	public Regs()
	{
		for (int i = 0; i < r.length; i++)
		{
			r[i] = new Reg16();
		}

		a = r[0];
		x = r[1];
		y = r[2];
		z = r[3];
		s = r[4];
		d = r[5];

		db = 0;
		e = false;
		irq = false;
		wai = false;
		mdr = 0;

		z.set(0);
	}
}
