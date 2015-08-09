/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.cpu;

public class DMA implements java.io.Serializable
{
	//private CPU self;

	public DMA(CPU self)
	{
		//this.self = self;
	}

	private boolean dma_transfer_valid(int bbus, int abus)
	{   // transfers from WRAM to WRAM are invalid; chip only has one address bus
		if (bbus == 0x80 && ((abus & 0xfe0000) == 0x7e0000 || (abus & 0x40e000) == 0x0000)) { return false; }
		return true;
	}

	private boolean dma_addr_valid(int abus)
	{   // A-bus access to B-bus or S-CPU registers are invalid
		if ((abus & 0x40ff00) == 0x2100) { return false; } // $[00-3f|80-bf]:[2100-21ff]
		if ((abus & 0x40fe00) == 0x4000) { return false; } // $[00-3f|80-bf]:[4000-41ff]
		if ((abus & 0x40ffe0) == 0x4200) { return false; } // $[00-3f|80-bf]:[4200-421f]
		if ((abus & 0x40ff80) == 0x4300) { return false; } // $[00-3f|80-bf]:[4300-437f]
		return true;
	}

	private byte dma_read(int abus)
	{
		if (dma_addr_valid(abus) == false) { return 0x00; }
		return CPU.cpu.bus.read8bit(abus);
	}

	private void dma_write(boolean valid, int addr, byte data)
	{
		if (valid)
		{
			CPU.cpu.bus.write8bit(addr, data);
		}
	}

	private void dma_transfer(boolean direction, int bbus, int abus)
	{
		if ((direction ? 1 : 0) == 0)
		{
			byte data = dma_read(abus);
			CPU.cpu.add_clocks(8);
			dma_write(dma_transfer_valid(bbus, abus), (0x2100 | bbus), data);
		}
		else
		{
			byte data = dma_transfer_valid(bbus, abus) ? CPU.cpu.bus.read8bit(0x2100 | (bbus & 0xFF)) : (byte) 0x00;
			CPU.cpu.add_clocks(8);
			dma_write(dma_addr_valid(abus), abus, data);
		}
	}

	private int dma_bbus(int i, int index)
	{
		switch (CPU.cpu.channel[i].transfer_mode)
		{
		default:
		case 0:
			return (CPU.cpu.channel[i].dest_addr); // 0
		case 1:
			return (CPU.cpu.channel[i].dest_addr + (index & 1)); // 0,1
		case 2:
			return (CPU.cpu.channel[i].dest_addr); // 0,0
		case 3:
			return (CPU.cpu.channel[i].dest_addr + ((index >> 1) & 1)); // 0,0,1,1
		case 4:
			return (CPU.cpu.channel[i].dest_addr + (index & 3)); // 0,1,2,3
		case 5:
			return (CPU.cpu.channel[i].dest_addr + (index & 1)); // 0,1,0,1
		case 6:
			return (CPU.cpu.channel[i].dest_addr); // 0,0 [2]
		case 7:
			return (CPU.cpu.channel[i].dest_addr + ((index >> 1) & 1)); // 0,0,1,1 [3]
		}
	}

	private int dma_addr(int i)
	{
		int result = (((CPU.cpu.channel[i].source_bank & 0xFF) << 16) | (CPU.cpu.channel[i].source_addr & 0xFFFF));

		if (CPU.cpu.channel[i].fixed_transfer == false)
		{
			if (CPU.cpu.channel[i].reverse_transfer == false)
			{
				CPU.cpu.channel[i].source_addr++;
			}
			else
			{
				CPU.cpu.channel[i].source_addr--;
			}
		}

		return result;
	}

	private int hdma_addr(int i)
	{
		return ((CPU.cpu.channel[i].source_bank & 0xFF) << 16) | (CPU.cpu.channel[i].hdma_addr++ & 0xFFFF);
	}

	private int hdma_iaddr(int i)
	{
		return ((CPU.cpu.channel[i].indirect_bank & 0xFF) << 16) | (CPU.cpu.channel[i].indirect_addr++ & 0xFFFF);
	}

	void dma_run()
	{
		CPU.cpu.add_clocks(16);

		for (int i = 0; i < 8; i++)
		{
			if (CPU.cpu.channel[i].dma_enabled == false)
			{
				continue;
			}
			CPU.cpu.add_clocks(8);

			int index = 0;
			do
			{
				dma_transfer(CPU.cpu.channel[i].direction, dma_bbus(i, index++), dma_addr(i));
			} while (CPU.cpu.channel[i].dma_enabled && (CPU.cpu.channel[i].transfer_size_decremented() & 0xFFFF) != 0);

			CPU.cpu.channel[i].dma_enabled = false;
		}

		CPU.cpu.status.irq_lock = true;
	}

	// private boolean hdma_active_after(int i)
	// {
	// for (int n = i + 1; i < 8; i++)
	// {
	// if (channel[i].hdma_enabled && !channel[i].hdma_completed)
	// {
	// return true;
	// }
	// }
	// return false;
	// }

	private void hdma_update(int i)
	{
		if ((CPU.cpu.channel[i].line_counter & 0x7f) == 0)
		{
			CPU.cpu.channel[i].line_counter = dma_read(hdma_addr(i)) & 0xFF;
			CPU.cpu.channel[i].hdma_completed = ((CPU.cpu.channel[i].line_counter & 0xFF) == 0);
			CPU.cpu.channel[i].hdma_do_transfer = !CPU.cpu.channel[i].hdma_completed;
			CPU.cpu.add_clocks(8);

			if (CPU.cpu.channel[i].indirect)
			{
				CPU.cpu.channel[i].indirect_addr = ((dma_read(hdma_addr(i)) & 0xFF) << 8) & 0xFFFF;
				CPU.cpu.add_clocks(8);

				// emulating this glitch causes a slight slowdown; only enable
				// if needed
				// if(!channel[i].hdma_completed || hdma_active_after(i)) {
				CPU.cpu.channel[i].indirect_addr >>= 8;
				CPU.cpu.channel[i].indirect_addr |= (((dma_read(hdma_addr(i)) & 0xFF) << 8) & 0xFFFF);
				CPU.cpu.add_clocks(8);
				// }
			}
		}
	}

	private static int[] transfer_length = { 1, 2, 2, 4, 4, 4, 2, 4 };

	void hdma_run()
	{
		int channels = 0;
		for (int i = 0; i < 8; i++)
		{
			if (CPU.cpu.channel[i].hdma_enabled)
			{
				channels++;
			}
		}
		if (channels == 0) { return; }

		CPU.cpu.add_clocks(16);
		for (int i = 0; i < 8; i++)
		{
			if (CPU.cpu.channel[i].hdma_enabled == false || CPU.cpu.channel[i].hdma_completed == true)
			{
				continue;
			}
			CPU.cpu.channel[i].dma_enabled = false;

			if (CPU.cpu.channel[i].hdma_do_transfer)
			{
				int length = transfer_length[CPU.cpu.channel[i].transfer_mode];
				for (int index = 0; index < length; index++)
				{
					int addr = CPU.cpu.channel[i].indirect == false ? hdma_addr(i) : hdma_iaddr(i);
					dma_transfer(CPU.cpu.channel[i].direction, dma_bbus(i, index), addr);
				}
			}
		}

		for (int i = 0; i < 8; i++)
		{
			if (CPU.cpu.channel[i].hdma_enabled == false || CPU.cpu.channel[i].hdma_completed == true)
			{
				continue;
			}

			CPU.cpu.channel[i].line_counter--;
			CPU.cpu.channel[i].hdma_do_transfer = (CPU.cpu.channel[i].line_counter & 0x80) != 0;
			hdma_update(i);
		}

		CPU.cpu.status.irq_lock = true;
	}

	void hdma_init()
	{
		int channels = 0;
		for (int i = 0; i < 8; i++)
		{
			CPU.cpu.channel[i].hdma_completed = false;
			CPU.cpu.channel[i].hdma_do_transfer = false;
			if (CPU.cpu.channel[i].hdma_enabled)
			{
				channels++;
			}
		}
		if (channels == 0) { return; }

		CPU.cpu.add_clocks(16);
		for (int i = 0; i < 8; i++)
		{
			if (!CPU.cpu.channel[i].hdma_enabled)
			{
				continue;
			}
			CPU.cpu.channel[i].dma_enabled = false;

			CPU.cpu.channel[i].hdma_addr = CPU.cpu.channel[i].source_addr & 0xFFFF;
			CPU.cpu.channel[i].line_counter = 0;
			hdma_update(i);
		}

		CPU.cpu.status.irq_lock = true;
	}

	void dma_reset()
	{
		for (int i = 0; i < 8; i++)
		{
			CPU.cpu.channel[i].dma_enabled = false;
			CPU.cpu.channel[i].hdma_enabled = false;

			CPU.cpu.channel[i].direction = true;
			CPU.cpu.channel[i].indirect = true;
			CPU.cpu.channel[i].unused = true;
			CPU.cpu.channel[i].reverse_transfer = true;
			CPU.cpu.channel[i].fixed_transfer = true;
			CPU.cpu.channel[i].transfer_mode = 0x07;

			CPU.cpu.channel[i].dest_addr = 0xff;
			CPU.cpu.channel[i].source_addr = 0xffff;
			CPU.cpu.channel[i].source_bank = 0xff;

			CPU.cpu.channel[i].transfer_size(0xffff);
			CPU.cpu.channel[i].indirect_addr = 0xffff;

			CPU.cpu.channel[i].indirect_bank = 0xff;
			CPU.cpu.channel[i].hdma_addr = 0xff;
			CPU.cpu.channel[i].line_counter = 0xff;
			CPU.cpu.channel[i].unknown = 0xff;

			CPU.cpu.channel[i].hdma_completed = false;
			CPU.cpu.channel[i].hdma_do_transfer = false;
		}
	}
}
