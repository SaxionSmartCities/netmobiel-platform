package eu.netmobiel.banker.rest.sepa;

import static org.junit.Assert.*;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SepaFormatTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSepaIdentifier() {
		assertEquals("Batch-01.1", SepaFormat.identifier("Bätch-01.1"));
		assertEquals(StringUtils.rightPad("Batch-01.1", SepaFormat.MAX_LENGTH_IDENTIFIER, 'x'), 
				 	SepaFormat.identifier(StringUtils.rightPad("Batch-01.1", SepaFormat.MAX_LENGTH_IDENTIFIER + 5, 'x')));
		assertEquals("Batch-01.1OK", SepaFormat.identifier("Batch-01.1|OK"));
	}

	@Test
	public void testSepaText() {
		assertEquals("Hor mich an", SepaFormat.text("Hör mich an"));
		assertEquals("Hor mich an Oder", SepaFormat.text("Hör mich an | Oder"));
		assertEquals(StringUtils.rightPad("myText", SepaFormat.MAX_LENGTH_TEXT, 'x'), SepaFormat.text(StringUtils.rightPad("myText", SepaFormat.MAX_LENGTH_TEXT + 5, 'x')));
	}
}
