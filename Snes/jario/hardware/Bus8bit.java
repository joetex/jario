/**
 * Fractal Component Plugin Spec v1.1
 * by Jason LaDere (Jario)
 */

package jario.hardware;

public interface Bus8bit extends java.io.Serializable
{
	public byte read8bit(int address);
	public void write8bit(int address, byte data);
}
