package de.fmi.searouter.utils;

import com.google.common.math.DoubleMath;

/**
 * Helper methods for Geometrical calculations on a sphere.
 */
public class IntersectionHelper {

    //earths radius is required for distance calculation, both as a double and an int value
    public static final int EARTH_RADIUS_METERS = 6371 * 1000;
    private static final double EARTH_RADIUS = 6371000.0;

    /**
     * Returns the position of a point relative to a given grid cell. The position is encoded in boolean array for
     * western, eastern, northern, southern positions in relation to the grid.
     *
     * @param pointToCheckLat  The latitude coordinate of the point which position should be checked
     *                         relative to the cell.
     * @param pointToCheckLon  The longitude coordinate of the point which position should be checked
     *                         relative to the cell.
     * @param westernBoundLon  Western bound of the cell.
     * @param easternBoundLon  Eastern bound of the cell.
     * @param southernBoundLon Southern bound of the cell.
     * @param northernBoundLon Northern bound of the cell.
     * @return A boolean array with four entries representing the compass direction the point is situated relative
     * to the grid cell. For example [true, false, true, false] tells that the point lays north-west in relation
     * to the cell.
     */
    public static boolean[] getPositionInfoOfPointRelativeToCell(double pointToCheckLat, double pointToCheckLon,
                                                                 double westernBoundLon, double easternBoundLon,
                                                                 double southernBoundLon, double northernBoundLon) {
        // Positions: west, east, north, south
        boolean[] position = new boolean[4];

        if (pointToCheckLon < westernBoundLon) {
            position[0] = true;
        } else if (pointToCheckLon > easternBoundLon) {
            position[1] = true;
        }

        if (pointToCheckLat < southernBoundLon) {
            position[3] = true;
        } else if (pointToCheckLat > northernBoundLon) {
            position[2] = true;
        }

        return position;
    }


    /**
     * Converts coordinate point in degrees to a radian n-vector representation
     * (https://en.wikipedia.org/wiki/N-vector).
     *
     * @param lat The latitude of the coordinate point in degrees.
     * @param lon The longitude of the coordinate point in degres.
     * @return A n-vector represented as double array with three entries.
     */
    private static double[] latLonToVector(double lat, double lon) {
        lat = Math.toRadians(lat);
        lon = Math.toRadians(lon);

        double latCos = Math.cos(lat);

        return new double[]{
                latCos * Math.cos(lon),
                latCos * Math.sin(lon),
                Math.sin(lat)
        };
    }

    /**
     * Adds two vectors each of which having 3 entries.
     *
     * @param a The first addend vector.
     * @param b The second addend vector.
     * @return The result vectors
     */
    private static double[] addVectors(double[] a, double[] b) {
        return new double[]{
                a[0] + b[0],
                a[1] + b[1],
                a[2] + b[2]
        };
    }

    /**
     * Calculates the cross product of two three-dimensional vectors.
     *
     * @param a The first factor of the cross product.
     * @param u The second factor of the cross product.
     * @return A vector being the result of the calculated cross product.
     */
    private static double[] crossProductOfVector(double[] a, double[] u) {
        return new double[]{
                a[1] * u[2] - a[2] * u[1],
                a[2] * u[0] - a[0] * u[2],
                a[0] * u[1] - a[1] * u[0]
        };
    }

    /**
     * Calculates the dot product of two three-dimensional vectors.
     *
     * @param a The first factor
     * @param b The second factor
     * @return The resulting vector
     */
    private static double dotProductOfVector(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    /**
     * Calculates the length of a three-dimensional vector.
     *
     * @param v The vector to get the length of.
     * @return The length of the vector.
     */
    private static double length(double[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    /**
     * Calculates the distance between two n-vectors (https://en.wikipedia.org/wiki/N-vector)
     * on the earths surface.
     *
     * @param v The start n-vector.
     * @param u The destination n-vector.
     * @return The distance in meters between the two points that are represented by the n-vectors.
     */
    private static double distance(double[] v, double[] u) {
        return EARTH_RADIUS_METERS * Math.atan2(length(crossProductOfVector(v, u)), dotProductOfVector(v, u));
    }

    public static double distance(double pointALat, double pointALon, double pointBLat, double pointBLon) {
        return distance(latLonToVector(pointALat, pointALon), latLonToVector(pointBLat, pointBLon));
    }

    /**
     * Determines whether two edges (arcs) on the earths surface intersect with each other.
     *
     * @param latSourceA Latitude of start point of edge A (in degrees)
     * @param lonSourceA Longitude of start point of edge A (in degrees)
     * @param latDestA   Latitude of destination point of edge A (in degrees)
     * @param lonDestA   Longitude of destination point of edge A (in degrees)
     * @param latSourceB Latitude of start point of edge B (in degrees)
     * @param lonSourceB Longitude of start point of edge B (in degrees)
     * @param latDestB   Latitude of destination point of edge B (in degrees)
     * @param lonDestB   Longitude of destination point of edge B (in degrees)
     * @return True if the edges (arcs) defined by the four points intersect.
     * @throws IllegalArgumentException If the start or end of the coastline are exactly on the vertical line checked.
     */
    public static boolean arcsIntersectWithException(
            double latSourceA, double lonSourceA,
            double latDestA, double lonDestA,
            double latSourceB, double lonSourceB,
            double latDestB, double lonDestB) throws IllegalArgumentException {
        // Start or end of the coastline are exactly on the vertical line checked if longitude is equal
        if (DoubleMath.fuzzyEquals(lonSourceA, lonDestB, 0.0000001) ||
                DoubleMath.fuzzyEquals(lonDestA, lonDestB, 0.0000001)) {
            throw new IllegalArgumentException();
        }
        return arcsIntersect(latSourceA, lonSourceA, latDestA, lonDestA, latSourceB, lonSourceB, latDestB, lonDestB);
    }


    /**
     * Determines whether two edges (arcs) on the earths surface intersect with each other.
     *
     * @param latSourceA Latitude of start point of edge A (in degrees)
     * @param lonSourceA Longitude of start point of edge A (in degrees)
     * @param latDestA   Latitude of destination point of edge A (in degrees)
     * @param lonDestA   Longitude of destination point of edge A (in degrees)
     * @param latSourceB Latitude of start point of edge B (in degrees)
     * @param lonSourceB Longitude of start point of edge B (in degrees)
     * @param latDestB   Latitude of destination point of edge B (in degrees)
     * @param lonDestB   Longitude of destination point of edge B (in degrees)
     * @return True if the edges (arcs) defined by the four points intersect.
     */
    public static boolean arcsIntersect(
            double latSourceA, double lonSourceA,
            double latDestA, double lonDestA,
            double latSourceB, double lonSourceB,
            double latDestB, double lonDestB) {
        // Start or end of the coastline are exactly on the vertical line checked if longitude is equal.
        // In this case, this function simply returns true.
        if (DoubleMath.fuzzyEquals(lonSourceA, lonDestA, 0.0000001) &&
                DoubleMath.fuzzyEquals(lonDestA, lonSourceB, 0.0000001) &&
                DoubleMath.fuzzyEquals(lonSourceB, lonDestB, 0.0000001)) {
            return true;
        }

        return greatCircleSegmentIntersection(
                latLonToVector(latSourceA, lonSourceA),
                latLonToVector(latDestA, lonDestA),
                latLonToVector(latSourceB, lonSourceB),
                latLonToVector(latDestB, lonDestB), new double[3]);
    }

    /**
     * Calculates if two segments (= edges) of a great circle intersect or not.
     *
     * @param sourceA The n-vector of the start point of edge A
     * @param destA   The n-vector of the destination point of edge A
     * @param sourceB The n-vector of the start point of edge B
     * @param destB   The n-vector of the destination point of edge B
     * @param target  Is a reference to a double array in which the intersection point is saved after the calculation finished.
     * @return True, if the edges intersect, false if not.
     */
    private static boolean greatCircleSegmentIntersection(double[] sourceA, double[] destA, double[] sourceB, double[] destB, double[] target) {
        // Calculate the intersection point of the great circle arcs
        boolean intersect = lineIntersection(sourceA, destA, sourceB, destB, target);
        // Test if the found intersection is really relevant for over segment of the great circle arc (our edge)
        if (isIntersectionInGreatCircleSegment(sourceA, destA, target) && isIntersectionInGreatCircleSegment(sourceB, destB, target)) {
            return intersect;
        } else {
            return false;
        }
    }

    /**
     * Calculates whether a calculated intersection point of a great circle is relevant for an intersection
     * of certain segments (our edges to look at) of these great circle arcs. This means that the intersection
     * point really lays on the edge.
     *
     * @param source The start point of the edge as n-vector
     * @param dest   The destination point of the edge as n-vector
     * @param inter  The intersection point as n-vector
     * @return True, if the intersection point really lays on the edge, False if not.
     */
    private static boolean isIntersectionInGreatCircleSegment(double[] source, double[] dest, double[] inter) {
        double arcLength = distance(source, dest);
        return distance(inter, source) <= arcLength && distance(inter, dest) <= arcLength;
    }

    /**
     * Calculates the intersection point of two great circle arcs defined by the coordinates of two edges as n-vectors.
     * <p>
     * Source: http://www.movable-type.co.uk/scripts/latlong-vectors.html
     * </p>
     *
     * @param sourceA The start point of edge A as n-vector
     * @param destA   The destination point of edge A as n-vector
     * @param sourceB The start point of edge B as n-vector
     * @param destB   The destination point of edge B as n-vector
     * @param target  A reference to a double array in which the intersection result will be written.
     * @return True if the great circle arcs intersect, false if not.
     */
    private static boolean lineIntersection(double[] sourceA, double[] destA, double[] sourceB, double[] destB, double[] target) {
        double[] c1 = crossProductOfVector(sourceA, destA);
        double[] c2 = crossProductOfVector(sourceB, destB);

        double[] i1 = crossProductOfVector(c1, c2);
        double[] i2 = crossProductOfVector(c2, c1);

        double[] midPoint = addVectors(sourceA, addVectors(destA, addVectors(sourceB, destB)));

        double dot = dotProductOfVector(midPoint, i1);
        if (dot > 0) {
            System.arraycopy(i1, 0, target, 0, 3);
            return true;
        } else if (dot < 0) {
            System.arraycopy(i2, 0, target, 0, 3);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Transforms a degree coordinate to a radian one.
     *
     * @param degreeCoord A coordinate in degree
     * @return The coordinate in radian
     */
    private static double degreeCoordinateToRadian(double degreeCoord) {
        return degreeCoord * Math.PI / 180;
    }

    /**
     * Checks if an arc defined by start point A (pointALat, pointALon) and
     * destination point B (pointBLat, pointBLon) intersects with a given latitude.
     * <p>
     * Source: https://edwilliams.org/avform147.htm#Par
     * </p>
     *
     * @param pointALat      Latitude of the start point of the arc
     * @param pointALon      Longitude of the start point of the arc
     * @param pointBLat      Latitude of the destination point of the arc
     * @param pointBLon      Longitude of the destination point of the arc
     * @param latToIntersect The latitude to check
     * @return True if the latitude intersects with the arc
     * @throws IllegalArgumentException If the start or end of a coastline is exactly on the horizontal line of the
     * latitude
     */
    public static boolean crossesLatitudeWithException(double pointALat, double pointALon, double pointBLat,
                                                       double pointBLon, double latToIntersect, double lonValStart,
                                                       double lonValDest)
            throws IllegalArgumentException {
        // Start or end of the coastline are exactly on the horizontal line of the latitude if latitude is equal.
        if (DoubleMath.fuzzyEquals(pointALat, latToIntersect, 0.0000001) ||
                DoubleMath.fuzzyEquals(pointBLat, latToIntersect, 0.0000001)) {
            throw new IllegalArgumentException();
        }
        return crossesLatitude(pointALat, pointALon, pointBLat, pointBLon, latToIntersect, lonValStart, lonValDest);
    }


    /**
     * Checks if an arc defined by start point A (pointALat, pointALon) and
     * destination point B (pointBLat, pointBLon) intersects with a given latitude.
     * <p>
     * Source: https://edwilliams.org/avform147.htm#Par
     * </p>
     *
     * @param pointALat      Latitude of the start point of the arc
     * @param pointALon      Longitude of the start point of the arc
     * @param pointBLat      Latitude of the destination point of the arc
     * @param pointBLon      Longitude of the destination point of the arc
     * @param latToIntersect The latitude to check
     * @return True if the latitude intersects with the arc
     */
    public static boolean crossesLatitude(double pointALat, double pointALon, double pointBLat,
                                          double pointBLon, double latToIntersect, double lonValStart, double lonValDest) {
        // Start or end of the coastline are exactly on the horizontal line of the latitude if latitude is equal.
        // In this case, this function simply returns true.
        if (DoubleMath.fuzzyEquals(pointALat, latToIntersect, 0.0000001) ||
                DoubleMath.fuzzyEquals(pointBLat, latToIntersect, 0.0000001)) {
            return true;
        }

        // Check required in case the arc is vertical
        if (Math.max(pointALat, pointBLat) < latToIntersect || Math.min(pointALat, pointBLat) > latToIntersect) {
            return false;
        }

        pointALat = degreeCoordinateToRadian(pointALat);
        pointBLat = degreeCoordinateToRadian(pointBLat);
        pointALon = degreeCoordinateToRadian(pointALon);
        pointBLon = degreeCoordinateToRadian(pointBLon);
        latToIntersect = degreeCoordinateToRadian(latToIntersect);
        lonValStart = degreeCoordinateToRadian(lonValStart);
        lonValDest = degreeCoordinateToRadian(lonValDest);

        double latMax = Math.max(pointALat, pointBLat);
        double latMin = Math.min(pointALat, pointBLat);
        if (!coordinateBetweenValues(latToIntersect, latMin, latMax)) {
            return false;
        }

        double lonDiff = pointALon - pointBLon;
        double A = Math.sin(pointALat) * Math.cos(pointBLat) * Math.cos(latToIntersect) * Math.sin(lonDiff);
        double B = Math.sin(pointALat) * Math.cos(pointBLat) * Math.cos(latToIntersect) * Math.cos(lonDiff) -
                Math.cos(pointALat) * Math.sin(pointBLat) * Math.cos(latToIntersect);
        double C = Math.cos(pointALat) * Math.cos(pointBLat) * Math.sin(latToIntersect) * Math.sin(lonDiff);
        double lon = Math.atan2(B, A);

        double sumABsquared = Math.sqrt(A * A + B * B);

        if (Math.abs(C) > sumABsquared) {
            // no crossing
            return false;
        } else {
            double dlon = Math.acos(C / sumABsquared);
            // Intersection longitudes
            double crossLonA = mod(pointALon + dlon + lon + Math.PI, 2 * Math.PI) - Math.PI;
            double crossLonB = mod(pointALon - dlon + lon + Math.PI, 2 * Math.PI) - Math.PI;

            double maxLon = Math.max(pointALon, pointBLon);
            double minLon = Math.min(pointALon, pointBLon);
            double lonMax = Math.max(lonValStart, lonValDest);
            double lonMin = Math.min(lonValStart, lonValDest);
            boolean firstIntersectValid = coordinateBetweenValues(crossLonA, minLon, maxLon) &&
                    coordinateBetweenValues(crossLonA, lonMin, lonMax);
            boolean secondIntersectValid = coordinateBetweenValues(crossLonB, minLon, maxLon) &&
                    coordinateBetweenValues(crossLonB, lonMin, lonMax);
            return firstIntersectValid || secondIntersectValid;
        }
    }

    /**
     * Checks whether a coordinate lays between two values (inclusive). MinValue must be smaller than MaxValue,
     * else the result may be wrong.
     *
     * @param coordinateToCheck The coordinate (in degrees) that should be checked
     * @param minValue The minimum value the coordinateToCheck should have (inclusive). minValue < maxValue must be met
     * @param maxValue The maximum value the coordinateToCheck should have (inclusive). maxValue > minValue must be met
     * @return True if (coordinateToCheck >= minValue && coordinateToCheck <= maxValue)
     */
    private static boolean coordinateBetweenValues(double coordinateToCheck, double minValue, double maxValue) {
        if (DoubleMath.fuzzyEquals(coordinateToCheck, minValue, 0.0000001) &&
                DoubleMath.fuzzyEquals(minValue, maxValue, 0.0000001)) {
            return true;
        }

        boolean result;
        if (maxValue > 175.0 && minValue < -175) { //special case with wraparound has to be considered, but only for lon
            result = (coordinateToCheck < 180.0 && coordinateToCheck > maxValue) ||
                    (coordinateToCheck > -180.0 && coordinateToCheck < minValue);
        } else {
            result = (coordinateToCheck < maxValue && coordinateToCheck > minValue);
        }
        return result;
    }

    /**
     * Implementation of a mod function
     *
     * Source: https://edwilliams.org/avform147.htm#Math
     *
     * @param y first double val x %
     * @param x second double val % y
     * @return the modular calcaulation result
     */
    public static double mod(double y, double x) {
        return y - x * Math.floor(y / x);
    }

    /**
     * Calculates the distance between two points basted on latitude and longitude.
     * Formulas used can be found here: http://www.movable-type.co.uk/scripts/latlong.html
     *
     * @param startLat  latitude of the first point
     * @param startLong longitude of the first point
     * @param endLat    latitude of the second point
     * @param endLong   longitude of the second point
     * @return the distance between fist and second point in meters
     */
    public static double getDistance(double startLat, double startLong, double endLat, double endLong) {
        double radianStartLat = convertToRadian(startLat);
        double radianEndLat = convertToRadian(endLat);
        double latDifference = convertToRadian(endLat - startLat);
        double longDifference = convertToRadian(endLong - startLong);

        double haversine = Math.sin(latDifference / 2) * Math.sin(latDifference / 2) +
                Math.cos(radianStartLat) * Math.cos(radianEndLat) * Math.sin(longDifference / 2) *
                        Math.sin(longDifference / 2);
        double c = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));

        return c * EARTH_RADIUS;
    }

    /**
     * Converts a coordinate in degrees to a radian representation
     *
     * @param coordinate the coordinate in degree to transform to radian
     * @return the coordinate in radian coordinates
     */
    private static double convertToRadian(double coordinate) {
        return coordinate * (Math.PI / 180.0);
    }
}
