package de.fmi.searouter.coastlinegrid;

import de.fmi.searouter.dijkstragrid.GridNode;
import de.fmi.searouter.utils.IntersectionHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A GridParent is a {@link GridCell} which is an intermediate node (not the leaf node)
 * of the multi-level grid cell tree.
 */
public class GridParent extends GridCell {

    // Boundaries of the cell
    private final double southernLatitude;
    private final double northernLatitude;
    private final double westernLongitude;
    private final double easternLongitude;

    // Child cells of this cell (sub-divison)
    private final GridCell[][] lowerLevelCells;

    // Thresholds denoting the borders between lower level grid cells
    private double[] innerLatBorders;
    private double[] innerLonBorders;

    // The coordinates of the center point of this cell
    private double centerPointLat;
    private double centerPointLon;

    /**
     * Initialzes a {@link GridParent} {@link GridCell}.
     *
     * @param edgeIDs          The edge IDs that belong to this new cell.
     * @param southernLatitude The southern border of the cell.
     * @param nothernLatitude  The northern border of the cell.
     * @param westernLongitude The western border of the cell.
     * @param easternLongitude The eastern border of the cell.
     */
    public GridParent(Set<Integer> edgeIDs, double southernLatitude, double nothernLatitude, double westernLongitude,
                      double easternLongitude) {
        this.southernLatitude = southernLatitude;
        this.northernLatitude = nothernLatitude;
        this.westernLongitude = westernLongitude;
        this.easternLongitude = easternLongitude;

        //use a 3x3 lower level to make sure the center point of [1][1] is the same as the one for the upper level
        lowerLevelCells = new GridCell[3][3];

        // inner border arrays initialized in this function
        buildLowerLevel(edgeIDs);
    }

    /**
     * Build the lower level grid. Note that this does not initialize the grid.
     *
     * @param edgeIDs A set of IDs of edges contained within this {@link GridParent}.
     */
    private void buildLowerLevel(Set<Integer> edgeIDs) {

        double latSeparation = Math.abs(northernLatitude - southernLatitude) / 3;
        double lonSeparation = Math.abs(westernLongitude - easternLongitude) / 3;

        // Calculate the borders of the lower level GridCells, both latitude and longitude
        double[] lowerLevelLat = new double[]{
                southernLatitude, southernLatitude + latSeparation, southernLatitude + 2 * latSeparation, northernLatitude
        };
        double[] lowerLevelLon = new double[]{
                westernLongitude, westernLongitude + lonSeparation, westernLongitude + 2 * lonSeparation, easternLongitude
        };

        // Assign inner border arrays, since we will need these values after construction of this object
        innerLatBorders = new double[]{lowerLevelLat[1], lowerLevelLat[2]};
        innerLonBorders = new double[]{lowerLevelLon[1], lowerLevelLon[2]};

        // Assign edges to sub grids
        for (int latIdx = 0; latIdx < 3; latIdx++) {

            for (int lonIdx = 0; lonIdx < 3; lonIdx++) {
                // Check which edges are contained within a lower level GridCell
                Set<Integer> edgesInCell = new HashSet<>();
                for (Integer edgeId : edgeIDs) {
                    boolean[] startResults = IntersectionHelper.getPositionInfoOfPointRelativeToCell(
                            CoastlineWays.getStartLatByEdgeIdx(edgeId),
                            CoastlineWays.getStartLonByEdgeIdx(edgeId),
                            lowerLevelLon[lonIdx], lowerLevelLon[lonIdx + 1],
                            lowerLevelLat[latIdx], lowerLevelLat[latIdx + 1]);
                    if (!(startResults[0] || startResults[1] || startResults[2] || startResults[3])) { // Is inside
                        edgesInCell.add(edgeId);
                        continue;
                    }
                    boolean[] destResults = IntersectionHelper.getPositionInfoOfPointRelativeToCell(
                            CoastlineWays.getDestLatByEdgeIdx(edgeId),
                            CoastlineWays.getDestLonByEdgeIdx(edgeId),
                            lowerLevelLon[lonIdx], lowerLevelLon[lonIdx + 1],
                            lowerLevelLat[latIdx], lowerLevelLat[latIdx + 1]);
                    if (!(destResults[0] || destResults[1] || destResults[2] || destResults[3])) { // Is inside
                        edgesInCell.add(edgeId);
                        continue;
                    }

                    // Check if intersection test is necessary or edge is trivially not in cell
                    if ((startResults[0] && destResults[0]) || (startResults[1] && destResults[1]) ||
                            (startResults[2] && destResults[2]) || (startResults[3] && destResults[3])) {
                        // Trivially not contained
                        continue;
                    }

                    // For cases where corner cases still exists (not trivial) a check of intersection is needed
                    boolean intersectsBorder = false;
                    float startLat = CoastlineWays.getStartLatByEdgeIdx(edgeId);
                    float startLon = CoastlineWays.getStartLonByEdgeIdx(edgeId);
                    float destLat = CoastlineWays.getDestLatByEdgeIdx(edgeId);
                    float destLon = CoastlineWays.getDestLonByEdgeIdx(edgeId);
                    if (IntersectionHelper.arcsIntersect( // Western vertical edge of cell, e.g. 0,0 - 1,0
                            startLat, startLon, destLat, destLon,
                            lowerLevelLat[latIdx], lowerLevelLon[lonIdx],
                            lowerLevelLat[latIdx + 1], lowerLevelLon[lonIdx])) {
                        intersectsBorder = true;
                    } else if (IntersectionHelper.arcsIntersect( // Eastern vertical edge of cell, e.g. 0,1 - 1,1
                            startLat, startLon, destLat, destLon,
                            lowerLevelLat[latIdx], lowerLevelLon[lonIdx + 1],
                            lowerLevelLat[latIdx + 1], lowerLevelLon[lonIdx + 1])) {
                        intersectsBorder = true;
                    } else if (IntersectionHelper.crossesLatitude( // Western latitude coordinate of cell
                            startLat, startLon, destLat, destLon,
                            lowerLevelLat[latIdx],
                            lowerLevelLon[lonIdx], lowerLevelLon[lonIdx + 1])) {
                        intersectsBorder = true;
                    } else if (IntersectionHelper.crossesLatitude( // Northern latitude coordinate of cel
                            startLat, startLon, destLat, destLon,
                            lowerLevelLat[latIdx + 1],
                            lowerLevelLon[lonIdx], lowerLevelLon[lonIdx + 1])) {
                        intersectsBorder = true;
                    }

                    if (intersectsBorder) {
                        edgesInCell.add(edgeId);
                    }
                }

                if (edgesInCell.size() >= EDGE_THRESHOLD) {
                    // A new grid cell subdivision is needed, due to the threshold being exceeded
                    lowerLevelCells[latIdx][lonIdx] = new GridParent(edgesInCell, lowerLevelLat[latIdx],
                            lowerLevelLat[latIdx + 1], lowerLevelLon[lonIdx], lowerLevelLon[lonIdx + 1]);
                } else {
                    // Calculate center point of new cell
                    double ctrLat = (lowerLevelLat[latIdx] + lowerLevelLat[latIdx + 1]) / 2;
                    double ctrLon = (lowerLevelLon[lonIdx] + lowerLevelLon[lonIdx + 1]) / 2;

                    // Transform the edge index list to an edge index array
                    int listSize = edgesInCell.size();
                    int[] idArray = new int[listSize];
                    int i = 0;
                    for (Integer toAdd : edgesInCell) {
                        idArray[i] = toAdd;
                        i++;
                    }

                    // As the number of edges is smaller than the threshold no further lower levels are needed --> leaf
                    lowerLevelCells[latIdx][lonIdx] = new GridLeaf(idArray, ctrLat, ctrLon);
                }
            }
        }
    }

    @Override
    public void setCenterPoint(double lat, double lon, boolean isInWater) {
        this.centerPointLat = lat;
        this.centerPointLon = lon;

        // Set middle center point of lower level
        lowerLevelCells[1][1].setCenterPoint(lat, lon, isInWater);

        // Prepare sets of edges contained in the middle row
        Set<Integer>[] middleEdgeLists = new Set[]{
                lowerLevelCells[1][0].getAllContainedEdgeIDs(),
                lowerLevelCells[1][1].getAllContainedEdgeIDs(),
                lowerLevelCells[1][2].getAllContainedEdgeIDs()
        };

        // Initialize middle row horizontally
        boolean[] centersInWater = new boolean[3];
        centersInWater[1] = isInWater;
        centersInWater[0] = lowerLevelCells[1][0].initCenterPoint(lat, lon, isInWater, middleEdgeLists[1],
                ApproachDirection.FROM_HORIZONTAL);
        centersInWater[2] = lowerLevelCells[1][2].initCenterPoint(lat, lon, isInWater, middleEdgeLists[1],
                ApproachDirection.FROM_HORIZONTAL);

        // Initialize top and bottom row vertically from middle row
        for (int i = 0; i < 3; i++) {
            lat = lowerLevelCells[1][i].getCtrLat();
            lon = lowerLevelCells[1][i].getCtrLon();
            lowerLevelCells[0][i].initCenterPoint(lat, lon, centersInWater[i], middleEdgeLists[i],
                    ApproachDirection.FROM_VERTICAL);
            lowerLevelCells[2][i].initCenterPoint(lat, lon, centersInWater[i], middleEdgeLists[i],
                    ApproachDirection.FROM_VERTICAL);
        }
    }

    @Override
    public boolean initCenterPoint(double originCenterPointLat, double originCenterPointLon,
                                   boolean originCenterPointInWater, Set<Integer> edgeIds,
                                   ApproachDirection dir) {
        // Current status whether the center point is in water depending on the number of
        // crossed edges being odd or even
        boolean centerInWater = originCenterPointInWater;

        // Calculate coordinates of center point
        double centerLat = (southernLatitude + northernLatitude) / 2;
        double centerLon = (westernLongitude + easternLongitude) / 2;

        if (dir == ApproachDirection.FROM_HORIZONTAL) {
            // Check for edges from the left, right, and middle subnodes
            for (int i = 0; i < 3; i++) {
                edgeIds.addAll(lowerLevelCells[1][i].getAllContainedEdgeIDs());
            }

            boolean noException;
            do {
                noException = true;
                try {
                    for (Integer edgeId : edgeIds) {
                        if (IntersectionHelper.crossesLatitudeWithException(CoastlineWays.getStartLatByEdgeIdx(edgeId),
                                CoastlineWays.getStartLonByEdgeIdx(edgeId), CoastlineWays.getDestLatByEdgeIdx(edgeId),
                                CoastlineWays.getDestLonByEdgeIdx(edgeId), centerLat, originCenterPointLon, centerLon)) {
                            centerInWater = !centerInWater;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // If this case occurs, the count of intersections may be off. Due to this, we check
                    // a slightly different latitude as a heuristic instead. In practice, no difference can be seen.
                    centerLat += 0.000001;
                    noException = false;

                    // Reset center in water
                    centerInWater = originCenterPointInWater;
                }
            } while (!noException);
        } else {  // Vertical approach
            // Check for edges from the top, bottom, and middle subnodes
            for (int i = 0; i < 3; i++) {
                edgeIds.addAll(lowerLevelCells[i][1].getAllContainedEdgeIDs());
            }

            boolean noException;
            do {
                noException = true;
                try {
                    for (Integer edgeId : edgeIds) {
                        if (IntersectionHelper.arcsIntersectWithException(CoastlineWays.getStartLatByEdgeIdx(edgeId),
                                CoastlineWays.getStartLonByEdgeIdx(edgeId), CoastlineWays.getDestLatByEdgeIdx(edgeId),
                                CoastlineWays.getDestLonByEdgeIdx(edgeId), centerLat, centerLon,
                                originCenterPointLat, originCenterPointLon)) {
                            centerInWater = !centerInWater;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // If an edge has an longitude that lays exactly on the longitude border of this cell, the count
                    // of intersection might be off. For this, the longitude of the origin center point is shifted slightly.
                    originCenterPointLon += 0.000001;
                    noException = false;
                    centerInWater = originCenterPointInWater;
                }
            } while (!noException);

        }

        setCenterPoint(centerLat, centerLon, centerInWater);
        return centerInWater;
    }

    @Override
    public void collectAllCenterpoints(List<GridNode> pointList) {
        for (int latIdx = 0; latIdx < lowerLevelCells.length; latIdx++) {
            for (int lonIdx = 0; lonIdx < lowerLevelCells[latIdx].length; lonIdx++) {
                lowerLevelCells[latIdx][lonIdx].collectAllCenterpoints(pointList);
            }
        }
    }

    @Override
    public double getCtrLat() {
        return lowerLevelCells[1][1].getCtrLat();
    }

    @Override
    public double getCtrLon() {
        return lowerLevelCells[1][1].getCtrLon();
    }

    @Override
    public boolean isPointInWater(float lat, float lon) {
        // First, determine lat idx of responsible lower level cell
        int latIdx;
        if (lat < innerLatBorders[0]) {
            latIdx = 0;
        } else if (lat < innerLatBorders[1]) {
            latIdx = 1;
        } else {
            latIdx = 2;
        }

        // Then, determine lon idx of responsible lower level cell
        int lonIdx;
        if (lon < innerLonBorders[0]) {
            lonIdx = 0;
        } else if (lon < innerLonBorders[1]) {
            lonIdx = 1;
        } else {
            lonIdx = 2;
        }

        // Pass call to responsible cell
        return lowerLevelCells[latIdx][lonIdx].isPointInWater(lat, lon);
    }

    @Override
    public Set<Integer> getAllContainedEdgeIDs() {
        Set<Integer> fullList = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                fullList.addAll(lowerLevelCells[i][j].getAllContainedEdgeIDs());
            }
        }
        return fullList;
    }

    @Override
    public GridNode getCenterPoint() {
        return new GridNode(this.centerPointLat, this.centerPointLon);
    }

}
