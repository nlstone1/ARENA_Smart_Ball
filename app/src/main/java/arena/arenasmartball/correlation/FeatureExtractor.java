package arena.arenasmartball.correlation;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.summary.Sum;

/**
 * Class to handle feature extraction from ImpactData acceleration time series.
 * @author Theodore Stone
 */

public class FeatureExtractor 
{
	/** The tag for this class. */
	public static final String TAG = "FeatureExtractor";
	
	/**
	 * Static class.
	 */
	private FeatureExtractor()
	{	}
	
	/**
	 * Calculates the values of the given Features for the given PredefinedFeaturable, putting the results in the given FeatureSet.
	 * @param object The PredefinedFeaturable to use
	 * @param featureSet The feature set
	 */
	public static void getFeatureValues(PredefinedFeaturable object, FeatureSet featureSet)
	{
		for (ConstantFeature f: object.getFeatures())
			featureSet.put(f.NAME, f.VALUE);
	}
	
	/**
	 * Calculates the values of the given Features for the given DataSeriesFeaturable.
	 * Feature values for multiple axes are averaged together.
	 * @param object The DataSeriesFeaturable to use
	 * @param features The Features to use
	 * @return The feature set
	 */
	public static FeatureSet getFeatureValues(DataSeriesFeaturable object, Feature[] features)
	{
		FeatureSet set = new FeatureSet();
		getFeatureValues(object, features, set);
		return set;
	}
	
	/**
	 * Calculates the values of the given Features for the given DataSeriesFeaturable, putting the results in the given FeatureSet.
	 * Feature values for multiple axes are averaged together.
	 * @param object The DataSeriesFeaturable to use
	 * @param features The Features to use
	 * @param featureSet The feature set
	 */
	public static void getFeatureValues(DataSeriesFeaturable object, Feature[] features, FeatureSet featureSet)
	{
		SensorData[] axes = object.getAxes();
		double[] safs = new double[axes.length];
		double[] dafs = axes.length > 1 ? new double[axes.length * (axes.length - 1) / 2] : null;
		
		// Calculate features
		double value;
		SingleAxisFeature saf;
		DoubleAxisFeature daf;
		Mean mean = new Mean();
		for (Feature feature: features)
		{
			if (feature instanceof SingleAxisFeature)
			{
				saf = (SingleAxisFeature)feature;
				
				for (int i = 0; i < axes.length; ++i)
					safs[i] = Math.abs(saf.calculate(axes[i])); 
				
				value = mean.evaluate(safs); 
			}
			else if (feature instanceof DoubleAxisFeature)
			{
				if (dafs != null)
				{
					daf = (DoubleAxisFeature)feature;
					
					for (int i = 0; i < axes.length - 1; ++i)
					{
						for (int j = i + 1; j < axes.length; ++j)
						{
							dafs[i + j - 1] = Math.abs(daf.calculate(axes[i], axes[j])); 
						}
					}
					
					value = mean.evaluate(dafs); 
				}
				else
					value = 0.0f;
			}
			else
				value = 0.0f;
		
			// Put value in feature set
			featureSet.put(feature.NAME, value);
		}
	}
	
	/**
	 * Calculates the energy of the fft, where energy is the sum of the square discrete fft component magnitudes.
	 * @param dft The DFT
	 * @return The energy of the given plot in the frequency domain
	 */
	public static double calculateEnergy(DFT dft)
	{
		double energy = 0.0;
		double fftMag;
		
		for (int i = 0; i < dft.length; ++i)
		{
			fftMag = Math.pow(dft.mags[i], 2) + Math.pow(dft.freqs[i], 2);
			energy += fftMag;
		}
		
		return energy;
	}
	
	/**
	 * Calculates the average deviation of the given data series.
	 * @param data The data series
	 * @return The average deviation of the data
	 */
	public static double averageDeviation(double[] data)
	{
		double avgDev = 0.0;
		double avg = new Sum().evaluate(data) / data.length;
		
		for (int i = 0; i < data.length; ++i)
			avgDev += Math.abs(data[i] - avg);
		
		return avgDev / data.length;
	}
	
	/**
	 * Calculates the rms amplitude of the given data series.
	 * @param data The data series
	 * @return The rms amplitude of the data
	 */
	public static double rmsAmplitude(double[] data)
	{
		double sum = 0.0;
		
		for (int i = 0; i < data.length; ++i)
			sum += Math.pow(data[i], 2.0);
		
		return Math.sqrt(sum / data.length);
	}
	
	/**
	 * Gets the maximum value in the given samples.
	 * @param samples The sample array
	 * @return The maximum value in the given samples
	 */
	public static double max(double[] samples)
	{
		double max = Double.MIN_VALUE;
		
		for (int i = 0; i < samples.length; ++ i)
		{
			if (samples[i] > max)
				max = samples[i];
		}
		return max;
	}
	
	/**
	 * Gets the minimum value in the given samples.
	 * @param samples The sample array
	 * @return The minimum value in the given samples
	 */
	public static double min(double[] samples)
	{
		double min = Double.MAX_VALUE;
		
		for (int i = 0; i < samples.length; ++ i)
		{
			if (samples[i] < min)
				min = samples[i];
		}
		return min;
	}
	
	/**
	 * Interface for objects that will have a predefined set of Features calculated.
	 * @author Theodore Stone
	 */
	public interface PredefinedFeaturable
	{
		/**
		 * Gets an array of Features for this Featurable.
		 * @return An array of Features for this Featurable
		 */
		ConstantFeature[] getFeatures();
	}
	
	/**
	 * Interface for objects that will have Features calculated from a data series.
	 * @author Theodore Stone
	 */
	public interface DataSeriesFeaturable
	{
		/**
		 * Gets an array of SensorData objects for each axis of data of this Featurable.
		 * @return An array of SensorData objects for each axis of data of this Featurable
		 */
		SensorData[] getAxes();
	}
	
	/**
	 * Class representing a single feature.
	 * @author Theodore Stone
	 */
	public static class Feature
	{
		/** The name of the Feature. */
		public final String NAME;
		
		/**
		 * Constructs a new Feature.
		 * @param name The name of the Feature
		 */
		private Feature(String name)
		{
			this.NAME = name;
		}
		
		/**
		 * Gets a String representation of this Feature.
		 * @return A String representation of this Feature
		 */ 
		@Override
		public String toString()
		{
			return NAME;
		}
	}
	
	/**
	 * Class representing a single feature with a constant value.
	 * @author Theodore Stone
	 */
	public static class ConstantFeature extends Feature
	{
		/** The value of this ConstantFeature. */
		public final double VALUE;
		
		/**
		 * Constructs a new ConstantFeature.
		 * @param name The name of the Feature
		 * @param value The value of the Feature
		 */
		public ConstantFeature(String name, double value)
		{
			super (name);
			VALUE = value;
		}
		
		/**
		 * Constructs a new ConstantFeature using the name of the given Feature.
		 * @param feature The Feature whose name to use
		 * @param value The value of the Feature
		 */
		public ConstantFeature(Feature feature, double value)
		{
			super (feature.NAME);
			VALUE = value;
		}
	}
	
	/**
	 * Class representing a feature calculated with data from one axis.
	 * @author Theodore Stone
	 */
	public static abstract class SingleAxisFeature extends Feature
	{
		/**
		 * Constructs a new SingleAxisFeature. 
		 * @param name The name of the Feature
		 */
		public SingleAxisFeature(String name)
		{
			super (name);
		}
		
		/** 
		 * Implement to calculate the value of this Feature for the given SensorData.
		 * @param data The SensorData for which to calculate the feature value
		 * @return The value of this Feature for the given data
		 */
		public abstract double calculate(SensorData data);
	}
	
	/**
	 * Class representing a feature calculated with data from two axes.
	 * @author Theodore Stone
	 */
	public static abstract class DoubleAxisFeature extends Feature
	{
		/**
		 * Constructs a new DoubleAxisFeature. 
		 * @param name The name of the Feature
		 */
		public DoubleAxisFeature(String name)
		{
			super (name);
		}
		
		/** 
		 * Implement to calculate the value of this Feature for the two given SensorDatas.
		 * @param data1 The first SensorData to use to calculate the feature value
		 * @param data2 The second SensorData to use to calculate the feature value
		 * @return The value of this Feature for the given data
		 */
		public abstract double calculate(SensorData data1, SensorData data2);
	}
}
