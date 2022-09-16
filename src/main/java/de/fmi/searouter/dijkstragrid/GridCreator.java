package de.fmi.searouter.dijkstragrid;

import de.fmi.searouter.coastlinegrid.PointInWaterChecker;
import de.fmi.searouter.coastlinegrid.CoastlineWays;
import de.fmi.searouter.utils.IntersectionHelper;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains logic for creating a grid graph whose nodes are distributed equally over the latitudes
 * and longitudes of a world map. Note: This distance is not equidistant, as longitude and latitude are simply divided.
 */
public class GridCreator {

    /**
     * Resolution of the grid (DIM_LAT * DIM_LON = number of vertices in the graph if each vertex would be on water)
     */
    private static final int DIMENSION_LATITUDE = 500;
    private static final int DIMENSION_LONGITUDE = 2000;

    /**
     * The name of the file where the final calculated grid graph should be exported (created on root-level of project)
     */
    public static final String GRID_FMI_FILE_NAME = "exported_grid.fmi";

    /**
     * Stores all calculated graph nodes that are situated on water.
     * Protected in order to be accessible from {@link NodeCreateWorkerThread}.
     */
    protected static List<GridNode> gridNodes;

    /**
     * Used for retrieving already created GridNode references by latitude and longitude.
     * <Latitude, <Longitude, GridNode>>
     */
    public static ConcurrentHashMap<Double, Map<Double, GridNode>> coordinateNodeStore;

    /**
     * Difference between latitude/longitude coordinates between two neighbor grid nodes
     */
    protected static double coordinate_step_latitude;
    protected static double coordinate_step_longitude;

    /**
     * The number of threads that should use to concurrently performing the point-in-water check
     */
    private static final int NUMBER_OF_THREADS_FOR_IN_WATER_CHECK = 15;

    /**
     * Initializes the GridCreator by creating GridNodes (the vertices of the graph) for all points that
     * are in water and therefore relevant for the graph creation. The created nodes
     * are stored in the data structures {@link #gridNodes} and {@link #coordinateNodeStore}.
     *
     * @throws InterruptedException If something with the concurrent calculation of the point-in-water check fails.
     */
    private static void initGridCreator() throws InterruptedException {
        gridNodes = Collections.synchronizedList(new ArrayList<>());

        coordinateNodeStore = new ConcurrentHashMap<>();

        // Calculate the numeric distance of the grid nodes in degrees
        coordinate_step_latitude = (double) 180 / DIMENSION_LATITUDE;
        coordinate_step_longitude = (double) 360 / DIMENSION_LONGITUDE;

        // We use BigDecimals here to allow the usage of the coordinateNodeStore HashMap where it is necessary
        // to have an exact equals() functionality for comparing the used keys. Tests showed that the overhead
        // of using BigDecimal instead of double has no relevant effect on the calculation time here.
        BigDecimal coordinateStepLat = BigDecimal.valueOf(coordinate_step_latitude);

        BigDecimal latEnd = BigDecimal.valueOf(-90);

        // Precalculate all latitudes that should be checked and assign those latitudes equally distributed to
        // a number of worker threads. The threads will then calculate the point-in-water check on their
        // assigned points using a multi-level grid strategy.
        int numberOfThreads = NUMBER_OF_THREADS_FOR_IN_WATER_CHECK;
        List<NodeCreateWorkerThread> threads = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            threads.add(new NodeCreateWorkerThread());
        }
        int count = 0;
        for (BigDecimal lat = BigDecimal.valueOf(90.0); lat.compareTo(latEnd) >= 0; lat = lat.subtract(coordinateStepLat)) {
            int threadToAssign = count % numberOfThreads;
            threads.get(threadToAssign).addLatitude(lat);
            count++;
        }

        for (NodeCreateWorkerThread n : threads) {
            n.start();
        }

        for (NodeCreateWorkerThread n : threads) {
            n.join();
        }
    }


    /**
     * Creates the grid graph for the Dijkstra routing. Fills the {@link Grid}, {@link Node} and {@link Edge}
     * data structures.
     *
     * @throws InterruptedException If something with the threads went wrong
     */
    public static void createGrid() throws InterruptedException {
        initGridCreator();

        // Create Node arrays
        double[] latitude = new double[gridNodes.size()];
        double[] longitude = new double[gridNodes.size()];

        // Assign new ids to nodes and fill up Node data structure
        for (int id = 0; id < gridNodes.size(); id++) {
            gridNodes.get(id).setId(id);
            latitude[id] = gridNodes.get(id).getLatitude();
            longitude[id] = gridNodes.get(id).getLongitude();
        }

        // Dynamic Edge info
        List<Integer> dynamicStartNode = new ArrayList<>();
        List<Integer> dynamicDestNode = new ArrayList<>();
        List<Integer> dynamicDist = new ArrayList<>();

        // For every node, check if neighbour nodes are existing and if yes add it as edge
        for (int nodeIdx = 0; nodeIdx < gridNodes.size(); nodeIdx++) {
            GridNode currNode = gridNodes.get(nodeIdx);

            // Calculate the lat/longs where a neighbour node should be. The created objects do not belong to the grid
            // and are only needed temporarily for checking if those candidate neighbor nodes are actually on water
            // and therefore relevant
            GridNode eastCalcNode = currNode.calcEasternNode(coordinate_step_longitude);
            GridNode westCalcNode = currNode.calcWesternNode(coordinate_step_longitude);
            GridNode northCalcNode = currNode.calcNorthernNode(coordinate_step_latitude);
            GridNode southCalcNode = currNode.calcSouthernNode(coordinate_step_latitude);

            // Init the real GridNode objects which are known by the current grid
            GridNode east = null;
            GridNode west = null;
            GridNode north = null;
            GridNode south = null;

            // Check if the calculated lat/longs of neighbor nodes are actually real existing water nodes in the grid
            if (eastCalcNode != null) {
                east = getNodeByLatLong(eastCalcNode.getLatitude(), eastCalcNode.getLongitude());
            }
            if (westCalcNode != null) {
                west = getNodeByLatLong(westCalcNode.getLatitude(), westCalcNode.getLongitude());
            }
            if (northCalcNode != null) {
                north = getNodeByLatLong(northCalcNode.getLatitude(), northCalcNode.getLongitude());
            }
            if (southCalcNode != null) {
                south = getNodeByLatLong(southCalcNode.getLatitude(), southCalcNode.getLongitude());
            }

            List<GridNode> neighbourNodes = Arrays.asList(east, west, north, south);

            // For all existing neighbour nodes: Add the information to the dynamic edge list
            for (GridNode node : neighbourNodes) {
                if (node != null) {
                    // Neighbor node is on water and therefore a valid vertice of the grpah to which
                    // an edge should be created
                    dynamicStartNode.add(nodeIdx);
                    dynamicDestNode.add(node.getId());
                    dynamicDist.add((int) IntersectionHelper.getDistance(
                            currNode.getLatitude(), currNode.getLongitude(),
                            node.getLatitude(), node.getLongitude())
                    );
                }
            }
        }

        // Convert dynamic Edge data structures to static arrays of the more efficient routing data structures
        int[] startNode = new int[dynamicStartNode.size()];
        int[] destNode = new int[dynamicDestNode.size()];
        int[] dist = new int[dynamicDist.size()];
        for (int i = 0; i < startNode.length; i++) {
            startNode[i] = dynamicStartNode.get(i);
            destNode[i] = dynamicDestNode.get(i);
            dist[i] = dynamicDist.get(i);
        }

        // Fill the Node and Edge classes. These classes together represent our calculated graph.
        Node.setLatitude(latitude);
        Node.setLongitude(longitude);
        Edge.setStartNode(startNode);
        Edge.setDestNode(destNode);
        Edge.setDist(dist);

        // Export the whole graph
        exportGridAsFMIFile();
    }

    /**
     * Exports the pre-processed graph to a text file with the ending .fmi. The file will
     * be created on the top level of this project with the name {@link #GRID_FMI_FILE_NAME}.
     */
    private static void exportGridAsFMIFile() {
        try {
            Grid.exportToFmiFile(GRID_FMI_FILE_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Finds a {@link GridNode} that is known to be on water by its coordinates.
     *
     * @param latitude  The latitude coordinate of the GridNode to search for
     * @param longitude The longitude coordinate of the GridNode to search for.
     * @return The searched {@link GridNode} object or null if such a node is not found
     */
    private static GridNode getNodeByLatLong(double latitude, double longitude) {
        if (coordinateNodeStore.containsKey(latitude) && coordinateNodeStore.get(latitude).containsKey(longitude)) {
            return coordinateNodeStore.get(latitude).get(longitude);
        }
        return null;
    }

    /**
     * Entry point for the pre-processing part of this project.
     *
     * @param args Not used
     */
    public static void main(String[] args) {
        // Start time of the whole pre-processing for tracking time statistics
        Date startTime = new Date();

        // Import the OSM coastline information
        CoastlineWays.initCoastlineWays();

        // Initialize the multi-level grid of the CoastlineChecker
        PointInWaterChecker.initPointInWaterChecker();

        // Create dijkstra grid for later use
        try {
            GridCreator.createGrid();
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // Calculate the needed time for the pre-processing for time statistics in minutes
        Date endTime = new Date();
        long timeDiffMin = ((endTime.getTime() - startTime.getTime()) / 1000) / 60;
        long timeDiffSec = ((endTime.getTime() - startTime.getTime()) / 1000) % 60;
        System.out.println("Preprocessing finished. Runtime: " + timeDiffMin + ":" + timeDiffSec);
    }
}
