package net.happybrackets.compositions;

import core.DynamoAction;
import de.sciss.net.OSCMessage;
import device.dynamic.Dynamo;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.WavePlayer;
import device.network.NetworkCommunication;

/**
 * Created by Ollie on 18/08/15.
 */
public class ATest implements DynamoAction {


    @Override
    public void action(final Dynamo d) {
        System.out.println("Hello world");


        d.communication.addListener(new NetworkCommunication.Listener() {
            @Override
            public void msg(OSCMessage msg) {
                if (msg.getName().equals("on")) {


                    Envelope e = new Envelope(d.ac, 100);
                    d.sound(new WavePlayer(d.ac, e, Buffer.SINE));
                    e.addSegment(500, 10000);
                }
            }
        });
    }
}
