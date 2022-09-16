package de.fmi.searouter.importdata;

/**
 * Represents an OSM node. Only used for pre-processing (until grid is initialized).
 */
public class Point {

    /**
     * Latitude coordinate of the node.
     */
    private final float lat;

    /**
     * Longitude coordinate of the node.
     */
    private final float lon;

    /**
     * OSM ID of this node.
     */
    private final long id;

    /**
     * Initializes a new Point.
     *
     * @param id The OSM ID of the node.
     * @param lat The latitude coordinate of the node.
     * @param lon The longitude coordinate of the node.
     */
    public Point(long id, float lat, float lon) {
        this.lat = lat;
        this.lon = lon;
        this.id = id;
    }

    public float getLat() {
        return lat;
    }

    public float getLon() {
        return lon;
    }

    public long getId() {
        return id;
    }
}
