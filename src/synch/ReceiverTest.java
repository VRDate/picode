package synch;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class ReceiverTest {

	public static void main(String[] args) throws IOException {
		MulticastSocket s = new MulticastSocket(5000);
		System.out.println("Created socket");
		s.joinGroup(InetAddress.getByName("225.4.5.6"));
		System.out.println("Joined group");
		byte buf[] = new byte[1024];
		DatagramPacket pack = new DatagramPacket(buf, buf.length);
		s.receive(pack);
		System.out.println("got data");
		
	}
	
}
