package server.my_pipos.miri;

import pi.dynamic.DynamoPI;
import server.network.SendToPI;
import core.PIPO;
import net.beadsproject.beads.ugens.Envelope;

public class StopPIPO implements PIPO {

	@Override
	public void action(DynamoPI d) {
		d.share.remove("s");
		final Envelope rwet = (Envelope)d.get("rwet");
		rwet.setValue(0);
		((Envelope)d.get("masterGain")).clearDependents();
	}


	public static void main(String[] args) throws Exception {
		String fullClassName = Thread.currentThread().getStackTrace()[1].getClassName().replace(".", "/");
		SendToPI.send(fullClassName, Recipients.list);
	}

}