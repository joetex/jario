/**
 * Fractal Component Plugin Spec v1.1
 * by Jason LaDere (Jario)
 */

package jario.hardware;

public interface Bus64bit extends java.io.Serializable
{
	public long read64bit(int address);
	public void write64bit(int address, long data);
}
