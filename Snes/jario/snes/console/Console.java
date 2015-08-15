/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.console;

import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;
import jario.snes.accessories.VideoPlayer;
import jario.snes.configuration.Configuration;
import jario.snes.video.Video;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Console implements Hardware, Clockable, Configurable, java.io.Serializable
{
	transient ExecutorService executor;
	private boolean done;

	transient Hardware bus;
	transient Hardware cpu;
	transient Hardware ppu;
	transient Hardware dsp;
	transient Hardware smp;
	transient Hardware cartridge;
	transient Hardware video;
	transient Hardware audio;
	transient Hardware input;
	
	private boolean enablevideo;
	private boolean freeze = false;

	public enum Region
	{
		NTSC, PAL, Autodetect
	}

	// public enum ExpansionPortDevice : uint { None = 0, BSX = 1 }

	private Region region;
	
	//adding helper method for serialization to save/initialize super class state
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException{
    	
        
    	cpu = (Hardware)ois.readObject();
    	ppu = (Hardware)ois.readObject();
    	bus = (Hardware)ois.readObject();
    	dsp = (Hardware)ois.readObject();
    	smp = (Hardware)ois.readObject();
    	cartridge = (Hardware)ois.readObject();
    	video = (Hardware)ois.readObject();
    	audio = (Hardware)ois.readObject();
    	input = (Hardware)ois.readObject();
        //notice the order of read and write should be same
    	
    	ois.defaultReadObject();
    	
    	executor = Executors.newSingleThreadExecutor();
        
        // connections
 		bus.connect(0, cpu);
 		bus.connect(1, ppu);
 		((Configurable) bus).writeConfig("wram init value", (byte) Configuration.config.cpu.wram_init_value);

 		cpu.connect(0, bus);
 		cpu.connect(1, smp);
 		cpu.connect(2, input);
 		cpu.connect(3, video);
 		cpu.connect(4, ppu);
 		((Configurable) cpu).writeConfig("cpu version", Configuration.config.cpu.version);
 		
 		ppu.connect(0, bus);
 		
 		((Configurable) ppu).writeConfig("ppu1 version", Configuration.config.ppu1.version);
 		((Configurable) ppu).writeConfig("ppu2 version", Configuration.config.ppu2.version);

 		smp.connect(0, dsp);
 		smp.connect(1, cpu);

 		dsp.connect(0, audio);

 		video.connect(1, ppu);
 		video.connect(2, audio); // to simulate multi-out
 		
 		
    }
     
    private void writeObject(ObjectOutputStream oos) throws IOException{
       
    	
    	oos.writeObject(cpu);
    	oos.writeObject(ppu);
    	oos.writeObject(bus);
    	oos.writeObject(dsp);
    	oos.writeObject(smp);
    	oos.writeObject(cartridge);
    	oos.writeObject(video);
    	oos.writeObject(audio);
    	oos.writeObject(input);
    	
    	oos.defaultWriteObject();
    }
    
	public Console()
	{
		region = Region.Autodetect;
		// expansion = ExpansionPortDevice.None;
		executor = Executors.newSingleThreadExecutor();

		// coprocessors init

		try
		{
			File dir = new File("components" + File.separator);
			File file = new File("components.properties");
			ClassLoader loader = this.getClass().getClassLoader();
			Properties prop = new Properties();
			try
			{
				if (dir.exists() && dir.listFiles().length > 0)
				{
					File[] files = dir.listFiles(new FileFilter(){public boolean accept(File f){return f.getPath().toLowerCase().endsWith(".jar");}});
					URL[] urls = new URL[files.length];
					for (int i = 0; i < files.length; i++) urls[i] = files[i].toURI().toURL();
					loader = new URLClassLoader(urls, this.getClass().getClassLoader());
				}
				URL url = file.exists() ? file.toURI().toURL() : loader.getResource("properties" + File.separator + "components.properties");
				if (url != null) prop.load(url.openStream());
			}
			catch (IOException e)
			{
			}

			bus = (Hardware) Class.forName(prop.getProperty("MEMORY", "MEMORY"), true, loader).newInstance();
			cpu = (Hardware) Class.forName(prop.getProperty("CPU", "CPU"), true, loader).newInstance();
			ppu = (Hardware) Class.forName(prop.getProperty("PPU", "PPU"), true, loader).newInstance();
			smp = (Hardware) Class.forName(prop.getProperty("SMP", "SMP"), true, loader).newInstance();
			dsp = (Hardware) Class.forName(prop.getProperty("DSP", "DSP"), true, loader).newInstance();
			video = (Hardware) Class.forName(prop.getProperty("ENC", "ENC"), true, loader).newInstance();
			audio = (Hardware) Class.forName(prop.getProperty("DAC", "DAC"), true, loader).newInstance();
			input = (Hardware) Class.forName(prop.getProperty("INPUT", "INPUT"), true, loader).newInstance();
		}
		catch (InstantiationException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}

		// connections
		bus.connect(0, cpu);
		bus.connect(1, ppu);
		//((Configurable) bus).writeConfig("wram init value", (byte) Configuration.config.cpu.wram_init_value);

		cpu.connect(0, bus);
		cpu.connect(1, smp);
		cpu.connect(2, input);
		cpu.connect(3, video);
		cpu.connect(4, ppu);
		((Configurable) cpu).writeConfig("cpu version", Configuration.config.cpu.version);
		
		ppu.connect(0, bus);
		
		((Configurable) ppu).writeConfig("ppu1 version", Configuration.config.ppu1.version);
		((Configurable) ppu).writeConfig("ppu2 version", Configuration.config.ppu2.version);

		smp.connect(0, dsp);
		smp.connect(1, cpu);

		dsp.connect(0, audio);

		video.connect(1, ppu);
		video.connect(2, audio); // to simulate multi-out
	}

	@Override
	public void connect(int port, Hardware hw)
	{
		switch (port)
		{
		case 0:
			input.connect(0, hw);
			hw.connect(0, bus);
			break;
		case 1:
			input.connect(1, hw);
			break;
		case 2:
			cartridge = hw;
			bus.connect(2, cartridge);
			if (cartridge != null)
			{
				cartridge.connect(0, bus);
				insertCartridge();
			}
			break;
		case 3:
			video.connect(0, hw);
			break;
		case 4:
			audio.connect(0, hw);
			break;
		case 5:
			cartridge = hw;
			bus.connect(3, cartridge);
			if (cartridge != null)
			{
				cartridge.connect(0, bus);
			}
			break;
		}
	}

	public long clockcnt = 0;
	
	@Override
	public void clock(long clocks)
	{
		final Clockable cpu_clk = (Clockable) cpu;
		done = false;
		
		//if( clocks != 1L )
		//	clockcnt = clocks;
		
		executor.execute(new Runnable()
		{
			public void run()
			{
				System.out.println("Console Clock Begin");
				while (!done)
				{
					//if( freeze ) continue;
					
					cpu_clk.clock(50000L);
					((VideoPlayer)((Video) video).output).updateFPS();
					///clockcnt = 50000L;
				}
				System.out.println("Console Clock End");
			}
		});
	}

	@Override
	public void reset()
	{
		done = true;

		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		cpu.reset();
		smp.reset();
		dsp.reset();
		ppu.reset();

		audio.reset();
		video.reset();

		((Clockable) input).clock(1L);
	}

	@Override
	public Object readConfig(String key)
	{
		if (key.equals("enablevideo")) return enablevideo;
		else if (key.equals("enableaudio")) return enablevideo;
		else if (key.equals("freeze")) return freeze;
		else if (key.equals("accuracy")) return ((Configurable) ppu).readConfig("accuracy");
		else if (key.equals("fps")) return ((Configurable) video).readConfig("fps");
		else if (key.equals("frozen")) return ((Configurable) cpu).readConfig("frozen");
		else if (key.equals("memory")) return ((Configurable) bus);
		else if (key.equals("ppucache")) return ((Configurable) ppu).readConfig("cache");

		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("enablevideo")) 
		{
			enablevideo = (Boolean) value;
			((Configurable) cpu).writeConfig("enableppu", value);
			((Configurable) video).writeConfig("enable", value);
		}
		else if (key.equals("done")) done = (Boolean) value;
		else if (key.equals("enableaudio")) {
			((Configurable) dsp).writeConfig(key, value);
			((Configurable) cpu).writeConfig(key, value);
		}
		else if (key.equals("accuracy")) ((Configurable) ppu).writeConfig("accuracy", value);
		else if (key.equals("fps")) ((Configurable) video).writeConfig("fps", value);
		else if (key.equals("ppucache")) ((Configurable) ppu).writeConfig("cache", value);

		
		else if(key.equals("save")) {
			ObjectOutputStream out = (ObjectOutputStream)value;
			
			try
			{
				out.writeObject(region);
				
				((Configurable) cartridge).writeConfig("savestate", value);
				
				((Configurable) bus).writeConfig(key, value);
				((Configurable) ppu).writeConfig(key, value);
				((Configurable) cpu).writeConfig(key, value);
				
				((Configurable) dsp).writeConfig(key, value);
				
				//((Configurable) input).writeConfig(key, value);
			}
			catch(Exception e) {}
		}
		
		else if(key.equals("load")) {
			ObjectInputStream in = (ObjectInputStream)value;
			
			try
			{
				region = (Region)in.readObject();
				
				((Configurable) cartridge).writeConfig("loadstate", value);
				
				((Configurable) bus).writeConfig(key, value);
				
				((Configurable) ppu).writeConfig(key, value);
				((Configurable) cpu).writeConfig(key, value);
				
				((Configurable) dsp).writeConfig(key, value);
				
				//((Configurable) input).writeConfig(key, value);
			}
			catch(Exception e) {}
			
			reconnect();
		}
	}
	
	public void reconnect()
	{
		// connections
		bus.connect(0, cpu);
		bus.connect(1, ppu);
		//((Configurable) bus).writeConfig("wram init value", (byte) Configuration.config.cpu.wram_init_value);

		cpu.connect(0, bus);
		cpu.connect(1, smp);
		cpu.connect(2, input);
		cpu.connect(3, video);
		cpu.connect(4, ppu);
		((Configurable) cpu).writeConfig("cpu version", Configuration.config.cpu.version);
		
		ppu.connect(0, bus);
		
		((Configurable) ppu).writeConfig("ppu1 version", Configuration.config.ppu1.version);
		((Configurable) ppu).writeConfig("ppu2 version", Configuration.config.ppu2.version);

		smp.connect(0, dsp);
		smp.connect(1, cpu);

		dsp.connect(0, audio);

		video.connect(1, ppu);
		video.connect(2, audio); // to simulate multi-out
	}

	private void insertCartridge()
	{
		reset();
		
		region = Configuration.config.region;
		// expansion = Configuration.config.expansion_port;
		if (region == Region.Autodetect)
		{
			region = (((Configurable) cartridge).readConfig("region").equals("ntsc") ? Region.NTSC : Region.PAL);
		}

		((Configurable) smp).writeConfig("region", region == Region.NTSC ? "ntsc" : "pal");
		((Configurable) ppu).writeConfig("region", region == Region.NTSC ? "ntsc" : "pal");
//		((Configurable) counter_cpu).writeConfig("region", region == Region.NTSC ? "ntsc" : "pal");
		((Configurable) cpu).writeConfig("region", region == Region.NTSC ? "ntsc" : "pal");

		// Audio.audio.coprocessor_enable(false);

		// coprocessors enable

		// add cpu coprocessors
		cpu.connect(5, cartridge);

		((Clockable) input).clock(1L);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		//bus.destroy();
		//cpu.destroy();
		//ppu.destroy();
		//dsp.destroy();
		//smp.destroy();
		//video.destroy();
		//audio.destroy();
	}
}
