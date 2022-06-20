package eu.netmobiel.commons.report;

import com.opencsv.bean.CsvBindByName;

public class ActivityReport extends ReportPeriodKey {
	private static final long serialVersionUID = -2609854526744056646L;

	/**
	 * ACT-1: The number of messages received.
	 */
	@CsvBindByName
	private int messageCount;
	
	/**
	 * ACT-2: The number of messages acknowledged (i.e. read)  
	 */
	@CsvBindByName
	private int messageAckedCount;
	
	/**
	 * ACT-3: The number of notifications received.
	 */
	@CsvBindByName
	private int notificationCount;
	
	/**
	 * ACT-4: The number of notifications acknowledged (i.e. read)  
	 */
	@CsvBindByName
	private int notificationAckedCount;
	
	/**
	 * ACT-5: The number of days in the month the app was used (to visit at least one page)  
	 */
	@CsvBindByName
	private int usageDaysPerMonthCount;

	public ActivityReport() {
		
	}
	
	public ActivityReport(ReportPeriodKey key) {
		super(key);
	}

	public ActivityReport(String managedIdentity, int year, int month) {
		super(managedIdentity, year, month);
	}

	public int getMessageCount() {
		return messageCount;
	}

	public void setMessageCount(int messageCount) {
		this.messageCount = messageCount;
	}

	public int getMessageAckedCount() {
		return messageAckedCount;
	}

	public void setMessageAckedCount(int messageAckedCount) {
		this.messageAckedCount = messageAckedCount;
	}

	public int getNotificationCount() {
		return notificationCount;
	}

	public void setNotificationCount(int notificationCount) {
		this.notificationCount = notificationCount;
	}

	public int getNotificationAckedCount() {
		return notificationAckedCount;
	}

	public void setNotificationAckedCount(int notificationAckedCount) {
		this.notificationAckedCount = notificationAckedCount;
	}
	
	public int getUsageDaysPerMonthCount() {
		return usageDaysPerMonthCount;
	}

	public void setUsageDaysPerMonthCount(int usageDaysPerMonthCount) {
		this.usageDaysPerMonthCount = usageDaysPerMonthCount;
	}

	@Override
	public String toString() {
		return String.format(
				"%s mr %d ma %d nr %d na %d",
				getKey(), messageCount, messageAckedCount, notificationCount, notificationAckedCount);
	}

	
}


