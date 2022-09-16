package de.fmi.searouter.coastlinegrid;

import java.io.Serializable;

/**
 * Helper class for enabling a (de)serialization mechanism of the static {@link CoastlineWays} class.
 */
class CoastlineWriter implements Serializable {

    private final int[] edgePosStart;
    private final float[] pointLon;
    private final float[] pointLat;

    public int[] getEdgePosStart() {
        return edgePosStart;
    }

    public float[] getPointLon() {
        return pointLon;
    }

    public float[] getPointLat() {
        return pointLat;
    }

    CoastlineWriter(int[] edgePosStart, float[] pointLat, float[] pointLon) {
        this.edgePosStart = edgePosStart;
        this.pointLat = pointLat;
        this.pointLon = pointLon;
    }
}
