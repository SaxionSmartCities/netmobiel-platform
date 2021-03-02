package eu.netmobiel.commons.report;

import com.opencsv.bean.CsvBindByName;

public class ReportKeyWithModality extends ReportPeriodKey {

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

	/**
	 * The key for the report. The month is two digits for proper sorting based on the string value of the key.
	 */
	public String getKey() {
		if (this.key == null) {
			this.key = String.format("%s-%d-%02d-%s", getManagedIdentity(), getYear(), getMonth(), modality);
		}
		return this.key;
	}
}
