/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.cpu;

public class CPUCoreOperation implements java.io.Serializable
{
	

	private CPUCoreOp op;
	private CPUCoreOpArgument args;

	public CPUCoreOperation(CPUCoreOp op, CPUCoreOpArgument args)
	{
		this.op = op;
		this.args = args;
	}

	public void Invoke()
	{
		op.Invoke(args);
	}
}
