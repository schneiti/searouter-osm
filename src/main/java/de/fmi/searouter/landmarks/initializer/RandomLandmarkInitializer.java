package de.fmi.searouter.landmarks.initializer;

import de.fmi.searouter.dijkstragrid.Node;
import de.fmi.searouter.landmarks.LandmarkInitializer;
import de.fmi.searouter.router.dijkstra.DijkstraRouter;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Distributes landmarks randomly over the earths map.
 */
public class RandomLandmarkInitializer implements LandmarkInitializer {

    /**
     * Stores for each landmark all distances to all other vertices in the graph.
     */
    private int[][] distanceOfLandmarkToEveryVertex;

    /**
     * Stores for each landmark the vertex ID like defined in {@link Node}
     */
    private int[] landmarkNodeIDs;

    /**
     * Number of landmarks that should be drawn randomly
     */
    private final int NO_OF_LANDMARKS_RANDOM = 370;

    /**
     * Stats the distribution of landmarks by choosing randomly vertices from the graph.
     */
    public RandomLandmarkInitializer() {

        distanceOfLandmarkToEveryVertex = new int[NO_OF_LANDMARKS_RANDOM][Node.getSize()];
        landmarkNodeIDs = new int[NO_OF_LANDMARKS_RANDOM];

        int maxNodeIdx = Node.getSize() - 1;

        DijkstraRouter router = new DijkstraRouter();

        // Random mode
        for (int noOfDraws = 0; noOfDraws < NO_OF_LANDMARKS_RANDOM; noOfDraws++) {
            int nodeIdx = ThreadLocalRandom.current().nextInt(0, maxNodeIdx + 1);
            landmarkNodeIDs[noOfDraws] = nodeIdx;
            int dest = ThreadLocalRandom.current().nextInt(0, maxNodeIdx + 1);
            distanceOfLandmarkToEveryVertex[noOfDraws] = router.routeToAllVertices(nodeIdx, dest);
        }
    }


    @Override
    public String getSerFileName() {
        return "landmarks_random_mode";
    }

    @Override
    public int[][] getDistanceOfLandmarkToEveryVertex() {
        return distanceOfLandmarkToEveryVertex;
    }

    @Override
    public int[] getLandmarkNodeIDs() {
        return landmarkNodeIDs;
    }
}
