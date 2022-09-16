package de.fmi.searouter.landmarks;

public class LandmarkIDLowerBoundMapping implements Comparable<LandmarkIDLowerBoundMapping> {
    public int landmarkID;
    public double lowerBound;

    public LandmarkIDLowerBoundMapping(int landmarkID, double lowerBound) {
        this.landmarkID = landmarkID;
        this.lowerBound = lowerBound;
    }

    @Override
    public int compareTo(LandmarkIDLowerBoundMapping o) {
        return Double.compare(lowerBound, o.lowerBound);
    }
}
