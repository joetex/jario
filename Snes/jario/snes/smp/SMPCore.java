/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.smp;

import jario.snes.smp.SMPCoreOperation.SMPCoreOp;

public abstract class SMPCore implements java.io.Serializable
{
	protected int op_readpc()
	{
		return op_read(regs.pc++) & 0xFF;
	}

	protected int op_readstack()
	{
		return op_read(0x0100 | ++regs.r[regs.sp]) & 0xFF;
	}

	protected void op_writestack(int data)
	{
		op_write(0x0100 | regs.r[regs.sp]--, (byte) data);
	}

	protected int op_readaddr(int addr)
	{
		return op_read(addr) & 0xFF;
	}

	protected void op_writeaddr(int addr, int data)
	{
		op_write(addr, (byte) data);
	}

	protected int op_readdp(int addr)
	{
		return op_read(((regs.p.p ? 1 : 0) << 8) + addr) & 0xFF;
	}

	protected void op_writedp(int addr, int data)
	{
		op_write(((regs.p.p ? 1 : 0) << 8) + addr, (byte) data);
	}

	public Regs regs = new Regs();
	public int dp, sp, rd, wr, bit, ya;
	private static final int OpCode_A = 0;
	private static final int OpCode_X = 1;
	private static final int OpCode_Y = 2;
	private static final int OpCode_SP = 3;

	public abstract void op_io();

	public abstract byte op_read(int addr);

	public abstract void op_write(int addr, byte data);

	public SMPCoreOp op_adc = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			int r = args.x + args.y + (regs.p.c ? 1 : 0);
			regs.p.n = (r & 0x80) != 0;
			regs.p.v = (~(args.x ^ args.y) & (args.x ^ r) & 0x80) != 0;
			regs.p.h = ((args.x ^ args.y ^ r) & 0x10) != 0;
			regs.p.z = (r & 0xFF) == 0;
			regs.p.c = r > 0xff;
			return new SMPCoreOpResult(r);
		}
	};

	public SMPCoreOp op_addw = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			int r;
			regs.p.c = false;
			r = op_adc.Invoke(new SMPCoreOpArgument(args.x & 0xFF, args.y & 0xFF)).result_byte();
			r |= (op_adc.Invoke(new SMPCoreOpArgument((args.x >> 8) & 0xFF, (args.y >> 8) & 0xFF)).result_byte() << 8);
			regs.p.z = r == 0;
			return new SMPCoreOpResult(r);
		}
	};

	public SMPCoreOp op_and = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			args.x &= args.y;
			regs.p.n = (args.x & 0x80) != 0;
			regs.p.z = args.x == 0;
			return new SMPCoreOpResult(args.x);
		}
	};

	public SMPCoreOp op_cmp = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			int r = args.x - args.y;
			regs.p.n = (r & 0x80) != 0;
			regs.p.z = (r & 0xFF) == 0;
			regs.p.c = r >= 0;
			return new SMPCoreOpResult(args.x);
		}
	};

	public SMPCoreOp op_cmpw = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			int r = args.x - args.y;
			regs.p.n = (r & 0x8000) != 0;
			regs.p.z = (r & 0xFFFF) == 0;
			regs.p.c = r >= 0;
			return new SMPCoreOpResult(args.x);
		}
	};

	public SMPCoreOp op_eor = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			args.x ^= args.y;
			regs.p.n = (args.x & 0x80) != 0;
			regs.p.z = args.x == 0;
			return new SMPCoreOpResult(args.x);
		}
	};

	public SMPCoreOp op_inc = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			args.x++;
			regs.p.n = (args.x & 0x80) != 0;
			regs.p.z = (args.x & 0xFF) == 0;
			return new SMPCoreOpResult(args.x);
		}
	};

	public SMPCoreOp op_dec = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			args.x--;
			regs.p.n = (args.x & 0x80) != 0;
			regs.p.z = (args.x & 0xFF) == 0;
			return new SMPCoreOpResult(args.x);
		}
	};

	public SMPCoreOp op_or = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			args.x |= args.y;
			regs.p.n = (args.x & 0x80) != 0;
			regs.p.z = args.x == 0;
			return new SMPCoreOpResult(args.x);
		}
	};

	public SMPCoreOp op_sbc = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			int r = args.x - args.y - (!regs.p.c ? 1 : 0);
			regs.p.n = (r & 0x80) != 0;
			regs.p.v = ((args.x ^ args.y) & (args.x ^ r) & 0x80) != 0;
			regs.p.h = (((args.x ^ args.y ^ r) & 0x10)) == 0;
			regs.p.z = (r & 0xFF) == 0;
			regs.p.c = r >= 0;
			return new SMPCoreOpResult(r);
		}
	};

	public SMPCoreOp op_subw = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			int r;
			regs.p.c = true;
			r = op_sbc.Invoke(new SMPCoreOpArgument(args.x & 0xFF, args.y & 0xFF)).result_byte();
			r |= (op_sbc.Invoke(new SMPCoreOpArgument((args.x >> 8) & 0xFF, (args.y >> 8) & 0xFF)).result_byte() << 8);
			regs.p.z = r == 0;
			return new SMPCoreOpResult(r);
		}
	};

	public SMPCoreOp op_asl = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			regs.p.c = (args.x & 0x80) != 0;
			args.x <<= 1;
			regs.p.n = (args.x & 0x80) != 0;
			regs.p.z = (args.x & 0xFF) == 0;
			return new SMPCoreOpResult(args.x);
		}
	};

	public SMPCoreOp op_lsr = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			regs.p.c = (args.x & 0x01) != 0;
			args.x >>= 1;
			regs.p.n = (args.x & 0x80) != 0;
			regs.p.z = (args.x & 0xFF) == 0;
			return new SMPCoreOpResult(args.x);
		}
	};

	public SMPCoreOp op_rol = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			int carry = (regs.p.c ? 1 : 0);
			regs.p.c = (args.x & 0x80) != 0;
			args.x = ((args.x << 1) | carry) & 0xFF;
			regs.p.n = (args.x & 0x80) != 0;
			regs.p.z = args.x == 0;
			return new SMPCoreOpResult(args.x);
		}
	};

	public SMPCoreOp op_ror = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			int carry = (regs.p.c ? 1 : 0) << 7;
			regs.p.c = (args.x & 0x01) != 0;
			args.x = (carry | (args.x >> 1)) & 0xFF;
			regs.p.n = (args.x & 0x80) != 0;
			regs.p.z = args.x == 0;
			return new SMPCoreOpResult(args.x);
		}
	};

	public SMPCoreOp op_mov_reg_reg = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			regs.r[args.to] = regs.r[args.from];
			regs.p.n = (regs.r[args.to] & 0x80) != 0;
			regs.p.z = (regs.r[args.to] == 0);
			return null;
		}
	};

	public SMPCoreOp op_mov_sp_x = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			regs.r[regs.sp] = regs.r[regs.x];
			return null;
		}
	};

	public SMPCoreOp op_mov_reg_const = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			regs.r[args.n] = op_readpc();
			regs.p.n = (regs.r[args.n] & 0x80) != 0;
			regs.p.z = (regs.r[args.n] == 0);
			return null;
		}
	};

	public SMPCoreOp op_mov_a_ix = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			regs.r[regs.a] = op_readdp(regs.r[regs.x]);
			regs.p.n = (regs.r[regs.a] & 0x80) != 0;
			regs.p.z = (regs.r[regs.a] == 0);
			return null;
		}
	};

	public SMPCoreOp op_mov_a_ixinc = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			regs.r[regs.a] = op_readdp(regs.r[regs.x]++);
			op_io();
			regs.p.n = (regs.r[regs.a] & 0x80) != 0;
			regs.p.z = (regs.r[regs.a] == 0);
			return null;
		}
	};

	public SMPCoreOp op_mov_reg_dp = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			sp = op_readpc();
			regs.r[args.n] = op_readdp(sp);
			regs.p.n = (regs.r[args.n] & 0x80) != 0;
			regs.p.z = (regs.r[args.n] == 0);
			return null;
		}
	};

	public SMPCoreOp op_mov_reg_dpr = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			sp = op_readpc();
			op_io();
			regs.r[args.n] = op_readdp(sp + regs.r[args.i]);
			regs.p.n = (regs.r[args.n] & 0x80) != 0;
			regs.p.z = (regs.r[args.n] == 0);
			return null;
		}
	};

	public SMPCoreOp op_mov_reg_addr = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			sp = (op_readpc() << 0);
			sp |= (op_readpc() << 8);
			regs.r[args.n] = op_readaddr(sp);
			regs.p.n = (regs.r[args.n] & 0x80) != 0;
			regs.p.z = (regs.r[args.n] == 0);
			return null;
		}
	};

	public SMPCoreOp op_mov_a_addrr = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			sp = (op_readpc() << 0);
			sp |= (op_readpc() << 8);
			op_io();
			regs.r[regs.a] = op_readaddr(sp + regs.r[args.i]);
			regs.p.n = (regs.r[regs.a] & 0x80) != 0;
			regs.p.z = (regs.r[regs.a] == 0);
			return null;
		}
	};

	public SMPCoreOp op_mov_a_idpx = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = (op_readpc() + regs.r[regs.x]) & 0xFFFF;
			op_io();
			sp = (op_readdp(dp + 0) << 0);
			sp |= (op_readdp(dp + 1) << 8);
			regs.r[regs.a] = op_readaddr(sp);
			regs.p.n = (regs.r[regs.a] & 0x80) != 0;
			regs.p.z = (regs.r[regs.a] == 0);
			return null;
		}
	};

	public SMPCoreOp op_mov_a_idpy = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = op_readpc();
			op_io();
			sp = (op_readdp(dp + 0) << 0);
			sp |= (op_readdp(dp + 1) << 8);
			regs.r[regs.a] = op_readaddr(sp + regs.r[regs.y]);
			regs.p.n = (regs.r[regs.a] & 0x80) != 0;
			regs.p.z = (regs.r[regs.a] == 0);
			return null;
		}
	};

	public SMPCoreOp op_mov_dp_dp = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			sp = op_readpc();
			rd = op_readdp(sp);
			dp = op_readpc();
			op_writedp(dp, rd);
			return null;
		}
	};

	public SMPCoreOp op_mov_dp_const = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			rd = op_readpc();
			dp = op_readpc();
			op_readdp(dp);
			op_writedp(dp, rd);
			return null;
		}
	};

	public SMPCoreOp op_mov_ix_a = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			op_readdp(regs.r[regs.x]);
			op_writedp(regs.r[regs.x], regs.r[regs.a]);
			return null;
		}
	};

	public SMPCoreOp op_mov_ixinc_a = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			op_io();
			op_writedp(regs.r[regs.x]++, regs.r[regs.a]);
			return null;
		}
	};

	public SMPCoreOp op_mov_dp_reg = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = op_readpc();
			op_readdp(dp);
			op_writedp(dp, regs.r[args.n]);
			return null;
		}
	};

	public SMPCoreOp op_mov_dpr_reg = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = op_readpc();
			op_io();
			dp += regs.r[args.i];
			op_readdp(dp);
			op_writedp(dp, regs.r[args.n]);
			return null;
		}
	};

	public SMPCoreOp op_mov_addr_reg = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = (op_readpc() << 0);
			dp |= (op_readpc() << 8);
			op_readaddr(dp);
			op_writeaddr(dp, regs.r[args.n]);
			return null;
		}
	};

	public SMPCoreOp op_mov_addrr_a = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = (op_readpc() << 0);
			dp |= (op_readpc() << 8);
			op_io();
			dp += regs.r[args.i];
			op_readaddr(dp);
			op_writeaddr(dp, regs.r[regs.a]);
			return null;
		}
	};

	public SMPCoreOp op_mov_idpx_a = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			sp = op_readpc();
			op_io();
			sp += regs.r[regs.x];
			dp = (op_readdp(sp + 0) << 0);
			dp |= (op_readdp(sp + 1) << 8);
			op_readaddr(dp);
			op_writeaddr(dp, regs.r[regs.a]);
			return null;
		}
	};

	public SMPCoreOp op_mov_idpy_a = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			sp = op_readpc();
			dp = (op_readdp(sp + 0) << 0);
			dp |= (op_readdp(sp + 1) << 8);
			op_io();
			dp += regs.r[regs.y];
			op_readaddr(dp);
			op_writeaddr(dp, regs.r[regs.a]);
			return null;
		}
	};

	public SMPCoreOp op_movw_ya_dp = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			sp = op_readpc();
			regs.r[regs.a] = op_readdp(sp + 0);
			op_io();
			regs.r[regs.y] = op_readdp(sp + 1);
			regs.p.n = (regs.ya.get() & 0x8000) != 0;
			regs.p.z = (regs.ya.get() == 0);
			return null;
		}
	};

	public SMPCoreOp op_movw_dp_ya = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = op_readpc();
			op_readdp(dp);
			op_writedp(dp + 0, regs.r[regs.a]);
			op_writedp(dp + 1, regs.r[regs.y]);
			return null;
		}
	};

	public SMPCoreOp op_mov1_c_bit = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			sp = (op_readpc() << 0);
			sp |= (op_readpc() << 8);
			bit = (sp >> 13);
			sp &= 0x1fff;
			rd = op_readaddr(sp);
			regs.p.c = (rd & (1 << bit)) != 0;
			return null;
		}
	};

	public SMPCoreOp op_mov1_bit_c = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = (op_readpc() << 0);
			dp |= (op_readpc() << 8);
			bit = (dp >> 13);
			dp &= 0x1fff;
			rd = op_readaddr(dp);
			if (regs.p.c)
			{
				rd |= (1 << bit);
			}
			else
			{
				rd &= (~(1 << bit));
			}
			op_io();
			op_writeaddr(dp, rd);
			return null;
		}
	};

	public SMPCoreOp op_bra = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			rd = op_readpc();
			op_io();
			op_io();
			regs.pc += ((byte) rd);
			return null;
		}
	};

	public SMPCoreOp op_branch = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			rd = op_readpc();
			if (((regs.p.get() & args.flag) != 0 ? 1 : 0) != args.value)
			{
			return null;
			}
			op_io();
			op_io();
			regs.pc += ((byte) rd);
			return null;
		}
	};

	public SMPCoreOp op_bitbranch = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = op_readpc();
			sp = op_readdp(dp);
			rd = op_readpc();
			op_io();
			if (((sp & args.mask) != 0 ? 1 : 0) != args.value)
			{
			return null;
			}
			op_io();
			op_io();
			regs.pc += ((byte) rd);
			return null;
		}
	};

	public SMPCoreOp op_cbne_dp = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = op_readpc();
			sp = op_readdp(dp);
			rd = op_readpc();
			op_io();
			if (regs.r[regs.a] == sp)
			{
			return null;
			}
			op_io();
			op_io();
			regs.pc += ((byte) rd);
			return null;
		}
	};

	public SMPCoreOp op_cbne_dpx = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = op_readpc();
			op_io();
			sp = op_readdp(dp + regs.r[regs.x]);
			rd = op_readpc();
			op_io();
			if (regs.r[regs.a] == sp)
			{
			return null;
			}
			op_io();
			op_io();
			regs.pc += ((byte) rd);
			return null;
		}
	};

	public SMPCoreOp op_dbnz_dp = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = op_readpc();
			wr = op_readdp(dp);
			op_writedp(dp, --wr);
			rd = op_readpc();
			if (wr == 0)
			{
			return null;
			}
			op_io();
			op_io();
			regs.pc += ((byte) rd);
			return null;
		}
	};

	public SMPCoreOp op_dbnz_y = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			rd = op_readpc();
			op_io();
			regs.r[regs.y]--;
			op_io();
			if (regs.r[regs.y] == 0)
			{
			return null;
			}
			op_io();
			op_io();
			regs.pc += ((byte) rd);
			return null;
		}
	};

	public SMPCoreOp op_jmp_addr = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			rd = (op_readpc() << 0);
			rd |= (op_readpc() << 8);
			regs.pc = rd;
			return null;
		}
	};

	public SMPCoreOp op_jmp_iaddrx = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = (op_readpc() << 0);
			dp |= (op_readpc() << 8);
			op_io();
			dp += regs.r[regs.x];
			rd = (op_readaddr(dp + 0) << 0);
			rd |= (op_readaddr(dp + 1) << 8);
			regs.pc = rd;
			return null;
		}
	};

	public SMPCoreOp op_call = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			rd = (op_readpc() << 0);
			rd |= (op_readpc() << 8);
			op_io();
			op_io();
			op_io();
			op_writestack(regs.pc >> 8);
			op_writestack(regs.pc >> 0);
			regs.pc = rd;
			return null;
		}
	};

	public SMPCoreOp op_pcall = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			rd = op_readpc();
			op_io();
			op_io();
			op_writestack(regs.pc >> 8);
			op_writestack(regs.pc >> 0);
			regs.pc = (0xff00 | rd);
			return null;
		}
	};

	public SMPCoreOp op_tcall = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = (0xffde - (args.n << 1));
			rd = (op_readaddr(dp + 0) << 0);
			rd |= (op_readaddr(dp + 1) << 8);
			op_io();
			op_io();
			op_io();
			op_writestack(regs.pc >> 8);
			op_writestack(regs.pc >> 0);
			regs.pc = rd;
			return null;
		}
	};

	public SMPCoreOp op_brk = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			rd = (op_readaddr(0xffde) << 0);
			rd |= (op_readaddr(0xffdf) << 8);
			op_io();
			op_io();
			op_writestack(regs.pc >> 8);
			op_writestack(regs.pc >> 0);
			op_writestack(regs.p.get());
			regs.pc = rd;
			regs.p.b = true;
			regs.p.i = false;
			return null;
		}
	};

	public SMPCoreOp op_ret = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			rd = (op_readstack() << 0);
			rd |= (op_readstack() << 8);
			op_io();
			op_io();
			regs.pc = rd;
			return null;
		}
	};

	public SMPCoreOp op_reti = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			regs.p.set(op_readstack());
			rd = (op_readstack() << 0);
			rd |= (op_readstack() << 8);
			op_io();
			op_io();
			regs.pc = rd;
			return null;
		}
	};

	public SMPCoreOp op_read_reg_const = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			rd = op_readpc();
			regs.r[args.n] = args.op_func.Invoke(new SMPCoreOpArgument(regs.r[args.n] & 0xFF, rd & 0xFF)).result_byte();
			return null;
		}
	};

	public SMPCoreOp op_read_a_ix = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			rd = op_readdp(regs.r[regs.x]);
			regs.r[regs.a] = args.op_func.Invoke(new SMPCoreOpArgument(regs.r[regs.a] & 0xFF, rd & 0xFF)).result_byte();
			return null;
		}
	};

	public SMPCoreOp op_read_reg_dp = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = op_readpc();
			rd = op_readdp(dp);
			regs.r[args.n] = args.op_func.Invoke(new SMPCoreOpArgument(regs.r[args.n] & 0xFF, rd & 0xFF)).result_byte();
			return null;
		}
	};

	public SMPCoreOp op_read_a_dpx = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = op_readpc();
			op_io();
			rd = op_readdp(dp + regs.r[regs.x]);
			regs.r[regs.a] = args.op_func.Invoke(new SMPCoreOpArgument(regs.r[regs.a] & 0xFF, rd & 0xFF)).result_byte();
			return null;
		}
	};

	public SMPCoreOp op_read_reg_addr = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = (op_readpc() << 0);
			dp |= (op_readpc() << 8);
			rd = op_readaddr(dp);
			regs.r[args.n] = args.op_func.Invoke(new SMPCoreOpArgument(regs.r[args.n] & 0xFF, rd & 0xFF)).result_byte();
			return null;
		}
	};

	public SMPCoreOp op_read_a_addrr = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = (op_readpc() << 0);
			dp |= (op_readpc() << 8);
			op_io();
			rd = op_readaddr(dp + regs.r[args.i]);
			regs.r[regs.a] = args.op_func.Invoke(new SMPCoreOpArgument(regs.r[regs.a] & 0xFF, rd & 0xFF)).result_byte();
			return null;
		}
	};

	public SMPCoreOp op_read_a_idpx = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = (op_readpc() + regs.r[regs.x]) & 0xFFFF;
			op_io();
			sp = (op_readdp(dp + 0) << 0);
			sp |= (op_readdp(dp + 1) << 8);
			rd = op_readaddr(sp);
			regs.r[regs.a] = args.op_func.Invoke(new SMPCoreOpArgument(regs.r[regs.a] & 0xFF, rd & 0xFF)).result_byte();
			return null;
		}
	};

	public SMPCoreOp op_read_a_idpy = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = op_readpc();
			op_io();
			sp = (op_readdp(dp + 0) << 0);
			sp |= (op_readdp(dp + 1) << 8);
			rd = op_readaddr(sp + regs.r[regs.y]);
			regs.r[regs.a] = args.op_func.Invoke(new SMPCoreOpArgument(regs.r[regs.a] & 0xFF, rd & 0xFF)).result_byte();
			return null;
		}
	};

	public SMPCoreOp op_read_ix_iy = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			rd = op_readdp(regs.r[regs.y]);
			wr = op_readdp(regs.r[regs.x]);
			wr = args.op_func.Invoke(new SMPCoreOpArgument(wr & 0xFF, rd & 0xFF)).result_byte();
			SMPCoreOp cmp = op_cmp;
			if (args.op_func != cmp)
			{
				op_writedp(regs.r[regs.x], wr);
			}
			else
			{
				op_io();
			}
			return null;
		}
	};

	public SMPCoreOp op_read_dp_dp = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			sp = op_readpc();
			rd = op_readdp(sp);
			dp = op_readpc();
			wr = op_readdp(dp);
			wr = args.op_func.Invoke(new SMPCoreOpArgument(wr & 0xFF, rd & 0xFF)).result_byte();
			SMPCoreOp cmp = op_cmp;
			if (args.op_func != cmp)
			{
				op_writedp(dp, wr);
			}
			else
			{
				op_io();
			}
			return null;
		}
	};

	public SMPCoreOp op_read_dp_const = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			rd = op_readpc();
			dp = op_readpc();
			wr = op_readdp(dp);
			wr = args.op_func.Invoke(new SMPCoreOpArgument(wr & 0xFF, rd & 0xFF)).result_byte();
			SMPCoreOp cmp = op_cmp;
			if (args.op_func != cmp)
			{
				op_writedp(dp, wr);
			}
			else
			{
				op_io();
			}
			return null;
		}
	};

	public SMPCoreOp op_read_ya_dp = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = op_readpc();
			rd = (op_readdp(dp + 0) << 0);
			op_io();
			rd |= (op_readdp(dp + 1) << 8);
			regs.ya.set(args.op_func.Invoke(new SMPCoreOpArgument(regs.ya.get(), rd & 0xFFFF)).result_ushort());
			return null;
		}
	};

	public SMPCoreOp op_cmpw_ya_dp = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = op_readpc();
			rd = (op_readdp(dp + 0) << 0);
			rd |= (op_readdp(dp + 1) << 8);
			op_cmpw.Invoke(new SMPCoreOpArgument(regs.ya.get(), rd & 0xFFFF));
			return null;
		}
	};

	public SMPCoreOp op_and1_bit = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = (op_readpc() << 0);
			dp |= (op_readpc() << 8);
			bit = (dp >> 13);
			dp &= 0x1fff;
			rd = op_readaddr(dp);
			regs.p.c = ((regs.p.c ? 1 : 0) & (((rd & (1 << bit)) != 0 ? 1 : 0) ^ args.op)) != 0;
			return null;
		}
	};

	public SMPCoreOp op_eor1_bit = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = (op_readpc() << 0);
			dp |= (op_readpc() << 8);
			bit = (dp >> 13);
			dp &= 0x1fff;
			rd = op_readaddr(dp);
			op_io();
			regs.p.c = ((regs.p.c ? 1 : 0) ^ ((rd & (1 << bit)) != 0 ? 1 : 0)) != 0;
			return null;
		}
	};

	public SMPCoreOp op_not1_bit = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = (op_readpc() << 0);
			dp |= (op_readpc() << 8);
			bit = (dp >> 13);
			dp &= 0x1fff;
			rd = op_readaddr(dp);
			rd ^= (1 << bit);
			op_writeaddr(dp, rd);
			return null;
		}
	};

	public SMPCoreOp op_or1_bit = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = (op_readpc() << 0);
			dp |= (op_readpc() << 8);
			bit = (dp >> 13);
			dp &= 0x1fff;
			rd = op_readaddr(dp);
			op_io();
			regs.p.c = ((regs.p.c ? 1 : 0) | (((rd & (1 << bit)) != 0 ? 1 : 0) ^ args.op)) != 0;
			return null;
		}
	};

	public SMPCoreOp op_adjust_reg = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			regs.r[args.n] = args.op_func.Invoke(new SMPCoreOpArgument(regs.r[args.n] & 0xFF)).result_byte();
			return null;
		}
	};

	public SMPCoreOp op_adjust_dp = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = op_readpc();
			rd = op_readdp(dp);
			rd = args.op_func.Invoke(new SMPCoreOpArgument(rd & 0xFF)).result_byte();
			op_writedp(dp, rd);
			return null;
		}
	};

	public SMPCoreOp op_adjust_dpx = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = op_readpc();
			op_io();
			rd = op_readdp(dp + regs.r[regs.x]);
			rd = args.op_func.Invoke(new SMPCoreOpArgument(rd & 0xFF)).result_byte();
			op_writedp(dp + regs.r[regs.x], rd);
			return null;
		}
	};

	public SMPCoreOp op_adjust_addr = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = (op_readpc() << 0);
			dp |= (op_readpc() << 8);
			rd = op_readaddr(dp);
			rd = args.op_func.Invoke(new SMPCoreOpArgument(rd & 0xFF)).result_byte();
			op_writeaddr(dp, rd);
			return null;
		}
	};

	public SMPCoreOp op_adjust_addr_a = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = (op_readpc() << 0);
			dp |= (op_readpc() << 8);
			rd = op_readaddr(dp);
			regs.p.n = ((regs.r[regs.a] - rd) & 0x80) != 0;
			regs.p.z = ((regs.r[regs.a] - rd) == 0);
			op_readaddr(dp);
			op_writeaddr(dp, (args.op != 0 ? rd | regs.r[regs.a] : rd & ~regs.r[regs.a]));
			return null;
		}
	};

	public SMPCoreOp op_adjustw_dp = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = op_readpc();
			rd = (op_readdp(dp) << 0);
			rd += (args.adjust & 0xFFFF);
			op_writedp(dp++, rd);
			rd += (op_readdp(dp) << 8);
			op_writedp(dp, rd >> 8);
			regs.p.n = (rd & 0x8000) != 0;
			regs.p.z = (rd == 0);
			return null;
		}
	};

	public SMPCoreOp op_nop = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			return null;
		}
	};

	public SMPCoreOp op_wait = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			System.out.println("SMP wait (entering infinite loop)");
			while (true)
			{
				op_io();
				op_io();
			}
		}
	};

	public SMPCoreOp op_xcn = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			op_io();
			op_io();
			op_io();
			regs.r[regs.a] = (regs.r[regs.a] >> 4) | (regs.r[regs.a] << 4) & 0xFF;
			regs.p.n = (regs.r[regs.a] & 0x80) != 0;
			regs.p.z = (regs.r[regs.a] == 0);
			return null;
		}
	};

	public SMPCoreOp op_daa = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			op_io();
			if (regs.p.c || (regs.r[regs.a]) > 0x99)
			{
				regs.r[regs.a] += 0x60;
				regs.p.c = true;
			}
			if (regs.p.h || (regs.r[regs.a] & 15) > 0x09)
			{
				regs.r[regs.a] += 0x06;
			}
			regs.p.n = (((regs.r[regs.a] & 0x80) != 0 ? 1 : 0)) != 0;
			regs.p.z = (regs.r[regs.a] == 0);
			return null;
		}
	};

	public SMPCoreOp op_das = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			op_io();
			if (!regs.p.c || (regs.r[regs.a]) > 0x99)
			{
				regs.r[regs.a] -= 0x60;
				regs.p.c = false;
			}
			if (!regs.p.h || (regs.r[regs.a] & 15) > 0x09)
			{
				regs.r[regs.a] -= 0x06;
			}
			regs.p.n = (((regs.r[regs.a] & 0x80) != 0 ? 1 : 0)) != 0;
			regs.p.z = (regs.r[regs.a] == 0);
			return null;
		}
	};

	public SMPCoreOp op_setbit = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			regs.p.set(((regs.p.get() & ~args.mask) | args.value));
			return null;
		}
	};

	public SMPCoreOp op_notc = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			op_io();
			regs.p.c = !regs.p.c;
			return null;
		}
	};

	public SMPCoreOp op_seti = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			op_io();
			regs.p.i = args.value != 0;
			return null;
		}
	};

	public SMPCoreOp op_setbit_dp = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			dp = op_readpc();
			rd = op_readdp(dp);
			rd = (args.op != 0 ? rd | args.value : rd & ~args.value);
			op_writedp(dp, rd);
			return null;
		}
	};

	public SMPCoreOp op_push_reg = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			op_io();
			op_writestack(regs.r[args.n]);
			return null;
		}
	};

	public SMPCoreOp op_push_p = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			op_io();
			op_writestack(regs.p.get());
			return null;
		}
	};

	public SMPCoreOp op_pop_reg = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			op_io();
			regs.r[args.n] = op_readstack();
			return null;
		}
	};

	public SMPCoreOp op_pop_p = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			op_io();
			regs.p.set(op_readstack());
			return null;
		}
	};

	public SMPCoreOp op_mul_ya = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			op_io();
			op_io();
			op_io();
			op_io();
			op_io();
			op_io();
			op_io();
			ya = (regs.r[regs.y] * regs.r[regs.a]) & 0xFFFF;
			regs.r[regs.a] = ya & 0xFF;
			regs.r[regs.y] = (ya >> 8) & 0xFF;
			// result is set based on y (high-byte) only
			regs.p.n = (((regs.r[regs.y] & 0x80) != 0 ? 1 : 0)) != 0;
			regs.p.z = (regs.r[regs.y] == 0);
			return null;
		}
	};

	public SMPCoreOp op_div_ya_x = new SMPCoreOp()
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args)
		{
			op_io();
			op_io();
			op_io();
			op_io();
			op_io();
			op_io();
			op_io();
			op_io();
			op_io();
			op_io();
			op_io();
			ya = regs.ya.get();
			// overflow set if quotient >= 256
			regs.p.v = !!(regs.r[regs.y] >= regs.r[regs.x]);
			regs.p.h = !!((regs.r[regs.y] & 15) >= (regs.r[regs.x] & 15));
			if (regs.r[regs.y] < (regs.r[regs.x] << 1))
			{
				// if quotient is <= 511 (will fit into 9-bit result)
				regs.r[regs.a] = (ya / regs.r[regs.x]) & 0xFF;
				regs.r[regs.y] = (ya % regs.r[regs.x]) & 0xFF;
			}
			else
			{
				// otherwise, the quotient won't fit into regs.p.v + regs.a
				// this emulates the odd behavior of the S-SMP in this case
				regs.r[regs.a] = (255 - (ya - (regs.r[regs.x] << 9)) / (256 - regs.r[regs.x])) & 0xFF;
				regs.r[regs.y] = (regs.r[regs.x] + (ya - (regs.r[regs.x] << 9)) % (256 - regs.r[regs.x])) & 0xFF;
			}
			// result is set based on a (quotient) only
			regs.p.n = (((regs.r[regs.a] & 0x80) != 0 ? 1 : 0)) != 0;
			regs.p.z = (regs.r[regs.a] == 0);
			return null;
		}
	};

	public SMPCoreOperation[] opcode_table = new SMPCoreOperation[256];

	public void initialize_opcode_table()
	{
		 opcode_table[0x00] = new SMPCoreOperation(op_nop, null);
		 opcode_table[0x01] = new SMPCoreOperation(op_tcall, new SMPCoreOpArgument() {{ n = 0; }});
		 opcode_table[0x02] = new SMPCoreOperation(op_setbit_dp, new SMPCoreOpArgument() {{ op = 1; value = 0x01; }});
		 opcode_table[0x03] = new SMPCoreOperation(op_bitbranch, new SMPCoreOpArgument() {{ mask = 0x01; value = 1; }});
		 opcode_table[0x04] = new SMPCoreOperation(op_read_reg_dp, new SMPCoreOpArgument() {{ op_func = op_or; n = OpCode_A; }});
		 opcode_table[0x05] = new SMPCoreOperation(op_read_reg_addr, new SMPCoreOpArgument() {{ op_func = op_or; n = OpCode_A; }});
		 opcode_table[0x06] = new SMPCoreOperation(op_read_a_ix, new SMPCoreOpArgument() {{ op_func = op_or; }});
		 opcode_table[0x07] = new SMPCoreOperation(op_read_a_idpx, new SMPCoreOpArgument() {{ op_func = op_or; }});
		 opcode_table[0x08] = new SMPCoreOperation(op_read_reg_const, new SMPCoreOpArgument() {{ op_func = op_or; n = OpCode_A; }});
		 opcode_table[0x09] = new SMPCoreOperation(op_read_dp_dp, new SMPCoreOpArgument() {{ op_func = op_or; }});
		 opcode_table[0x0a] = new SMPCoreOperation(op_or1_bit, new SMPCoreOpArgument() {{ op = 0; }});
		 opcode_table[0x0b] = new SMPCoreOperation(op_adjust_dp, new SMPCoreOpArgument() {{ op_func = op_asl; }});
		 opcode_table[0x0c] = new SMPCoreOperation(op_adjust_addr, new SMPCoreOpArgument() {{ op_func = op_asl; }});
		 opcode_table[0x0d] = new SMPCoreOperation(op_push_p, null);
		 opcode_table[0x0e] = new SMPCoreOperation(op_adjust_addr_a, new SMPCoreOpArgument() {{ op = 1; }});
		 opcode_table[0x0f] = new SMPCoreOperation(op_brk, null);
		 opcode_table[0x10] = new SMPCoreOperation(op_branch, new SMPCoreOpArgument() {{ flag = 0x80; value = 0; }});
		 opcode_table[0x11] = new SMPCoreOperation(op_tcall, new SMPCoreOpArgument() {{ n = 1; }});
		 opcode_table[0x12] = new SMPCoreOperation(op_setbit_dp, new SMPCoreOpArgument() {{ op = 0; value = 0x01; }});
		 opcode_table[0x13] = new SMPCoreOperation(op_bitbranch, new SMPCoreOpArgument() {{ mask = 0x01; value = 0; }});
		 opcode_table[0x14] = new SMPCoreOperation(op_read_a_dpx, new SMPCoreOpArgument() {{ op_func = op_or; }});
		 opcode_table[0x15] = new SMPCoreOperation(op_read_a_addrr, new SMPCoreOpArgument() {{ op_func = op_or; i = OpCode_X; }});
		 opcode_table[0x16] = new SMPCoreOperation(op_read_a_addrr, new SMPCoreOpArgument() {{ op_func = op_or; i = OpCode_Y; }});
		 opcode_table[0x17] = new SMPCoreOperation(op_read_a_idpy, new SMPCoreOpArgument() {{ op_func = op_or; }});
		 opcode_table[0x18] = new SMPCoreOperation(op_read_dp_const, new SMPCoreOpArgument() {{ op_func = op_or; }});
		 opcode_table[0x19] = new SMPCoreOperation(op_read_ix_iy, new SMPCoreOpArgument() {{ op_func = op_or; }});
		 opcode_table[0x1a] = new SMPCoreOperation(op_adjustw_dp, new SMPCoreOpArgument() {{ adjust = -1; }});
		 opcode_table[0x1b] = new SMPCoreOperation(op_adjust_dpx, new SMPCoreOpArgument() {{ op_func = op_asl; }});
		 opcode_table[0x1c] = new SMPCoreOperation(op_adjust_reg, new SMPCoreOpArgument() {{ op_func = op_asl; n = OpCode_A; }});
		 opcode_table[0x1d] = new SMPCoreOperation(op_adjust_reg, new SMPCoreOpArgument() {{ op_func = op_dec; n = OpCode_X; }});
		 opcode_table[0x1e] = new SMPCoreOperation(op_read_reg_addr, new SMPCoreOpArgument() {{ op_func = op_cmp; n = OpCode_X; }});
		 opcode_table[0x1f] = new SMPCoreOperation(op_jmp_iaddrx, null);
		 opcode_table[0x20] = new SMPCoreOperation(op_setbit, new SMPCoreOpArgument() {{ mask = 0x20; value = 0x00; }});
		 opcode_table[0x21] = new SMPCoreOperation(op_tcall, new SMPCoreOpArgument() {{ n = 2; }});
		 opcode_table[0x22] = new SMPCoreOperation(op_setbit_dp, new SMPCoreOpArgument() {{ op = 1; value = 0x02; }});
		 opcode_table[0x23] = new SMPCoreOperation(op_bitbranch, new SMPCoreOpArgument() {{ mask = 0x02; value = 1; }});
		 opcode_table[0x24] = new SMPCoreOperation(op_read_reg_dp, new SMPCoreOpArgument() {{ op_func = op_and; n = OpCode_A; }});
		 opcode_table[0x25] = new SMPCoreOperation(op_read_reg_addr, new SMPCoreOpArgument() {{ op_func = op_and; n = OpCode_A; }});
		 opcode_table[0x26] = new SMPCoreOperation(op_read_a_ix, new SMPCoreOpArgument() {{ op_func = op_and; }});
		 opcode_table[0x27] = new SMPCoreOperation(op_read_a_idpx, new SMPCoreOpArgument() {{ op_func = op_and; }});
		 opcode_table[0x28] = new SMPCoreOperation(op_read_reg_const, new SMPCoreOpArgument() {{ op_func = op_and; n = OpCode_A; }});
		 opcode_table[0x29] = new SMPCoreOperation(op_read_dp_dp, new SMPCoreOpArgument() {{ op_func = op_and; }});
		 opcode_table[0x2a] = new SMPCoreOperation(op_or1_bit, new SMPCoreOpArgument() {{ op = 1; }});
		 opcode_table[0x2b] = new SMPCoreOperation(op_adjust_dp, new SMPCoreOpArgument() {{ op_func = op_rol; }});
		 opcode_table[0x2c] = new SMPCoreOperation(op_adjust_addr, new SMPCoreOpArgument() {{ op_func = op_rol; }});
		 opcode_table[0x2d] = new SMPCoreOperation(op_push_reg, new SMPCoreOpArgument() {{ n = OpCode_A; }});
		 opcode_table[0x2e] = new SMPCoreOperation(op_cbne_dp, null);
		 opcode_table[0x2f] = new SMPCoreOperation(op_bra, null);
		 opcode_table[0x30] = new SMPCoreOperation(op_branch, new SMPCoreOpArgument() {{ flag = 0x80; value = 1; }});
		 opcode_table[0x31] = new SMPCoreOperation(op_tcall, new SMPCoreOpArgument() {{ n = 3; }});
		 opcode_table[0x32] = new SMPCoreOperation(op_setbit_dp, new SMPCoreOpArgument() {{ op = 0; value = 0x02; }});
		 opcode_table[0x33] = new SMPCoreOperation(op_bitbranch, new SMPCoreOpArgument() {{ mask = 0x02; value = 0; }});
		 opcode_table[0x34] = new SMPCoreOperation(op_read_a_dpx, new SMPCoreOpArgument() {{ op_func = op_and; }});
		 opcode_table[0x35] = new SMPCoreOperation(op_read_a_addrr, new SMPCoreOpArgument() {{ op_func = op_and; i = OpCode_X; }});
		 opcode_table[0x36] = new SMPCoreOperation(op_read_a_addrr, new SMPCoreOpArgument() {{ op_func = op_and; i = OpCode_Y; }});
		 opcode_table[0x37] = new SMPCoreOperation(op_read_a_idpy, new SMPCoreOpArgument() {{ op_func = op_and; }});
		 opcode_table[0x38] = new SMPCoreOperation(op_read_dp_const, new SMPCoreOpArgument() {{ op_func = op_and; }});
		 opcode_table[0x39] = new SMPCoreOperation(op_read_ix_iy, new SMPCoreOpArgument() {{ op_func = op_and; }});
		 opcode_table[0x3a] = new SMPCoreOperation(op_adjustw_dp, new SMPCoreOpArgument() {{ adjust = +1; }});
		 opcode_table[0x3b] = new SMPCoreOperation(op_adjust_dpx, new SMPCoreOpArgument() {{ op_func = op_rol; }});
		 opcode_table[0x3c] = new SMPCoreOperation(op_adjust_reg, new SMPCoreOpArgument() {{ op_func = op_rol; n = OpCode_A; }});
		 opcode_table[0x3d] = new SMPCoreOperation(op_adjust_reg, new SMPCoreOpArgument() {{ op_func = op_inc; n = OpCode_X; }});
		 opcode_table[0x3e] = new SMPCoreOperation(op_read_reg_dp, new SMPCoreOpArgument() {{ op_func = op_cmp; n = OpCode_X; }});
		 opcode_table[0x3f] = new SMPCoreOperation(op_call, null);
		 opcode_table[0x40] = new SMPCoreOperation(op_setbit, new SMPCoreOpArgument() {{ mask = 0x20; value = 0x20; }});
		 opcode_table[0x41] = new SMPCoreOperation(op_tcall, new SMPCoreOpArgument() {{ n = 4; }});
		 opcode_table[0x42] = new SMPCoreOperation(op_setbit_dp, new SMPCoreOpArgument() {{ op = 1; value = 0x04; }});
		 opcode_table[0x43] = new SMPCoreOperation(op_bitbranch, new SMPCoreOpArgument() {{ mask = 0x04; value = 1; }});
		 opcode_table[0x44] = new SMPCoreOperation(op_read_reg_dp, new SMPCoreOpArgument() {{ op_func = op_eor; n = OpCode_A; }});
		 opcode_table[0x45] = new SMPCoreOperation(op_read_reg_addr, new SMPCoreOpArgument() {{ op_func = op_eor; n = OpCode_A; }});
		 opcode_table[0x46] = new SMPCoreOperation(op_read_a_ix, new SMPCoreOpArgument() {{ op_func = op_eor; }});
		 opcode_table[0x47] = new SMPCoreOperation(op_read_a_idpx, new SMPCoreOpArgument() {{ op_func = op_eor; }});
		 opcode_table[0x48] = new SMPCoreOperation(op_read_reg_const, new SMPCoreOpArgument() {{ op_func = op_eor; n = OpCode_A; }});
		 opcode_table[0x49] = new SMPCoreOperation(op_read_dp_dp, new SMPCoreOpArgument() {{ op_func = op_eor; }});
		 opcode_table[0x4a] = new SMPCoreOperation(op_and1_bit, new SMPCoreOpArgument() {{ op = 0; }});
		 opcode_table[0x4b] = new SMPCoreOperation(op_adjust_dp, new SMPCoreOpArgument() {{ op_func = op_lsr; }});
		 opcode_table[0x4c] = new SMPCoreOperation(op_adjust_addr, new SMPCoreOpArgument() {{ op_func = op_lsr; }});
		 opcode_table[0x4d] = new SMPCoreOperation(op_push_reg, new SMPCoreOpArgument() {{ n = OpCode_X; }});
		 opcode_table[0x4e] = new SMPCoreOperation(op_adjust_addr_a, new SMPCoreOpArgument() {{ op = 0; }});
		 opcode_table[0x4f] = new SMPCoreOperation(op_pcall, null);
		 opcode_table[0x50] = new SMPCoreOperation(op_branch, new SMPCoreOpArgument() {{ flag = 0x40; value = 0; }});
		 opcode_table[0x51] = new SMPCoreOperation(op_tcall, new SMPCoreOpArgument() {{ n = 5; }});
		 opcode_table[0x52] = new SMPCoreOperation(op_setbit_dp, new SMPCoreOpArgument() {{ op = 0; value = 0x04; }});
		 opcode_table[0x53] = new SMPCoreOperation(op_bitbranch, new SMPCoreOpArgument() {{ mask = 0x04; value = 0; }});
		 opcode_table[0x54] = new SMPCoreOperation(op_read_a_dpx, new SMPCoreOpArgument() {{ op_func = op_eor; }});
		 opcode_table[0x55] = new SMPCoreOperation(op_read_a_addrr, new SMPCoreOpArgument() {{ op_func = op_eor; i = OpCode_X; }});
		 opcode_table[0x56] = new SMPCoreOperation(op_read_a_addrr, new SMPCoreOpArgument() {{ op_func = op_eor; i = OpCode_Y; }});
		 opcode_table[0x57] = new SMPCoreOperation(op_read_a_idpy, new SMPCoreOpArgument() {{ op_func = op_eor; }});
		 opcode_table[0x58] = new SMPCoreOperation(op_read_dp_const, new SMPCoreOpArgument() {{ op_func = op_eor; }});
		 opcode_table[0x59] = new SMPCoreOperation(op_read_ix_iy, new SMPCoreOpArgument() {{ op_func = op_eor; }});
		 opcode_table[0x5a] = new SMPCoreOperation(op_cmpw_ya_dp, null);
		 opcode_table[0x5b] = new SMPCoreOperation(op_adjust_dpx, new SMPCoreOpArgument() {{ op_func = op_lsr; }});
		 opcode_table[0x5c] = new SMPCoreOperation(op_adjust_reg, new SMPCoreOpArgument() {{ op_func = op_lsr; n = OpCode_A; }});
		 opcode_table[0x5d] = new SMPCoreOperation(op_mov_reg_reg, new SMPCoreOpArgument() {{ to = OpCode_X; from = OpCode_A; }});
		 opcode_table[0x5e] = new SMPCoreOperation(op_read_reg_addr, new SMPCoreOpArgument() {{ op_func = op_cmp; n = OpCode_Y; }});
		 opcode_table[0x5f] = new SMPCoreOperation(op_jmp_addr, null);
		 opcode_table[0x60] = new SMPCoreOperation(op_setbit, new SMPCoreOpArgument() {{ mask = 0x01; value = 0x00; }});
		 opcode_table[0x61] = new SMPCoreOperation(op_tcall, new SMPCoreOpArgument() {{ n = 6; }});
		 opcode_table[0x62] = new SMPCoreOperation(op_setbit_dp, new SMPCoreOpArgument() {{ op = 1; value = 0x08; }});
		 opcode_table[0x63] = new SMPCoreOperation(op_bitbranch, new SMPCoreOpArgument() {{ mask = 0x08; value = 1; }});
		 opcode_table[0x64] = new SMPCoreOperation(op_read_reg_dp, new SMPCoreOpArgument() {{ op_func = op_cmp; n = OpCode_A; }});
		 opcode_table[0x65] = new SMPCoreOperation(op_read_reg_addr, new SMPCoreOpArgument() {{ op_func = op_cmp; n = OpCode_A; }});
		 opcode_table[0x66] = new SMPCoreOperation(op_read_a_ix, new SMPCoreOpArgument() {{ op_func = op_cmp; }});
		 opcode_table[0x67] = new SMPCoreOperation(op_read_a_idpx, new SMPCoreOpArgument() {{ op_func = op_cmp; }});
		 opcode_table[0x68] = new SMPCoreOperation(op_read_reg_const, new SMPCoreOpArgument() {{ op_func = op_cmp; n = OpCode_A; }});
		 opcode_table[0x69] = new SMPCoreOperation(op_read_dp_dp, new SMPCoreOpArgument() {{ op_func = op_cmp; }});
		 opcode_table[0x6a] = new SMPCoreOperation(op_and1_bit, new SMPCoreOpArgument() {{ op = 1; }});
		 opcode_table[0x6b] = new SMPCoreOperation(op_adjust_dp, new SMPCoreOpArgument() {{ op_func = op_ror; }});
		 opcode_table[0x6c] = new SMPCoreOperation(op_adjust_addr, new SMPCoreOpArgument() {{ op_func = op_ror; }});
		 opcode_table[0x6d] = new SMPCoreOperation(op_push_reg, new SMPCoreOpArgument() {{ n = OpCode_Y; }});
		 opcode_table[0x6e] = new SMPCoreOperation(op_dbnz_dp, null);
		 opcode_table[0x6f] = new SMPCoreOperation(op_ret, null);
		 opcode_table[0x70] = new SMPCoreOperation(op_branch, new SMPCoreOpArgument() {{ flag = 0x40; value = 1; }});
		 opcode_table[0x71] = new SMPCoreOperation(op_tcall, new SMPCoreOpArgument() {{ n = 7; }});
		 opcode_table[0x72] = new SMPCoreOperation(op_setbit_dp, new SMPCoreOpArgument() {{ op = 0; value = 0x08; }});
		 opcode_table[0x73] = new SMPCoreOperation(op_bitbranch, new SMPCoreOpArgument() {{ mask = 0x08; value = 0; }});
		 opcode_table[0x74] = new SMPCoreOperation(op_read_a_dpx, new SMPCoreOpArgument() {{ op_func = op_cmp; }});
		 opcode_table[0x75] = new SMPCoreOperation(op_read_a_addrr, new SMPCoreOpArgument() {{ op_func = op_cmp; i = OpCode_X; }});
		 opcode_table[0x76] = new SMPCoreOperation(op_read_a_addrr, new SMPCoreOpArgument() {{ op_func = op_cmp; i = OpCode_Y; }});
		 opcode_table[0x77] = new SMPCoreOperation(op_read_a_idpy, new SMPCoreOpArgument() {{ op_func = op_cmp; }});
		 opcode_table[0x78] = new SMPCoreOperation(op_read_dp_const, new SMPCoreOpArgument() {{ op_func = op_cmp; }});
		 opcode_table[0x79] = new SMPCoreOperation(op_read_ix_iy, new SMPCoreOpArgument() {{ op_func = op_cmp; }});
		 opcode_table[0x7a] = new SMPCoreOperation(op_read_ya_dp, new SMPCoreOpArgument() {{ op_func = op_addw; }});
		 opcode_table[0x7b] = new SMPCoreOperation(op_adjust_dpx, new SMPCoreOpArgument() {{ op_func = op_ror; }});
		 opcode_table[0x7c] = new SMPCoreOperation(op_adjust_reg, new SMPCoreOpArgument() {{ op_func = op_ror; n = OpCode_A; }});
		 opcode_table[0x7d] = new SMPCoreOperation(op_mov_reg_reg, new SMPCoreOpArgument() {{ to = OpCode_A; from = OpCode_X; }});
		 opcode_table[0x7e] = new SMPCoreOperation(op_read_reg_dp, new SMPCoreOpArgument() {{ op_func = op_cmp; n = OpCode_Y; }});
		 opcode_table[0x7f] = new SMPCoreOperation(op_reti, null);
		 opcode_table[0x80] = new SMPCoreOperation(op_setbit, new SMPCoreOpArgument() {{ mask = 0x01; value = 0x01; }});
		 opcode_table[0x81] = new SMPCoreOperation(op_tcall, new SMPCoreOpArgument() {{ n = 8; }});
		 opcode_table[0x82] = new SMPCoreOperation(op_setbit_dp, new SMPCoreOpArgument() {{ op = 1; value = 0x10; }});
		 opcode_table[0x83] = new SMPCoreOperation(op_bitbranch, new SMPCoreOpArgument() {{ mask = 0x10; value = 1; }});
		 opcode_table[0x84] = new SMPCoreOperation(op_read_reg_dp, new SMPCoreOpArgument() {{ op_func = op_adc; n = OpCode_A; }});
		 opcode_table[0x85] = new SMPCoreOperation(op_read_reg_addr, new SMPCoreOpArgument() {{ op_func = op_adc; n = OpCode_A; }});
		 opcode_table[0x86] = new SMPCoreOperation(op_read_a_ix, new SMPCoreOpArgument() {{ op_func = op_adc; }});
		 opcode_table[0x87] = new SMPCoreOperation(op_read_a_idpx, new SMPCoreOpArgument() {{ op_func = op_adc; }});
		 opcode_table[0x88] = new SMPCoreOperation(op_read_reg_const, new SMPCoreOpArgument() {{ op_func = op_adc; n = OpCode_A; }});
		 opcode_table[0x89] = new SMPCoreOperation(op_read_dp_dp, new SMPCoreOpArgument() {{ op_func = op_adc; }});
		 opcode_table[0x8a] = new SMPCoreOperation(op_eor1_bit, null);
		 opcode_table[0x8b] = new SMPCoreOperation(op_adjust_dp, new SMPCoreOpArgument() {{ op_func = op_dec; }});
		 opcode_table[0x8c] = new SMPCoreOperation(op_adjust_addr, new SMPCoreOpArgument() {{ op_func = op_dec; }});
		 opcode_table[0x8d] = new SMPCoreOperation(op_mov_reg_const, new SMPCoreOpArgument() {{ n = OpCode_Y; }});
		 opcode_table[0x8e] = new SMPCoreOperation(op_pop_p, null);
		 opcode_table[0x8f] = new SMPCoreOperation(op_mov_dp_const, null);
		 opcode_table[0x90] = new SMPCoreOperation(op_branch, new SMPCoreOpArgument() {{ flag = 0x01; value = 0; }});
		 opcode_table[0x91] = new SMPCoreOperation(op_tcall, new SMPCoreOpArgument() {{ n = 9; }});
		 opcode_table[0x92] = new SMPCoreOperation(op_setbit_dp, new SMPCoreOpArgument() {{ op = 0; value = 0x10; }});
		 opcode_table[0x93] = new SMPCoreOperation(op_bitbranch, new SMPCoreOpArgument() {{ mask = 0x10; value = 0; }});
		 opcode_table[0x94] = new SMPCoreOperation(op_read_a_dpx, new SMPCoreOpArgument() {{ op_func = op_adc; }});
		 opcode_table[0x95] = new SMPCoreOperation(op_read_a_addrr, new SMPCoreOpArgument() {{ op_func = op_adc; i = OpCode_X; }});
		 opcode_table[0x96] = new SMPCoreOperation(op_read_a_addrr, new SMPCoreOpArgument() {{ op_func = op_adc; i = OpCode_Y; }});
		 opcode_table[0x97] = new SMPCoreOperation(op_read_a_idpy, new SMPCoreOpArgument() {{ op_func = op_adc; }});
		 opcode_table[0x98] = new SMPCoreOperation(op_read_dp_const, new SMPCoreOpArgument() {{ op_func = op_adc; }});
		 opcode_table[0x99] = new SMPCoreOperation(op_read_ix_iy, new SMPCoreOpArgument() {{ op_func = op_adc; }});
		 opcode_table[0x9a] = new SMPCoreOperation(op_read_ya_dp, new SMPCoreOpArgument() {{ op_func = op_subw; }});
		 opcode_table[0x9b] = new SMPCoreOperation(op_adjust_dpx, new SMPCoreOpArgument() {{ op_func = op_dec; }});
		 opcode_table[0x9c] = new SMPCoreOperation(op_adjust_reg, new SMPCoreOpArgument() {{ op_func = op_dec; n = OpCode_A; }});
		 opcode_table[0x9d] = new SMPCoreOperation(op_mov_reg_reg, new SMPCoreOpArgument() {{ to = OpCode_X; from = OpCode_SP; }});
		 opcode_table[0x9e] = new SMPCoreOperation(op_div_ya_x, null);
		 opcode_table[0x9f] = new SMPCoreOperation(op_xcn, null);
		 opcode_table[0xa0] = new SMPCoreOperation(op_seti, new SMPCoreOpArgument() {{ value = 1; }});
		 opcode_table[0xa1] = new SMPCoreOperation(op_tcall, new SMPCoreOpArgument() {{ n = 10; }});
		 opcode_table[0xa2] = new SMPCoreOperation(op_setbit_dp, new SMPCoreOpArgument() {{ op = 1; value = 0x20; }});
		 opcode_table[0xa3] = new SMPCoreOperation(op_bitbranch, new SMPCoreOpArgument() {{ mask = 0x20; value = 1; }});
		 opcode_table[0xa4] = new SMPCoreOperation(op_read_reg_dp, new SMPCoreOpArgument() {{ op_func = op_sbc; n = OpCode_A; }});
		 opcode_table[0xa5] = new SMPCoreOperation(op_read_reg_addr, new SMPCoreOpArgument() {{ op_func = op_sbc; n = OpCode_A; }});
		 opcode_table[0xa6] = new SMPCoreOperation(op_read_a_ix, new SMPCoreOpArgument() {{ op_func = op_sbc; }});
		 opcode_table[0xa7] = new SMPCoreOperation(op_read_a_idpx, new SMPCoreOpArgument() {{ op_func = op_sbc; }});
		 opcode_table[0xa8] = new SMPCoreOperation(op_read_reg_const, new SMPCoreOpArgument() {{ op_func = op_sbc; n = OpCode_A; }});
		 opcode_table[0xa9] = new SMPCoreOperation(op_read_dp_dp, new SMPCoreOpArgument() {{ op_func = op_sbc; }});
		 opcode_table[0xaa] = new SMPCoreOperation(op_mov1_c_bit, null);
		 opcode_table[0xab] = new SMPCoreOperation(op_adjust_dp, new SMPCoreOpArgument() {{ op_func = op_inc; }});
		 opcode_table[0xac] = new SMPCoreOperation(op_adjust_addr, new SMPCoreOpArgument() {{ op_func = op_inc; }});
		 opcode_table[0xad] = new SMPCoreOperation(op_read_reg_const, new SMPCoreOpArgument() {{ op_func = op_cmp; n = OpCode_Y; }});
		 opcode_table[0xae] = new SMPCoreOperation(op_pop_reg, new SMPCoreOpArgument() {{ n = OpCode_A; }});
		 opcode_table[0xaf] = new SMPCoreOperation(op_mov_ixinc_a, null);
		 opcode_table[0xb0] = new SMPCoreOperation(op_branch, new SMPCoreOpArgument() {{ flag = 0x01; value = 1; }});
		 opcode_table[0xb1] = new SMPCoreOperation(op_tcall, new SMPCoreOpArgument() {{ n = 11; }});
		 opcode_table[0xb2] = new SMPCoreOperation(op_setbit_dp, new SMPCoreOpArgument() {{ op = 0; value = 0x20; }});
		 opcode_table[0xb3] = new SMPCoreOperation(op_bitbranch, new SMPCoreOpArgument() {{ mask = 0x20; value = 0; }});
		 opcode_table[0xb4] = new SMPCoreOperation(op_read_a_dpx, new SMPCoreOpArgument() {{ op_func = op_sbc; }});
		 opcode_table[0xb5] = new SMPCoreOperation(op_read_a_addrr, new SMPCoreOpArgument() {{ op_func = op_sbc; i = OpCode_X; }});
		 opcode_table[0xb6] = new SMPCoreOperation(op_read_a_addrr, new SMPCoreOpArgument() {{ op_func = op_sbc; i = OpCode_Y; }});
		 opcode_table[0xb7] = new SMPCoreOperation(op_read_a_idpy, new SMPCoreOpArgument() {{ op_func = op_sbc; }});
		 opcode_table[0xb8] = new SMPCoreOperation(op_read_dp_const, new SMPCoreOpArgument() {{ op_func = op_sbc; }});
		 opcode_table[0xb9] = new SMPCoreOperation(op_read_ix_iy, new SMPCoreOpArgument() {{ op_func = op_sbc; }});
		 opcode_table[0xba] = new SMPCoreOperation(op_movw_ya_dp, null);
		 opcode_table[0xbb] = new SMPCoreOperation(op_adjust_dpx, new SMPCoreOpArgument() {{ op_func = op_inc; }});
		 opcode_table[0xbc] = new SMPCoreOperation(op_adjust_reg, new SMPCoreOpArgument() {{ op_func = op_inc; n = OpCode_A; }});
		 opcode_table[0xbd] = new SMPCoreOperation(op_mov_sp_x, null);
		 opcode_table[0xbe] = new SMPCoreOperation(op_das, null);
		 opcode_table[0xbf] = new SMPCoreOperation(op_mov_a_ixinc, null);
		 opcode_table[0xc0] = new SMPCoreOperation(op_seti, new SMPCoreOpArgument() {{ value = 0; }});
		 opcode_table[0xc1] = new SMPCoreOperation(op_tcall, new SMPCoreOpArgument() {{ n = 12; }});
		 opcode_table[0xc2] = new SMPCoreOperation(op_setbit_dp, new SMPCoreOpArgument() {{ op = 1; value = 0x40; }});
		 opcode_table[0xc3] = new SMPCoreOperation(op_bitbranch, new SMPCoreOpArgument() {{ mask = 0x40; value = 1; }});
		 opcode_table[0xc4] = new SMPCoreOperation(op_mov_dp_reg, new SMPCoreOpArgument() {{ n = OpCode_A; }});
		 opcode_table[0xc5] = new SMPCoreOperation(op_mov_addr_reg, new SMPCoreOpArgument() {{ n = OpCode_A; }});
		 opcode_table[0xc6] = new SMPCoreOperation(op_mov_ix_a, null);
		 opcode_table[0xc7] = new SMPCoreOperation(op_mov_idpx_a, null);
		 opcode_table[0xc8] = new SMPCoreOperation(op_read_reg_const, new SMPCoreOpArgument() {{ op_func = op_cmp; n = OpCode_X; }});
		 opcode_table[0xc9] = new SMPCoreOperation(op_mov_addr_reg, new SMPCoreOpArgument() {{ n = OpCode_X; }});
		 opcode_table[0xca] = new SMPCoreOperation(op_mov1_bit_c, null);
		 opcode_table[0xcb] = new SMPCoreOperation(op_mov_dp_reg, new SMPCoreOpArgument() {{ n = OpCode_Y; }});
		 opcode_table[0xcc] = new SMPCoreOperation(op_mov_addr_reg, new SMPCoreOpArgument() {{ n = OpCode_Y; }});
		 opcode_table[0xcd] = new SMPCoreOperation(op_mov_reg_const, new SMPCoreOpArgument() {{ n = OpCode_X; }});
		 opcode_table[0xce] = new SMPCoreOperation(op_pop_reg, new SMPCoreOpArgument() {{ n = OpCode_X; }});
		 opcode_table[0xcf] = new SMPCoreOperation(op_mul_ya, null);
		 opcode_table[0xd0] = new SMPCoreOperation(op_branch, new SMPCoreOpArgument() {{ flag = 0x02; value = 0; }});
		 opcode_table[0xd1] = new SMPCoreOperation(op_tcall, new SMPCoreOpArgument() {{ n = 13; }});
		 opcode_table[0xd2] = new SMPCoreOperation(op_setbit_dp, new SMPCoreOpArgument() {{ op = 0; value = 0x40; }});
		 opcode_table[0xd3] = new SMPCoreOperation(op_bitbranch, new SMPCoreOpArgument() {{ mask = 0x40; value = 0; }});
		 opcode_table[0xd4] = new SMPCoreOperation(op_mov_dpr_reg, new SMPCoreOpArgument() {{ n = OpCode_A; i = OpCode_X; }});
		 opcode_table[0xd5] = new SMPCoreOperation(op_mov_addrr_a, new SMPCoreOpArgument() {{ i = OpCode_X; }});
		 opcode_table[0xd6] = new SMPCoreOperation(op_mov_addrr_a, new SMPCoreOpArgument() {{ i = OpCode_Y; }});
		 opcode_table[0xd7] = new SMPCoreOperation(op_mov_idpy_a, null);
		 opcode_table[0xd8] = new SMPCoreOperation(op_mov_dp_reg, new SMPCoreOpArgument() {{ n = OpCode_X; }});
		 opcode_table[0xd9] = new SMPCoreOperation(op_mov_dpr_reg, new SMPCoreOpArgument() {{ n = OpCode_X; i = OpCode_Y; }});
		 opcode_table[0xda] = new SMPCoreOperation(op_movw_dp_ya, null);
		 opcode_table[0xdb] = new SMPCoreOperation(op_mov_dpr_reg, new SMPCoreOpArgument() {{ n = OpCode_Y; i = OpCode_X; }});
		 opcode_table[0xdc] = new SMPCoreOperation(op_adjust_reg, new SMPCoreOpArgument() {{ op_func = op_dec; n = OpCode_Y; }});
		 opcode_table[0xdd] = new SMPCoreOperation(op_mov_reg_reg, new SMPCoreOpArgument() {{ to = OpCode_A; from = OpCode_Y; }});
		 opcode_table[0xde] = new SMPCoreOperation(op_cbne_dpx, null);
		 opcode_table[0xdf] = new SMPCoreOperation(op_daa, null);
		 opcode_table[0xe0] = new SMPCoreOperation(op_setbit, new SMPCoreOpArgument() {{ mask = 0x48; value = 0x00; }});
		 opcode_table[0xe1] = new SMPCoreOperation(op_tcall, new SMPCoreOpArgument() {{ n = 14; }});
		 opcode_table[0xe2] = new SMPCoreOperation(op_setbit_dp, new SMPCoreOpArgument() {{ op = 1; value = 0x80; }});
		 opcode_table[0xe3] = new SMPCoreOperation(op_bitbranch, new SMPCoreOpArgument() {{ mask = 0x80; value = 1; }});
		 opcode_table[0xe4] = new SMPCoreOperation(op_mov_reg_dp, new SMPCoreOpArgument() {{ n = OpCode_A; }});
		 opcode_table[0xe5] = new SMPCoreOperation(op_mov_reg_addr, new SMPCoreOpArgument() {{ n = OpCode_A; }});
		 opcode_table[0xe6] = new SMPCoreOperation(op_mov_a_ix, null);
		 opcode_table[0xe7] = new SMPCoreOperation(op_mov_a_idpx, null);
		 opcode_table[0xe8] = new SMPCoreOperation(op_mov_reg_const, new SMPCoreOpArgument() {{ n = OpCode_A; }});
		 opcode_table[0xe9] = new SMPCoreOperation(op_mov_reg_addr, new SMPCoreOpArgument() {{ n = OpCode_X; }});
		 opcode_table[0xea] = new SMPCoreOperation(op_not1_bit, null);
		 opcode_table[0xeb] = new SMPCoreOperation(op_mov_reg_dp, new SMPCoreOpArgument() {{ n = OpCode_Y; }});
		 opcode_table[0xec] = new SMPCoreOperation(op_mov_reg_addr, new SMPCoreOpArgument() {{ n = OpCode_Y; }});
		 opcode_table[0xed] = new SMPCoreOperation(op_notc, null);
		 opcode_table[0xee] = new SMPCoreOperation(op_pop_reg, new SMPCoreOpArgument() {{ n = OpCode_Y; }});
		 opcode_table[0xef] = new SMPCoreOperation(op_wait, null);
		 opcode_table[0xf0] = new SMPCoreOperation(op_branch, new SMPCoreOpArgument() {{ flag = 0x02; value = 1; }});
		 opcode_table[0xf1] = new SMPCoreOperation(op_tcall, new SMPCoreOpArgument() {{ n = 15; }});
		 opcode_table[0xf2] = new SMPCoreOperation(op_setbit_dp, new SMPCoreOpArgument() {{ op = 0; value = 0x80; }});
		 opcode_table[0xf3] = new SMPCoreOperation(op_bitbranch, new SMPCoreOpArgument() {{ mask = 0x80; value = 0; }});
		 opcode_table[0xf4] = new SMPCoreOperation(op_mov_reg_dpr, new SMPCoreOpArgument() {{ n = OpCode_A; i = OpCode_X; }});
		 opcode_table[0xf5] = new SMPCoreOperation(op_mov_a_addrr, new SMPCoreOpArgument() {{ i = OpCode_X; }});
		 opcode_table[0xf6] = new SMPCoreOperation(op_mov_a_addrr, new SMPCoreOpArgument() {{ i = OpCode_Y; }});
		 opcode_table[0xf7] = new SMPCoreOperation(op_mov_a_idpy, null);
		 opcode_table[0xf8] = new SMPCoreOperation(op_mov_reg_dp, new SMPCoreOpArgument() {{ n = OpCode_X; }});
		 opcode_table[0xf9] = new SMPCoreOperation(op_mov_reg_dpr, new SMPCoreOpArgument() {{ n = OpCode_X; i = OpCode_Y; }});
		 opcode_table[0xfa] = new SMPCoreOperation(op_mov_dp_dp, null);
		 opcode_table[0xfb] = new SMPCoreOperation(op_mov_reg_dpr, new SMPCoreOpArgument() {{ n = OpCode_Y; i = OpCode_X; }});
		 opcode_table[0xfc] = new SMPCoreOperation(op_adjust_reg, new SMPCoreOpArgument() {{ op_func = op_inc; n = OpCode_Y; }});
		 opcode_table[0xfd] = new SMPCoreOperation(op_mov_reg_reg, new SMPCoreOpArgument() {{ to = OpCode_Y; from = OpCode_A; }});
		 opcode_table[0xfe] = new SMPCoreOperation(op_dbnz_y, null);
		 opcode_table[0xff] = new SMPCoreOperation(op_wait, null);
	}

	public SMPCore()
	{
		initialize_opcode_table();
	}
}
