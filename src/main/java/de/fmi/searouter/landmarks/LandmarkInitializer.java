package de.fmi.searouter.landmarks;

/**
 * A LandmarkInitializer distributes landmarks on earths water surface using a certain
 * distribution strategy.
 */
public interface LandmarkInitializer {

    /**
     * Get the name of a file which should be used as a file name for the serialization mechanism.
     *
     * @return The file name.
     */
    String getSerFileName();

    /**
     * Get an array which stores for each landmark (identified by the first array index) the distances
     * to all other vertices in the graph defined by {@link de.fmi.searouter.dijkstragrid.Grid}.
     *
     * @return An two-dimensional array with the first index representing different landmarks and the second
     * one distances to all other vertices in the grid graph.
     */
    int[][] getDistanceOfLandmarkToEveryVertex();

    /**
     * Get the node IDs for each landmark as defined in {@link de.fmi.searouter.dijkstragrid.Node}.
     * This allows a mapping of landmark id (being the index in the landmark arrays) to vertices ID
     * in the graph.
     *
     * @return An array with the node IDs of landmarks.
     */
    int[] getLandmarkNodeIDs();

}
