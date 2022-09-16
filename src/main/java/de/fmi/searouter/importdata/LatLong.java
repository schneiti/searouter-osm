package de.fmi.searouter.importdata;

/**
 * Stores latitude and longitude values for a JSON unmarshalling needed for the
 * HTTP API implementation.
 */
public class LatLong {

    private double latitude;
    private double longitude;

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
}
