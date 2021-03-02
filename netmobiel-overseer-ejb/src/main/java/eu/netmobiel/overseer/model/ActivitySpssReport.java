package eu.netmobiel.overseer.model;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import com.opencsv.bean.CsvBindAndJoinByName;

import eu.netmobiel.commons.report.ActivityReport;
import eu.netmobiel.commons.report.SpssReportBase;

public class ActivitySpssReport extends SpssReportBase<ActivityReport> {

	/**
	 * ACT-1: The number of messages received.
	 */
	@CsvBindAndJoinByName(column = "messageCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> messageCount;
	
	/**
	 * ACT-2: The number of messages acknowledged (i.e. read)  
	 */
	@CsvBindAndJoinByName(column = "messageAckedCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> messageAckedCount;
	
	/**
	 * ACT-3: The number of notifications received.
	 */
	@CsvBindAndJoinByName(column = "notificationCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> notificationCount;
	
	/**
	 * ACT-4: The number of notifications acknowledged (i.e. read)  
	 */
	@CsvBindAndJoinByName(column = "notificationAckedCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> notificationAckedCount;

	public ActivitySpssReport(String managedIdentity, String home) {
		super(managedIdentity, home);
		this.messageCount = new ArrayListValuedHashMap<>();
		this.messageAckedCount = new ArrayListValuedHashMap<>();
		this.notificationCount = new ArrayListValuedHashMap<>();
		this.notificationAckedCount = new ArrayListValuedHashMap<>();
	}
	
	@Override
	public void addReportValues(ActivityReport r) {
		messageCount.put(String.format("messageCount_%d_%02d", r.getYear(), r.getMonth()), r.getMessageCount());
		messageAckedCount.put(String.format("messageAckedCount_%d_%02d", r.getYear(), r.getMonth()), r.getMessageAckedCount());
		notificationCount.put(String.format("notificationCount_%d_%02d", r.getYear(), r.getMonth()), r.getNotificationCount());
		notificationAckedCount.put(String.format("notificationAckedCount_%d_%02d", r.getYear(), r.getMonth()), r.getNotificationAckedCount());
	}
	
	public MultiValuedMap<String, Integer> getMessageCount() {
		return messageCount;
	}

	public MultiValuedMap<String, Integer> getMessageAckedCount() {
		return messageAckedCount;
	}

	public MultiValuedMap<String, Integer> getNotificationCount() {
		return notificationCount;
	}

	public MultiValuedMap<String, Integer> getNotificationAckedCount() {
		return notificationAckedCount;
	}
	
}
