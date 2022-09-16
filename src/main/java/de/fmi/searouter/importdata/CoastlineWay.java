package de.fmi.searouter.importdata;

import de.fmi.searouter.coastlinegrid.CoastlineWays;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an OSM (open street map) Way (https://wiki.openstreetmap.org/wiki/Way) with the tag
 * "natural=coastline" (https://wiki.openstreetmap.org/wiki/Coastline). Instances of this class are
 * only used for the import of OSM data and the initialization of more efficient data structures like
 * the {@link CoastlineWays} class.
 *
 * A valid CoastlineWay should contain at least one edge (= 2 points).
 */
public class CoastlineWay {

    /**
     * All nodes of this CoastlineWay
     */
    private List<Point> points;

    /**
     * Creates a new empty {@link CoastlineWay}.
     */
    public CoastlineWay() {
        points = new ArrayList<>();
    }

    /**
     * Creates a new {@link CoastlineWay} with an initial list of {@link Point points/nodes}.
     *
     * @param points All points/nodes that belong to this way.
     */
    public CoastlineWay(List<Point> points) {
        this.points = points;
    }

    /**
     * @return The number of edges that make up this {@link CoastlineWay}.
     */
    public int getNumberOfEdges() {
        if (points.size() <= 1) {
            return 0;
        }
        return points.size() - 1;
    }

    /**
     * @return The number of points/nodes that make up this {@link CoastlineWay}.
     * If the CoastlineWay contains only one point 0 is returned as then it is no valid coastline way.
     */
    public int getNumberOfPoints() {
        if (points.size() <= 1) {
            return 0;
        }

        return points.size();
    }

    public List<Point> getPoints() {
        return points;
    }

    public void setPoints(List<Point> points) {
        this.points = points;
    }
}

