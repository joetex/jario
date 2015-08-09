/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.input;

import jario.hardware.Bus16bit;
import jario.snes.input.Input.Device;

public class Port implements java.io.Serializable
{
	public Device device;
	public int counter0; // read counters
	public int counter1;
	
	public Bus16bit bus;

	public Superscope superscope = new Superscope();
	public Justifier justifier = new Justifier();
}
