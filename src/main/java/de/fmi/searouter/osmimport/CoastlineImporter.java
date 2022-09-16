package de.fmi.searouter.osmimport;

import com.wolt.osm.parallelpbf.ParallelBinaryParser;
import com.wolt.osm.parallelpbf.entity.Header;
import de.fmi.searouter.importdata.CoastlineWay;
import de.fmi.searouter.importdata.Point;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.util.*;

/**
 * Provides means to import a PBF file (https://wiki.openstreetmap.org/wiki/PBF_Format)
 * containing coastline information.
 */
public class CoastlineImporter {

    /**
     * The imported coastlineway
     */
    private final List<CoastlineWay> coastLineWays;

    /**
     * Node IDs need to be mapped to Points as in the pbf file coastlines and nodes are defined separately and
     * the reference has to be resolved in order to get the node information.
     */
    private Map<Long, Point> allNodes;

    public CoastlineImporter() {
        this.allNodes = new HashMap<>();
        this.coastLineWays = new ArrayList<>();
    }

    /**
     * Imports osm coastlines from a given pbf file.
     *
     * @param pbfCoastlineFilePath The path of the pbf file as a string and relative to the resource directory.
     * @throws FileNotFoundException If under the passed path name no file can be found.
     */
    public List<CoastlineWay> importPBF(String pbfCoastlineFilePath) throws IOException {
        this.allNodes = new HashMap<>();

        // Get an input stream for the pbf file located in the resources directory
        Resource pbfResource = new ClassPathResource(pbfCoastlineFilePath);
        InputStream inputStream = pbfResource.getInputStream();

        // Start import
        new ParallelBinaryParser(inputStream, 1)
                .onHeader(this::processHeader)
                .onNode(this::processNode)
                .onWay(this::processWay)
                .onComplete(this::onCompletion)
                .parse();

        return this.coastLineWays;
    }

    /**
     * @param way the Way to check whether it is a coast line
     * @return True if a way is a coastline as defined by OSM.
     */
    private static boolean isCoastlineEntity(com.wolt.osm.parallelpbf.entity.Way way) {
        // bogus is a special tag for additional edges that are used to make the antarctica a polygon.
        // However this coastline does not exist in real and is only added to OSM data sets for enabling certain
        // algorithms. We explicitly exclude such bogus coastlines.
        return "coastline".equals(way.getTags().get("natural")) && way.getNodes().size() > 0 &&
                !"bogus".equals(way.getTags().get("coastline"));
    }

    /**
     * Things to do when the import finished.
     */
    private void onCompletion() {
        // Empty the nodes list to save memory
        this.allNodes = null;
    }

    /**
     * Handles an osm way when finding one during the pbf import.
     *
     * @param way The osm way
     */
    private void processWay(com.wolt.osm.parallelpbf.entity.Way way) {
        if (isCoastlineEntity(way)) {
            CoastlineWay cWay = new CoastlineWay();
            for (int i = 0; i < way.getNodes().size(); i++) {
                Point node = this.allNodes.get(way.getNodes().get(i));
                if (node != null) {
                    cWay.getPoints().add(node);
                }
            }
            this.coastLineWays.add(cWay);
        }

    }

    /**
     * Processes nodes during the PBF file import.
     *
     * @param node The node to process
     */
    private void processNode(com.wolt.osm.parallelpbf.entity.Node node) {
        allNodes.put(node.getId(), new Point(node.getId(), (float) node.getLat(), (float) node.getLon()));
    }

    private void processHeader(Header header) {
        // We are not interested in the header
    }

}