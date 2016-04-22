package arena.arenasmartball.correlation;

import android.app.Activity;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import arena.arenasmartball.R;
import neuralNetwork.NetworkUtil;
import neuralNetwork.NeuralNetwork;

/**
 * Class for calculating force from data features, using coefficients from an offline MLR.
 * Created by Theodore on 4/17/2016.
 */
public class Correlator
{
    // The tag for this class
    private static final String TAG = "Correlator";

//    // The MLR coefficients, calculated offline
//    private static final double[] COEFFICIENTS =
//            new double[]{-2980.95850432648, 1461.8982554902464, 173.37314544160128, -2418.0521989188455,
//                         -399.2262087333679, 187.0622012502032, 54.859923289910135, 0.16875243713025267,
//                          352.4888589144222, -0.015857487780531386, -0.0024783405319758404, -2714.053825149518,
//                          2779.021009588462, -2.205621071881506E-5, -883.5685037159376, 859.114476160566,
//                          0.27568724869874084, -3.530260583361065, -39.33852881626251, -1322.9362407109838,
//                          190.6937924720649, 1681.7055578548873, 413.1189600248107, 11947.67931809564,
//                          -12382.14837403676, -63.35759838490164, 2663.201496130363};

//    private static final double[] COEFFICIENTS =
//            new double[]{686.6106849365942, 387.9860020594098, -1223.7015696256153, -149.09388881861926, 2.1772531007981435, 1240.453533056784, -90.4301873474037};

    // The NeuralNetwork used to identify hits
    private static NeuralNetwork neuralNetwork;

    // The scales to apply to the NeuralNetwork Inputs
    private static final String[] NN_INPUT_FEATURE_NAMES = new String[] {Features.ENERGY.NAME, Features.IRREG_K.NAME};

    // The scales to apply to the NeuralNetwork Inputs
    private static final float[] NN_INPUT_SCALES = new float[] {0.00001f, 0.001f};

    // Temporary input array
    private static final float[] TEMP_INPUTS = new float[NN_INPUT_SCALES.length];

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
     * Initializes this Correlator.
     * @param activity The current Activity
     */
    public static void initialize(Activity activity)
    {
        InputStream in =  activity.getResources().openRawResource(R.raw.sbnn);
        neuralNetwork = NetworkUtil.createFromInputStream(in);
    }

    /**
     * Calculates the value from the given FeatureSet using the MLR coefficients of this class.
     * @param featureSet The feature set to evaluate
     * @return The calculated value
     */
    public static double evaluate(FeatureSet featureSet)
    {
        ArrayList<Double> features = featureSet.getFeatureArray();

        // TODO print feature values
        for (int i = 0; i < features.size(); ++i)
            System.out.println("\t" + FEATURES_TO_USE[i].NAME + "\t" + features.get(i));

        // Get inputs
        for (int i = 0; i < NN_INPUT_FEATURE_NAMES.length; ++i)
            TEMP_INPUTS[i] = (float)(featureSet.get(NN_INPUT_FEATURE_NAMES[i])) * NN_INPUT_SCALES[i];

        // Calculate Impact value (0, 1)
        float[] value = neuralNetwork.evaluate(TEMP_INPUTS);

        return value[0];

//
////        return 40.0 + Math.random() * 30.0;
//
//        if (features.size() != (COEFFICIENTS.length - 1))
//            throw new IllegalArgumentException("Feature length must match beta length (excluding beta[0])");
//
//        double force = COEFFICIENTS[0];
//
//        for (int i = 0; i < features.size(); ++i)
//            force += features.get(i) * COEFFICIENTS[i + 1];
//
//        return force;
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
