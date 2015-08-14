package jario.ai.supermetroid;

import jario.ai.utils.Vector2D;
import jario.hardware.Bus8bit;
import jario.hardware.Hardware;

public class AIPlayerInfo implements Hardware {
	
	public Bus8bit memorybus;
	
	public AIPlayerInfo()
	{
		
	}
	
	public int getHealth() {
		return 0;
	}
	
	public int getEnergy() {
		return 0;
	}
	
	public int getMissleCount() {
		return 0;
	}
	
	public int getSuperMissleCount() {
		return 0;
	}
	
	public int getPowerBombCount() {
		return 0;
	}
	
	public int getBombCount() {
		return 0;
	}
	
	public int getCurrentMode() {
		return 0;
	}
	
	public int getCurrentSuit() {
		return 0;
	}
	
	public int getCurrentWeapon() {
		return 0;
	}
	
	public Vector2D getPosition() {
		return null;
	}
	
	public Vector2D getVelocity() {
		return null;
	}

	@Override
	public void connect(int port, Hardware hw) {
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
