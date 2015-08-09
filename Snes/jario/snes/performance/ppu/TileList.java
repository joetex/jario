/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.ppu;

public class TileList implements java.io.Serializable
{
	public int x;
	public int y;
	public int priority;
	public int palette;
	public int tile;
	public boolean hflip;
}
