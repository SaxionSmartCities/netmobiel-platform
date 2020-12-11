package eu.netmobiel.commons.report;

import com.opencsv.bean.CsvBindByName;

public abstract class SpssReportBase<R> {
	/**
	 * The identity of the user the report is about.
	 */
	@CsvBindByName
	private String managedIdentity;

	public SpssReportBase(String managedIdentity) {
		this.managedIdentity = managedIdentity;
	}
	
	public String getManagedIdentity() {
		return managedIdentity;
	}

	public void setManagedIdentity(String managedIdentity) {
		this.managedIdentity = managedIdentity;
	}

	public abstract void addReportValues(R ar);
}
