package de.fmi.searouter.utils;

import de.fmi.searouter.coastlinegrid.CoastlineWays;
import de.fmi.searouter.importdata.CoastlineWay;
import de.fmi.searouter.importdata.Point;
import de.fmi.searouter.dijkstragrid.GridNode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import java.util.List;

/**
 * Provides methods to parse osm entities to GeoJSON for debugging and test purposes.
 */
public class GeoJsonConverter {

    /**
     * Parses a list of {@link CoastlineWay}s to a GeoJSON object of the
     * type "FeatureCollection".
     *
     * @return The GeoJSON as FeatureCollection type
     */
    public static JSONObject coastlineWaysToGeoJSON() {
        // Outer FeatureCollection object (top-level obj)
        JSONObject featureCollection = new JSONObject();
        featureCollection.put("type", "FeatureCollection");

        // Features, each representing one LineString (one Way of the coast)
        JSONArray features = new JSONArray();
        for (int i = 0; i < 10000; i++) {
            features.put(GeoJsonConverter.edgeToJSON(CoastlineWays.getStartLatByEdgeIdx(i), CoastlineWays.getStartLonByEdgeIdx(i), CoastlineWays.getDestLatByEdgeIdx(i), CoastlineWays.getDestLonByEdgeIdx(i)));
        }

        featureCollection.put("features", features);

        return featureCollection;
    }

    /**
     * Parses a {@link Way} object to a GeoJSON representation using the type "Feature".
     *
     * @return
     */
    public static JSONObject edgeToJSON(double lat1, double lon1, double lat2, double lon2) {
        // Build an json array of coordinate pairs (longitude-latitude pairs)
        JSONArray longLatArray = new JSONArray();

        JSONArray longLatPair = new JSONArray()
                .put(lon1)
                .put(lat1);
        longLatArray.put(longLatPair);

        JSONArray longLatPair2 = new JSONArray()
                .put(lon2)
                .put(lat2);
        longLatArray.put(longLatPair2);


        // Geometry json obj inside  a feature obj
        JSONObject geometry = new JSONObject()
                .put("type", "LineString")
                .put("coordinates", longLatArray);

        // Outer feature obj
        JSONObject feature = new JSONObject()
                .put("type", "Feature")
                .put("properties", new JSONObject())
                .put("geometry", geometry);

        return feature;
    }

    /**
     * Parses a list of {@link CoastlineWay}s to a GeoJSON object of the
     * type "FeatureCollection".
     *
     * @param waysToConvert The {@link CoastlineWay}s
     * @return The GeoJSON as FeatureCollection type
     */
    public static JSONObject coastlineWayToGeoJSON(List<CoastlineWay> waysToConvert) {
        // Outer FeatureCollection object (top-level obj)
        JSONObject featureCollection = new JSONObject();
        featureCollection.put("type", "FeatureCollection");

        // Features, each representing one LineString (one Way of the coast)
        JSONArray features = new JSONArray();
        for (CoastlineWay currWay : waysToConvert) {
            features.put(GeoJsonConverter.osmWayToGeoJSON(currWay));
        }

        featureCollection.put("features", features);

        return featureCollection;
    }

    /**
     * Returns the GeoJSON representation of multiple {@link GridNode GridNodes}.
     *
     * @param nodes The GridNodes to get the GeoJSON representation of.
     * @return A {@link JSONObject} representing the GridNodes.
     */
    public static JSONObject osmNodesToGeoJSON(List<GridNode> nodes, List<String> colors, List<String> addProp) {
        // Outer FeatureCollection object (top-level obj)
        JSONObject featureCollection = new JSONObject();
        featureCollection.put("type", "FeatureCollection");

        // Features, each representing one LineString (one Way of the coast)
        JSONArray features = new JSONArray();
        for (int i = 0; i < nodes.size(); i++) {
            features.put(GeoJsonConverter.gridNodeToGeoJSONFeature(nodes.get(i), colors.get(i), addProp.get(i)));
        }

        featureCollection.put("features", features);

        return featureCollection;
    }

    /**
     * Returns the GeoJSON representation of one {@link GridNode}.
     *
     * @param node The GridNode to get the GeoJSON feature representation of.
     * @return A JSONObject representing the GridNdde in GeoJSON.
     */
    public static JSONObject gridNodeToGeoJSONFeature(GridNode node, String hexColor, String addProp) {
        JSONObject topLevelobj = new JSONObject();

        topLevelobj.put("type", "Feature");

        JSONObject properties = new JSONObject();
        if (hexColor != null && !hexColor.equals("")) {
            properties.put("marker-color", hexColor);
            properties.put("marker-size", "small");
            properties.put("marker-symbol", "");
            properties.put("add-prop", addProp);
        }

        topLevelobj.put("properties", properties);

        JSONObject geometry = new JSONObject();
        geometry.put("type", "Point");
        JSONArray arr = new JSONArray();
        arr.put(node.getLongitude());
        arr.put(node.getLatitude());

        geometry.put("coordinates", arr);

        topLevelobj.put("geometry", geometry);

        return topLevelobj;
    }

    /**
     * Parses a {@link Way} object to a GeoJSON representation using the type "Feature".
     *
     * @param wayToConvert The {@link Way} which should be parsed as GeoJSON of the type "Feature"
     * @return
     */
    public static JSONObject osmWayToGeoJSON(CoastlineWay wayToConvert) {
        List<Point> wayNodes = wayToConvert.getPoints();
        // Build an json array of coordinate pairs (longitude-latitude pairs)
        JSONArray longLatArray = new JSONArray();

        for (int i = 0; i < wayToConvert.getNumberOfPoints(); i++) {
            JSONArray longLatPair = new JSONArray()
                    .put(wayNodes.get(i).getLon())
                    .put(wayNodes.get(i).getLat());
            longLatArray.put(longLatPair);
        }

        // Geometry json obj inside  a feature obj
        JSONObject geometry = new JSONObject()
                .put("type", "LineString")
                .put("coordinates", longLatArray);

        // Outer feature obj
        JSONObject feature = new JSONObject()
                .put("type", "Feature")
                .put("properties", new JSONObject())
                .put("geometry", geometry);

        return feature;
    }


}
