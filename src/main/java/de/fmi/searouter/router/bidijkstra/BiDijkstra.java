package de.fmi.searouter.router.bidijkstra;

import de.fmi.searouter.dijkstragrid.Edge;
import de.fmi.searouter.dijkstragrid.Grid;
import de.fmi.searouter.dijkstragrid.Node;
import de.fmi.searouter.router.Router;
import de.fmi.searouter.router.RoutingResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Router using a bidirectional dijkstra implementation.
 */
@Component
public class BiDijkstra implements Router {
    //current distance to the target node
    protected final int[] currDistanceToNodeForward;
    protected final int[] currDistanceToNodeBackward;
    //previous node on the way to the target node
    private final int[] previousNodeForward;
    private final int[] previousNodeBackward;
    private final BiDijkstraHeapForward vertexHeapForward;
    private final BiDijkstraHeapFBackward vertexHeapBackward;
    private final boolean[] nodeTouchedForward;
    private final boolean[] nodeTouchedBackward;

    private int currentSmallestDistnace;
    private int currentBestWayForwardEndId;
    private int currentBestWayBackwardEndId;

    /**
     * constructor. also initializes internal fields
     */
    public BiDijkstra() {
        this.currDistanceToNodeForward = new int[Node.getSize()];
        this.currDistanceToNodeBackward = new int[Node.getSize()];
        this.previousNodeForward = new int[Node.getSize()];
        this.previousNodeBackward = new int[Node.getSize()];
        this.nodeTouchedForward = new boolean[Node.getSize()];
        this.nodeTouchedBackward = new boolean[Node.getSize()];
        this.vertexHeapForward = new BiDijkstraHeapForward(this);
        this.vertexHeapBackward = new BiDijkstraHeapFBackward(this);

        this.currentSmallestDistnace = Integer.MAX_VALUE;
        this.currentBestWayForwardEndId = -1;
        this.currentBestWayBackwardEndId = -1;

        Arrays.fill(currDistanceToNodeForward, Integer.MAX_VALUE);
        Arrays.fill(currDistanceToNodeBackward, Integer.MAX_VALUE);
        Arrays.fill(previousNodeForward, -1);
        Arrays.fill(previousNodeBackward, -1);
        Arrays.fill(nodeTouchedForward, false);
        Arrays.fill(nodeTouchedBackward, false);
    }

    /**
     * resets the state of a previous calculation
     */
    private void resetState() {
        Arrays.fill(currDistanceToNodeForward, Integer.MAX_VALUE);

        Arrays.fill(currDistanceToNodeBackward, Integer.MAX_VALUE);

        Arrays.fill(previousNodeForward, -1);
        Arrays.fill(previousNodeBackward, -1);
        Arrays.fill(nodeTouchedForward, false);
        Arrays.fill(nodeTouchedBackward, false);

        this.currentSmallestDistnace = Integer.MAX_VALUE;
        this.currentBestWayForwardEndId = -1;
        this.currentBestWayBackwardEndId = -1;

        vertexHeapForward.resetState();
        vertexHeapBackward.resetState();
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

        currDistanceToNodeForward[startNodeIdx] = 0;
        currDistanceToNodeBackward[destNodeIdx] = 0;
        previousNodeForward[startNodeIdx] = startNodeIdx;
        previousNodeBackward[destNodeIdx] = destNodeIdx;
        vertexHeapForward.add(startNodeIdx);
        vertexHeapBackward.add(destNodeIdx);

        // Counts how many nodes where popped out the heap, meaning that they had the "labeled" status
        int vertexPoppedOutCount = 0;

        while (!vertexHeapForward.isEmpty() && !vertexHeapBackward.isEmpty()) {

            // ========================
            // ==== FORWARD SEARCH ====
            // ========================

            int nodeToHandleIdForward = vertexHeapForward.getNext();
            vertexPoppedOutCount++;
            nodeTouchedForward[nodeToHandleIdForward] = true;

            // Break early if target node reached
            if (nodeToHandleIdForward == destNodeIdx) {

                long stopTime = System.nanoTime();

                return new RoutingResult(true, getPathToNodeWithIDForward(nodeToHandleIdForward, startNodeIdx),currDistanceToNodeForward[destNodeIdx] + Edge.getDest(Grid.getEdgeIDByNodeIDs(nodeToHandleIdForward, destNodeIdx)), (double) (stopTime - startTime) / 1000000, vertexPoppedOutCount);
            }

            // Backward and forward search met --> terminate
            if (nodeTouchedBackward[nodeToHandleIdForward]) {
                break;
            }

            for (int neighbourEdgeId = Grid.offset[nodeToHandleIdForward]; neighbourEdgeId < Grid.offset[nodeToHandleIdForward + 1]; ++neighbourEdgeId) {

                int destinationVertexId = Edge.getDest(neighbourEdgeId);

                if (nodeTouchedForward[destinationVertexId]) {
                    continue;
                }

                // Calculate the distance to the destination vertex using the current edge
                int newDistanceOverThisEdgeToDestVertex = currDistanceToNodeForward[nodeToHandleIdForward] + Edge.getDist(neighbourEdgeId);

                // If the new calculated distance to the destination vertex is lower as the previously known, update the corresponding data structures
                if (newDistanceOverThisEdgeToDestVertex < currDistanceToNodeForward[destinationVertexId]) {
                    currDistanceToNodeForward[destinationVertexId] = newDistanceOverThisEdgeToDestVertex;
                    previousNodeForward[destinationVertexId] = nodeToHandleIdForward;
                    vertexHeapForward.add(destinationVertexId);
                }


                if (nodeTouchedBackward[destinationVertexId]) {
                    int newDistanceOverall = currDistanceToNodeForward[nodeToHandleIdForward] +  Edge.getDist(neighbourEdgeId) + currDistanceToNodeBackward[destinationVertexId];
                    if (currentSmallestDistnace > newDistanceOverall) {
                        currentSmallestDistnace = newDistanceOverall;
                        currentBestWayForwardEndId = nodeToHandleIdForward;
                        currentBestWayBackwardEndId = destinationVertexId;
                    }
                }

            }

            // =========================
            // ==== BACKWARD SEARCH ====
            // =========================

            int nodeToHandleIdBackward = vertexHeapBackward.getNext();
            vertexPoppedOutCount++;
            nodeTouchedBackward[nodeToHandleIdBackward] = true;

            if (nodeToHandleIdBackward == startNodeIdx) {

                long stopTime = System.nanoTime();

                return new RoutingResult(true, getPathToNodeWithIDBackward(nodeToHandleIdBackward, destNodeIdx),currDistanceToNodeBackward[startNodeIdx] + Edge.getDest(Grid.getEdgeIDByNodeIDs(nodeToHandleIdBackward, startNodeIdx)), (double) (stopTime - startTime) / 1000000, vertexPoppedOutCount);
            }

            // Backward and forward search met --> terminate
            if (nodeTouchedForward[nodeToHandleIdBackward]) {
                break;
            }

            for (int neighbourEdgeId = Grid.offset[nodeToHandleIdBackward]; neighbourEdgeId < Grid.offset[nodeToHandleIdBackward + 1]; ++neighbourEdgeId) {

                int destinationVertexId = Edge.getDest(neighbourEdgeId);

                if (nodeTouchedBackward[destinationVertexId]) {
                    continue;
                }

                // Calculate the distance to the destination vertex using the current edge
                int newDistanceOverThisEdgeToDestVertex = currDistanceToNodeBackward[nodeToHandleIdBackward] + Edge.getDist(neighbourEdgeId);

                // If the new calculated distance to the destination vertex is lower as the previously known, update the corresponding data structures
                if (newDistanceOverThisEdgeToDestVertex < currDistanceToNodeBackward[destinationVertexId]) {
                    currDistanceToNodeBackward[destinationVertexId] = newDistanceOverThisEdgeToDestVertex;
                    previousNodeBackward[destinationVertexId] = nodeToHandleIdBackward;
                    vertexHeapBackward.add(destinationVertexId);
                }

                if (nodeTouchedForward[destinationVertexId]) {
                    int newDistanceOverall = currDistanceToNodeBackward[nodeToHandleIdBackward] +  Edge.getDist(neighbourEdgeId) + currDistanceToNodeForward[destinationVertexId];
                    if (currentSmallestDistnace > newDistanceOverall) {
                        currentSmallestDistnace = newDistanceOverall;
                        currentBestWayBackwardEndId = nodeToHandleIdBackward;
                        currentBestWayForwardEndId = destinationVertexId;
                    }
                }

            }
        }

        // Here, we are done with dijkstra but need to gather all relevant data from the resulting data structures
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
        return "Bidirectional Dijkstra";
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
}