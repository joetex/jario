/**
 * Fractal Component Plugin Spec v1.1
 * by Jason LaDere (Jario)
 */

package jario.hardware;

public interface Clockable extends java.io.Serializable
{
	public void clock(long time);
}
