/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.smp;

public class Status implements java.io.Serializable
{
	// timing
	public int clock_counter;
	public int dsp_counter;
	public int timer_step;

	// $00f0
	public int clock_speed;
	public int timer_speed;
	public boolean timers_enabled;
	public boolean ram_disabled;
	public boolean ram_writable;
	public boolean timers_disabled;

	// $00f1
	public boolean iplrom_enabled;

	// $00f2
	public int dsp_addr;

	// $00f8,$00f9
	public int ram0;
	public int ram1;
}
