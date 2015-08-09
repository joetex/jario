/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.memory;

import jario.hardware.Bus8bit;

public class Page implements java.io.Serializable
{
	public transient Bus8bit access;
	public int offset;
}
