package net.happybrackets.core.misc_tests;

import static org.junit.Assert.*;

import core.DeviceConfig;
import org.junit.Before;
import org.junit.Test;

public class PIConfigTest {
	protected DeviceConfig env;
	
	@Before
	public void setUp() throws Exception {
		env = new DeviceConfig();
	}

	@Test
	public void myHostNameTest() {
		String myHostname = env.getMyHostName();
		assertTrue(myHostname != null);
		assertFalse( myHostname.isEmpty() );
	}

}
