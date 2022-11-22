package eu.netmobiel.payment.client;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PaymentClientTest {
	private PaymentClient client;
	
	@Before
	public void setUp() throws Exception {
		client = new PaymentClient();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDummy() {
		assertNotNull(client);
	}

}
