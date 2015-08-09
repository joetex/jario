/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.input;

public class Justifier implements java.io.Serializable
{
	public boolean active;

	public int x1, x2;
	public int y1, y2;

	public boolean trigger1, trigger2;
	public boolean start1, start2;
}
