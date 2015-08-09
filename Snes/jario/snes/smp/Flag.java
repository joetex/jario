/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.smp;

public class Flag implements java.io.Serializable
{
	public boolean n, v, p, b, h, i, z, c;

	public int get()
	{
		return (((n ? 1 : 0) << 7) + ((v ? 1 : 0) << 6) + ((p ? 1 : 0) << 5) + ((b ? 1 : 0) << 4)
				+ ((h ? 1 : 0) << 3) + ((i ? 1 : 0) << 2) + ((z ? 1 : 0) << 1) + ((c ? 1 : 0) << 0));
	}

	public void set(int data)
	{
		n = (data & 0x80) != 0;
		v = (data & 0x40) != 0;
		p = (data & 0x20) != 0;
		b = (data & 0x10) != 0;
		h = (data & 0x08) != 0;
		i = (data & 0x04) != 0;
		z = (data & 0x02) != 0;
		c = (data & 0x01) != 0;
	}

	public Flag()
	{
		n = v = p = b = h = i = z = c = false;
	}
}
