package eu.netmobiel.planner.model;

import java.io.Serializable;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Represents an instruction for directions. Three examples from New York City:
 * <p>
 * Turn onto Broadway from W 57th St (coming from 7th Ave): <br/>
 * distance = 100 (say) <br/>
 * walkDirection = RIGHT <br/>
 * streetName = Broadway <br/>
 * everything else null/false <br/>
 * </p>
 * <p>
 * Now, turn from Broadway onto Central Park S via Columbus Circle <br/>
 * distance = 200 (say) <br/>
 * walkDirection = CIRCLE_COUNTERCLOCKWISE <br/>
 * streetName = Central Park S <br/>
 * exit = 1 (first exit) <br/>
 * immediately everything else false <br/>
 * </p>
 * <p>
 * Instead, go through the circle to continue on Broadway <br/>
 * distance = 100 (say) <br/>
 * walkDirection = CIRCLE_COUNTERCLOCKWISE <br/>
 * streetName = Broadway <br/>
 * exit = 3 <br/>
 * stayOn = true <br/>
 * everything else false <br/>
 * </p>
 * */
@Embeddable
@Vetoed
@Access(AccessType.FIELD)
public class GuideStep implements Serializable {

	private static final long serialVersionUID = 6096844876269982454L;
	/**
	 * We don't use inheritance from GeoLocation, because:
	 * 1. We don't use the PostGIS features
	 * 2. The JPA spec does not allow/support it. Maybe Hibernate does. See https://en.wikibooks.org/wiki/Java_Persistence/Embeddables 
	 */
			
	@Basic
	private Double latitude;
	
	@Basic
	private Double longitude;

	/**
	 * The streetname or comparable. 
	 */
	@Column(name = "name", length = 128)
	private String name;
	/**
     * The distance in meters that this step takes.
     */
	@Basic
    private Integer distance;

    /**
     * The relative direction of this step.
     */
	@Column(name = "relative_direction", length = 3)
    private RelativeDirection relativeDirection;

    /**
     * The absolute direction of this step.
     */
	@Column(name = "absolute_direction", length = 2)
    private AbsoluteDirection absoluteDirection;

    /**
     * When exiting a highway or traffic circle, the exit name/number.
     */
	@Column(name = "exit", length = 16)
	private String exit;

    /**
     * Indicates whether or not a street changes direction at an intersection.
     */
	@Column(name = "stay_on")
	private Boolean stayOn = false;

    /**
     * This step is on an open area, such as a plaza or train platform, and thus the directions should say something like "cross"
     */
	@Column(name = "is_area", length = 16)
	private Boolean area = false;

    /**
     * The name of this street was generated by the system, so we should only display it once, and generally just display right/left directions
     */
	@Column(name = "is_bogus_name")
	private Boolean bogusName = false;

    public GuideStep() {
    }

    public GuideStep(Double lon, Double lat, String name) {
    	this.latitude = lat;
    	this.longitude = lon;
    	this.name = name;
    }
    
    public GuideStep(Double lon, Double lat, String name, RelativeDirection reldir, AbsoluteDirection absdir) {
    	this(lat, lon, name);
    	this.relativeDirection = reldir;
    	this.absoluteDirection = absdir;
    }

    public GuideStep(GuideStep other) {
    	this.latitude = other.latitude;
    	this.longitude = other.longitude;
    	this.name = other.name;
    	this.relativeDirection = other.relativeDirection;
    	this.absoluteDirection = other.absoluteDirection;
    	this.area = other.area;
    	this.bogusName = other.bogusName;
    	this.exit = other.exit;
    	this.stayOn = other.stayOn;
    }
    
    public GuideStep copy() {
    	return new GuideStep(this);
    }

    public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getDistance() {
		return distance;
	}

	public void setDistance(Integer distance) {
		this.distance = distance;
	}

	public RelativeDirection getRelativeDirection() {
		return relativeDirection;
	}

	public void setRelativeDirection(RelativeDirection relativeDirection) {
		this.relativeDirection = relativeDirection;
	}

	public AbsoluteDirection getAbsoluteDirection() {
		return absoluteDirection;
	}

	public void setAbsoluteDirection(AbsoluteDirection absoluteDirection) {
		this.absoluteDirection = absoluteDirection;
	}

	public String getExit() {
		return exit;
	}

	public void setExit(String exit) {
		this.exit = exit;
	}

	public Boolean getStayOn() {
		return stayOn;
	}

	public void setStayOn(Boolean stayOn) {
		this.stayOn = stayOn;
	}

	public Boolean getArea() {
		return area;
	}

	public void setArea(Boolean area) {
		this.area = area;
	}

	public Boolean getBogusName() {
		return bogusName;
	}

	public void setBogusName(Boolean bogusName) {
		this.bogusName = bogusName;
	}

	@Override
	public String toString() {
        String direction = absoluteDirection.toString();
        if (relativeDirection != null) {
            direction = relativeDirection.toString();
        }
        return String.format("GuideStep %s on %s for %dm", direction, name, distance);
    }

    public static RelativeDirection getRelativeDirection(double lastAngle, double thisAngle, boolean roundabout) {

        double angleDiff = thisAngle - lastAngle;
        if (angleDiff < 0) {
            angleDiff += Math.PI * 2;
        }
        double ccwAngleDiff = Math.PI * 2 - angleDiff;

        if (roundabout) {
            // roundabout: the direction we turn onto it implies the circling direction
            if (angleDiff > ccwAngleDiff) {
                return RelativeDirection.CIRCLE_CLOCKWISE;
            }
            return RelativeDirection.CIRCLE_COUNTERCLOCKWISE;
        }

        // less than 0.3 rad counts as straight, to simplify walking instructions
        if (angleDiff < 0.3 || ccwAngleDiff < 0.3) {
            return RelativeDirection.CONTINUE;
        } else if (angleDiff < 0.7) {
            return RelativeDirection.SLIGHTLY_RIGHT;
        } else if (ccwAngleDiff < 0.7) {
            return RelativeDirection.SLIGHTLY_LEFT;
        } else if (angleDiff < 2) {
            return RelativeDirection.RIGHT;
        } else if (ccwAngleDiff < 2) {
            return RelativeDirection.LEFT;
        } else if (angleDiff < Math.PI) {
            return RelativeDirection.HARD_RIGHT;
        } else {
            return RelativeDirection.HARD_LEFT;
        }
    }

}
