package compositions.bowls2015;

import java.util.Hashtable;
import java.util.Map;

import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.events.KillTrigger;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.Noise;
import net.beadsproject.beads.ugens.WavePlayer;
import pi.dynamic.DynamoPI;
import pi.sensors.MiniMU.MiniMUListener;
import controller.network.SendToPI;
import core.EZShell;
import core.PIPO;
import core.Synchronizer.BroadcastListener;

/**
 * 
 * TODO:
 * 
 *  > how does the Pi get its ID?? perhaps last 4 digits of hostname?
 *  > check different states, broadcast states, work out who is being played
 *  
 *  
 *  GAME RULES:
 *  
 *  > When all balls are still, it is the end of game, 
 *  > when all balls are moving, it is the transition.
 *  
 *  > 
 * 
 * 
 * @author ollie
 *
 */


public class BowlsGameMain implements PIPO {
	
	
	
	public static void main(String[] args) throws Exception {
		String fullClassName = Thread.currentThread().getStackTrace()[1].getClassName().replace(".", "/");
		System.out.println("The path will be run: " + fullClassName);
		SendToPI.send(fullClassName, new String[]{
			"localhost"
			});
	}
	
	final static int NUM_BALLS = 2;
	
	public enum MovementState {
		UNKNOWN, STILL, ROLLING, SLIGHT, FREEFALL, UP
	}
	
	public enum Team {
		BLACK, WHITE, WOOD, GREEN
	}
	
	final static Map<String, Team> teams;
	static {
		teams = new Hashtable<String, Team>();
		teams.put("47ef", Team.BLACK);
		teams.put("502d", Team.BLACK);
		teams.put("3fb2", Team.WHITE);
		teams.put("4eec", Team.WHITE);
		teams.put("50e2", Team.WOOD);
		teams.put("5093", Team.WOOD);
	}
	
	private static final long serialVersionUID = 1L;

	int[] blues = {0, 3, 5, 6, 7, 10, 12, 15, 17, 18, 19, 22, 24, 27, 29, 30, 31, 34, 36, 39, 41, 42, 43, 46, 48};
	int[] mel = {0, 0, 0, 1, 2, 3, 0, 0, 0, 1, 2, 3, 0, 3, 3, 1, 2, 4, 4, 4, 3, 2, 1, 12, 13, 14, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 11, 11, 11, 12, 9, 7, 5, 3, 2, 0, 0};
	int[] offsets = {0, 5, 7, 12, 17, 19, 24, 29};
	
	String myID;
	Map<String, MovementState> movementStates;
	
	
	double upness = 0;
	int numStill = 0;
	int numUp = 0;
	
	DynamoPI d;
	
	
	//audio stuff
	
	
	@Override
	public void action(final DynamoPI d) {
		this.d = d;
		String hostname = EZShell.call("hostname");
		if(hostname != null) {
			myID = hostname.substring(hostname.length() - 5, hostname.length() - 1);
		} else {
			myID = "" + d.rng.nextInt(10) + "" + d.rng.nextInt(10) + "" + d.rng.nextInt(10) + "" + d.rng.nextInt(10);
		}
		
		System.out.println("My ID is: " + myID);
		
		movementStates = new Hashtable<String, MovementState>();
		movementStates.put(myID, MovementState.UNKNOWN);
		
		//Let the game begin
		
		
		d.pattern(new Bead() {
			public void messageReceived(Bead msg) {
				//TODO ?? Do something with a pattern??
			}
		});
		
		d.mu.addListener(new MiniMUListener() {
			
			@Override
			public void imuData(double x, double y, double z, double x2, double y2, double z2, double x3, double y3, double z3) {
				//backup state
				MovementState oldState = movementStates.get(myID);
				//accel
				double daccel = Math.sqrt(x*x + y*y + z*z);
				upness = z / 4000.;
				if(upness > 1) upness = 1; else if(upness < -1) upness = -1;
				if(daccel < 450 && oldState != MovementState.FREEFALL) {
					movementStates.put(myID, MovementState.FREEFALL);
					startFreefallSound();
				}
				if(daccel > 10000) {
					impact();
				}
				//gyro data
				double dgyro = Math.sqrt(x2*x2 + y2*y2 + z2*z2);
//				System.out.println("d=" + d);
				if(dgyro > 9100 && oldState != MovementState.ROLLING) {
					movementStates.put(myID, MovementState.ROLLING);
					startRollingSound();
				} else if(dgyro < 100 && oldState != MovementState.STILL) {
					movementStates.put(myID, MovementState.STILL);
					startStillSound();
				} else if(movementStates.get(myID) != MovementState.FREEFALL) {
					MovementState newState = MovementState.SLIGHT;
					if(upness > 0.7f) {
						newState = MovementState.UP;
					}
					movementStates.put(myID, newState);
					if(newState != oldState) {		//works for both UP and SLIGHT
						startSlightSound();
					} 				
				}
				System.out.println(movementStates.get(myID));
				//send only if there is a change
				MovementState newState = movementStates.get(myID);
				if(oldState != newState) {
					//every so often send
					String result;
					if(newState == MovementState.SLIGHT) {
						result = "mstate " + myID + " " + newState + " " + upness + " ";
					} else {
						result = "mstate " + myID + " " + newState + " ";
					}
					d.synch.broadcast(result);
					try {
						d.setStatus(newState.toString());
					} catch(Exception e) {
						//snub
					}
				}
			}
			
		});
		
		//use d.synch.broadcast("message"); to broadcast things
		//use d.synch.doAtTime(Runnable, time) to make something happen at a synchronised time, the 'time' variable should synch across machines
		//use d.synch.addBroadcastListener(new BroadcastListener()); to listen to things
		d.synch.addBroadcastListener(new BroadcastListener() {
			@Override
			public void messageReceived(String s) {
				String[] components = s.split("[ ]");
				if(components[0].equals("mstate")) {
					movementStates.put(components[1], MovementState.valueOf(components[2]));
				}
//				System.out.println("msg received: " + s);
				if(movementStates.size() == NUM_BALLS) {
					int prevNumStill = numStill;
					int prevNumUp = numUp;
					numStill = 0;
					numUp = 0;
					for(MovementState ms : movementStates.values()) {
						if(ms == MovementState.STILL) {
							numStill++;
						} else if(ms == MovementState.UP) {
							numUp++;
						}
					}
					if(prevNumStill != numStill && numStill == NUM_BALLS) {
						endGame();
					} else if(prevNumUp != numUp && numUp == NUM_BALLS) {
						allup();
					}
//					System.out.println("Num still: " + numStill);	
				}
			}
		});
	}
	
	void startStillSound() {
		//TODO
	}
	
	void startSlightSound() {
		//TODO
	}
	
	void startFreefallSound() {
		//TODO
	}
	
	void startRollingSound() {
		//TODO
	}
	
	void impact() {
		//TODO
	}
	
	void allup() {
		//TODO
	}
	
	void endGame() {
		//TODO
	}
	
	
}
