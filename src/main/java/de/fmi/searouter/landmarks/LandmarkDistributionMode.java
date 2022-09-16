package de.fmi.searouter.landmarks;

import de.fmi.searouter.landmarks.initializer.*;

/**
 * Enum type indicating which landmark distribution strategy should be chosen.
 */
public enum LandmarkDistributionMode {
    RANDOM(RandomLandmarkInitializer.class, "landmarks_random_mode"),
    EQUAL_2D(EqualDistributed2DMapInitializer.class, "landmarks_equal_distributed_2d_map"),
    EQUAL_SPHERE(EqualDistributedOnSphereInitializer.class, "landmarks_equal_distributed_sphere"),
    COASTLINE(CoastlineLandmarkInitializer.class, "landmarks_equal_distributed_coastline"),
    MAX_AVOID(MaxAvoidInitializer.class, "landmarks_max_avoid");

    private final Class<? extends LandmarkInitializer> initClass;
    private final String serFileName;

    private LandmarkDistributionMode(Class<? extends LandmarkInitializer> initClass, String serFileName) {
        this.initClass = initClass;
        this.serFileName = serFileName;
    }

    public Class<? extends LandmarkInitializer> getInitializerClass(){
        return this.initClass;
    }

    /**
     * @return Name of the serialization file name of the distribution strateg.
     */
    public String getSerFileName() {
        return this.serFileName;
    }
}
