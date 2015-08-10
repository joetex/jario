/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.system;

import jario.hardware.Bus8bit;
import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;
import jario.snes.console.Console;
import jario.snes.performance.ppu.Cache;

import java.awt.Canvas;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

public class SnesSystem implements Hardware
{
	private Canvas canvas;
	private JFrame window;
	private Hardware video;
	private Hardware audio;
	private Hardware controller1;
	private Hardware controller2;
	private Map<String, Hardware> controllers = new HashMap<String, Hardware>();
	private Hardware console;
	private Hardware cartridge;

	@SuppressWarnings("serial")
	private class Jario64MenuBar extends JMenuBar
	{
		public Jario64MenuBar()
		{
			add(makeFileMenu());
			add(makeController1Menu());
			add(makeController2Menu());
			add(makeSettingsMenu());
		}

		private JMenu makeFileMenu()
		{
			JMenu fileMenu = new JMenu();
			fileMenu.setText("File");

			JMenuItem loadRom = new JMenuItem("Insert Game Cartridge");
			loadRom.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					openRomFile();
				}
			});
			fileMenu.add(loadRom);

			JMenuItem loadCustomState = new JMenuItem("Load Custom Save State");
			loadCustomState.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					openLoadState();
				}
			});
			fileMenu.add(loadCustomState);
			
			JMenuItem loadLastSave = new JMenuItem("Load last save");
			loadLastSave.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt)
				{
					//LoadCartridge("C:\\Users\\Joel\\Documents\\JavaProjects\\roms\\Super Mario World (USA).smc");
					freeze();
					loadstate();
					unfreeze();
				}
			});
			fileMenu.add(loadLastSave);
			
			JMenuItem saveGame = new JMenuItem("Save game state");
			saveGame.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt)
				{
					freeze();
					savestate();
					unfreeze();
				}
			});
			fileMenu.add(saveGame);
			
			
			
			JMenuItem exit = new JMenuItem("Exit");
			exit.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					exit();
				}
			});
			
			
			fileMenu.add(exit);

			return fileMenu;
		}
		
		public void openRomFile()
		{
			// choose a rom file
			File romFile = null;
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Open ROM");
			fileChooser.setFileFilter(new FileFilter()
			{
				@Override
				public boolean accept(File f)
				{
					return f.isDirectory() || f.getName().endsWith(".smc") || f.getName().endsWith(".sfc");
				}

				@Override
				public String getDescription()
				{
					return "ROM files (.smc, .sfc)";
				}
			});
			int returnVal = fileChooser.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				romFile = fileChooser.getSelectedFile();
				LoadCartridge(romFile.getAbsolutePath());
			}
			else
			{
				return;
			}
		}
		
		public void openLoadState()
		{
			// choose a rom file
			File saveStateFile = null;
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Open ROM");
			fileChooser.setFileFilter(new FileFilter()
			{
				@Override
				public boolean accept(File f)
				{
					String filename = f.getName();
					System.out.println("Filename=" + filename);
					String extension = "";
					if( !f.isDirectory() )
						extension = filename.substring(filename.lastIndexOf("."));
					
					return f.isDirectory() || (extension.lastIndexOf(".j") == 0);
				}

				@Override
				public String getDescription()
				{
					return "Save Game State files (.j###)";
				}
			});
			int returnVal = fileChooser.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				saveStateFile = fileChooser.getSelectedFile();
				String filepath = saveStateFile.getAbsolutePath();
				String filename = saveStateFile.getName();
				loadstate(filepath.substring(0,filepath.lastIndexOf(filename)), filename);
			}
			else
			{
				return;
			}
		}

		private JMenu makeSettingsMenu()
		{
			JMenu settingsMenu = new JMenu();
			settingsMenu.setText("Settings");

			JCheckBoxMenuItem audioToggle = new JCheckBoxMenuItem("Enable Audio");
			audioToggle.setState((Boolean) ((Configurable) audio).readConfig("enable"));
			audioToggle.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					JCheckBoxMenuItem i = (JCheckBoxMenuItem) evt.getSource();
					((Configurable) audio).writeConfig("enable", i.isSelected());
				}
			});
			settingsMenu.add(audioToggle);
			
			JCheckBoxMenuItem videoToggle = new JCheckBoxMenuItem("Enable Video");
			videoToggle.setState((Boolean) ((Configurable) video).readConfig("enable"));
			videoToggle.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					JCheckBoxMenuItem i = (JCheckBoxMenuItem) evt.getSource();
					((Configurable) video).writeConfig("enable", i.isSelected());
					((Configurable) console).writeConfig("enablevideo", i.isSelected());
				}
			});
			settingsMenu.add(videoToggle);

			JCheckBoxMenuItem fps30 = new JCheckBoxMenuItem("No Limit FPS");
			fps30.setState((Integer) ((Configurable) console).readConfig("fps") > 60);
			fps30.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					JCheckBoxMenuItem i = (JCheckBoxMenuItem) evt.getSource();
					if (i.isSelected())
					{
						((Configurable) console).writeConfig("fps", 10000);
						((Configurable) audio).writeConfig("samplerate", 33334);
						
						
					}
					else
					{
						((Configurable) console).writeConfig("fps", 60);
						((Configurable) audio).writeConfig("samplerate", 33334);
					}
				}
			});
			settingsMenu.add(fps30);

			
			return settingsMenu;
		}
		
		private JMenu makeController1Menu()
		{
			JMenu controllerMenu = new JMenu();
			controllerMenu.setText("Controller 1");
			
			ButtonGroup group = new ButtonGroup();
			
			JRadioButtonMenuItem nocontrollerOption = new JRadioButtonMenuItem("NONE");
			nocontrollerOption.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					JRadioButtonMenuItem i = (JRadioButtonMenuItem) evt.getSource();
					if (i.isSelected())
					{
						controller1 = null;
						console.connect(0, controller1); // controller1
					}
				}
			});
			group.add(nocontrollerOption);
			controllerMenu.add(nocontrollerOption);
			
			for (Hardware hardware : controllers.values())
			{
				JRadioButtonMenuItem controllerOption = new JRadioButtonMenuItem(hardware.getClass().getSimpleName());
				if (controller1 != null && hardware.getClass().getName().equals(controller1.getClass().getName()))
				{
					controllerOption.setSelected(true);
				}
				controllerOption.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent evt)
					{
						JRadioButtonMenuItem i = (JRadioButtonMenuItem) evt.getSource();
						if (i.isSelected())
						{
							controller1 = controllers.get(i.getText());
							console.connect(0, controller1); // controller1
							// should set controller 2 to none?
						}
					}
				});
				group.add(controllerOption);
				controllerMenu.add(controllerOption);
			}

			return controllerMenu;
		}
		
		private JMenu makeController2Menu()
		{
			JMenu controllerMenu = new JMenu();
			controllerMenu.setText("Controller 2");
			
			ButtonGroup group = new ButtonGroup();
			
			JRadioButtonMenuItem nocontrollerOption = new JRadioButtonMenuItem("NONE");
			nocontrollerOption.setSelected(true);
			nocontrollerOption.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					JRadioButtonMenuItem i = (JRadioButtonMenuItem) evt.getSource();
					if (i.isSelected())
					{
						controller2 = null;
						console.connect(1, controller2); // controller2
					}
				}
			});
			group.add(nocontrollerOption);
			controllerMenu.add(nocontrollerOption);
			
			for (Hardware hardware : controllers.values())
			{
				JRadioButtonMenuItem controllerOption = new JRadioButtonMenuItem(hardware.getClass().getSimpleName());
				controllerOption.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent evt)
					{
						JRadioButtonMenuItem i = (JRadioButtonMenuItem) evt.getSource();
						if (i.isSelected())
						{
							controller2 = controllers.get(i.getText());
							console.connect(1, controller2); // controller2
							System.out.println("connected controller 2: "+i.getText());
						}
					}
				});
				group.add(controllerOption);
				controllerMenu.add(controllerOption);
			}

			return controllerMenu;
		}
	}

	private WindowAdapter winListener = new WindowAdapter()
	{
		public void windowClosing(WindowEvent e)
		{
			exit();
		}
	};

	public SnesSystem()
	{
		LoadConsole();
		LoadCartridge("C:\\Users\\Joel\\Documents\\JavaProjects\\roms\\Super Mario World (USA).smc");
	}
	
	public void LoadConsole()
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
					File[] files = dir.listFiles();
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
			
			ServiceLoader<Hardware> sl = ServiceLoader.load(Hardware.class, loader);
			Iterator<Hardware> it = sl.iterator();
			while (it.hasNext())
			{
				Hardware hardware = it.next();
				controllers.put(hardware.getClass().getSimpleName(), hardware);
				if (prop.getProperty("CONTROLLER", "CONTROLLER").equals(hardware.getClass().getName()))
				{
					controller1 = hardware;
				}
			}

			video = (Hardware) Class.forName(prop.getProperty("VIDEO_PLAYER", "VIDEO_PLAYER"), true, loader).newInstance();
			audio = (Hardware) Class.forName(prop.getProperty("AUDIO_PLAYER", "AUDIO_PLAYER"), true, loader).newInstance();
			if (controller1 == null) controller1 = (Hardware) Class.forName(prop.getProperty("CONTROLLER", "CONTROLLER"), true, loader).newInstance();
			console = (Hardware) Class.forName(prop.getProperty("CONSOLE", "CONSOLE"), true, loader).newInstance();
			cartridge = (Hardware) Class.forName(prop.getProperty("CARTRIDGE", "CARTRIDGE"), true, loader).newInstance();
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

		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		window = new JFrame("Super Jario ES");
		window.addWindowListener(winListener);
		window.setJMenuBar(new Jario64MenuBar());
		canvas = new Canvas();
		canvas.setPreferredSize(new java.awt.Dimension(512, 448));
		window.getContentPane().add(canvas);
		window.pack();
		window.setVisible(true);

		((Configurable) video).writeConfig("window", window);

		console.connect(3, video);
		console.connect(4, audio);
		console.connect(0, controller1);
		console.connect(1, controller2);
		
	}
	
	public void createSettingsPane()
	{
		JFrame f = new JFrame();
		JPanel p = new JPanel();
		
	}
	
	public static String SAVE_STATE_PATH="save\\";
	public static String SAVE_GAME_PATH="save\\";
	
	public void freeze()
	{
		((Configurable) console).writeConfig("done",  true);
	
		try
		{
			System.out.println("Deserialize Sleep Begin");
			Thread.sleep(500);
			System.out.println("Deserialize Sleep End");
		}
		catch(Exception e)
		{
			
		}
	}
	
	public void unfreeze()
	{
		
		((Clockable) console).clock(1L);
	}
	public void savestate()
	{
		Configurable c = ((Configurable) console);
		Bus8bit membus = (Bus8bit)c.readConfig("memory");
		
	
		String filepath = SAVE_STATE_PATH;
		String filenameNoExt = removeExtension(CARTNAME);
		int nextExtension = getNextExtension(filepath, filenameNoExt, "j");
		String filename = filenameNoExt + ".j" + nextExtension;
		String tempFile = filename + ".temp";
		
		try
		{
			FileOutputStream fileOut = new FileOutputStream(filepath + tempFile);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			
			c.writeConfig("save", out);
	
			out.close();
			fileOut.close();
			
			compressGzipFile(filepath + tempFile, filepath + filename);
			
			System.out.printf("Serialized SNES memory is saved in " + filepath + filename);
			
			File file = new File(filepath + tempFile);
			file.delete();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/*
	 * Used for numbering an extension from j0 to jN, where N can be infinite.
	 * Marker is what comes before the number, for example, .j1, the marker would be "j"
	 */
	public int getNextExtension(String filepath, String cartname, String marker)
	{
		File folder = new File(filepath);
		
		int lowest = -1;
		System.out.println("Filepath = " + filepath);
	    for (final File fileEntry : folder.listFiles()) {
	    	String filename = fileEntry.getName();
	    	String ex = filename.substring(filename.lastIndexOf('.')+1).toLowerCase();
	    	if( ex.indexOf(marker) == 0 )
	    	{
	    		String szNum = ex.substring(marker.length());
	    		try
	    		{
	    			int number = Integer.parseInt(szNum);
	    			if( number > lowest )
	    				lowest = number;
	    		}
	    		catch(Exception e) {}
	    	}
	    }
		
	    lowest++;
	    return lowest;
	}
	
	public static String getAppPath() {
		try	{ return new File(".").getCanonicalPath(); }
		catch(Exception e) {}
		return "";
	}
	
	public String removeExtension(String filepath)
	{
		return filepath.substring(0, filepath.lastIndexOf("."));
	}
	
	public void loadstate()
	{
		String filepath = SAVE_STATE_PATH;
		String filenameNoExt = removeExtension(CARTNAME);
		int nextExtension = getNextExtension(filepath, filenameNoExt, "j") - 1;
		String filename = filenameNoExt + ".j" + nextExtension;
		
	    loadstate(filepath, filename); 
	}
	
	public void loadstate(String filepath, String filename)
	{
		Configurable c = ((Configurable) console);
		
		String tempFile = filename + ".temp";
		System.out.println("Loading Save State: " + filename);
		try
		{
			decompressGzipFile( filepath + filename, filepath + tempFile);
			
			FileInputStream fileIn = new FileInputStream(filepath + tempFile);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			
			c.writeConfig("load", in);
			
			in.close();
			fileIn.close();
		
			File file = new File(filepath + tempFile);
			file.delete();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	private static void compressGzipFile(String file, String gzipFile) {
        try {
            FileInputStream fis = new FileInputStream(file);
            FileOutputStream fos = new FileOutputStream(gzipFile);
            GZIPOutputStream gzipOS = new GZIPOutputStream(fos);
            byte[] buffer = new byte[1024];
            int len;
            while((len=fis.read(buffer)) != -1){
                gzipOS.write(buffer, 0, len);
            }
            //close resources
            gzipOS.close();
            fos.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
         
    }
	
	private static void decompressGzipFile(String gzipFile, String newFile) {
        try {
            FileInputStream fis = new FileInputStream(gzipFile);
            GZIPInputStream gis = new GZIPInputStream(fis);
            FileOutputStream fos = new FileOutputStream(newFile);
            byte[] buffer = new byte[1024];
            int len;
            while((len = gis.read(buffer)) != -1){
                fos.write(buffer, 0, len);
            }
            //close resources
            fos.close();
            gis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
         
    }
	
	@Override
	public void connect(int port, Hardware hw)
	{
	}

	@Override
	public void reset()
	{
	}

	public static String CARTNAME="";
	
	private void LoadCartridge(String cartridgeName)
	{
		CARTNAME = cartridgeName;
		
		int lastSlash = cartridgeName.lastIndexOf("\\");
		CARTNAME = cartridgeName.substring(lastSlash+1, cartridgeName.length());
		SAVE_STATE_PATH = cartridgeName.substring(0, lastSlash) + "\\save\\";
		
		System.out.println("Save State Path = " + SAVE_STATE_PATH);
		System.out.println("Cart Name = " + CARTNAME);
		
		console.reset();
		cartridge.reset();
		((Configurable) cartridge).writeConfig("romfile", cartridgeName);
		System.out.println("opened: " + cartridgeName);
		console.connect(2, cartridge);
		((Clockable) console).clock(1L); // starts cpu thread
		
		((Configurable) console).writeConfig("fps", 60);
		((Configurable) audio).writeConfig("samplerate", 33334);
	}

	private void exit()
	{
		// remove cartridge
		//console.connect(2, null);
		//console.reset();
		if (cartridge != null)
		{
			// save cartridge data
			//cartridge.reset();
		}
		System.exit(0);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		//video.destroy();
		//audio.destroy();
		//cartridge.destroy();
		//console.destroy();
	}
}
