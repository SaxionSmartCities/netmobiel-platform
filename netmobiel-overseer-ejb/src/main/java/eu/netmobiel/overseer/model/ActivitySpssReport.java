package eu.netmobiel.overseer.model;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import com.opencsv.bean.CsvBindAndJoinByName;
import com.opencsv.bean.CsvBindByName;

import eu.netmobiel.communicator.model.ActivityReport;

public class ActivitySpssReport {
	/**
	 * The identity of the user the report is about.
	 */
	@CsvBindByName(column = "ManagedIdentity")
	private String managedIdentity;
	/**
	 * The number of messages received.
	 */
	@CsvBindAndJoinByName(column = "MESSAGECOUNT_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> messageCount;
	
	/**
	 * The number of messages acknowledged (i.e. read)  
	 */
//	private MultiValuedMap<String, Integer> messageAckedCount;
	
	/**
	 * The number of notifications received.
	 */
//	private MultiValuedMap<String, Integer> notificationCount;
	
	/**
	 * The number of notifications acknowledged (i.e. read)  
	 */
//	private MultiValuedMap<String, Integer> notificationAckedCount;

	public ActivitySpssReport(String managedIdentity) {
		this.managedIdentity = managedIdentity;
		this.messageCount = new ArrayListValuedHashMap<>();
	}
	
	public void addActivityReport(ActivityReport ar) {
		messageCount.put(String.format("messageCount_%d_%02d", ar.getYear(), ar.getMonth()), ar.getMessageCount());
	}

	public String getManagedIdentity() {
		return managedIdentity;
	}

	public MultiValuedMap<String, Integer> getMessageCount() {
		return messageCount;
	}
	
	
}
