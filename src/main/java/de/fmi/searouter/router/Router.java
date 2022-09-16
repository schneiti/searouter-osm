package de.fmi.searouter.router;

import de.fmi.searouter.dijkstragrid.*;

/**
 * A router gets two nodes of a {@link Grid} as input and
 * returns the shortest path from the start node to the destination node as a list
 * of nodes.
 */
public interface Router {

    /**
     * Calculates the shortest path from one start node to a destination node. Node definitions
     * are in {@link Node}, edge definition in {@link Edge} and the relationships between those two
     * data structures in {@link Grid}.
     *
     * @param startNodeIdx The index of the start node (corresponding to {@link Node} indices)
     * @param destNodeIdx The index of the destination node (corresponding to {@link Node} indices)
     * @return a route between start and destination node
     */
    RoutingResult route(int startNodeIdx, int destNodeIdx);

    /**
     * Get the name of the router.
     * @return The name of the router.
     */
    String getName();
}
