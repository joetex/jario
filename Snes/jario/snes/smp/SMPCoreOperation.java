/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.smp;

public class SMPCoreOperation implements java.io.Serializable
{
	public interface SMPCoreOp extends java.io.Serializable
	{
		public SMPCoreOpResult Invoke(SMPCoreOpArgument args);
	}

	private SMPCoreOp op;
	private SMPCoreOpArgument args;

	public SMPCoreOperation(SMPCoreOp op, SMPCoreOpArgument args)
	{
		this.op = op;
		this.args = args;
	}

	public final void Invoke()
	{
		op.Invoke(args);
	}
}
