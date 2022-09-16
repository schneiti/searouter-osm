package de.fmi.searouter.coastlinegrid;

import de.fmi.searouter.importdata.CoastlineWay;
import de.fmi.searouter.importdata.Point;
import de.fmi.searouter.osmimport.CoastlineImporter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CoastlineWays stores all OSM information about every coastlines on earth. This includes coastline edges, their
 * vertices and coordinate positions.
 */
public class CoastlineWays {

    /**
     * The name of the serialization file which is used for storing data stored in this class.
     */
    public static final String COASTLINE_WAYS_SERIALIZATION_FILE_NAME = "CoastlineWays.ser";

    /**
     * Each entry represents one edge of a coastline. The index within this array is the ID of this
     * edge. The stored value is the offset where the coordinate information of the start point of
     * this edge can be found in the {@link #pointLat} and {@link #pointLon} array. The coordinate
     * information of the destination point is stored at index offset+1 in those arrays.
     */
    private static int[] edgePosStart;

    /**
     * In these arrays, the positions of start and end points of coastline edges is stored.
     */
    private static float[] pointLon;
    private static float[] pointLat;

    /**
     * The name of the pbf file containing the coastline information from OSM.
     */
    private static final String PBF_FILE_PATH = "planet-coastlinespbf-cleaned.pbf";

    /**
     * Initialize the coastline ways. If no file with the serialized data is present,
     * the PBF file will be read and used to create the data structures. In this case,
     * a serialization file will then be saved in order to speed up later calls of this method.
     */
    public static void initCoastlineWays() {
        // Serialization file for the CoastlineWays (objects containing the relevant imported coastline data
        // of PBF files)
        File serializationFile = new File(COASTLINE_WAYS_SERIALIZATION_FILE_NAME);

        if (!serializationFile.exists()) {
            // Import coastlines from the PBF file if no serialization file for the coastlines exists already
            CoastlineImporter importer = new CoastlineImporter();
            List<CoastlineWay> coastlines = new ArrayList<>();

            // Perform the PBF file import
            try {
                coastlines = importer.importPBF(PBF_FILE_PATH);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Initialize a more efficient data structure for storing the coastlines. This data structure is used
            // later on for the implementation of the multi-level grid used for the point-in-water check.
            initEdges(coastlines);

            // This is thought of to be a signal to the garbage collector to remove objects that are
            // not needed anymore.
            coastlines = null;

            // Serialization
            storeData();
        } else {
            // Deserialization
            getData();
        }
    }

    /**
     * Writes all information stored in this class to disk using Javas in-built serialization mechanism.
     * The serialization file will be created at the top-level of this project with
     * the name {@link #COASTLINE_WAYS_SERIALIZATION_FILE_NAME}.
     *
     * The data can again be deserialized using {@link #getData()}.
     */
    private static void storeData() {
        CoastlineWriter writer = new CoastlineWriter(edgePosStart, pointLat, pointLon);

        // Serialization
        try {
            //Saving of object in a file
            FileOutputStream file = new FileOutputStream(COASTLINE_WAYS_SERIALIZATION_FILE_NAME);
            ObjectOutputStream out = new ObjectOutputStream(file);

            // Method for serialization of object
            out.writeObject(writer);

            out.close();
            file.close();

            System.out.println("Coastline ways have been serialized");

        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Deserializes previously serialized information (via {@link #storeData()}) of a CoastlineChecker
     * and fills up the data structures of this CoastlineChecker accordingly.
     */
    private static void getData() {
        try {
            // Reading the object from a file
            FileInputStream file = new FileInputStream(COASTLINE_WAYS_SERIALIZATION_FILE_NAME);
            ObjectInputStream in = new ObjectInputStream(file);

            // Method for deserialization of object
            CoastlineWriter writer = (CoastlineWriter) in.readObject();

            in.close();
            file.close();

            System.out.println("Coastline ways have been deserialized ");
            edgePosStart = writer.getEdgePosStart();
            pointLat = writer.getPointLat();
            pointLon = writer.getPointLon();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Setup the data structures of this class using OSM coastline information based on
     * {@link CoastlineWay} objects.
     *
     * @param importedCoastlines A list of {@link CoastlineWay} objects whose contained data should
     *                           be included in the more efficient data structure of this {@link CoastlineWays}
     *                           class.
     */
    public static void initEdges(List<CoastlineWay> importedCoastlines) {

        // Find out the needed size of arrays for storing all edges of CoastlineWays
        int numberOfEdges = 0;
        int numberOfCoordinates = 0;
        for (CoastlineWay currCoastline : importedCoastlines) {
            numberOfEdges += currCoastline.getNumberOfEdges();
            numberOfCoordinates += currCoastline.getNumberOfPoints();
        }

        // Init arrays representing all coastline edges
        edgePosStart = new int[numberOfEdges];
        pointLon = new float[numberOfCoordinates];
        pointLat = new float[numberOfCoordinates];

        // Counter for the edge index
        int nextEdgeIdx = 0;
        // Counter for the coordinate point index
        int nextCoordIdx = 0;

        // Add all edges of each CoastlineWay to the coastline edge arrays storing all edges on earth
        for (CoastlineWay currCoastlineWay : importedCoastlines) {
            // All points/nodes that make up one CoastlineWay
            List<Point> currPoints = currCoastlineWay.getPoints();

            // Ignore empty CoastlineWays
            if (currPoints.size() <= 1) {
                continue;
            }

            // Iterate over all points/nodes in a CoastlineWay and add adjacent pairs of them as edges to
            // the CoastlineWays arrays
            int currPointsSize = currPoints.size();
            for (int pointIdx = 0; pointIdx < currPointsSize; pointIdx++) {
                Point currPoint = currPoints.get(pointIdx);

                pointLon[nextCoordIdx] = currPoint.getLon();
                pointLat[nextCoordIdx] = currPoint.getLat();

                if (pointIdx == currPointsSize - 1) {
                    // Last point of a CoastlineWay. This point is only a destination point
                    // of an edge, therefore no increase of nextEdgeIdx is performed.
                    nextCoordIdx++;
                } else {
                    edgePosStart[nextEdgeIdx] = nextCoordIdx;
                    nextCoordIdx++;
                    nextEdgeIdx++;
                }

            }
        }
    }


    public static int getNumberOfEdges() {
        return edgePosStart.length;
    }

    // All functions below return based on the position stored for the edge-ID (which is the index in edgePosStart)
    public static float getStartLatByEdgeIdx(int edgeIdx) {
        return pointLat[edgePosStart[edgeIdx]];
    }

    public static float getDestLatByEdgeIdx(int edgeIdx) {
        return pointLat[edgePosStart[edgeIdx] + 1];
    }

    public static float getStartLonByEdgeIdx(int edgeIdx) {
        return pointLon[edgePosStart[edgeIdx]];
    }

    public static float getDestLonByEdgeIdx(int edgeIdx) {
        return pointLon[edgePosStart[edgeIdx] + 1];
    }

}
