/**
 * Fractal Component Plugin Spec v1.1
 * by Jason LaDere (Jario)
 */

package jario.hardware;

public interface Bus32bit extends java.io.Serializable
{
	public int read32bit(int address);
	public void write32bit(int address, int data);
}
