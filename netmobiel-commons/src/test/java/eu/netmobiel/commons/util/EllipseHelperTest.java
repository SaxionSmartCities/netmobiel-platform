package eu.netmobiel.commons.util;

import static org.junit.Assert.*;

import javax.measure.unit.SI;

import org.jscience.geography.coordinates.UTM;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.util.AffineTransformation;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.util.EllipseHelper.EligibleArea;

/**
 * The Ellipse helper uses the UTM projection. One of the vertical gridlines of theUTM run across the Netherlands, on 6 degrees longitude.
 * When performing calculations we must take care that the difference coordinates are respected.
 *   
 * @author Jaap Reitsma
 *
 */
public class EllipseHelperTest {
	public static final GeoLocation placeZutphen = GeoLocation.fromString("Rabobank Zutphen::52.148125, 6.196966");
	public static final GeoLocation placeHengelo = GeoLocation.fromString("Rembrandtstraat 8, Hengelo (OV)::52.273440,6.785370");
	public static final GeoLocation placeAmersfoort = GeoLocation.fromString("Rembrandtstraat 8, Amersfoort (UT)::52.145710,5.388120");
	public static final GeoLocation placeWommels = GeoLocation.fromString("Slachte 6, Wommels (FR)::53.090700,5.570020");
	private static Logger log = LoggerFactory.getLogger(EllipseHelperTest.class);

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void calculateEllipse_singleUTM() {
		EligibleArea area = EllipseHelper.calculateEllipse(placeZutphen.getPoint(),	placeHengelo.getPoint(), 1000.0, 0.0);
		log.debug(area.toString());
		// Now we check: Are the two point inside the ellipse?
		assertTrue(placeZutphen.getPoint().within(area.eligibleAreaGeometry));
		assertTrue(placeHengelo.getPoint().within(area.eligibleAreaGeometry));
		assertFalse(placeWommels.getPoint().within(area.eligibleAreaGeometry));
		assertFalse(placeAmersfoort.getPoint().within(area.eligibleAreaGeometry));

		area = EllipseHelper.calculateEllipse(placeWommels.getPoint(),	placeAmersfoort.getPoint(), 1000.0, 0.0);
		assertFalse(placeZutphen.getPoint().within(area.eligibleAreaGeometry));
		assertFalse(placeHengelo.getPoint().within(area.eligibleAreaGeometry));
		assertTrue(placeWommels.getPoint().within(area.eligibleAreaGeometry));
		assertTrue(placeAmersfoort.getPoint().within(area.eligibleAreaGeometry));
	}

	@Test
	public void calculateEllipse_differentUTM() {
		EligibleArea area = EllipseHelper.calculateEllipse(placeAmersfoort.getPoint(),	placeHengelo.getPoint(), 1000.0, 0.0);
		log.debug(area.toString());
		dump(placeAmersfoort);
		dump(placeWommels);
		dump(placeHengelo);
		dump(placeZutphen);
//		dump(GeoLocation.fromString("0,6::0.0,6.0"));
//		dump(GeoLocation.fromString("0,3::0.0,3.0"));
//		dump(GeoLocation.fromString("0,9::0.0,9.0"));
//		dump(GeoLocation.fromString("52,0::52.0,0.0"));
//		dump(GeoLocation.fromString("52,3::52.0,3.0"));
//		dump(GeoLocation.fromString("52,5.999999::52.0,5.999999"));
//		dump(GeoLocation.fromString("52,6::52.0,6.0"));
//		dump(GeoLocation.fromString("52,9::52.0,9.0"));
//		dump(GeoLocation.fromString("52,12::52.0,12.0"));
//		dump(GeoLocation.fromString("52,15::52.0,15.0"));
//		dump(GeoLocation.fromString("53,0::53.0,0.0"));
//		dump(GeoLocation.fromString("53,3::53.0,3.0"));
//		dump(GeoLocation.fromString("53,6::53.0,6.0"));
//		dump(GeoLocation.fromString("53,9::53.0,9.0"));
//		dump(GeoLocation.fromString("53,12::53.0,12.0"));
//		dump(GeoLocation.fromString("53,15::53.0,15.0"));
//		UTM utmref = EllipseHelper.polar2utm(GeoLocation.fromString("0,3::0.0,3.0").getPoint().getCoordinate());
//		dump(500000, 5761038, utmref);
//		dump(600000, 5761038, utmref);
//		dump(700000, 5761038, utmref);
//		dump(800000, 5761038, utmref);
		// Now we check: Are the two point inside the ellipse?
		assertTrue(placeAmersfoort.getPoint().within(area.eligibleAreaGeometry));
		assertTrue(placeHengelo.getPoint().within(area.eligibleAreaGeometry));
		assertFalse(placeWommels.getPoint().within(area.eligibleAreaGeometry));
		assertTrue(placeZutphen.getPoint().within(area.eligibleAreaGeometry));
	}
	
	private static void dump(GeoLocation loc) {
		UTM utm = EllipseHelper.polar2utm(loc.getPoint().getCoordinate());
		log.debug(String.format("%s --> %.0f %.0f %c %d", loc.toString(), utm.eastingValue(SI.METER), utm.northingValue(SI.METER), 
				utm.latitudeZone(), utm.longitudeZone()));
	}

	@SuppressWarnings("unused")
	private static void dump(double x, double y, UTM utmref) {
		Coordinate c = EllipseHelper.utm2Polar(new Coordinate(x, y), utmref);
		GeoLocation loc = GeoLocation.fromDegrees(c.y, c.x);
		log.debug(String.format("%.0f %.0f %c %d --> %s", x, y, 
				utmref.latitudeZone(), utmref.longitudeZone(), loc.toString()));
	}
	
	@Test
	public void calculateClosestUTMOffset() {
		AffineTransformation tf = EllipseHelper.calculateTranslationToClosestUTMCenter(placeZutphen.getPoint(), placeHengelo.getPoint());
		// Should be UTM 32U, meridian is at 9 degrees
		log.debug(String.format("Translation: %s", tf.toString())); 
		log.debug(String.format("Offset: lat long %f %f", tf.getMatrixEntries()[5], tf.getMatrixEntries()[2])); 
		assertTrue(tf.getMatrixEntries()[5] == 0);
		assertTrue(tf.getMatrixEntries()[2] > 0);

		tf = EllipseHelper.calculateTranslationToClosestUTMCenter(placeAmersfoort.getPoint(), placeWommels.getPoint());
		// Should be UTM 31U, meridian is at 3 degrees
		log.debug(String.format("Translation: %s", tf.toString())); 
		log.debug(String.format("Offset: lat long %f %f", tf.getMatrixEntries()[5], tf.getMatrixEntries()[2])); 
		assertTrue(tf.getMatrixEntries()[5] == 0);
		assertTrue(tf.getMatrixEntries()[2] < 0);

		tf = EllipseHelper.calculateTranslationToClosestUTMCenter(placeHengelo.getPoint(), placeWommels.getPoint());
		// Should be UTM 32U, meridian is at 9 degrees
		log.debug(String.format("Translation: %s", tf.toString())); 
		log.debug(String.format("Offset: lat long %f %f", tf.getMatrixEntries()[5], tf.getMatrixEntries()[2])); 
		assertTrue(tf.getMatrixEntries()[5] == 0);
		assertTrue(tf.getMatrixEntries()[2] > 0);
		
		Point p = (Point) tf.transform(placeHengelo.getPoint());
		log.debug(String.format("Transformed: %s %s", p.toString(), p.getClass())); 
		
	}

}
