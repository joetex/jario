/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.input;

public class Superscope implements java.io.Serializable
{ 
	public int x, y;

	public boolean trigger;
	public boolean cursor;
	public boolean turbo;
	public boolean pause;
	public boolean offscreen;

	public boolean turbolock;
	public boolean triggerlock;
	public boolean pauselock;
}
