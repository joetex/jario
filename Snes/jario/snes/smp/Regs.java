/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.smp;

public class Regs implements java.io.Serializable
{
	public int pc;
	public int[] r = new int[4];
	public int a, x, y, sp;
	public RegYA ya;
	public Flag p = new Flag();

	public Regs()
	{
		a = 0;
		x = 1;
		y = 2;
		sp = 3;
		ya = new RegYA(r, 2, 0);
	}
}
