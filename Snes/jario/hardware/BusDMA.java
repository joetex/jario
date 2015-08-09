/**
 * Fractal Component Plugin Spec v1.1
 * by Jason LaDere (Jario)
 */

package jario.hardware;

import java.nio.ByteBuffer;

public interface BusDMA extends java.io.Serializable
{
	public void readDMA(int address, ByteBuffer data, int offset, int length);
	public void writeDMA(int address, ByteBuffer data, int offset, int length);
}
