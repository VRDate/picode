package net.happybrackets.core.misc_tests;

import core.EnvironmentConf;
import junit.framework.TestCase;

public class EnvironmentConfTest extends TestCase {
	protected EnvironmentConf env;
	
	protected void setUp() {
		env = new EnvironmentConf(){
			
		};
	}
	
	public void testMyHostname() {
		String myHostname = env.getMyHostName();
		assertTrue(myHostname != null);
		assertFalse( myHostname.isEmpty() );
	}
}
