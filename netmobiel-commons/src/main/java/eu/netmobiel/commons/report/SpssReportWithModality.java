package eu.netmobiel.commons.report;

import com.opencsv.bean.CsvBindByName;

public abstract class SpssReportWithModality<R extends ReportKeyWithModality> extends SpssReportBase<R> {
	/**
	 * The modality where the report is about.
	 */
	@CsvBindByName
	private String modality;

	protected SpssReportWithModality(String managedIdentity, String home, String modality) {
		super(managedIdentity, home);
		this.modality = modality;
	}

	public String getModality() {
		return modality;
	}

	public void setModality(String modality) {
		this.modality = modality;
	}

	
}
