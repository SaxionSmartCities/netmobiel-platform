package eu.netmobiel.commons.report;

import com.opencsv.bean.CsvBindByName;

public class ProfileReport extends ReportKey {
	private static final long serialVersionUID = 5251079360179443539L;

	/**
	 * PRF-1: Age of the user.
	 */
	@CsvBindByName
	private Integer yearOfBirth;

	/**
	 * PRF-2: Is the user a Passenger?  
	 */
	@CsvBindByName
	private Boolean isPassenger;
	
	/**
	 * PRF-3: Is the user a Driver?
	 */
	@CsvBindByName
	private Boolean isDriver;

	/**
	 * PRF-4: The number of active cars.
	 */
	@CsvBindByName
	private Integer nrActiveCars;
	
	public ProfileReport() {
		
	}
	
	public ProfileReport(ReportPeriodKey key) {
		super(key);
	}

	public ProfileReport(String managedIdentity) {
		super(managedIdentity);
	}

	public Integer getYearOfBirth() {
		return yearOfBirth;
	}

	public void setYearOfBirth(Integer yearOfBirth) {
		this.yearOfBirth = yearOfBirth;
	}

	public Boolean getIsPassenger() {
		return isPassenger;
	}

	public void setIsPassenger(Boolean isPassenger) {
		this.isPassenger = isPassenger;
	}

	public Boolean getIsDriver() {
		return isDriver;
	}

	public void setIsDriver(Boolean isDriver) {
		this.isDriver = isDriver;
	}

	public Integer getNrActiveCars() {
		return nrActiveCars;
	}

	public void setNrActiveCars(Integer nrActiveCars) {
		this.nrActiveCars = nrActiveCars;
	}

}


