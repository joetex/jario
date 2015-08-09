/**
 * Fractal Component Plugin Spec v1.1
 * by Jason LaDere (Jario)
 */

package jario.hardware;

public interface Hardware extends java.io.Serializable
{
	public void connect(int port, Hardware hw);
	public void reset();
	public void destroy();
}
