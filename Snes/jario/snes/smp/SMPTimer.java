/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.smp;

public class SMPTimer implements java.io.Serializable
{
	public int stage0_ticks;
	public int stage1_ticks;
	public int stage2_ticks;
	public int stage3_ticks;
	public boolean current_line;
	public boolean enabled;
	public int target;

	public void tick()
	{ // stage 0 increment
		stage0_ticks += (smp.status.timer_step & 0xFF);
		if (stage0_ticks < timer_frequency) { return; }
		stage0_ticks -= (timer_frequency & 0xFF);

		// stage 1 increment
		stage1_ticks ^= 1;
		sync_stage1();
	}

	public void sync_stage1()
	{
		boolean new_line = stage1_ticks != 0;
		if (smp.status.timers_enabled == false)
		{
			new_line = false;
		}
		if (smp.status.timers_disabled == true)
		{
			new_line = false;
		}

		boolean old_line = current_line;
		current_line = new_line;
		if (old_line != true || new_line != false) { return; } // only pulse on 1->0 transition
		// stage 2 increment
		if (enabled == false) { return; }
		stage2_ticks++;
		if (stage2_ticks != target) { return; }

		// stage 3 increment
		stage2_ticks = 0;
		stage3_ticks++;
		stage3_ticks &= 15;
	}

	private int timer_frequency;
	private SMP smp;

	public SMPTimer(int timer_frequency_, SMP smp)
	{
		timer_frequency = timer_frequency_;
		this.smp = smp;
	}
}
