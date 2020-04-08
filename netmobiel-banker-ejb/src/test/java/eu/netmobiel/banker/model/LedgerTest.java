package eu.netmobiel.banker.model;

import static org.junit.Assert.*;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

public class LedgerTest {

	@Test
	public void testFitsPeriod_Closed() {
    	String startTime = "2019-01-01T01:00:00Z";
    	String endTime = "2020-01-01T01:00:00Z";
		Ledger ledger = new Ledger();
    	ledger.setStartPeriod(Instant.parse(startTime));
    	ledger.setEndPeriod(Instant.parse(endTime));
    	ledger.setName(String.format("%d", ledger.getStartPeriod().atOffset(ZoneOffset.UTC).getYear()));

    	assertTrue(ledger.fitsPeriod(Instant.parse(startTime)));
    	assertFalse(ledger.fitsPeriod(Instant.parse(endTime)));
    	assertTrue(ledger.fitsPeriod(Instant.parse("2019-08-01T01:00:00Z")));
    	assertFalse(ledger.fitsPeriod(Instant.parse("2018-08-01T01:00:00Z")));
	}

	@Test
	public void testFitsPeriod_Open() {
    	String startTime = "2019-01-01T01:00:00Z";
		Ledger ledger = new Ledger();
    	ledger.setStartPeriod(Instant.parse(startTime));
    	ledger.setName(String.format("%d", ledger.getStartPeriod().atOffset(ZoneOffset.UTC).getYear()));

    	assertTrue(ledger.fitsPeriod(Instant.parse(startTime)));
    	assertTrue(ledger.fitsPeriod(Instant.parse("2019-08-01T01:00:00Z")));
    	assertFalse(ledger.fitsPeriod(Instant.parse("2018-08-01T01:00:00Z")));
    	assertTrue(ledger.fitsPeriod(Instant.parse("2020-08-01T01:00:00Z")));
	}
}
