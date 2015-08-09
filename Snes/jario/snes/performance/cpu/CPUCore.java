/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.cpu;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CPUCore implements CPUCoreInterface, java.io.Serializable
{
	protected int op_readpc()
	{
		int addr = (regs.pc.b() << 16) + regs.pc.w();
		regs.pc.w(regs.pc.w() + 1);
		return op_read(addr) & 0xFF;
	}

	private int op_readstack()
	{
		if (regs.e)
		{
			regs.s.l(regs.s.l() + 1);
		}
		else
		{
			regs.s.w(regs.s.w() + 1);
		}
		return op_read(regs.s.w()) & 0xFF;
	}

	private int op_readstackn()
	{
		regs.s.w(regs.s.w() + 1);
		return op_read(regs.s.w()) & 0xFF;
	}

	private int op_readaddr(int addr)
	{
		return op_read(addr & 0xffff) & 0xFF;
	}

	private int op_readlong(int addr)
	{
		return op_read(addr & 0xffffff) & 0xFF;
	}

	private int op_readdbr(int addr)
	{
		return op_read((((regs.db & 0xFF) << 16) + addr) & 0xffffff) & 0xFF;
	}

	private int op_readpbr(int addr)
	{
		return op_read(((regs.pc.b() << 16) + (addr & 0xffff))) & 0xFF;
	}

	private int op_readdp(int addr)
	{
		if (regs.e && regs.d.l() == 0x00)
		{
			return op_read((regs.d.get() & 0xff00) + ((regs.d.get() + (addr & 0xffff)) & 0xff)) & 0xFF;
		}
		else
		{
			return op_read((regs.d.get() + (addr & 0xffff)) & 0xffff) & 0xFF;
		}
	}

	private int op_readsp(int addr)
	{
		return op_read((regs.s.get() + (addr & 0xffff)) & 0xffff) & 0xFF;
	}

	protected void op_writestack(int data)
	{
		op_write(regs.s.w(), (byte) data);
		if (regs.e)
		{
			regs.s.l(regs.s.l() - 1);
		}
		else
		{
			regs.s.w(regs.s.w() - 1);
		}

	}

	private void op_writestackn(int data)
	{
		op_write(regs.s.w(), (byte) data);
		regs.s.w(regs.s.w() - 1);
	}

	private void op_writelong(int addr, int data)
	{
		op_write(addr & 0xffffff, (byte) data);
	}

	private void op_writedbr(int addr, int data)
	{
		op_write((((regs.db & 0xFF) << 16) + addr) & 0xffffff, (byte) data);
	}

	private void op_writedp(int addr, int data)
	{
		if (regs.e && regs.d.l() == 0x00)
		{
			op_write((regs.d.get() & 0xff00) + ((regs.d.get() + (addr & 0xffff)) & 0xff), (byte) data);
		}
		else
		{
			op_write((regs.d.get() + (addr & 0xffff)) & 0xffff, (byte) data);
		}
	}

	private void op_writesp(int addr, int data)
	{
		op_write((regs.s.get() + (addr & 0xffff)) & 0xffff, (byte) data);
	}
	
	public int sp, dp;
	public Reg24 aa = new Reg24();
	public Reg24 rd = new Reg24();
	
	public Regs regs = new Regs();
	

	

	private void op_io_irq()
	{
		if (interrupt_pending())
		{
			// modify I/O cycle to bus read cycle, do not increment PC
			op_read(regs.pc.get());
		}
		else
		{
			op_io();
		}
	}

	private void op_io_cond2()
	{
		if (regs.d.l() != 0x00)
		{
			op_io();
		}
	}

	private void op_io_cond4(int x, int y)
	{
		if (!regs.p.x || (x & 0xff00) != (y & 0xff00))
		{
			op_io();
		}
	}

	private void op_io_cond6(int addr)
	{
		if (regs.e && (regs.pc.w() & 0xff00) != (addr & 0xff00))
		{
			op_io();
		}
	}

	public transient CPUCoreOp op_adc_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int result;

			if (!regs.p.d)
			{
				result = regs.a.l() + rd.l() + (regs.p.c ? 1 : 0);
			}
			else
			{
				result = (regs.a.l() & 0x0f) + (rd.l() & 0x0f) + ((regs.p.c ? 1 : 0) << 0);
				if (result > 0x09)
				{
					result += 0x06;
				}
				regs.p.c = result > 0x0f;
				result = (regs.a.l() & 0xf0) + (rd.l() & 0xf0) + ((regs.p.c ? 1 : 0) << 4) + (result & 0x0f);
			}

			regs.p.v = (~(regs.a.l() ^ rd.l()) & (regs.a.l() ^ result) & 0x80) != 0;
			if (regs.p.d && result > 0x9f)
			{
				result += 0x60;
			}
			regs.p.c = result > 0xff;
			regs.p.n = (result & 0x80) != 0;
			regs.p.z = (result & 0xFF) == 0;

			regs.a.l(result);
		}
	};

	public transient CPUCoreOp op_adc_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int result;

			if (!regs.p.d)
			{
				result = regs.a.w() + rd.w() + (regs.p.c ? 1 : 0);
			}
			else
			{
				result = (regs.a.w() & 0x000f) + (rd.w() & 0x000f) + ((regs.p.c ? 1 : 0) << 0);
				if (result > 0x0009)
				{
					result += 0x0006;
				}
				regs.p.c = result > 0x000f;
				result = (regs.a.w() & 0x00f0) + (rd.w() & 0x00f0) + ((regs.p.c ? 1 : 0) << 4) + (result & 0x000f);
				if (result > 0x009f)
				{
					result += 0x0060;
				}
				regs.p.c = result > 0x00ff;
				result = (regs.a.w() & 0x0f00) + (rd.w() & 0x0f00) + ((regs.p.c ? 1 : 0) << 8) + (result & 0x00ff);
				if (result > 0x09ff)
				{
					result += 0x0600;
				}
				regs.p.c = result > 0x0fff;
				result = (regs.a.w() & 0xf000) + (rd.w() & 0xf000) + ((regs.p.c ? 1 : 0) << 12) + (result & 0x0fff);
			}

			regs.p.v = (~(regs.a.w() ^ rd.w()) & (regs.a.w() ^ result) & 0x8000) != 0;
			if (regs.p.d && result > 0x9fff)
			{
				result += 0x6000;
			}
			regs.p.c = result > 0xffff;
			regs.p.n = (result & 0x8000) != 0;
			regs.p.z = (result & 0xFFFF) == 0;

			regs.a.w(result);
		}
	};

	public transient CPUCoreOp op_and_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.a.l(regs.a.l() & rd.l());
			regs.p.n = (regs.a.l() & 0x80) != 0;
			regs.p.z = regs.a.l() == 0;
		}
	};

	public transient CPUCoreOp op_and_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.a.w(regs.a.w() & rd.w());
			regs.p.n = (regs.a.w() & 0x8000) != 0;
			regs.p.z = regs.a.w() == 0;
		}
	};

	public transient CPUCoreOp op_bit_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.p.n = (rd.l() & 0x80) != 0;
			regs.p.v = (rd.l() & 0x40) != 0;
			regs.p.z = (rd.l() & regs.a.l()) == 0;
		}
	};

	public transient CPUCoreOp op_bit_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.p.n = (rd.w() & 0x8000) != 0;
			regs.p.v = (rd.w() & 0x4000) != 0;
			regs.p.z = (rd.w() & regs.a.w()) == 0;
		}
	};

	public transient CPUCoreOp op_cmp_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int r = regs.a.l() - rd.l();
			regs.p.n = (r & 0x80) != 0;
			regs.p.z = (r & 0xFF) == 0;
			regs.p.c = r >= 0;
		}
	};

	public transient CPUCoreOp op_cmp_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int r = regs.a.w() - rd.w();
			regs.p.n = (r & 0x8000) != 0;
			regs.p.z = (r & 0xFFFF) == 0;
			regs.p.c = r >= 0;
		}
	};

	public transient CPUCoreOp op_cpx_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int r = regs.x.l() - rd.l();
			regs.p.n = (r & 0x80) != 0;
			regs.p.z = (r & 0xFF) == 0;
			regs.p.c = r >= 0;
		}
	};

	public transient CPUCoreOp op_cpx_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int r = regs.x.w() - rd.w();
			regs.p.n = (r & 0x8000) != 0;
			regs.p.z = (r & 0xFFFF) == 0;
			regs.p.c = r >= 0;
		}
	};

	public transient CPUCoreOp op_cpy_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int r = regs.y.l() - rd.l();
			regs.p.n = (r & 0x80) != 0;
			regs.p.z = (r & 0xFF) == 0;
			regs.p.c = r >= 0;
		}
	};

	public transient CPUCoreOp op_cpy_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int r = regs.y.w() - rd.w();
			regs.p.n = (r & 0x8000) != 0;
			regs.p.z = (r & 0xFFFF) == 0;
			regs.p.c = r >= 0;
		}
	};

	public transient CPUCoreOp op_eor_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.a.l(regs.a.l() ^ rd.l());
			regs.p.n = (regs.a.l() & 0x80) != 0;
			regs.p.z = regs.a.l() == 0;
		}
	};

	public transient CPUCoreOp op_eor_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.a.w(regs.a.w() ^ rd.w());
			regs.p.n = (regs.a.w() & 0x8000) != 0;
			regs.p.z = regs.a.w() == 0;
		}
	};

	public transient CPUCoreOp op_lda_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.a.l(rd.l());
			regs.p.n = (regs.a.l() & 0x80) != 0;
			regs.p.z = regs.a.l() == 0;
		}
	};

	public transient CPUCoreOp op_lda_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.a.w(rd.w());
			regs.p.n = (regs.a.w() & 0x8000) != 0;
			regs.p.z = regs.a.w() == 0;
		}
	};

	public transient CPUCoreOp op_ldx_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.x.l(rd.l());
			regs.p.n = (regs.x.l() & 0x80) != 0;
			regs.p.z = regs.x.l() == 0;
		}
	};

	public transient CPUCoreOp op_ldx_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.x.w(rd.w());
			regs.p.n = (regs.x.w() & 0x8000) != 0;
			regs.p.z = regs.x.w() == 0;
		}
	};

	public transient CPUCoreOp op_ldy_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.y.l(rd.l());
			regs.p.n = (regs.y.l() & 0x80) != 0;
			regs.p.z = regs.y.l() == 0;
		}
	};

	public transient CPUCoreOp op_ldy_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.y.w(rd.w());
			regs.p.n = (regs.y.w() & 0x8000) != 0;
			regs.p.z = regs.y.w() == 0;
		}
	};

	public transient CPUCoreOp op_ora_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.a.l(regs.a.l() | rd.l());
			regs.p.n = (regs.a.l() & 0x80) != 0;
			regs.p.z = regs.a.l() == 0;
		}
	};

	public transient CPUCoreOp op_ora_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.a.w(regs.a.w() | rd.w());
			regs.p.n = (regs.a.w() & 0x8000) != 0;
			regs.p.z = regs.a.w() == 0;
		}
	};

	public transient CPUCoreOp op_sbc_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int result;
			rd.l(rd.l() ^ 0xff);

			if (!regs.p.d)
			{
				result = regs.a.l() + rd.l() + (regs.p.c ? 1 : 0);
			}
			else
			{
				result = (regs.a.l() & 0x0f) + (rd.l() & 0x0f) + ((regs.p.c ? 1 : 0) << 0);
				if (result <= 0x0f)
				{
					result -= 0x06;
				}
				regs.p.c = result > 0x0f;
				result = (regs.a.l() & 0xf0) + (rd.l() & 0xf0) + ((regs.p.c ? 1 : 0) << 4) + (result & 0x0f);
			}

			regs.p.v = (~(regs.a.l() ^ rd.l()) & (regs.a.l() ^ result) & 0x80) != 0;
			if (regs.p.d && result <= 0xff)
			{
				result -= 0x60;
			}
			regs.p.c = result > 0xff;
			regs.p.n = (result & 0x80) != 0;
			regs.p.z = (result & 0xFF) == 0;

			regs.a.l(result);
		}
	};

	public transient CPUCoreOp op_sbc_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int result;
			rd.w(rd.w() ^ 0xffff);

			if (!regs.p.d)
			{
				result = regs.a.w() + rd.w() + (regs.p.c ? 1 : 0);
			}
			else
			{
				result = (regs.a.w() & 0x000f) + (rd.w() & 0x000f) + ((regs.p.c ? 1 : 0) << 0);
				if (result <= 0x000f)
				{
					result -= 0x0006;
				}
				regs.p.c = result > 0x000f;
				result = (regs.a.w() & 0x00f0) + (rd.w() & 0x00f0) + ((regs.p.c ? 1 : 0) << 4) + (result & 0x000f);
				if (result <= 0x00ff)
				{
					result -= 0x0060;
				}
				regs.p.c = result > 0x00ff;
				result = (regs.a.w() & 0x0f00) + (rd.w() & 0x0f00) + ((regs.p.c ? 1 : 0) << 8) + (result & 0x00ff);
				if (result <= 0x0fff)
				{
					result -= 0x0600;
				}
				regs.p.c = result > 0x0fff;
				result = (regs.a.w() & 0xf000) + (rd.w() & 0xf000) + ((regs.p.c ? 1 : 0) << 12) + (result & 0x0fff);
			}

			regs.p.v = (~(regs.a.w() ^ rd.w()) & (regs.a.w() ^ result) & 0x8000) != 0;
			if (regs.p.d && result <= 0xffff)
			{
				result -= 0x6000;
			}
			regs.p.c = result > 0xffff;
			regs.p.n = (result & 0x8000) != 0;
			regs.p.z = (result & 0xFFFF) == 0;

			regs.a.w(result);
		}
	};

	public transient CPUCoreOp op_inc_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			rd.l(rd.l() + 1);
			regs.p.n = (rd.l() & 0x80) != 0;
			regs.p.z = rd.l() == 0;
		}
	};

	public transient CPUCoreOp op_inc_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			rd.w(rd.w() + 1);
			regs.p.n = (rd.w() & 0x8000) != 0;
			regs.p.z = rd.w() == 0;
		}
	};

	public transient CPUCoreOp op_dec_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			rd.l(rd.l() - 1);
			regs.p.n = (rd.l() & 0x80) != 0;
			regs.p.z = rd.l() == 0;
		}
	};

	public transient CPUCoreOp op_dec_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			rd.w(rd.w() - 1);
			regs.p.n = (rd.w() & 0x8000) != 0;
			regs.p.z = rd.w() == 0;
		}
	};

	public transient CPUCoreOp op_asl_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.p.c = (rd.l() & 0x80) != 0;
			rd.l(rd.l() << 1);
			regs.p.n = (rd.l() & 0x80) != 0;
			regs.p.z = rd.l() == 0;
		}
	};

	public transient CPUCoreOp op_asl_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.p.c = (rd.w() & 0x8000) != 0;
			rd.w(rd.w() << 1);
			regs.p.n = (rd.w() & 0x8000) != 0;
			regs.p.z = rd.w() == 0;
		}
	};

	public transient CPUCoreOp op_lsr_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.p.c = (rd.l() & 1) != 0;
			rd.l(rd.l() >> 1);
			regs.p.n = (rd.l() & 0x80) != 0;
			regs.p.z = rd.l() == 0;
		}
	};

	public transient CPUCoreOp op_lsr_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.p.c = (rd.w() & 1) != 0;
			rd.w(rd.w() >> 1);
			regs.p.n = (rd.w() & 0x8000) != 0;
			regs.p.z = rd.w() == 0;
		}
	};

	public transient CPUCoreOp op_rol_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int carry = regs.p.c ? 1 : 0;
			regs.p.c = (rd.l() & 0x80) != 0;
			rd.l((rd.l() << 1) | carry);
			regs.p.n = (rd.l() & 0x80) != 0;
			regs.p.z = rd.l() == 0;
		}
	};

	public transient CPUCoreOp op_rol_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int carry = regs.p.c ? 1 : 0;
			regs.p.c = (rd.w() & 0x8000) != 0;
			rd.w((rd.w() << 1) | carry);
			regs.p.n = (rd.w() & 0x8000) != 0;
			regs.p.z = rd.w() == 0;
		}
	};

	public transient CPUCoreOp op_ror_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int carry = (regs.p.c ? 1 : 0) << 7;
			regs.p.c = (rd.l() & 1) != 0;
			rd.l(carry | (rd.l() >> 1));
			regs.p.n = (rd.l() & 0x80) != 0;
			regs.p.z = rd.l() == 0;
		}
	};

	public transient CPUCoreOp op_ror_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int carry = (regs.p.c ? 1 : 0) << 15;
			regs.p.c = (rd.w() & 1) != 0;
			rd.w(carry | (rd.w() >> 1));
			regs.p.n = (rd.w() & 0x8000) != 0;
			regs.p.z = rd.w() == 0;
		}
	};

	public transient CPUCoreOp op_trb_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.p.z = (rd.l() & regs.a.l()) == 0;
			rd.l(rd.l() & (~regs.a.l()));
		}
	};

	public transient CPUCoreOp op_trb_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.p.z = (rd.w() & regs.a.w()) == 0;
			rd.w(rd.w() & (~regs.a.w()));
		}
	};

	public transient CPUCoreOp op_tsb_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.p.z = (rd.l() & regs.a.l()) == 0;
			rd.l(rd.l() | regs.a.l());
		}
	};

	public transient CPUCoreOp op_tsb_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			regs.p.z = (rd.w() & regs.a.w()) == 0;
			rd.w(rd.w() | regs.a.w());
		}
	};

	public transient CPUCoreOp op_read_const_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			last_cycle();
			rd.l(op_readpc());
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_const_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			rd.l(op_readpc());
			last_cycle();
			rd.h(op_readpc());
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_bit_const_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			last_cycle();
			rd.l(op_readpc());
			regs.p.z = (rd.l() & regs.a.l()) == 0;
		}
	};

	public transient CPUCoreOp op_read_bit_const_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			rd.l(op_readpc());
			last_cycle();
			rd.h(op_readpc());
			regs.p.z = (rd.w() & regs.a.w()) == 0;
		}
	};

	public transient CPUCoreOp op_read_addr_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			aa.l(op_readpc());
			aa.h(op_readpc());
			last_cycle();
			rd.l(op_readdbr(aa.w()));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_addr_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			aa.l(op_readpc());
			aa.h(op_readpc());
			rd.l(op_readdbr(aa.w() + 0));
			last_cycle();
			rd.h(op_readdbr(aa.w() + 1));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_addrx_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			aa.l(op_readpc());
			aa.h(op_readpc());
			op_io_cond4(aa.w(), aa.w() + regs.x.w());
			last_cycle();
			rd.l(op_readdbr(aa.w() + regs.x.w()));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_addrx_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			aa.l(op_readpc());
			aa.h(op_readpc());
			op_io_cond4(aa.w(), aa.w() + regs.x.w());
			rd.l(op_readdbr(aa.w() + regs.x.w() + 0));
			last_cycle();
			rd.h(op_readdbr(aa.w() + regs.x.w() + 1));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_addry_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			aa.l(op_readpc());
			aa.h(op_readpc());
			op_io_cond4(aa.w(), aa.w() + regs.y.w());
			last_cycle();
			rd.l(op_readdbr(aa.w() + regs.y.w()));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_addry_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			aa.l(op_readpc());
			aa.h(op_readpc());
			op_io_cond4(aa.w(), aa.w() + regs.y.w());
			rd.l(op_readdbr(aa.w() + regs.y.w() + 0));
			last_cycle();
			rd.h(op_readdbr(aa.w() + regs.y.w() + 1));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_long_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			aa.l(op_readpc());
			aa.h(op_readpc());
			aa.b(op_readpc());
			last_cycle();
			rd.l(op_readlong(aa.get()));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_long_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			aa.l(op_readpc());
			aa.h(op_readpc());
			aa.b(op_readpc());
			rd.l(op_readlong(aa.get() + 0));
			last_cycle();
			rd.h(op_readlong(aa.get() + 1));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_longx_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			aa.l(op_readpc());
			aa.h(op_readpc());
			aa.b(op_readpc());
			last_cycle();
			rd.l(op_readlong(aa.get() + regs.x.w()));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_longx_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			aa.l(op_readpc());
			aa.h(op_readpc());
			aa.b(op_readpc());
			rd.l(op_readlong(aa.get() + regs.x.w() + 0));
			last_cycle();
			rd.h(op_readlong(aa.get() + regs.x.w() + 1));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_dp_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			dp = op_readpc();
			op_io_cond2();
			last_cycle();
			rd.l(op_readdp(dp));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_dp_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			dp = op_readpc();
			op_io_cond2();
			rd.l(op_readdp(dp + 0));
			last_cycle();
			rd.h(op_readdp(dp + 1));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_dpr_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			int n = args.x;
			dp = op_readpc();
			op_io_cond2();
			op_io();
			last_cycle();
			rd.l(op_readdp(dp + regs.r[n].w()));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_dpr_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			int n = args.x;
			dp = op_readpc();
			op_io_cond2();
			op_io();
			rd.l(op_readdp(dp + regs.r[n].w() + 0));
			last_cycle();
			rd.h(op_readdp(dp + regs.r[n].w() + 1));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_idp_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			dp = op_readpc();
			op_io_cond2();
			aa.l(op_readdp(dp + 0));
			aa.h(op_readdp(dp + 1));
			last_cycle();
			rd.l(op_readdbr(aa.w()));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_idp_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			dp = op_readpc();
			op_io_cond2();
			aa.l(op_readdp(dp + 0));
			aa.h(op_readdp(dp + 1));
			rd.l(op_readdbr(aa.w() + 0));
			last_cycle();
			rd.h(op_readdbr(aa.w() + 1));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_idpx_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			dp = op_readpc();
			op_io_cond2();
			op_io();
			aa.l(op_readdp(dp + regs.x.w() + 0));
			aa.h(op_readdp(dp + regs.x.w() + 1));
			last_cycle();
			rd.l(op_readdbr(aa.w()));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_idpx_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			dp = op_readpc();
			op_io_cond2();
			op_io();
			aa.l(op_readdp(dp + regs.x.w() + 0));
			aa.h(op_readdp(dp + regs.x.w() + 1));
			rd.l(op_readdbr(aa.w() + 0));
			last_cycle();
			rd.h(op_readdbr(aa.w() + 1));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_idpy_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			dp = op_readpc();
			op_io_cond2();
			aa.l(op_readdp(dp + 0));
			aa.h(op_readdp(dp + 1));
			op_io_cond4(aa.w(), aa.w() + regs.y.w());
			last_cycle();
			rd.l(op_readdbr(aa.w() + regs.y.w()));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_idpy_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			dp = op_readpc();
			op_io_cond2();
			aa.l(op_readdp(dp + 0));
			aa.h(op_readdp(dp + 1));
			op_io_cond4(aa.w(), aa.w() + regs.y.w());
			rd.l(op_readdbr(aa.w() + regs.y.w() + 0));
			last_cycle();
			rd.h(op_readdbr(aa.w() + regs.y.w() + 1));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_ildp_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			dp = op_readpc();
			op_io_cond2();
			aa.l(op_readdp(dp + 0));
			aa.h(op_readdp(dp + 1));
			aa.b(op_readdp(dp + 2));
			last_cycle();
			rd.l(op_readlong(aa.get()));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_ildp_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			dp = op_readpc();
			op_io_cond2();
			aa.l(op_readdp(dp + 0));
			aa.h(op_readdp(dp + 1));
			aa.b(op_readdp(dp + 2));
			rd.l(op_readlong(aa.get() + 0));
			last_cycle();
			rd.h(op_readlong(aa.get() + 1));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_ildpy_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			dp = op_readpc();
			op_io_cond2();
			aa.l(op_readdp(dp + 0));
			aa.h(op_readdp(dp + 1));
			aa.b(op_readdp(dp + 2));
			last_cycle();
			rd.l(op_readlong(aa.get() + regs.y.w()));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_ildpy_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			dp = op_readpc();
			op_io_cond2();
			aa.l(op_readdp(dp + 0));
			aa.h(op_readdp(dp + 1));
			aa.b(op_readdp(dp + 2));
			rd.l(op_readlong(aa.get() + regs.y.w() + 0));
			last_cycle();
			rd.h(op_readlong(aa.get() + regs.y.w() + 1));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_sr_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			sp = op_readpc();
			op_io();
			last_cycle();
			rd.l(op_readsp(sp));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_sr_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			sp = op_readpc();
			op_io();
			rd.l(op_readsp(sp + 0));
			last_cycle();
			rd.h(op_readsp(sp + 1));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_isry_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			sp = op_readpc();
			op_io();
			aa.l(op_readsp(sp + 0));
			aa.h(op_readsp(sp + 1));
			op_io();
			last_cycle();
			rd.l(op_readdbr(aa.w() + regs.y.w()));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_read_isry_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			sp = op_readpc();
			op_io();
			aa.l(op_readsp(sp + 0));
			aa.h(op_readsp(sp + 1));
			op_io();
			rd.l(op_readdbr(aa.w() + regs.y.w() + 0));
			last_cycle();
			rd.h(op_readdbr(aa.w() + regs.y.w() + 1));
			op.Invoke(null);
		}
	};

	public transient CPUCoreOp op_write_addr_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int n = args.x;
			aa.l(op_readpc());
			aa.h(op_readpc());
			last_cycle();
			op_writedbr(aa.w(), regs.r[n].get());
		}
	};

	public transient CPUCoreOp op_write_addr_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int n = args.x;
			aa.l(op_readpc());
			aa.h(op_readpc());
			op_writedbr(aa.w() + 0, regs.r[n].get() >> 0);
			last_cycle();
			op_writedbr(aa.w() + 1, regs.r[n].get() >> 8);
		}
	};

	public transient CPUCoreOp op_write_addrr_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int n = args.x;
			int i = args.y;
			aa.l(op_readpc());
			aa.h(op_readpc());
			op_io();
			last_cycle();
			op_writedbr(aa.w() + regs.r[i].get(), regs.r[n].get());
		}
	};

	public transient CPUCoreOp op_write_addrr_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int n = args.x;
			int i = args.y;
			aa.l(op_readpc());
			aa.h(op_readpc());
			op_io();
			op_writedbr(aa.w() + regs.r[i].get() + 0, regs.r[n].get() >> 0);
			last_cycle();
			op_writedbr(aa.w() + regs.r[i].get() + 1, regs.r[n].get() >> 8);
		}
	};

	public transient CPUCoreOp op_write_longr_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int i = args.x;
			aa.l(op_readpc());
			aa.h(op_readpc());
			aa.b(op_readpc());
			last_cycle();
			op_writelong(aa.get() + regs.r[i].get(), regs.a.l());
		}
	};

	public transient CPUCoreOp op_write_longr_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int i = args.x;
			aa.l(op_readpc());
			aa.h(op_readpc());
			aa.b(op_readpc());
			op_writelong(aa.get() + regs.r[i].get() + 0, regs.a.l());
			last_cycle();
			op_writelong(aa.get() + regs.r[i].get() + 1, regs.a.h());
		}
	};

	public transient CPUCoreOp op_write_dp_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int n = args.x;
			dp = op_readpc();
			op_io_cond2();
			last_cycle();
			op_writedp(dp, regs.r[n].get());
		}
	};

	public transient CPUCoreOp op_write_dp_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int n = args.x;
			dp = op_readpc();
			op_io_cond2();
			op_writedp(dp + 0, regs.r[n].get() >> 0);
			last_cycle();
			op_writedp(dp + 1, regs.r[n].get() >> 8);
		}
	};

	public transient CPUCoreOp op_write_dpr_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int n = args.x;
			int i = args.y;
			dp = op_readpc();
			op_io_cond2();
			op_io();
			last_cycle();
			op_writedp(dp + regs.r[i].get(), regs.r[n].get());
		}
	};

	public transient CPUCoreOp op_write_dpr_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int n = args.x;
			int i = args.y;
			dp = op_readpc();
			op_io_cond2();
			op_io();
			op_writedp(dp + regs.r[i].get() + 0, regs.r[n].get() >> 0);
			last_cycle();
			op_writedp(dp + regs.r[i].get() + 1, regs.r[n].get() >> 8);
		}
	};

	public transient CPUCoreOp op_sta_idp_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			dp = op_readpc();
			op_io_cond2();
			aa.l(op_readdp(dp + 0));
			aa.h(op_readdp(dp + 1));
			last_cycle();
			op_writedbr(aa.w(), regs.a.l());
		}
	};

	public transient CPUCoreOp op_sta_idp_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			dp = op_readpc();
			op_io_cond2();
			aa.l(op_readdp(dp + 0));
			aa.h(op_readdp(dp + 1));
			op_writedbr(aa.w() + 0, regs.a.l());
			last_cycle();
			op_writedbr(aa.w() + 1, regs.a.h());
		}
	};

	public transient CPUCoreOp op_sta_ildp_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			dp = op_readpc();
			op_io_cond2();
			aa.l(op_readdp(dp + 0));
			aa.h(op_readdp(dp + 1));
			aa.b(op_readdp(dp + 2));
			last_cycle();
			op_writelong(aa.get(), regs.a.l());
		}
	};

	public transient CPUCoreOp op_sta_ildp_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			dp = op_readpc();
			op_io_cond2();
			aa.l(op_readdp(dp + 0));
			aa.h(op_readdp(dp + 1));
			aa.b(op_readdp(dp + 2));
			op_writelong(aa.get() + 0, regs.a.l());
			last_cycle();
			op_writelong(aa.get() + 1, regs.a.h());
		}
	};

	public transient CPUCoreOp op_sta_idpx_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			dp = op_readpc();
			op_io_cond2();
			op_io();
			aa.l(op_readdp(dp + regs.x.w() + 0));
			aa.h(op_readdp(dp + regs.x.w() + 1));
			last_cycle();
			op_writedbr(aa.w(), regs.a.l());
		}
	};

	public transient CPUCoreOp op_sta_idpx_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			dp = op_readpc();
			op_io_cond2();
			op_io();
			aa.l(op_readdp(dp + regs.x.w() + 0));
			aa.h(op_readdp(dp + regs.x.w() + 1));
			op_writedbr(aa.w() + 0, regs.a.l());
			last_cycle();
			op_writedbr(aa.w() + 1, regs.a.h());
		}
	};

	public transient CPUCoreOp op_sta_idpy_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			dp = op_readpc();
			op_io_cond2();
			aa.l(op_readdp(dp + 0));
			aa.h(op_readdp(dp + 1));
			op_io();
			last_cycle();
			op_writedbr(aa.w() + regs.y.w(), regs.a.l());
		}
	};

	public transient CPUCoreOp op_sta_idpy_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			dp = op_readpc();
			op_io_cond2();
			aa.l(op_readdp(dp + 0));
			aa.h(op_readdp(dp + 1));
			op_io();
			op_writedbr(aa.w() + regs.y.w() + 0, regs.a.l());
			last_cycle();
			op_writedbr(aa.w() + regs.y.w() + 1, regs.a.h());
		}
	};

	public transient CPUCoreOp op_sta_ildpy_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			dp = op_readpc();
			op_io_cond2();
			aa.l(op_readdp(dp + 0));
			aa.h(op_readdp(dp + 1));
			aa.b(op_readdp(dp + 2));
			last_cycle();
			op_writelong(aa.get() + regs.y.w(), regs.a.l());
		}
	};

	public transient CPUCoreOp op_sta_ildpy_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			dp = op_readpc();
			op_io_cond2();
			aa.l(op_readdp(dp + 0));
			aa.h(op_readdp(dp + 1));
			aa.b(op_readdp(dp + 2));
			op_writelong(aa.get() + regs.y.w() + 0, regs.a.l());
			last_cycle();
			op_writelong(aa.get() + regs.y.w() + 1, regs.a.h());
		}
	};

	public transient CPUCoreOp op_sta_sr_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			sp = op_readpc();
			op_io();
			last_cycle();
			op_writesp(sp, regs.a.l());
		}
	};

	public transient CPUCoreOp op_sta_sr_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			sp = op_readpc();
			op_io();
			op_writesp(sp + 0, regs.a.l());
			last_cycle();
			op_writesp(sp + 1, regs.a.h());
		}
	};

	public transient CPUCoreOp op_sta_isry_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			sp = op_readpc();
			op_io();
			aa.l(op_readsp(sp + 0));
			aa.h(op_readsp(sp + 1));
			op_io();
			last_cycle();
			op_writedbr(aa.w() + regs.y.w(), regs.a.l());
		}
	};

	public transient CPUCoreOp op_sta_isry_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			sp = op_readpc();
			op_io();
			aa.l(op_readsp(sp + 0));
			aa.h(op_readsp(sp + 1));
			op_io();
			op_writedbr(aa.w() + regs.y.w() + 0, regs.a.l());
			last_cycle();
			op_writedbr(aa.w() + regs.y.w() + 1, regs.a.h());
		}
	};

	public transient CPUCoreOp op_adjust_imm_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int n = args.x;
			int adjust = args.y;
			last_cycle();
			op_io_irq();
			regs.r[n].l(regs.r[n].l() + (adjust & 0xFF));
			regs.p.n = (regs.r[n].l() & 0x80) != 0;
			regs.p.z = regs.r[n].l() == 0;
		}
	};

	public transient CPUCoreOp op_adjust_imm_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int n = args.x;
			int adjust = args.y;
			last_cycle();
			op_io_irq();
			regs.r[n].w(regs.r[n].w() + (adjust & 0xFFFF));
			regs.p.n = (regs.r[n].w() & 0x8000) != 0;
			regs.p.z = regs.r[n].w() == 0;
		}
	};

	public transient CPUCoreOp op_asl_imm_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			last_cycle();
			op_io_irq();
			regs.p.c = (regs.a.l() & 0x80) != 0;
			regs.a.l(regs.a.l() << 1);
			regs.p.n = (regs.a.l() & 0x80) != 0;
			regs.p.z = regs.a.l() == 0;
		}
	};

	public transient CPUCoreOp op_asl_imm_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			last_cycle();
			op_io_irq();
			regs.p.c = (regs.a.w() & 0x8000) != 0;
			regs.a.w(regs.a.w() << 1);
			regs.p.n = (regs.a.w() & 0x8000) != 0;
			regs.p.z = regs.a.w() == 0;
		}
	};

	public transient CPUCoreOp op_lsr_imm_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			last_cycle();
			op_io_irq();
			regs.p.c = (regs.a.l() & 0x01) != 0;
			regs.a.l(regs.a.l() >> 1);
			regs.p.n = (regs.a.l() & 0x80) != 0;
			regs.p.z = regs.a.l() == 0;
		}
	};

	public transient CPUCoreOp op_lsr_imm_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			last_cycle();
			op_io_irq();
			regs.p.c = (regs.a.w() & 0x0001) != 0;
			regs.a.w(regs.a.w() >> 1);
			regs.p.n = (regs.a.w() & 0x8000) != 0;
			regs.p.z = regs.a.w() == 0;
		}
	};

	public transient CPUCoreOp op_rol_imm_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			last_cycle();
			op_io_irq();
			int carry = regs.p.c ? 1 : 0;
			regs.p.c = (regs.a.l() & 0x80) != 0;
			regs.a.l((regs.a.l() << 1) | carry);
			regs.p.n = (regs.a.l() & 0x80) != 0;
			regs.p.z = regs.a.l() == 0;
		}
	};

	public transient CPUCoreOp op_rol_imm_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			last_cycle();
			op_io_irq();
			int carry = regs.p.c ? 1 : 0;
			regs.p.c = (regs.a.w() & 0x8000) != 0;
			regs.a.w((regs.a.w() << 1) | carry);
			regs.p.n = (regs.a.w() & 0x8000) != 0;
			regs.p.z = regs.a.w() == 0;
		}
	};

	public transient CPUCoreOp op_ror_imm_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			last_cycle();
			op_io_irq();
			int carry = regs.p.c ? 1 : 0;
			regs.p.c = (regs.a.l() & 0x01) != 0;
			regs.a.l((carry << 7) | (regs.a.l() >> 1));
			regs.p.n = (regs.a.l() & 0x80) != 0;
			regs.p.z = regs.a.l() == 0;
		}
	};

	public transient CPUCoreOp op_ror_imm_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			last_cycle();
			op_io_irq();
			int carry = regs.p.c ? 1 : 0;
			regs.p.c = (regs.a.w() & 0x0001) != 0;
			regs.a.w((carry << 15) | (regs.a.w() >> 1));
			regs.p.n = (regs.a.w() & 0x8000) != 0;
			regs.p.z = regs.a.w() == 0;
		}
	};

	public transient CPUCoreOp op_adjust_addr_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			aa.l(op_readpc());
			aa.h(op_readpc());
			rd.l(op_readdbr(aa.w()));
			op_io();
			op.Invoke(null);
			last_cycle();
			op_writedbr(aa.w(), rd.l());
		}
	};

	public transient CPUCoreOp op_adjust_addr_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			aa.l(op_readpc());
			aa.h(op_readpc());
			rd.l(op_readdbr(aa.w() + 0));
			rd.h(op_readdbr(aa.w() + 1));
			op_io();
			op.Invoke(null);
			op_writedbr(aa.w() + 1, rd.h());
			last_cycle();
			op_writedbr(aa.w() + 0, rd.l());
		}
	};

	public transient CPUCoreOp op_adjust_addrx_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			aa.l(op_readpc());
			aa.h(op_readpc());
			op_io();
			rd.l(op_readdbr(aa.w() + regs.x.w()));
			op_io();
			op.Invoke(null);
			last_cycle();
			op_writedbr(aa.w() + regs.x.w(), rd.l());
		}
	};

	public transient CPUCoreOp op_adjust_addrx_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			aa.l(op_readpc());
			aa.h(op_readpc());
			op_io();
			rd.l(op_readdbr(aa.w() + regs.x.w() + 0));
			rd.h(op_readdbr(aa.w() + regs.x.w() + 1));
			op_io();
			op.Invoke(null);
			op_writedbr(aa.w() + regs.x.w() + 1, rd.h());
			last_cycle();
			op_writedbr(aa.w() + regs.x.w() + 0, rd.l());
		}
	};

	public transient CPUCoreOp op_adjust_dp_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			dp = op_readpc();
			op_io_cond2();
			rd.l(op_readdp(dp));
			op_io();
			op.Invoke(null);
			last_cycle();
			op_writedp(dp, rd.l());
		}
	};

	public transient CPUCoreOp op_adjust_dp_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			dp = op_readpc();
			op_io_cond2();
			rd.l(op_readdp(dp + 0));
			rd.h(op_readdp(dp + 1));
			op_io();
			op.Invoke(null);
			op_writedp(dp + 1, rd.h());
			last_cycle();
			op_writedp(dp + 0, rd.l());
		}
	};

	public transient CPUCoreOp op_adjust_dpx_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			dp = op_readpc();
			op_io_cond2();
			op_io();
			rd.l(op_readdp(dp + regs.x.w()));
			op_io();
			op.Invoke(null);
			last_cycle();
			op_writedp(dp + regs.x.w(), rd.l());
		}
	};

	public transient CPUCoreOp op_adjust_dpx_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			CPUCoreOp op = args.op;
			dp = op_readpc();
			op_io_cond2();
			op_io();
			rd.l(op_readdp(dp + regs.x.w() + 0));
			rd.h(op_readdp(dp + regs.x.w() + 1));
			op_io();
			op.Invoke(null);
			op_writedp(dp + regs.x.w() + 1, rd.h());
			last_cycle();
			op_writedp(dp + regs.x.w() + 0, rd.l());
		}
	};

	public transient CPUCoreOp op_branch = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int bit = args.x;
			int val = args.y;
			if ((((regs.p.get() & bit) != 0) ? 1 : 0) != val)
			{
				last_cycle();
				rd.l(op_readpc());
			}
			else
			{
				rd.l(op_readpc());
				aa.w(regs.pc.get() + (byte) rd.l());
				op_io_cond6(aa.w());
				last_cycle();
				op_io();
				regs.pc.w(aa.w());
			}
		}
	};

	public transient CPUCoreOp op_bra = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			rd.l(op_readpc());
			aa.w(regs.pc.get() + (byte) rd.l());
			op_io_cond6(aa.w());
			last_cycle();
			op_io();
			regs.pc.w(aa.w());
		}
	};

	public transient CPUCoreOp op_brl = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			rd.l(op_readpc());
			rd.h(op_readpc());
			last_cycle();
			op_io();
			regs.pc.w(regs.pc.get() + rd.w());
		}
	};

	public transient CPUCoreOp op_jmp_addr = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			rd.l(op_readpc());
			last_cycle();
			rd.h(op_readpc());
			regs.pc.w(rd.w());
		}
	};

	public transient CPUCoreOp op_jmp_long = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			rd.l(op_readpc());
			rd.h(op_readpc());
			last_cycle();
			rd.b(op_readpc());
			regs.pc.set(rd.get());
		}
	};

	public transient CPUCoreOp op_jmp_iaddr = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			aa.l(op_readpc());
			aa.h(op_readpc());
			rd.l(op_readaddr(aa.w() + 0));
			last_cycle();
			rd.h(op_readaddr(aa.w() + 1));
			regs.pc.w(rd.w());
		}
	};

	public transient CPUCoreOp op_jmp_iaddrx = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			aa.l(op_readpc());
			aa.h(op_readpc());
			op_io();
			rd.l(op_readpbr(aa.w() + regs.x.w() + 0));
			last_cycle();
			rd.h(op_readpbr(aa.w() + regs.x.w() + 1));
			regs.pc.w(rd.w());
		}
	};

	public transient CPUCoreOp op_jmp_iladdr = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			aa.l(op_readpc());
			aa.h(op_readpc());
			rd.l(op_readaddr(aa.w() + 0));
			rd.h(op_readaddr(aa.w() + 1));
			last_cycle();
			rd.b(op_readaddr(aa.w() + 2));
			regs.pc.set(rd.get());
		}
	};

	public transient CPUCoreOp op_jsr_addr = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			aa.l(op_readpc());
			aa.h(op_readpc());
			op_io();
			regs.pc.w(regs.pc.w() - 1);
			op_writestack(regs.pc.h());
			last_cycle();
			op_writestack(regs.pc.l());
			regs.pc.w(aa.w());
		}
	};

	public transient CPUCoreOp op_jsr_long_e = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			aa.l(op_readpc());
			aa.h(op_readpc());
			op_writestackn(regs.pc.b());
			op_io();
			aa.b(op_readpc());
			regs.pc.w(regs.pc.w() - 1);
			op_writestackn(regs.pc.h());
			last_cycle();
			op_writestackn(regs.pc.l());
			regs.pc.set(aa.get());
			regs.s.h(0x01);
		}
	};

	public transient CPUCoreOp op_jsr_long_n = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			aa.l(op_readpc());
			aa.h(op_readpc());
			op_writestackn(regs.pc.b());
			op_io();
			aa.b(op_readpc());
			regs.pc.w(regs.pc.w() - 1);
			op_writestackn(regs.pc.h());
			last_cycle();
			op_writestackn(regs.pc.l());
			regs.pc.set(aa.get());
		}
	};

	public transient CPUCoreOp op_jsr_iaddrx_e = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			aa.l(op_readpc());
			op_writestackn(regs.pc.h());
			op_writestackn(regs.pc.l());
			aa.h(op_readpc());
			op_io();
			rd.l(op_readpbr(aa.w() + regs.x.w() + 0));
			last_cycle();
			rd.h(op_readpbr(aa.w() + regs.x.w() + 1));
			regs.pc.w(rd.w());
			regs.s.h(0x01);
		}
	};

	public transient CPUCoreOp op_jsr_iaddrx_n = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			aa.l(op_readpc());
			op_writestackn(regs.pc.h());
			op_writestackn(regs.pc.l());
			aa.h(op_readpc());
			op_io();
			rd.l(op_readpbr(aa.w() + regs.x.w() + 0));
			last_cycle();
			rd.h(op_readpbr(aa.w() + regs.x.w() + 1));
			regs.pc.w(rd.w());
		}
	};

	public transient CPUCoreOp op_rti_e = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			op_io();
			op_io();
			regs.p.set(op_readstack() | 0x30);
			rd.l(op_readstack());
			last_cycle();
			rd.h(op_readstack());
			regs.pc.w(rd.w());
		}
	};

	public transient CPUCoreOp op_rti_n = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			op_io();
			op_io();
			regs.p.set(op_readstack());
			if (regs.p.x)
			{
				regs.x.h(0x00);
				regs.y.h(0x00);
			}
			rd.l(op_readstack());
			rd.h(op_readstack());
			last_cycle();
			rd.b(op_readstack());
			regs.pc.set(rd.get());
			update_table();
		}
	};

	public transient CPUCoreOp op_rts = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			op_io();
			op_io();
			rd.l(op_readstack());
			rd.h(op_readstack());
			last_cycle();
			op_io();
			rd.w(rd.w() + 1);
			regs.pc.w(rd.w());
		}
	};

	public transient CPUCoreOp op_rtl_e = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			op_io();
			op_io();
			rd.l(op_readstackn());
			rd.h(op_readstackn());
			last_cycle();
			rd.b(op_readstackn());
			regs.pc.b(rd.b());
			rd.w(rd.w() + 1);
			regs.pc.w(rd.w());
			regs.s.h(0x01);
		}
	};

	public transient CPUCoreOp op_rtl_n = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			op_io();
			op_io();
			rd.l(op_readstackn());
			rd.h(op_readstackn());
			last_cycle();
			rd.b(op_readstackn());
			regs.pc.b(rd.b());
			rd.w(rd.w() + 1);
			regs.pc.w(rd.w());
		}
	};

	public transient CPUCoreOp op_nop = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			last_cycle();
			op_io_irq();
		}
	};

	public transient CPUCoreOp op_wdm = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			last_cycle();
			op_readpc();
		}
	};

	public transient CPUCoreOp op_xba = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			op_io();
			last_cycle();
			op_io();
			regs.a.l(regs.a.l() ^ regs.a.h());
			regs.a.h(regs.a.h() ^ regs.a.l());
			regs.a.l(regs.a.l() ^ regs.a.h());
			regs.p.n = (regs.a.l() & 0x80) != 0;
			regs.p.z = regs.a.l() == 0;
		}
	};

	public transient CPUCoreOp op_move_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int adjust = args.x;
			dp = op_readpc();
			sp = op_readpc();
			regs.db = (byte) dp;
			rd.l(op_readlong((sp << 16) | regs.x.w()));
			op_writelong((dp << 16) | regs.y.w(), rd.l());
			op_io();
			regs.x.l(regs.x.l() + (adjust & 0xFF));
			regs.y.l(regs.y.l() + (adjust & 0xFF));
			last_cycle();
			op_io();
			if (regs.a.w() != 0)
			{
				regs.pc.w(regs.pc.w() - 3);
			}
			regs.a.w(regs.a.w() - 1);
		}
	};

	public transient CPUCoreOp op_move_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int adjust = args.x;
			dp = op_readpc();
			sp = op_readpc();
			regs.db = (byte) dp;
			rd.l(op_readlong((sp << 16) | regs.x.w()));
			op_writelong((dp << 16) | regs.y.w(), rd.l());
			op_io();
			regs.x.w(regs.x.w() + (short) adjust);
			regs.y.w(regs.y.w() + (short) adjust);
			last_cycle();
			op_io();
			if (regs.a.w() != 0)
			{
				regs.pc.w(regs.pc.w() - 3);
			}
			regs.a.w(regs.a.w() - 1);
		}
	};

	public transient CPUCoreOp op_interrupt_e = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int vectorE = args.x;
			op_readpc();
			op_writestack(regs.pc.h());
			op_writestack(regs.pc.l());
			op_writestack(regs.p.get());
			rd.l(op_readlong(vectorE + 0));
			regs.pc.b(0);
			regs.p.i = true;
			regs.p.d = false;
			last_cycle();
			rd.h(op_readlong(vectorE + 1));
			regs.pc.w(rd.w());
		}
	};

	public transient CPUCoreOp op_interrupt_n = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int vectorN = args.y;
			op_readpc();
			op_writestack(regs.pc.b());
			op_writestack(regs.pc.h());
			op_writestack(regs.pc.l());
			op_writestack(regs.p.get());
			rd.l(op_readlong(vectorN + 0));
			regs.pc.b(0x00);
			regs.p.i = true;
			regs.p.d = false;
			last_cycle();
			rd.h(op_readlong(vectorN + 1));
			regs.pc.w(rd.w());
		}
	};

	public transient CPUCoreOp op_stp = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			// System.out.println("wai true");
			regs.wai = true;
			while (regs.wai)
			{
				last_cycle();
				op_io();
			}
		}
	};

	public transient CPUCoreOp op_wai = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			// System.out.println("wai true start");
			regs.wai = true;
			while (regs.wai)
			{
				// System.out.println("wai true while");
				last_cycle();
				op_io();
			}
			// System.out.println("wai true end");
			op_io();
		}
	};

	public transient CPUCoreOp op_xce = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			last_cycle();
			op_io_irq();
			boolean carry = regs.p.c;
			regs.p.c = regs.e;
			regs.e = carry;
			if (regs.e)
			{
				regs.p.set(regs.p.get() | 0x30);
				regs.s.h(0x01);
			}
			if (regs.p.x)
			{
				regs.x.h(0x00);
				regs.y.h(0x00);
			}
			update_table();
		}
	};

	public transient CPUCoreOp op_flag = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int mask = args.x;
			int value = args.y;
			last_cycle();
			op_io_irq();
			regs.p.set((regs.p.get() & ~mask) | value);
		}
	};

	public transient CPUCoreOp op_pflag_e = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			boolean mode = args.x != 0;
			rd.l(op_readpc());
			last_cycle();
			op_io();
			regs.p.set(mode ? regs.p.get() | rd.l() : regs.p.get() & ~rd.l());
			regs.p.set(regs.p.get() | 0x30);
			if (regs.p.x)
			{
				regs.x.h(0x00);
				regs.y.h(0x00);
			}
			update_table();
		}
	};

	public transient CPUCoreOp op_pflag_n = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			boolean mode = args.x != 0;
			rd.l(op_readpc());
			last_cycle();
			op_io();
			regs.p.set(mode ? regs.p.get() | rd.l() : regs.p.get() & ~rd.l());
			if (regs.p.x)
			{
				regs.x.h(0x00);
				regs.y.h(0x00);
			}
			update_table();
		}
	};

	public transient CPUCoreOp op_transfer_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int from = args.x;
			int to = args.y;
			last_cycle();
			op_io_irq();
			regs.r[to].l(regs.r[from].l());
			regs.p.n = (regs.r[to].l() & 0x80) != 0;
			regs.p.z = regs.r[to].l() == 0;
		}
	};

	public transient CPUCoreOp op_transfer_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int from = args.x;
			int to = args.y;
			last_cycle();
			op_io_irq();
			regs.r[to].w(regs.r[from].w());
			regs.p.n = (regs.r[to].w() & 0x8000) != 0;
			regs.p.z = regs.r[to].w() == 0;
		}
	};

	public transient CPUCoreOp op_tcs_e = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			last_cycle();
			op_io_irq();
			regs.s.l(regs.a.l());
		}
	};

	public transient CPUCoreOp op_tcs_n = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			last_cycle();
			op_io_irq();
			regs.s.w(regs.a.w());
		}
	};

	public transient CPUCoreOp op_tsx_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			last_cycle();
			op_io_irq();
			regs.x.l(regs.s.l());
			regs.p.n = (regs.x.l() & 0x80) != 0;
			regs.p.z = regs.x.l() == 0;
		}
	};

	public transient CPUCoreOp op_tsx_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			last_cycle();
			op_io_irq();
			regs.x.w(regs.s.w());
			regs.p.n = (regs.x.w() & 0x8000) != 0;
			regs.p.z = regs.x.w() == 0;
		}
	};

	public transient CPUCoreOp op_txs_e = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			last_cycle();
			op_io_irq();
			regs.s.l(regs.x.l());
		}
	};

	public transient CPUCoreOp op_txs_n = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			last_cycle();
			op_io_irq();
			regs.s.w(regs.x.w());
		}
	};

	public transient CPUCoreOp op_push_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int n = args.x;
			op_io();
			last_cycle();
			op_writestack(regs.r[n].l());
		}
	};

	public transient CPUCoreOp op_push_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int n = args.x;
			op_io();
			op_writestack(regs.r[n].h());
			last_cycle();
			op_writestack(regs.r[n].l());
		}
	};

	public transient CPUCoreOp op_phd_e = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			op_io();
			op_writestackn(regs.d.h());
			last_cycle();
			op_writestackn(regs.d.l());
			regs.s.h(0x01);
		}
	};

	public transient CPUCoreOp op_phd_n = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			op_io();
			op_writestackn(regs.d.h());
			last_cycle();
			op_writestackn(regs.d.l());
		}
	};

	public transient CPUCoreOp op_phb = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			op_io();
			last_cycle();
			op_writestack(regs.db);
		}
	};

	public transient CPUCoreOp op_phk = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			op_io();
			last_cycle();
			op_writestack(regs.pc.b());
		}
	};

	public transient CPUCoreOp op_php = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			op_io();
			last_cycle();
			op_writestack(regs.p.get());
		}
	};

	public transient CPUCoreOp op_pull_b = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int n = args.x;
			op_io();
			op_io();
			last_cycle();
			regs.r[n].l(op_readstack());
			regs.p.n = (regs.r[n].l() & 0x80) != 0;
			regs.p.z = regs.r[n].l() == 0;
		}
	};

	public transient CPUCoreOp op_pull_w = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			int n = args.x;
			op_io();
			op_io();
			regs.r[n].l(op_readstack());
			last_cycle();
			regs.r[n].h(op_readstack());
			regs.p.n = (regs.r[n].w() & 0x8000) != 0;
			regs.p.z = regs.r[n].w() == 0;
		}
	};

	public transient CPUCoreOp op_pld_e = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			op_io();
			op_io();
			regs.d.l(op_readstackn());
			last_cycle();
			regs.d.h(op_readstackn());
			regs.p.n = (regs.d.w() & 0x8000) != 0;
			regs.p.z = regs.d.w() == 0;
			regs.s.h(0x01);
		}
	};

	public transient CPUCoreOp op_pld_n = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			op_io();
			op_io();
			regs.d.l(op_readstackn());
			last_cycle();
			regs.d.h(op_readstackn());
			regs.p.n = (regs.d.w() & 0x8000) != 0;
			regs.p.z = regs.d.w() == 0;
		}
	};

	public transient CPUCoreOp op_plb = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			op_io();
			op_io();
			last_cycle();
			regs.db = (byte) op_readstack();
			regs.p.n = (regs.db & 0x80) != 0;
			regs.p.z = regs.db == 0;
		}
	};

	public transient CPUCoreOp op_plp_e = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			op_io();
			op_io();
			last_cycle();
			regs.p.set(op_readstack() | 0x30);
			if (regs.p.x)
			{
				regs.x.h(0x00);
				regs.y.h(0x00);
			}
			update_table();
		}
	};

	public transient CPUCoreOp op_plp_n = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			op_io();
			op_io();
			last_cycle();
			regs.p.set(op_readstack());
			if (regs.p.x)
			{
				regs.x.h(0x00);
				regs.y.h(0x00);
			}
			update_table();
		}
	};

	public transient CPUCoreOp op_pea_e = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			aa.l(op_readpc());
			aa.h(op_readpc());
			op_writestackn(aa.h());
			last_cycle();
			op_writestackn(aa.l());
			regs.s.h(0x01);
		}
	};

	public transient CPUCoreOp op_pea_n = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			aa.l(op_readpc());
			aa.h(op_readpc());
			op_writestackn(aa.h());
			last_cycle();
			op_writestackn(aa.l());
		}
	};

	public transient CPUCoreOp op_pei_e = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			dp = op_readpc();
			op_io_cond2();
			aa.l(op_readdp(dp + 0));
			aa.h(op_readdp(dp + 1));
			op_writestackn(aa.h());
			last_cycle();
			op_writestackn(aa.l());
			regs.s.h(0x01);
		}
	};

	public transient CPUCoreOp op_pei_n = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			dp = op_readpc();
			op_io_cond2();
			aa.l(op_readdp(dp + 0));
			aa.h(op_readdp(dp + 1));
			op_writestackn(aa.h());
			last_cycle();
			op_writestackn(aa.l());
		}
	};

	public transient CPUCoreOp op_per_e = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			aa.l(op_readpc());
			aa.h(op_readpc());
			op_io();
			rd.w(regs.pc.get() + aa.w());
			op_writestackn(rd.h());
			last_cycle();
			op_writestackn(rd.l());
			regs.s.h(0x01);
		}
	};

	public transient CPUCoreOp op_per_n = new CPUCoreOp()
	{
		public void Invoke(CPUCoreOpArgument args)
		{
			aa.l(op_readpc());
			aa.h(op_readpc());
			op_io();
			rd.w(regs.pc.get() + aa.w());
			op_writestackn(rd.h());
			last_cycle();
			op_writestackn(rd.l());
		}
	};

	protected ArrayList<CPUCoreOperation> opcode_table;
	public CPUCoreOperation[] op_table = new CPUCoreOperation[256 * 5];

	public CPUCoreOp GetCoreOp(String name)
	{
		return GetCoreOp(name, "");
	}

	public CPUCoreOp GetCoreOp(String name, String modifier)
	{
		try
		{
			return (CPUCoreOp) CPUCore.class.getDeclaredField("op_" + name + modifier).get(this);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	private void opA(int id, String name)
	{
		op_table[Table_EM + id] = op_table[Table_MX + id] = op_table[Table_Mx + id] = op_table[Table_mX + id] = op_table[Table_mx + id] = new CPUCoreOperation(GetCoreOp(name), null);
	}

	private void opAII(int id, String name, int x, int y)
	{
		op_table[Table_EM + id] = op_table[Table_MX + id] = op_table[Table_Mx + id] = op_table[Table_mX + id] = op_table[Table_mx + id] = new CPUCoreOperation(GetCoreOp(name), new CPUCoreOpArgument(x, y));
	}

	private void opE(int id, String name)
	{
		op_table[Table_EM + id] = new CPUCoreOperation(GetCoreOp(name, "_e"), null);
		op_table[Table_MX + id] = op_table[Table_Mx + id] = op_table[Table_mX + id] = op_table[Table_mx + id] = new CPUCoreOperation(GetCoreOp(name, "_n"), null);
	}

	private void opEI(int id, String name, int x)
	{
		op_table[Table_EM + id] = new CPUCoreOperation(GetCoreOp(name, "_e"), new CPUCoreOpArgument(x));
		op_table[Table_MX + id] = op_table[Table_Mx + id] = op_table[Table_mX + id] = op_table[Table_mx + id] = new CPUCoreOperation(GetCoreOp(name, "_n"), new CPUCoreOpArgument(x));
	}

	private void opEII(int id, String name, int x, int y)
	{
		op_table[Table_EM + id] = new CPUCoreOperation(GetCoreOp(name, "_e"), new CPUCoreOpArgument(x, y));
		op_table[Table_MX + id] = op_table[Table_Mx + id] = op_table[Table_mX + id] = op_table[Table_mx + id] = new CPUCoreOperation(GetCoreOp(name, "_n"), new CPUCoreOpArgument(x, y));
	}

	private void opM(int id, String name)
	{
		op_table[Table_EM + id] = op_table[Table_MX + id] = op_table[Table_Mx + id] = new CPUCoreOperation(GetCoreOp(name, "_b"), null);
		op_table[Table_mX + id] = op_table[Table_mx + id] = new CPUCoreOperation(GetCoreOp(name, "_w"), null);
	}

	private void opMI(int id, String name, int x)
	{
		op_table[Table_EM + id] = op_table[Table_MX + id] = op_table[Table_Mx + id] = new CPUCoreOperation(GetCoreOp(name, "_b"), new CPUCoreOpArgument(x));
		op_table[Table_mX + id] = op_table[Table_mx + id] = new CPUCoreOperation(GetCoreOp(name, "_w"), new CPUCoreOpArgument(x));
	}

	private void opMII(int id, String name, int x, int y)
	{
		op_table[Table_EM + id] = op_table[Table_MX + id] = op_table[Table_Mx + id] = new CPUCoreOperation(GetCoreOp(name, "_b"), new CPUCoreOpArgument(x, y));
		op_table[Table_mX + id] = op_table[Table_mx + id] = new CPUCoreOperation(GetCoreOp(name, "_w"), new CPUCoreOpArgument(x, y));
	}

	private void opMF(int id, String name, String fn)
	{
		op_table[Table_EM + id] = op_table[Table_MX + id] = op_table[Table_Mx + id] = new CPUCoreOperation(GetCoreOp(name, "_b"), new CPUCoreOpArgument(GetCoreOp(fn, "_b")));
		op_table[Table_mX + id] = op_table[Table_mx + id] = new CPUCoreOperation(GetCoreOp(name, "_w"), new CPUCoreOpArgument(GetCoreOp(fn, "_w")));
	}

	private void opMFI(int id, String name, String fn, int x)
	{
		op_table[Table_EM + id] = op_table[Table_MX + id] = op_table[Table_Mx + id] = new CPUCoreOperation(GetCoreOp(name, "_b"), new CPUCoreOpArgument(GetCoreOp(fn, "_b"), x));
		op_table[Table_mX + id] = op_table[Table_mx + id] = new CPUCoreOperation(GetCoreOp(name, "_w"), new CPUCoreOpArgument(GetCoreOp(fn, "_w"), x));
	}

	private void opX(int id, String name)
	{
		op_table[Table_EM + id] = op_table[Table_MX + id] = op_table[Table_mX + id] = new CPUCoreOperation(GetCoreOp(name, "_b"), null);
		op_table[Table_Mx + id] = op_table[Table_mx + id] = new CPUCoreOperation(GetCoreOp(name, "_w"), null);
	}

	private void opXI(int id, String name, int x)
	{
		op_table[Table_EM + id] = op_table[Table_MX + id] = op_table[Table_mX + id] = new CPUCoreOperation(GetCoreOp(name, "_b"), new CPUCoreOpArgument(x));
		op_table[Table_Mx + id] = op_table[Table_mx + id] = new CPUCoreOperation(GetCoreOp(name, "_w"), new CPUCoreOpArgument(x));
	}

	private void opXII(int id, String name, int x, int y)
	{
		op_table[Table_EM + id] = op_table[Table_MX + id] = op_table[Table_mX + id] = new CPUCoreOperation(GetCoreOp(name, "_b"), new CPUCoreOpArgument(x, y));
		op_table[Table_Mx + id] = op_table[Table_mx + id] = new CPUCoreOperation(GetCoreOp(name, "_w"), new CPUCoreOpArgument(x, y));
	}

	private void opXF(int id, String name, String fn)
	{
		op_table[Table_EM + id] = op_table[Table_MX + id] = op_table[Table_mX + id] = new CPUCoreOperation(GetCoreOp(name, "_b"), new CPUCoreOpArgument(GetCoreOp(fn, "_b")));
		op_table[Table_Mx + id] = op_table[Table_mx + id] = new CPUCoreOperation(GetCoreOp(name, "_w"), new CPUCoreOpArgument(GetCoreOp(fn, "_w")));
	}

	private void opXFI(int id, String name, String fn, int x)
	{
		op_table[Table_EM + id] = op_table[Table_MX + id] = op_table[Table_mX + id] = new CPUCoreOperation(GetCoreOp(name, "_b"), new CPUCoreOpArgument(GetCoreOp(fn, "_b"), x));
		op_table[Table_Mx + id] = op_table[Table_mx + id] = new CPUCoreOperation(GetCoreOp(name, "_w"), new CPUCoreOpArgument(GetCoreOp(fn, "_w"), x));
	}

	protected void initialize_opcode_table()
	{
		opEII(0x00, "interrupt", 0xfffe, 0xffe6);
		opMF(0x01, "read_idpx", "ora");
		opEII(0x02, "interrupt", 0xfff4, 0xffe4);
		opMF(0x03, "read_sr", "ora");
		opMF(0x04, "adjust_dp", "tsb");
		opMF(0x05, "read_dp", "ora");
		opMF(0x06, "adjust_dp", "asl");
		opMF(0x07, "read_ildp", "ora");
		opA(0x08, "php");
		opMF(0x09, "read_const", "ora");
		opM(0x0a, "asl_imm");
		opE(0x0b, "phd");
		opMF(0x0c, "adjust_addr", "tsb");
		opMF(0x0d, "read_addr", "ora");
		opMF(0x0e, "adjust_addr", "asl");
		opMF(0x0f, "read_long", "ora");
		opAII(0x10, "branch", 0x80, 0);
		opMF(0x11, "read_idpy", "ora");
		opMF(0x12, "read_idp", "ora");
		opMF(0x13, "read_isry", "ora");
		opMF(0x14, "adjust_dp", "trb");
		opMFI(0x15, "read_dpr", "ora", OpCode_X);
		opMF(0x16, "adjust_dpx", "asl");
		opMF(0x17, "read_ildpy", "ora");
		opAII(0x18, "flag", 0x01, 0x00);
		opMF(0x19, "read_addry", "ora");
		opMII(0x1a, "adjust_imm", OpCode_A, +1);
		opE(0x1b, "tcs");
		opMF(0x1c, "adjust_addr", "trb");
		opMF(0x1d, "read_addrx", "ora");
		opMF(0x1e, "adjust_addrx", "asl");
		opMF(0x1f, "read_longx", "ora");
		opA(0x20, "jsr_addr");
		opMF(0x21, "read_idpx", "and");
		opE(0x22, "jsr_long");
		opMF(0x23, "read_sr", "and");
		opMF(0x24, "read_dp", "bit");
		opMF(0x25, "read_dp", "and");
		opMF(0x26, "adjust_dp", "rol");
		opMF(0x27, "read_ildp", "and");
		opE(0x28, "plp");
		opMF(0x29, "read_const", "and");
		opM(0x2a, "rol_imm");
		opE(0x2b, "pld");
		opMF(0x2c, "read_addr", "bit");
		opMF(0x2d, "read_addr", "and");
		opMF(0x2e, "adjust_addr", "rol");
		opMF(0x2f, "read_long", "and");
		opAII(0x30, "branch", 0x80, 1);
		opMF(0x31, "read_idpy", "and");
		opMF(0x32, "read_idp", "and");
		opMF(0x33, "read_isry", "and");
		opMFI(0x34, "read_dpr", "bit", OpCode_X);
		opMFI(0x35, "read_dpr", "and", OpCode_X);
		opMF(0x36, "adjust_dpx", "rol");
		opMF(0x37, "read_ildpy", "and");
		opAII(0x38, "flag", 0x01, 0x01);
		opMF(0x39, "read_addry", "and");
		opMII(0x3a, "adjust_imm", OpCode_A, -1);
		opAII(0x3b, "transfer_w", OpCode_S, OpCode_A);
		opMF(0x3c, "read_addrx", "bit");
		opMF(0x3d, "read_addrx", "and");
		opMF(0x3e, "adjust_addrx", "rol");
		opMF(0x3f, "read_longx", "and");
		opE(0x40, "rti");
		opMF(0x41, "read_idpx", "eor");
		opA(0x42, "wdm");
		opMF(0x43, "read_sr", "eor");
		opXI(0x44, "move", -1);
		opMF(0x45, "read_dp", "eor");
		opMF(0x46, "adjust_dp", "lsr");
		opMF(0x47, "read_ildp", "eor");
		opMI(0x48, "push", OpCode_A);
		opMF(0x49, "read_const", "eor");
		opM(0x4a, "lsr_imm");
		opA(0x4b, "phk");
		opA(0x4c, "jmp_addr");
		opMF(0x4d, "read_addr", "eor");
		opMF(0x4e, "adjust_addr", "lsr");
		opMF(0x4f, "read_long", "eor");
		opAII(0x50, "branch", 0x40, 0);
		opMF(0x51, "read_idpy", "eor");
		opMF(0x52, "read_idp", "eor");
		opMF(0x53, "read_isry", "eor");
		opXI(0x54, "move", +1);
		opMFI(0x55, "read_dpr", "eor", OpCode_X);
		opMF(0x56, "adjust_dpx", "lsr");
		opMF(0x57, "read_ildpy", "eor");
		opAII(0x58, "flag", 0x04, 0x00);
		opMF(0x59, "read_addry", "eor");
		opXI(0x5a, "push", OpCode_Y);
		opAII(0x5b, "transfer_w", OpCode_A, OpCode_D);
		opA(0x5c, "jmp_long");
		opMF(0x5d, "read_addrx", "eor");
		opMF(0x5e, "adjust_addrx", "lsr");
		opMF(0x5f, "read_longx", "eor");
		opA(0x60, "rts");
		opMF(0x61, "read_idpx", "adc");
		opE(0x62, "per");
		opMF(0x63, "read_sr", "adc");
		opMI(0x64, "write_dp", OpCode_Z);
		opMF(0x65, "read_dp", "adc");
		opMF(0x66, "adjust_dp", "ror");
		opMF(0x67, "read_ildp", "adc");
		opMI(0x68, "pull", OpCode_A);
		opMF(0x69, "read_const", "adc");
		opM(0x6a, "ror_imm");
		opE(0x6b, "rtl");
		opA(0x6c, "jmp_iaddr");
		opMF(0x6d, "read_addr", "adc");
		opMF(0x6e, "adjust_addr", "ror");
		opMF(0x6f, "read_long", "adc");
		opAII(0x70, "branch", 0x40, 1);
		opMF(0x71, "read_idpy", "adc");
		opMF(0x72, "read_idp", "adc");
		opMF(0x73, "read_isry", "adc");
		opMII(0x74, "write_dpr", OpCode_Z, OpCode_X);
		opMFI(0x75, "read_dpr", "adc", OpCode_X);
		opMF(0x76, "adjust_dpx", "ror");
		opMF(0x77, "read_ildpy", "adc");
		opAII(0x78, "flag", 0x04, 0x04);
		opMF(0x79, "read_addry", "adc");
		opXI(0x7a, "pull", OpCode_Y);
		opAII(0x7b, "transfer_w", OpCode_D, OpCode_A);
		opA(0x7c, "jmp_iaddrx");
		opMF(0x7d, "read_addrx", "adc");
		opMF(0x7e, "adjust_addrx", "ror");
		opMF(0x7f, "read_longx", "adc");
		opA(0x80, "bra");
		opM(0x81, "sta_idpx");
		opA(0x82, "brl");
		opM(0x83, "sta_sr");
		opXI(0x84, "write_dp", OpCode_Y);
		opMI(0x85, "write_dp", OpCode_A);
		opXI(0x86, "write_dp", OpCode_X);
		opM(0x87, "sta_ildp");
		opXII(0x88, "adjust_imm", OpCode_Y, -1);
		opM(0x89, "read_bit_const");
		opMII(0x8a, "transfer", OpCode_X, OpCode_A);
		opA(0x8b, "phb");
		opXI(0x8c, "write_addr", OpCode_Y);
		opMI(0x8d, "write_addr", OpCode_A);
		opXI(0x8e, "write_addr", OpCode_X);
		opMI(0x8f, "write_longr", OpCode_Z);
		opAII(0x90, "branch", 0x01, 0);
		opM(0x91, "sta_idpy");
		opM(0x92, "sta_idp");
		opM(0x93, "sta_isry");
		opXII(0x94, "write_dpr", OpCode_Y, OpCode_X);
		opMII(0x95, "write_dpr", OpCode_A, OpCode_X);
		opXII(0x96, "write_dpr", OpCode_X, OpCode_Y);
		opM(0x97, "sta_ildpy");
		opMII(0x98, "transfer", OpCode_Y, OpCode_A);
		opMII(0x99, "write_addrr", OpCode_A, OpCode_Y);
		opE(0x9a, "txs");
		opXII(0x9b, "transfer", OpCode_X, OpCode_Y);
		opMI(0x9c, "write_addr", OpCode_Z);
		opMII(0x9d, "write_addrr", OpCode_A, OpCode_X);
		opMII(0x9e, "write_addrr", OpCode_Z, OpCode_X);
		opMI(0x9f, "write_longr", OpCode_X);
		opXF(0xa0, "read_const", "ldy");
		opMF(0xa1, "read_idpx", "lda");
		opXF(0xa2, "read_const", "ldx");
		opMF(0xa3, "read_sr", "lda");
		opXF(0xa4, "read_dp", "ldy");
		opMF(0xa5, "read_dp", "lda");
		opXF(0xa6, "read_dp", "ldx");
		opMF(0xa7, "read_ildp", "lda");
		opXII(0xa8, "transfer", OpCode_A, OpCode_Y);
		opMF(0xa9, "read_const", "lda");
		opXII(0xaa, "transfer", OpCode_A, OpCode_X);
		opA(0xab, "plb");
		opXF(0xac, "read_addr", "ldy");
		opMF(0xad, "read_addr", "lda");
		opXF(0xae, "read_addr", "ldx");
		opMF(0xaf, "read_long", "lda");
		opAII(0xb0, "branch", 0x01, 1);
		opMF(0xb1, "read_idpy", "lda");
		opMF(0xb2, "read_idp", "lda");
		opMF(0xb3, "read_isry", "lda");
		opXFI(0xb4, "read_dpr", "ldy", OpCode_X);
		opMFI(0xb5, "read_dpr", "lda", OpCode_X);
		opXFI(0xb6, "read_dpr", "ldx", OpCode_Y);
		opMF(0xb7, "read_ildpy", "lda");
		opAII(0xb8, "flag", 0x40, 0x00);
		opMF(0xb9, "read_addry", "lda");
		opX(0xba, "tsx");
		opXII(0xbb, "transfer", OpCode_Y, OpCode_X);
		opXF(0xbc, "read_addrx", "ldy");
		opMF(0xbd, "read_addrx", "lda");
		opXF(0xbe, "read_addry", "ldx");
		opMF(0xbf, "read_longx", "lda");
		opXF(0xc0, "read_const", "cpy");
		opMF(0xc1, "read_idpx", "cmp");
		opEI(0xc2, "pflag", 0);
		opMF(0xc3, "read_sr", "cmp");
		opXF(0xc4, "read_dp", "cpy");
		opMF(0xc5, "read_dp", "cmp");
		opMF(0xc6, "adjust_dp", "dec");
		opMF(0xc7, "read_ildp", "cmp");
		opXII(0xc8, "adjust_imm", OpCode_Y, +1);
		opMF(0xc9, "read_const", "cmp");
		opXII(0xca, "adjust_imm", OpCode_X, -1);
		opA(0xcb, "wai");
		opXF(0xcc, "read_addr", "cpy");
		opMF(0xcd, "read_addr", "cmp");
		opMF(0xce, "adjust_addr", "dec");
		opMF(0xcf, "read_long", "cmp");
		opAII(0xd0, "branch", 0x02, 0);
		opMF(0xd1, "read_idpy", "cmp");
		opMF(0xd2, "read_idp", "cmp");
		opMF(0xd3, "read_isry", "cmp");
		opE(0xd4, "pei");
		opMFI(0xd5, "read_dpr", "cmp", OpCode_X);
		opMF(0xd6, "adjust_dpx", "dec");
		opMF(0xd7, "read_ildpy", "cmp");
		opAII(0xd8, "flag", 0x08, 0x00);
		opMF(0xd9, "read_addry", "cmp");
		opXI(0xda, "push", OpCode_X);
		opA(0xdb, "stp");
		opA(0xdc, "jmp_iladdr");
		opMF(0xdd, "read_addrx", "cmp");
		opMF(0xde, "adjust_addrx", "dec");
		opMF(0xdf, "read_longx", "cmp");
		opXF(0xe0, "read_const", "cpx");
		opMF(0xe1, "read_idpx", "sbc");
		opEI(0xe2, "pflag", 1);
		opMF(0xe3, "read_sr", "sbc");
		opXF(0xe4, "read_dp", "cpx");
		opMF(0xe5, "read_dp", "sbc");
		opMF(0xe6, "adjust_dp", "inc");
		opMF(0xe7, "read_ildp", "sbc");
		opXII(0xe8, "adjust_imm", OpCode_X, +1);
		opMF(0xe9, "read_const", "sbc");
		opA(0xea, "nop");
		opA(0xeb, "xba");
		opXF(0xec, "read_addr", "cpx");
		opMF(0xed, "read_addr", "sbc");
		opMF(0xee, "adjust_addr", "inc");
		opMF(0xef, "read_long", "sbc");
		opAII(0xf0, "branch", 0x02, 1);
		opMF(0xf1, "read_idpy", "sbc");
		opMF(0xf2, "read_idp", "sbc");
		opMF(0xf3, "read_isry", "sbc");
		opE(0xf4, "pea");
		opMFI(0xf5, "read_dpr", "sbc", OpCode_X);
		opMF(0xf6, "adjust_dpx", "inc");
		opMF(0xf7, "read_ildpy", "sbc");
		opAII(0xf8, "flag", 0x08, 0x08);
		opMF(0xf9, "read_addry", "sbc");
		opXI(0xfa, "pull", OpCode_X);
		opA(0xfb, "xce");
		opE(0xfc, "jsr_iaddrx");
		opMF(0xfd, "read_addrx", "sbc");
		opMF(0xfe, "adjust_addrx", "inc");
		opMF(0xff, "read_longx", "sbc");
	}

	protected void update_table()
	{
		if (regs.e)
		{
			opcode_table = new ArrayList<CPUCoreOperation>(Arrays.asList(op_table).subList(Table_EM, Table_EM + (op_table.length - Table_EM)));
		}
		else if (regs.p.m)
		{
			if (regs.p.x)
			{
				opcode_table = new ArrayList<CPUCoreOperation>(Arrays.asList(op_table).subList(Table_MX, Table_MX + (op_table.length - Table_MX)));
			}
			else
			{
				opcode_table = new ArrayList<CPUCoreOperation>(Arrays.asList(op_table).subList(Table_Mx, Table_Mx + (op_table.length - Table_Mx)));
			}
		}
		else
		{
			if (regs.p.x)
			{
				opcode_table = new ArrayList<CPUCoreOperation>(Arrays.asList(op_table).subList(Table_mX, Table_mX + (op_table.length - Table_mX)));
			}
			else
			{
				opcode_table = new ArrayList<CPUCoreOperation>(Arrays.asList(op_table).subList(Table_mx, Table_mx + (op_table.length - Table_mx)));
			}
		}
	}

	public static final int Table_EM = 0;
	public static final int Table_MX = 256;
	public static final int Table_Mx = 512;
	public static final int Table_mX = 768;
	public static final int Table_mx = 1024;

	private static final int OpCode_A = 0;
	private static final int OpCode_X = 1;
	private static final int OpCode_Y = 2;
	private static final int OpCode_Z = 3;
	private static final int OpCode_S = 4;
	private static final int OpCode_D = 5;

	public CPUCore()
	{
		initialize_opcode_table();
	}

	@Override
	public void op_io() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public byte op_read(int addr) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void op_write(int addr, byte data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void last_cycle() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean interrupt_pending() {
		// TODO Auto-generated method stub
		return false;
	}
}
