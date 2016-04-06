package core;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public interface ControllerDiscoverer {
	
	default String listenForController(String multicastAddress, int multicastPort) throws UnknownHostException {
		byte[] buf = new byte[256];
		String controllerHostname = null;
		
		try ( MulticastSocket clientSocket = new MulticastSocket(multicastPort) ) {
			clientSocket.joinGroup( InetAddress.getByName(multicastAddress) );
			
			while (controllerHostname == null) {
				DatagramPacket msgPacket = new DatagramPacket(buf, buf.length);
				clientSocket.receive(msgPacket);
				
				//String msg = new String(buf, 0, buf.length).trim();
				//System.out.println("Recieved message: " + msg);
				
				//String[] msgParts = msg.split(" ");
				String[] msgParts = new String(buf, 0, buf.length).trim().split(" ");
				
				if ( msgParts.length == 2 && msgParts[0].equals("controllerHostname:") ) {
					if (InetAddress.getByName(controllerHostname).isReachable(500)) {
						controllerHostname = msgParts[1];
					}
					else {
						System.out.println("Unable to reach controllerHostname: " + msgParts[1]);
					}
				}
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		
		return controllerHostname;
	}

}
