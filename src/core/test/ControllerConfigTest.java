package core.test;

import core.ControllerConfig;
import junit.framework.TestCase;

public class ControllerConfigTest extends TestCase {
	protected ControllerConfig env;

	protected void setUp() throws Exception {
		super.setUp();
		env = new ControllerConfig();
	}
	
	public void testMyHostname() {
		String myHostname = env.getMyHostName();
		assertTrue(myHostname != null);
		assertTrue(!myHostname.isEmpty());
	}
	
	public void testControllerHostname() {
		assertTrue(env.getMyHostName() == env.getControllerHostname());
	}

}