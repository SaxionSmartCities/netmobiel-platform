package eu.netmobiel.commons.report;

public class ShoutOutRecipientReport extends ReportPeriodKey {
	private static final long serialVersionUID = -2609854526744056646L;

	/**
	 * RCG-5: The number of shout-out notifications received.
	 */
	private int shoutOutNotificationCount;
	
	/**
	 * RCG-6: The number of shout-out notifications acknowledged (i.e. read)  
	 */
	private int shoutOutNotificationAckedCount;
	

	public ShoutOutRecipientReport() {
		
	}
	
	public ShoutOutRecipientReport(ReportPeriodKey key) {
		super(key);
	}

	public ShoutOutRecipientReport(String managedIdentity, int year, int month) {
		super(managedIdentity, year, month);
	}

	
	public int getShoutOutNotificationCount() {
		return shoutOutNotificationCount;
	}

	public void setShoutOutNotificationCount(int shoutOutNotificationCount) {
		this.shoutOutNotificationCount = shoutOutNotificationCount;
	}

	public int getShoutOutNotificationAckedCount() {
		return shoutOutNotificationAckedCount;
	}

	public void setShoutOutNotificationAckedCount(int shoutOutNotificationAckedCount) {
		this.shoutOutNotificationAckedCount = shoutOutNotificationAckedCount;
	}

	@Override
	public String toString() {
		return String.format(
				"%s snr %d sna %d",
				getKey(), shoutOutNotificationCount, shoutOutNotificationAckedCount);
	}

	
}


