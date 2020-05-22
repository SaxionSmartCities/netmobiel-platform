package eu.netmobiel.rideshare.model;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class RecurrenceIterator implements Iterator<LocalDate> {
	private LocalDate reference = null;
	private LocalDate start = null;
	private LocalDate horizon = null;
	private Integer interval = null;
	private Byte dowMask;
	private Iterator<LocalDate> delegatedIterator = null;
	
	/**
	 * Constructor for the iterator. This iterator can be called one time too often to obtain the
	 * next value that would be generated.
	 * @param aPattern the pattern definition
	 * @param reference the reference day, which must be a valid date for the iterator. 
	 * 					If start is equal to reference, then reference is the first iteration value.
	 * @param start the date (inclusive) from which iteration values are returned, if any. 
	 * 					If set to null then the reference date is the first date. 
	 * @param horizon The horizon to use, may be null. The horizon date is exclusive. i.e. 
	 *                every iteration value is before the horizon.
	 */
	public RecurrenceIterator(Recurrence aPattern, LocalDate reference, LocalDate start, LocalDate horizon) {
		this.reference = reference;
		this.start = start;
		this.horizon = horizon;
		this.interval = aPattern.getInterval();
		this.dowMask = aPattern.getDaysOfWeekMask();
		if (this.interval == null || this.interval < 1) {
			throw new IllegalArgumentException("Interval must be at least 1");
		}
		if (this.reference == null) {
			throw new IllegalArgumentException("No reference date set!");
		}
		if (this.start == null) {
			this.start = this.reference;
		}
		if (this.horizon == null) {
			throw new IllegalArgumentException("No horizon in sight!");
		}
		if (! this.reference.isBefore(this.start) && ! this.reference.isEqual(this.start)) {
			throw new IllegalArgumentException("Reference date must be before or equal to start date!");
		}
		if (aPattern.getHorizon() != null && aPattern.getLocalHorizon().isBefore(horizon)) {
			this.horizon = aPattern.getLocalHorizon();  
		}
		if (aPattern.getUnit() == TimeUnit.DAY) {
			delegatedIterator = new DayIterator();
		} else if (aPattern.getUnit() == TimeUnit.WEEK) {
			delegatedIterator = new WeekIterator();
		} else {
			throw new UnsupportedOperationException("TimeUnit other than day or week");
		}
	}
	
	@Override
	public boolean hasNext() {
		return delegatedIterator.hasNext();
	}

	@Override
	public LocalDate next() {
		return delegatedIterator.next();
	}

	private class DayIterator implements Iterator<LocalDate> {
		private LocalDate cursor;
		private boolean exhausted = false;
		
		public DayIterator() {
			cursor = reference;
			// Step up until the cursor is on or past the start date. 
			while (cursor.isBefore(start)) {
				findNext();
			}
		}
		
		private void findNext() {
			cursor = cursor.plusDays(interval);
		}
		@Override
		public boolean hasNext() {
			return cursor.isBefore(horizon);
		}

		@Override
		public LocalDate next() {
			if (! hasNext()) {
				if (exhausted) {
					throw new NoSuchElementException("No more dates");
				} else {
					exhausted = true;
				}
			}
			LocalDate element = cursor;
			findNext();
			return element;
		}
		
	}

	/**
	 * Iterate over the days of the week that match the template
	 *
	 */
	private class DayOfWeekIterator implements Iterator<LocalDate> {
		private LocalDate weekCursor;
		private int dowStart;
		private int dowCursor;
		private LocalDate dayCursor;
		private boolean exhausted = false;
		
		public DayOfWeekIterator(LocalDate start) {
			this.weekCursor = start;
			this.dowStart = start.getDayOfWeek().getValue();
			dowCursor = dowStart;
			if (dowMask == null || (dowMask & 0x7F) == 0) {
				throw new IllegalArgumentException("DayOfWeek mask must have set at least one bit 0..6!");
			}
			findNext();
		}
		
		private void findNext() {
			while (dowCursor <= 7) {
				if ((dowMask & (1 << (dowCursor - 1))) != 0) {
					dayCursor = weekCursor.plusDays(dowCursor - dowStart);
					if (dayCursor.isEqual(start) || dayCursor.isAfter(start)) {
						break;
					}
				}
				dowCursor++;
			}
		}

		@Override
		public boolean hasNext() {
			return dowCursor <= 7 && dayCursor.isBefore(horizon);
		}

		@Override
		public LocalDate next() {
			if (! hasNext()) {
				if (exhausted) {
					throw new NoSuchElementException("No more days in this week!");
				} else {
					exhausted = true;
				}
			}
			LocalDate element = dayCursor;
			dowCursor++;
			findNext();
			return element;
		}
		
	}
	private class WeekIterator implements Iterator<LocalDate> {
		private LocalDate weekCursor;
		private  DayOfWeekIterator dowCursor;
		private boolean exhausted = false;
		
		public WeekIterator() {
			weekCursor = reference;
			// Advance until weekCursor is in same week or past week as start
			LocalDate startSow = startOfWeek(start);
			while (true) {
				LocalDate cursorSow = startOfWeek(weekCursor);
				if (cursorSow.isEqual(startSow) || cursorSow.isAfter(startSow)) {
					break;
				}
				advanceWeekCursor();
			}
			findNext();
		}
		
		private LocalDate startOfWeek(LocalDate date) {
			return date.minusDays(date.getDayOfWeek().getValue() - 1);
		}
		
		private void advanceWeekCursor() {
			weekCursor = weekCursor.plusWeeks(interval);
			// Be sure to set to start of week
			weekCursor = startOfWeek(weekCursor);
		}
		
		private void findNext() {
			if (dowCursor == null) {
				dowCursor = new DayOfWeekIterator(weekCursor);
			}
			if (! dowCursor.hasNext()) {
				advanceWeekCursor();
				dowCursor = new DayOfWeekIterator(weekCursor);
			}
		}

		@Override
		public boolean hasNext() {
			return dowCursor.hasNext();
		}

		@Override
		public LocalDate next() {
			if (! hasNext()) {
				if (exhausted) {
					throw new NoSuchElementException("No more dates");
				} else {
					exhausted = true;
				}
			}
			LocalDate element = dowCursor.next();
			findNext();
			return element;
		}
		
	}
}
