package de.fmi.searouter.landmarks.initializer;

import de.fmi.searouter.dijkstragrid.Edge;
import de.fmi.searouter.dijkstragrid.Node;
import de.fmi.searouter.landmarks.LandmarkInitializer;
import de.fmi.searouter.router.dijkstra.DijkstraRouter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Distributes landmarks along coastlines by using the heuristic of keeping a certain distance (on the
 * graph paths) between coastline landmarks. This should ideally result in a equal distribution of landmarks
 * around coastlines.
 */
public class CoastlineLandmarkInitializer implements LandmarkInitializer {

    /**
     * Stores for each landmark all distances to all other vertices in the graph.
     */
    private int[][] distanceOfLandmarkToEveryVertex;

    /**
     * Stores for each landmark the vertex ID like defined in {@link Node}
     */
    private int[] landmarkNodeIDs;

    /**
     * The number of landmarks that are tried to be retrieved using the heuristic.
     */
    private final int NUMBER_OF_LANDMARKS = 508;

    public CoastlineLandmarkInitializer() {

        // The minimum distance two coastline landmarks are allowed to have
        int distance = 1000 * 1000;
        System.out.println("distance " + distance);


        /*
         * Retrieve all nodes in the graph that are next to a coastline and thus candidates.
         */

        int[] noOfOutgoingEdgesOfNode = new int[Node.getSize()];
        // We can assume that the graph is undirected as the import secures this.
        for (int edgeIdx = 0; edgeIdx < Edge.getSize(); edgeIdx++) {
            noOfOutgoingEdgesOfNode[Edge.getStart(edgeIdx)]++;
        }
        List<Integer> allCoastlineNodes = new ArrayList<>();
        for (int nodeIdx = 0; nodeIdx < Node.getSize(); nodeIdx++) {
            if (noOfOutgoingEdgesOfNode[nodeIdx] < 4 && noOfOutgoingEdgesOfNode[nodeIdx] > 0) {
                allCoastlineNodes.add(nodeIdx);
            }
        }

        /*
         * Add landmarks from the pre-calculated candidate subset if the distance constraint
         * is fulfilled.
         */

        DijkstraRouter router = new DijkstraRouter();
        List<Integer> currLandmarks = new ArrayList<>();
        Random rand = new Random(4304344);
        // Shuffle the coastlines to assure that there is no pre-sorting that might affect the strategy somehow
        Collections.shuffle(allCoastlineNodes, rand);

        currLandmarks.add(allCoastlineNodes.get(0));
        int currCoastlineNodeIdx = 1;
        for (int landMarkCount = 1; landMarkCount < NUMBER_OF_LANDMARKS; landMarkCount++) {


            // Get a new landmark candidate
            int newLandmarkCandidate = -1;
            for (;currCoastlineNodeIdx < allCoastlineNodes.size(); ++currCoastlineNodeIdx) {
                boolean currLandmarkNotSuitable = false;

                newLandmarkCandidate = allCoastlineNodes.get(currCoastlineNodeIdx);

                // Check whether the candidate fulfills the properties

                int[] distancesOfCandidate = router.routeToAllVertices(newLandmarkCandidate, 4);
                for (Integer i : currLandmarks) {
                    if (distancesOfCandidate[i] < distance) {
                        currLandmarkNotSuitable = true;
                        break;
                    }
                }

                if (currLandmarkNotSuitable) {
                    continue;
                }

                currLandmarks.add(newLandmarkCandidate);
                System.out.println(landMarkCount + "Added coastline vertex as landmark " + currCoastlineNodeIdx);
                currCoastlineNodeIdx++;
                break;
            }
        }

        distanceOfLandmarkToEveryVertex = new int[currLandmarks.size()][Node.getSize()];

        landmarkNodeIDs = new int[currLandmarks.size()];

        for (int i = 0; i < currLandmarks.size(); i++) {
            landmarkNodeIDs[i] = currLandmarks.get(i);
            distanceOfLandmarkToEveryVertex[i] = router.routeToAllVertices(currLandmarks.get(i), 4);
        }

        System.out.println("No landmarks: " + NUMBER_OF_LANDMARKS);
        System.out.println("all coastline nodes: " + allCoastlineNodes.size());
    }

    @Override
    public String getSerFileName() {
        return "landmarks_equal_distributed_coastline";
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
