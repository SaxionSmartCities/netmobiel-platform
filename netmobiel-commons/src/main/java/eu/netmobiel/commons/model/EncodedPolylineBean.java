package eu.netmobiel.commons.model;

import java.io.Serializable;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

/**
 * A list of coordinates encoded as a string.
 * 
 * See <a href="http://code.google.com/apis/maps/documentation/polylinealgorithm.html">Encoded polyline algorithm format</a>
 */

@Embeddable
@Vetoed
@Access(AccessType.FIELD)
public class EncodedPolylineBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "points", length = 1024)
    private String points;	// The encoded points of the polyline.
    @Transient
	private String levels;	// Levels describes which points should be shown at various zoom levels. Presently, we show all points at all zoom levels.
    @Basic
	private int length;		// The number of points in the string

    public EncodedPolylineBean() {

    }

    public EncodedPolylineBean(String points, String levels, int length) {
        this.points = points;
        this.levels = levels;
        this.length = length;
    }

    /**
     * The encoded points of the polyline.
     */
    public String getPoints() {
        return points;
    }

    public void setPoints(String points) {
        this.points = points;
    }

    /**
     * Levels describes which points should be shown at various zoom levels. Presently, we show all
     * points at all zoom levels.
    */
    public String getLevels() {
        return levels;
    }

    public String getLevels(int defaultLevel) {
        if (levels == null) {
            StringBuilder b = new StringBuilder();
            String l = encodeNumber(defaultLevel);
            for (int i = 0; i < length; i++)
                b.append(l);
            return b.toString();
        }
        return levels;
    }

    public void setLevels(String levels) {
        this.levels = levels;
    }

    /**
     * The number of points in the string
     */
    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    private static String encodeNumber(int num) {

        StringBuffer encodeString = new StringBuffer();

        while (num >= 0x20) {
            int nextValue = (0x20 | (num & 0x1f)) + 63;
            encodeString.append((char) (nextValue));
            num >>= 5;
        }

        num += 63;
        encodeString.append((char) (num));

        return encodeString.toString();
    }
}