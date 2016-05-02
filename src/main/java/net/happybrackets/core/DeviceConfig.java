package net.happybrackets.core;

import net.happybrackets.core.ControllerDiscoverer;

import java.net.UnknownHostException;

public class DeviceConfig extends LoadableConfig implements EnvironmentConf, ControllerDiscoverer {
	private String controllerHostName;

	public synchronized String getControllerHostname() {
		if (controllerHostName != null) {
			return controllerHostName;
		}
		
		//Block and search for a controller, we need a logging framework too now :/
		try {
			controllerHostName = listenForController( getMulticastSynchAddr(), getControllerDiscoveryPort());
		} catch (UnknownHostException e) {
			System.out.println("Error obtaining controller hostname.");
			e.printStackTrace();
		}
		return controllerHostName;
	}
	
	public int getMyId() {
		return 0;
	}

}
