/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.smp;

public class SMPCoreOpResult implements java.io.Serializable
{
	private int result;

	public SMPCoreOpResult(int result)
	{
		this.result = result;
	}

	public int result_byte()
	{
		return result & 0xFF;
	}

	public int result_ushort()
	{
		return result & 0xFFFF;
	}
}
