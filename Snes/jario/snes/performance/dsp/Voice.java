/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.dsp;

import jario.snes.performance.dsp.SPCDSP.EnvMode;

public class Voice implements java.io.Serializable
{
	public int[] buf = new int[SPCDSP.brr_buf_size * 2]; // decoded samples (twice the size to simplify wrap handling)
	public int buf_pos; // place in buffer where next samples will be decoded
	public int interp_pos; // relative fractional position in sample (0x1000 = 1.0)
	public int brr_addr; // address of current BRR block
	public int brr_offset; // current decoding offset in BRR block
	public int[] regs; // pointer to voice's DSP registers
	public int r;
	public int vbit; // bitmask for voice: 0x01 for voice 0, 0x02 for voice 1, etc.
	public int kon_delay; // KON delay/current setup phase
	public EnvMode env_mode;
	public int env; // current envelope level
	public int hidden_env; // used by GAIN mode 7, very obscure quirk
	public int t_envx_out;
}
