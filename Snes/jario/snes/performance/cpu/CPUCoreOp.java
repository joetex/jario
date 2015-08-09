package jario.snes.performance.cpu;

public interface CPUCoreOp extends java.io.Serializable
{
	public void Invoke(CPUCoreOpArgument args);
}
