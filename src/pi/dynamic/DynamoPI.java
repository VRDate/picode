package pi.dynamic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Random;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.events.KillTrigger;
import net.beadsproject.beads.ugens.Clock;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.PolyLimit;
import net.beadsproject.beads.ugens.WavePlayer;
import pi.network.ControllerConnection;
import pi.sensors.MiniMU;
import core.AudioSetup;
import core.Config;
import core.PIPO;
import core.Synchronizer;
import de.sciss.net.OSCMessage;

public class DynamoPI {
	

	// audio stuffs
	public final AudioContext ac;
	public final Clock clock;
	public final Envelope clockInterval;
	public final PolyLimit pl;
	public final Envelope masterGainEnv;
	boolean audioOn = false;

	// sensor stuffs
	public final MiniMU mu;

	// shared data
	public final Hashtable<String, Object> share = new Hashtable<String, Object>();
	int nextElementID = 0;

	// random number generator
	public final Random rng = new Random();

	// network stuff
	public ControllerConnection controller;
	public Synchronizer synch;

	public static void main(String[] args) throws IOException {
		DynamoPI pi = new DynamoPI(AudioSetup.getAudioContext(args));
		if(args.length > 4) {
			boolean autostart = Boolean.parseBoolean(args[4]);
			System.out.println(args[4] + " is interpreted as " + autostart);
			if(autostart) {
				System.out.println("Detected autostart. Starting audio right away.");
				pi.startAudio();
			}
		}
	}

	public DynamoPI(AudioContext _ac) throws IOException {
		ac = _ac;
		// default audio setup (note we don't start the audio context yet)
		masterGainEnv = new Envelope(ac, 0);
		masterGainEnv.addSegment(1, 5000);
		ac.out.setGain(masterGainEnv);
		clockInterval = new Envelope(ac, 500);
		clock = new Clock(ac, clockInterval);
		pl = new PolyLimit(ac, 1, 4);
		pl.setSteal(true);
		ac.out.addInput(pl);
		ac.out.addDependent(clock);
		System.out.println("DynamoPI audio setup complete.");
		// sensor setup
		mu = new MiniMU();
		mu.start();
		// start the connection
		controller = new ControllerConnection();
		synch = Synchronizer.get();
		//start various other listeners
		startCommandListener();
		// start listening for code
		startListeningForCode();
	}
	
	private void startCommandListener() {
		ControllerConnection.Listener commandListener = new ControllerConnection.Listener() {
			@Override
			public void msg(OSCMessage msg) {
				//master commands...
				if(msg.getName().equals("/PI/sync")) {
					sync((Long)msg.getArg(0));
				} else if(msg.getName().equals("/PI/reboot")) {
					rebootPI();
				} else if(msg.getName().equals("/PI/shutdown")) {
					shutdownPI();
				} else if(msg.getName().equals("/PI/gain")) {
					masterGainEnv.addSegment((Float)msg.getArg(0), (Float)msg.getArg(1));
				} else if(msg.getName().equals("/PI/reset")) {
					reset();
				} else if(msg.getName().equals("/PI/reset_sounding")) {
					resetLeaveSounding();
				} else if(msg.getName().equals("/PI/clearsound")) {
					clearSound();
				} else if(msg.getName().equals("/PI/fadeout_reset")) {
					fadeOutReset((Float)msg.getArg(0));
				} else if(msg.getName().equals("/PI/fadeout_clearsound")) {
					fadeOutClearSound((Float)msg.getArg(0));
				} 
				
			}
		};
		controller.addListener(commandListener);
	}
	
	private void sync(long time) {
		synch.doAtTime(new Runnable() {
			public void run() {
				startAudio();
			}
		}, time);
	}
	
	public void startAudio() {
		ac.start();
		audioOn = true;
		testBleep();
	}
	
	public void testBleep() {
		Envelope e = new Envelope(ac, 0);
		Gain g = new Gain(ac, 1, e);
		WavePlayer wp = new WavePlayer(ac, 500, Buffer.SINE);
		g.addInput(wp);
		pl.addInput(g);
		e.addSegment(0, 1000);
		e.addSegment(0.4f, 0);
		e.addSegment(0.4f, 300);
		e.addSegment(0f, 10);
		e.addSegment(0f, 400);
		e.addSegment(0.4f, 0);
		e.addSegment(0.4f, 300);
		e.addSegment(0f, 10);
		e.addSegment(0f, 400);
		e.addSegment(0.4f, 0);
		e.addSegment(0.4f, 600);
		e.addSegment(0, 10, new KillTrigger(g));
	}

	private void startListeningForCode() {
		new Thread() {
			public void run() {
				try {
					// socket server (listens to incoming classes)
					DynamoClassLoader loader = new DynamoClassLoader(ClassLoader.getSystemClassLoader());
					ServerSocket server = new ServerSocket(Config.codeToPIPort);
					// start socket server listening loop
					while (true) {
						// must reopen socket each time
						Socket s = server.accept();
						Class<? extends PIPO> pipoClass = null;
						try {
							InputStream input = s.getInputStream();
							ByteArrayOutputStream buffer = new ByteArrayOutputStream();
							int data = input.read();
							while (data != -1) {
								buffer.write(data);
								data = input.read();
							}
							byte[] classData = buffer.toByteArray();
							Class<?> c = loader.createNewClass(classData);
							Class<?>[] interfaces = c.getInterfaces();
							boolean isPIPO = false;
							for (Class<?> cc : interfaces) {
								if (cc.equals(PIPO.class)) {
									isPIPO = true;
									break;
								}
							}
							if (isPIPO) {
								pipoClass = (Class<? extends PIPO>) c;
								System.out.println("new PIPO >> " + pipoClass.getName());
								// this means we're done with the sequence, time
								// to
								// recreate
								// the classloader to avoid duplicate errors
								loader = new DynamoClassLoader(ClassLoader.getSystemClassLoader());
							}
						} catch (Exception e) {/* snub it? */
							System.out.println("Exception Caught trying to read Object from Socket");
							e.printStackTrace();
						}
						if (pipoClass != null) {
							PIPO pipo = null;
							try {
								pipo = pipoClass.newInstance();
								pipo.action(DynamoPI.this);
							} catch (Exception e) {
								e.printStackTrace(); // catching all exceptions
													 // means that we avert an exception
													 // heading up to audio processes.
							}
						}
						s.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();

	}

	public void put(String s, Object o) {
		share.put(s, o);
	}

	public Object get(String s) {
		return share.get(s);
	}

	public int getInt(String s) {
		return (Integer) share.get(s);
	}

	public float getFloat(String s) {
		return (Float) share.get(s);
	}

	public String getString(String s) {
		return (String) share.get(s);
	}

	public UGen getUGen(String s) {
		return (UGen) share.get(s);
	}

	public Bead getBead(String s) {
		return (Bead) share.get(s);
	}

	public String pattern(Bead pattern) {
		clock.addMessageListener(pattern);
		String name = "pattern" + nextElementID++;
		put(name, pattern);
		System.out.println(name);
		return name;
	}

	public String sound(UGen snd) {
		pl.addInput(snd);
		String name = "snd" + nextElementID++;
		put(name, snd);
		System.out.println(name);
		return name;
	}
	
	/**
	 * Warning, this leaves dependents etc. Just cleans the audio signal chain.
	 */
	public void clearSound() {
		//rebuilt top elements of signal chain
		ac.out.clearInputConnections();
		ac.out.addInput(pl);
		pl.clearInputConnections();
	}

	public void reset() {
		resetLeaveSounding();
		clearSound();
	}

	// This is like reset() except that any sounds currently playing are kept.
	public void resetLeaveSounding() {
		//clear dependencies and inputs
		ac.out.clearDependents();
		ac.out.addDependent(clock);
		clock.clearMessageListeners();
		clock.clearInputConnections();
		clock.clearDependents();
		pl.clearDependents();
		//clear data store
		share.clear();
		//clear mu listeners
		mu.clearListeners();
		//clear osc listeners
		controller.clearListeners();
		//re-connect the main command listener
		startCommandListener();
	}

	public void fadeOutClearSound(float fadeTime) {
		masterGainEnv.addSegment(0, fadeTime, new Bead() {
			public void messageReceived(Bead message) {
				clearSound();
				masterGainEnv.addSegment(1, 10);
			}
		});
	}
	
	public void fadeOutReset(float fadeTime) {
		masterGainEnv.addSegment(0, fadeTime, new Bead() {
			public void messageReceived(Bead message) {
				reset();
				masterGainEnv.addSegment(1, 10);
			}
		});
	}
	
	public int myIndex() {
		return controller.getID();
	}
	
	//reboots the PI
	public static void rebootPI() {
		try {
			Runtime.getRuntime().exec(new String[]{"/bin/bash","-c","sudo reboot"}).waitFor();
		} catch (Exception e) {}
	}
	
	//shuts down the PI
		public static void shutdownPI() {
			try {
				Runtime.getRuntime().exec(new String[]{"/bin/bash","-c","sudo shutdown now"}).waitFor();
			} catch (Exception e) {}
		}

}


