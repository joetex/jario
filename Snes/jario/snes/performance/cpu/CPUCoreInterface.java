package jario.snes.performance.cpu;

public interface CPUCoreInterface 
{
	public void op_io();

	public byte op_read(int addr);

	public void op_write(int addr, byte data);

	public void last_cycle();

	public boolean interrupt_pending();
}
