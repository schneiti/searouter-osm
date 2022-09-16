package de.fmi.searouter.coastlinegrid;

import de.fmi.searouter.dijkstragrid.GridNode;
import de.fmi.searouter.utils.IntersectionHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A GridLeaf is a {@link GridCell} which is an leaf node (not an intermediate {@link GridParent})
 * of the multi-level grid cell tree.
 */
public class GridLeaf extends GridCell {

    /**
     * Contains all IDs of edges contained in this {@link GridLeaf}
     */
    private final int[] edgeIds;

    /**
     * Number of edges in this {@link GridLeaf}
     */
    private final int edgeCount;

    // Center point information
    private boolean centerPointInWater;
    private double latCenterPoint;
    private double lonCenterPoint;

    public GridLeaf(int[] edgeIds, double latCenterPoint, double lonCenterPoint) {
        this.edgeIds = edgeIds;
        this.edgeCount = edgeIds.length;
        this.latCenterPoint = latCenterPoint;
        this.lonCenterPoint = lonCenterPoint;
    }

    @Override
    public void setCenterPoint(double lat, double lon, boolean isInWater) {
        this.latCenterPoint = lat;
        this.lonCenterPoint = lon;
        this.centerPointInWater = isInWater;
    }

    @Override
    public boolean initCenterPoint(double originCenterPointLat, double originCenterPointLon,
                                   boolean originCenterPointInWater, Set<Integer> allEdgeIds,
                                   ApproachDirection dir) {
        centerPointInWater = originCenterPointInWater;
        for (int edgeId : this.edgeIds) {
            allEdgeIds.add(edgeId);
        }

        if (dir == ApproachDirection.FROM_HORIZONTAL) {
            boolean noException;
            do {
                noException = true;
                try {
                    for (Integer edgeId : allEdgeIds) {
                        if (IntersectionHelper.crossesLatitudeWithException(CoastlineWays.getStartLatByEdgeIdx(edgeId),
                                CoastlineWays.getStartLonByEdgeIdx(edgeId), CoastlineWays.getDestLatByEdgeIdx(edgeId),
                                CoastlineWays.getDestLonByEdgeIdx(edgeId), latCenterPoint,
                                originCenterPointLon, lonCenterPoint)) {
                            centerPointInWater = !centerPointInWater;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // If an edge has an longitude that lays exactly on the longitude border of this cell, the count
                    // of intersection might be off. For this, the longitude of the origin center point is shifted
                    // slightly.
                    latCenterPoint += 0.000001;
                    noException = false;
                    centerPointInWater = originCenterPointInWater;
                }
            } while (!noException);
        } else {
            boolean noException;
            do {
                noException = true;
                try {
                    for (Integer edgeId : allEdgeIds) {
                        if (IntersectionHelper.arcsIntersectWithException(CoastlineWays.getStartLatByEdgeIdx(edgeId),
                                CoastlineWays.getStartLonByEdgeIdx(edgeId), CoastlineWays.getDestLatByEdgeIdx(edgeId),
                                CoastlineWays.getDestLonByEdgeIdx(edgeId),
                                originCenterPointLat, originCenterPointLon, latCenterPoint, lonCenterPoint)) {
                            centerPointInWater = !centerPointInWater;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // If an edge has an longitude that lays exactly on the longitude border of this cell, the count
                    // of intersection might be off. For this, the longitude of the origin center point is shifted slightly.
                    lonCenterPoint += 0.000001;
                    noException = false;
                    centerPointInWater = originCenterPointInWater;
                }
            } while (!noException);
        }

        return centerPointInWater;
    }

    @Override
    public boolean isPointInWater(float lat, float lon) {
        boolean pointInWater = centerPointInWater;

        // Ray casting algorithm to find out whether the point is on water
        for (int i = 0; i < edgeCount; i++) {
            int idx = edgeIds[i];
            if (IntersectionHelper.arcsIntersect(lat, lon, latCenterPoint, lonCenterPoint,
                    CoastlineWays.getStartLatByEdgeIdx(idx), CoastlineWays.getStartLonByEdgeIdx(idx),
                    CoastlineWays.getDestLatByEdgeIdx(idx), CoastlineWays.getDestLonByEdgeIdx(idx))) {
                pointInWater = !pointInWater;
            }
        }

        return pointInWater;
    }

    @Override
    public Set<Integer> getAllContainedEdgeIDs() {
        return Arrays.stream(edgeIds).boxed().collect(Collectors.toSet());
    }

    @Override
    public GridNode getCenterPoint() {
        return new GridNode(latCenterPoint, lonCenterPoint);
    }

    @Override
    public void collectAllCenterpoints(List<GridNode> pointList) {
        pointList.add(this.getCenterPoint());
    }

    @Override
    public double getCtrLat() {
        return latCenterPoint;
    }

    @Override
    public double getCtrLon() {
        return lonCenterPoint;
    }

}
