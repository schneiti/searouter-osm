package de.fmi.searouter.router.alt;

import de.fmi.searouter.router.Router;
import de.fmi.searouter.router.RoutingResult;

public interface ALTRouter extends Router {

    void setMaxNumberOfQueryLandmarks(int maxLandmarks);

}
