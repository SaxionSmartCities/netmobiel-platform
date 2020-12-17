package eu.netmobiel.planner.model;

import com.opencsv.bean.CsvBindByName;

public class PassengerModalityBehaviourReport extends ReportKeyWithModality {
	
	private static final long serialVersionUID = -7202730003166618230L;

	/**
	 * RGP-8 Number of completed monomodal trips (ignoring Walking)
	 */
	@CsvBindByName
	private int tripsMonoModalCount;

	/**
	 * RGP-10 Number of completed multi-modal trips (ignoring Walking)
	 */
	@CsvBindByName
	private int tripsMultiModalCount;

	public PassengerModalityBehaviourReport() {
		
	}
	
	public PassengerModalityBehaviourReport(ReportKeyWithModality key) {
		super(key);
	}

	public PassengerModalityBehaviourReport(String managedIdentity, int year, int month, String modality) {
		super(managedIdentity, year, month, modality);
	}

	public int getTripsMonoModalCount() {
		return tripsMonoModalCount;
	}

	public void setTripsMonoModalCount(int tripsMonoModalCount) {
		this.tripsMonoModalCount = tripsMonoModalCount;
	}

	public int getTripsMultiModalCount() {
		return tripsMultiModalCount;
	}

	public void setTripsMultiModalCount(int tripsMultiModalCount) {
		this.tripsMultiModalCount = tripsMultiModalCount;
	}

}


