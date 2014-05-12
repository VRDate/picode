package server.my_pipos.breaks;

import pi.dynamic.DynamoPI;
import server.network.SendToPI;
import core.PIPO;
import net.beadsproject.beads.data.SampleManager;

public class PIPOLoadSound implements PIPO {

	private static final long serialVersionUID = 1L;

	public static void main(String[] args) throws Exception {
		String fullClassName = Thread.currentThread().getStackTrace()[1].getClassName().replace(".", "/");
		SendToPI.send(fullClassName, new String[] {"localhost"});
	}
	
	@Override
	public void action(final DynamoPI d) {
		SampleManager.sample("amen", "audio/amen-175.aif");
		SampleManager.sample("pianoA", "audio/piano.ff.A2.aiff");
	}
	
}