package jario.ai.supermetroid;

import jario.ai.utils.BusExtras;
import jario.ai.utils.Vector2D;

public class EnemyInfo {
	
	public int id;
	public int startAddress;
	public Vector2D position = new Vector2D();
	public Vector2D subposition = new Vector2D();
	
	public Vector2D boxsize = new Vector2D();
	public int health;
	
	public void loadEnemyInfo(int address)
	{
		startAddress = address;
		id = BusExtras.read16bit(AIController.memorybus, address);
		
		position.x = BusExtras.read16bit(AIController.memorybus, address+2);
		position.y = BusExtras.read16bit(AIController.memorybus, address+6);
		
		subposition.x = BusExtras.read16bit(AIController.memorybus, address+4);
		subposition.y = BusExtras.read16bit(AIController.memorybus, address+8);
		
		boxsize.x = BusExtras.read16bit(AIController.memorybus, address+10);
		boxsize.y = BusExtras.read16bit(AIController.memorybus, address+12);
		
		health = BusExtras.read16bit(AIController.memorybus, address+20);
	}
	
	public void updateHealth()
	{
		health = BusExtras.read16bit(AIController.memorybus, startAddress+20);
	}
	
	public void updateBoxSize()
	{
		boxsize.x = BusExtras.read16bit(AIController.memorybus, startAddress+10);
		boxsize.y = BusExtras.read16bit(AIController.memorybus, startAddress+12);
	}
	public void updatePosition()
	{
		position.x = BusExtras.read16bit(AIController.memorybus, startAddress+2);
		position.y = BusExtras.read16bit(AIController.memorybus, startAddress+6);
		
		subposition.x = BusExtras.read16bit(AIController.memorybus, startAddress+4);
		subposition.y = BusExtras.read16bit(AIController.memorybus, startAddress+8);
	}
}
