package de.fmi.searouter.rest;

import de.fmi.searouter.router.Router;
import de.fmi.searouter.router.RoutingRequest;
import de.fmi.searouter.dijkstragrid.Grid;
import de.fmi.searouter.router.alt.astar.AltAStarRouter;
import de.fmi.searouter.router.alt.consistentbiastar.AltConsBiAStar;
import de.fmi.searouter.router.alt.symmetricastar.AltBiAStarSymmetric;
import de.fmi.searouter.router.astar.AStarRouter;
import de.fmi.searouter.router.bidijkstra.BiDijkstra;
import de.fmi.searouter.router.consistentbiastar.BiAStarConsistent;
import de.fmi.searouter.router.dijkstra.DijkstraRouter;
import de.fmi.searouter.router.RoutingResult;
import de.fmi.searouter.router.symmetricastar.BiAStarSymmetric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/route")
public class RoutingController {

    /*
    @Autowired
    DijkstraRouter dijkstraRouter;

    @Autowired
    BiDijkstra biDijkstraRouter;

    @Autowired
    AStarRouter aStarRouter;

    @Autowired
    BiAStarSymmetric biAStarSymmetric;

    @Autowired
    BiAStarConsistent biAStarConsistent;

    @Autowired
    AltAStarRouter altAStarRouter;

    @Autowired
    AltBiAStarSymmetric altBiAStarSymmetric;

    @Autowired
    AltConsBiAStar altConsBiAStar;
*/

    @PostMapping("")
    public ResponseEntity getRoute(@RequestBody RoutingRequest routingRequest) {

        String routerName = routingRequest.getRouter();
        Router router;

        if (routerName == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No valid router chosen");
        }

        switch (routerName) {
            case "dijkstra":
                router = new DijkstraRouter();
                break;
            case "bidijkstra":
                router = new BiDijkstra();
                break;
            case "astar":
                router = new AStarRouter();
                break;
            case "biastar_sym":
                router = new BiAStarSymmetric();
                break;
            case "biastar_cons":
                router = new BiAStarConsistent();
                break;
            case "alt_astar":
                router = new AltAStarRouter();
                break;
            case "alt_biastar_sym":
                router = new AltBiAStarSymmetric();
                break;
            case "alt_biastar_cons":
                router = new AltConsBiAStar();
                break;
            default:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No valid router chosen");
        }

        /*
        switch (routerName) {
            case "dijkstra":
                router = dijkstraRouter;
                break;
            case "bidijkstra":
                router = biDijkstraRouter;
                break;
            case "astar":
                router = aStarRouter;
                break;
            case "biastar_sym":
                router = biAStarSymmetric;
                break;
            case "biastar_cons":
                router = biAStarConsistent;
                break;
            case "alt_astar":
                router = altAStarRouter;
                break;
            case "alt_biastar_sym":
                router = altBiAStarSymmetric;
                break;
            case "alt_biastar_cons":
                router = altConsBiAStar;
                break;
            default:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No valid router chosen");
        }

         */

       int startNodeId = Grid.getNearestGridNodeByCoordinates(routingRequest.getStartPoint().getLatitude(), routingRequest.getStartPoint().getLongitude(), true);
       int destNodeId = Grid.getNearestGridNodeByCoordinates(routingRequest.getEndPoint().getLatitude(), routingRequest.getEndPoint().getLongitude(), true);

        if (startNodeId < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Start position is not on the ocean!");
        }

        if (destNodeId < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Destination position is not on the ocean!");
        }

        RoutingResult res = router.route(startNodeId, destNodeId);

        return ResponseEntity.ok(res);
    }

    @GetMapping("/test")
    public String getTest() {
        return "testSuccess" ;
    }
}
