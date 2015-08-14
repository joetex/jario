package jario.ai.utils;

public class Vector2D 
{
	public float x;
	public float y;
	
	public Vector2D() {
		x=0;
		y=0;
	}
	
	public Vector2D(float x, float y)
	{
		this.x = x;
		this.y = y;
	}
	
	public void Multiply(float scale)
	{
		x*=scale;
		y*=scale;
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
		return new Vector2D(x/len, y/len);
	}
	
	public void Normalize()
	{
		float len = this.Length();
		x/=len;
		y/=len;
	}
}
