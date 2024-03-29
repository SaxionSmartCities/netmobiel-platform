package eu.netmobiel.commons.report;

public class NumericReportValue extends ReportPeriodKey {

	private static final long serialVersionUID = -9007011398666870472L;

	/**
	 * The numeric value of a property.
	 */
	private int value;
	
	public NumericReportValue() {
		
	}

	public NumericReportValue(String managedIdentity) {
		super(managedIdentity, 0, 0);
	}

	public NumericReportValue(String managedIdentity, int year, int month, int value) {
		super(managedIdentity, year, month);
		this.value = value;
	}


	public int getValue() {
		return value;
	}


	public void setValue(int value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return String.format("%s %d", getKey(), value);
	}

}


