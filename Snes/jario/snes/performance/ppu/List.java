/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.ppu;

public class List implements java.io.Serializable
{
	public int width;
	public int height;
	public int x;
	public int y;
	public int character;
	public boolean use_nameselect;
	public boolean vflip;
	public boolean hflip;
	public int palette;
	public int priority;
	public boolean size;
}
