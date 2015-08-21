package jario.ai.utils;

public class Vector2D 
{
	public int x;
	public int y;
	
	public Vector2D() {
		x=0;
		y=0;
	}
	
	public Vector2D(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	
	public void Multiply(float scale)
	{
		x=(int)((float)x*scale);
		y=(int)((float)y*scale);
	}
	
	public float SqrLength()
	{
		return x*x + y*y;
	}
	
	public float Length()
	{
		float scale = x*x + y*y;
		return (float)Math.sqrt(scale);
	}
	
	public Vector2D Normalized()
	{
		float len = this.Length();
		return new Vector2D((int)((float)x/len), (int)((float)y/len));
	}
	
	public void Normalize()
	{
		float len = this.Length();
		x=(int)((float)x/len);
		y=(int)((float)y/len);
	}
}
