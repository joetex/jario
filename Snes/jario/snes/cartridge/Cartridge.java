/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.cartridge;

import jario.hardware.Bus8bit;
import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;
import jario.snes.memory.StaticRAM;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Cartridge implements Hardware, Clockable, Configurable, java.io.Serializable
{
	public enum Mode
	{
		Normal, BsxSlotted, Bsx, SufamiTurbo, SuperGameBoy
	}

	public enum Region
	{
		NTSC, PAL
	}
	
	public enum SuperGameBoyVersion
	{
	    Version1,
	    Version2
	};

	public enum MapMode
	{
		Direct, Linear, Shadow
	}

	// assigned externally to point to file-system datafiles (msu1 and serial)
	// example: "/path/to/filename.sfc" would set this to "/path/to/filename"
	public String basename;
	String cartridgeName;
	
	private boolean loaded;
	
	Mode mode;
	Region region;
	int ram_size;
	int spc7110_data_rom_offset;
	SuperGameBoyVersion supergameboy_version;
	int supergameboy_ram_size;
	int supergameboy_rtc_size;

	boolean has_bsx_slot;
	boolean has_superfx;
	boolean has_sa1;
	boolean has_upd77c25;
	boolean has_srtc;
	boolean has_sdd1;
	boolean has_spc7110;
	boolean has_spc7110rtc;
	boolean has_cx4;
	boolean has_obc1;
	boolean has_st0010;
	boolean has_st0011;
	boolean has_st0018;
	boolean has_msu1;
	boolean has_serial;
	
	Collection<Mapping> mapping = new ArrayList<Mapping>();
	MappedRAM cartrom;
	MappedRAM cartram;
	
	static Bus8bit bus;
	
	XmlParser xml;
	List<Hardware> chips = new ArrayList<Hardware>();
	List<Clockable> coprocessors = new ArrayList<Clockable>();

	public Cartridge()
	{
		xml = new XmlParser(this);
		cartrom = new MappedRAM();
		cartram = new MappedRAM();
		
		loaded = false;
		reset();
	}

	@Override
	public void connect(int port, Hardware hw)
	{
		switch (port)
		{
		case 0:
			bus = (Bus8bit) hw;
			for (Hardware chip : chips)
			{
				chip.connect(0, hw);
				if (chip instanceof Clockable)
				{
					coprocessors.add((Clockable)chip);
				}
			}
			break;
		
		case 1:
			bus = (Bus8bit) hw;
			for (Hardware chip : chips)
			{
				chip.connect(0, hw);
				
			}
			break;
		}
	}

	@Override
	public void reset()
	{
		//save();
		unload();
	}

	@Override
	public Object readConfig(String key)
	{
		if (key.equals("region")) return region.name().toLowerCase();
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("romfile"))
		{
			cartridgeName = value.toString();
			loadDataFromRomFile();
		}
		else if(key.equals("save"))
		{
			save();
		}
		else if(key.equals("load"))
		{
			load();
		}
		else if (key.equals("mapmem"))
		{
			Configurable bus_mmio = (Configurable)((Configurable)value).readConfig("mmio");
			for (Mapping m : mapping)
			{
				if (m.memory != null)
				{
					((Configurable)value).writeConfig("map", new Object[]{m.mode.ordinal(), m.banklo&0xFF, m.bankhi&0xFF, m.addrlo&0xFFFF, m.addrhi&0xFFFF, m.memory, m.offset, m.size});
				}
				else if (m.mmio != null)
				{
					for (int i = m.addrlo; i <= m.addrhi; i++)
					{
						bus_mmio.writeConfig(Integer.toHexString(i), m.mmio);
					}
				}
			}
		}
	
		
		else if(key.equals("savestate")) {
			ObjectOutputStream out = (ObjectOutputStream)value;
			
			try
			{
				//out.writeObject(cartrom);
				//out.writeObject(cartram);
			}
			catch(Exception e) {}
		}
		
		else if(key.equals("loadstate")) {
			ObjectInputStream in = (ObjectInputStream)value;
			
			try
			{
				//cartrom = (MappedRAM)in.readObject();
				//cartram = (MappedRAM)in.readObject();
			}
			catch(Exception e) {}
			
		}
	}
	
	@Override
	public void clock(long clocks)
	{
		for (int i = 0; i < coprocessors.size(); i++)
		{
			coprocessors.get(i).clock(clocks);
		}
	}

	private void load(Mode cartridge_mode, String[] xml_list)
	{
		region = Region.NTSC;
		ram_size = 0;

		xml.parse_xml(xml_list);

		if (ram_size > 0)
		{
			byte[] repeat = new byte[ram_size];
			Arrays.fill(repeat, (byte) 0xff);
			cartram.map(repeat, ram_size);
		}

		cartrom.write_protect(true);
		cartram.write_protect(false);
		
		for (Hardware chip : chips)
		{
			chip.connect(2, cartram);
		}

		loaded = true;
	}

	private void unload()
	{
		cartridgeName = null;
		cartrom.reset();
		cartram.reset();

		if (loaded == false) { return; }
		loaded = false;
	}

	private void loadNormal(byte[] rom_data)
	{
		if (rom_data != null)
		{
			cartrom.copy(rom_data, rom_data.length);
		}
		String xmlrom = new SnesInformation(rom_data, rom_data.length).xml_memory_map;
		System.out.println(xmlrom);
		load(Cartridge.Mode.Normal, new String[] { xmlrom });
	}

	private void loadDataFromRomFile()
	{
		try
		{
			RandomAccessFile fs = new RandomAccessFile(new File(cartridgeName), "r");
			byte[] rom = new byte[(int) fs.length()];
			if (rom.length % 1024 != 0)
			{
				fs.skipBytes(0x200);
			}
			fs.read(rom);
			fs.close();
			loadNormal(rom);
			File save = new File("save" + File.separator + cartridgeName.substring(cartridgeName.lastIndexOf(File.separator) + 1, cartridgeName.lastIndexOf(".")) + ".save");
			if (save.exists())
			{
				fs = new RandomAccessFile(save, "r");
				byte[] ram = new byte[(int) fs.length()];
				fs.read(ram);
				fs.close();
				cartram.copy(ram, ram.length);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void load()
	{
		File save = new File("save" + File.separator + cartridgeName.substring(cartridgeName.lastIndexOf(File.separator) + 1, cartridgeName.lastIndexOf(".")) + ".save");
		if (save.exists())
		{
			try
			{
				RandomAccessFile fs = new RandomAccessFile(save, "r");
				byte[] ram = new byte[(int) fs.length()];
				fs.read(ram);
				fs.close();
				cartram.copy(ram, ram.length);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void save()
	{
		if (cartridgeName != null)
		{
			File save = new File("save" + File.separator + cartridgeName.substring(cartridgeName.lastIndexOf(File.separator) + 1, cartridgeName.lastIndexOf(".")) + ".save");
			try
			{
				RandomAccessFile fs = new RandomAccessFile(save, "rw");
				byte[] ram = cartram.data();
				if (ram != null) fs.write(ram);
				fs.close();
				System.out.println("saved: " + save.getAbsolutePath());
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
	
}
