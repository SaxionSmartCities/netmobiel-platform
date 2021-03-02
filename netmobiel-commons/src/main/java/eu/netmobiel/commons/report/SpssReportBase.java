package eu.netmobiel.commons.report;

import com.opencsv.bean.CsvBindByName;

public abstract class SpssReportBase<R> {
	/**
	 * The identity of the user the report is about.
	 */
	@CsvBindByName
	private String managedIdentity;

	/**
	 * Home: The home locality of the user.
	 */
	@CsvBindByName
	private String home;

	public SpssReportBase(String managedIdentity, String home) {
		this.managedIdentity = managedIdentity;
		this.home = home;
	}
	
	public String getManagedIdentity() {
		return managedIdentity;
	}

	public void setManagedIdentity(String managedIdentity) {
		this.managedIdentity = managedIdentity;
	}

	public String getHome() {
		return home;
	}

	public void setHome(String home) {
		this.home = home;
	}

	public abstract void addReportValues(R ar);
}
