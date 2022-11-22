package eu.netmobiel.overseer.processor;

import static org.junit.Assert.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.netmobiel.commons.report.ActivityReport;
import eu.netmobiel.overseer.model.ActivitySpssReport;

public class ReportProcessorTest {

	private List<ActivityReport> report; 
	private ReportProcessor processor;
	private ZonedDateTime since;
	private ZonedDateTime until;
	@Before
	public void setUp() throws Exception {
		since = ZonedDateTime.parse("2020-04-01T00:00:00+01:00[Europe/Amsterdam]");
		until = ZonedDateTime.parse("2020-08-01T00:00:00+01:00[Europe/Amsterdam]");
		processor = new ReportProcessor();
		report = new ArrayList<>();
		ActivityReport ar = new ActivityReport("A", 2020, 4);
		ar.setMessageCount(44);
		report.add(ar);
		ar = new ActivityReport("B", 2020, 5);
		ar.setMessageCount(55);
		report.add(ar);
		ar = new ActivityReport("B", 2020, 6);
		ar.setMessageCount(66);
		report.add(ar);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testPlainReport() {
		try {
			String s = processor.convertToCsv(report, ActivityReport.class);
			System.out.println(s);

		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.toString());
		}
		
	}

	@Test
	public void testSpssReport() {
		try {
			Collection<ActivitySpssReport> spssReport = processor.createSpssReport(report, ActivitySpssReport.class); 
			String s = processor.convertToCsvforSpss(spssReport, ActivitySpssReport.class, since, until);
			System.out.println(s);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.toString());
		}
		
	}
}
