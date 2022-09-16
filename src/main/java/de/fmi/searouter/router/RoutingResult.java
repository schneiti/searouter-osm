package de.fmi.searouter.router;

import de.fmi.searouter.dijkstragrid.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * class storing the result of a routing request. Used for communicating with the REST API
 */
public class RoutingResult {

    /**
     * All graph nodes ids that make up the resulted path between a start and destination node.
     */
    private List<Integer> path;

    /**
     * Stores coordinates of points. The inner list represents lat-lon pairs representing the coordinates
     * of one point. The outer list stores all these points in the order of the path found.
     * E.g.: [[0.2, 48.1], [24.0, -47.3]]
     */
    private List<List<Double>> pathCoordinates;

    /**
     * The overall distance of the calculated path.
     */
    private int overallDistance;

    /**
     * The time it took to calculate the path.
     */
    private double calculationTimeInMs;
    private int amountOfNodesPoppedOutOfHeap;
    private boolean routeFound;

    public RoutingResult( boolean routeFound, List<Integer> path, int overallDistance, double calculationTimeInMs, int amountOfNodesPoppedOutOfHeap) {
        this.setPath(path);
        this.routeFound = routeFound;
        this.overallDistance = overallDistance;
        this.calculationTimeInMs = calculationTimeInMs;
        this.amountOfNodesPoppedOutOfHeap = amountOfNodesPoppedOutOfHeap;
    }

    public List<Integer> getPath() {
        return path;
    }

    /**
     * Calculates the path coordinates based on a list of IDs of nodes.
     * @param path contains the IDs of all nodes on the path
     */
    public void setPath(List<Integer> path) {
        this.path = path;
        this.pathCoordinates = new ArrayList<>();
        for (Integer nodeIdx : path) {
            List<Double> coord = new ArrayList<>();
            coord.add(Node.getLatitude(nodeIdx));
            coord.add(Node.getLongitude(nodeIdx));
            this.pathCoordinates.add(coord);
        }
    }

    public int getOverallDistance() {
        return overallDistance;
    }

    public void setOverallDistance(int overallDistance) {
        this.overallDistance = overallDistance;
    }

    public double getCalculationTimeInMs() {
        return calculationTimeInMs;
    }

    public void setCalculationTimeInMs(double calculationTimeInMs) {
        this.calculationTimeInMs = calculationTimeInMs;
    }

    public List<List<Double>> getPathCoordinates() {
        return pathCoordinates;
    }

    public void setPathCoordinates(List<List<Double>> pathCoordinates) {
        this.pathCoordinates = pathCoordinates;
    }

    @Override
    public String toString() {
        return "RoutingResult{" +
                "path=" + path +
                ", overallDistance=" + overallDistance +
                ", calculationTimeInMs=" + calculationTimeInMs +
                '}';
    }

    public int getAmountOfNodesPoppedOutOfHeap() {
        return amountOfNodesPoppedOutOfHeap;
    }

    public void setAmountOfNodesPoppedOutOfHeap(int amountOfNodesPoppedOutOfHeap) {
        this.amountOfNodesPoppedOutOfHeap = amountOfNodesPoppedOutOfHeap;
    }

    public boolean isRouteFound() {
        return routeFound;
    }

    public void setRouteFound(boolean routeFound) {
        this.routeFound = routeFound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoutingResult)) return false;
        RoutingResult that = (RoutingResult) o;
        return overallDistance == that.overallDistance &&
                routeFound == that.routeFound;
    }

    @Override
    public int hashCode() {
        return Objects.hash(overallDistance, routeFound);
    }
}
