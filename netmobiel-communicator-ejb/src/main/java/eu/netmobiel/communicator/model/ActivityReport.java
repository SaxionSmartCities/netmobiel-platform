package eu.netmobiel.communicator.model;

import java.util.Comparator;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvIgnore;

import eu.netmobiel.commons.report.ReportKey;

public class ActivityReport extends ReportKey {
	private static final long serialVersionUID = -2609854526744056646L;

	@CsvIgnore
	public static final Comparator<ActivityReport> COMPARATOR_ASC = new Comparator<>() {
		
		@Override
		public int compare(ActivityReport r1, ActivityReport r2) {
			return r1.getKey().compareTo(r2.getKey());
		}
	}; 
	/**
	 * The number of messages received.
	 */
	@CsvBindByName
	private int messageCount;
	
	/**
	 * The number of messages acknowledged (i.e. read)  
	 */
	@CsvBindByName
	private int messageAckedCount;
	
	/**
	 * The number of notifications received.
	 */
	@CsvBindByName
	private int notificationCount;
	
	/**
	 * The number of notifications acknowledged (i.e. read)  
	 */
	@CsvBindByName
	private int notificationAckedCount;
	

	public ActivityReport() {
		
	}
	
	public ActivityReport(ReportKey key) {
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
	
	@Override
	public String toString() {
		return String.format(
				"%s %d-%02d mc %d mr%d nc %d nr %d",
				getManagedIdentity(), getYear(), getMonth(), messageCount, messageAckedCount, notificationCount, notificationAckedCount);
	}

	
}


