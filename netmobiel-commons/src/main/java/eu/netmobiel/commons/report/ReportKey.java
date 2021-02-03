package eu.netmobiel.commons.report;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvIgnore;

/**
 * This class defines the key for a basic report. 
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
	 * The key to this report, containing the managed identity, year and month.
	 */
	@CsvIgnore
	protected String key;

	public ReportKey() {
		
	}
	
	public ReportKey(String identity) {
		this.managedIdentity = identity;
	}

	public ReportKey(ReportKey key) {
		super();
		this.managedIdentity = key.managedIdentity;
	}
	
	public String getKey() {
		if (this.key == null) {
			this.key = String.format("%s", managedIdentity);
		}
		return this.key;
	}

	public String getManagedIdentity() {
		return managedIdentity;
	}

	public void setManagedIdentity(String managedIdentity) {
		this.managedIdentity = managedIdentity;
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


