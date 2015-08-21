package jario.snes.debug;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class ActionWatch extends JPanel
{
	JLabel lblHeader;
	JLabel lblTitle;
	JLabel lblFromAddress;
	JLabel lblToAddress;
	JLabel lblBitsize;
	
	JTextField txtTitle;
	JTextField txtFromAddress;
	JTextField txtToAddress;
	JComboBox<String> cmbBitsize;
	
	JButton btnAdd;
	
	//Map<String, WatchItem> watchList = new HashMap<String, WatchItem>();
	List<WatchItem> watchList = new ArrayList<WatchItem>();
	
	JPanel pnlHeader;
	JPanel pnlTitle;
	JPanel pnlFields;
	
	JPanel pnlItems;
	JScrollPane scrlItems;
	
	public ActionWatch()
	{
		
		
		lblHeader = new JLabel("Add Watch Value");
		lblHeader.setMaximumSize(new Dimension(300,20));
		
		lblTitle = new JLabel("Title:");
		lblTitle.setMaximumSize(new Dimension(100,20));
		
		lblFromAddress = new JLabel("From Address:");
		lblFromAddress.setMaximumSize(new Dimension(50,20));
		
		lblToAddress = new JLabel("To Address (optional):");
		lblToAddress.setMaximumSize(new Dimension(50,20));
		
		lblBitsize = new JLabel("Bit size:");
		lblBitsize.setMaximumSize(new Dimension(50,20));
		
		txtTitle = new JTextField();
		txtTitle.setMaximumSize(new Dimension(100,20));
		
		txtFromAddress = new JTextField();
		txtFromAddress.setMaximumSize(new Dimension(50,20));
		
		txtToAddress = new JTextField();
		txtToAddress.setMaximumSize(new Dimension(50,20));
		
		String[] sizeString = { "8 bits", "16 bits" };
		cmbBitsize = new JComboBox<String>(sizeString);
		cmbBitsize.setSelectedIndex(1);
		cmbBitsize.setMaximumSize(new Dimension(50,20));
		
		btnAdd = new JButton("Add");
		btnAdd.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				String title = txtTitle.getText().trim();
				String fromAddress = txtFromAddress.getText().trim();
				String toAddress = txtToAddress.getText().trim();
				int selectedItem = cmbBitsize.getSelectedIndex();
				addWatchItems(title, fromAddress, toAddress, selectedItem);
			}          
	    });
		btnAdd.setMaximumSize(new Dimension(50,30));
		
		
		pnlHeader = new JPanel();
		pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.X_AXIS));
		pnlHeader.add(lblHeader);
		pnlHeader.setMaximumSize(new Dimension(300,20));
		pnlHeader.setPreferredSize(new Dimension(300,20));
		
		pnlTitle = new JPanel();
		pnlTitle.setLayout(new BoxLayout(pnlTitle, BoxLayout.X_AXIS));
		pnlTitle.add(lblTitle);
		pnlTitle.add(lblFromAddress);
		pnlTitle.add(lblToAddress);
		pnlTitle.add(lblBitsize);
		pnlTitle.setMaximumSize(new Dimension(300,20));
		pnlTitle.setPreferredSize(new Dimension(300,20));
		
		pnlFields = new JPanel();
		pnlFields.setLayout(new BoxLayout(pnlFields, BoxLayout.X_AXIS));
		pnlFields.add(txtTitle);
		pnlFields.add(txtFromAddress);
		pnlFields.add(txtToAddress);
		pnlFields.add(cmbBitsize);
		pnlFields.add(btnAdd);
		pnlFields.setMaximumSize(new Dimension(300,20));
		pnlFields.setPreferredSize(new Dimension(300,20));
		
		pnlItems = new JPanel();
		pnlItems.setLayout(new BoxLayout(pnlItems, BoxLayout.Y_AXIS));
		
		scrlItems = new JScrollPane(pnlItems);
		scrlItems.setMaximumSize(new Dimension(300,380));
		scrlItems.setPreferredSize(new Dimension(300,380));
		//scrlItems.add(pnlItems);
		
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(pnlHeader);
		this.add(pnlTitle);
		this.add(pnlFields);
		this.add(scrlItems);
	}
	
	public void addWatchItems(String title, String fromAddress, String toAddress, int selectedItem)
	{
		int bitsize = 8;
		switch(selectedItem)
		{
		case 0: bitsize = 8; break;
		case 1: bitsize = 16; break;
		}
		
		int iFromAddress = HEXtoINT(fromAddress);
		
		WatchItem watch = new WatchItem(title, iFromAddress, bitsize);
		watchList.add(watch);
		pnlItems.add(watch);
		
		//Let's calculate if we should add a range of values
		int iToAddress = 0;
		if( toAddress.length() == 0 )
			return;
		
		//calculate how many watch items we need
		iToAddress = HEXtoINT(toAddress);
		int diff = iToAddress - iFromAddress;
		if( diff <= 0 )
			return;
		
		int increment = bitsize / 8;
		for(int i=increment; i<=diff; i+=increment)
		{
			watch = new WatchItem(title+"-"+i, iFromAddress+i, bitsize);
			watchList.add(watch);
			pnlItems.add(watch);
		}
		
		this.invalidate();
	}
	
	public void update()
	{
		for(int i=watchList.size()-1; i>=0;i--)
		{
			WatchItem watch = watchList.get(i);
			if( watch.shouldDelete() )
			{
				if( watchList.size() == 1 )
				{
					pnlItems.removeAll();
					watchList.remove(i);
					continue;
				}
				
				pnlItems.remove(i);
				watchList.remove(i);
				continue;
			}
			
			watch.update();
		}
		
		
		this.revalidate();
		this.repaint();
		
	}
	
	
	public int HEXtoINT(String hex)
	{
		hex = hex.replaceAll("[^A-Fa-f0-9]", "");
		return Integer.parseInt(hex, 16);
	}
}
