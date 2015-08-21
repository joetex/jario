package jario.ai.supermetroid;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jario.ai.utils.BusExtras;
import jario.ai.utils.Vector2D;

/*
 * Draw your AI information here for debugging
 * 
 */
public class AIWindow 
{
	JFrame window;
	Graphics graphics;
	AIPlayerInfo playerinfo;
	
	BufferedImage[] imageBlocks = new BufferedImage[16];
	BufferedImage[] imageSlopes = new BufferedImage[128];
	
	JLabel lblPosition;
	
	BufferedImage worldImage = null;
	
	JTextField txtMemoryTitle;
	JTextField txtMemoryAddress;
	
	JTextField txtEditMemoryAddress;
	JTextField txtEditMemoryValue;
	
	JButton btnViewMemory;
	JButton btnEditMemory;
	
	List<JLabel> lstMemoryTitles = new ArrayList<JLabel>();
	List<JLabel> lstMemoryAddress = new ArrayList<JLabel>();
	List<JLabel> lstMemoryValue = new ArrayList<JLabel>();
	List<JButton> lstButtonDelete = new ArrayList<JButton>();
	
	
	
	JPanel pnlEdit;
	JPanel pnlView;
	JPanel pnlAction;
	
	
	public AIWindow(JFrame frame, AIPlayerInfo ai)
	{
		window = frame;
		graphics = (Graphics2D) window.getContentPane().getComponent(0).getGraphics();
		playerinfo = ai;
		
		//lblPosition = new JLabel("Position:", JLabel.CENTER);
		
		//window.add(lblPosition);
		
		loadBlocks();
		loadSlopes();
	}
	
	
	public void drawRoom()
	{
		if( playerinfo.roomInfo == null )
			return;
		
		window.repaint();
		
		int tilesize = 16;
		
		int width = playerinfo.roomInfo.roomWidth * tilesize;
		int height = playerinfo.roomInfo.roomHeight * tilesize;
		
		if( width > 0 && height > 0 && worldImage == null )
		{
			loadRoomImage();
		}
		
		Vector2D pos = playerinfo.getPosition();
		Vector2D psize = playerinfo.getSize();
		
		
		int left = pos.x - 256;
		int top = pos.y - 256;
		int right = pos.x + 256;
		int bottom = pos.y + 256;
		
		int centerX = (pos.x - left);
		int centerY = (pos.y - top);
		
		if( left < 0 )
		{
			right -= left;
			centerX += left;
			left = 0;
		}
		if( top < 0 ){
			bottom -= top;
			centerY += top;
			top = 0;
		}
		if( bottom > height ){
			top -= (bottom-height);
			centerY += (bottom-height);
			bottom = height;
		}
		if( right > width ){
			left -= (right-width);
			centerX += (right-width);
			right = width;
		}
		
		graphics.drawImage(worldImage, 0, 0, 512, 512, left, top, right, bottom, null);
		
		
		
		graphics.fillRect(centerX-psize.x, centerY-psize.y, psize.x*2, psize.y*2);
		graphics.setColor(new Color(255,0,0));   
	}
	
	public void loadRoomImage()
	{
		int width = playerinfo.roomInfo.roomWidth;
		int height = playerinfo.roomInfo.roomHeight;
		int tilesize = 16;
		worldImage = new BufferedImage(width*tilesize, height*tilesize, BufferedImage.TYPE_INT_RGB);
		
		Graphics g = worldImage.getGraphics();
		BufferedImage image;
		
		for(int x=0; x<width; x++)
		{
			for(int y=0; y<height; y++)
			{
				TileInfo tile = playerinfo.roomInfo.tilemap[x][y];
				if( tile.ttype == TileInfo.BlockType.Slope )
				{
					image = imageSlopes[ (tile.torientation.ordinal()*32) + (tile.bts % 32) ];
				}
				else
				{
					if( tile.ttype == TileInfo.BlockType.Vertical )
					{
						int offsetX = x;
						int offsetY = y;
						image = imageBlocks[0];
					}
					else if( tile.ttype == TileInfo.BlockType.Horizonal )
					{
						image = imageBlocks[0];
					}
					else
						image = imageBlocks[tile.ttype.ordinal()];
				}
			
				g.drawImage(image, x*tilesize, y*tilesize, tilesize, tilesize, null);
			}
		}
	}
	
	public void loadBlocks()
	{
		try
		{
			for(int i=0; i<imageBlocks.length; i++)
			{
				String filename = "/resources/ai/supermetroid/blocks/" + Integer.toString(i, 16) + ".png";
				System.out.println("Loading " + filename);
				imageBlocks[i] = ImageIO.read( getClass().getResource(filename) );
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void loadSlopes()
	{
		try
		{
			int cnt = 0;
			for(int j=0; j<4; j++)
			for(int i=0; i<imageSlopes.length/4; i++)
			{
				String filename = "/resources/ai/supermetroid/slopes/" + Integer.toString(i, 16) + ".png";
				System.out.println("Loading " + filename);
				imageSlopes[cnt] = ImageIO.read( getClass().getResource(filename) );
				switch(j)
				{
				case 0:
					
					break;
				case 1:
					// Flip the image horizontally
					AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
					tx.translate(-imageSlopes[cnt].getWidth(null), 0);
					AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
					imageSlopes[cnt] = op.filter(imageSlopes[cnt], null);
					break;
				case 2:
					// Flip the image vertically
					AffineTransform tx2 = AffineTransform.getScaleInstance(1, -1);
					tx2.translate(0, -imageSlopes[cnt].getHeight(null));
					AffineTransformOp op2 = new AffineTransformOp(tx2, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
					imageSlopes[cnt] = op2.filter(imageSlopes[cnt], null);
					break;
				case 3:
					// Flip the image vertically and horizontally; equivalent to rotating the image 180 degrees
					AffineTransform tx3 = AffineTransform.getScaleInstance(-1, -1);
					tx3.translate(-imageSlopes[cnt].getWidth(null), -imageSlopes[cnt].getHeight(null));
					AffineTransformOp op3 = new AffineTransformOp(tx3, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
					imageSlopes[cnt] = op3.filter(imageSlopes[cnt], null);
					break;
				}
				cnt++;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void update()
	{
		//updateWatchMemory();
		//bufferedImage.
		//Vector2D pos = playerinfo.getPosition();
		//lblPosition.setText("Position : (" + pos.x + ", " + pos.y + ")");
		
		//drawRoom();
	}
}
