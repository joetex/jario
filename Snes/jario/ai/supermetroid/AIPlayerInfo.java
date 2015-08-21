package jario.ai.supermetroid;

import jario.ai.utils.BusExtras;
import jario.ai.utils.Vector2D;
import jario.hardware.Bus8bit;
import jario.hardware.Hardware;
import java.util.ArrayList;

public class AIPlayerInfo implements Hardware {
	
	public Bus8bit memorybus;
	
	RoomInfo roomInfo = null;
	
	public AIPlayerInfo()
	{
		
	}
	
	public int getRoomTime()
	{
		return 0;
	}
	
	/*
	 * Game state. 01 = Title screen, 04 = menus, 05 = Load area, 
	 * 06 = Loading game, 07 = Samus being electrified in save capsule, 
	 * 08 = normal gameplay, 0C = pausing, 0F = paused, 
	 * 12 = unpausing, 15, 17, 18, 19, 1A = Dead or dying, 
	 * 23 = Timer up. 24 = blackout and gameover (Ceres explodes if not escaping Zebes). 
	 * 2A = intro demos
	 */
	public int gameState()
	{
		return BusExtras.read16bit(memorybus,0x7E0998);
	}
	
	public int isElevatorTransition()
	{
		return BusExtras.read16bit(memorybus,0x7E0E16);
	}
	
	public int isRoomTransition()
	{
		return BusExtras.read16bit(memorybus,0x7E0797);
	}
	
	public Vector2D roomBlockSize()
	{
		int w = memorybus.read8bit(0x7E07A5);
		int h = memorybus.read8bit(0x7E07A7);
		return new Vector2D(w,h);
	}
	
	public int getRoomID()
	{
		return BusExtras.read16bit(memorybus,0x7E079D);
	}
	
	public int getRegionNumber()
	{
		return BusExtras.read16bit(memorybus,0x7E079F);
	}
	
	
	
	public int getEnergy() 
	{
		return BusExtras.read16bit(memorybus,0x7E09C2);
	}
	
	public int getMissleCount() 
	{
		return BusExtras.read16bit(memorybus,0x7E09C6);
	}
	
	public int getSuperMissleCount() 
	{
		return BusExtras.read16bit(memorybus,0x7E09CA);
	}
	
	public int getPowerBombCount() 
	{
		return BusExtras.read16bit(memorybus,0x7E09CE);
	}
	
	public int getBombCount()
	{
		return BusExtras.read16bit(memorybus,0x7E0CD2);
	}
	
	public int getSelectedItem()
	{
		return BusExtras.read16bit(memorybus,0x7E09D2);
	}
	
	public int getCurrentMode() 
	{
		return BusExtras.read16bit(memorybus,0x7E09A2);
	}
	
	public int getCurrentSuit()
	{
		return BusExtras.read16bit(memorybus,0x7E09A2);
	}
	
	public int getCurrentWeapon() 
	{
		return BusExtras.read16bit(memorybus,0x7E09A6);
	}
	
	
	//7E:0DC4 - 7E:0DC5    Current block index (nth block of the room)
	public int getCurrentBlockIndex()
	{
		return BusExtras.read16bit(memorybus,0x7E0DC4);
	}
	
	//7E:0DC6 - 7E:0DC7    
	//Unknown, something to do with collisions (direction?) 
	//0 = jumping up?, 1 = ground, 2 = moving down, 4 = hit head, 5 = walljump. Top byte is related but used seperately
	public int getMovementCollisionType()
	{
		return BusExtras.read16bit(memorybus,0x7E0DC6);
	}
	
	public Vector2D getPosition() 
	{
		int x = BusExtras.read16bit(memorybus, 0x7E0AF6);
		int y = BusExtras.read16bit(memorybus, 0x7E0AFA);
		return new Vector2D(x,y);
	}
	
	//7E:0AFE - 7E:0AFF    Samus's X radius
	//7E:0B00 - 7E:0B01    Samus's Y radius
	public Vector2D getSize() 
	{
		int x = BusExtras.read16bit(memorybus, 0x7E0AFE);
		int y = BusExtras.read16bit(memorybus, 0x7E0B00);
		return new Vector2D(x,y);
	}
	
	//Position/State?
	public int getState()
	{
		return memorybus.read8bit(0x7E0A1C);
	}
	
	public int getMovementType()
	{
		int movementtype = memorybus.read8bit(0x7E0A1F);
		return (movementtype & 0xFF);
	}
	
	public Vector2D getVelocity() 
	{
		int x = memorybus.read8bit(0x7E0B44);
		int y = memorybus.read8bit(0x7E0B2C);
		return new Vector2D(x,y);
	}
	
	
	//X = horizontal momentum
	//Y = vertical acceleration
	public Vector2D getMomentum()
	{
		int x = memorybus.read8bit(0x7E0B48);
		int y = memorybus.read8bit(0x7E0B32);
		return new Vector2D(x,y);
	}
	
	
	
	
	public void loadRoom()
	{
		roomInfo = new RoomInfo();
		roomInfo.loadRoom();
		
	}
	
	
	@Override
	public void connect(int port, Hardware hw) 
	{
		// TODO Auto-generated method stub
		switch(port)
		{
		case 0:
			memorybus = (Bus8bit)hw;
			break;
		case 1:
			break;
		}
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
}
