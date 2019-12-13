package eu.netmobiel.rideshare.model;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class RecurrenceIteratorTest {
	private static final int MAX_DATES = 100;

	protected List<LocalDate> generate(RecurrenceIterator rix) {
		List<LocalDate> dates = new ArrayList<>();
		while (rix.hasNext()) {
			dates.add(rix.next());
			if (dates.size() > MAX_DATES) {
				throw new IllegalStateException("Iterator does not stop");
			}
		}
		return dates;
	}

	@Test
	public void testDayHorizonTomorrow() {
		LocalDate reference = LocalDate.now();
		LocalDate horizon = reference.plusDays(1);
		int interval = 1;
		Recurrence r = new Recurrence(interval);
		RecurrenceIterator rix = new RecurrenceIterator(r, reference, reference, horizon);
		List<LocalDate> dates = generate(rix);
		assertEquals(1, dates.size());
		assertEquals(reference, dates.get(0));
	}

	@Test
	public void testDayHorizonTomorrowOverride() {
		LocalDate reference = LocalDate.now();
		LocalDate horizon = reference.plusDays(1);
		int interval = 1;
		Recurrence r = new Recurrence(interval, horizon);
		RecurrenceIterator rix = new RecurrenceIterator(r, reference, reference, horizon.plusDays(10));
		List<LocalDate> dates = generate(rix);
		assertEquals(1, dates.size());
		assertEquals(reference, dates.get(0));
	}

	@Test
	public void testDayOffset() {
		LocalDate reference = LocalDate.now();
		LocalDate horizon = reference.plusDays(3);
		int interval = 1;
		Recurrence r = new Recurrence(interval);
		RecurrenceIterator rix = new RecurrenceIterator(r, reference, reference.plusDays(2), horizon);
		List<LocalDate> dates = generate(rix);
		assertEquals(1, dates.size());
		assertEquals(reference.plusDays(2), dates.get(0));
	}

	@Test
	public void testDayHorizonNow() {
		LocalDate reference = LocalDate.now();
		LocalDate horizon = reference.plusDays(0);
		int interval = 1;
		Recurrence r = new Recurrence(interval);
		RecurrenceIterator rix = new RecurrenceIterator(r, reference, reference, horizon);
		List<LocalDate> dates = generate(rix);
		assertEquals(0, dates.size());
	}

	@Test
	public void testDayInterval2HorizonFortnight() {
		LocalDate reference = LocalDate.now();
		LocalDate horizon = reference.plusDays(14);
		int interval = 2;
		Recurrence r = new Recurrence(interval);
		RecurrenceIterator rix = new RecurrenceIterator(r, reference, reference, horizon);
		List<LocalDate> dates = generate(rix);
		assertEquals(7, dates.size());
		for (int i = 0; i < dates.size() - 1; i++) {
			assertEquals(dates.get(i).plusDays(interval), dates.get(i + 1));
			
		}
	}

	@Test
	public void testDayInterval2HorizonOverride() {
		LocalDate reference = LocalDate.now();
		LocalDate horizon = reference.plusDays(14);
		int interval = 2;
		Recurrence r = new Recurrence(interval, reference.plusDays(7));
		RecurrenceIterator rix = new RecurrenceIterator(r, reference, reference, horizon);
		List<LocalDate> dates = generate(rix);
		assertEquals(4, dates.size());
		for (int i = 0; i < dates.size() - 1; i++) {
			assertEquals(dates.get(i).plusDays(interval), dates.get(i + 1));
			
		}
	}

	@Test
	public void testWeekHorizonNow() {
		LocalDate reference = LocalDate.now();
		LocalDate horizon = reference.plusWeeks(0);
		int interval = 1;
		int dayOfWeek = 1;	// Monday
		Recurrence r = new Recurrence(interval, (byte) (1 << (dayOfWeek - 1)));
		RecurrenceIterator rix = new RecurrenceIterator(r, reference, reference, horizon);
		List<LocalDate> dates = generate(rix);
		assertEquals(0, dates.size());
	}

	@Test
	public void testDayHorizonNextWeek() {
		LocalDate reference = LocalDate.now();
		LocalDate horizon = reference.plusWeeks(1);
		int interval = 1;
		for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
			Recurrence r = new Recurrence(interval, (byte) (1 << (dayOfWeek - 1)));
			RecurrenceIterator rix = new RecurrenceIterator(r, reference, reference, horizon);
			List<LocalDate> dates = generate(rix);
			assertEquals(1, dates.size());
			assertEquals(reference.plusDays((7 - reference.getDayOfWeek().getValue() + dayOfWeek) % 7), dates.get(0));
		}
	}

	@Test
	public void testWeekFortnight() {
		LocalDate reference = LocalDate.now();
		LocalDate horizon = reference.plusWeeks(4);
		int interval = 2;
		int dayOfWeek = 1;	// Monday
		Recurrence r = new Recurrence(interval, (byte) (1 << (dayOfWeek - 1)));
		RecurrenceIterator rix = new RecurrenceIterator(r, reference, reference, horizon);
		List<LocalDate> dates = generate(rix);
		assertEquals(2, dates.size());
		for (int i = 0; i < dates.size() - 1; i++) {
			assertEquals(dates.get(i).plusWeeks(interval), dates.get(i + 1));
		}
	}

	@Test
	public void testWeek_Weekdays() {
		LocalDate reference = LocalDate.now();
		LocalDate horizon = reference.plusWeeks(2);
		int interval = 1;
		Recurrence r = new Recurrence(interval, (byte) 0x1F);
		RecurrenceIterator rix = new RecurrenceIterator(r, reference, reference, horizon);
		List<LocalDate> dates = generate(rix);
		assertEquals(5 * 2, dates.size());
		for (int i = 0; i < dates.size() - 1; i++) {
			assertTrue(dates.get(i).getDayOfWeek().getValue() <= 5);
		}
	}

	@Test
	public void testWeek_MondaysOffset() {
		LocalDate reference = LocalDate.parse("2019-11-18");
		LocalDate start = LocalDate.parse("2019-11-21");
		LocalDate horizon = reference.plusWeeks(4);
		int interval = 2;
		int dayOfWeek = 1;	// Monday
		Recurrence r = new Recurrence(interval, (byte) (1 << (dayOfWeek - 1)));
		RecurrenceIterator rix = new RecurrenceIterator(r, reference, start, horizon);
		List<LocalDate> dates = generate(rix);
		assertEquals(1, dates.size());
		assertEquals(reference.plusWeeks(2), dates.get(0));
	}
	@Test
	public void testWeek_MondaysOffsetHorizonEarly() {
		LocalDate reference = LocalDate.parse("2019-11-18");
		LocalDate start = LocalDate.parse("2019-11-21");
		LocalDate horizon = start.plusWeeks(4);
		int interval = 2;
		int dayOfWeek = 1;	// Monday
		Recurrence r = new Recurrence(interval, (byte) (1 << (dayOfWeek - 1)));
		RecurrenceIterator rix = new RecurrenceIterator(r, reference, start, horizon.minusWeeks(3));
		List<LocalDate> dates = generate(rix);
		assertEquals(0, dates.size());
	}
}
