package de.fmi.searouter.router;

import de.fmi.searouter.importdata.LatLong;

/**
 * User request asking for a new route calculation. Used as JSON mapping object for the REST api.
 */
public class RoutingRequest {

    private LatLong startPoint;
    private LatLong endPoint;
    private String router;

    public String getRouter() {
        return router;
    }

    public void setRouter(String router) {
        this.router = router;
    }

    public LatLong getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(LatLong startPoint) {
        this.startPoint = startPoint;
    }

    public LatLong getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(LatLong endPoint) {
        this.endPoint = endPoint;
    }
}
