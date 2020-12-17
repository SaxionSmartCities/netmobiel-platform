package eu.netmobiel.planner.model;

public class ModalityNumericReportValue extends ReportKeyWithModality {

	private static final long serialVersionUID = -8056270425252535347L;

	/**
	 * The numeric value of a property.
	 */
	private int value;

	public ModalityNumericReportValue() {
		super();
	}

	public ModalityNumericReportValue(String managedIdentity, int year, int month, int value, String modality) {
		super(managedIdentity, year, month, modality);
		this.value = value;
	}

	public ModalityNumericReportValue(String managedIdentity) {
		super(managedIdentity, 0, 0, null);
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
