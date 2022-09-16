package de.fmi.searouter.dijkstragrid;

import de.fmi.searouter.landmarks.Landmark;
import de.fmi.searouter.landmarks.LandmarkDistributionMode;
import de.fmi.searouter.utils.IntersectionHelper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Contains the offset data structure which connects the {@link Edge}s with the {@link Node}s to
 * a graph representation. Provides in addition methods for grid operations.
 */
public class Grid {
    // Stores for each node id the position in the Edge array where the edges for the respective nodes start.
    // To get all outgoing edge IDs of a node one can call Grid.offset[nodeToHandleId] and then iterate over
    // the {@link Edge} array until the index Grid.offset[nodeToHandleId + 1] is reached (this is the beginning
    // of another nodes outgoing edges).
    public static int[] offset;

    /**
     * Whether to initialize Landmarks for ALT algorithms or not
     */
    private static final boolean USE_LANDMARKS = true;

    /**
     * Which {@link LandmarkDistributionMode} should be used for the Landmark selection process.
     */
    private static final LandmarkDistributionMode LANDMARK_DISTRIBUTION_MODE = LandmarkDistributionMode.EQUAL_SPHERE;


    public static int getEdgeIDByNodeIDs(int start, int dest) {
        for (int neighbourEdgeId = Grid.offset[start]; neighbourEdgeId < Grid.offset[start + 1]; ++neighbourEdgeId) {
            if (Edge.getDest(neighbourEdgeId) == dest) {
                return neighbourEdgeId;
            }
        }
        return -1;
    }

    /**
     * Returns the nearest existing grid node of the Grid of a given point P.
     *
     * @param latitude    Latitude of point P (0 to 90°)
     * @param longitude   Longitude of point P  (-180 to 180°)
     * @param routingMode True if only grid nodes should be considered that are within the same lat/lon degree. False if all nodes should be considered
     * @return The index within the {@link Node} data structure that points to the nearest grid node. -1 if no node exists
     * in the requested plane of integer degrees.
     */
    public static int getNearestGridNodeByCoordinates(double latitude, double longitude, boolean routingMode) {
        // Strategy: Get all grid nodes on the plane of integer grid numbers and then check manually the distance

        // Integer coordinate degrees to search for
        int iLat = (int) latitude;
        int iLong = (int) longitude;

        List<Integer> candidateNodes = new ArrayList<>();

        // Add all node indices to the candidate node list that are within the integer degree plane
        for (int i = 0; i < Node.getSize(); i++) {
            if (routingMode) {
                if (iLat == (int) Node.getLatitude(i) && iLong == (int) Node.getLongitude(i)) {
                    candidateNodes.add(i);
                }
            } else {
                candidateNodes.add(i);
            }
        }

        if (candidateNodes.size() <= 0) {
            return -1;
        }

        // For all candidates, calculate the distances to the requested coordinates
        double minDistance = Double.MAX_VALUE;
        int minNodeIdx = 0;
        for (Integer nodeIdx : candidateNodes) {
            double currDistance = IntersectionHelper.getDistance(
                    latitude, longitude,
                    Node.getLatitude(nodeIdx), Node.getLongitude(nodeIdx)
            );
            if (currDistance < minDistance) {
                minDistance = currDistance;
                minNodeIdx = nodeIdx;
            }
        }

        return minNodeIdx;
    }


    /**
     * Only temporary used during the import of fmi files needed (for sorting).
     */
    private static class TmpEdge {
        public int startNode;
        public int destNode;
        public int dist;

        public TmpEdge(int startNode, int destNode, int dist) {
            this.startNode = startNode;
            this.destNode = destNode;
            this.dist = dist;
        }
    }

    /**
     * Only temporary during the import of fmi files needed (for sorting).
     */
    private static class TmpNode {
        public int id;
        public double latitude;
        public double longitude;

        public TmpNode(int id, double latitude, double longitude) {
            this.id = id;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    /**
     * Imports a grid graph of a .fmi file format.
     *
     * @param filePath The path within the resources folder where the file to import is placed.
     * @throws IOException If I/O fails.
     */
    public static void importFmiFile(String filePath) throws IOException {
        Resource fmiResource = new ClassPathResource(filePath);
        InputStream inputStream = fmiResource.getInputStream();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {

            br.readLine();
            br.readLine();

            // Map file id to internal used node id
            Map<Integer, Integer> fileNodeIdToInternalUsedId = new HashMap<>();

            int noNodes = Integer.parseInt(br.readLine().trim());
            int noEdges = Integer.parseInt(br.readLine().trim());

            List<TmpNode> nodeList = new ArrayList<>();

            // Node handling
            for (int nodeIdx = 0; nodeIdx < noNodes; nodeIdx++) {
                String line = br.readLine();
                line = line.trim();

                String[] split = line.split(" ");

                TmpNode node = new TmpNode(Integer.parseInt(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
                nodeList.add(node);

                fileNodeIdToInternalUsedId.put(node.id, nodeIdx);
            }

            // Sort node list by id
            nodeList.sort(new Comparator<TmpNode>() {
                @Override
                public int compare(TmpNode o1, TmpNode o2) {
                    return Integer.compare(fileNodeIdToInternalUsedId.get(o1.id), fileNodeIdToInternalUsedId.get(o2.id));
                }
            });

            // Edge handling
            List<TmpEdge> edgeList = new ArrayList<>();

            for (int edgeIdx = 0; edgeIdx < noEdges; edgeIdx++) {
                String line = br.readLine();
                line = line.trim();

                String[] split = line.split(" ");
                TmpEdge edge = new TmpEdge(
                        fileNodeIdToInternalUsedId.get(Integer.parseInt(split[0])),
                        fileNodeIdToInternalUsedId.get(Integer.parseInt(split[1])),
                        Integer.parseInt(split[2])
                );
                edgeList.add(edge);
            }

            // Sort edges by start ids
            edgeList.sort(new Comparator<TmpEdge>() {
                @Override
                public int compare(TmpEdge o1, TmpEdge o2) {
                    return Integer.compare(o1.startNode, o2.startNode);
                }
            });

            // Build an adjacency map for the following operations
            Map<Integer, List<TmpEdge>> adjacenceMap = new HashMap<>();
            for (int edgeIdx = 0; edgeIdx < noEdges; edgeIdx++) {
                int currStartNodeID = edgeList.get(edgeIdx).startNode;
                if (!adjacenceMap.containsKey(currStartNodeID)) {
                    adjacenceMap.put(currStartNodeID, new ArrayList<>());
                }

                adjacenceMap.get(currStartNodeID).add(edgeList.get(edgeIdx));
            }

            // Assure that the graph is undirected by adding missing unidirectional edges
            List<TmpEdge> additionalEdgesThatWereMissing = new ArrayList<>();
            for (Map.Entry<Integer, List<TmpEdge>> e : adjacenceMap.entrySet()) {

                List<TmpEdge> reverseEdgeStartNodesToCheck = e.getValue();
                boolean oppositeEdgeFound = false;

                for (TmpEdge revEdge : reverseEdgeStartNodesToCheck) {
                    List<TmpEdge> toCheck = adjacenceMap.get(revEdge.destNode);
                    for (TmpEdge edges : toCheck) {
                        if (edges.startNode == revEdge.destNode && edges.destNode == revEdge.startNode) {
                            oppositeEdgeFound = true;
                            break;
                        }
                    }
                    if (!oppositeEdgeFound) {
                        additionalEdgesThatWereMissing.add(new TmpEdge(revEdge.destNode, revEdge.startNode, revEdge.dist));
                        noEdges++;
                        System.out.println("Added edge " + revEdge.destNode + " | " + revEdge.startNode);
                    }
                }

            }
            edgeList.addAll(additionalEdgesThatWereMissing);

            // Sort edges by start ids
            edgeList.sort(new Comparator<TmpEdge>() {
                @Override
                public int compare(TmpEdge o1, TmpEdge o2) {
                    return Integer.compare(o1.startNode, o2.startNode);
                }
            });

            // Fill arrays
            double[] latitude = new double[noNodes];
            double[] longitude = new double[noNodes];

            for (int i = 0; i < latitude.length; i++) {
                latitude[i] = nodeList.get(i).latitude;
                longitude[i] = nodeList.get(i).longitude;
            }

            Node.setLatitude(latitude);
            Node.setLongitude(longitude);


            int[] startNode = new int[noEdges];
            int[] destNode = new int[noEdges];
            int[] dist = new int[noEdges];

            for (int i = 0; i < startNode.length; i++) {
                startNode[i] = edgeList.get(i).startNode;
                destNode[i] = edgeList.get(i).destNode;
                dist[i] = edgeList.get(i).dist;
            }

            Edge.setStartNode(startNode);
            Edge.setDestNode(destNode);
            Edge.setDist(dist);

            //
            offset = new int[Node.getSize() + 1];
            for (int i = 0; i < Edge.getSize(); ++i) {
                offset[Edge.getStart(i) + 1] += 1;
            }
            for (int i = 1; i < offset.length; ++i) {
                offset[i] += offset[i - 1];
            }

            br.close();

            // Initialize landmarks for ALT algorithms
            if (USE_LANDMARKS) {
                try {
                    Landmark.initLandmarks(LANDMARK_DISTRIBUTION_MODE);
                } catch (NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * Exports the current grid graph representation (contents of {@link Edge} and {@link Node}).
     *
     * @param filePath The export path (relative to the main directory of this project).
     * @throws IOException If I/O fails.
     */
    public static void exportToFmiFile(String filePath) throws IOException {
        // Get an input stream for the pbf file located in the resources directory

        File f = new File(filePath);
        f.createNewFile();

        BufferedWriter writer = new BufferedWriter(new FileWriter(f));

        writer.write("#\n\n");

        // Number of nodes
        writer.append(String.valueOf(Node.getSize())).append("\n");
        // Number of edges
        writer.append(String.valueOf(Edge.getSize())).append("\n");

        // Write all nodes
        for (int nodeIdx = 0; nodeIdx < Node.getSize(); nodeIdx++) {
            writer.append(String.valueOf(nodeIdx)).append(" ").append(String.valueOf(Node.getLatitude(nodeIdx))).append(" ").append(String.valueOf(Node.getLongitude(nodeIdx))).append("\n");
        }

        // Write all edges
        for (int edgeIdx = 0; edgeIdx < Edge.getSize(); edgeIdx++) {
            writer.append(String.valueOf(Edge.getStart(edgeIdx))).append(" ").append(String.valueOf(Edge.getDest(edgeIdx))).append(" ").append(String.valueOf(Edge.getDist(edgeIdx))).append("\n");
        }

        writer.close();
    }

}
