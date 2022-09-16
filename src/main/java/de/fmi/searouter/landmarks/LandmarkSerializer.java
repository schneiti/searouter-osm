package de.fmi.searouter.landmarks;

import java.io.Serializable;

/**
 * Enables a serialization mechanism for already pre-calculated landmarks that were serialized
 * on disk.
 */
public class LandmarkSerializer implements Serializable {

    public  int[][] distanceOfLandmarkToEveryVertex;

    public  int[] landmarkNodeIDs;

    public LandmarkSerializer(int[][] distanceOfLandmarkToEveryVertex, int[] landmarkNodeIDs) {
        this.distanceOfLandmarkToEveryVertex = distanceOfLandmarkToEveryVertex;
        this.landmarkNodeIDs = landmarkNodeIDs;
    }

    public int[][] getDistanceOfLandmarkToEveryVertex() {
        return distanceOfLandmarkToEveryVertex;
    }

    public int[] getLandmarkNodeIDs() {
        return landmarkNodeIDs;
    }
}
