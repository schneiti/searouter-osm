package de.fmi.searouter.landmarks.initializer;

import de.fmi.searouter.dijkstragrid.Grid;
import de.fmi.searouter.dijkstragrid.Node;
import de.fmi.searouter.landmarks.LandmarkInitializer;
import de.fmi.searouter.router.dijkstra.DijkstraRouter;

import java.util.ArrayList;
import java.util.List;

/**
 * Equally distributes landmarks on a sphere.
 */
public class EqualDistributedOnSphereInitializer implements LandmarkInitializer {

    /**
     * Stores for each landmark all distances to all other vertices in the graph.
     */
    private int[][] distanceOfLandmarkToEveryVertex;

    /**
     * Stores for each landmark the vertex ID like defined in {@link Node}
     */
    private int[] landmarkNodeIDs;

    /**
     * Number of landmarks that would be distributed on earth if no land would exist. The final
     * number of landmarks are NUMBER_OF_PLANNED_LANDMARKS minus the number of landmarks that would
     * be placed on water.
     */
    private final int NUMBER_OF_PLANNED_LANDMARKS = 418;

    public static final int EARTH_RADIUS_METERS = 6371 * 1000;

    /**
     * Distributes landmarks equally on a sphere model of the earth.
     *
     * Algorithm for equi-distributed points on a sphere from: https://www.cmu.edu/biolphys/deserno/pdf/sphere_equi.pdf
     * with one change: For calculating a it would be wrong to take the earth radius into account as then
     * d would become 0 and while calculating m_theta a null-division would take place. This seems to be an error
     * in the above mentioned paper.
     */
    public EqualDistributedOnSphereInitializer() {
        List<Integer> currChosenLandmarks = new ArrayList<>();

        // Find landmark candidates
        int noLandmarks = 0;
        double a = 4 * Math.PI * 1 * 1 / NUMBER_OF_PLANNED_LANDMARKS;
        double d = Math.sqrt(a);
        int m_theta = (int) (Math.PI / d);
        double d_theta = Math.PI / m_theta;
        double d_phi = a / d_theta;
        for (int m = 0; m < m_theta; m++) {
            double theta = Math.PI * (m + 0.5) / m_theta;
            int m_phi = (int) (2 * Math.PI * Math.sin(theta) / d_phi);
            for (int n = 0; n < m_phi; n++) {
                double phi = 2 * Math.PI * n / m_phi;
                double[] newLandmark = cartesianCoordinatesToLatLonDegree(getCartesianPointByTheta(theta, phi));
                int landmarkID = Grid.getNearestGridNodeByCoordinates(newLandmark[0], newLandmark[1], true);
                if (landmarkID >= 0) {
                    currChosenLandmarks.add(landmarkID);
                    noLandmarks++;
                }
            }
        }

        /*
         * Init datastructures, calculate landmark destinations to all vertices
         */
        distanceOfLandmarkToEveryVertex = new int[noLandmarks][Node.getSize()];
        landmarkNodeIDs = new int[noLandmarks];

        DijkstraRouter router = new DijkstraRouter();
        System.out.println("No of landmarks: " + noLandmarks);

        for (int i = 0; i < currChosenLandmarks.size(); i++) {
            landmarkNodeIDs[i] = currChosenLandmarks.get(i);
            distanceOfLandmarkToEveryVertex[i] = router.routeToAllVertices(currChosenLandmarks.get(i), 4);
        }

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
     * Converts cartesian coordinates of a sphere to a lat/lon representation in degree.
     *
     * @param cartesianCoordinates 3D array representing cartesian coordinates on sphere
     * @return 2D array with 0: Latitude and 1: longitude in degree.
     */
    private double[] cartesianCoordinatesToLatLonDegree(double[] cartesianCoordinates) {
        double lat = convertToDegree(Math.asin(cartesianCoordinates[2] / EARTH_RADIUS_METERS));
        double lon = convertToDegree(Math.atan2(cartesianCoordinates[1], cartesianCoordinates[0]));

        return new double[]{lat, lon};
    }

    /**
     * Returns cartesian coordinates by parameter theta and phi
     * as defined in https://www.cmu.edu/biolphys/deserno/pdf/sphere_equi.pdf
     *
     * @param theta Theta parameter of the above mentioned algorithm
     * @param phi   Phi parameter of the above mentioned algorithm
     * @return 3D array representing cartesian coordinates on sphere
     */
    private double[] getCartesianPointByTheta(double theta, double phi) {
        double sinTheta = Math.sin(theta);
        double x = EARTH_RADIUS_METERS * sinTheta * Math.cos(phi);
        double y = EARTH_RADIUS_METERS * sinTheta * Math.sin(phi);
        double z = EARTH_RADIUS_METERS * Math.cos(theta);

        return new double[]{x, y, z};
    }

    @Override
    public String getSerFileName() {
        return "landmarks_equal_distributed_sphere";
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
