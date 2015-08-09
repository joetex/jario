/**
 * Fractal Component Plugin Spec v1.1
 * by Jason LaDere (Jario)
 */

package jario.hardware;

public interface Bus1bit extends java.io.Serializable
{
	public boolean read1bit(int address);
	public void write1bit(int address, boolean data);
}
