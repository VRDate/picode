package core;

import java.util.HashSet;
import java.util.Set;


public abstract class Config {

	//hosts and ports for network messages
	public final static String controllerHostname  	= "boing.local"; 
	public final static String multicastSynchAddr   = "225.2.2.5";
	public final static int statusFromPIPort 		= 2223;
	public final static int clockSynchPort			= 2224;
	public final static int codeToPIPort			= 2225;
	public final static int controlToPIPort			= 2226;
	
	//how often the PI sends an alive message to the server
	public static final int aliveInterval = 1000;   		
	
	//places
	public final static String workingDir = ".";
	public final static String audioDir = workingDir + "/audio";
	public final static String knownPIsFile = workingDir + "/config/known_pis";
	
}
