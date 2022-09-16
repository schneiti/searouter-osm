package de.fmi.searouter.router.alt.symmetricastar;

import de.fmi.searouter.dijkstragrid.Edge;
import de.fmi.searouter.dijkstragrid.Grid;
import de.fmi.searouter.dijkstragrid.Node;
import de.fmi.searouter.landmarks.Landmark;
import de.fmi.searouter.router.Router;
import de.fmi.searouter.router.RoutingResult;
import de.fmi.searouter.router.alt.ALTRouter;
import de.fmi.searouter.utils.IntersectionHelper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Router using an ALT variant of a symmetric bidirectional A* implementation.
 */
public class AltBiAStarSymmetric implements ALTRouter {

    private int maxNumberOfQueryLandmarks = 5; // default


    //current distance to the target node
    protected final int[] currDistanceToNodeForward;
    protected final double[] currDistanceToNodeForwardAStar;
    protected final int[] currDistanceToNodeBackward;
    protected final double[] currDistanceToNodeBackwardAStar;

    protected final double[] distanceToTargetForward;
    protected final double[] distanceToTargetBackward;

    //previous node on the way to the target node
    private final int[] previousNodeForward;
    private final int[] previousNodeBackward;
    private final AltSymmetricBiAStarHeapForward vertexHeapForward;
    private final AltSymmetricBiAStarHeapBackward vertexHeapBackward;
    private final boolean[] nodeTouchedForward;
    private final boolean[] nodeTouchedBackward;

    private int currentSmallestDistnace;
    private int currentBestWayForwardEndId;
    private int currentBestWayBackwardEndId;

    private int startNodeIdx;
    private int destNodeIdx;

    /**
     * constructor. also initializes internal fields
     */
    public AltBiAStarSymmetric() {
        this.currDistanceToNodeForward = new int[Node.getSize()];
        this.currDistanceToNodeForwardAStar = new double[Node.getSize()];
        this.currDistanceToNodeBackward = new int[Node.getSize()];
        this.currDistanceToNodeBackwardAStar = new double[Node.getSize()];
        this.previousNodeForward = new int[Node.getSize()];
        this.previousNodeBackward = new int[Node.getSize()];
        this.nodeTouchedForward = new boolean[Node.getSize()];
        this.nodeTouchedBackward = new boolean[Node.getSize()];
        this.vertexHeapForward = new AltSymmetricBiAStarHeapForward(this);
        this.vertexHeapBackward = new AltSymmetricBiAStarHeapBackward(this);
        this.distanceToTargetForward = new double[Node.getSize()];
        this.distanceToTargetBackward = new double[Node.getSize()];


        this.currentSmallestDistnace = Integer.MAX_VALUE;
        this.currentBestWayForwardEndId = -1;
        this.currentBestWayBackwardEndId = -1;

        Arrays.fill(currDistanceToNodeForward, Integer.MAX_VALUE);
        Arrays.fill(currDistanceToNodeForwardAStar, Double.MAX_VALUE);
        Arrays.fill(currDistanceToNodeBackward, Integer.MAX_VALUE);
        Arrays.fill(currDistanceToNodeBackwardAStar, Double.MAX_VALUE);
        Arrays.fill(previousNodeForward, -1);
        Arrays.fill(previousNodeBackward, -1);
        Arrays.fill(nodeTouchedForward, false);
        Arrays.fill(nodeTouchedBackward, false);
        Arrays.fill(distanceToTargetForward, -1);
        Arrays.fill(distanceToTargetBackward, -1);
    }

    /**
     * resets the state of a previous calculation
     */
    private void resetState(int startNodeIdx, int destNodeIdx) {
        Arrays.fill(currDistanceToNodeForward, Integer.MAX_VALUE);
        Arrays.fill(currDistanceToNodeForwardAStar, Double.MAX_VALUE);

        Arrays.fill(currDistanceToNodeBackward, Integer.MAX_VALUE);
        Arrays.fill(currDistanceToNodeBackwardAStar, Double.MAX_VALUE);

        Arrays.fill(previousNodeForward, -1);
        Arrays.fill(previousNodeBackward, -1);
        Arrays.fill(nodeTouchedForward, false);
        Arrays.fill(nodeTouchedBackward, false);

        Arrays.fill(distanceToTargetForward, -1);
        Arrays.fill(distanceToTargetBackward, -1);

        this.currentSmallestDistnace = Integer.MAX_VALUE;

        this.currentBestWayForwardEndId = -1;
        this.currentBestWayBackwardEndId = -1;

        vertexHeapForward.resetState();
        vertexHeapBackward.resetState();

        this.startNodeIdx = startNodeIdx;
        this.destNodeIdx = destNodeIdx;

        currDistanceToNodeForwardAStar[startNodeIdx] = 0;
        currDistanceToNodeForward[startNodeIdx] = 0;
        currDistanceToNodeBackward[destNodeIdx] = 0;
        currDistanceToNodeBackwardAStar[destNodeIdx] = 0;
        previousNodeForward[startNodeIdx] = startNodeIdx;
        previousNodeBackward[destNodeIdx] = destNodeIdx;
        vertexHeapForward.add(startNodeIdx);
        vertexHeapBackward.add(destNodeIdx);

    }

    /**
     * Estimate dist(v, t) where t is target node
     *
     * @param currNodeIdx The index of vertex v.
     * @return The distance estimate dist(v, t).
     */
    private double pi_f(int currNodeIdx) {
        return Landmark.distance(currNodeIdx, destNodeIdx);
    }

    /**
     * Estimate dist(v, s) where s is start node
     *
     * @param currNodeIdx The index of vertex v.
     * @return The distance estimate dist(v, s).
     */
    private double pi_r(int currNodeIdx) {
        return Landmark.distance(currNodeIdx, startNodeIdx);
    }

    /**
     * Calculates the shortest path from one start node to a destination node. Node definitions
     * are in {@link Node}, edge definition in {@link Edge} and the relationships between those two
     * data structures in {@link Grid}.
     *
     * @param startNodeIdx The index of the start node (corresponding to {@link Node} indices)
     * @param destNodeIdx  The index of the destination node (corresponding to {@link Node} indices)
     * @return a route between start and destination node
     */
    @Override
    public RoutingResult route(int startNodeIdx, int destNodeIdx) {
        long startTime = System.nanoTime();
        resetState(startNodeIdx, destNodeIdx);
        Landmark.prepareLandmarksForQueryPhase(maxNumberOfQueryLandmarks, startNodeIdx, destNodeIdx);


        // Counts how many nodes where popped out the heap, meaning that they had the "labeled" status
        int vertexPoppedOutCount = 0;

        while (!vertexHeapForward.isEmpty() && !vertexHeapBackward.isEmpty()) {

            // ========================
            // ==== FORWARD SEARCH ====
            // ========================

            int nodeToHandleIdForward = vertexHeapForward.getNext();
            vertexPoppedOutCount++;
            nodeTouchedForward[nodeToHandleIdForward] = true;

            if (currDistanceToNodeForwardAStar[nodeToHandleIdForward] > currentSmallestDistnace) {
                break;
            }

            for (int neighbourEdgeId = Grid.offset[nodeToHandleIdForward]; neighbourEdgeId < Grid.offset[nodeToHandleIdForward + 1]; ++neighbourEdgeId) {

                int destinationVertexId = Edge.getDest(neighbourEdgeId);


                if (nodeTouchedForward[destinationVertexId]) {
                    continue;
                }


                if (nodeTouchedBackward[destinationVertexId]) {
                    int newRealDistanceOverall = currDistanceToNodeForward[nodeToHandleIdForward] + Edge.getDist(neighbourEdgeId) + currDistanceToNodeBackward[destinationVertexId];

                    if (currentSmallestDistnace > newRealDistanceOverall) {
                        currentSmallestDistnace = newRealDistanceOverall;
                        currentBestWayForwardEndId = nodeToHandleIdForward;
                        currentBestWayBackwardEndId = destinationVertexId;
                    }
                } else { // Optimization suggested by Kwa

                    if (distanceToTargetForward[destinationVertexId] < 0) {
                        distanceToTargetForward[destinationVertexId] = pi_f(destinationVertexId);
                    }

                    // Calculate the distance to the destination vertex using the current edge
                    int newDistanceOverThisEdgeToDestVertex = currDistanceToNodeForward[nodeToHandleIdForward] + Edge.getDist(neighbourEdgeId);
                    double newDistanceOverThisEdgeToDestVertexAStar = newDistanceOverThisEdgeToDestVertex + distanceToTargetForward[destinationVertexId];

                    // If the new calculated distance to the destination vertex is lower as the previously known, update the corresponding data structures
                    if (newDistanceOverThisEdgeToDestVertex < currDistanceToNodeForward[destinationVertexId]) {
                        currDistanceToNodeForward[destinationVertexId] = newDistanceOverThisEdgeToDestVertex;
                        currDistanceToNodeForwardAStar[destinationVertexId] = newDistanceOverThisEdgeToDestVertexAStar;
                        previousNodeForward[destinationVertexId] = nodeToHandleIdForward;
                        vertexHeapForward.add(destinationVertexId);
                    }
                }


            }

            // =========================
            // ==== BACKWARD SEARCH ====
            // =========================

            int nodeToHandleIdBackward = vertexHeapBackward.getNext();
            vertexPoppedOutCount++;
            nodeTouchedBackward[nodeToHandleIdBackward] = true;

            if (currDistanceToNodeBackwardAStar[nodeToHandleIdBackward] > currentSmallestDistnace) {
                break;
            }

            for (int neighbourEdgeId = Grid.offset[nodeToHandleIdBackward]; neighbourEdgeId < Grid.offset[nodeToHandleIdBackward + 1]; ++neighbourEdgeId) {

                int destinationVertexId = Edge.getDest(neighbourEdgeId);

                if (nodeTouchedBackward[destinationVertexId]) {
                    continue;
                }

                if (nodeTouchedForward[destinationVertexId]) {
                    int newRealDistanceOverall = currDistanceToNodeBackward[nodeToHandleIdBackward] + Edge.getDist(neighbourEdgeId) + currDistanceToNodeForward[destinationVertexId];
                    if (currentSmallestDistnace > newRealDistanceOverall) {
                        currentSmallestDistnace = newRealDistanceOverall;
                        currentBestWayBackwardEndId = nodeToHandleIdBackward;
                        currentBestWayForwardEndId = destinationVertexId;
                    }
                } else { // Optimization proposed by Kwa

                    if (distanceToTargetBackward[destinationVertexId] < 0) {
                        distanceToTargetBackward[destinationVertexId] = pi_r(destinationVertexId);
                    }

                    // Calculate the distance to the destination vertex using the current edge
                    int newDistanceOverThisEdgeToDestVertex = currDistanceToNodeBackward[nodeToHandleIdBackward] + Edge.getDist(neighbourEdgeId);
                    double newDistanceOverThisEdgeToDestVertexAStar = newDistanceOverThisEdgeToDestVertex + distanceToTargetBackward[destinationVertexId];


                    // If the new calculated distance to the destination vertex is lower as the previously known, update the corresponding data structures
                    if (newDistanceOverThisEdgeToDestVertex < currDistanceToNodeBackward[destinationVertexId]) {
                        currDistanceToNodeBackward[destinationVertexId] = newDistanceOverThisEdgeToDestVertex;
                        currDistanceToNodeBackwardAStar[destinationVertexId] = newDistanceOverThisEdgeToDestVertexAStar;
                        previousNodeBackward[destinationVertexId] = nodeToHandleIdBackward;
                        vertexHeapBackward.add(destinationVertexId);
                    }
                }


            }

        }


        // ========================
        // ==== STOP CRITERION ====
        // ========================

        if (this.currentBestWayForwardEndId < 0) {
            long stopTime = System.nanoTime();
            return new RoutingResult(false, Arrays.asList(startNodeIdx, destNodeIdx), Integer.MAX_VALUE, (double) (stopTime - startTime) / 1000000, vertexPoppedOutCount);
        }

        List<Integer> path = new ArrayList<>();
        List<Integer> firstPathList = getPathToNodeWithIDForward(this.currentBestWayForwardEndId, startNodeIdx);
        List<Integer> secondPathList = getPathToNodeWithIDBackward(this.currentBestWayBackwardEndId, destNodeIdx);

        if (firstPathList.size() <= 0 && secondPathList.size() <= 0) {
            long stopTime = System.nanoTime();
            return new RoutingResult(false, Arrays.asList(startNodeIdx, destNodeIdx), Integer.MAX_VALUE, (double) (stopTime - startTime) / 1000000, vertexPoppedOutCount);
        }

        path.addAll(getPathToNodeWithIDForward(this.currentBestWayForwardEndId, startNodeIdx));
        path.addAll(getPathToNodeWithIDBackward(this.currentBestWayBackwardEndId, destNodeIdx));

        long stopTime = System.nanoTime();

        return new RoutingResult(true, path, currentSmallestDistnace, (double) (stopTime - startTime) / 1000000, vertexPoppedOutCount);
    }

    @Override
    public String getName() {
        return "ALT: Bidirectional AStar Symmetric, noActiveLandmarks" + maxNumberOfQueryLandmarks;
    }

    public List<Integer> getPathToNodeWithIDForward(int id, int startNodeIdx) {
        List<Integer> path = new ArrayList<>();
        int currNodeUnderInvestigation = id;

        path.add(id);
        while (currNodeUnderInvestigation != startNodeIdx) {
            int previousNodeIdx = previousNodeForward[currNodeUnderInvestigation];
            if (previousNodeIdx < 0) {
                return null;
            }
            path.add(previousNodeIdx);
            currNodeUnderInvestigation = previousNodeIdx;
        }

        // Reverse order of path and save it to array
        Collections.reverse(path);
        return path;
    }

    public List<Integer> getPathToNodeWithIDBackward(int id, int destNodeIdx) {
        List<Integer> path = new ArrayList<>();
        int currNodeUnderInvestigation = id;

        path.add(id);
        while (currNodeUnderInvestigation != destNodeIdx) {
            int previousNodeIdx = previousNodeBackward[currNodeUnderInvestigation];
            if (previousNodeIdx < 0) {
                return null;
            }
            path.add(previousNodeIdx);
            currNodeUnderInvestigation = previousNodeIdx;
        }
        return path;
    }

    @Override
    public void setMaxNumberOfQueryLandmarks(int maxLandmarks) {
        this.maxNumberOfQueryLandmarks = maxLandmarks;
    }
}