package jario.snes.cartridge;

import jario.hardware.Bus32bit;
import jario.hardware.Bus8bit;
import jario.hardware.Configurable;
import jario.hardware.Hardware;
import jario.snes.cartridge.Cartridge.MapMode;
import jario.snes.cartridge.Cartridge.Mode;
import jario.snes.cartridge.Cartridge.Region;
import jario.snes.cartridge.Cartridge.SuperGameBoyVersion;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlParser implements java.io.Serializable
{
	private Cartridge cart;
	
	public XmlParser(Cartridge cart)
	{
		this.cart = cart;
	}

	public void parse_xml(String[] list) {
	  cart.mapping.clear();
	  parse_xml_cartridge(list[0]);

	  if(cart.mode == Mode.BsxSlotted) {
	    parse_xml_bsx(list[1]);
	  } else if(cart.mode == Mode.Bsx) {
	    parse_xml_bsx(list[1]);
	  } else if(cart.mode == Mode.SufamiTurbo) {
	    parse_xml_sufami_turbo(list[1], 0);
	    parse_xml_sufami_turbo(list[2], 1);
	  } else if(cart.mode == Mode.SuperGameBoy) {
	    parse_xml_gameboy(list[1]);
	  }
	}

	private void parse_xml_cartridge(String data) {
	  //xml_element document = xml_parse(data);
	  Document document;
	  try
		{
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			document = db.parse(new InputSource(new ByteArrayInputStream(data.getBytes("utf-8"))));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
	  //if(document.element.size() == 0) return;
	  if (!document.hasChildNodes()) { return; }

	  //foreach(head, document.element) {
	  NodeList head = document.getElementsByTagName("cartridge");
	    //if(head.name == "cartridge") {
	    if (head.getLength() != 0) {
	      //foreach(attr, head.attribute) {
	    	Element attr = (Element) head.item(0);
	        //if(attr.name == "region") {
	        if (attr.hasAttribute("region")) {
	          if(attr.getAttribute("region").equals("NTSC")) cart.region = Region.NTSC;
	          if(attr.getAttribute("region").equals("PAL")) cart.region = Region.PAL;
	        }
	      //}

	      for(int i=0; i<attr.getChildNodes().getLength(); i++) {
	    	Node node = attr.getChildNodes().item(i);
	      //foreach(node, head.element) {
	        if(node.getNodeName().equals("rom")) xml_parse_rom((Element)node);
	        if(node.getNodeName().equals("ram")) xml_parse_ram((Element)node);
	        if(node.getNodeName().equals("superfx")) xml_parse_superfx((Element)node);
	        if(node.getNodeName().equals("sa1")) xml_parse_sa1((Element)node);
	        if(node.getNodeName().equals("upd77c25")) xml_parse_upd77c25((Element)node);
	        if(node.getNodeName().equals("bsx")) xml_parse_bsx((Element)node);
	        if(node.getNodeName().equals("sufamiturbo")) xml_parse_sufamiturbo((Element)node);
	        if(node.getNodeName().equals("supergameboy")) xml_parse_supergameboy((Element)node);
	        if(node.getNodeName().equals("srtc")) xml_parse_srtc((Element)node);
	        if(node.getNodeName().equals("sdd1")) xml_parse_sdd1((Element)node);
	        if(node.getNodeName().equals("spc7110")) xml_parse_spc7110((Element)node);
	        if(node.getNodeName().equals("cx4")) xml_parse_cx4((Element)node);
	        if(node.getNodeName().equals("obc1")) xml_parse_obc1((Element)node);
	        if(node.getNodeName().equals("setadsp")) xml_parse_setadsp((Element)node);
	        if(node.getNodeName().equals("setarisc")) xml_parse_setarisc((Element)node);
	        if(node.getNodeName().equals("msu1")) xml_parse_msu1((Element)node);
	        if(node.getNodeName().equals("serial")) xml_parse_serial((Element)node);
	      }
	    }
	  //}
	}

	private void parse_xml_bsx(String data) {
	}

	private void parse_xml_sufami_turbo(String data, int slot) {
	}

	private void parse_xml_gameboy(String data) {
	  //xml_element document = xml_parse(data);
	  Document document;
	  try
		{
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			document = db.parse(new InputSource(new ByteArrayInputStream(data.getBytes("utf-8"))));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
	  //if(document.element.size() == 0) return;
	  if (!document.hasChildNodes()) { return; }

	  //foreach(head, document.element) {
	  NodeList head = document.getElementsByTagName("cartridge");
	    //if(head.name == "cartridge") {
	  if (head.getLength() != 0) {
		  Element attr = (Element) head.item(0);
	      //foreach(attr, head.attribute) {
	        if (attr.hasAttribute("rtc")) {
	          cart.supergameboy_rtc_size = (attr.getAttribute("rtc").equals("true")) ? 4 : 0;
		    }

	      for(int i=0; i<attr.getChildNodes().getLength(); i++) {
	    	Node leaf = attr.getChildNodes().item(i);
	      //foreach(leaf, head.element) {
	        if(leaf instanceof Element && leaf.getNodeName().equals("ram")) {
	          if (((Element) leaf).hasAttribute("size")) {
	        	  cart.supergameboy_ram_size = Integer.parseInt(((Element) leaf).getAttribute("size"), 16);
		      }
	        }
	      }
	    //}
	  }
	}

	private void xml_parse_rom(Element root) {
	  NodeList maps = root.getElementsByTagName("map");
	  for (int i = 0; i < maps.getLength(); i++) {
		Element leaf = (Element) maps.item(i);
	  //foreach(leaf, root.element) {
	    //if(leaf.name == "map") {
	      Mapping m = new Mapping(cart.cartrom, true);
	      //foreach(attr, leaf.attribute) {
	        if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	        if(leaf.hasAttribute("mode")) xml_parse_mode(m, leaf.getAttribute("mode"));
	        if(leaf.hasAttribute("offset")) m.offset = Integer.parseInt(leaf.getAttribute("offset"), 16);
	        if(leaf.hasAttribute("size")) m.size = Integer.parseInt(leaf.getAttribute("size"), 16);
	      //}
	      cart.mapping.add(m);
	    //}
	  }
	}

	private void xml_parse_ram(Element root) {
	  if (root.hasAttribute("size")) {
		cart.ram_size = Integer.parseInt(root.getAttribute("size"), 16);
	  }

	  NodeList maps = root.getElementsByTagName("map");
	  for (int i = 0; i < maps.getLength(); i++) {
		Element leaf = (Element) maps.item(i);
	  //foreach(leaf, root.element) {
	    //if(leaf.name == "map") {
	      Mapping m = new Mapping(cart.cartram, true);
	      //foreach(attr, leaf.attribute) {
	        if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	        if(leaf.hasAttribute("mode")) xml_parse_mode(m, leaf.getAttribute("mode"));
	        if(leaf.hasAttribute("offset")) m.offset = Integer.parseInt(leaf.getAttribute("offset"), 16);
	        if(leaf.hasAttribute("size")) m.size = Integer.parseInt(leaf.getAttribute("size"), 16);
	      //}
	      cart.mapping.add(m);
	    //}
	  }
	}

	private void xml_parse_superfx(Element root) {
		Hardware superfx;
		if ((superfx = loadChip("SUPERFX")) == null) {
			System.out.println("Failed to load chip: SUPERFX");
			return;
		}
		superfx.connect(1, cart.cartrom);
		cart.chips.add(superfx);
	  cart.has_superfx = true;

	  NodeList nodes = root.getChildNodes();
	  for (int i = 0; i < nodes.getLength(); i++) {
		Node node = nodes.item(i);
	  //foreach(node, root.element) {
	    if(node instanceof Element && node.getNodeName().equals("rom")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		  	Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) ((Configurable) superfx).readConfig("fxrom"), true);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	            if(leaf.hasAttribute("mode")) xml_parse_mode(m, leaf.getAttribute("mode"));
	            if(leaf.hasAttribute("offset")) m.offset = Integer.parseInt(leaf.getAttribute("offset"), 16);
	            if(leaf.hasAttribute("size")) m.size = Integer.parseInt(leaf.getAttribute("size"), 16);
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    } else if(node instanceof Element && node.getNodeName().equals("ram")) {
	      if (((Element) node).hasAttribute("size")) {
	        cart.ram_size = Integer.parseInt(((Element) node).getAttribute("size"), 16);
	      }

	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		  	Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) ((Configurable) superfx).readConfig("fxram"), true);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	            if(leaf.hasAttribute("mode")) xml_parse_mode(m, leaf.getAttribute("mode"));
	            if(leaf.hasAttribute("offset")) m.offset = Integer.parseInt(leaf.getAttribute("offset"), 16);
	            if(leaf.hasAttribute("size")) m.size = Integer.parseInt(leaf.getAttribute("size"), 16);
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    } else if(node instanceof Element && node.getNodeName().equals("mmio")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		    Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) superfx, false);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    }
	  }
	}

	private void xml_parse_sa1(Element root) {
		Hardware sa1;
		if ((sa1 = loadChip("SA1")) == null) {
			System.out.println("Failed to load chip: SA1");
			return;
		}
		sa1.connect(1, cart.cartrom);
		((Configurable) sa1).writeConfig("region", cart.region);
		cart.chips.add(sa1);
	  cart.has_sa1 = true;

	  NodeList nodes = root.getChildNodes();
	  for (int i = 0; i < nodes.getLength(); i++) {
		Node node = nodes.item(i);
	  //foreach(node, root.element) {
	    if(node instanceof Element && node.getNodeName().equals("rom")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
	  	  for (int j = 0; j < maps.getLength(); j++) {
	  		Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) ((Configurable) sa1).readConfig("vsprom"), true);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	            if(leaf.hasAttribute("mode")) xml_parse_mode(m, leaf.getAttribute("mode"));
	            if(leaf.hasAttribute("offset")) m.offset = Integer.parseInt(leaf.getAttribute("offset"), 16);
	            if(leaf.hasAttribute("size")) m.size = Integer.parseInt(leaf.getAttribute("size"), 16);
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    } else if(node instanceof Element && node.getNodeName().equals("iram")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		  	Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) ((Configurable) sa1).readConfig("cpuiram"), true);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	            if(leaf.hasAttribute("mode")) xml_parse_mode(m, leaf.getAttribute("mode"));
	            if(leaf.hasAttribute("offset")) m.offset = Integer.parseInt(leaf.getAttribute("offset"), 16);
	            if(leaf.hasAttribute("size")) m.size = Integer.parseInt(leaf.getAttribute("size"), 16);
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    } else if(node instanceof Element && node.getNodeName().equals("bwram")) {
	      if (((Element) node).hasAttribute("size")) {
	        cart.ram_size = Integer.parseInt(((Element) node).getAttribute("size"), 16);
	      }

	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		  	Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) ((Configurable) sa1).readConfig("cc1bwram"), true);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	            if(leaf.hasAttribute("mode")) xml_parse_mode(m, leaf.getAttribute("mode"));
	            if(leaf.hasAttribute("offset")) m.offset = Integer.parseInt(leaf.getAttribute("offset"), 16);
	            if(leaf.hasAttribute("size")) m.size = Integer.parseInt(leaf.getAttribute("size"), 16);
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    } else if(node instanceof Element && node.getNodeName().equals("mmio")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
			for (int j = 0; j < maps.getLength(); j++) {
			  Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) sa1, false);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    }
	  }
	}

	private void xml_parse_upd77c25(Element root) {
		Hardware upd77c25;
		if ((upd77c25 = loadChip("UPD77C25")) == null) {
			System.out.println("Failed to load chip: UPD77C25");
			return;
		}
		cart.chips.add(upd77c25);
	  cart.has_upd77c25 = true;

	  boolean program = false;
	  boolean sha256 = false;
	  String xml_hash = "";
	  StringBuffer rom_hash = new StringBuffer();

//	  for(unsigned n = 0; n < 2048; n++) upd77c25.programROM[n] = 0;
//	  for(unsigned n = 0; n < 1024; n++) upd77c25.dataROM[n] = 0;

	    if (root.hasAttribute("program")) {
	  //foreach(attr, root.attribute) {
	    //if(attr.name == "program") {
		  try {
		  RandomAccessFile fp;
		  fp = new RandomAccessFile(new File(basename()+root.getAttribute("program")), "r");
	      if(/*fp.open() &&*/ fp.length() == 8192) {
	        program = true;

	        for(int n = 0; n < 2048; n++) {
	        	int b0 = fp.read();
				int b1 = fp.read();
				int b2 = fp.read();
	          //upd77c25.programROM[n] = fp.readm(3);
	          ((Bus32bit) upd77c25).write32bit(n, (b0 << 16) | (b1 << 8) | (b2 << 0));
	        }
	        for(int n = 0; n < 1024; n++) {
	        	int b0 = fp.read();
				int b1 = fp.read();
	          //upd77c25.dataROM[n] = fp.readm(2);
	          ((Bus32bit) upd77c25).write32bit(n + 2048, (b0 << 8) | (b1 << 0));
	        }

	        fp.seek(0);
	        byte[] data = new byte[8192];
	        fp.read(data);
	        fp.close();

	        MessageDigest shahash = MessageDigest.getInstance("SHA-256");
			shahash.update(data);
			for (byte n : shahash.digest()) rom_hash.append(Integer.toString((n & 0xff) + 0x100, 16).substring(1));
	      }
		  }
	      catch (Exception e) { e.printStackTrace(); }
	    }
		if(root.hasAttribute("sha256")) {
	      sha256 = true;
	      xml_hash = root.getAttribute("sha256");
	    }
//	  }

	  NodeList nodes = root.getChildNodes();
	  for (int i = 0; i < nodes.getLength(); i++) {
		Node node = nodes.item(i);
	  //foreach(node, root.element) {
	    if(node instanceof Element && node.getNodeName().equals("dr")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
			Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) ((Configurable) upd77c25).readConfig("upd77c25dr"), true);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    } else if(node instanceof Element && node.getNodeName().equals("sr")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
			Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) ((Configurable) upd77c25).readConfig("upd77c25sr"), true);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	          //}
	          cart.mapping.add(m);
//	        }
	      }
	    }
	  }

	  if(program == false) {
		  System.out.println("Warning: uPD77C25 program is missing.");
	  } else if(sha256 == true && !xml_hash.equals(rom_hash.toString())) {
		  System.out.println(
	      "Warning: uPD77C25 program SHA256 is incorrect.\n\n"+
	      "Expected:\n"+xml_hash+"\n\n"+
	      "Actual:\n"+rom_hash
	    );
	  }
	}

	private void xml_parse_bsx(Element root) {
	  if(cart.mode != Mode.BsxSlotted && cart.mode != Mode.Bsx) return;
	  Hardware bsxcart;
		if ((bsxcart = loadChip("BSX")) == null) {
			System.out.println("Failed to load chip: BSX");
			return;
		}
		cart.chips.add(bsxcart);

	  NodeList nodes = root.getChildNodes();
	  for (int i = 0; i < nodes.getLength(); i++) {
	    Node node = nodes.item(i);
	  //foreach(node, root.element) {
	    if(node instanceof Element && node.getNodeName().equals("slot")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
	  	  for (int j = 0; j < maps.getLength(); j++) {
	  	    Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) ((Configurable) bsxcart).readConfig("bsxflash"), true);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	            if(leaf.hasAttribute("mode")) xml_parse_mode(m, leaf.getAttribute("mode"));
	            if(leaf.hasAttribute("offset")) m.offset = Integer.parseInt(leaf.getAttribute("offset"), 16);
	            if(leaf.hasAttribute("size")) m.size = Integer.parseInt(leaf.getAttribute("size"), 16);
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    } else if(node instanceof Element && node.getNodeName().equals("mmio")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		    Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) bsxcart, false);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    }
	  }
	}

	private void xml_parse_sufamiturbo(Element root) {
	  if(cart.mode != Mode.SufamiTurbo) return;
	  Hardware sufamiturbo;
		if ((sufamiturbo = loadChip("SUFAMITURBO")) == null) {
			System.out.println("Failed to load chip: SUFAMITURBO");
			return;
		}
		cart.chips.add(sufamiturbo);

	  NodeList nodes = root.getChildNodes();
	  for (int i = 0; i < nodes.getLength(); i++) {
	    Node node = nodes.item(i);
	  //foreach(node, root.element) {
	    if(node instanceof Element && node.getNodeName().equals("slot")) {
	      int slotid = 0;
	      if (((Element) node).hasAttribute("id")) {
			  if(((Element) node).getAttribute("id").equals("A")) slotid = 0;
		      if(((Element) node).getAttribute("id").equals("B")) slotid = 1;
		  }

	      NodeList slots = node.getChildNodes();
		  for (int s = 0; s < slots.getLength(); s++) {
		    Node slot = slots.item(s);
	      //foreach(slot, node.element) {
	        if(slot instanceof Element && slot.getNodeName().equals("rom")) {
	          NodeList maps = ((Element) slot).getElementsByTagName("map");
	  		  for (int j = 0; j < maps.getLength(); j++) {
	  		    Element leaf = (Element) maps.item(j);
	          //foreach(leaf, slot.element) {
	            //if(leaf.name == "map") {
	              Mapping m = new Mapping(slotid == 0 ? (Bus8bit) ((Configurable) sufamiturbo).readConfig("stArom") : (Bus8bit) ((Configurable) sufamiturbo).readConfig("stBrom"), true);
	              //foreach(attr, leaf.attribute) {
	                if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	                if(leaf.hasAttribute("mode")) xml_parse_mode(m, leaf.getAttribute("mode"));
	                if(leaf.hasAttribute("offset")) m.offset = Integer.parseInt(leaf.getAttribute("offset"), 16);
	                if(leaf.hasAttribute("size")) m.size = Integer.parseInt(leaf.getAttribute("size"), 16);
	              //}
	              if(m.memory instanceof Configurable && ((Configurable)m.memory).readConfig("size") != null && (Integer)((Configurable)m.memory).readConfig("size") > 0) cart.mapping.add(m);
	            //}
	          }
	        } else if(slot instanceof Element && slot.getNodeName().equals("ram")) {
	          NodeList maps = ((Element) slot).getElementsByTagName("map");
	  		  for (int j = 0; j < maps.getLength(); j++) {
	  		    Element leaf = (Element) maps.item(j);
	          //foreach(leaf, slot.element) {
	            //if(leaf.name == "map") {
	              Mapping m = new Mapping(slotid == 0 ? (Bus8bit) ((Configurable) sufamiturbo).readConfig("stAram") : (Bus8bit) ((Configurable) sufamiturbo).readConfig("stBram"), true);
	              //foreach(attr, leaf.attribute) {
	                if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	                if(leaf.hasAttribute("mode")) xml_parse_mode(m, leaf.getAttribute("mode"));
	                if(leaf.hasAttribute("offset")) m.offset = Integer.parseInt(leaf.getAttribute("offset"), 16);
	                if(leaf.hasAttribute("size")) m.size = Integer.parseInt(leaf.getAttribute("size"), 16);
	              //}
	              if(m.memory instanceof Configurable && ((Configurable)m.memory).readConfig("size") != null && (Integer)((Configurable)m.memory).readConfig("size") > 0) cart.mapping.add(m);
	            //}
	          }
	        }
	      }
	    }
	  }
	}

	private void xml_parse_supergameboy(Element root) {
	  if(cart.mode != Mode.SuperGameBoy) return;
	  Hardware supergameboy;
		if ((supergameboy = loadChip("SUPERGAMEBOY")) == null) {
			System.out.println("Failed to load chip: SUPERGAMEBOY");
			return;
		}
		cart.chips.add(supergameboy);

	  if (root.hasAttribute("revision")) {
		  if(root.getAttribute("revision").equals("1")) cart.supergameboy_version = SuperGameBoyVersion.Version1;
	      if(root.getAttribute("revision").equals("2")) cart.supergameboy_version = SuperGameBoyVersion.Version2;
	  }

	  NodeList nodes = root.getChildNodes();
	  for (int i = 0; i < nodes.getLength(); i++) {
		Node node = nodes.item(i);
	  //foreach(node, root.element) {
	    if(node instanceof Element && node.getNodeName().equals("mmio")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		    Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) supergameboy, true);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    }
	  }
	}

	private void xml_parse_srtc(Element root) {
		Hardware srtc;
		if ((srtc = loadChip("SRTC")) == null) {
			System.out.println("Failed to load chip: SRTC");
			return;
		}
		cart.chips.add(srtc);
	  cart.has_srtc = true;

	  NodeList nodes = root.getChildNodes();
	  for (int i = 0; i < nodes.getLength(); i++) {
		Node node = nodes.item(i);
	  //foreach(node, root.element) {
	    if(node instanceof Element && node.getNodeName().equals("mmio")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		    Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) srtc, false);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    }
	  }
	}

	private void xml_parse_sdd1(Element root) {
		Hardware sdd1;
		if ((sdd1 = loadChip("SDD1")) == null) {
			System.out.println("Failed to load chip: SDD1");
			return;
		}
		sdd1.connect(1, cart.cartrom);
		cart.chips.add(sdd1);
	  cart.has_sdd1 = true;

	  NodeList nodes = root.getChildNodes();
	  for (int i = 0; i < nodes.getLength(); i++) {
		Node node = nodes.item(i);
	  //foreach(node, root.element) {
	    if(node instanceof Element && node.getNodeName().equals("mcu")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		    Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) ((Configurable) sdd1).readConfig("mcu"), true);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    } else if(node instanceof Element && node.getNodeName().equals("mmio")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		    Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) sdd1, false);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    }
	  }
	}

	private void xml_parse_spc7110(Element root) {
		Hardware spc7110;
		if ((spc7110 = loadChip("SPC7110")) == null) {
			System.out.println("Failed to load chip: SPC7110");
			return;
		}
		cart.chips.add(spc7110);
	  cart.has_spc7110 = true;

	  NodeList nodes = root.getChildNodes();
	  for (int i = 0; i < nodes.getLength(); i++) {
		Node node = nodes.item(i);
	  //foreach(node, root.element) {
	    if(node instanceof Element && node.getNodeName().equals("dcu")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		    Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) ((Configurable) spc7110).readConfig("spc7110dcu"), true);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    } else if(node instanceof Element && node.getNodeName().equals("mcu")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		    Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) ((Configurable) spc7110).readConfig("spc7110mcu"), true);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	            if(leaf.hasAttribute("offset")) cart.spc7110_data_rom_offset = Integer.parseInt(leaf.getAttribute("offset"), 16);
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    } else if(node instanceof Element && node.getNodeName().equals("mmio")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		    Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) spc7110, false);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    } else if(node instanceof Element && node.getNodeName().equals("ram")) {
	      if (((Element) node).hasAttribute("size")) {
		    cart.ram_size = Integer.parseInt(((Element) node).getAttribute("size"), 16);
		  }

	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		    Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) ((Configurable) spc7110).readConfig("spc7110ram"), true);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	            if(leaf.hasAttribute("mode")) xml_parse_mode(m, leaf.getAttribute("mode"));
	            if(leaf.hasAttribute("offset")) m.offset = Integer.parseInt(leaf.getAttribute("offset"), 16);
	            if(leaf.hasAttribute("size")) m.size = Integer.parseInt(leaf.getAttribute("size"), 16);
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    } else if(node instanceof Element && node.getNodeName().equals("rtc")) {
	      cart.has_spc7110rtc = true;

	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		    Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) spc7110, false);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    }
	  }
	}

	private void xml_parse_cx4(Element root) {
		Hardware cx4;
		if ((cx4 = loadChip("CX4")) == null) {
			System.out.println("Failed to load chip: CX4");
			return;
		}
		cart.chips.add(cx4);
	  cart.has_cx4 = true;

	  NodeList nodes = root.getChildNodes();
	  for (int i = 0; i < nodes.getLength(); i++) {
		Node node = nodes.item(i);
	  //foreach(node, root.element) {
	    if(node instanceof Element && node.getNodeName().equals("mmio")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		  	Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) cx4, true);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    }
	  }
	}

	private void xml_parse_obc1(Element root) {
		Hardware obc1;
		if ((obc1 = loadChip("OBC1")) == null) {
			System.out.println("Failed to load chip: OBC1");
			return;
		}
		cart.chips.add(obc1);
	  cart.has_obc1 = true;

	  NodeList nodes = root.getChildNodes();
	  for (int i = 0; i < nodes.getLength(); i++) {
		Node node = nodes.item(i);
	  //foreach(node, root.element) {
	    if(node instanceof Element && node.getNodeName().equals("mmio")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		    Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) obc1, false);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    }
	  }
	}

	private void xml_parse_setadsp(Element root) {
		Hardware st0010;
		if ((st0010 = loadChip("ST0010")) == null) {
			System.out.println("Failed to load chip: ST0010");
			return;
		}
		cart.chips.add(st0010);
	  int program = 0;

	  if (root.hasAttribute("program")) {
		  if(root.getAttribute("program").equals("ST-0010")) {
			  program = 1;
		      cart.has_st0010 = true;
		  }
	      if(root.getAttribute("program").equals("ST-0011")) {
	    	  program = 2;
		      cart.has_st0011 = true;
	      }
	  }

	  Bus8bit[] map = { null, (Bus8bit) st0010, null };

	  NodeList nodes = root.getChildNodes();
	  for (int i = 0; i < nodes.getLength(); i++) {
		Node node = nodes.item(i);
	  //foreach(node, root.element) {
	    if(node instanceof Element && node.getNodeName().equals("mmio") && map[program] != null) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		    Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping(map[program], true);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    }
	  }
	}

	private void xml_parse_setarisc(Element root) {
		Hardware st0018;
		if ((st0018 = loadChip("ST0018")) == null) {
			System.out.println("Failed to load chip: ST0018");
			return;
		}
		cart.chips.add(st0018);
	  int program = 0;

	  if (root.hasAttribute("program")) {
		  if(root.getAttribute("program").equals("ST-0018")) {
			  program = 1;
		      cart.has_st0018 = true;
		  }
	  }

	  Bus8bit[] map = { null, (Bus8bit) st0018 };

	  NodeList nodes = root.getChildNodes();
	  for (int i = 0; i < nodes.getLength(); i++) {
		Node node = nodes.item(i);
	  //foreach(node, root.element) {
	    if(node instanceof Element && node.getNodeName().equals("mmio") && map[program] != null) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		    Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping(map[program], false);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    }
	  }
	}

	private void xml_parse_msu1(Element root) {
		Hardware msu1;
		if ((msu1 = loadChip("MSU1")) == null) {
			System.out.println("Failed to load chip: MSU1");
			return;
		}
		cart.chips.add(msu1);
	  cart.has_msu1 = true;

	  NodeList nodes = root.getChildNodes();
	  for (int i = 0; i < nodes.getLength(); i++) {
		Node node = nodes.item(i);
	  //foreach(node, root.element) {
	    if(node instanceof Element && node.getNodeName().equals("mmio")) {
	      NodeList maps = ((Element) node).getElementsByTagName("map");
		  for (int j = 0; j < maps.getLength(); j++) {
		  	Element leaf = (Element) maps.item(j);
	      //foreach(leaf, node.element) {
	        //if(leaf.name == "map") {
	          Mapping m = new Mapping((Bus8bit) msu1, false);
	          //foreach(attr, leaf.attribute) {
	            if(leaf.hasAttribute("address")) xml_parse_address(m, leaf.getAttribute("address"));
	          //}
	          cart.mapping.add(m);
	        //}
	      }
	    }
	  }
	}

	private void xml_parse_serial(Element root) {
		System.out.println("SERIAL not implemented yet.");
	  //has_serial = true;
	}

	private void xml_parse_address(Mapping m, String data) {
	  String[] part;
	  part = data.split(":");
	  if(part.length != 2) return;

	  String[] subpart;
	  subpart = part[0].split("-");
	  if(subpart.length == 1) {
	    m.banklo = Integer.parseInt(subpart[0], 16);
	    m.bankhi = m.banklo;
	  } else if(subpart.length == 2) {
	    m.banklo = Integer.parseInt(subpart[0], 16);
	    m.bankhi = Integer.parseInt(subpart[1], 16);
	  }

	  subpart = part[1].split("-");
	  if(subpart.length == 1) {
	    m.addrlo = Integer.parseInt(subpart[0], 16);
	    m.addrhi = m.addrlo;
	  } else if(subpart.length == 2) {
	    m.addrlo = Integer.parseInt(subpart[0], 16);
	    m.addrhi = Integer.parseInt(subpart[1], 16);
	  }
	}

	private void xml_parse_mode(Mapping m, String data) {
	       if(data.equals("direct")) m.mode = MapMode.Direct;
	  else if(data.equals("linear")) m.mode = MapMode.Linear;
	  else if(data.equals("shadow")) m.mode = MapMode.Shadow;
	}
	
	private String basename()
	{
		return cart.cartridgeName.substring(0, cart.cartridgeName.lastIndexOf(File.separator) + 1);
	}
	
	private Hardware loadChip(String name)
	{
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
				URL url = file.exists() ? file.toURI().toURL() : loader.getResource("resources" + File.separator + "components.properties");
				if (url != null) prop.load(url.openStream());
			}
			catch (IOException e)
			{
			}

			return (Hardware) Class.forName(prop.getProperty(name, name), true, loader).newInstance();
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
		
		return null;
	}

}
