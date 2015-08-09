/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.smp;

import jario.snes.smp.SMPCoreOperation.SMPCoreOp;

public class SMPCoreOpArgument implements java.io.Serializable
{
	public int x;
	public int y;
	public int to;
	public int from;
	public int n;
	public int i;
	public int flag;
	public int value;
	public int mask;
	public SMPCoreOp op_func;
	public int op;
	public int adjust;

	public SMPCoreOpArgument()
	{
	}

	public SMPCoreOpArgument(int x)
	{
		this.x = x;
	}

	public SMPCoreOpArgument(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
}
