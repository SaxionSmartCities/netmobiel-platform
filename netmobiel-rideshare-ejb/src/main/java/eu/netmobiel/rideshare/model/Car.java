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

import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@NamedEntityGraph()
@Entity
@Table(name = "car", uniqueConstraints = @UniqueConstraint(name="car_uc", columnNames = {"driver", "registration_country", "license_plate"}))
@Vetoed
@SequenceGenerator(name = "car_sg", sequenceName = "car_id_seq", allocationSize = 1, initialValue = 50)
public class Car implements Serializable {

	private static final long serialVersionUID = 1045941720040157428L;
	public static final String URN_PREFIX = RideshareUrnHelper.createUrnPrefix("car");

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "car_sg")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "driver", nullable = false, foreignKey = @ForeignKey(name = "car_driver_fk"))
    private User driver;

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

    @NotNull
    @Size(min = 1, max = 16)
    @Column(name = "license_plate")
    private String licensePlate;


    @NotNull
    @NotEmpty
    @Size(max = 32)
    private String brand;

    @NotNull
    @NotEmpty
    @Size(max = 32)		//TODO Too small for 85-BV-RS "TRAFIC 1200 L2 H1 1.9DCI 82 DUBBELE CABINE"
    private String model;
    
	@NotNull
	@Column(length = 3)
    private CarType type;

    @NotNull
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

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public User getDriver() {
		return driver;
	}

	public void setDriver(User driver) {
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

	public String getDriverRef() {
		if (driverRef == null) {
	    	if (driver != null) {
	    		driverRef = RideshareUrnHelper.createUrn(User.URN_PREFIX, driver.getId());
	    	}
		}
		return driverRef;
	}

	@Override
	public String toString() {
		return String.format("Car [%s %s %s]", registrationCountry, licensePlate, driverRef);
	}

   
}
