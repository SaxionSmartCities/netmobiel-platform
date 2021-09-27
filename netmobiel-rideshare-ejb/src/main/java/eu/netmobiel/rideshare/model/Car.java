package eu.netmobiel.rideshare.model;

import java.io.Serializable;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedEntityGraph;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.ReferableObject;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@NamedEntityGraph()
@Entity
@Table(name = "car", uniqueConstraints = @UniqueConstraint(name="car_uc", columnNames = {"driver", "registration_country", "license_plate_raw"}))
@Vetoed
@SequenceGenerator(name = "car_sg", sequenceName = "car_id_seq", allocationSize = 1, initialValue = 50)
public class Car extends ReferableObject implements Serializable {

	private static final long serialVersionUID = 1045941720040157428L;
	public static final String URN_PREFIX = RideshareUrnHelper.createUrnPrefix("car");

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "car_sg")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "driver", nullable = false, foreignKey = @ForeignKey(name = "car_driver_fk"))
    private RideshareUser driver;

    @Transient
    private String driverRef;
    
    /**
     * ISO 3166-1 alpha-3 codes are three-letter country codes defined in ISO 3166-1, part of the ISO 3166 standard published 
     * by the International Organization for Standardization (ISO), to represent countries, dependent territories, 
     * and special areas of geographical interest.
     * @See https://en.wikipedia.org/wiki/ISO_3166-1_alpha-3
     */
    @NotNull
    @NotEmpty
    @Size(max = 3)
    @Column(name = "registration_country", length = 3)
    private String registrationCountry;

    /**
     * The license plate without dashes and spaces.
     */
    @NotNull
    @Size(min = 1, max = 12)
    @Column(name = "license_plate_raw", length = 12)
    private String licensePlateRaw;

    /**
     * The license plate formatted by the user.
     */
    @NotNull
    @Size(min = 1, max = 16)
    @Column(name = "license_plate", length = 16)
    private String licensePlate;

    /**
     * The brand of the car, e.g. Volvo
     */
    @NotNull
    @NotEmpty
    @Size(max = 32)
    private String brand;

    /**
     * The model of the car, e.g. V70
     */
    @NotNull
    @NotEmpty
    @Size(max = 64)
    private String model;
    
    @NotNull
	@Column(length = 3)
    private CarType type;

    @NotEmpty
    @Size(max = 16)
    private String color;

    @Size(max = 16)
    private String color2;

    @Min(1900)
    @Max(2100)
    @Column(name = "registration_year")
    private Integer registrationYear;

    @Min(0)
    @Max(99)
    @Column(name = "nr_seats")
    private Integer nrSeats;

    @Min(0)
    @Max(6)
    @Column(name = "nr_doors")
    private Integer nrDoors;
    /**
     * Emission of CO2 in [g / km]
     */
    @PositiveOrZero
    @Column(name = "co2_emission")
    private Integer co2Emission;

    @Size(max = 40)
    @Column(name = "type_registration_id")
    private String typeRegistrationId;

    private Boolean deleted;
    
	@Override
    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String getUrnPrefix() {
		return URN_PREFIX;
	}

	public String getLicensePlateRaw() {
		return licensePlateRaw;
	}

	public void setLicensePlateRaw(String licensePlateRaw) {
		this.licensePlateRaw = licensePlateRaw;
	}

	public String getLicensePlate() {
		return licensePlate;
	}

	public void setLicensePlate(String licensePlate) {
		this.licensePlate = licensePlate;
	}

	public String getRegistrationCountry() {
		return registrationCountry;
	}

	public void setRegistrationCountry(String registrationCountry) {
		this.registrationCountry = registrationCountry;
	}

	public Integer getRegistrationYear() {
		return registrationYear;
	}

	public void setRegistrationYear(Integer registrationYear) {
		this.registrationYear = registrationYear;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public CarType getType() {
		return type;
	}

	public void setType(CarType type) {
		this.type = type;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getColor2() {
		return color2;
	}

	public void setColor2(String color2) {
		this.color2 = color2;
	}

	public Integer getNrSeats() {
		return nrSeats;
	}

	public void setNrSeats(Integer nrSeats) {
		this.nrSeats = nrSeats;
	}

	public RideshareUser getDriver() {
		return driver;
	}

	public void setDriver(RideshareUser driver) {
		this.driver = driver;
		this.driverRef = null;
	}

	public Integer getNrDoors() {
		return nrDoors;
	}

	public void setNrDoors(Integer nrDoors) {
		this.nrDoors = nrDoors;
	}

	public Integer getCo2Emission() {
		return co2Emission;
	}

	public void setCo2Emission(Integer co2Emission) {
		this.co2Emission = co2Emission;
	}

	public String getTypeRegistrationId() {
		return typeRegistrationId;
	}

	public void setTypeRegistrationId(String typeRegistrationId) {
		this.typeRegistrationId = typeRegistrationId;
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	public String getDriverRef() {
		if (driverRef == null) {
	    	if (driver != null) {
	    		driverRef = UrnHelper.createUrn(RideshareUser.URN_PREFIX, driver.getId());
	    	}
		}
		return driverRef;
	}

	public boolean isOwnedBy(RideshareUser aDriver) {
		return getDriver().getId().equals(aDriver.getId());
	}

	public String getName() {
		StringBuilder sb = new StringBuilder();
		if (getBrand() != null) {
			sb.append(getBrand()).append(" ");
		}
		if (getModel() != null) {
			sb.append(getModel());
		}
		String name = sb.toString().trim();
		return name.length() > 0 ? name : null;
	}

	@Override
	public String toString() {
		return String.format("Car [%s %s %s]", registrationCountry, licensePlateRaw, driverRef);
	}

	public static String unformatPlate(String plate) {
		return plate.replaceAll("[\\s-]", "").toUpperCase();
	}
}
