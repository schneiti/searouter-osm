package de.fmi.searouter.dijkstragrid;

/**
 * Stores all edges of the grid graph. All data of one edge is accessible
 * using the same index.
 */
public class Edge {

    // Start node id of this edge (id corresponds to position in static Node class)
    private static int[] startNode;

    // Dest node id of this edge (id corresponds to position in static Node class)
    private static int[] destNode;

    // Length of this edge (length between startNode and destNode)
    private static int[] dist;

    public static int getSize() {
        return startNode.length;
    }

    public static int getStart(int i) {
        return startNode[i];
    }

    public static int getDest(int i) {
        return destNode[i];
    }

    public static int getDist(int i) {
        return dist[i];
    }

    public static void setStartNode(int[] startNode) {
        Edge.startNode = startNode;
    }

    public static void setDestNode(int[] destNode) {
        Edge.destNode = destNode;
    }

    public static void setDist(int[] dist) {
        Edge.dist = dist;
    }
}

