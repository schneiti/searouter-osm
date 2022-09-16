package de.fmi.searouter.dijkstragrid;

import de.fmi.searouter.coastlinegrid.PointInWaterChecker;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread which is responsible to perform a point-in-polygon check for {@link GridNode}s
 * in a certain range of value of latitudes.
 */
public class NodeCreateWorkerThread extends Thread {

    /**
     * Used to check if a node is on land or in water water building the the graph
     */
    private static final PointInWaterChecker POINT_IN_WATER_CHECKER = PointInWaterChecker.getInstance();


    /**
     * List of all latitude rows that this thread should calculate
     */
    private final List<BigDecimal> latList;

    public NodeCreateWorkerThread() {
        this.latList = new ArrayList<>();
    }

    /**
     * Adds a a latitude for which a row of nodes will be calculated.
     * @param lat the latitude of the row.
     */
    public void addLatitude(BigDecimal lat) {
        this.latList.add(lat);
    }


    /**
     * Calculate nodes for all rows for which a latitude was given previously. These nodes are then added
     * to the gridNodes in the {@link GridCreator}.
     */
    @Override
    public void run() {
        BigDecimal coordinateStepLong = BigDecimal.valueOf(GridCreator.coordinate_step_longitude);

        BigDecimal longEnd = BigDecimal.valueOf(-180);
        // For all latitude rows, check if the points are in water.
        for (BigDecimal lat : latList) {
            for (BigDecimal longitude = BigDecimal.valueOf(180); longitude.compareTo(longEnd) > 0;
                 longitude = longitude.subtract(coordinateStepLong)) {

                if (!POINT_IN_WATER_CHECKER.pointInWater((float) lat.doubleValue(), (float) longitude.doubleValue())) {
                    continue;
                }

                GridNode node = new GridNode(lat.doubleValue(), longitude.doubleValue());

                // Add the node to the central data structures in the GridCreator
                GridCreator.gridNodes.add(node);
                GridCreator.coordinateNodeStore.putIfAbsent(lat.doubleValue(), new ConcurrentHashMap<>());
                GridCreator.coordinateNodeStore.get(lat.doubleValue()).put(longitude.doubleValue(), node);

            }
        }

    }
}
