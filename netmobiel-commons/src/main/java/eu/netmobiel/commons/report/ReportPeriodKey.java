package eu.netmobiel.commons.report;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvIgnore;

/**
 * This class defines the key for the report. 
 * 
 * @author Jaap Reitsma
 *
 */
public class ReportKey implements Serializable, Comparable<ReportKey> {
	private static final long serialVersionUID = -2609854526744056646L;

	/**
	 * The identity of the user the report is about.
	 */
	@CsvBindByName
	private String managedIdentity;
	
	/**
	 * The year the record is about.
	 */
	@CsvBindByName
	private int year;
	
	/**
	 * The month the record is about. Month 1 is january.
	 */
	@CsvBindByName
	private int month;
	
	/**
	 * The key to this report, containing the managed identity, year and month.
	 */
	@CsvIgnore
	protected String key;

	public ReportKey() {
		
	}
	
	public ReportKey(ReportKey key) {
		super();
		this.managedIdentity = key.managedIdentity;
		this.year = key.year;
		this.month = key.month;
	}
	
	public ReportKey(String managedIdentity, int year, int month) {
		super();
		this.managedIdentity = managedIdentity;
		this.year = year;
		this.month = month;
	}

	public String getKey() {
		if (this.key == null) {
			this.key = String.format("%s-%d-%d", managedIdentity, year, month);
		}
		return this.key;
	}

	public String getManagedIdentity() {
		return managedIdentity;
	}

	public void setManagedIdentity(String managedIdentity) {
		this.managedIdentity = managedIdentity;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public int getMonth() {
		return month;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	@Override
	public int compareTo(ReportKey other) {
		return Objects.compare(getKey(), other.getKey(), Comparator.naturalOrder());
	}

	@Override
	public String toString() {
		return getKey();
	}

}


