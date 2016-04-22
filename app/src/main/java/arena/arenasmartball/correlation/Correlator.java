package arena.arenasmartball.correlation;

import android.app.Activity;
import android.util.Log;

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

    private static NeuralNetwork nn_hardSoft;

    private static NeuralNetwork nn_hitDrop;

    /**
     * Skewness, Energy, Flatness, Spec Crest
     */
    private static final int[] HARD_SOFT_FEATURES = new int[] {4, 8, 20, 17};

    private static final float[] HARD_SOFT_FEATURE_SCALES = new float[] {1, 0.0001f, 1, 1};

    /**
     * Std Dev, ...
     */
    private static final int[] HIT_DROP_FEATURES = new int[] {0, 5, 6, 7, 10, 11, 12, 25, 17};

    private static final float[] HIT_DROP_FEATURE_SCALES = new float[] {1, 1, 1, 1, 1, 1, 1, 1, 1};


    private static final int NUM_HARD_SOFT_FEATURES = HARD_SOFT_FEATURES.length;
    private static final int NUM_HIT_DROP_FEATURES = HIT_DROP_FEATURES.length;

    private static final float[] SOFT = new float[] {0.001f};
    private static final float[] HARD = new float[] {0.999f};

    private static final float[] HIT = new float[] {0.001f};
    private static final float[] DROP = new float[] {0.999f};

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

    // Temporary input arrays
    private static final float[] TEMP_INPUTS_HARD_SOFT = new float[HARD_SOFT_FEATURES.length];
    private static final float[] TEMP_INPUTS_HIT_DROP = new float[HIT_DROP_FEATURES.length];

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
        InputStream in =  activity.getResources().openRawResource(R.raw.nn_hardsoft);
        nn_hardSoft = NetworkUtil.createFromInputStream(in);

        in =  activity.getResources().openRawResource(R.raw.nn_hitdrop);
        nn_hitDrop = NetworkUtil.createFromInputStream(in);
    }

    /**
     * Calculates the value from the given FeatureSet using the MLR coefficients of this class.
     * @param featureSet The feature set to evaluate
     * @return The calculated value
     */
    public static double evaluate(FeatureSet featureSet)
    {
        ArrayList<Double> features = featureSet.getFeatureArray();

//        // TODO print feature values
//        for (int i = 0; i < features.size(); ++i)
//            System.out.println("\t" + FEATURES_TO_USE[i].NAME + "\t" + features.get(i));

        // Get inputs
        for (int i = 0; i < HARD_SOFT_FEATURES.length; ++i)
        {
            TEMP_INPUTS_HARD_SOFT[i] = features.get(HARD_SOFT_FEATURES[i]).floatValue() * HARD_SOFT_FEATURE_SCALES[i];
        }

        for (int i = 0; i < HIT_DROP_FEATURES.length; ++i)
        {
            TEMP_INPUTS_HIT_DROP[i] = features.get(HIT_DROP_FEATURES[i]).floatValue() * HIT_DROP_FEATURE_SCALES[i];
        }


        // Calculate Impact value (0, 1)
        float[] hitDropValue = nn_hitDrop.evaluate(TEMP_INPUTS_HIT_DROP);
        float[] hardSoftValue = nn_hardSoft.evaluate(TEMP_INPUTS_HARD_SOFT);

        Log.w(TAG, "Hit/Drop Value = " + hitDropValue[0]);
        Log.w(TAG, "Hard/Soft = " + hardSoftValue[0]);

        return hardSoftValue[0];
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
}
