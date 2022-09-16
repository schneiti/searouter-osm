package de.fmi.searouter.coastlinegrid;

import de.fmi.searouter.dijkstragrid.GridNode;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * A GridCell is a area on the world map.
 */
public abstract class GridCell implements Serializable {

    /**
     * From which direction the cell was created. Relevant for {@link #initCenterPoint}.
     */
    public enum ApproachDirection {
        FROM_HORIZONTAL, FROM_VERTICAL
    }

    /**
     * Set the a new center point of a {@link GridCell}.
     *
     * @param lat The latitude of the new center point
     * @param lon The longitude of the new center point.
     * @param isInWater Whether the center point is in water or on land.
     */
    public abstract void setCenterPoint(double lat, double lon, boolean isInWater);

    /**
     * The maximum number of coastline edges that are allowed to be in one cell.
     */
    protected static int EDGE_THRESHOLD = 1000;

    /**
     * Initializes the center point of the cell by using information of the center point
     * of the previous cell.
     *
     * @param originCenterPointLat The latitude of the comparison center point.
     * @param originCenterPointLon The longitude of the comparison center point.
     * @param originCenterPointInWater The status of the center point to compare (in water or not on land)
     * @param additionalEdges Edges that need to be considered during the initialization of the center point
     * @param dir From which direction the cell was created (whether the comparison center point lays vertically or
     *            horizontally to the new center point).
     * @return True, if new initialized center point is in water, false if not.
     */
    public abstract boolean initCenterPoint(double originCenterPointLat, double originCenterPointLon,
                                         boolean originCenterPointInWater, Set<Integer> additionalEdges,
                                         ApproachDirection dir);

    /**
     * Check if a given point is in water or on land. Note that the result is only valid if the point is located
     * within the area of this {@link GridCell}.
     * @param lat The latitude of the point to check
     * @param lon The longitude of the point to check
     * @return true if the point is in water, else false
     */
    public abstract boolean isPointInWater(float lat, float lon);

    /**
     * @return All edge ids that are contained in this {@link GridCell}.
     */
    public abstract Set<Integer> getAllContainedEdgeIDs();

    /**
     * @return A {@link GridNode} with the coordinates of the center point of the {@link GridCell}.
     */
    public abstract GridNode getCenterPoint();

    /**
     * Collects all center points of a grid cell in puts it into the pointList.
     *
     * @param pointList The list in which all center points are collected.
     */
    public abstract void collectAllCenterpoints(List<GridNode> pointList);

    /**
     * @return The latitude of the centerpoint of this {@link GridCell}.
     */
    public abstract double getCtrLat();

    /**
     * @return The longitude of the centerpoint of this {@link GridCell}.
     */
    public abstract double getCtrLon();


}
