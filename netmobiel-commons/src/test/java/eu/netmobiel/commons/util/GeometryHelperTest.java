package eu.netmobiel.commons.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;

public class GeometryHelperTest {
	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Test
	public void testSimple() {
		Geometry gm = GeometryHelper.createEllipse(52.5, 6.5, 10000.0, 100.0, Math.toRadians(0));
		String wkt = GeometryHelper.createWKT(gm);
		assertNotNull(wkt);
		log.info(wkt);
	}

	@Test
	public void testEnschedeBorne() {
		Geometry gm = GeometryHelper.createEllipse(52.26, 6.82, 12000, 100, 2.63);
		String wkt = GeometryHelper.createWKT(gm);
		assertNotNull(wkt);
		log.info(wkt);
	}
}
