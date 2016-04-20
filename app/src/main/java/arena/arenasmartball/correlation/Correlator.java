package arena.arenasmartball.correlation;

import java.util.ArrayList;

/**
 * Class for calculating force from data features, using coefficients from an offline MLR.
 * Created by Theodore on 4/17/2016.
 */
public class Correlator
{
    // The tag for this class
    private static final String TAG = "Correlator";

    // The MLR coefficients, calculated offline
    private static final double[] COEFFICIENTS =
            new double[]{-2980.95850432648, 1461.8982554902464, 173.37314544160128, -2418.0521989188455,
                         -399.2262087333679, 187.0622012502032, 54.859923289910135, 0.16875243713025267,
                          352.4888589144222, -0.015857487780531386, -0.0024783405319758404, -2714.053825149518,
                          2779.021009588462, -2.205621071881506E-5, -883.5685037159376, 859.114476160566,
                          0.27568724869874084, -3.530260583361065, -39.33852881626251, -1322.9362407109838,
                          190.6937924720649, 1681.7055578548873, 413.1189600248107, 11947.67931809564,
                          -12382.14837403676, -63.35759838490164, 2663.201496130363};

    /** The Features to use */
    public static final FeatureExtractor.Feature[] FEATURES_TO_USE = new FeatureExtractor.Feature[]
    {
        Features.AVERAGE,
        Features.KURT,
        Features.MAX,
        Features.MIN,
        Features.SKEW,
        Features.STD_DEV,
        Features.AVG_DEV,
        Features.RMS_AMPLITUDE,
        Features.ENERGY,
        Features.PCOR,
        Features.SCOR,
        Features.KCOR,
        Features.COV,
        Features.SPEC_STD_DEV,
        Features.SPEC_CENTROID,
        Features.SPEC_SKEWNESS,
        Features.SPEC_KURTOSIS,
        Features.SPEC_CREST,
        Features.IRREG_K,
        Features.IRREG_J,
        Features.FLATNESS,
        Features.SMOOTHNESS,
        Features.RMSSD,
        Features.MeanFirstDifferences,
        Features.MeanSecondDifferences,
        Features.ZeroCrossingRate
    };

    /**
     * Static class.
     */
    private Correlator()
    {   }

    /**
     * Calculates the value from the given FeatureSet using the MLR coefficients of this class.
     * @param featureSet The feature set to evaluate
     * @return The calculated value
     */
    public static double evaluate(FeatureSet featureSet)
    {
        ArrayList<Double> features = featureSet.getFeatureArray();

        if (features.size() != (COEFFICIENTS.length - 1))
            throw new IllegalArgumentException("Feature length must match beta length (excluding beta[0])");

        double force = COEFFICIENTS[0];

        for (int i = 0; i < features.size(); ++i)
            force += features.get(i) * COEFFICIENTS[i + 1];

        return force;
    }

    /**
     * Calculates the force for the given DataSeriesFeaturable.
     * @param dataSeriesFeaturable The DataSeriesFeaturable
     * @return The calculated force
     */
    public static double evaluate(FeatureExtractor.DataSeriesFeaturable dataSeriesFeaturable)
    {
        return evaluate(FeatureExtractor.getFeatureValues(dataSeriesFeaturable, FEATURES_TO_USE));
    }

//    /**
//     * Calculates the value from the given feature array using the MLR coefficients of this class.
//     * @param features The feature array
//     * @return The calculated value
//     */
//    public static double evaluate(double[] features)
//    {
//        if (features.length != (COEFFICIENTS.length - 1))
//            throw new IllegalArgumentException("Feature length must match beta length (excluding beta[0])");
//
//        double force = COEFFICIENTS[0];
//
//        for (int i = 0; i < features.length; ++i)
//            force += features[i] * COEFFICIENTS[i + 1];
//
//        return force;
//    }

}
