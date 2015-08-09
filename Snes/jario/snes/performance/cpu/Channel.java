/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.cpu;

public class Channel implements java.io.Serializable
{
	public boolean dma_enabled;
	public boolean hdma_enabled;

	public boolean direction;
	public boolean indirect;
	public boolean unused;
	public boolean reverse_transfer;
	public boolean fixed_transfer;
	public int transfer_mode;

	public int dest_addr;
	public int source_addr;
	public int source_bank;

	public int indirect_addr;
	public int transfer_size() { return indirect_addr; }
	public int transfer_size_decremented() { return --indirect_addr; }
	public void transfer_size(int s) { indirect_addr = s; }

	public int indirect_bank;
	public int hdma_addr;
	public int line_counter;
	public int unknown;

	public boolean hdma_completed;
	public boolean hdma_do_transfer;
}
