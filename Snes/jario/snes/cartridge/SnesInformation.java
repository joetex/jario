/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.cartridge;

public class SnesInformation implements java.io.Serializable
{
	public String xml_memory_map;

	public SnesInformation(byte[] data, int size)
	{
		read_header(data, size);

		String xml = "<?xml version='1.0' encoding='UTF-8'?>\n";

		 if (type == Type.Bsx)
		 {
		 xml += "<cartridge/>";
		 xml_memory_map = xml;
		 return;
		 }
		
		 if (type == Type.SufamiTurbo)
		 {
		 xml += "<cartridge/>";
		 xml_memory_map = xml;
		 return;
		 }
		
		 if (type == Type.GameBoy)
		 {
		 xml += "<cartridge rtc='" + gameboy_has_rtc(data, size) + "'>\n";
		 if (gameboy_ram_size(data, size) > 0)
		 {
		 xml += "  <ram size='" + Integer.toHexString(gameboy_ram_size(data, size)) +
		 "'/>\n";
		 }
		 xml += "</cartridge>\n";
		 xml_memory_map = xml;
		 return;
		 }

		xml += "<cartridge";
		if (region == Region.NTSC)
		{
			xml += " region='NTSC'";
		}
		else
		{
			xml += " region='PAL'";
		}
		xml += ">\n";

		if(type == Type.SuperGameBoy1Bios) {
		    xml += "  <rom>\n";
		    xml += "    <map mode='linear' address='00-7f:8000-ffff'/>\n";
		    xml += "    <map mode='linear' address='80-ff:8000-ffff'/>\n";
		    xml += "  </rom>\n";
		    xml += "  <supergameboy revision='1'>\n";
		    xml += "    <mmio>\n";
		    xml += "      <map address='00-3f:6000-7fff'/>\n";
		    xml += "      <map address='80-bf:6000-7fff'/>\n";
		    xml += "    </mmio>\n";
		    xml += "  </supergameboy>\n";
		  } else if(type == Type.SuperGameBoy2Bios) {
		    xml += "  <rom>\n";
		    xml += "    <map mode='linear' address='00-7f:8000-ffff'/>\n";
		    xml += "    <map mode='linear' address='80-ff:8000-ffff'/>\n";
		    xml += "  </rom>\n";
		    xml += "  <supergameboy revision='2'>\n";
		    xml += "    <mmio>\n";
		    xml += "      <map address='00-3f:6000-7fff'/>\n";
		    xml += "      <map address='80-bf:6000-7fff'/>\n";
		    xml += "    </mmio>\n";
		    xml += "  </supergameboy>\n";
		  } else if(has_spc7110) {
		    xml += "  <rom>\n";
		    xml += "    <map mode='shadow' address='00-0f:8000-ffff'/>\n";
		    xml += "    <map mode='shadow' address='80-bf:8000-ffff'/>\n";
		    xml += "    <map mode='linear' address='c0-cf:0000-ffff'/>\n";
		    xml += "  </rom>\n";

		    xml += "  <spc7110>\n";
		    xml += "    <mcu>\n";
		    xml += "      <map address='d0-ff:0000-ffff' offset='100000' size='" + Integer.toHexString(size - 0x100000) + "'/>\n";
		    xml += "    </mcu>\n";
		    xml += "    <ram size='" + Integer.toHexString(ram_size) + "'>\n";
		    xml += "      <map mode='linear' address='00:6000-7fff'/>\n";
		    xml += "      <map mode='linear' address='30:6000-7fff'/>\n";
		    xml += "    </ram>\n";
		    xml += "    <mmio>\n";
		    xml += "      <map address='00-3f:4800-483f'/>\n";
		    xml += "      <map address='80-bf:4800-483f'/>\n";
		    xml += "    </mmio>\n";
		    if(has_spc7110rtc) {
		      xml += "    <rtc>\n";
		      xml += "      <map address='00-3f:4840-4842'/>\n";
		      xml += "      <map address='80-bf:4840-4842'/>\n";
		      xml += "    </rtc>\n";
		    }
		    xml += "    <dcu>\n";
		    xml += "      <map address='50:0000-ffff'/>\n";
		    xml += "    </dcu>\n";
		    xml += "  </spc7110>\n";
		  }
		
		  else if (mapper == MemoryMapper.LoROM)
		{
			xml += "  <rom>\n";
			xml += "    <map mode='linear' address='00-7f:8000-ffff'/>\n";
			xml += "    <map mode='linear' address='80-ff:8000-ffff'/>\n";
			xml += "  </rom>\n";

			if (ram_size > 0)
			{
				xml += "  <ram size='" + Integer.toHexString(ram_size) + "'>\n";
				xml += "    <map mode='linear' address='20-3f:6000-7fff'/>\n";
				xml += "    <map mode='linear' address='a0-bf:6000-7fff'/>\n";
				if ((rom_size > 0x200000) || (ram_size > 32 * 1024))
				{
					xml += "    <map mode='linear' address='70-7f:0000-7fff'/>\n";
					xml += "    <map mode='linear' address='f0-ff:0000-7fff'/>\n";
				}
				else
				{
					xml += "    <map mode='linear' address='70-7f:0000-ffff'/>\n";
					xml += "    <map mode='linear' address='f0-ff:0000-ffff'/>\n";
				}
				xml += "  </ram>\n";
			}
		}
		else if (mapper == MemoryMapper.HiROM)
		{
			xml += "  <rom>\n";
			xml += "    <map mode='shadow' address='00-3f:8000-ffff'/>\n";
			xml += "    <map mode='linear' address='40-7f:0000-ffff'/>\n";
			xml += "    <map mode='shadow' address='80-bf:8000-ffff'/>\n";
			xml += "    <map mode='linear' address='c0-ff:0000-ffff'/>\n";
			xml += "  </rom>\n";

			if (ram_size > 0)
			{
				xml += "  <ram size='" + Integer.toHexString(ram_size) + "'>\n";
				xml += "    <map mode='linear' address='20-3f:6000-7fff'/>\n";
				xml += "    <map mode='linear' address='a0-bf:6000-7fff'/>\n";
				if ((rom_size > 0x200000) || (ram_size > 32 * 1024))
				{
					xml += "    <map mode='linear' address='70-7f:0000-7fff'/>\n";
				}
				else
				{
					xml += "    <map mode='linear' address='70-7f:0000-ffff'/>\n";
				}
				xml += "  </ram>\n";
			}
		}
		else if (mapper == MemoryMapper.ExLoROM)
		{
			xml += "  <rom>\n";
			xml += "    <map mode='linear' address='00-3f:8000-ffff'/>\n";
			xml += "    <map mode='linear' address='40-7f:0000-ffff'/>\n";
			xml += "    <map mode='linear' address='80-bf:8000-ffff'/>\n";
			xml += "  </rom>\n";

			if (ram_size > 0)
			{
				xml += "  <ram size='" + Integer.toHexString(ram_size) + "'>\n";
				xml += "    <map mode='linear' address='20-3f:6000-7fff'/>\n";
				xml += "    <map mode='linear' address='a0-bf:6000-7fff'/>\n";
				xml += "    <map mode='linear' address='70-7f:0000-7fff'/>\n";
				xml += "  </ram>\n";
			}
		}
		else if (mapper == MemoryMapper.ExHiROM)
		{
			xml += "  <rom>\n";
			xml += "    <map mode='shadow' address='00-3f:8000-ffff' offset='400000'/>\n";
			xml += "    <map mode='linear' address='40-7f:0000-ffff' offset='400000'/>\n";
			xml += "    <map mode='shadow' address='80-bf:8000-ffff' offset='000000'/>\n";
			xml += "    <map mode='linear' address='c0-ff:0000-ffff' offset='000000'/>\n";
			xml += "  </rom>\n";

			if (ram_size > 0)
			{
				xml += "  <ram size='" + Integer.toHexString(ram_size) + "'>\n";
				xml += "    <map mode='linear' address='20-3f:6000-7fff'/>\n";
				xml += "    <map mode='linear' address='a0-bf:6000-7fff'/>\n";
				if ((rom_size > 0x200000) || (ram_size > 32 * 1024))
				{
					xml += "    <map mode='linear' address='70-7f:0000-7fff'/>\n";
				}
				else
				{
					xml += "    <map mode='linear' address='70-7f:0000-ffff'/>\n";
				}
				xml += "  </ram>\n";
			}
		}
		
		else if (mapper == MemoryMapper.SuperFXROM)
		{
		    xml += "  <superfx revision='2'>\n";
		    xml += "    <rom>\n";
		    xml += "      <map mode='linear' address='00-3f:8000-ffff'/>\n";
		    xml += "      <map mode='linear' address='40-5f:0000-ffff'/>\n";
		    xml += "      <map mode='linear' address='80-bf:8000-ffff'/>\n";
		    xml += "      <map mode='linear' address='c0-df:0000-ffff'/>\n";
		    xml += "    </rom>\n";
		    xml += "    <ram size='" + Integer.toHexString(ram_size) + "'>\n";
		    xml += "      <map mode='linear' address='00-3f:6000-7fff' size='2000'/>\n";
		    xml += "      <map mode='linear' address='60-7f:0000-ffff'/>\n";
		    xml += "      <map mode='linear' address='80-bf:6000-7fff' size='2000'/>\n";
		    xml += "      <map mode='linear' address='e0-ff:0000-ffff'/>\n";
		    xml += "    </ram>\n";
		    xml += "    <mmio>\n";
		    xml += "      <map address='00-3f:3000-32ff'/>\n";
		    xml += "      <map address='80-bf:3000-32ff'/>\n";
		    xml += "    </mmio>\n";
		    xml += "  </superfx>\n";
		  }
		else if (mapper == MemoryMapper.SA1ROM)
		{
		    xml += "  <sa1>\n";
		    xml += "    <rom>\n";
		    xml += "      <map mode='linear' address='00-3f:8000-ffff'/>\n";
		    xml += "      <map mode='linear' address='80-bf:8000-ffff'/>\n";
		    xml += "      <map mode='linear' address='c0-ff:0000-ffff'/>\n";
		    xml += "    </rom>\n";
		    xml += "    <iram size='800'>\n";
		    xml += "      <map mode='linear' address='00-3f:3000-37ff'/>\n";
		    xml += "      <map mode='linear' address='80-bf:3000-37ff'/>\n";
		    xml += "    </iram>\n";
		    xml += "    <bwram size='" + Integer.toHexString(ram_size) + "'>\n";
		    xml += "      <map mode='linear' address='00-3f:6000-7fff'/>\n";
		    xml += "      <map mode='linear' address='40-4f:0000-ffff'/>\n";
		    xml += "      <map mode='linear' address='80-bf:6000-7fff'/>\n";
		    xml += "    </bwram>\n";
		    xml += "    <mmio>\n";
		    xml += "      <map address='00-3f:2200-23ff'/>\n";
		    xml += "      <map address='80-bf:2200-23ff'/>\n";
		    xml += "    </mmio>\n";
		    xml += "  </sa1>\n";
		  }
		
		else if (mapper == MemoryMapper.BSCLoROM)
		{
			xml += "  <rom>\n";
			xml += "    <map mode='linear' address='00-1f:8000-ffff' offset='000000'/>\n";
			xml += "    <map mode='linear' address='20-3f:8000-ffff' offset='100000'/>\n";
			xml += "    <map mode='linear' address='80-9f:8000-ffff' offset='200000'/>\n";
			xml += "    <map mode='linear' address='a0-bf:8000-ffff' offset='100000'/>\n";
			xml += "  </rom>\n";
			xml += "  <ram size='" + Integer.toHexString(ram_size) + "'>\n";
			xml += "    <map mode='linear' address='70-7f:0000-7fff'/>\n";
			xml += "    <map mode='linear' address='f0-ff:0000-7fff'/>\n";
			xml += "  </ram>\n";
			xml += "  <bsx>\n";
			xml += "    <slot>\n";
			xml += "      <map mode='linear' address='c0-ef:0000-ffff'/>\n";
			xml += "    </slot>\n";
			xml += "  </bsx>\n";
		}
		else if (mapper == MemoryMapper.BSCHiROM)
		{
			xml += "  <rom>\n";
			xml += "    <map mode='shadow' address='00-1f:8000-ffff'/>\n";
			xml += "    <map mode='linear' address='40-5f:0000-ffff'/>\n";
			xml += "    <map mode='shadow' address='80-9f:8000-ffff'/>\n";
			xml += "    <map mode='linear' address='c0-df:0000-ffff'/>\n";
			xml += "  </rom>\n";
			xml += "  <ram size='" + Integer.toHexString(ram_size) + "'>\n";
			xml += "    <map mode='linear' address='20-3f:6000-7fff'/>\n";
			xml += "    <map mode='linear' address='a0-bf:6000-7fff'/>\n";
			xml += "  </ram>\n";
			xml += "  <bsx>\n";
			xml += "    <slot>\n";
			xml += "      <map mode='shadow' address='20-3f:8000-ffff'/>\n";
			xml += "      <map mode='linear' address='60-7f:0000-ffff'/>\n";
			xml += "      <map mode='shadow' address='a0-bf:8000-ffff'/>\n";
			xml += "      <map mode='linear' address='e0-ff:0000-ffff'/>\n";
			xml += "    </slot>\n";
			xml += "  </bsx>\n";
		}
		
		else if (mapper == MemoryMapper.BSXROM)
		{
		    xml += "  <rom>\n";
		    xml += "    <map mode='linear' address='00-3f:8000-ffff'/>\n";
		    xml += "    <map mode='linear' address='80-bf:8000-ffff'/>\n";
		    xml += "  </rom>\n";
		    xml += "  <bsx>\n";
		    xml += "    <mmio>\n";
		    xml += "      <map address='00-3f:5000-5fff'/>\n";
		    xml += "      <map address='80-bf:5000-5fff'/>\n";
		    xml += "    </mmio>\n";
		    xml += "  </bsx>\n";
		  }
		else if (mapper == MemoryMapper.STROM)
		{
		    xml += "  <rom>\n";
		    xml += "    <map mode='linear' address='00-1f:8000-ffff'/>\n";
		    xml += "    <map mode='linear' address='80-9f:8000-ffff'/>\n";
		    xml += "  </rom>\n";
		    xml += "  <sufamiturbo>\n";
		    xml += "    <slot id='A'>\n";
		    xml += "      <rom>\n";
		    xml += "        <map mode='linear' address='20-3f:8000-ffff'/>\n";
		    xml += "        <map mode='linear' address='a0-bf:8000-ffff'/>\n";
		    xml += "      </rom>\n";
		    xml += "      <ram>\n";
		    xml += "        <map mode='linear' address='60-63:8000-ffff'/>\n";
		    xml += "        <map mode='linear' address='e0-e3:8000-ffff'/>\n";
		    xml += "      </ram>\n";
		    xml += "    </slot>\n";
		    xml += "    <slot id='B'>\n";
		    xml += "      <rom>\n";
		    xml += "        <map mode='linear' address='40-5f:8000-ffff'/>\n";
		    xml += "        <map mode='linear' address='c0-df:8000-ffff'/>\n";
		    xml += "      </rom>\n";
		    xml += "      <ram>\n";
		    xml += "        <map mode='linear' address='70-73:8000-ffff'/>\n";
		    xml += "        <map mode='linear' address='f0-f3:8000-ffff'/>\n";
		    xml += "      </ram>\n";
		    xml += "    </slot>\n";
		    xml += "  </sufamiturbo>\n";
		  }

		  if (has_srtc)
		  {
		    xml += "  <srtc>\n";
		    xml += "    <mmio>\n";
		    xml += "      <map address='00-3f:2800-2801'/>\n";
		    xml += "      <map address='80-bf:2800-2801'/>\n";
		    xml += "    </mmio>\n";
		    xml += "  </srtc>\n";
		  }

		  if (has_sdd1)
		  {
		    xml += "  <sdd1>\n";
		    xml += "    <mcu>\n";
		    xml += "      <map address='c0-ff:0000-ffff'/>\n";
		    xml += "    </mcu>\n";
		    xml += "    <mmio>\n";
		    xml += "      <map address='00-3f:4800-4807'/>\n";
		    xml += "      <map address='80-bf:4800-4807'/>\n";
		    xml += "    </mmio>\n";
		    xml += "  </sdd1>\n";
		  }

		  if (has_cx4)
		  {
		    xml += "  <cx4>\n";
		    xml += "    <mmio>\n";
		    xml += "      <map address='00-3f:6000-7fff'/>\n";
		    xml += "      <map address='80-bf:6000-7fff'/>\n";
		    xml += "    </mmio>\n";
		    xml += "  </cx4>\n";
		  }

		  if (has_dsp1)
		  {
		    xml += "  <upd77c25 program='dsp1b.bin' sha256='4d42db0f36faef263d6b93f508e8c1c4ae8fc2605fd35e3390ecc02905cd420c'>\n";
		    if (dsp1_mapper == DSP1MemoryMapper.DSP1LoROM1MB)
		    {
		      xml += "    <dr>\n";
		      xml += "      <map address='20-3f:8000-bfff'/>\n";
		      xml += "      <map address='a0-bf:8000-bfff'/>\n";
		      xml += "    </dr>\n";
		      xml += "    <sr>\n";
		      xml += "      <map address='20-3f:c000-ffff'/>\n";
		      xml += "      <map address='a0-bf:c000-ffff'/>\n";
		      xml += "    </sr>\n";
		    }
		    else if (dsp1_mapper == DSP1MemoryMapper.DSP1LoROM2MB)
		    {
		      xml += "    <dr>\n";
		      xml += "      <map address='60-6f:0000-3fff'/>\n";
		      xml += "      <map address='e0-ef:0000-3fff'/>\n";
		      xml += "    </dr>\n";
		      xml += "    <sr>\n";
		      xml += "      <map address='60-6f:4000-7fff'/>\n";
		      xml += "      <map address='e0-ef:4000-7fff'/>\n";
		      xml += "    </sr>\n";
		    }
		    else if (dsp1_mapper == DSP1MemoryMapper.DSP1HiROM)
		    {
		      xml += "    <dr>\n";
		      xml += "      <map address='00-1f:6000-6fff'/>\n";
		      xml += "      <map address='80-9f:6000-6fff'/>\n";
		      xml += "    </dr>\n";
		      xml += "    <sr>\n";
		      xml += "      <map address='00-1f:7000-7fff'/>\n";
		      xml += "      <map address='80-9f:7000-7fff'/>\n";
		      xml += "    </sr>\n";
		    }
		    xml += "  </upd77c25>\n";
		  }

		  if (has_dsp2)
		  {
		    xml += "  <upd77c25 program='dsp2.bin' sha256='5efbdf96ed0652790855225964f3e90e6a4d466cfa64df25b110933c6cf94ea1'>\n";
		    xml += "    <dr>\n";
		    xml += "      <map address='20-3f:8000-bfff'/>\n";
		    xml += "      <map address='a0-bf:8000-bfff'/>\n";
		    xml += "    </dr>\n";
		    xml += "    <sr>\n";
		    xml += "      <map address='20-3f:c000-ffff'/>\n";
		    xml += "      <map address='a0-bf:c000-ffff'/>\n";
		    xml += "    </sr>\n";
		    xml += "  </upd77c25>\n";
		  }

		  if (has_dsp3)
		  {
		    xml += "  <upd77c25 program='dsp3.bin' sha256='2e635f72e4d4681148bc35429421c9b946e4f407590e74e31b93b8987b63ba90'>\n";
		    xml += "    <dr>\n";
		    xml += "      <map address='20-3f:8000-bfff'/>\n";
		    xml += "      <map address='a0-bf:8000-bfff'/>\n";
		    xml += "    </dr>\n";
		    xml += "    <sr>\n";
		    xml += "      <map address='20-3f:c000-ffff'/>\n";
		    xml += "      <map address='a0-bf:c000-ffff'/>\n";
		    xml += "    </sr>\n";
		    xml += "  </upd77c25>\n";
		  }

		  if (has_dsp4)
		  {
		    xml += "  <upd77c25 program='dsp4.bin' sha256='63ede17322541c191ed1fdf683872554a0a57306496afc43c59de7c01a6e764a'>\n";
		    xml += "    <dr>\n";
		    xml += "      <map address='30-3f:8000-bfff'/>\n";
		    xml += "      <map address='b0-bf:8000-bfff'/>\n";
		    xml += "    </dr>\n";
		    xml += "    <sr>\n";
		    xml += "      <map address='30-3f:c000-ffff'/>\n";
		    xml += "      <map address='b0-bf:c000-ffff'/>\n";
		    xml += "    </sr>\n";
		    xml += "  </upd77c25>\n";
		  }

		  if (has_obc1)
		  {
		    xml += "  <obc1>\n";
		    xml += "    <mmio>\n";
		    xml += "      <map address='00-3f:6000-7fff'/>\n";
		    xml += "      <map address='80-bf:6000-7fff'/>\n";
		    xml += "    </mmio>\n";
		    xml += "  </obc1>\n";
		  }

		  if (has_st010)
		  {
		    xml += "  <setadsp program='ST-0010'>\n";
		    xml += "    <mmio>\n";
		    xml += "      <map address='68-6f:0000-0fff'/>\n";
		    xml += "      <map address='e8-ef:0000-0fff'/>\n";
		    xml += "    </mmio>\n";
		    xml += "  </setadsp>\n";
		  }

		  if (has_st011)
		  {
		    //ST-0011 addresses not verified; chip is unsupported
		    xml += "  <setadsp program='ST-0011'>\n";
		    xml += "    <mmio>\n";
		    xml += "      <map address='68-6f:0000-0fff'/>\n";
		    xml += "      <map address='e8-ef:0000-0fff'/>\n";
		    xml += "    </mmio>\n";
		    xml += "  </setadsp>\n";
		  }

		  if (has_st018)
		  {
		    xml += "  <setarisc program='ST-0018'>\n";
		    xml += "    <mmio>\n";
		    xml += "      <map address='00-3f:3800-38ff'/>\n";
		    xml += "      <map address='80-bf:3800-38ff'/>\n";
		    xml += "    </mmio>\n";
		    xml += "  </setarisc>\n";
		  }

		xml += "</cartridge>\n";
		xml_memory_map = xml;
	}

	private void read_header(byte[] data, int size)
	{
		type        = Type.Unknown;
		  mapper      = MemoryMapper.LoROM;
		  dsp1_mapper = DSP1MemoryMapper.DSP1Unmapped;
		  region      = Region.NTSC;
		  rom_size    = size;
		  ram_size    = 0;

		  has_bsx_slot   = false;
		  has_superfx    = false;
		  has_sa1        = false;
		  has_srtc       = false;
		  has_sdd1       = false;
		  has_spc7110    = false;
		  has_spc7110rtc = false;
		  has_cx4        = false;
		  has_dsp1       = false;
		  has_dsp2       = false;
		  has_dsp3       = false;
		  has_dsp4       = false;
		  has_obc1       = false;
		  has_st010      = false;
		  has_st011      = false;
		  has_st018      = false;

		  //=====================
		  //detect Game Boy carts
		  //=====================

		  if(size >= 0x0140) {
		    if((data[0x0104]&0xFF) == 0xce && (data[0x0105]&0xFF) == 0xed && (data[0x0106]&0xFF) == 0x66 && (data[0x0107]&0xFF) == 0x66
		    && (data[0x0108]&0xFF) == 0xcc && (data[0x0109]&0xFF) == 0x0d && (data[0x010a]&0xFF) == 0x00 && (data[0x010b]&0xFF) == 0x0b) {
		      type = Type.GameBoy;
		      return;
		    }
		  }

		  if(size < 32768) {
		    type = Type.Unknown;
		    return;
		  }

		  int index = find_header(data, size);
		  int mapperid = data[index + HeaderField_Mapper] & 0xFF;
		  int rom_type = data[index + HeaderField_RomType] & 0xFF;
		  int rom_size = data[index + HeaderField_RomSize] & 0xFF;
		  int company  = data[index + HeaderField_Company] & 0xFF;
		  int regionid = data[index + HeaderField_CartRegion] & 0x7f;

		  ram_size = 1024 << (data[index + HeaderField_RamSize] & 7);
		  if(ram_size == 1024) ram_size = 0;  //no RAM present

		  //0, 1, 13 = NTSC; 2 - 12 = PAL
		  region = (regionid <= 1 || regionid >= 13) ? Region.NTSC : Region.PAL;

		  //=======================
		  //detect BS-X flash carts
		  //=======================

		  if((data[index + 0x13]&0xFF) == 0x00 || (data[index + 0x13]&0xFF) == 0xff) {
		    if((data[index + 0x14]&0xFF) == 0x00) {
		      /*uint8_t*/int n15 = (data[index + 0x15]&0xFF);
		      if(n15 == 0x00 || n15 == 0x80 || n15 == 0x84 || n15 == 0x9c || n15 == 0xbc || n15 == 0xfc) {
		        if((data[index + 0x1a]&0xFF) == 0x33 || (data[index + 0x1a]&0xFF) == 0xff) {
		          type = Type.Bsx;
		          mapper = MemoryMapper.BSXROM;
		          region = Region.NTSC;  //BS-X only released in Japan
		          return;
		        }
		      }
		    }
		  }

		  //=========================
		  //detect Sufami Turbo carts
		  //=========================

		  if(new String(data, 0, 14).equals("BANDAI SFC-ADX")) {
		    if(new String(data, 16, 14).equals("SFC-ADX BACKUP")) {
		      type = Type.SufamiTurboBios;
		    } else {
		      type = Type.SufamiTurbo;
		    }
		    mapper = MemoryMapper.STROM;
		    region = Region.NTSC;  //Sufami Turbo only released in Japan
		    return;         //RAM size handled outside this routine
		  }

		  //==========================
		  //detect Super Game Boy BIOS
		  //==========================

		  if(new String(data, index, 14).equals("Super GAMEBOY2")) {
		    type = Type.SuperGameBoy2Bios;
		    return;
		  }

		  if(new String(data, index, 13).equals("Super GAMEBOY")) {
		    type = Type.SuperGameBoy1Bios;
		    return;
		  }

		  //=====================
		  //detect standard carts
		  //=====================

		  //detect presence of BS-X flash cartridge connector (reads extended header information)
		  if((data[index - 14] & 0xFF) == 'Z') {
		    if((data[index - 11] & 0xFF) == 'J') {
		      int n13 = data[index - 13]&0xFF;
		      if((n13 >= 'A' && n13 <= 'Z') || (n13 >= '0' && n13 <= '9')) {
		        if(company == 0x33 || ((data[index - 10] & 0xFF) == 0x00 && (data[index - 4] & 0xFF) == 0x00)) {
		          has_bsx_slot = true;
		        }
		      }
		    }
		  }

		  if(has_bsx_slot) {
		    if(new String(data, index, 21).equals("Satellaview BS-X     ")) {
		      //BS-X base cart
		      type = Type.BsxBios;
		      mapper = MemoryMapper.BSXROM;
		      region = Region.NTSC;  //BS-X only released in Japan
		      return;         //RAM size handled internally by load_cart_bsx() -> BSXCart class
		    } else {
		      type = Type.BsxSlotted;
		      mapper = (index == 0x7fc0 ? MemoryMapper.BSCLoROM : MemoryMapper.BSCHiROM);
		      region = Region.NTSC;  //BS-X slotted cartridges only released in Japan
		    }
		  } else {
		    //standard cart
		    type = Type.Normal;

		    if(index == 0x7fc0 && size >= 0x401000) {
		      mapper = MemoryMapper.ExLoROM;
		    } else if(index == 0x7fc0 && mapperid == 0x32) {
		      mapper = MemoryMapper.ExLoROM;
		    } else if(index == 0x7fc0) {
		      mapper = MemoryMapper.LoROM;
		    } else if(index == 0xffc0) {
		      mapper = MemoryMapper.HiROM;
		    } else {  //index == 0x40ffc0
		      mapper = MemoryMapper.ExHiROM;
		    }
		  }

		  if(mapperid == 0x20 && (rom_type == 0x13 || rom_type == 0x14 || rom_type == 0x15 || rom_type == 0x1a)) {
		    has_superfx = true;
		    mapper = MemoryMapper.SuperFXROM;
		    ram_size = 1024 << (data[index - 3] & 7);
		    if(ram_size == 1024) ram_size = 0;
		  }

		  if(mapperid == 0x23 && (rom_type == 0x32 || rom_type == 0x34 || rom_type == 0x35)) {
		    has_sa1 = true;
		    mapper = MemoryMapper.SA1ROM;
		  }

		  if(mapperid == 0x35 && rom_type == 0x55) {
		    has_srtc = true;
		  }

		  if(mapperid == 0x32 && (rom_type == 0x43 || rom_type == 0x45)) {
		    has_sdd1 = true;
		  }

		  if(mapperid == 0x3a && (rom_type == 0xf5 || rom_type == 0xf9)) {
		    has_spc7110 = true;
		    has_spc7110rtc = (rom_type == 0xf9);
		    mapper = MemoryMapper.SPC7110ROM;
		  }

		  if(mapperid == 0x20 && rom_type == 0xf3) {
		    has_cx4 = true;
		  }

		  if((mapperid == 0x20 || mapperid == 0x21) && rom_type == 0x03) {
		    has_dsp1 = true;
		  }

		  if(mapperid == 0x30 && rom_type == 0x05 && company != 0xb2) {
		    has_dsp1 = true;
		  }

		  if(mapperid == 0x31 && (rom_type == 0x03 || rom_type == 0x05)) {
		    has_dsp1 = true;
		  }

		  if(has_dsp1 == true) {
		    if((mapperid & 0x2f) == 0x20 && size <= 0x100000) {
		      dsp1_mapper = DSP1MemoryMapper.DSP1LoROM1MB;
		    } else if((mapperid & 0x2f) == 0x20) {
		      dsp1_mapper = DSP1MemoryMapper.DSP1LoROM2MB;
		    } else if((mapperid & 0x2f) == 0x21) {
		      dsp1_mapper = DSP1MemoryMapper.DSP1HiROM;
		    }
		  }

		  if(mapperid == 0x20 && rom_type == 0x05) {
		    has_dsp2 = true;
		  }

		  if(mapperid == 0x30 && rom_type == 0x05 && company == 0xb2) {
		    has_dsp3 = true;
		  }

		  if(mapperid == 0x30 && rom_type == 0x03) {
		    has_dsp4 = true;
		  }

		  if(mapperid == 0x30 && rom_type == 0x25) {
		    has_obc1 = true;
		  }

		  if(mapperid == 0x30 && rom_type == 0xf6 && rom_size >= 10) {
		    has_st010 = true;
		  }

		  if(mapperid == 0x30 && rom_type == 0xf6 && rom_size < 10) {
		    has_st011 = true;
		  }

		  if(mapperid == 0x30 && rom_type == 0xf5) {
		    has_st018 = true;
		  }
	}

	private int find_header(byte[] data, int size)
	{
		int score_lo = score_header(data, size, 0x007fc0);
		int score_hi = score_header(data, size, 0x00ffc0);
		int score_ex = score_header(data, size, 0x40ffc0);
		if (score_ex != 0)
		{
			score_ex += 4; // favor ExHiROM on images > 32mbits
		}

		if (score_lo >= score_hi && score_lo >= score_ex)
		{
			return 0x007fc0;
		}
		else if (score_hi >= score_ex)
		{
			return 0x00ffc0;
		}
		else
		{
			return 0x40ffc0;
		}
	}

	private int score_header(byte[] data, int size, int addr)
	{
		// image too small to contain header at this location?
		if (size < addr + 64) { return 0; }
		
		int score = 0;

		int resetvector = (data[addr + HeaderField_ResetVector] & 0xFF)
				| ((data[addr + HeaderField_ResetVector + 1] & 0xFF) << 8);
		int checksum = (data[addr + HeaderField_Checksum] & 0xFF)
				| ((data[addr + HeaderField_Checksum + 1] & 0xFF) << 8);
		int complement = (data[addr + HeaderField_Complement] & 0xFF)
				| ((data[addr + HeaderField_Complement + 1] & 0xFF) << 8);

		// first opcode executed upon reset
		int resetop = data[((addr & 0xFFFF) & ~0x7fff) | (resetvector & 0x7fff)] & 0xFF;
		
		// mask off irrelevent FastROM-capable bit
		int mapper = ((data[addr + HeaderField_Mapper] & 0xFF) & ~0x10) & 0xFF;

		// $00:[000-7fff] contains uninitialized RAM and MMIO.
		// reset vector must point to ROM at $00:[8000-ffff] to be considered
		// valid.
		if (resetvector < 0x8000) { return 0; }

		// some images duplicate the header in multiple locations, and others
		// have completely
		// invalid header information that cannot be relied upon.
		// below code will analyze the first opcode executed at the specified
		// reset vector to
		// determine the probability that this is the correct header.

		// most likely opcodes
		if (resetop == 0x78 // sei
				|| resetop == 0x18 // clc (clc; xce)
				|| resetop == 0x38 // sec (sec; xce)
				|| resetop == 0x9c // stz $nnnn (stz $4200)
				|| resetop == 0x4c // jmp $nnnn
				|| resetop == 0x5c // jml $nnnnnn
		)
		{
			score += 8;
		}

		// plausible opcodes
		if (resetop == 0xc2 // rep #$nn
				|| resetop == 0xe2 // sep #$nn
				|| resetop == 0xad // lda $nnnn
				|| resetop == 0xae // ldx $nnnn
				|| resetop == 0xac // ldy $nnnn
				|| resetop == 0xaf // lda $nnnnnn
				|| resetop == 0xa9 // lda #$nn
				|| resetop == 0xa2 // ldx #$nn
				|| resetop == 0xa0 // ldy #$nn
				|| resetop == 0x20 // jsr $nnnn
				|| resetop == 0x22 // jsl $nnnnnn
		)
		{
			score += 4;
		}

		// implausible opcodes
		if (resetop == 0x40 // rti
				|| resetop == 0x60 // rts
				|| resetop == 0x6b // rtl
				|| resetop == 0xcd // cmp $nnnn
				|| resetop == 0xec // cpx $nnnn
				|| resetop == 0xcc // cpy $nnnn
		) score -= 4;

		// least likely opcodes
		if (resetop == 0x00 // brk #$nn
				|| resetop == 0x02 // cop #$nn
				|| resetop == 0xdb // stp
				|| resetop == 0x42 // wdm
				|| resetop == 0xff // sbc $nnnnnn,x
		)
		{
			score -= 8;
		}

		// at times, both the header and reset vector's first opcode will match
		// ...
		// fallback and rely on info validity in these cases to determine more
		// likely header.

		// a valid checksum is the biggest indicator of a valid header.
		if ((checksum + complement) == 0xffff && (checksum != 0) && (complement != 0))
		{
			score += 4;
		}

		if (addr == 0x007fc0 && mapper == 0x20)
		{
			score += 2; // 0x20 is usually LoROM
		}
		if (addr == 0x00ffc0 && mapper == 0x21)
		{
			score += 2; // 0x21 is usually HiROM
		}
		if (addr == 0x007fc0 && mapper == 0x22)
		{
			score += 2; // 0x22 is usually ExLoROM
		}
		if (addr == 0x40ffc0 && mapper == 0x25)
		{
			score += 2; // 0x25 is usually ExHiROM
		}

		if ((data[addr + HeaderField_Company] & 0xFF) == 0x33)
		{
			score += 2; // 0x33 indicates extended header
		}
		if ((data[addr + HeaderField_RomType] & 0xFF) < 0x08)
		{
			score++;
		}
		if ((data[addr + HeaderField_RomSize] & 0xFF) < 0x10)
		{
			score++;
		}
		if ((data[addr + HeaderField_RamSize] & 0xFF) < 0x08)
		{
			score++;
		}
		if ((data[addr + HeaderField_CartRegion] & 0xFF) < 14)
		{
			score++;
		}

		if (score < 0)
		{
			score = 0;
		}
		return score;
	}
	
	private int gameboy_ram_size(byte[] data, int size) {
		  if(size < 512) return 0;
		  switch(data[0x0149]&0xff) {
		    case 0x00: return   0 * 1024;
		    case 0x01: return   8 * 1024;
		    case 0x02: return   8 * 1024;
		    case 0x03: return  32 * 1024;
		    case 0x04: return 128 * 1024;
		    case 0x05: return 128 * 1024;
		    default:   return 128 * 1024;
		  }
		}

		private boolean gameboy_has_rtc(byte[] data, int size) {
		  if(size < 512) return false;
		  if((data[0x0147]&0xff) == 0x0f ||(data[0x0147]&0xff) == 0x10) return true;
		  return false;
		}


	public static final int HeaderField_CartName = 0x00;
	public static final int HeaderField_Mapper = 0x15;
	public static final int HeaderField_RomType = 0x16;
	public static final int HeaderField_RomSize = 0x17;
	public static final int HeaderField_RamSize = 0x18;
	public static final int HeaderField_CartRegion = 0x19;
	public static final int HeaderField_Company = 0x1a;
	public static final int HeaderField_Version = 0x1b;
	public static final int HeaderField_Complement = 0x1c;
	public static final int HeaderField_Checksum = 0x1e;
	public static final int HeaderField_ResetVector = 0x3c;

	private enum Mode { Normal, BsxSlotted, Bsx, SufamiTurbo, SuperGameBoy }
	
	private enum Type { Normal, BsxSlotted, BsxBios, Bsx, SufamiTurboBios,
		SufamiTurbo, SuperGameBoy1Bios, SuperGameBoy2Bios, GameBoy, Unknown }
	
	private enum Region
	{
		NTSC, PAL
	}

	private enum MemoryMapper
	{
		LoROM, HiROM, ExLoROM, ExHiROM, SuperFXROM, SA1ROM, SPC7110ROM, BSCLoROM, BSCHiROM, BSXROM, STROM
	}

	private enum DSP1MemoryMapper
	{
		DSP1Unmapped, DSP1LoROM1MB, DSP1LoROM2MB, DSP1HiROM 
	}

	// private bool loaded; //is a base cartridge inserted?
	// private uint crc32; //crc32 of all cartridges (base+slot(s))
	private int rom_size;
	private int ram_size;

	private Mode mode;
	private Type type;
	private Region region;
	private MemoryMapper mapper;
	private DSP1MemoryMapper dsp1_mapper;
	
	private boolean has_bsx_slot;
	private boolean has_superfx;
	private boolean has_sa1;
	private boolean has_srtc;
	private boolean has_sdd1;
	private boolean has_spc7110;
	private boolean has_spc7110rtc;
	private boolean has_cx4;
	private boolean has_dsp1;
	private boolean has_dsp2;
	private boolean has_dsp3;
	private boolean has_dsp4;
	private boolean has_obc1;
	private boolean has_st010;
	private boolean has_st011;
	private boolean has_st018;

}
