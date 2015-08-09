/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.cpu;

public class PriorityQueue implements java.io.Serializable
{
	public interface Callback extends java.io.Serializable
	{
		public void call(int arg);
	}

	// priority queue implementation using binary min-heap array;
	// does not require normalize() function.
	// O(1) find (tick)
	// O(log n) insert (enqueue)
	// O(log n) remove (dequeue)

	public transient Callback priority_queue_nocallback = new Callback()
	{
		public void call(int arg)
		{
		}
	};

	public void tick(int ticks)
	{
		basecounter += ticks;
		while (heapsize != 0 && gte(basecounter, heap[0].counter))
		{
			callback.call(dequeue());
		}
	}

	// counter is relative to current time (eg enqueue(64, ...) fires in 64
	// ticks);
	// counter cannot exceed std::numeric_limits<uint>::max() >> 1.
	public void enqueue(int counter, int Event)
	{
		int child = heapsize++;
		counter += basecounter;

		while (child != 0)
		{
			int parent = (child - 1) >> 1;
			if (gte(counter, heap[parent].counter))
			{
				break;
			}

			heap[child].counter = heap[parent].counter;
			heap[child].Event = heap[parent].Event;
			child = parent;
		}

		heap[child].counter = counter;
		heap[child].Event = Event;
	}

	public int dequeue()
	{
		int Event = heap[0].Event;
		int parent = 0;
		int counter = heap[--heapsize].counter;

		while (true)
		{
			int child = (parent << 1) + 1;
			if (child >= heapsize)
			{
				break;
			}
			if (child + 1 < heapsize && gte(heap[child].counter, heap[child + 1].counter))
			{
				child++;
			}
			if (gte(heap[child].counter, counter))
			{
				break;
			}

			heap[parent].counter = heap[child].counter;
			heap[parent].Event = heap[child].Event;
			parent = child;
		}

		heap[parent].counter = counter;
		heap[parent].Event = heap[heapsize].Event;
		return Event;
	}

	public void reset()
	{
		basecounter = 0;
		heapsize = 0;
	}

	public PriorityQueue(int size)
	{
		this(size, null);
	}

	public PriorityQueue(int size, Callback callback_)
	{
		if (callback_ == null)
		{
			callback = priority_queue_nocallback;
		}
		else
		{
			callback = callback_;
		}
		heap = new Heap[size];
		for (int i = 0; i < heap.length; i++)
			heap[i] = new Heap();
		reset();
	}

	private Callback callback;
	private int basecounter;
	private int heapsize;

	private class Heap implements java.io.Serializable
	{
		public int counter;
		public int Event;
	}

	Heap[] heap;

	// return true if x is greater than or equal to y
	private boolean gte(int x, int y)
	{
		return x >= y;
	}
}
