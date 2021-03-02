package eu.netmobiel.commons.report;

import java.io.Serializable;

import com.opencsv.bean.CsvBindByName;

/**
 * This class defines the key for a periodic report. 
 * 
 * @author Jaap Reitsma
 *
 */
public class ReportPeriodKey extends ReportKey implements Serializable {
	private static final long serialVersionUID = -2609854526744056646L;

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
	
	public ReportPeriodKey() {
		
	}
	
	public ReportPeriodKey(ReportPeriodKey key) {
		super(key);
		this.year = key.year;
		this.month = key.month;
	}
	
	public ReportPeriodKey(String managedIdentity, int year, int month) {
		super(managedIdentity);
		this.year = year;
		this.month = month;
	}

	/**
	 * The key for the report. The month is two digits for proper sorting based on the string value of the key.
	 */
	public String getKey() {
		if (this.key == null) {
			this.key = String.format("%s-%d-%02d", getManagedIdentity(), year, month);
		}
		return this.key;
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

}


