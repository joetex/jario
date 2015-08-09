/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.cpu;

public class Reg24 implements java.io.Serializable
{
	private int d;
	private int bh;

	public int get() { return (d & 0xFFFF) | (bh & 0xFF) << 16; }
	public void set(int i) { d = i & 0xFFFF; bh = (i >> 16) & 0xFF; }

	public int w() { return d; }
	public void w(int i) { d = i & 0xFFFF; }

	public int l() { return d & 0xFF; }
	public void l(int i) { d = (d & 0xFF00) | (i & 0xFF); }

	public int h() { return (d >> 8) & 0xFF; }
	public void h(int i) { d = (d & 0x00FF) | ((i & 0xFF) << 8); }

	public int b() { return bh & 0xFF; }
	public void b(int i) { bh = i & 0xFF; }

	public Reg24()
	{
		d = 0;
		bh = 0;
	}
}
