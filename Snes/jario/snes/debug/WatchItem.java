package jario.snes.debug;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jario.ai.supermetroid.AIController;
import jario.ai.utils.BusExtras;
import javafx.scene.paint.Color;

public class WatchItem extends JPanel
{
	private static final long serialVersionUID = 7169764393624329274L;
	
	public JTextField 		txtTitle;
	public JTextField 		txtAddress;
	public JTextField 		txtValue;
	
	int iAddress;
	String szAddress;
	int iBitSize;
	
	public DeleteButton btnDelete;
	
	public boolean markedForDelete = false;
	
	public WatchItem(String title, int address, int bitsize)
	{
		txtTitle = new JTextField(title);
		txtTitle.setMaximumSize(new Dimension(100,20));
		txtTitle.setEditable(false);
		txtTitle.setBackground(java.awt.Color.LIGHT_GRAY);
		txtTitle.setMaximumSize(new Dimension(100,20));
		
		//szAddress = address.replaceAll("[^A-Fa-f0-9]", "");
		iAddress = address;//Integer.parseInt(address);
		txtAddress = new JTextField(INTtoHEX(address));
		txtAddress.setEditable(false);
		txtAddress.setBackground(java.awt.Color.LIGHT_GRAY);
		txtAddress.setMaximumSize(new Dimension(100,20));
		
		
		txtValue = new JTextField("");
		txtValue.setMaximumSize(new Dimension(100,20));
		txtValue.setEditable(false);
		txtValue.setBackground(java.awt.Color.LIGHT_GRAY);
		txtValue.setMaximumSize(new Dimension(100,20));
		
		btnDelete = new DeleteButton("X", szAddress);
		btnDelete.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				markedForDelete = true;
			}          
	    });
		btnDelete.setMaximumSize(new Dimension(50,20));
		
		iBitSize = bitsize;
		
		this.setMaximumSize(new Dimension(300, 20));
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		this.add(txtTitle);
		this.add(txtAddress);
		this.add(txtValue);
		this.add(btnDelete);
	}
	
	public boolean shouldDelete()
	{
		return markedForDelete;
	}
	
	public void update()
	{
		int value = 0;
		if( iBitSize == 16 )
		{
			value = BusExtras.read16bit(AIController.memorybus, iAddress);
		}
		else if( iBitSize == 8 )
		{
			value = AIController.memorybus.read8bit(iAddress);
		}
		
		txtValue.setText("" + value);
	}
	
	public String INTtoHEX(int address)
	{
		return Integer.toHexString(address);
	}
}
