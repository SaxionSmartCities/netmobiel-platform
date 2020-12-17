package eu.netmobiel.planner.model;

import com.opencsv.bean.CsvBindByName;

import eu.netmobiel.commons.report.ReportKey;

public class ReportKeyWithModality extends ReportKey {

	private static final long serialVersionUID = -6559984699496241503L;

	/**
	 * The modality as part of the key.
	 */
	@CsvBindByName
	private String modality;

	public ReportKeyWithModality() {
		super();
	}


	public ReportKeyWithModality(ReportKeyWithModality key) {
		super(key);
		this.modality = key.modality;
	}


	public ReportKeyWithModality(String managedIdentity, int year, int month, String modality) {
		super(managedIdentity, year, month);
		this.modality = modality;
	}

	public String getModality() {
		return modality;
	}
	
	public void setModality(String modality) {
		this.modality = modality;
	}

	public String getKey() {
		if (this.key == null) {
			this.key = String.format("%s-%d-%d-%s", getManagedIdentity(), getYear(), getMonth(), modality);
		}
		return this.key;
	}
}
