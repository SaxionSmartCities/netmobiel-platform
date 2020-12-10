package eu.netmobiel.overseer.processor;

import static org.junit.Assert.*;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.netmobiel.communicator.model.ActivityReport;

public class ReportProcessorTest {

	private List<ActivityReport> report; 
	private ReportProcessor processor;
	@Before
	public void setUp() throws Exception {
		processor = new ReportProcessor();
		report = new ArrayList<>();
		ActivityReport ar = new ActivityReport("A", 2020, 4);
		report.add(ar);
		ar = new ActivityReport("B", 2020, 5);
		report.add(ar);
		ar = new ActivityReport("B", 2020, 6);
		report.add(ar);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		try {
			Writer w = processor.convertToCsv(report);
			System.out.println(w.toString());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.toString());
		}
		
	}

}
