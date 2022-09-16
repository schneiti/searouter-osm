package de.fmi.searouter.landmarks.initializer;

import de.fmi.searouter.dijkstragrid.Grid;
import de.fmi.searouter.dijkstragrid.Node;
import de.fmi.searouter.landmarks.LandmarkInitializer;
import de.fmi.searouter.router.dijkstra.DijkstraRouter;
import de.fmi.searouter.utils.IntersectionHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This strategy was adapted from Goldberg et al. (https://www.cs.princeton.edu/courses/archive/spr06/cos423/Handouts/GH05.pdf)
 * and is based on distributing landmarks step-by-step with always choosing a landmark that is farthest away from
 * the current set of landmarks.
 * <p>
 * HOWEVER: This does not work well on a big sphere with many landmarks well as very soon there will be a sort of clustering
 * due to on earth all farthest distances lay on a great circle with many farthest away candidates and a sort
 * of saturation effect that occurs because of the geographic midpoint becomes more and more fixed the more
 * landmarks are already in the set of landmarks.
 */
public class MaxAvoidInitializer implements LandmarkInitializer {

    /**
     * Stores for each landmark all distances to all other vertices in the graph.
     */
    private int[][] distanceOfLandmarkToEveryVertex;

    /**
     * Stores for each landmark the vertex ID like defined in {@link Node}
     */
    private int[] landmarkNodeIDs;

    private final int NUMBER_OF_LANDMARKS = 300;

    public MaxAvoidInitializer() {

        List<Integer> currChosenLandmarks = new ArrayList<>();

        double[] startPoint = new double[]{10.0800, 100.0800};

        List<double[]> currentLandmarks = new ArrayList<>();
        currChosenLandmarks.add(Grid.getNearestGridNodeByCoordinates(startPoint[0], startPoint[1], true));
        currentLandmarks.add(startPoint);

        DijkstraRouter router = new DijkstraRouter();

        for (int i = 0; i < NUMBER_OF_LANDMARKS - 1; i++) {
            // Calculate the current center point
            double[] geogaraphicMidpoint = getGeographicMidpoint(currentLandmarks);
            // Test if midpoint can be found on water
            int midpointNodeIdx = Grid.getNearestGridNodeByCoordinates(geogaraphicMidpoint[0], geogaraphicMidpoint[1], true);

            if (midpointNodeIdx < 0) {
                // Land node --> get the nearest water node
                midpointNodeIdx = Grid.getNearestGridNodeByCoordinates(geogaraphicMidpoint[0], geogaraphicMidpoint[1], false);
            }

            /* VARIANT USING HAVERTSINE DISTANCE INSTEAD OF GRAPH DISTANCE
            int maxIdx = -1;
            double currMaxDistance = 0;
            for (int nodeIdx = 0; nodeIdx < Node.getSize(); nodeIdx++) {
                double currDistance = IntersectionHelper.getDistance(geogaraphicMidpoint[0], geogaraphicMidpoint[1], Node.getLatitude(nodeIdx), Node.getLongitude(nodeIdx));
                if (currDistance > currMaxDistance && !currChosenLandmarks.contains(nodeIdx)) {
                    maxIdx = nodeIdx;
                    currMaxDistance = currDistance;
                }
            }
            */

            // Get the graph point that is the farthest away from the geographic midpoint
            int[] distancesToGeographicMidpoint = router.routeToAllVertices(midpointNodeIdx, 4);
            int maxIdx = -1;
            int currMaxDistance = 0;
            List<Integer> collectMaxDistanceNodes = new ArrayList<>(); // List as there are potential many nodes with the same distance (as infinity)
            for (int j = 0; j < distancesToGeographicMidpoint.length; j++) {
                if (distancesToGeographicMidpoint[j] == Integer.MAX_VALUE && !currChosenLandmarks.contains(j)) {
                    collectMaxDistanceNodes.add(j);
                }

                if (currMaxDistance < distancesToGeographicMidpoint[j] && !currChosenLandmarks.contains(j)) {
                    currMaxDistance = distancesToGeographicMidpoint[j];
                    maxIdx = j;
                }
            }


            // If there are nodes that have the same path distance this method assures that the one is chosen that is farthest
            // away based on the Havertsine distance
            if (collectMaxDistanceNodes.size() > 0) {
                maxIdx = -1;
                double currMaxD = 0;
                for (Integer nodeIdx : collectMaxDistanceNodes) {
                    double currDistance = IntersectionHelper.getDistance(geogaraphicMidpoint[0], geogaraphicMidpoint[1], Node.getLatitude(nodeIdx), Node.getLongitude(nodeIdx));
                    if (currDistance > currMaxD) {
                        maxIdx = nodeIdx;
                        currMaxD = currDistance;
                    }
                }
            }

            currentLandmarks.add(new double[]{Node.getLatitude(maxIdx), Node.getLongitude(maxIdx)});
            System.out.println("Added landmark");
            currChosenLandmarks.add(maxIdx);
        }

        // Init data structures by calculating all distances to the landmarks
        distanceOfLandmarkToEveryVertex = new int[NUMBER_OF_LANDMARKS][Node.getSize()];
        landmarkNodeIDs = new int[NUMBER_OF_LANDMARKS];

        for (int i = 0; i < currChosenLandmarks.size(); i++) {
            landmarkNodeIDs[i] = currChosenLandmarks.get(i);
            distanceOfLandmarkToEveryVertex[i] = router.routeToAllVertices(currChosenLandmarks.get(i), 4);
        }
    }

    /**
     * Calculates the geographic midpoint of a list of given latitude longitude pairs representing
     * a point on earth. On 2D this approach would be called "centroid".
     *
     * Algorithm from: http://www.geomidpoint.com/calculation.html
     *
     * @param latLonPairsInDegree A list of latitude longitude pairs (2D array)
     * @return 2D array representing the geographic midpoint of the latLonPairsInDegree (0: latitude, 1: longitude)
     */
    public double[] getGeographicMidpoint(List<double[]> latLonPairsInDegree) {
        double[][] latLonPairsInCartesian = new double[latLonPairsInDegree.size()][3];

        // Additional weights for geographic points. With w=1 all points are weighted equally.
        int[] weight = new int[latLonPairsInDegree.size()];
        Arrays.fill(weight, 1);

        // Transform all coordinates to a cartesian representation
        for (int i = 0; i < latLonPairsInCartesian.length; i++) {
            latLonPairsInCartesian[i] = latLonToCartesianCoordinates(
                    convertToRadian(latLonPairsInDegree.get(i)[0]),
                    convertToRadian(latLonPairsInDegree.get(i)[1])
            );
        }

        double combinedWeight = Arrays.stream(weight).sum();

        double weightedAvgX = 0;
        double weightedAvgY = 0;
        double weightedAvgZ = 0;
        for (int i = 0; i < latLonPairsInCartesian.length; i++) {
            weightedAvgX += latLonPairsInCartesian[i][0] * weight[i];
            weightedAvgY += latLonPairsInCartesian[i][1] * weight[i];
            weightedAvgZ += latLonPairsInCartesian[i][2] * weight[i];
        }
        weightedAvgX = weightedAvgX / combinedWeight;
        weightedAvgY = weightedAvgY / combinedWeight;
        weightedAvgZ = weightedAvgZ / combinedWeight;

        return cartesianToLatLonDegree(weightedAvgX, weightedAvgY, weightedAvgZ);
    }

    private double[] latLonToCartesianCoordinates(double lat, double lon) {
        double cosLat = Math.cos(lat);
        double x = cosLat * Math.cos(lon);
        double y = cosLat * Math.sin(lon);
        double z = Math.sin(lat);

        return new double[]{x, y, z};
    }

    /**
     * @param x x cartesian coordinate
     * @param y y cartesian coordinate
     * @param z z cartesian coordinate
     * @return Point in lat/lon degree coordinates, 2D array (0: latitude, 1: longitude)
     */
    public double[] cartesianToLatLonDegree(double x, double y, double z) {
        double lon = Math.atan2(y, x);
        double lat = Math.atan2(z, Math.sqrt(x * x + y * y));

        return new double[]{convertToDegree(lat), convertToDegree(lon)};
    }

    /**
     * Converts a coordinate in radian to a degree representation
     *
     * @param coordinate the coordinate in degree to transform to radian
     * @return the coordinate in radian coordinates
     */
    private double convertToDegree(double coordinate) {
        return coordinate * (180.0 / Math.PI);
    }

    /**
     * Converts a coordinate in degrees to a radian representation
     *
     * @param coordinate the coordinate in degree to transform to radian
     * @return the coordinate in radian coordinates
     */
    private double convertToRadian(double coordinate) {
        return coordinate * (Math.PI / 180.0);
    }

    @Override
    public String getSerFileName() {
        return "landmarks_max_avoid";
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
