/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.cpu;


public class CPUCoreOpArgument implements java.io.Serializable
{
	public CPUCoreOp op;
	public int x;
	public int y;

	public CPUCoreOpArgument(int x)
	{
		this.x = x;
	}

	public CPUCoreOpArgument(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	public CPUCoreOpArgument(CPUCoreOp op)
	{
		this.op = op;
	}

	public CPUCoreOpArgument(CPUCoreOp op, int x)
	{
		this.op = op;
		this.x = x;
	}
}
