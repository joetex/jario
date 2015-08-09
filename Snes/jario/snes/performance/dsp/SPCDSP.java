/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.dsp;

public class SPCDSP implements java.io.Serializable
{
	// Setup

	private static final int Clamp16(int io)
	{
		if ((short) io != io) { return (io >> 31) ^ 0x7FFF; }
		return io;
	}

	// Initializes DSP and has it use the 64K RAM provided
	public void init(byte[] ram_64k)
	{
		m.ram = ram_64k;
		mute_voices(0);
		disable_surround(false);
		set_output(null, 0);
		reset();
	}

	// Sets destination for output samples. If out is NULL or out_size is 0,
	// doesn't generate any.
	public void set_output(short[] _out, int size)
	{
		assert (size & 1) == 0; // must be even
		if (_out == null)
		{
			_out = m.extra;
			size = extra_size;
		}
		m._out = _out;
		m._out_Offset = 0;
	}

	// Number of samples written to output since it was last set, always
	// a multiple of 2. Undefined if more samples were generated than
	// output buffer could hold.
	public int sample_count()
	{
		return m._out_Offset;
	}

	// Emulation

	public static int[] initial_regs = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xE0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

	// Resets DSP to power-on state
	public void reset()
	{
		load(initial_regs);
	}

	// Emulates pressing reset switch on SNES
	public void soft_reset()
	{
		m.regs[GlobalReg_flg] = 0xE0;
		soft_reset_common();
	}

	// Reads/writes DSP registers. For accuracy, you must first call run()
	// to catch the DSP up to present.
	public byte read(int addr)
	{
		assert addr < register_count;
		return (byte) m.regs[addr];
	}

	public void write(int addr, byte data)
	{
		assert addr < register_count;

		m.regs[addr] = data & 0xFF;
		switch (addr & 0x0F)
		{
		case VoiceReg_envx:
			m.envx_buf = data & 0xFF;
			break;
		case VoiceReg_outx:
			m.outx_buf = data & 0xFF;
			break;
		case 0x0C:
			if (addr == GlobalReg_kon)
			{
				m.new_kon = data & 0xFF;
			}
			if (addr == GlobalReg_endx)
			{
				// always cleared, regardless of data written
				m.endx_buf = 0;
				m.regs[GlobalReg_endx] = 0;
			}
			break;
		}
	}

	private static final boolean Phase(int n, int clocks_remain)
	{
		return (n != 0) && (--clocks_remain == 0);
	}

	// Runs DSP for specified number of clocks (~1024000 per second). Every 32 clocks
	// a pair of samples is be generated.
	public void run(int clocks_remain)
	{
		assert clocks_remain > 0;

		int phase = m.phase;
		m.phase = (phase + clocks_remain) & 31;
		do
		{
			switch (phase)
			{
			case 0:
				voice_V5(m.voices[0]);
				voice_V2(m.voices[1]);
				if (Phase(1, clocks_remain))
					break;
			case 1:
				voice_V6(m.voices[0]);
				voice_V3(m.voices[1]);
				if (Phase(2, clocks_remain))
					break;
			case 2:
				voice_V7_V4_V1(m.voices, 0);
				if (Phase(3, clocks_remain))
					break;
			case 3:
				voice_V8_V5_V2(m.voices, 0);
				if (Phase(4, clocks_remain))
					break;
			case 4:
				voice_V9_V6_V3(m.voices, 0);
				if (Phase(5, clocks_remain))
					break;
			case 5:
				voice_V7_V4_V1(m.voices, 1);
				if (Phase(6, clocks_remain))
					break;
			case 6:
				voice_V8_V5_V2(m.voices, 1);
				if (Phase(7, clocks_remain))
					break;
			case 7:
				voice_V9_V6_V3(m.voices, 1);
				if (Phase(8, clocks_remain))
					break;
			case 8:
				voice_V7_V4_V1(m.voices, 2);
				if (Phase(9, clocks_remain))
					break;
			case 9:
				voice_V8_V5_V2(m.voices, 2);
				if (Phase(10, clocks_remain))
					break;
			case 10:
				voice_V9_V6_V3(m.voices, 2);
				if (Phase(11, clocks_remain))
					break;
			case 11:
				voice_V7_V4_V1(m.voices, 3);
				if (Phase(12, clocks_remain))
					break;
			case 12:
				voice_V8_V5_V2(m.voices, 3);
				if (Phase(13, clocks_remain))
					break;
			case 13:
				voice_V9_V6_V3(m.voices, 3);
				if (Phase(14, clocks_remain))
					break;
			case 14:
				voice_V7_V4_V1(m.voices, 4);
				if (Phase(15, clocks_remain))
					break;
			case 15:
				voice_V8_V5_V2(m.voices, 4);
				if (Phase(16, clocks_remain))
					break;
			case 16:
				voice_V9_V6_V3(m.voices, 4);
				if (Phase(17, clocks_remain))
					break;
			case 17:
				voice_V1(m.voices[0]);
				voice_V7(m.voices[5]);
				voice_V4(m.voices[6]);
				if (Phase(18, clocks_remain))
					break;
			case 18:
				voice_V8_V5_V2(m.voices, 5);
				if (Phase(19, clocks_remain))
					break;
			case 19:
				voice_V9_V6_V3(m.voices, 5);
				if (Phase(20, clocks_remain))
					break;
			case 20:
				voice_V1(m.voices[1]);
				voice_V7(m.voices[6]);
				voice_V4(m.voices[7]);
				if (Phase(21, clocks_remain))
					break;
			case 21:
				voice_V8(m.voices[6]);
				voice_V5(m.voices[7]);
				voice_V2(m.voices[0]);
				if (Phase(22, clocks_remain))
					break;
			case 22:
				voice_V3a(m.voices[0]);
				voice_V9(m.voices[6]);
				voice_V6(m.voices[7]);
				echo_22();
				if (Phase(23, clocks_remain))
					break;
			case 23:
				voice_V7(m.voices[7]);
				echo_23();
				if (Phase(24, clocks_remain))
					break;
			case 24:
				voice_V8(m.voices[7]);
				echo_24();
				if (Phase(25, clocks_remain))
					break;
			case 25:
				voice_V3b(m.voices[0]);
				voice_V9(m.voices[7]);
				echo_25();
				if (Phase(26, clocks_remain))
					break;
			case 26:
				echo_26();
				if (Phase(27, clocks_remain))
					break;
			case 27:
				misc_27();
				echo_27();
				if (Phase(28, clocks_remain))
					break;
			case 28:
				misc_28();
				echo_28();
				if (Phase(29, clocks_remain))
					break;
			case 29:
				misc_29();
				echo_29();
				if (Phase(30, clocks_remain))
					break;
			case 30:
				misc_30();
				voice_V3c(m.voices[0]);
				echo_30();
				if (Phase(31, clocks_remain))
					break;
			case 31:
				voice_V4(m.voices[0]);
				voice_V1(m.voices[2]);
				break;
			}
		} while (--clocks_remain != 0);
	}

	// Sound control

	// Mutes voices corresponding to non-zero bits in mask (issues repeated KOFF
	// events).
	// Reduces emulation accuracy.
	public static final int voice_count = 8;

	public void mute_voices(int mask)
	{
		m.mute_mask = mask;
	}

	// State

	// Resets DSP and uses supplied values to initialize registers
	public static final int register_count = 128;

	public void load(int[] regs)
	{
		System.arraycopy(regs, 0, m.regs, 0, m.regs.length);

		// TODO: What the HELL is this doing?
		// memset( &m.regs [register_count], 0, offsetof (state_t,ram) -
		// register_count );

		// Internal state
		for (int i = voice_count; --i >= 0;)
		{
			Voice v = m.voices[i];
			v.brr_offset = 1;
			v.vbit = 1 << i;
			v.regs = m.regs;
			v.r = i * 0x10;
		}
		m.new_kon = m.regs[GlobalReg_kon] & 0xFF;
		m.t_dir = m.regs[GlobalReg_dir] & 0xFF;
		m.t_esa = m.regs[GlobalReg_esa] & 0xFF;

		soft_reset_common();
	}

	// Returns non-zero if new key-on events occurred since last call
	public boolean check_kon()
	{
		boolean old = m.kon_check;
		m.kon_check = false;
		return old;
	}

	// DSP register addresses

	// Global registers
	public static final int GlobalReg_mvoll = 0x0c;
	public static final int GlobalReg_mvolr = 0x1c;
	public static final int GlobalReg_evoll = 0x2c;
	public static final int GlobalReg_evolr = 0x3c;
	public static final int GlobalReg_kon = 0x4c;
	public static final int GlobalReg_koff = 0x5c;
	public static final int GlobalReg_flg = 0x6c;
	public static final int GlobalReg_endx = 0x7c;
	public static final int GlobalReg_efb = 0x0d;
	public static final int GlobalReg_pmon = 0x2d;
	public static final int GlobalReg_non = 0x3d;
	public static final int GlobalReg_eon = 0x4d;
	public static final int GlobalReg_dir = 0x5d;
	public static final int GlobalReg_esa = 0x6d;
	public static final int GlobalReg_edl = 0x7d;
	public static final int GlobalReg_fir = 0x0f;

	// Voice registers
	public static final int VoiceReg_voll = 0x00;
	public static final int VoiceReg_volr = 0x01;
	public static final int VoiceReg_pitchl = 0x02;
	public static final int VoiceReg_pitchh = 0x03;
	public static final int VoiceReg_srcn = 0x04;
	public static final int VoiceReg_adsr0 = 0x05;
	public static final int VoiceReg_adsr1 = 0x06;
	public static final int VoiceReg_gain = 0x07;
	public static final int VoiceReg_envx = 0x08;
	public static final int VoiceReg_outx = 0x09;

	public static final int extra_size = 16;

	public void disable_surround(boolean disable)
	{
		// not supported
	}

	public static final int echo_hist_size = 8;

	public enum EnvMode
	{
		release, attack, decay, sustain
	}

	public static final int brr_buf_size = 12;

	private static final int brr_block_size = 9;

	public State m = new State();

	private void init_counter()
	{
		m.counter = 0;
	}

	private static final int simple_counter_range = 2048 * 5 * 3; // 30720

	private final void run_counters()
	{
		if (--m.counter < 0)
		{
			m.counter = simple_counter_range - 1;
		}
	}

	private static int[] counter_rates = { simple_counter_range + 1 /* never fires */, 2048, 1536, 1280, 1024, 768, 640, 512, 384, 320, 256, 192, 160, 128, 96, 80, 64, 48, 40, 32, 24, 20, 16, 12, 10, 8, 6, 5, 4, 3, 2, 1 };
	private static int[] counter_offsets = { 1, 0, 1040, 536, 0, 1040, 536, 0, 1040, 536, 0, 1040, 536, 0, 1040, 536, 0, 1040, 536, 0, 1040, 536, 0, 1040, 536, 0, 1040, 536, 0, 1040, 0, 0 };

	private int read_counter(int rate)
	{
		return (m.counter + counter_offsets[rate]) % counter_rates[rate];
	}

	private static short[] gauss =
	{
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2,
			2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 5, 5, 5, 5,
			6, 6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9, 9, 10, 10, 10,
			11, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 15, 16, 16, 17, 17,
			18, 19, 19, 20, 20, 21, 21, 22, 23, 23, 24, 24, 25, 26, 27, 27,
			28, 29, 29, 30, 31, 32, 32, 33, 34, 35, 36, 36, 37, 38, 39, 40,
			41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56,
			58, 59, 60, 61, 62, 64, 65, 66, 67, 69, 70, 71, 73, 74, 76, 77,
			78, 80, 81, 83, 84, 86, 87, 89, 90, 92, 94, 95, 97, 99, 100, 102,
			104, 106, 107, 109, 111, 113, 115, 117, 118, 120, 122, 124, 126, 128, 130, 132,
			134, 137, 139, 141, 143, 145, 147, 150, 152, 154, 156, 159, 161, 163, 166, 168,
			171, 173, 175, 178, 180, 183, 186, 188, 191, 193, 196, 199, 201, 204, 207, 210,
			212, 215, 218, 221, 224, 227, 230, 233, 236, 239, 242, 245, 248, 251, 254, 257,
			260, 263, 267, 270, 273, 276, 280, 283, 286, 290, 293, 297, 300, 304, 307, 311,
			314, 318, 321, 325, 328, 332, 336, 339, 343, 347, 351, 354, 358, 362, 366, 370,
			374, 378, 381, 385, 389, 393, 397, 401, 405, 410, 414, 418, 422, 426, 430, 434,
			439, 443, 447, 451, 456, 460, 464, 469, 473, 477, 482, 486, 491, 495, 499, 504,
			508, 513, 517, 522, 527, 531, 536, 540, 545, 550, 554, 559, 563, 568, 573, 577,
			582, 587, 592, 596, 601, 606, 611, 615, 620, 625, 630, 635, 640, 644, 649, 654,
			659, 664, 669, 674, 678, 683, 688, 693, 698, 703, 708, 713, 718, 723, 728, 732,
			737, 742, 747, 752, 757, 762, 767, 772, 777, 782, 787, 792, 797, 802, 806, 811,
			816, 821, 826, 831, 836, 841, 846, 851, 855, 860, 865, 870, 875, 880, 884, 889,
			894, 899, 904, 908, 913, 918, 923, 927, 932, 937, 941, 946, 951, 955, 960, 965,
			969, 974, 978, 983, 988, 992, 997, 1001, 1005, 1010, 1014, 1019, 1023, 1027, 1032, 1036,
			1040, 1045, 1049, 1053, 1057, 1061, 1066, 1070, 1074, 1078, 1082, 1086, 1090, 1094, 1098, 1102,
			1106, 1109, 1113, 1117, 1121, 1125, 1128, 1132, 1136, 1139, 1143, 1146, 1150, 1153, 1157, 1160,
			1164, 1167, 1170, 1174, 1177, 1180, 1183, 1186, 1190, 1193, 1196, 1199, 1202, 1205, 1207, 1210,
			1213, 1216, 1219, 1221, 1224, 1227, 1229, 1232, 1234, 1237, 1239, 1241, 1244, 1246, 1248, 1251,
			1253, 1255, 1257, 1259, 1261, 1263, 1265, 1267, 1269, 1270, 1272, 1274, 1275, 1277, 1279, 1280,
			1282, 1283, 1284, 1286, 1287, 1288, 1290, 1291, 1292, 1293, 1294, 1295, 1296, 1297, 1297, 1298,
			1299, 1300, 1300, 1301, 1302, 1302, 1303, 1303, 1303, 1304, 1304, 1304, 1304, 1304, 1305, 1305,
	};

	private int interpolate(Voice v)
	{ // Make pointers into gaussian based on fractional position between
		// samples
		int offset = v.interp_pos >> 4 & 0xFF;
		short[] fwd = gauss;
		int fwd_Offset = 255 - offset;
		short[] rev = gauss;
		int rev_Offset = offset;

		int[] _in = v.buf;
		int _in_Offset = (v.interp_pos >> 12) + v.buf_pos;
		int _out;
		_out = (fwd[fwd_Offset + 0] * _in[_in_Offset + 0]) >> 11;
		_out += (fwd[fwd_Offset + 256] * _in[_in_Offset + 1]) >> 11;
		_out += (rev[rev_Offset + 256] * _in[_in_Offset + 2]) >> 11;
		_out = (short) _out;
		_out += (rev[rev_Offset + 0] * _in[_in_Offset + 3]) >> 11;

		_out = Clamp16(_out);
		_out &= ~1;
		return _out;
	}

	private void run_envelope(Voice v)
	{
		int env = v.env;
		if (v.env_mode == EnvMode.release)
		{ // 60%
			if ((env -= 0x8) < 0)
			{
				env = 0;
			}
			v.env = env;
		}
		else
		{
			int rate;
			int env_data = v.regs[v.r + VoiceReg_adsr1] & 0xFF;
			if ((m.t_adsr0 & 0x80) != 0)
			{ // 99% ADSR
				if (v.env_mode.ordinal() >= EnvMode.decay.ordinal())
				{ // 99%
					env--;
					env -= env >> 8;
					rate = env_data & 0x1F;
					if (v.env_mode == EnvMode.decay)
					{ // 1%
						rate = (m.t_adsr0 >> 3 & 0x0E) + 0x10;
					}
				}
				else
				{ // env_attack
					rate = (m.t_adsr0 & 0x0F) * 2 + 1;
					env += rate < 31 ? 0x20 : 0x400;
				}
			}
			else
			{ // GAIN
				int mode;
				env_data = v.regs[v.r + VoiceReg_gain] & 0xFF;
				mode = env_data >> 5;
				if (mode < 4)
				{ // direct
					env = env_data * 0x10;
					rate = 31;
				}
				else
				{
					rate = env_data & 0x1F;
					if (mode == 4)
					{ // 4: linear decrease
						env -= 0x20;
					}
					else if (mode < 6)
					{ // 5: exponential decrease
						env--;
						env -= env >> 8;
					}
					else
					{ // 6,7: linear increase
						env += 0x20;
						if (mode > 6 && (v.hidden_env & 0xFFFFFFFFL) >= 0x600L)
						{ // 7: two-slope linear increase
							env += 0x8 - 0x20;
						}
					}
				}
			}

			// Sustain level
			if ((env >> 8) == (env_data >> 5) && v.env_mode == EnvMode.decay)
			{
				v.env_mode = EnvMode.sustain;
			}

			v.hidden_env = env;

			// uint cast because linear decrease going negative also triggers this
			if ((env & 0xFFFFFFFFL) > 0x7FFL)
			{
				env = (env < 0 ? 0 : 0x7FF);
				if (v.env_mode == EnvMode.attack)
				{
					v.env_mode = EnvMode.decay;
				}
			}

			if (read_counter(rate) == 0)
			{
				v.env = env; // nothing else is controlled by the counter
			}
		}
	}

	private void decode_brr(Voice v)
	{
		// Arrange the four input nybbles in 0xABCD order for easy decoding
		int nybbles = m.t_brr_byte * 0x100 + (m.ram[(v.brr_addr + v.brr_offset + 1) & 0xFFFF] & 0xFF);

		int header = m.t_brr_header;

		// Write to next four samples in circular buffer
		int[] pos = v.buf;
		int pos_p = v.buf_pos;
		int end_p;
		if ((v.buf_pos += 4) >= brr_buf_size)
		{
			v.buf_pos = 0;
		}

		// Decode four samples
		for (end_p = pos_p + 4; pos_p < end_p; pos_p++, nybbles <<= 4)
		{
			// Extract nybble and sign-extend
			int s = (short) nybbles >> 12;

			// Shift sample based on header
			int shift = header >> 4;
			s = (s << shift) >> 1;
			if (shift >= 0xD)
			{ // handle invalid range
				s = (s >> 25) << 11; // same as: s = (s < 0 ? -0x800 : 0)
			}

			// Apply IIR filter (8 is the most commonly used)
			int filter = header & 0x0C;
			int p1 = pos[pos_p + brr_buf_size - 1];
			int p2 = pos[pos_p + brr_buf_size - 2] >> 1;
			if (filter >= 8)
			{
				s += p1;
				s -= p2;
				if (filter == 8)
				{ // s += p1 * 0.953125 - p2 * 0.46875
					s += p2 >> 4;
					s += (p1 * -3) >> 6;
				}
				else
				{ // s += p1 * 0.8984375 - p2 * 0.40625
					s += (p1 * -13) >> 7;
					s += (p2 * 3) >> 4;
				}
			}
			else if (filter != 0)
			{ // s += p1 * 0.46875
				s += p1 >> 1;
				s += (-p1) >> 5;
			}

			// Adjust and write sample
			s = Clamp16(s);
			s = (short) (s * 2);
			// second copy simplifies wrap-around
			pos[pos_p + brr_buf_size] = pos[pos_p + 0] = s;
		}
	}

	private void misc_27()
	{
		// voice 0 doesn't support PMON
		m.t_pmon = m.regs[GlobalReg_pmon] & 0xFE;
	}

	private void misc_28()
	{
		m.t_non = m.regs[GlobalReg_non] & 0xFF;
		m.t_eon = m.regs[GlobalReg_eon] & 0xFF;
		m.t_dir = m.regs[GlobalReg_dir] & 0xFF;
	}

	private void misc_29()
	{
		if ((m.every_other_sample ^= 1) != 0)
		{
			m.new_kon &= ~m.kon; // clears KON 63 clocks after it was last read
		}
	}

	private void misc_30()
	{
		if (m.every_other_sample != 0)
		{
			m.kon = m.new_kon;
			m.t_koff = (m.regs[GlobalReg_koff] & 0xFF) | m.mute_mask;
		}

		run_counters();

		// Noise
		if ((read_counter(m.regs[GlobalReg_flg] & 0x1F)) == 0)
		{
			int feedback = (m.noise << 13) ^ (m.noise << 14);
			m.noise = (feedback & 0x4000) ^ (m.noise >> 1);
		}
	}

	private void voice_output(Voice v, int ch)
	{ // Apply left/right volume
		int amp = (m.t_output * (byte) (v.regs[v.r + VoiceReg_voll + ch] & 0xFF)) >> 7;

		// Add to output total
		m.t_main_out[ch] += amp;
		m.t_main_out[ch] = Clamp16(m.t_main_out[ch]);

		// Optionally add to echo total
		if ((m.t_eon & v.vbit) != 0)
		{
			m.t_echo_out[ch] += amp;
			m.t_echo_out[ch] = Clamp16(m.t_echo_out[ch]);
		}
	}

	private void voice_V1(Voice v)
	{
		m.t_dir_addr = m.t_dir * 0x100 + m.t_srcn * 4;
		m.t_srcn = v.regs[v.r + VoiceReg_srcn] & 0xFF;
	}

	private void voice_V2(Voice v)
	{ // Read sample pointer (ignored if not needed)
		int entry = m.t_dir_addr & 0xFFFF;
		if (v.kon_delay == 0)
		{
			entry += 2;
		}
		int lo = m.ram[entry + 0] & 0xFF;
		int hi = m.ram[entry + 1] & 0xFF;
		m.t_brr_next_addr = ((hi << 8) + lo);

		m.t_adsr0 = v.regs[v.r + VoiceReg_adsr0] & 0xFF;

		// Read pitch, spread over two clocks
		m.t_pitch = v.regs[v.r + VoiceReg_pitchl] & 0xFF;
	}

	private void voice_V3(Voice v)
	{
		voice_V3a(v);
		voice_V3b(v);
		voice_V3c(v);
	}

	private void voice_V3a(Voice v)
	{
		m.t_pitch += (v.regs[v.r + VoiceReg_pitchh] & 0x3F) << 8;
	}

	private void voice_V3b(Voice v)
	{ // Read BRR header and byte
		m.t_brr_byte = m.ram[(v.brr_addr + v.brr_offset) & 0xFFFF] & 0xFF;
		m.t_brr_header = m.ram[v.brr_addr] & 0xFF; // brr_addr doesn't need masking
	}

	private void voice_V3c(Voice v)
	{
		// Pitch modulation using previous voice's output
		if ((m.t_pmon & v.vbit) != 0)
			m.t_pitch += ((m.t_output >> 5) * m.t_pitch) >> 10;

		if ((v.kon_delay) != 0)
		{
			// Get ready to start BRR decoding on next sample
			if (v.kon_delay == 5)
			{
				v.brr_addr = m.t_brr_next_addr;
				v.brr_offset = 1;
				v.buf_pos = 0;
				m.t_brr_header = 0; // header is ignored on this sample
				m.kon_check = true;
			}

			// Envelope is never run during KON
			v.env = 0;
			v.hidden_env = 0;

			// Disable BRR decoding until last three samples
			v.interp_pos = 0;
			if ((--v.kon_delay & 3) != 0)
			{
				v.interp_pos = 0x4000;
			}

			// Pitch is never added during KON
			m.t_pitch = 0;
		}

		// Gaussian interpolation
		{
			int output = interpolate(v);

			// Noise
			if ((m.t_non & v.vbit) != 0)
			{
				output = (short) (m.noise * 2);
			}

			// Apply envelope
			m.t_output = (output * v.env) >> 11 & ~1;
			v.t_envx_out = (v.env >> 4) & 0xFF;
		}

		// Immediate silence due to end of sample or soft reset
		if ((m.regs[GlobalReg_flg] & 0x80) != 0 || (m.t_brr_header & 3) == 1)
		{
			v.env_mode = EnvMode.release;
			v.env = 0;
		}

		if (m.every_other_sample != 0)
		{
			// KOFF
			if ((m.t_koff & v.vbit) != 0)
			{
				v.env_mode = EnvMode.release;
			}

			// KON
			if ((m.kon & v.vbit) != 0)
			{
				v.kon_delay = 5;
				v.env_mode = EnvMode.attack;
			}
		}

		// Run envelope for next sample
		if ((v.kon_delay) == 0)
		{
			run_envelope(v);
		}
	}

	private void voice_V4(Voice v)
	{ // Decode BRR
		m.t_looped = 0;
		if (v.interp_pos >= 0x4000)
		{
			decode_brr(v);

			if ((v.brr_offset += 2) >= brr_block_size)
			{
				// Start decoding next BRR block
				assert v.brr_offset == brr_block_size;
				v.brr_addr = (v.brr_addr + brr_block_size) & 0xFFFF;
				if ((m.t_brr_header & 1) != 0)
				{
					v.brr_addr = m.t_brr_next_addr;
					m.t_looped = v.vbit;
				}
				v.brr_offset = 1;
			}
		}

		// Apply pitch
		v.interp_pos = (v.interp_pos & 0x3FFF) + m.t_pitch;

		// Keep from getting too far ahead (when using pitch modulation)
		if (v.interp_pos > 0x7FFF)
		{
			v.interp_pos = 0x7FFF;
		}

		// Output left
		voice_output(v, 0);
	}

	private void voice_V5(Voice v)
	{ // Output right
		voice_output(v, 1);

		// ENDX, OUTX, and ENVX won't update if you wrote to them 1-2 clocks earlier
		int endx_buf = (m.regs[GlobalReg_endx] & 0xFF) | m.t_looped;

		// Clear bit in ENDX if KON just began
		if (v.kon_delay == 5)
		{
			endx_buf &= ~v.vbit;
		}
		m.endx_buf = endx_buf & 0xFF;
	}

	private void voice_V6(Voice v)
	{
		m.outx_buf = (m.t_output >> 8) & 0xFF;
	}

	private void voice_V7(Voice v)
	{ // Update ENDX
		m.regs[GlobalReg_endx] = m.endx_buf & 0xFF;

		m.envx_buf = v.t_envx_out & 0xFF;
	}

	private void voice_V8(Voice v)
	{ // Update OUTX
		v.regs[v.r + VoiceReg_outx] = m.outx_buf & 0xFF;
	}

	private void voice_V9(Voice v)
	{ // Update ENVX
		v.regs[v.r + VoiceReg_envx] = m.envx_buf & 0xFF;
	}

	private final void voice_V7_V4_V1(Voice[] v, int offset)
	{
		voice_V7(v[offset + 0]);
		voice_V1(v[offset + 3]);
		voice_V4(v[offset + 1]);
	}

	private final void voice_V8_V5_V2(Voice[] v, int offset)
	{
		voice_V8(v[offset + 0]);
		voice_V5(v[offset + 1]);
		voice_V2(v[offset + 2]);
	}

	private final void voice_V9_V6_V3(Voice[] v, int offset)
	{
		voice_V9(v[offset + 0]);
		voice_V6(v[offset + 1]);
		voice_V3(v[offset + 2]);
	}

	private void echo_read(int ch)
	{
		int lo = m.ram[((m.t_echo_ptr + ch * 2) + 0) & 0xFFFF] & 0xFF;
		int hi = m.ram[((m.t_echo_ptr + ch * 2) + 1) & 0xFFFF] & 0xFF;
		int s = (short) ((hi << 8) + lo);
		// second copy simplifies wrap-around handling
		m.echo_hist[m.echo_hist_pos + 0][ch] = m.echo_hist[m.echo_hist_pos + 8][ch] = s >> 1;
	}

	private int echo_output(int ch)
	{
		int _out = (short) ((m.t_main_out[ch] * (byte) (m.regs[GlobalReg_mvoll + ch * 0x10] & 0xFF)) >> 7) +
				(short) ((m.t_echo_in[ch] * (byte) (m.regs[GlobalReg_evoll + ch * 0x10] & 0xFF)) >> 7);
		_out = Clamp16(_out);
		return _out;
	}

	private void echo_write(int ch)
	{
		if ((m.t_echo_enabled & 0x20) == 0)
		{
			int s = m.t_echo_out[ch];
			m.ram[((m.t_echo_ptr + ch * 2) + 0) & 0xFFFF] = (byte) s;
			m.ram[((m.t_echo_ptr + ch * 2) + 1) & 0xFFFF] = (byte) (s >> 8);
		}
		m.t_echo_out[ch] = 0;
	}

	private int CalcFir(int i, int ch)
	{
		return ((m.echo_hist[m.echo_hist_pos + i + 1][ch] * (byte) (m.regs[GlobalReg_fir + i * 0x10] & 0xFF)) >> 6);
	}

	private void echo_22()
	{ // History
		if (m.echo_hist_pos + 1 >= echo_hist_size)
		{
			m.echo_hist_pos = 0;
		}
		else
		{
			m.echo_hist_pos = m.echo_hist_pos + 1;
		}

		m.t_echo_ptr = (m.t_esa * 0x100 + m.echo_offset) & 0xFFFF;
		echo_read(0);

		// FIR (using l and r temporaries below helps compiler optimize)
		int l = CalcFir(0, 0);
		int r = CalcFir(0, 1);

		m.t_echo_in[0] = l;
		m.t_echo_in[1] = r;
	}

	private void echo_23()
	{
		int l = CalcFir(1, 0) + CalcFir(2, 0);
		int r = CalcFir(1, 1) + CalcFir(2, 1);

		m.t_echo_in[0] += l;
		m.t_echo_in[1] += r;

		echo_read(1);
	}

	private void echo_24()
	{
		int l = CalcFir(3, 0) + CalcFir(4, 0) + CalcFir(5, 0);
		int r = CalcFir(3, 1) + CalcFir(4, 1) + CalcFir(5, 1);

		m.t_echo_in[0] += l;
		m.t_echo_in[1] += r;
	}

	private void echo_25()
	{
		int l = m.t_echo_in[0] + CalcFir(6, 0);
		int r = m.t_echo_in[1] + CalcFir(6, 1);

		l = (short) l;
		r = (short) r;

		l += (short) CalcFir(7, 0);
		r += (short) CalcFir(7, 1);

		l = Clamp16(l);
		r = Clamp16(r);

		m.t_echo_in[0] = l & ~1;
		m.t_echo_in[1] = r & ~1;
	}

	private void echo_26()
	{ // Left output volumes
		// (save sample for next clock so we can output both together)
		m.t_main_out[0] = echo_output(0);

		// Echo feedback
		int l = m.t_echo_out[0] + (short) ((m.t_echo_in[0] * (byte) (m.regs[GlobalReg_efb] & 0xFF)) >> 7);
		int r = m.t_echo_out[1] + (short) ((m.t_echo_in[1] * (byte) (m.regs[GlobalReg_efb] & 0xFF)) >> 7);

		l = Clamp16(l);
		r = Clamp16(r);

		m.t_echo_out[0] = l & ~1;
		m.t_echo_out[1] = r & ~1;
	}

	private final void WRITE_SAMPLES(int l, int r)
	{
		m._out[m._out_Offset + 0] = (short) l;
		m._out[m._out_Offset + 1] = (short) r;
		m._out_Offset += 2;
		if (m._out_Offset >= m._out.length)
		{
			assert m._out_Offset == m._out.length;
			// TODO: fix this assert
			// Debug.Assert(m._out.Array.Length != m.extra[extra_size] ||
			// (m.extra <= m.out_begin && m.extra < m.extra[extra_size]));
			// m._out = new ArraySegment<short>(m.extra, 0, m.extra.Length);
			m._out = m.extra;
			// TODO: determine what's really happening here in bsnes code
			// m._out = new ArraySegment<short>(m.extra, extra_size,
			// m.extra.Length - extra_size);
		}
	}

	private void echo_27()
	{ // Output
		int l = m.t_main_out[0];
		int r = echo_output(1);
		m.t_main_out[0] = 0;
		m.t_main_out[1] = 0;

		// global muting isn't this simple (turns DAC on and off
		// or something, causing small ~37-sample pulse when first muted)
		if ((m.regs[GlobalReg_flg] & 0x40) != 0)
		{
			l = 0;
			r = 0;
		}

		// Output sample to DAC
		WRITE_SAMPLES(l, r);
	}

	private void echo_28()
	{
		m.t_echo_enabled = m.regs[GlobalReg_flg] & 0xFF;
	}

	private void echo_29()
	{
		m.t_esa = m.regs[GlobalReg_esa] & 0xFF;

		if (m.echo_offset == 0)
		{
			m.echo_length = (m.regs[GlobalReg_edl] & 0x0F) * 0x800;
		}

		m.echo_offset += 4;
		if (m.echo_offset >= m.echo_length)
		{
			m.echo_offset = 0;
		}

		// Write left echo
		echo_write(0);

		m.t_echo_enabled = m.regs[GlobalReg_flg] & 0xFF;
	}

	private void echo_30()
	{ // Write right echo
		echo_write(1);
	}

	private void soft_reset_common()
	{
		assert m.ram != null; // init() must have been called already

		m.noise = 0x4000;
		m.echo_hist_pos = 0;
		m.every_other_sample = 1;
		m.echo_offset = 0;
		m.phase = 0;

		init_counter();
	}
}
