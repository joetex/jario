/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.configuration;

public class CPU implements java.io.Serializable
{
	public int version;
	public int ntsc_frequency;
	public int pal_frequency;
	public int wram_init_value;
}
