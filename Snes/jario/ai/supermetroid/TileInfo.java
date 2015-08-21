package jario.ai.supermetroid;

import java.util.ArrayList;

import jario.ai.utils.BusExtras;
import jario.ai.utils.Vector2D;

public class TileInfo 
{
	public BlockType ttype;
	public Orientation torientation;
	public int bts;
	public Vector2D pos;
	
	public enum BlockType
	{
		AirXRay,		Slope,		AirFoolXRay,		Treadmill,
		ShootableAir,	Horizonal,	AirWhat,			BombableAir,
		SolidBlock,		Door,		SpikeBlock,			CrumbleBlock,
		ShotBlock,		Vertical,	GrappleBlock,		BombBlock
	}
	private static BlockType[] blockvalues = BlockType.values();
	
	//(2 bits in format: YX) for orientation
	//00 = no flip
	//01 = flip horizontal
	//10 = flip vertical
	//11 = flip horizontal and vertical
	public enum Orientation { RightDown,	LeftDown,	RightUp,	LeftUp }
	private static Orientation[] orientationvalues = Orientation.values();
	
	public void setType(int typeid)
	{
		ttype = blockvalues[typeid];
	}
	
	public void setOrientation(int oid)
	{
		torientation = orientationvalues[oid];
	}
	
	public void setBTS(int btsid)
	{
		bts = btsid;
	}
	
	
	//tiledata format (16 bits):
	// TTTTYXZZ ZZZZZZZZ
	// T = type
	// Y = vertical flip
	// X = horizontal flip
	// Z = tile index (we don't need the graphics, so ignore)
	public static ArrayList getRoomTileMap(int roomsize)
	{
		ArrayList<TileInfo> tilemap = new ArrayList<TileInfo>();
		TileInfo binfo;
		
		for(int i=0; i<roomsize; i++)
		{
			//7F:0002 - 7F:6401		Room Tilemap
			int tiledata = BusExtras.read16bit(AIController.memorybus, 0x7F0002 + (i*2)) & 0xFFFF;
			binfo = new TileInfo();
			
			//bits 16, 15, 14, 13 = type
			//(4 bits)
			binfo.setType((tiledata >> 12) & 0xF);
			
			//bits 12, 11 = xy flip
			//(2 bits)
			binfo.setOrientation((tiledata >> 10) & 0x3);
			
			tilemap.add(binfo);
		}
		
		for(int i=0; i<roomsize; i++)
		{
			//7F:6402 - 7F:9601     Room BTS map
			//(8 bits each)
			tilemap.get(i).setBTS((AIController.memorybus.read8bit(0x7F6402 + (i)) & 0xFF));
		}
		return tilemap;
	}
	
	public static TileInfo[][] getTileMap2D(int roomWidth, int roomHeight)
	{
		
		
		TileInfo[][] tiles = new TileInfo[roomWidth][];
		for(int x=0; x<roomWidth; x++)
		{
			tiles[x] = new TileInfo[roomHeight];
		}
		
		ArrayList tilemap = getRoomTileMap(roomWidth*roomHeight);
		
		for(int posid=0; posid<tilemap.size(); posid++)
		{
			int x = posid % roomWidth;
			int y = posid / roomWidth;
			tiles[x][y] = (TileInfo)tilemap.get(posid);
			tiles[x][y].pos = new Vector2D(x,y);
		}
		
		tilemap.clear();
		
		return tiles;
	}
}
