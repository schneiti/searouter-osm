package de.fmi.searouter.evaluation;

import de.fmi.searouter.dijkstragrid.Grid;
import de.fmi.searouter.router.*;
import de.fmi.searouter.router.alt.ALTRouter;
import de.fmi.searouter.router.alt.astar.AltAStarRouter;
import de.fmi.searouter.router.alt.consistentbiastar.AltConsBiAStar;
import de.fmi.searouter.router.alt.symmetricastar.AltBiAStarSymmetric;
import de.fmi.searouter.router.astar.AStarRouter;
import de.fmi.searouter.router.bidijkstra.BiDijkstra;
import de.fmi.searouter.router.consistentbiastar.BiAStarConsistent;
import de.fmi.searouter.router.dijkstra.DijkstraRouter;
import de.fmi.searouter.router.symmetricastar.BiAStarSymmetric;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point to the router evaluation script.
 */
public class EvaluationMain {

    public static void main(String[] args) {

        // Import grid graph
        try {
           Grid.importFmiFile("exported_grid.fmi");
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
          Evaluation, change these lines to test different router/router configurations
         */
        List<Router> routerToEvaluate = new ArrayList<>();
        routerToEvaluate.add(new BiDijkstra());
        routerToEvaluate.add(new AStarRouter());
        routerToEvaluate.add(new BiAStarSymmetric());
        routerToEvaluate.add(new BiAStarConsistent());
        routerToEvaluate.add(new AltAStarRouter());
        routerToEvaluate.add(new AltBiAStarSymmetric());
        routerToEvaluate.add(new AltConsBiAStar());

        Evaluator eval = new Evaluator();
        eval.evaluateRouting(routerToEvaluate, new DijkstraRouter());

        /* OPTIONAL for visualizing Landmark distributions
        try {
            Landmark.toGeoJSON();
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }

}
