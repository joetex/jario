/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.smp;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import jario.hardware.Bus8bit;
import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

public class SMP extends SMPCore implements Hardware, Clockable, Bus8bit, Configurable, java.io.Serializable
{
	private static final int SMP_FREQUENCY_NTSC_PAL = 24607104; // 32040.5 * 768
	private static final int CPU_FREQUENCY_NTSC = 21477272; // 315 / 88 * 6000000
	private static final int CPU_FREQUENCY_PAL = 21281370;

	private long clock;
	private int frequency_multiplier;

	protected Bus8bit apuram;
	protected Clockable dsp_clk;
	protected Bus8bit dsp_bus;
	protected Bus8bit cpu_bus;
	
	private boolean enableaudio = true;
	
	public SMP()
	{
		t0 = new SMPTimer(192, this);
		t1 = new SMPTimer(192, this);
		t2 = new SMPTimer(24, this);
		power();
	}

	@Override
	public void connect(int port, Hardware hw)
	{
		switch (port)
		{
		case 0:
			dsp_bus = (Bus8bit) hw;
			dsp_clk = (Clockable) hw;
			apuram = (Bus8bit) ((Configurable) hw).readConfig("SRAM");
			break;
		case 1:
			cpu_bus = (Bus8bit) ((Configurable) hw).readConfig("BUS B");
			break;
		}
	}

	@Override
	public byte read8bit(int port)
	{
		return apuram.read8bit(0xf4 + (port & 0x3));
	}

	@Override
	public void write8bit(int port, byte data)
	{
		apuram.write8bit(0xf4 + (port & 0x3), data);
	}

	@Override
	public void clock(long clocks)
	{
		clock -= clocks * SMP_FREQUENCY_NTSC_PAL;
		while (clock < 0)
		{
			opcode_table[op_readpc()].Invoke();
		}
	}

	@Override
	public void reset()
	{
		clock = 0;

		regs.pc = 0xffc0;
		regs.r[regs.a] = 0x00;
		regs.r[regs.x] = 0x00;
		regs.r[regs.y] = 0x00;
		regs.r[regs.sp] = 0xef;
		regs.p.set(0x02);

		if (apuram != null)
		{
			for (int i = 0; i < 64 * 1024; i++)
			{
				apuram.write8bit(i, (byte) 0);
			}
		}

		status.clock_counter = 0;
		status.dsp_counter = 0;
		status.timer_step = 3;

		// $00f0
		status.clock_speed = 0;
		status.timer_speed = 0;
		status.timers_enabled = true;
		status.ram_disabled = false;
		status.ram_writable = true;
		status.timers_disabled = false;

		// $00f1
		status.iplrom_enabled = true;

		// $00f2
		status.dsp_addr = 0x00;

		// $00f8,$00f9
		status.ram0 = 0x00;
		status.ram1 = 0x00;

		t0.stage0_ticks = 0;
		t1.stage0_ticks = 0;
		t2.stage0_ticks = 0;

		t0.stage1_ticks = 0;
		t1.stage1_ticks = 0;
		t2.stage1_ticks = 0;

		t0.stage2_ticks = 0;
		t1.stage2_ticks = 0;
		t2.stage2_ticks = 0;

		t0.stage3_ticks = 0;
		t1.stage3_ticks = 0;
		t2.stage3_ticks = 0;

		t0.current_line = false;
		t1.current_line = false;
		t2.current_line = false;

		t0.enabled = false;
		t1.enabled = false;
		t2.enabled = false;
	}

	private void power()
	{ // targets not initialized/changed upon reset
		t0.target = 0;
		t1.target = 0;
		t2.target = 0;

		reset();
	}

	private int ram_read(int addr)
	{
		if ((addr & 0xFFFF) >= 0xffc0 && status.iplrom_enabled) { return iplrom[addr & 0x3f]; }
		if (status.ram_disabled) { return 0x5a; // 0xff on mini-SNES
		}
		return apuram.read8bit(addr & 0xFFFF) & 0xFF;
	}

	private void ram_write(int addr, int data)
	{ // writes to $ffc0-$ffff always go to apuram, even if the iplrom is enabled
		if (status.ram_writable && !status.ram_disabled)
		{
			apuram.write8bit(addr & 0xFFFF, (byte) data);
		}
	}

	private int op_busread(int addr)
	{
		int r = 0;
		if ((addr & 0xfff0) == 0x00f0)
		{ // 00f0-00ff
			switch (addr)
			{
			case 0xf0:
			{ // TEST -- write-only register
				r = 0x00;
			}
				break;
			case 0xf1:
			{ // CONTROL -- write-only register
				r = 0x00;
			}
				break;
			case 0xf2:
			{ // DSPADDR
				r = status.dsp_addr;
			}
				break;
			case 0xf3:
			{ // DSPDATA
				// 0x80-0xff are read-only mirrors of 0x00-0x7f
				r = dsp_bus.read8bit(status.dsp_addr & 0x7f) & 0xFF;
			}
				break;
			case 0xf4: // CPUIO0
			case 0xf5: // CPUIO1
			case 0xf6: // CPUIO2
			case 0xf7:
			{ // CPUIO3
				// synchronize_cpu();
				r = cpu_bus.read8bit(addr) & 0xFF;
			}
				break;
			case 0xf8:
			{ // RAM0
				r = status.ram0;
			}
				break;
			case 0xf9:
			{ // RAM1
				r = status.ram1;
			}
				break;
			case 0xfa: // T0TARGET
			case 0xfb: // T1TARGET
			case 0xfc:
			{ // T2TARGET -- write-only registers
				r = 0x00;
			}
				break;
			case 0xfd:
			{ // T0OUT -- 4-bit counter value
				r = t0.stage3_ticks & 15;
				t0.stage3_ticks = 0;
			}
				break;
			case 0xfe:
			{ // T1OUT -- 4-bit counter value
				r = t1.stage3_ticks & 15;
				t1.stage3_ticks = 0;
			}
				break;
			case 0xff:
			{ // T2OUT -- 4-bit counter value
				r = t2.stage3_ticks & 15;
				t2.stage3_ticks = 0;
			}
				break;
			}
		}
		else
		{
			r = ram_read(addr);
		}

		return r & 0xFF;
	}

	private void op_buswrite(int addr, int data)
	{
		if ((addr & 0xfff0) == 0x00f0)
		{ // $00f0-00ff
			switch (addr)
			{
			case 0xf0:
			{ // TEST
				if (regs.p.p)
				{
					break; // writes only valid when P flag is clear
				}

				status.clock_speed = ((data >> 6) & 3) & 0xFF;
				status.timer_speed = ((data >> 4) & 3) & 0xFF;
				status.timers_enabled = (data & 0x08) != 0;
				status.ram_disabled = (data & 0x04) != 0;
				status.ram_writable = (data & 0x02) != 0;
				status.timers_disabled = (data & 0x01) != 0;

				status.timer_step = (1 << status.clock_speed) + (2 << status.timer_speed);

				t0.sync_stage1();
				t1.sync_stage1();
				t2.sync_stage1();
			}
				break;
			case 0xf1:
			{ // CONTROL
				status.iplrom_enabled = (data & 0x80) != 0;

				if ((data & 0x30) != 0)
				{
					// one-time clearing of APU port read registers,
					// emulated by simulating CPU writes of 0x00
					// synchronize_cpu();
					if ((data & 0x20) != 0)
					{
						cpu_bus.write8bit(2, (byte) 0x00);
						cpu_bus.write8bit(3, (byte) 0x00);
					}
					if ((data & 0x10) != 0)
					{
						cpu_bus.write8bit(0, (byte) 0x00);
						cpu_bus.write8bit(1, (byte) 0x00);
					}
				}

				// 0->1 transistion resets timers
				if (t2.enabled == false && (data & 0x04) != 0)
				{
					t2.stage2_ticks = 0;
					t2.stage3_ticks = 0;
				}
				t2.enabled = (data & 0x04) != 0;

				if (t1.enabled == false && (data & 0x02) != 0)
				{
					t1.stage2_ticks = 0;
					t1.stage3_ticks = 0;
				}
				t1.enabled = (data & 0x02) != 0;

				if (t0.enabled == false && (data & 0x01) != 0)
				{
					t0.stage2_ticks = 0;
					t0.stage3_ticks = 0;
				}
				t0.enabled = (data & 0x01) != 0;
			}
				break;
			case 0xf2:
			{ // DSPADDR
				status.dsp_addr = data & 0xFF;
			}
				break;
			case 0xf3:
			{ // DSPDATA
				// 0x80-0xff are read-only mirrors of 0x00-0x7f
				if ((status.dsp_addr & 0x80) == 0)
				{
					dsp_bus.write8bit(status.dsp_addr & 0x7f, (byte) data);
				}
			}
				break;
			case 0xf4: // CPUIO0
			case 0xf5: // CPUIO1
			case 0xf6: // CPUIO2
			case 0xf7:
			{ // CPUIO3
				// synchronize_cpu();
				write8bit(addr, (byte) data);
			}
				break;
			case 0xf8:
			{ // RAM0
				status.ram0 = data & 0xFF;
			}
				break;
			case 0xf9:
			{ // RAM1
				status.ram1 = data & 0xFF;
			}
				break;
			case 0xfa:
			{ // T0TARGET
				t0.target = data & 0xFF;
			}
				break;
			case 0xfb:
			{ // T1TARGET
				t1.target = data & 0xFF;
			}
				break;
			case 0xfc:
			{ // T2TARGET
				t2.target = data & 0xFF;
			}
				break;
			case 0xfd: // T0OUT
			case 0xfe: // T1OUT
			case 0xff:
			{ // T2OUT -- read-only registers
			}
				break;
			}
		}

		// all writes, even to MMIO registers, appear on bus
		ram_write(addr, data & 0xFF);
	}

	@Override
	public void op_io()
	{
		add_clocks(24);
		cycle_edge();
	}

	@Override
	public byte op_read(int addr)
	{
		add_clocks(12);
		int r = op_busread(addr);
		add_clocks(12);
		cycle_edge();
		return (byte) r;
	}

	@Override
	public void op_write(int addr, byte data)
	{
		add_clocks(24);
		op_buswrite(addr, data);
		cycle_edge();
	}

	private SMPTimer t0;
	private SMPTimer t1;
	private SMPTimer t2;

	private void add_clocks(long clocks)
	{
		clock += clocks * frequency_multiplier;
		dsp_clk.clock(clocks);

		// forcefully sync S-SMP to S-CPU in case chips are not communicating
		// sync if S-SMP is more than 24 samples ahead of S-CPU
		// if (getProcessor().clock > +(768 * 24 * (long)24000000))
		// {
		// synchronize_cpu();
		// }
	}

	private void cycle_edge()
	{
		t0.tick();
		t1.tick();
		t2.tick();

		// TEST register S-SMP speed control
		// 24 clocks have already been added for this cycle at this point
		switch (status.clock_speed)
		{
		case 0:
			break; // 100% speed
		case 1:
			add_clocks(24);
			break; // 50% speed
		case 2:
			System.out.println("SMP lock (entering infinite loop)");
			while (true)
			{
				add_clocks(24); // 0% speed -- locks S-SMP
			}
		case 3:
			add_clocks(216); // 24 * 9
			break; // 10% speed
		}
	}

	// this is the IPLROM for the S-SMP coprocessor.
	// the S-SMP does not allow writing to the IPLROM.
	// all writes are instead mapped to the extended
	// RAM region, accessible when $f1.d7 is clear.

	private static int[] iplrom =
	{
			/* ffc0 */0xcd, 0xef, // mov x,#$ef
			/* ffc2 */0xbd, // mov sp,x
			/* ffc3 */0xe8, 0x00, // mov a,#$00
			/* ffc5 */0xc6, // mov (x),a
			/* ffc6 */0x1d, // dec x
			/* ffc7 */0xd0, 0xfc, // bne $ffc5
			/* ffc9 */0x8f, 0xaa, 0xf4, // mov $f4,#$aa
			/* ffcc */0x8f, 0xbb, 0xf5, // mov $f5,#$bb
			/* ffcf */0x78, 0xcc, 0xf4, // cmp $f4,#$cc
			/* ffd2 */0xd0, 0xfb, // bne $ffcf
			/* ffd4 */0x2f, 0x19, // bra $ffef
			/* ffd6 */0xeb, 0xf4, // mov y,$f4
			/* ffd8 */0xd0, 0xfc, // bne $ffd6
			/* ffda */0x7e, 0xf4, // cmp y,$f4
			/* ffdc */0xd0, 0x0b, // bne $ffe9
			/* ffde */0xe4, 0xf5, // mov a,$f5
			/* ffe0 */0xcb, 0xf4, // mov $f4,y
			/* ffe2 */0xd7, 0x00, // mov ($00)+y,a
			/* ffe4 */0xfc, // inc y
			/* ffe5 */0xd0, 0xf3, // bne $ffda
			/* ffe7 */0xab, 0x01, // inc $01
			/* ffe9 */0x10, 0xef, // bpl $ffda
			/* ffeb */0x7e, 0xf4, // cmp y,$f4
			/* ffed */0x10, 0xeb, // bpl $ffda
			/* ffef */0xba, 0xf6, // movw ya,$f6
			/* fff1 */0xda, 0x00, // movw $00,ya
			/* fff3 */0xba, 0xf4, // movw ya,$f4
			/* fff5 */0xc4, 0xf4, // mov $f4,a
			/* fff7 */0xdd, // mov a,y
			/* fff8 */0x5d, // mov x,a
			/* fff9 */0xd0, 0xdb, // bne $ffd6
			/* fffb */0x1f, 0x00, 0x00, // jmp ($0000+x)
			/* fffe */0xc0, 0xff // reset vector location ($ffc0)
	};

	Status status = new Status();

	@Override
	public Object readConfig(String key)
	{
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("region")) frequency_multiplier = value.toString().equals("ntsc") ? CPU_FREQUENCY_NTSC : CPU_FREQUENCY_PAL;
		else if(key.equals("enableaudio"))
		{
			enableaudio = (Boolean) value;
		}
		else if(key.equals("save")) {
			ObjectOutputStream out = (ObjectOutputStream)value;
			
			try
			{
				out.writeInt(frequency_multiplier);
				out.writeLong(clock);
				out.writeObject(t0);
				out.writeObject(t1);
				out.writeObject(t2);
				out.writeObject(status);
				
				out.writeObject(regs);
				out.writeInt(dp);
				out.writeInt(sp);
				out.writeInt(rd);
				out.writeInt(wr);
				out.writeInt(bit);
				out.writeInt(ya);
			}
			catch(Exception e) { e.printStackTrace(); }
			
			
			
		}
		else if(key.equals("load")) {
			ObjectInputStream in = (ObjectInputStream)value;
			try
			{
				frequency_multiplier = in.readInt();
				clock = in.readLong();
				t0 = (SMPTimer)in.readObject();
				t1 = (SMPTimer)in.readObject();
				t2 = (SMPTimer)in.readObject();
				status = (Status)in.readObject();
		        
				regs = (jario.snes.smp.Regs)in.readObject();
				dp = in.readInt();
				sp = in.readInt();
				rd = in.readInt();
				wr = in.readInt();
				bit = in.readInt();
				ya = in.readInt();
			}
			catch(Exception e) { e.printStackTrace(); }
			
		}
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		apuram = null;
		dsp_clk = null;
		dsp_bus = null;
		cpu_bus = null;
		t0 = null;
		t1 = null;
		t2 = null;
		status = null;
	}
}
