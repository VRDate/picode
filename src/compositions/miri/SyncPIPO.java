package compositions.miri;

import controller.network.SendToPI;
import pi.dynamic.DynamoPI;
import core.PIPO;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.Glide;

public class SyncPIPO implements PIPO {

	@Override
	public void action(DynamoPI d) {
		
		//choose sample
		d.put("s", "synch1");

		//set time to zero
		d.put("p", new Long(0));
		
		//turn off loop
		Envelope looperStart = (Envelope)d.get("loopStart");
		looperStart.setValue(-1); 

		//turn off effect
		Envelope rwet = (Envelope)d.get("rwet");
		rwet.setValue(0);
		
	}

	public static void main(String[] args) throws Exception {
		String fullClassName = Thread.currentThread().getStackTrace()[1].getClassName().replace(".", "/");
		SendToPI.send(fullClassName, Recipients.list);
	}

}