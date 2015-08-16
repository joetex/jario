package jario.ai.supermetroid;

import jario.ai.utils.BusExtras;
import jario.ai.utils.Vector2D;
import jario.hardware.Bus8bit;
import jario.hardware.Hardware;
import java.util.ArrayList;

public class AIPlayerInfo implements Hardware {
	
	public Bus8bit memorybus;
	
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
		return memorybus.read8bit(0x7E0998);
	}
	
	public int isElevatorTransition()
	{
		return memorybus.read8bit(0x7E0E16);
	}
	
	public int isRoomTransition()
	{
		return memorybus.read8bit(0x7E0797);
	}
	
	public Vector2D roomBlockSize()
	{
		int w = memorybus.read8bit(0x7E07A5);
		int h = memorybus.read8bit(0x7E07A7);
		return new Vector2D(w,h);
	}
	
	public int getRoomID()
	{
		return memorybus.read8bit(0x7E079D);
	}
	
	public int getRegionNumber()
	{
		return memorybus.read8bit(0x7E079F);
	}
	
	public int getRoomEnemyCount()
	{
		return memorybus.read8bit(0x7E0E4E);
	}
	
	public int getRoomEnemiesKilled()
	{
		return memorybus.read8bit(0x7E0E50);
	}
	
	public int getRoomKillsNeeded()
	{
		return memorybus.read8bit(0x7E0E52);
	}
	
	public int getEnergy() 
	{
		return memorybus.read8bit(0x7E09C2);
	}
	
	public int getMissleCount() 
	{
		return memorybus.read8bit(0x7E09C6);
	}
	
	public int getSuperMissleCount() 
	{
		return memorybus.read8bit(0x7E09CA);
	}
	
	public int getPowerBombCount() 
	{
		return memorybus.read8bit(0x7E09CE);
	}
	
	public int getBombCount()
	{
		return memorybus.read8bit(0x7E0CD2);
	}
	
	public int getSelectedItem()
	{
		return memorybus.read8bit(0x7E09D2);
	}
	
	public int getCurrentMode() 
	{
		return memorybus.read8bit(0x7E09A2);
	}
	
	public int getCurrentSuit()
	{
		return memorybus.read8bit(0x7E09A2);
	}
	
	public int getCurrentWeapon() 
	{
		return memorybus.read8bit(0x7E09A6);
	}
	
	public Vector2D getPosition() 
	{
		int x = memorybus.read8bit(0x7E0AF8);
		int y = memorybus.read8bit(0x7E0AFC);
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
