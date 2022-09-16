package de.fmi.searouter.coastlinegrid;

import de.fmi.searouter.utils.IntersectionHelper;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Singleton class that provides information whether a given coordinate point is on land or on water.
 * <p>
 * For this, a multi-level grid system is used where the earth is recursively divided in {@link GridCell Gridcells}
 * each of which containing a maximum of 1000 coastline edges.
 * For the decision whether a given point is on land or on water a ray casting algorithm is used in combination
 * with a pre-computed center point of each GridCell for which the water/land status is already pre-computed.
 */
public final class PointInWaterChecker implements Serializable{
    //information on the initial point given to calculate the grid used to determine if a point is on land or in water.
    private static final double INITIAL_POINT_LAT = -83.0;
    private static final double INITIAL_POINT_LON = -170.0;
    private static final boolean INITIAL_POINT_IN_WATER = false;

    /**
     * The instance of the singleton
     */
    private static PointInWaterChecker INSTANCE;

    /**
     * top level of the grid used to determine if a point is on land or in water.
     */
    private final GridCell[][] topLevelGrid;

    /**
     * Whether {@link #initPointInWaterChecker()} was already called.
     */
    private static boolean initCalled = false;

    /**
     * The name of the serialization file which is created for storing the PointInWaterCheckers information.
     */
    private static final String POINT_IN_WATER_CHECKER_SERIALIZATION_FILE_NAME = "coastlineChecker.ser";

    /**
     * Initializes this {@link PointInWaterChecker} singleton.
     */
    public static void initPointInWaterChecker() {
        // PointInWaterChecker file
        File serializationFile = new File(POINT_IN_WATER_CHECKER_SERIALIZATION_FILE_NAME);

        if (!serializationFile.exists()) {
            PointInWaterChecker pointInWaterChecker = new PointInWaterChecker();

            // Create PointInWaterChecker instance and serialize it for later use.
            try {
                FileOutputStream file = new FileOutputStream(POINT_IN_WATER_CHECKER_SERIALIZATION_FILE_NAME);
                ObjectOutputStream out = new ObjectOutputStream(file);

                out.writeObject(pointInWaterChecker);

                out.close();
                file.close();

                System.out.println("PointInWaterChecker has been serialized");
                INSTANCE = pointInWaterChecker;

            } catch (IOException ex) {
                ex.printStackTrace();
                System.exit(0);
            }
        } else {
            // Deserialization
            try {
                // Reading the object from a file
                FileInputStream file = new FileInputStream(POINT_IN_WATER_CHECKER_SERIALIZATION_FILE_NAME);
                ObjectInputStream in = new ObjectInputStream(file);

                // Method for deserialization of object
                PointInWaterChecker pointInWaterChecker = (PointInWaterChecker) in.readObject();

                in.close();
                file.close();

                System.out.println("PointInWaterChecker has been deserialized");
                INSTANCE = pointInWaterChecker;
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
                System.exit(-1);
            }
        }
        initCalled = true;
    }

    /**
     * @return The instance of this singleton.
     */
    public static PointInWaterChecker getInstance() {
        if (!initCalled) {
            initPointInWaterChecker();
        }

        return INSTANCE;
    }

    /**
     * Private constructor - this is a singleton.
     * <p>
     * Initializes the multi-level grid which is a data structure needed to decide efficiently
     * whether a given coordinate point is on water or on land.
     */
    private PointInWaterChecker() {
        topLevelGrid = new GridCell[18][36];

        // Init the top level (parent) cells.
        for (int latIdx = 0; latIdx < 18; latIdx++) {
            double lowerLatBound = 10.0 * (latIdx - 9);
            double upperLatBound = lowerLatBound + 10.0;
            for (int lonIdx = 0; lonIdx < 36; lonIdx++) { //full size: 36
                double leftLonBound = 10.0 * (lonIdx - 18);
                double rightLonBound = leftLonBound + 10.0;

                // Find out which edges belong in the top-level cell currently looked at
                Set<Integer> edgesInCell = new HashSet<>();
                int numOfEdges = CoastlineWays.getNumberOfEdges();

                for (int edgeId = 0; edgeId < numOfEdges; edgeId++) {
                    /*
                     * Check whether the edge is trivially (not) contained in the cell and skip it in this cases
                     */
                    boolean[] startResults = IntersectionHelper.getPositionInfoOfPointRelativeToCell(
                            CoastlineWays.getStartLatByEdgeIdx(edgeId),
                            CoastlineWays.getStartLonByEdgeIdx(edgeId),
                            leftLonBound, rightLonBound,
                            lowerLatBound, upperLatBound);
                    if (!(startResults[0] || startResults[1] || startResults[2] || startResults[3])) {
                        // edge is inside cell
                        edgesInCell.add(edgeId);
                        continue;
                    }
                    boolean[] destResults = IntersectionHelper.getPositionInfoOfPointRelativeToCell(
                            CoastlineWays.getDestLatByEdgeIdx(edgeId),
                            CoastlineWays.getDestLonByEdgeIdx(edgeId),
                            leftLonBound, rightLonBound,
                            lowerLatBound, upperLatBound);
                    if (!(destResults[0] || destResults[1] || destResults[2] || destResults[3])) {
                        // edge is inside cell
                        edgesInCell.add(edgeId);
                        continue;
                    }
                    //check if intersection test is necessary or edge is trivially not in cell
                    if ((startResults[0] && destResults[0]) || (startResults[1] && destResults[1]) ||
                            (startResults[2] && destResults[2]) || (startResults[3] && destResults[3])) {
                        //edge is trivially not contained
                        continue;
                    }

                    /*
                     * The "trivial" checks are not enough -> we need to perform real intersection checks
                     * (intersect edge with the cell boundaries) to find out whether the edge is contained
                     * in the cell or not
                     */
                    float startLat = CoastlineWays.getStartLatByEdgeIdx(edgeId);
                    float startLon = CoastlineWays.getStartLonByEdgeIdx(edgeId);
                    float destLat = CoastlineWays.getDestLatByEdgeIdx(edgeId);
                    float destLon = CoastlineWays.getDestLonByEdgeIdx(edgeId);
                    boolean intersectsBorder = false;
                    if (IntersectionHelper.arcsIntersect( // Left vertical edge of cell, e.g.
                            startLat, startLon, destLat, destLon,
                            lowerLatBound, leftLonBound,
                            upperLatBound, leftLonBound)) {
                        intersectsBorder = true;
                    } else if (IntersectionHelper.arcsIntersect( // Right vertical edge of cell, e.g. 0,1 - 1,1
                            startLat, startLon, destLat, destLon,
                            lowerLatBound, rightLonBound,
                            upperLatBound, rightLonBound)) {
                        intersectsBorder = true;
                    } else if (IntersectionHelper.crossesLatitude( // Lower latitude coordinate of cell
                            startLat, startLon, destLat, destLon,
                            lowerLatBound,
                            leftLonBound, rightLonBound)) {
                        intersectsBorder = true;
                    } else if (IntersectionHelper.crossesLatitude( // Upper latitude coordinate of cell
                            startLat, startLon, destLat, destLon,
                            upperLatBound,
                            leftLonBound, rightLonBound)) {
                        intersectsBorder = true;
                    }

                    if (intersectsBorder) {
                        edgesInCell.add(edgeId);
                    }
                }

                // Calculate the lower level cells of the grid cells in the respective GridParent constructor
                topLevelGrid[latIdx][lonIdx] = new GridParent(edgesInCell, lowerLatBound, upperLatBound,
                        leftLonBound, rightLonBound);

            }
        }

        /*
         * Constructing grids is finished, now initialize the center points
         */

        // Pre-calculate center point coordinates of top-level cells for faster reuse
        double[] centerPointLat = new double[18];
        double[] centerPointLon = new double[36];
        double lat = -85.0;
        for (int i = 0; i < 18; i++) {
            centerPointLat[i] = lat;
            lat += 10.0;
        }
        double lon = -175.0;
        for (int i = 0; i < 36; i++) {
            centerPointLon[i] = lon;
            lon += 10.0;
        }

        //first, check if the middle point of the first cell is in water or on land
        boolean firstPointInWater = INITIAL_POINT_IN_WATER;
        int numOfEdges = CoastlineWays.getNumberOfEdges();
        for (int edgeId = 0; edgeId < numOfEdges; edgeId++) {
            if (IntersectionHelper.arcsIntersect(
                    CoastlineWays.getStartLatByEdgeIdx(edgeId),
                    CoastlineWays.getStartLonByEdgeIdx(edgeId),
                    CoastlineWays.getDestLatByEdgeIdx(edgeId),
                    CoastlineWays.getDestLonByEdgeIdx(edgeId),
                    INITIAL_POINT_LAT, INITIAL_POINT_LON,
                    centerPointLat[0], centerPointLon[0]
            )) {
                firstPointInWater = !firstPointInWater;
            }
        }

        // Just this once, we use set instead of init (since point in water is known for the first middle point)
        topLevelGrid[0][0].setCenterPoint(centerPointLat[0], centerPointLon[0], firstPointInWater);

        // store whether center point of the top level cell is in water and all edge IDs for bottom row of GridParents
        boolean[] firstCenterPointInWater = new boolean[36];
        Set<Integer>[] firstAdditionalEdges = new Set[36];

        firstCenterPointInWater[0] = firstPointInWater;
        firstAdditionalEdges[0] = topLevelGrid[0][0].getAllContainedEdgeIDs();

        // first, calculate bottom row
        double firstRowLat = centerPointLat[0];
        for (int lonIdx = 1; lonIdx < 36; lonIdx++) { //start at index 1, as first element is alredy known
            firstCenterPointInWater[lonIdx] = topLevelGrid[0][lonIdx].initCenterPoint(firstRowLat,
                    centerPointLon[lonIdx - 1], firstCenterPointInWater[lonIdx - 1],
                    firstAdditionalEdges[lonIdx - 1], GridCell.ApproachDirection.FROM_HORIZONTAL);
            firstAdditionalEdges[lonIdx] = topLevelGrid[0][lonIdx].getAllContainedEdgeIDs();
        }

        //now, calculate by column
        for (int lonIdx = 0; lonIdx < 36; lonIdx++) {
            boolean previousPointInWater = firstCenterPointInWater[lonIdx];
            Set<Integer> previousEdges = firstAdditionalEdges[lonIdx];

            for (int latIdx = 1; latIdx < 18; latIdx++) { //first row already calculated, so start at idx 1
                previousPointInWater = topLevelGrid[latIdx][lonIdx].initCenterPoint(centerPointLat[latIdx - 1],
                        centerPointLon[lonIdx], previousPointInWater,
                        previousEdges, GridCell.ApproachDirection.FROM_VERTICAL);

                previousEdges = topLevelGrid[latIdx][lonIdx].getAllContainedEdgeIDs();
            }
        }
    }

    /**
     * Check whether a given coordinate point is in water or not.
     *
     * @param lat The latitude coordinate of the point
     * @param lon The longitude coordinate of the point
     * @return True if the point is on water, false if not.
     */
    public boolean pointInWater(float lat, float lon) {
        int latIdx;
        int lonIdx;

        // Calculate the grid cell index for the cell array depending on lat
        if (lat == 90) {
            latIdx = 17;
        } else if (lat == -90) {
            latIdx = 0;
        } else if (lat > 0) {
            latIdx = (int) (((lat) - (lat % 10)) / 10) + 9;
        } else {
            float tmpLat = lat * (-1); // * -1 to make mod calculation work
            latIdx = (int) (8 - (((tmpLat) - (tmpLat % 10)) / 10));
        }

        // Calculate the grid cell index for the cell array depending on lon
        if (lon == 180) {
            lonIdx = 35;
        } else if (lon == -180) {
            lonIdx = 0;
        } else if (lon > 0) {
            lonIdx = (int) (((lon) - (lon % 10)) / 10) + 18;
        } else {
            float tmpLon = lon * (-1); // * -1 to make mod calculation work
            lonIdx = (int) (17 - (((tmpLon) - (tmpLon % 10)) / 10));
        }

        //recursive call until GridLeaf is reached
        return topLevelGrid[latIdx][lonIdx].isPointInWater(lat, lon);
    }

}
