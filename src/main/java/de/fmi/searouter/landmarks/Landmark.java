package de.fmi.searouter.landmarks;

import de.fmi.searouter.dijkstragrid.Grid;
import de.fmi.searouter.dijkstragrid.GridNode;
import de.fmi.searouter.dijkstragrid.Node;
import de.fmi.searouter.router.dijkstra.DijkstraRouter;
import de.fmi.searouter.utils.GeoJsonConverter;
import org.json.JSONObject;

import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * Stores information about all Landmarks that are currently in use for the ALT algorithms. This includes
 * pre-calculated distances of landmarks to all vertices.
 */
public class Landmark {

    /**
     * Stores for each landmark all distances to all other vertices in the graph.
     */
    public static int[][] distanceOfLandmarkToEveryVertex;

    /**
     * Stores for each landmark the vertex ID as defined in {@link Node}
     */
    public static int[] landmarkNodeIDs;

    /**
     * How often a certain landmark is used during a query. Only used for optional statistics.
     */
    public static int[] numberOfUsagesOfLandmark;

    /**
     * Stores the indices of {@link Landmark#landmarkNodeIDs} entries that are landmarks that
     * should be used for a certain routing query.
     */
    public static int[] landmarksToUseForQuery;

    public static double[] landmarkLowerBoundOnSTDistance;

    private static LandmarkBoundMaxHeap maxHeap;

    /**
     * Comparable landmark representation that enables a sorting of landmarks by its
     * calculated lower bound for a given start-destination node pair.
     */
/*    private static class LandmarkIDLowerBoundMapping implements Comparable<LandmarkIDLowerBoundMapping> {
        public int landmarkID;
        public double lowerBound;

        public LandmarkIDLowerBoundMapping(int landmarkID, double lowerBound) {
            this.landmarkID = landmarkID;
            this.lowerBound = lowerBound;
        }

        @Override
        public int compareTo(LandmarkIDLowerBoundMapping o) {
            return Double.compare(lowerBound, o.lowerBound);
        }
    }
*/

    /**
     * Prepares the landmarks for the query phase by selecting a subset of all available landmarks
     * by choosing the ones that perform best on the start-destination vertex pair of the routing query.
     *
     * @param maxNumberOfLandmarksToConsider The number of landmarks that should be flagged as active for the query phase.
     * @param startNode The ID of a start vertex
     * @param destinationNode The ID of a destination vertex
     */
    public static void prepareLandmarksForQueryPhase(int maxNumberOfLandmarksToConsider, int startNode, int destinationNode) {

        // This is a variant without max heap which tends to perform a little worse
        /*
        landmarksToUseForQuery = new int[maxNumberOfLandmarksToConsider];

        List<LandmarkIDLowerBoundMapping> allLowerBounds = new ArrayList<>();

        // 1. Get the landmarks that perform best on the s-d distance (maximum)
        for (int currLandmarkMarkIdx = 0; currLandmarkMarkIdx < landmarkNodeIDs.length; currLandmarkMarkIdx++) {
            allLowerBounds.add(new LandmarkIDLowerBoundMapping(
                    currLandmarkMarkIdx,
                    Math.abs(distanceOfLandmarkToEveryVertex[currLandmarkMarkIdx][startNode] - distanceOfLandmarkToEveryVertex[currLandmarkMarkIdx][destinationNode])
            ));
        }

        Collections.sort(allLowerBounds);
        for (int i = 0; i < landmarksToUseForQuery.length; i++) {
            landmarksToUseForQuery[i] = allLowerBounds.get(allLowerBounds.size()-1-i).landmarkID;
        }
        */

        // Variant using max heap
        landmarksToUseForQuery = new int[maxNumberOfLandmarksToConsider];

        maxHeap.resetState();

        // 1. Get the landmarks that perform best on the s-d distance (maximum)
        for (int currLandmarkMarkIdx = 0; currLandmarkMarkIdx < landmarkNodeIDs.length; currLandmarkMarkIdx++) {
            landmarkLowerBoundOnSTDistance[currLandmarkMarkIdx] = Math.abs(distanceOfLandmarkToEveryVertex[currLandmarkMarkIdx][startNode] - distanceOfLandmarkToEveryVertex[currLandmarkMarkIdx][destinationNode]);
            maxHeap.add(currLandmarkMarkIdx);
        }

        for (int i = 0; i < landmarksToUseForQuery.length; i++) {
            landmarksToUseForQuery[i] = maxHeap.getNext();
        }
    }

    /**
     * Calculates a heuristic for the distance from a start node to a destination node. The heuristic
     * is hereby based on the triangle equation using an additional landmark as third point. This additional
     * landmark is chosen depending on which landmark gives the longest distance estimation.
     *
     * @param firstNodeIdx  The start node id.
     * @param secondNodeIdx The target node id.
     * @return An estimated distance of the two nodes.
     */
    public static double distance(int firstNodeIdx, int secondNodeIdx) {
        int currMax = Integer.MIN_VALUE;
        //int usedLandmarkIdx = -1;

        for (int i = 0; i < landmarksToUseForQuery.length; i++) {
            int currVal = Math.abs(distanceOfLandmarkToEveryVertex[landmarksToUseForQuery[i]][firstNodeIdx] - distanceOfLandmarkToEveryVertex[landmarksToUseForQuery[i]][secondNodeIdx]);
            if (currMax < currVal) {
               // usedLandmarkIdx = i;
                currMax = currVal;
            }
        }
        //numberOfUsagesOfLandmark[usedLandmarkIdx]++;

        return currMax;
    }

    /**
     * Stores the landmarks to a serialization file.
     */
    private static void storeData(String fileName) {
        LandmarkSerializer writer = new LandmarkSerializer(distanceOfLandmarkToEveryVertex, landmarkNodeIDs);

        // Serialization
        try {
            //Saving of object in a file
            FileOutputStream file = new FileOutputStream(fileName);
            ObjectOutputStream out = new ObjectOutputStream(file);

            // Method for serialization of object
            out.writeObject(writer);

            out.close();
            file.close();

            System.out.println("Landmarks ways have been serialized");

        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Reads landmark definitions from a serialization file.
     */
    private static void getData(String fileName) {
        try {
            // Reading the object from a file
            FileInputStream file = new FileInputStream(fileName);
            ObjectInputStream in = new ObjectInputStream(file);

            // Method for deserialization of object
            LandmarkSerializer writer = (LandmarkSerializer) in.readObject();

            in.close();
            file.close();

            int cutOffLength = writer.getDistanceOfLandmarkToEveryVertex().length;
            //int cutOffLength = 370; Can be used to speed-up the pre-processing once a serialization file with many landmarks already exists


            System.out.println("Coastline ways have been deserialized ");
            distanceOfLandmarkToEveryVertex = Arrays.copyOfRange(writer.getDistanceOfLandmarkToEveryVertex(), 0, cutOffLength);
            landmarkNodeIDs = Arrays.copyOfRange(writer.getLandmarkNodeIDs(),0, cutOffLength);
            numberOfUsagesOfLandmark = new int[landmarkNodeIDs.length];
            Arrays.fill(numberOfUsagesOfLandmark, 0);
            System.out.println("Read landmarks in: " + landmarkNodeIDs.length);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Initializes the landmarks that should be later used for the routing with ALT algorithms.
     * This includes basically to first choose a certain distribution of landmarks and secondly to
     * to pre-calculate all distances of the landmarks to all other vertices.
     *
     * @param mode The {@link LandmarkDistributionMode} type that defines which distrbution strategy should be applied.
     */
    public static void initLandmarks(LandmarkDistributionMode mode) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        System.out.println("Started initializing landmarks. This might take a while!");

        File f = new File(mode.getSerFileName());

        if (f.exists()) {
            getData(mode.getSerFileName());
        } else {
            LandmarkInitializer initializer = mode.getInitializerClass().getConstructor().newInstance();
            distanceOfLandmarkToEveryVertex = initializer.getDistanceOfLandmarkToEveryVertex();
            landmarkNodeIDs = initializer.getLandmarkNodeIDs();
            numberOfUsagesOfLandmark = new int[landmarkNodeIDs.length];
            storeData(mode.getSerFileName());
        }

        landmarkLowerBoundOnSTDistance = new double[landmarkNodeIDs.length];
        maxHeap = new LandmarkBoundMaxHeap();

    }

    /**
     * ONLY FOR TEST PURPOSES to test two single landmarks for a query.
     */
    public static void initTwoLandmarksForTests() {

        numberOfUsagesOfLandmark = new int[2];
        landmarkNodeIDs = new int[2];


        int firstLandmarkID = Grid.getNearestGridNodeByCoordinates(58.21136309491939, -90.21023977480255, true);
        // int startPointID = Grid.getNearestGridNodeByCoordinates(58.91200127989527, -87.01178301212924);
        int secondLandmarkID = Grid.getNearestGridNodeByCoordinates(23.724819953363948, -64.82243614115036, true);
        // int targetPointID = Grid.getNearestGridNodeByCoordinates(27.219255594563343, -67.9900736817729);
        landmarkNodeIDs[0] = firstLandmarkID;
        landmarkNodeIDs[1] = secondLandmarkID;

        distanceOfLandmarkToEveryVertex = new int[2][Node.getSize()];
        //distanceOfLandmarkToEveryVertex = new int[1][Node.getSize()];

        DijkstraRouter router = new DijkstraRouter();

        distanceOfLandmarkToEveryVertex[0] = router.routeToAllVertices(firstLandmarkID, 4);
        distanceOfLandmarkToEveryVertex[1] = router.routeToAllVertices(secondLandmarkID, 4);
    }

    /**
     * @return The number of all available landmarks.
     */
    public static int getSize() {
        return Landmark.distanceOfLandmarkToEveryVertex.length;
    }

    /**
     * Calculates a color for a heatmap in hex code format.
     *
     * @param value The value which should be represented by the color.
     * @param maxValue The max value that is possible.
     * @param minValue The minimum value that occurred.
     * @return A hex string representing a color.
     */
    private static String getHeatmapColorForCSS(int value, int maxValue, int minValue) {
        float normalizedValue0to1 = (float) value / (float) maxValue;

        float minHue = 350;
        float maxHue = 110;
        float hueDiff = (minHue + maxHue) % 360;

        float h = (minHue + (normalizedValue0to1) * hueDiff) % 360;
        float s = 1;
        float b = 1f;

        Color newColor = Color.getHSBColor(h / 360, s, b);
        return String.format("#%02X%02X%02X", newColor.getRed(), newColor.getGreen(), newColor.getBlue());
    }

    /**
     * Exports all landmarks to a GeoJSON file with different colors for landmarks depending on how
     * much they were used.
     *
     * @throws IOException If something with writing to the export file fails.
     */
    public static void toGeoJSON() throws IOException {
        int maxUsages = Collections.max(Arrays.asList(org.apache.commons.lang3.ArrayUtils.toObject(numberOfUsagesOfLandmark)));
        int minUsages = Collections.min(Arrays.asList(org.apache.commons.lang3.ArrayUtils.toObject(numberOfUsagesOfLandmark)));

        List<String> colorHexs = new ArrayList<>();
        List<GridNode> nodes = new ArrayList<>();
        List<String> addProps = new ArrayList<>();
        for (int i = 0; i < distanceOfLandmarkToEveryVertex.length; i++) {
            GridNode node = new GridNode(Node.getLatitude(landmarkNodeIDs[i]), Node.getLongitude(landmarkNodeIDs[i]));
            nodes.add(node);
            colorHexs.add(getHeatmapColorForCSS(numberOfUsagesOfLandmark[i], maxUsages, minUsages));
            addProps.add(Integer.toString(numberOfUsagesOfLandmark[i]));
        }


        JSONObject json = GeoJsonConverter.osmNodesToGeoJSON(nodes, colorHexs, addProps);
        Files.writeString(Path.of("landmarkHeat.json"), json.toString(), StandardCharsets.UTF_8);
    }
}
