/**
 * Fractal Component Plugin Spec v1.1
 * by Jason LaDere (Jario)
 */

package jario.hardware;

public interface Configurable extends java.io.Serializable
{
	public Object readConfig(String key);
	public void writeConfig(String key, Object value);
}
