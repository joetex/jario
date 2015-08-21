package jario.ai.supermetroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import jario.ai.utils.BusExtras;

public class RoomInfo 
{
	public TileInfo[][] tilemap;
	public DoorInfo[] doorList;
	public EnemyInfo[] enemyList;
	
	int roomWidth = 0;
	int roomHeight = 0;
	int roomAddress = 0;
	
	public class DoorInfo
	{
		ArrayList<TileInfo> tiles = new ArrayList<TileInfo>();
		int nextRoomAddress = 0;
		int doorType = 0;
		int doorNum = 0;
	}
	
	public void loadRoom()
	{
		roomWidth = getRoomBlockWidth();
		roomHeight = getRoomBlockHeight();
		
		tilemap = TileInfo.getTileMap2D(roomWidth, roomHeight);
		
		loadEnemies();
		loadDoors();
	}
	
	//7F:0000 - 7F:0001    Size of room tilemap in bytes (and size of background tilemap, and 2x size of bts map)
	public static int getRoomSize()
	{
		return BusExtras.read16bit(AIController.memorybus, 0x7F0000);
	}
	
	//7E:07A5 - 7E:07A6    Current room's width in blocks
	public static int getRoomBlockWidth()
	{
		return BusExtras.read16bit(AIController.memorybus, 0x7E07A5);
	}
	
	//7E:07A7 - 7E:07A8    Current room's height in blocks
	public static int getRoomBlockHeight()
	{
		return BusExtras.read16bit(AIController.memorybus, 0x7E07A7);
	}
		
	public int getEnemyCount()
	{
		return BusExtras.read16bit(AIController.memorybus, 0x7E0E4E);
	}
	
	public int getEnemiesKilled()
	{
		return BusExtras.read16bit(AIController.memorybus, 0x7E0E50);
	}
	
	public int getKillsNeeded()
	{
		return BusExtras.read16bit(AIController.memorybus, 0x7E0E52);
	}
	
	public void loadEnemies()
	{
		int enemyCount = getEnemyCount();
		
		//Address where Enemy Data list starts
		int startAddress = 0x7E0F78;
		
		//Each enemy data is 0x3F bytes in size
		enemyList = new EnemyInfo[enemyCount];
		for(int i=0; i<enemyCount; i++)
		{
			int address = startAddress + (i*0x3F);
			EnemyInfo enemy = new EnemyInfo();
			enemy.loadEnemyInfo(address);
			enemyList[i] = enemy;
		}
	}
	
	
	// [10:39] <Smiley> 7E:07B5 - 7E:07B6    Room's doorout pointer
	// [10:39] <Smiley> That is a pointer to pointers in bank $8F
	// [10:39] <Smiley> And each of those pointers points to actual door data in $83
	// 7E:07B5 - 7E:07B6    Room's doorout pointer
	public void loadDoors()
	{
		Map<Integer, DoorInfo> doors = new HashMap<Integer, DoorInfo>();
		
		//Find each door tile and create a DoorInfo for each group of tiles
		//We scan from top down, so the tiles will always be ordered from top tile to bottom tile of each door,
		// or from left to right for each door that is oriented facing up or down.
		int width = tilemap.length;
		for(int x=0; x<width; x++)
		{
			int height = tilemap[x].length;
			for(int y=0; y<height; y++)
			{
				TileInfo tile = tilemap[x][y];
				
				if( tile.ttype == TileInfo.BlockType.Door )
				{
					if( !doors.containsKey(tile.bts) )
					{
						DoorInfo door = new DoorInfo();
						door.tiles.add(tile);
						door.doorNum = tile.bts;
						doors.put(tile.bts, door);
					}
					else
					{
						DoorInfo door = doors.get(tile.bts);
						door.tiles.add(tile);
						door.doorNum = tile.bts;
					}
				}
			}
		}
		
		//Move from our map to an array list
		ArrayList<DoorInfo> list = new ArrayList<DoorInfo>();
		for (Map.Entry<Integer, DoorInfo> e : doors.entrySet())
		{
			list.add(e.getValue());
		}
		
		//sort the doors with lowest numbered door first 
		Collections.sort(list, new Comparator<DoorInfo>() 
		{
		    public int compare(DoorInfo obj1, DoorInfo obj2) {
		        return obj1.doorNum - obj2.doorNum;
		    }
		});
		 
		//output arraylist to a regular array
		doorList = new DoorInfo[list.size()];
		for(int i=0; i<list.size(); i++)
		{
			doorList[i] = list.get(i);
		}
		
		//Find the door data in the ROM
		//We start with a pointer to the "pointers", which is the size of our door count
		//Then, each pointer goes to a set of 12 bytes of information
		//First 2 bytes read are the next room address
		int doorPointer = BusExtras.read16bit(AIController.memorybus, 0x7E07B5) & 0xFFFF;
		for(int i=0; i<doorList.length; i++)
		{
			int startDoorPointer = BusExtras.read16bit(AIController.memorybus, 0x8F0000 + doorPointer+(i*2));
			int nextRoom = BusExtras.read16bit(AIController.memorybus, 0x830000 + startDoorPointer);
			doorList[i].nextRoomAddress = nextRoom;
		}
	}
}
