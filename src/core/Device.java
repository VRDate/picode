package core;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Scanner;

public abstract class Device {

	public static final String myHostname;						//the hostname for this PI (wifi)
	public static final String myMAC;							//the wlan MAC for this PI (wifi)

	static {
		String tmpHostname = null;
		String tmpMAC = null;
		try {
			NetworkInterface netInterface;
			System.out.println("Detected OS: " + System.getProperty("os.name"));
			if (System.getProperty("os.name").startsWith("Mac OS")) {
				netInterface = NetworkInterface.getByName("en1");
				//if you can't get the wlan then get the ethernet mac address:
				if(netInterface == null) {
					netInterface = NetworkInterface.getByName("en0");
				}
			}
			else if (System.getProperty("os.name").startsWith("Windows")) {
				System.out.println("Interfaces:");
				Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
				String favouriteInterfaceName = null;
				while (interfaces.hasMoreElements()) {
					netInterface = interfaces.nextElement();
					
					// Windows by default has a lot of extra interfaces,
					//  lets at least try and get a real interface...
					if (isViableNetworkInterface(netInterface)) {
						favouriteInterfaceName = netInterface.getName();
						System.out.println("I like: " + favouriteInterfaceName + ", " + netInterface.getDisplayName());
					}
					else {
						System.out.println("Ignored: " + netInterface.getName() + ", " + netInterface.getDisplayName());
					}
				}
				
				if (favouriteInterfaceName != null ) {
					netInterface = NetworkInterface.getByName(favouriteInterfaceName);
				}
				else {
					netInterface = NetworkInterface.getByName("wlan0"); // take a stab in the dark...
				}
				
				System.out.println("Selected interface: " + netInterface.getName() + ", " + netInterface.getDisplayName());
				
				//while there isn't a clear way to install zeroconf without itunes on windows let's use their IP as the hostname
				tmpHostname = netInterface.getInetAddresses().nextElement().getHostAddress();
				System.out.println("IP: " + tmpHostname);
			}
			else {
				netInterface = NetworkInterface.getByName("wlan0");
				if (netInterface == null) {
					netInterface = NetworkInterface.getByName("eth0");
				}
			}
			
			if(netInterface != null) {
				byte[] mac = netInterface.getHardwareAddress();
				StringBuilder builder = new StringBuilder();
				for (byte a : mac) {
					builder.append(String.format("%02x", a));
				}
				tmpMAC = builder.substring(0, builder.length());
			}
			//first attempt at hostname is to query the /etc/hostname file which should have
			//renamed itself (on the PI) before this Java code runs
			try {
				Scanner s = new Scanner(new File("/etc/hostname"));
				String line = s.next();
				if (line != null && !line.isEmpty() && !line.endsWith("-")) {
					tmpHostname = line;
				}
				s.close();
			} catch(Exception e) {/*Swallow this exception*/}
			//if we don't have the mac derive the MAC from the hostname
			if(tmpMAC == null && tmpHostname != null) {
				tmpMAC = tmpHostname.substring(8, 20);
			}
			//if we don't have the hostname get by traditional means
			// Windows seems to like this one.
			if(tmpHostname == null) {
				try {
					tmpHostname = InetAddress.getLocalHost().getHostName();
				}
				catch (UnknownHostException e) {
					System.out.println("Unable to find host name, resorting to IP Address");
					e.printStackTrace();
				}
			}
			
			//If everything still isn't working lets try via our interface for an IP address
			if (tmpHostname == null) {
				String address = netInterface.getInetAddresses().nextElement().getHostAddress();
				//strip off trailing interface name if present
				if (address.contains("%")) {
					tmpHostname = address.split("%")[0];
				}
				else {
					tmpHostname = address;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//ensure we have a local suffix
		// Windows won't care either way but *nix systems need it
		//If there are ':' we are probably dealing with a IPv6 address
		if (tmpHostname != null && !tmpHostname.contains(".") && !tmpHostname.contains(":")) {
			tmpHostname += ".local";	//we'll assume a .local extension is required if no extension exists
		}
		
		myHostname = tmpHostname;
		myMAC = tmpMAC;
		//report
		System.out.println("My hostname is: " + myHostname);
		System.out.println("My wlan MAC address is: " + myMAC);
	}
	
	public static boolean isViableNetworkInterface(NetworkInterface ni) {
		try {
			if ( !ni.supportsMulticast()						) return false;
			if ( ni.isLoopback()								) return false;
			if ( !ni.isUp()										) return false;
			if ( ni.isVirtual()									) return false;
			if ( ni.getDisplayName().matches(".*[Vv]irtual.*")	) return false; //try and catch out any interfaces which don't admit to being virtual
		} catch (SocketException e) {
			System.out.println("Error checking interface " + ni.getName());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static void main(String[] args) {
		//static code above will run
		@SuppressWarnings("unused")
		String x = Device.myHostname;
	}
	
}
