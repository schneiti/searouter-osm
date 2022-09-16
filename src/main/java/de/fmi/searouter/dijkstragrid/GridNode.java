package de.fmi.searouter.dijkstragrid;

import java.math.BigDecimal;

/**
 * This is an object used temporarily when to store information used for building the adjacency array structure
 * of the graph grid structure.
 */
public class GridNode {

    private double latitude;
    private double longitude;

    private int id;

    public GridNode(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Calculates the grid node neighbor to the north
     * @param latitudeOffset The offset between two neighbor nodes between the north/south
     * @return A new {@link GridNode} object representing the neighbor node.
     */
    public GridNode calcNorthernNode(double latitudeOffset) {
        BigDecimal offset = BigDecimal.valueOf(latitudeOffset);

        BigDecimal nLatitude = BigDecimal.valueOf(latitude).add(offset);

        //check if resulting node "too far north"
        if (nLatitude.doubleValue() > 90.0) {
            return null;
        }

        return new GridNode(nLatitude.doubleValue(), longitude);
    }

    /**
     * Calculates the grid node neighbor to the south
     * @param latitudeOffset The offset between two neighbor nodes between the north/south
     * @return A new {@link GridNode} object representing the neighbor node.
     */
    public GridNode calcSouthernNode(double latitudeOffset) {
        BigDecimal offset = BigDecimal.valueOf(latitudeOffset);

        BigDecimal nLatitude = BigDecimal.valueOf(latitude).subtract(offset);

        //check if resulting node "too far south"
        if (nLatitude.doubleValue() < -90.0) {
            return null;
        }

        return new GridNode(nLatitude.doubleValue(), longitude);
    }

    /**
     * Calculates the grid node neighbor to the east
     * @param longitudeOffset The offset between two neighbor nodes between the east/west
     * @return A new {@link GridNode} object representing the neighbor node.
     */
    public GridNode calcEasternNode(double longitudeOffset) {
        BigDecimal offset = BigDecimal.valueOf(longitudeOffset);

        BigDecimal nLongitude = BigDecimal.valueOf(longitude).add(offset);

        if (nLongitude.doubleValue() > 180) {
            nLongitude = BigDecimal.valueOf(-180).add(nLongitude.remainder(BigDecimal.valueOf(180)));
        }

        return new GridNode(latitude, nLongitude.doubleValue());
    }

    /**
     * Calculates the grid node neighbor laying to the west
     * @param longitudeOffset The offset between two neighbor nodes between the east/west
     * @return A new {@link GridNode} object representing the neighbor node.
     */
    public GridNode calcWesternNode(double longitudeOffset) {
        BigDecimal offset = BigDecimal.valueOf(longitudeOffset);

        BigDecimal nLongitude = BigDecimal.valueOf(longitude).subtract(offset);

        if (nLongitude.doubleValue() < -180.0) {
            // nLongitude = nLongitude % 180;
            nLongitude = nLongitude.remainder(BigDecimal.valueOf(180));
        }

        return new GridNode(latitude, nLongitude.doubleValue());
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
