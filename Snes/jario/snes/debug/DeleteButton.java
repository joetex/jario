package jario.snes.debug;

import javax.swing.JButton;

public class DeleteButton extends JButton
{
	private static final long serialVersionUID = 2656781331760368150L;
	
	String key;
	
	public DeleteButton(String name, String key)
	{
		super(name);
		this.key = key;
	}
}
