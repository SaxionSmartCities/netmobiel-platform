package eu.netmobiel.overseer.model;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import com.opencsv.bean.CsvBindAndJoinByName;

import eu.netmobiel.commons.report.PassengerModalityBehaviourReport;
import eu.netmobiel.commons.report.SpssReportWithModality;

public class PassengerModalityBehaviourSpssReport  extends SpssReportWithModality<PassengerModalityBehaviourReport> {

	/**
	 * RGP-8 mono-modal count per modality (separate report)
	 */
	@CsvBindAndJoinByName(column = "tripsMonoModalCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> tripsMonoModalCount = new ArrayListValuedHashMap<>();

	/**
	 * RGP-10 Multi-modal count per modality (separate report)
	 */
	@CsvBindAndJoinByName(column = "tripsMultiModalCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> tripsMultiModalCount = new ArrayListValuedHashMap<>();

	public PassengerModalityBehaviourSpssReport(String managedIdentity, String home, String modality) {
		super(managedIdentity, home, modality);
	}

	@Override
	public void addReportValues(PassengerModalityBehaviourReport r) {
		// 8
		tripsMonoModalCount.put(String.format("tripsMonoModalCount_%d_%02d", r.getYear(), r.getMonth()), r.getTripsMonoModalCount());
		
		// 10
		tripsMultiModalCount.put(String.format("tripsMultiModalCount_%d_%02d", r.getYear(), r.getMonth()), r.getTripsMultiModalCount());
	}

	public MultiValuedMap<String, Integer> getTripsMonoModalCount() {
		return tripsMonoModalCount;
	}

	public void setTripsMonoModalCount(MultiValuedMap<String, Integer> tripsMonoModalCount) {
		this.tripsMonoModalCount = tripsMonoModalCount;
	}

	public MultiValuedMap<String, Integer> getTripsMultiModalCount() {
		return tripsMultiModalCount;
	}

	public void setTripsMultiModalCount(MultiValuedMap<String, Integer> tripsMultiModalCount) {
		this.tripsMultiModalCount = tripsMultiModalCount;
	}

}


