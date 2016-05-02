package net.happybrackets.device;

import net.happybrackets.core.DeviceConfig;
import net.happybrackets.core.LoadableConfig;

public class ManagerAgent {

	public static void main(String[] args) {
		DeviceConfig env = new DeviceConfig();
		env = LoadableConfig.load("config/device-config.json", env);
		System.out.println("PI Manager Agent Started as: " + env.getMyHostName());
		System.out.println("Listening for Controller...");
		System.out.println("Found controller on host: " + env.getControllerHostname());
		
		System.out.println("Requesting configuration settings from controller...");

		System.out.println("PI Manager Agent Exiting");
	}

}
