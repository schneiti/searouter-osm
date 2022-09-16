package de.fmi.searouter.landmarks.initializer;

import de.fmi.searouter.dijkstragrid.Grid;
import de.fmi.searouter.dijkstragrid.Node;
import de.fmi.searouter.landmarks.LandmarkInitializer;
import de.fmi.searouter.router.dijkstra.DijkstraRouter;

import java.util.ArrayList;
import java.util.List;

/**
 * Tries to distribute landmarks equally over a 2D map, assuming that the coordinate degree borders
 * have a constant distance to each other. On earth (which is not flat) this ends up in a distribution
 * where at the poles the distribution becomes tighter.
 */
public class EqualDistributed2DMapInitializer implements LandmarkInitializer {

    /**
     * Stores for each landmark all distances to all other vertices in the graph.
     */
    private int[][] distanceOfLandmarkToEveryVertex;

    /**
     * Stores for each landmark the vertex ID like defined in {@link Node}
     */
    private int[] landmarkNodeIDs;

    /**
     * The distance of two landmarks in degrees that is considered by this distribution.
     */
    private int degreeDistanceBtwTwoLandmarks = 13;

    public EqualDistributed2DMapInitializer() {
        List<Integer> currChosenLandmarks = new ArrayList<>();

        // Choose landmarks on the earths map
        for (int latIdx = -90 / degreeDistanceBtwTwoLandmarks; latIdx <= 90 / degreeDistanceBtwTwoLandmarks; latIdx++) {
            for (int lonIdx = -180 / degreeDistanceBtwTwoLandmarks; lonIdx < 180 / degreeDistanceBtwTwoLandmarks; lonIdx++) {
                int node = Grid.getNearestGridNodeByCoordinates(latIdx * degreeDistanceBtwTwoLandmarks, lonIdx * degreeDistanceBtwTwoLandmarks, true);
                if (node >= 0) {
                    currChosenLandmarks.add(node);
                }
            }
        }

        int noLandmarks = currChosenLandmarks.size();
        distanceOfLandmarkToEveryVertex = new int[noLandmarks][Node.getSize()];
        landmarkNodeIDs = new int[noLandmarks];

        // For every chosen landmark: Pre-calculate the distances to all other vertices in the graph
        DijkstraRouter router = new DijkstraRouter();
        for (int i = 0; i < currChosenLandmarks.size(); i++) {
            landmarkNodeIDs[i] = currChosenLandmarks.get(i);
            distanceOfLandmarkToEveryVertex[i] = router.routeToAllVertices(currChosenLandmarks.get(i), 4);
        }

        System.out.println("No-landmarks: " + noLandmarks);
    }

    @Override
    public String getSerFileName() {
        return "landmarks_equal_distributed_2d_map";
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
