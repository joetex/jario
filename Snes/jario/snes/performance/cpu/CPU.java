/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.cpu;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.Executors;

import jario.hardware.Bus1bit;
import jario.hardware.Bus32bit;
import jario.hardware.Bus8bit;
import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;
import jario.snes.accessories.VideoPlayer;
import jario.snes.performance.cpu.PriorityQueue.Callback;
import jario.snes.video.Video;

public class CPU extends CPUCore implements Hardware, Clockable, Bus1bit, Bus8bit, Configurable, java.io.Serializable
{
	public static final int NTSC = 0;
	public static final int PAL = 1;

	protected int region;
	
	public static CPU cpu;

	protected transient Bus8bit bus;
	protected transient Clockable smp;
	protected transient Clockable ppu;
	protected transient Bus1bit ppu1bit;
	protected transient Bus8bit smp_bus;
	protected transient Bus8bit input_port;
	protected transient Bus32bit video;

	private long smp_clock;

	private transient Clockable coprocessors;

	// timing
	private static final int QueueEvent_DramRefresh = 0;
	private static final int QueueEvent_HdmaRun = 1;
	// private static final int QueueEvent_ControllerLatch = 2;
	private PriorityQueue queue;

	private transient DMA dma;
	// registers
	int[] port_data = new int[4];
	Channel[] channel = new Channel[8];
	Status status = new BusB();
	transient HVCounter counter = new HVCounter();

	private boolean enableppu = true;
	private boolean enableaudio = true;
	
	public long clockcnt = 0;
	private boolean freeze = false;
	private boolean frozen = false;
	
	//adding helper method for serialization to save/initialize super class state
	
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException{
        ois.defaultReadObject();
        
        cpu = this;
        dma = new DMA(this);
        
        //notice the order of read and write should be same
        counter = new HVCounter();
        counter.scanline = this.scanline;
    }
     
    private void writeObject(ObjectOutputStream oos) throws IOException{
        oos.defaultWriteObject();
         
    }
    
	public CPU()
	{
		cpu = this;
		for (int i = 0; i < channel.length; i++) channel[i] = new Channel();
		queue = new PriorityQueue(512, this.queue_event);
		dma = new DMA(this);
		counter.scanline = this.scanline;
		power();
	}

	@Override
	public void connect(int port, Hardware hw)
	{
		switch (port)
		{
		case 0:
			bus = (Bus8bit) hw;
			break;
		case 1:
			smp = (Clockable) hw;
			smp_bus = (Bus8bit) hw;
			break;
		case 2:
			input_port = (Bus8bit) hw;
			break;
		case 3:
			video = (Bus32bit) hw;
			break;
		case 4:
			ppu = (Clockable) hw;
			ppu1bit = (Bus1bit) hw;
			counter.ppu1bit = ppu1bit;
			break;
		case 5:
			coprocessors = (Clockable) hw;
			break;
		}
	}

	@Override
	public void clock(long clocks)
	{
		clockcnt = clocks;
		
		while (clocks-- > 0L)
		{
			if( freeze ) {
				if( !frozen )
				{
					frozen = true;
					
				}
				continue;
			}
			
			if (status.nmi_pending)
			{
				status.nmi_pending = false;
				op_irq((regs.e == false ? 0xffea : 0xfffa));
			}

			if (status.irq_pending)
			{
				status.irq_pending = false;
				op_irq((regs.e == false ? 0xffee : 0xfffe));
			}

			opcode_table.get(op_readpc()).Invoke();
			clockcnt = clocks;
		}
	}

	@Override
	public void reset()
	{
		smp_clock = 0;

		// should this remove the coprocessors or reset them?
		//coprocessors = null;
		counter.reset();

		regs.pc.set(0x000000);
		regs.x.h(0x00);
		regs.y.h(0x00);
		regs.s.h(0x01);
		regs.d.set(0x0000);
		regs.db = 0x00;
		regs.p.set(0x34);
		regs.e = true;
		regs.mdr = 0x00;
		regs.wai = false;
		update_table();

		if (bus != null)
		{
			regs.pc.l(bus.read8bit(0xfffc) & 0xFF);
			regs.pc.h(bus.read8bit(0xfffd) & 0xFF);
			regs.pc.b(0x00);
		}
		else
		{
			regs.pc.set(0x000000);
		}

		status.nmi_valid = false;
		status.nmi_line = false;
		status.nmi_transition = false;
		status.nmi_pending = false;

		status.irq_valid = false;
		status.irq_line = false;
		status.irq_transition = false;
		status.irq_pending = false;

		status.irq_lock = false;
		status.hdma_pending = false;

		status.wram_addr = 0x000000;

		status.nmi_enabled = false;
		status.virq_enabled = false;
		status.hirq_enabled = false;
		status.auto_joypad_poll_enabled = false;

		status.pio = 0xff;

		status.htime = 0x0000;
		status.vtime = 0x0000;

		status.rom_speed = 8;

		status.joy1l = status.joy1h = 0x00;
		status.joy2l = status.joy2h = 0x00;
		status.joy3l = status.joy3h = 0x00;
		status.joy4l = status.joy4h = 0x00;

		dma.dma_reset();
	}

	@Override
	public byte read8bit(int addr)
	{
		if ((addr & 0xffc0) == 0x2140)
		{
			synchronize_smp();
			return smp_bus.read8bit(addr & 3);
		}

		switch (addr & 0xffff)
		{
		case 0x2180:
		{
			byte result = bus.read8bit(0x7e0000 | status.wram_addr);
			status.wram_addr = (status.wram_addr + 1) & 0x01ffff;
			return result;
		}
		case 0x4016:
		{
			int result = (regs.mdr & 0xfc);
			result |= (input_port.read8bit(0) & 3);
			return (byte) result;
		}
		case 0x4017:
		{
			int result = ((regs.mdr & 0xe0) | 0x1c);
			result |= (input_port.read8bit(1) & 3);
			return (byte) result;
		}
		case 0x4210:
		{
			int result = (regs.mdr & 0x70);
			result |= ((status.nmi_line ? 1 : 0) << 7);
			result |= 0x02; // CPU revision
			status.nmi_line = false;
			return (byte) result;
		}
		case 0x4211:
		{
			int result = (regs.mdr & 0x7f);
			result |= ((status.irq_line ? 1 : 0) << 7);
			status.irq_line = false;
			return (byte) result;
		}
		case 0x4212:
		{
			int result = (regs.mdr & 0x3e);
			int vbstart = !ppu1bit.read1bit(1) ? 225 : 240;

			if (counter.vcounter() >= vbstart && counter.vcounter() <= vbstart + 2)
			{
				result |= 0x01;
			}
			if (counter.hcounter() <= 2 || counter.hcounter() >= 1096)
			{
				result |= 0x40;
			}
			if (counter.vcounter() >= vbstart)
			{
				result |= 0x80;
			}

			return (byte) result;
		}
		case 0x4213:
			return (byte) status.pio;
		case 0x4214:
			return (byte) (status.rddiv >> 0);
		case 0x4215:
			return (byte) (status.rddiv >> 8);
		case 0x4216:
			return (byte) (status.rdmpy >> 0);
		case 0x4217:
			return (byte) (status.rdmpy >> 8);
		case 0x4218:
			return (byte) status.joy1l;
		case 0x4219:
			return (byte) status.joy1h;
		case 0x421a:
			return (byte) status.joy2l;
		case 0x421b:
			return (byte) status.joy2h;
		case 0x421c:
			return (byte) status.joy3l;
		case 0x421d:
			return (byte) status.joy3h;
		case 0x421e:
			return (byte) status.joy4l;
		case 0x421f:
			return (byte) status.joy4h;
		}

		if ((addr & 0xff80) == 0x4300)
		{
			int i = (addr >> 4) & 7;
			switch (addr & 0xff8f)
			{
			case 0x4300:
			{
				return (byte) (((channel[i].direction ? 1 : 0) << 7)
						| ((channel[i].indirect ? 1 : 0) << 6)
						| ((channel[i].unused ? 1 : 0) << 5)
						| ((channel[i].reverse_transfer ? 1 : 0) << 4)
						| ((channel[i].fixed_transfer ? 1 : 0) << 3)
						| (channel[i].transfer_mode << 0));
			}

			case 0x4301:
				return (byte) channel[i].dest_addr;
			case 0x4302:
				return (byte) (channel[i].source_addr >> 0);
			case 0x4303:
				return (byte) (channel[i].source_addr >> 8);
			case 0x4304:
				return (byte) channel[i].source_bank;
			case 0x4305:
				return (byte) (channel[i].transfer_size() >> 0);
			case 0x4306:
				return (byte) (channel[i].transfer_size() >> 8);
			case 0x4307:
				return (byte) channel[i].indirect_bank;
			case 0x4308:
				return (byte) (channel[i].hdma_addr >> 0);
			case 0x4309:
				return (byte) (channel[i].hdma_addr >> 8);
			case 0x430a:
				return (byte) channel[i].line_counter;
			case 0x430b:
			case 0x430f:
				return (byte) channel[i].unknown;
			}
		}

		return (byte) regs.mdr;
	}

	@Override
	public void write8bit(int addr, byte data_)
	{
		int data = data_ & 0xFF;

		if ((addr & 0xffc0) == 0x2140)
		{
			synchronize_smp();
			port_write((addr & 3), data_);
			return;
		}

		switch (addr & 0xffff)
		{
		case 0x2180:
		{
			bus.write8bit(0x7e0000 | status.wram_addr, data_);
			status.wram_addr = (status.wram_addr + 1) & 0x01ffff;
			return;
		}
		case 0x2181:
		{
			status.wram_addr = (status.wram_addr & 0x01ff00) | (data << 0);
			return;
		}
		case 0x2182:
		{
			status.wram_addr = (status.wram_addr & 0x0100ff) | (data << 8);
			return;
		}
		case 0x2183:
		{
			status.wram_addr = (status.wram_addr & 0x00ffff) | ((data & 1) << 16);
			return;
		}
		case 0x4016:
		{
			input_port.write8bit(0, data_);
			return;
		}
		case 0x4200:
		{
			boolean nmi_enabled = status.nmi_enabled;

			status.nmi_enabled = (data & 0x80) != 0;
			status.virq_enabled = (data & 0x20) != 0;
			status.hirq_enabled = (data & 0x10) != 0;
			status.auto_joypad_poll_enabled = (data & 0x01) != 0;

			if (!nmi_enabled && status.nmi_enabled && status.nmi_line)
			{
				status.nmi_transition = true;
			}

			if (status.virq_enabled && !status.hirq_enabled && status.irq_line)
			{
				status.irq_transition = true;
			}

			if (!status.virq_enabled && !status.hirq_enabled)
			{
				status.irq_line = false;
				status.irq_transition = false;
			}

			status.irq_lock = true;
			return;
		}
		case 0x4201:
		{
			ppu1bit.write1bit(29, ((data >> 7) & 0x1) != 0);
			status.pio = data;
			// goto case 0x4202;
		}
		case 0x4202:
		{
			status.wrmpya = data;
			return;
		}
		case 0x4203:
		{
			status.wrmpyb = data;
			status.rdmpy = (status.wrmpya * status.wrmpyb) & 0xFFFF;
			return;
		}
		case 0x4204:
		{
			status.wrdiva = ((status.wrdiva & 0xff00) | (data << 0));
			return;
		}
		case 0x4205:
		{
			status.wrdiva = ((data << 8) | (status.wrdiva & 0x00ff));
			return;
		}
		case 0x4206:
		{
			status.wrdivb = data;
			status.rddiv = ((status.wrdivb) != 0 ? status.wrdiva / status.wrdivb : 0xffff) & 0xFFFF;
			status.rdmpy = ((status.wrdivb) != 0 ? status.wrdiva % status.wrdivb : status.wrdiva) & 0xFFFF;
			return;
		}
		case 0x4207:
		{
			status.htime = ((status.htime & 0x0100) | (data << 0));
			return;
		}
		case 0x4208:
		{
			status.htime = (((data & 1) << 8) | (status.htime & 0x00ff));
			return;
		}
		case 0x4209:
		{
			status.vtime = ((status.vtime & 0x0100) | (data << 0));
			return;
		}
		case 0x420a:
		{
			status.vtime = (((data & 1) << 8) | (status.vtime & 0x00ff));
			return;
		}
		case 0x420b:
		{
			for (int i = 0; i < 8; i++)
			{
				channel[i].dma_enabled = (data & (1 << i)) != 0;
			}
			if (data != 0)
			{
				dma.dma_run();
			}
			return;
		}
		case 0x420c:
		{
			for (int i = 0; i < 8; i++)
			{
				channel[i].hdma_enabled = (data & (1 << i)) != 0;
			}
			return;
		}
		case 0x420d:
		{
			status.rom_speed = (data & 1) != 0 ? 6 : 8;
			return;
		}
		}

		if ((addr & 0xff80) == 0x4300)
		{
			int i = (addr >> 4) & 7;
			switch (addr & 0xff8f)
			{
			case 0x4300:
			{
				channel[i].direction = (data & 0x80) != 0;
				channel[i].indirect = (data & 0x40) != 0;
				channel[i].unused = (data & 0x20) != 0;
				channel[i].reverse_transfer = (data & 0x10) != 0;
				channel[i].fixed_transfer = (data & 0x08) != 0;
				channel[i].transfer_mode = (data & 0x07);
				return;
			}
			case 0x4301:
			{
				channel[i].dest_addr = data;
				return;
			}
			case 0x4302:
			{
				channel[i].source_addr = ((channel[i].source_addr & 0xff00) | (data << 0));
				return;
			}
			case 0x4303:
			{
				channel[i].source_addr = ((data << 8) | (channel[i].source_addr & 0x00ff));
				return;
			}
			case 0x4304:
			{
				channel[i].source_bank = data;
				return;
			}
			case 0x4305:
			{
				channel[i].transfer_size(((channel[i].transfer_size() & 0xff00) | (data << 0)));
				return;
			}
			case 0x4306:
			{
				channel[i].transfer_size(((data << 8) | (channel[i].transfer_size() & 0x00ff)));
				return;
			}
			case 0x4307:
			{
				channel[i].indirect_bank = data;
				return;
			}
			case 0x4308:
			{
				channel[i].hdma_addr = ((channel[i].hdma_addr & 0xff00) | (data << 0));
				return;
			}
			case 0x4309:
			{
				channel[i].hdma_addr = ((data << 8) | (channel[i].hdma_addr & 0x00ff));
				return;
			}
			case 0x430a:
			{
				channel[i].line_counter = data;
				return;
			}
			case 0x430b:
			case 0x430f:
			{
				channel[i].unknown = data;
				return;
			}
			}
		}
	}

	@Override
	public Object readConfig(String key)
	{
		if( key.equals("enableppu")) return enableppu;
		else if(key.equals("freeze")) return freeze;
		else if (key.equals("BUS B")) return status;
		else if(key.equals("frozen")) return frozen;
		else if (key.equals("regs_aa")) {
			return aa;	
		}
		else if (key.equals("regs_rd")) {
			return rd;
		}
		else if (key.equals("regs")) {
			return regs;
		}
		else if (key.equals("sp")) {
			return sp;
		}
		else if (key.equals("dp")) {
			return dp;
		}
		
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if(key.equals("enableppu")) enableppu = (Boolean) value;
		else if(key.equals("freeze")) {
			freeze = (Boolean) value;
			((Configurable) cpu).writeConfig(key, value);
		}
		else if (key.equals("region")) region = counter.region = value.toString().equals("ntsc") ? NTSC : PAL;
		else if (key.equals("enableaudio"))
		{
			enableaudio = (Boolean) value;
			((Configurable) smp).writeConfig(key, value);
		}
		else if(key.equals("save")) {
			ObjectOutputStream out = (ObjectOutputStream)value;
			
			try
			{
				out.writeInt(region);
				out.writeObject(aa);
				out.writeObject(rd);
				out.writeObject(regs);
				out.writeInt(sp);
				out.writeInt(dp);
				out.writeLong(smp_clock);
				//out.writeObject(coprocessors);
				
				out.writeObject(status);
				out.writeObject(channel);
				//out.writeObject(queue);
				out.writeObject(port_data);
				//out.writeObject(counter);
			}
			catch(Exception e) { e.printStackTrace(); }
			
			((Configurable) smp).writeConfig(key, value);
			//((Configurable) input_port).writeConfig(key, value);
			
		}
		else if(key.equals("load")) {
			ObjectInputStream in = (ObjectInputStream)value;
			try
			{
				region = in.readInt();
				aa = (Reg24)in.readObject();
				rd = (Reg24)in.readObject();
				regs = (jario.snes.performance.cpu.Regs)in.readObject();
				sp = in.readInt();
				dp = in.readInt();
				smp_clock = in.readLong();
				//coprocessors = (Clockable)in.readObject();
				status = (Status)in.readObject();
				channel = (Channel[])in.readObject();
				//queue = (PriorityQueue)in.readObject();
				port_data = (int[])in.readObject();
				//counter = (HVCounter)in.readObject();
			}
			catch(Exception e) { e.printStackTrace(); }
			
			((Configurable) smp).writeConfig(key, value);
			//((Configurable) input_port).writeConfig(key, value);
		}
	}

	@Override
	public void op_io()
	{
		add_clocks(6);
	}

	@Override
	public byte op_read(int addr)
	{
		regs.mdr = bus.read8bit(addr);
		add_clocks(speed(addr));
		return regs.mdr;
	}

	@Override
	public void op_write(int addr, byte data)
	{
		add_clocks(speed(addr));
		bus.write8bit(addr, regs.mdr = data);
	}

	@Override
	public boolean interrupt_pending()
	{
		return false;
	}

	@Override
	public void last_cycle()
	{
		if (status.irq_lock)
		{
			status.irq_lock = false;
			return;
		}

		if (status.nmi_transition)
		{
			regs.wai = false;
			status.nmi_transition = false;
			status.nmi_pending = true;
		}

		if (status.irq_transition || regs.irq)
		{
			regs.wai = false;
			status.irq_transition = false;
			status.irq_pending = !regs.p.i;
		}
	}

	protected void step(int clocks)
	{
		smp_clock += clocks;
		//if( enableppu )
			ppu.clock(clocks);
		
		if (coprocessors != null)
		{
			coprocessors.clock(clocks);
		}
	}

	private void synchronize_smp()
	{
		//if( enableaudio )
		{
			smp.clock(smp_clock);
			smp_clock = 0;
		}
		
	}
	
	private void synchronize_coprocessor()
	{
		if (coprocessors != null)
		{
		    coprocessors.clock(0);
		}
	}

	// private byte port_read(int port)
	// {
	// return (byte)port_data[port & 3];
	// }

	private void port_write(int port, byte data)
	{
		port_data[port & 3] = data & 0xFF;
	}

	protected void power()
	{
		regs.a.set(0x0000);
		regs.x.set(0x0000);
		regs.y.set(0x0000);
		regs.s.set(0x01ff);

		reset();
	}

	private void op_irq(int vector)
	{
		op_read(regs.pc.get());
		op_io();
		if (!regs.e)
		{
			op_writestack(regs.pc.b());
		}
		op_writestack(regs.pc.h());
		op_writestack(regs.pc.l());
		op_writestack(regs.e ? (regs.p.get() & ~0x10) & 0xFF : regs.p.get() & 0xFF);
		rd.l(op_read(vector + 0));
		regs.pc.b(0x00);
		regs.p.i = true;
		regs.p.d = false;
		rd.h(op_read(vector + 1));
		regs.pc.w(rd.w());
	}

	private transient Callback queue_event = new Callback()
	{
		public void call(int id)
		{
			switch (id)
			{
			case QueueEvent_DramRefresh:
			{
				add_clocks(40);
				return;
			}
			case QueueEvent_HdmaRun:
			{
				dma.hdma_run();
				return;
			}
			// case QueueEvent_ControllerLatch:
			// {
			// //PPU.ppu.latch_counters();
			// display_bus.write8bit(0, display_bus.read8bit(3));
			// return;
			// }
			}
		}
	};

	final void add_clocks(int clocks)
	{
		if (status.hirq_enabled)
		{
			if (status.virq_enabled)
			{
				int cpu_time = counter.vcounter() * 1364 + counter.hcounter();
				int irq_time = status.vtime * 1364 + status.htime * 4;
				int framelines = (region == NTSC ? 262 : 312) + (counter.field() ? 1 : 0);
				if (cpu_time > irq_time)
				{
					irq_time += framelines * 1364;
				}
				boolean irq_valid = status.irq_valid;
				status.irq_valid = cpu_time <= irq_time && cpu_time + clocks > irq_time;
				if (!irq_valid && status.irq_valid)
				{
					status.irq_line = true;
				}
			}
			else
			{
				int irq_time = status.htime * 4;
				if (counter.hcounter() > irq_time)
				{
					irq_time += 1364;
				}
				boolean irq_valid = status.irq_valid;
				status.irq_valid = counter.hcounter() <= irq_time && counter.hcounter() + clocks > irq_time;
				if (!irq_valid && status.irq_valid)
				{
					status.irq_line = true;
				}
			}
			if (status.irq_line)
			{
				status.irq_transition = true;
			}
		}
		else if (status.virq_enabled)
		{
			boolean irq_valid = status.irq_valid;
			status.irq_valid = counter.vcounter() == status.vtime;
			if (!irq_valid && status.irq_valid)
			{
				status.irq_line = true;
			}
			if (status.irq_line)
			{
				status.irq_transition = true;
			}
		}
		else
		{
			status.irq_valid = false;
		}

		counter.tick(clocks);
		queue.tick(clocks);
		step(clocks);
	}

	private transient Runnable scanline = new Runnable()
	{
		public void run()
		{
			
			synchronize_smp();
			// synchronize_ppu();
			synchronize_coprocessor();
			
			//if( enableppu )
				video.write32bit(0, counter.vcounter());
			
			if (counter.vcounter() == 241)
			{
				//Send clock signal up to the controller (for AI)
				((Clockable) input_port).clock(0L);
				
				if( enableppu )
					((Clockable) video).clock(0L);
				((VideoPlayer)((Video) video).output).updateFPS();
			}
		
			
			

			if (counter.vcounter() == 0)
			{
				dma.hdma_init();
			}

			queue.enqueue(534, QueueEvent_DramRefresh);

			if (counter.vcounter() <= (!ppu1bit.read1bit(1) ? 224 : 239))
			{
				queue.enqueue(1104 + 8, QueueEvent_HdmaRun);
			}

			// if (PPUCounter.read32bit(0) == (input_port.read8bit(-2)&0xFF))
			// {
			// queue.enqueue((input_port.read8bit(-1)&0xFF),
			// QueueEvent_ControllerLatch);
			// }

			boolean nmi_valid = status.nmi_valid;
		
			status.nmi_valid = counter.vcounter() >= (!ppu1bit.read1bit(1) ? 225 : 240);

			if (!nmi_valid && status.nmi_valid)
			{
				status.nmi_line = true;
				if (status.nmi_enabled)
				{
					status.nmi_transition = true;
				}
			}
			else if (nmi_valid && !status.nmi_valid)
			{
				status.nmi_line = false;
			}

			
			if (status.auto_joypad_poll_enabled && counter.vcounter() == (!ppu1bit.read1bit(1) ? 227 : 242))
			{
				input_port.write8bit(1, (byte) 0); // poll
				run_auto_joypad_poll();
			}
		}
	};

	private void run_auto_joypad_poll()
	{
		int joy1 = 0, joy2 = 0, joy3 = 0, joy4 = 0;
		for (int i = 0; i < 16; i++)
		{
			byte port0 = input_port.read8bit(0);
			byte port1 = input_port.read8bit(1);

			joy1 |= ((port0 & 1) != 0 ? (0x8000 >> i) : 0);
			joy2 |= ((port1 & 1) != 0 ? (0x8000 >> i) : 0);
			joy3 |= ((port0 & 2) != 0 ? (0x8000 >> i) : 0);
			joy4 |= ((port1 & 2) != 0 ? (0x8000 >> i) : 0);
		}

		status.joy1l = joy1 & 0xFF;
		status.joy1h = (joy1 >> 8) & 0xFF;

		status.joy2l = joy2 & 0xFF;
		status.joy2h = (joy2 >> 8) & 0xFF;

		status.joy3l = joy3 & 0xFF;
		status.joy3h = (joy3 >> 8) & 0xFF;

		status.joy4l = joy4 & 0xFF;
		status.joy4h = (joy4 >> 8) & 0xFF;
	}

	private int speed(int addr)
	{
		if ((addr & 0x408000) != 0)
		{
			if ((addr & 0x800000) != 0) { return status.rom_speed; }
			return 8;
		}
		if (((addr + 0x6000) & 0x4000) != 0) { return 8; }
		if (((addr - 0x4000) & 0x7e00) != 0) { return 6; }
		return 12;
	}

	@Override
	public boolean read1bit(int address)
	{
		return false;
	}

	@Override
	public void write1bit(int address, boolean data)
	{
		if (address == 0) regs.irq = data;
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		cpu = null;
		bus = null;
		smp = null;
		ppu = null;
		ppu1bit = null;
		smp_bus = null;
		input_port = null;
		video = null;
		dma = null;
		
	}
}
