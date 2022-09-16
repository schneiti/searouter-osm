package de.fmi.searouter.evaluation;

import de.fmi.searouter.dijkstragrid.Edge;
import de.fmi.searouter.dijkstragrid.Grid;
import de.fmi.searouter.dijkstragrid.Node;
import de.fmi.searouter.router.Router;
import de.fmi.searouter.router.RoutingResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Can be used to perform an evaluation of different routing algorithms against a default
 * routing algorithm. Prints routing statistics to the console and verifies that all results
 * match with a given comparison router.
 */
public class Evaluator {

    /**
     * The number of queries that should be performed for evaluation.
     */
    private final int NO_OF_QUERIES = 100;

    /**
     * Start node indices conform with the index definitions in {@link Node}
     */
    private List<Integer> randomSourceNodes;

    /**
     * Destination node indices conform with the index definitions in {@link Node}
     */
    private List<Integer> randomTargetNodes;

    /**
     * Seed for making the random start-destination node generator deterministic.
     */
    long SEED = 666;

    /**
     * Initializes {@link Evaluator#randomSourceNodes} and {@link Evaluator#randomTargetNodes} by choosing
     * randomly nodes from all available nodes.
     */
    private void chooseArbitraryTestNodes() {
        // The maximum allowed node index
        int maxNodeIdx = Node.getSize() - 1;

        randomSourceNodes = new ArrayList<>();
        randomTargetNodes = new ArrayList<>();

        Random random = new Random(SEED);


        for (int noOfDraws = 0; noOfDraws < NO_OF_QUERIES; noOfDraws++) {
            randomSourceNodes.add(random.nextInt(maxNodeIdx + 1));
            randomTargetNodes.add(random.nextInt(maxNodeIdx + 1));
        }


        randomSourceNodes.add(Grid.getNearestGridNodeByCoordinates(15.84, -30.96, true));
        randomTargetNodes.add(Grid.getNearestGridNodeByCoordinates(23.76, 152.64, true));
    }

    /**
     * @param routersToEvaluate A list of {@link Router Routers} that should be evaluated.
     * @param comparisonRouter  The router which acts as a comparison router, which means that its results
     *                          must be matched (concerning distance and negative routing results) by all routersToEvaluate
     *                          routers to be valid.
     */
    public void evaluateRouting(List<Router> routersToEvaluate, Router comparisonRouter) {
        chooseArbitraryTestNodes();

        List<RoutingResult> comparisonRouterResults = new ArrayList<>();

        for (int queryIdx = 0; queryIdx < randomSourceNodes.size(); queryIdx++) {
            RoutingResult result = comparisonRouter.route(randomSourceNodes.get(queryIdx), randomTargetNodes.get(queryIdx));
            comparisonRouterResults.add(result);
        }

        System.out.println("== RESULTS for comparison router: " + comparisonRouter.getName() + " router ==");
        System.out.println("Average running time: " + comparisonRouterResults.stream().mapToDouble(RoutingResult::getCalculationTimeInMs).average().getAsDouble() + " ms");
        System.out.println("Average number of nodes popped out of heap: " + comparisonRouterResults.stream().mapToInt(RoutingResult::getAmountOfNodesPoppedOutOfHeap).average().getAsDouble());

        for (int i = 0; i < routersToEvaluate.size(); i++) {
            Router r = routersToEvaluate.get(i);

            List<RoutingResult> results = new ArrayList<>();
            List<RoutingResult> nonSuccessfulResults = new ArrayList<>();

            for (int queryIdx = 0; queryIdx < randomSourceNodes.size(); queryIdx++) {
                RoutingResult result = r.route(randomSourceNodes.get(queryIdx), randomTargetNodes.get(queryIdx));


                results.add(result);
                if (!result.isRouteFound()) {
                    nonSuccessfulResults.add(result);
                }

                if (!comparisonRouterResults.get(queryIdx).equals(result)) {

                    long distanceBidirectional = 0;
                    for (int currNodeIdx = 0; currNodeIdx < result.getPath().size() - 1; currNodeIdx++) {
                        int edgeID = Grid.getEdgeIDByNodeIDs(result.getPath().get(currNodeIdx), result.getPath().get(currNodeIdx + 1));
                        if (edgeID < 0) {
                            System.out.println("Keine edge zwischen " + result.getPath().get(currNodeIdx) + " und " + result.getPath().get(currNodeIdx + 1));
                            System.out.println("Koordinaten: " + Node.getLatitude(result.getPath().get(currNodeIdx)) + ", " + Node.getLongitude(result.getPath().get(currNodeIdx)) + " || "
                                    + Node.getLatitude(result.getPath().get(currNodeIdx + 1)) + ", " + Node.getLongitude(result.getPath().get(currNodeIdx + 1)));
                        } else {
                            distanceBidirectional += Edge.getDist(edgeID);
                        }
                    }

                    throw new IllegalStateException("Dijkstra and router " + r.getName() + " result do not match");
                }
            }

            System.out.println("== RESULTS for " + r.getName() + " router ==");
            System.out.println("Average running time: " + results.stream().mapToDouble(RoutingResult::getCalculationTimeInMs).average().getAsDouble() + " ms");
            System.out.println("Average number of nodes popped out of heap: " + results.stream().mapToInt(RoutingResult::getAmountOfNodesPoppedOutOfHeap).average().getAsDouble());
            System.out.println("Negative Results: ");

            for (RoutingResult negativeResult : nonSuccessfulResults) {
                System.out.println("Point A: " + negativeResult.getPathCoordinates().get(0).get(0) + ", " + negativeResult.getPathCoordinates().get(0).get(1) + "| Point B: " + negativeResult.getPathCoordinates().get(negativeResult.getPathCoordinates().size() - 1).get(0) + ", " + negativeResult.getPathCoordinates().get(negativeResult.getPathCoordinates().size() - 1).get(1));
            }

            System.out.println("=================================================");

        }
    }

        /**
         * USE THIS ONLY IF ONLY ONE ALT ROUTER IS USED. OTHERWISE LATER RUNNING ALT ROUTERS WILL HAVE
         * AN ADVANTAGE.
         *
         * Compared to {@link Evaluator#evaluateRouting}, this method evaluates routers probably
         * more resilient against temporary performance differences due to current hardware workloads.
         * This is done by letting all routers to compare calculate the same query one after each other
         * instead of processing all queries by the first router, then by the second and so on.
         *
         * @param routersToEvaluate A list of {@link Router Routers} that should be evaluated.
         * @param comparisonRouter  The router which acts as a comparison router, which means that its results
         *                          must be matched (concerning distance and negative routing results) by all routersToEvaluate
         *                          routers to be valid.
         */
        public void evaluateRoutingImproved(List<Router> routersToEvaluate, Router comparisonRouter) {
            chooseArbitraryTestNodes();

            List<List<RoutingResult>> allRoutingResults = new ArrayList<>();

            for(int i = 0; i < routersToEvaluate.size() + 1; i++) {
                allRoutingResults.add(new ArrayList<>());
            }

            for(int testQueryID = 0; testQueryID < this.randomTargetNodes.size(); testQueryID++) {
                int startNode = this.randomSourceNodes.get(testQueryID);
                int destNode = this.randomTargetNodes.get(testQueryID);

                allRoutingResults.get(0).add(comparisonRouter.route(startNode, destNode));
                for (int j = 1; j < routersToEvaluate.size() + 1; j++) {
                    allRoutingResults.get(j).add(routersToEvaluate.get(j-1).route(startNode, destNode));
                }
            }

            List<RoutingResult> comparisonRouterResults = allRoutingResults.remove(0);

            System.out.println("== RESULTS for comparison router: " + comparisonRouter.getName() + " router ==");
            System.out.println("Average running time: " + comparisonRouterResults.stream().mapToDouble(RoutingResult::getCalculationTimeInMs).average().getAsDouble() + " ms");
            System.out.println("Average number of nodes popped out of heap: " + comparisonRouterResults.stream().mapToInt(RoutingResult::getAmountOfNodesPoppedOutOfHeap).average().getAsDouble());

            for (int i = 0; i < routersToEvaluate.size(); i++) {
                Router r = routersToEvaluate.get(i);

                List<RoutingResult> results = new ArrayList<>();
                List<RoutingResult> nonSuccessfulResults = new ArrayList<>();

                for (int queryIdx = 0; queryIdx < randomSourceNodes.size(); queryIdx++) {
                    RoutingResult result = allRoutingResults.get(i).get(queryIdx);

                    results.add(result);
                    if (!result.isRouteFound()) {
                        nonSuccessfulResults.add(result);
                    }

                    if (!comparisonRouterResults.get(queryIdx).equals(result)) {

                        long distanceBidirectional = 0;
                        for (int currNodeIdx = 0; currNodeIdx < result.getPath().size() - 1; currNodeIdx++) {
                            int edgeID = Grid.getEdgeIDByNodeIDs(result.getPath().get(currNodeIdx), result.getPath().get(currNodeIdx + 1));
                            if (edgeID < 0) {
                                System.out.println("Keine edge zwischen " + result.getPath().get(currNodeIdx) + " und " + result.getPath().get(currNodeIdx + 1));
                                System.out.println("Koordinaten: " + Node.getLatitude(result.getPath().get(currNodeIdx)) + ", " + Node.getLongitude(result.getPath().get(currNodeIdx)) + " || "
                                        + Node.getLatitude(result.getPath().get(currNodeIdx + 1)) + ", " + Node.getLongitude(result.getPath().get(currNodeIdx + 1)));
                            } else {
                                distanceBidirectional += Edge.getDist(edgeID);
                            }
                        }
                        throw new IllegalStateException("Dijkstra and router " + r.getName() + " result do not match");
                    }
                }

                System.out.println("== RESULTS for " + r.getName() + " router ==");
                System.out.println("Average running time: " + results.stream().mapToDouble(RoutingResult::getCalculationTimeInMs).average().getAsDouble() + " ms");
                System.out.println("Average number of nodes popped out of heap: " + results.stream().mapToInt(RoutingResult::getAmountOfNodesPoppedOutOfHeap).average().getAsDouble());
                System.out.println("Negative Results: ");

                for (RoutingResult negativeResult : nonSuccessfulResults) {
                    System.out.println("Point A: " + negativeResult.getPathCoordinates().get(0).get(0) + ", " + negativeResult.getPathCoordinates().get(0).get(1) + "| Point B: " + negativeResult.getPathCoordinates().get(negativeResult.getPathCoordinates().size() - 1).get(0) + ", " + negativeResult.getPathCoordinates().get(negativeResult.getPathCoordinates().size() - 1).get(1));
                }

                System.out.println("=================================================");
            }
    }
}
