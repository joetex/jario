/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.cpu;

public class Status implements java.io.Serializable
{
	public boolean nmi_valid;
	public boolean nmi_line;
	public boolean nmi_transition;
	public boolean nmi_pending;

	public boolean irq_valid;
	public boolean irq_line;
	public boolean irq_transition;
	public boolean irq_pending;

	public boolean irq_lock;
	public boolean hdma_pending;

	public int wram_addr;

	public boolean nmi_enabled;
	public boolean virq_enabled;
	public boolean hirq_enabled;
	public boolean auto_joypad_poll_enabled;

	public int pio;

	public int wrmpya;
	public int wrmpyb;
	public int wrdiva;
	public int wrdivb;

	public int htime;
	public int vtime;

	public int rom_speed;

	public int rddiv;
	public int rdmpy;

	public int joy1l, joy1h;
	public int joy2l, joy2h;
	public int joy3l, joy3h;
	public int joy4l, joy4h;
}
