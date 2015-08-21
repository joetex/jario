package jario.snes.debug;

import java.awt.Graphics;

import javax.swing.JFrame;

public class DebugWindow extends JFrame
{
	JFrame window;
	Graphics graphics;
	
	public boolean enabled = false;
	
	ActionWatch pnlWatch;
	
	public DebugWindow()
	{
		setTitle("Debug Window");
		
		pnlWatch = new ActionWatch();
		
		this.add(pnlWatch);
		this.invalidate();
	}
	
	public void update()
	{
		if( !enabled )
			return;
		
		pnlWatch.update();
		
		this.invalidate();
	}
}
