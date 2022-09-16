package de.fmi.searouter.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests if the intersection tests implemented work properly.
 */
class IntersectionHelperTest {

    @Test
    void testArcIntersectionOnNorthernHemisphere() {
        assertTrue(IntersectionHelper.arcsIntersect(48.29395855961613, 10.260511730545458,
                49.74558758549278, 8.95313872788445, 48.46543834180716, 7.354628039756917,
                49.407203996970246, 11.716200157878095));
    }

    @Test
    void testArcIntersectionWithLat() {
       // assertTrue(IntersectionHelper.crossesLatitude(0.592539, 2.066470, 48.890711547179826, 1.287762,
       //         0.709186,  5.827305847480904, 15.378583103069795));


        assertTrue(IntersectionHelper.crossesLatitude(48.62046075163475, 8.982794304185463, 48.890711547179826, 9.290944154745407,
                48.797685831687566,  5.827305847480904, 15.378583103069795));


    }

}