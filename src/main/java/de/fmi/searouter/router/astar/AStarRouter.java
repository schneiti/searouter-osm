package de.fmi.searouter.router.astar;

import de.fmi.searouter.dijkstragrid.Edge;
import de.fmi.searouter.dijkstragrid.Grid;
import de.fmi.searouter.dijkstragrid.Node;
import de.fmi.searouter.router.Router;
import de.fmi.searouter.router.RoutingResult;
import de.fmi.searouter.utils.IntersectionHelper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Used to find a route when given a start and end node using A*.
 */
public class AStarRouter implements Router {

    //current distance to the target node
    protected final double[] currDistanceToNodeAStar;
    protected final int[] realDistanceToNode;
    protected final double[] distanceToTarget;
    //previous node on the way to the target node
    private final int[] previousNode;
    private final AStarHeap vertexHeap;
    private final boolean[] nodeTouched;

    private int destNodeIdx;

    /**
     * constructor. also initializes internal fields
     */
    public AStarRouter() {
        this.currDistanceToNodeAStar = new double[Node.getSize()];
        this.realDistanceToNode = new int[Node.getSize()];
        this.previousNode = new int[Node.getSize()];
        this.distanceToTarget = new double[Node.getSize()];
        this.nodeTouched = new boolean[Node.getSize()];
        this.vertexHeap = new AStarHeap(this);

        Arrays.fill(currDistanceToNodeAStar, Double.MAX_VALUE);
        Arrays.fill(distanceToTarget, -1);
        Arrays.fill(realDistanceToNode, Integer.MAX_VALUE);
        Arrays.fill(previousNode, -1);
        Arrays.fill(nodeTouched, false);
    }

    /**
     * resets the state of a previous calculation
     */
    private void resetState() {
        Arrays.fill(currDistanceToNodeAStar, Integer.MAX_VALUE);
        Arrays.fill(realDistanceToNode, Integer.MAX_VALUE);
        Arrays.fill(distanceToTarget, -1);
        Arrays.fill(previousNode, -1);
        Arrays.fill(nodeTouched, false);

        vertexHeap.resetState();
    }

    /**
     * This is the heuristic function for A* estimating dist(v, t) with t being the destination point
     * of a routing query.
     *
     * @param currNodeIdx The node index for which the heuristic should be calculated.
     * @return The heuristics estimation value.
     */
    private double pi(int currNodeIdx) {
       return IntersectionHelper.getDistance(Node.getLatitude(currNodeIdx), Node.getLongitude(currNodeIdx), Node.getLatitude(destNodeIdx), Node.getLongitude(destNodeIdx));
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
        resetState();

        this.destNodeIdx = destNodeIdx;

        currDistanceToNodeAStar[startNodeIdx] = 0;
        realDistanceToNode[startNodeIdx] = 0;
        previousNode[startNodeIdx] = startNodeIdx;
        vertexHeap.add(startNodeIdx);

        // Counts how many nodes where popped out the heap, meaning that they had the "labeled" status
        int vertexPoppedOutCount = 0;

        while (!vertexHeap.isEmpty()) {
            int nodeToHandleId = vertexHeap.getNext();
            vertexPoppedOutCount++;

            nodeTouched[nodeToHandleId] = true;

            // Break early if target node reached
            if (nodeToHandleId == destNodeIdx) {
                break;
            }

            for (int neighbourEdgeId = Grid.offset[nodeToHandleId]; neighbourEdgeId < Grid.offset[nodeToHandleId + 1]; ++neighbourEdgeId) {

                int destinationVertexId = Edge.getDest(neighbourEdgeId);

                if (nodeTouched[destinationVertexId]) {
                    continue;
                }

                if (distanceToTarget[destinationVertexId] < 0) {
                    distanceToTarget[destinationVertexId] = pi(destinationVertexId);
                }

                // Calculate the distance to the destination vertex using the current edge
                int newRealDistanceOverThisEdgeToDestVertex = (realDistanceToNode[nodeToHandleId] + Edge.getDist(neighbourEdgeId));
                double newDistanceOverThisEdgeToDestVertex =  newRealDistanceOverThisEdgeToDestVertex + distanceToTarget[destinationVertexId];

                // If the new calculated distance to the destination vertex is lower as the previously known, update the corresponding data structures
                if (newRealDistanceOverThisEdgeToDestVertex < realDistanceToNode[destinationVertexId]) {
                    currDistanceToNodeAStar[destinationVertexId] = newDistanceOverThisEdgeToDestVertex;
                    realDistanceToNode[destinationVertexId] = newRealDistanceOverThisEdgeToDestVertex;
                    previousNode[destinationVertexId] = nodeToHandleId;
                    vertexHeap.add(destinationVertexId);
                }

            }
        }

        // Here, we are done with dijkstra but need to gather all relevant data from the resulting data structures
        List<Integer> path = new ArrayList<>();
        int currNodeUnderInvestigation = destNodeIdx;

        path.add(destNodeIdx);
        while (currNodeUnderInvestigation != startNodeIdx) {
            if (currNodeUnderInvestigation < 0) {
                path = new ArrayList<>();
                path.add(destNodeIdx);
                path.add(startNodeIdx);
                long stopTime = System.nanoTime();
                return new RoutingResult(false, path, Integer.MAX_VALUE, (double) (stopTime - startTime) / 1000000, vertexPoppedOutCount);
            }

            int previousNodeIdx = previousNode[currNodeUnderInvestigation];
            path.add(previousNodeIdx);
            currNodeUnderInvestigation = previousNodeIdx;
        }

        // Reverse order of path and save it to array
        Collections.reverse(path);
        long stopTime = System.nanoTime();

        return new RoutingResult(true, path, realDistanceToNode[destNodeIdx], (double) (stopTime - startTime) / 1000000, vertexPoppedOutCount);
    }

    @Override
    public String getName() {
        return "AStar";
    }
}
