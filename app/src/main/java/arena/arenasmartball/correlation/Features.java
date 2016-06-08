package arena.arenasmartball.correlation;

import java.util.ArrayList;

import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.summary.Sum;

import arena.arenasmartball.correlation.FeatureExtractor.Feature;
import arena.arenasmartball.correlation.FeatureExtractor.SingleAxisFeature;


/**
 * Class to hold Feature definitions.
 * @author Theodore Stone
 */

public class Features 
{
	/** Static class */
	private Features()
	{	}
	
	public static final Feature AVERAGE = new FeatureExtractor.SingleAxisFeature("Avg")
	{
		@Override
		public double calculate(SensorData data)
		{
			return new Mean().evaluate(data.timeSeries()); 
		}
	};
	
	public static final Feature STD_DEV = new FeatureExtractor.SingleAxisFeature("Std_Dev")
	{
		@Override
		public double calculate(SensorData data)
		{
			return new StandardDeviation().evaluate(data.timeSeries());
		}
	};
	
	public static final Feature AVG_DEV = new FeatureExtractor.SingleAxisFeature("Avg_Dev")
	{
		@Override
		public double calculate(SensorData data)
		{
			return FeatureExtractor.averageDeviation(data.timeSeries()); 
		}
	};
	
	public static final Feature RMS_AMPLITUDE = new FeatureExtractor.SingleAxisFeature("RMS_Amp")
	{
		@Override
		public double calculate(SensorData data)
		{
			return FeatureExtractor.rmsAmplitude(data.timeSeries()); 
		}
	};
	
	public static final Feature MAX = new FeatureExtractor.SingleAxisFeature("Max")
	{
		@Override
		public double calculate(SensorData data)
		{
			return FeatureExtractor.max(data.timeSeries());
		}
	};
	
	public static final Feature MIN = new FeatureExtractor.SingleAxisFeature("Min")
	{
		@Override
		public double calculate(SensorData data)
		{
			return FeatureExtractor.min(data.timeSeries());
		}
	};

	public static final Feature SKEW = new FeatureExtractor.SingleAxisFeature("Skew")
	{
		@Override
		public double calculate(SensorData data)
		{
			return new Skewness().evaluate(data.timeSeries());
		}
	};
	
	public static final Feature KURT = new FeatureExtractor.SingleAxisFeature("Kurt")
	{
		@Override
		public double calculate(SensorData data)
		{
			return new Kurtosis().evaluate(data.timeSeries());
		}
	};
	
	public static final Feature ENERGY = new SingleAxisFeature("Energy")
	{
		@Override
		public double calculate(SensorData data)
		{
			return FeatureExtractor.calculateEnergy(data.frequencySeries());
		}
	};
	
	public static final Feature PCOR = new FeatureExtractor.DoubleAxisFeature("PCor")
	{
		@Override
		public double calculate(SensorData data1, SensorData data2) 
		{
			return new PearsonsCorrelation().correlation(data1.timeSeries(), data2.timeSeries());  
		}
	};
	
	public static final Feature SCOR = new FeatureExtractor.DoubleAxisFeature("SCor")
	{
		@Override
		public double calculate(SensorData data1, SensorData data2) 
		{
			return new SpearmansCorrelation().correlation(data1.timeSeries(), data2.timeSeries()); 
		}
	};
	
	
	public static final Feature KCOR = new FeatureExtractor.DoubleAxisFeature("KCor")
	{
		@Override
		public double calculate(SensorData data1, SensorData data2) 
		{
			return new KendallsCorrelation().correlation(data1.timeSeries(), data2.timeSeries()); 
		}
	};
	
	public static final Feature COV = new FeatureExtractor.DoubleAxisFeature("Cov")
	{
		@Override
		public double calculate(SensorData data1, SensorData data2) 
		{
			return new Covariance().covariance(data1.timeSeries(), data2.timeSeries()); 
		}
	};
	
	public static final SingleAxisFeature SPEC_STD_DEV = new SingleAxisFeature("Spec_Std_Dev")
	{
		@Override
		public double calculate(SensorData data)
		{
			DFT dft = data.frequencySeries();
			double magSum = new Sum().evaluate(dft.reals);
			double specStdDev = 0.0;
			
			for (int i = 0; i < dft.length; ++i)
				specStdDev += Math.pow(dft.imags[i], 2.0) * (dft.reals[i]);
			
			return Math.sqrt(specStdDev / magSum);
		}
	};
	
	public static final SingleAxisFeature SPEC_CENTROID = new SingleAxisFeature("Spec_Centroid")
	{
		@Override
		public double calculate(SensorData data)
		{
			DFT dft = data.frequencySeries();
			double magSum = new Sum().evaluate(dft.reals);
			double specCen = 0.0;
			
			for (int i = 0; i < dft.length; ++i)
				specCen += dft.imags[i] * dft.reals[i];
			
			return specCen / Math.abs(magSum);
		}
	};
	
	public static final Feature SPEC_SKEWNESS = new SingleAxisFeature("Spec_Skew")
	{
		@Override
		public double calculate(SensorData data)
		{
			double specStdDev = SPEC_STD_DEV.calculate(data);
			double specCen = SPEC_CENTROID.calculate(data);
			
			DFT dft = data.frequencySeries();
			
			double value = 0.0;
			for (int i = 0; i < dft.length; ++i)
				value += Math.pow(dft.reals[i] - specCen, 3.0) * dft.reals[i];
			
			return value / Math.pow(specStdDev, 3.0);
		}
	};
	
	public static final Feature SPEC_KURTOSIS = new SingleAxisFeature("Spec_Kurt")
	{
		@Override
		public double calculate(SensorData data)
		{
			double specStdDev = SPEC_STD_DEV.calculate(data);
			double specCen = SPEC_CENTROID.calculate(data);
			
			DFT dft = data.frequencySeries();
			
			double value = 0.0;
			for (int i = 0; i < dft.length; ++i)
				value += Math.pow(dft.reals[i] - specCen, 4.0) * dft.reals[i];
			
			return value / Math.pow(specStdDev, 4.0) - 3.0;
		}
	};
	
	public static final Feature SPEC_CREST = new SingleAxisFeature("Spec_Crest")
	{
		@Override
		public double calculate(SensorData data)
		{
			double specCen = SPEC_CENTROID.calculate(data);
			DFT dft = data.frequencySeries();
			
			return FeatureExtractor.max(dft.reals) / specCen;
		}
	};
	
	public static final Feature IRREG_K = new SingleAxisFeature("Irreg_K")
	{
		@Override
		public double calculate(SensorData data)
		{
			DFT dft = data.frequencySeries();
			
			double value = 0.0;
			for (int i = 1; i < dft.length - 1; ++i)
				value += Math.abs(dft.reals[i] - (dft.reals[i-1] + dft.reals[i] + dft.reals[i+1]) / 3.0);
			return value;
		}
	};
	
	public static final Feature IRREG_J = new SingleAxisFeature("Irreg_J")
	{
		@Override
		public double calculate(SensorData data)
		{
			DFT dft = data.frequencySeries();
			
			double num = 0.0, den = 0.0;
			
			for (int i = 1; i < dft.length - 1; ++i)
			{
				num += Math.pow(dft.reals[i] - dft.reals[i+1], 2.0);
				den += Math.pow(dft.reals[i], 2.0);
			}
			
			return num / den;
		}
	};
	
	public static final Feature SMOOTHNESS = new SingleAxisFeature("Smoothness")
	{
		@Override
		public double calculate(SensorData data)
		{
			DFT dft = data.frequencySeries();
			
			double value = 0.0;
			
			for (int i = 1; i < dft.length - 1; ++i)
			{
				value += 20.0 * Math.abs(Math.log(dft.reals[i]) -
						(Math.log(dft.reals[i-1]) + Math.log(dft.reals[i]) + Math.log(dft.reals[i+1])) / 3.0);
			}
			
			return value;
		}
	};
	
	public static final Feature FLATNESS = new SingleAxisFeature("Flatness")
	{
		@Override
		public double calculate(SensorData data)
		{
			DFT dft = data.frequencySeries();
			
			double num = 0.0, den = 0.0;
			
			for (int i = 0; i < dft.length; ++i)
			{
				if (i == 0)
					num = dft.reals[i];
				else
					num *= dft.reals[i];
				
				den += dft.reals[i];
			}
			
			num = Math.pow(num, 1.0 / dft.length);
			den = den / dft.length;
			
			if (Double.isNaN(num) || Double.isNaN(den) || den == 0.0)
				return 0.0;
			else
				return num / den;
		}
	};
	
	/**
	 * Successive differences between intervals.
	 */
	public static final Feature RMSSD = new SingleAxisFeature("RMSSD")
	{
		@Override
		public double calculate(SensorData data)
		{
			// Data axis
			double[] vals = data.timeSeries();
			
			// List of peak locations
			ArrayList<Integer> peaks = new ArrayList<>();
			
			for (int i = 1; i < vals.length - 1; ++i)
			{
				if (vals[i - 1] < vals[i] && vals[i + 1] < vals[i])
					peaks.add(i);
			}
			
			// Calculate RMSSD
			double rmssd = 0;
			
			for (int i = 1; i < peaks.size(); ++i)
			{
				rmssd += Math.pow(peaks.get(i) - peaks.get(i - 1), 2);
			}
			
			rmssd /= peaks.size() - 1;
			
			return rmssd;
		}
	};
	
	/**
	 * Mean of the absolute values of the first differences.	 *
	 */
	public static final Feature MeanFirstDifferences = new SingleAxisFeature("Mean_of_First_difference")
	{
		@Override
		public double calculate(SensorData data)
		{
			// Data axis
			double[] vals = data.timeSeries();
			
			int num = 0;
			double sum = 0;
			
			for (int i = 1; i < vals.length; ++i)
			{
				++num;
				sum += Math.abs(vals[i] - vals[i - 1]);
			}
			
			return sum / num;
		}
	};
	
	/**
	 * Mean of the absolute values of the second differences.
	 *
	 */
	public static final Feature MeanSecondDifferences = new SingleAxisFeature("Mean_of_Second_differences")
	{

		@Override
		public double calculate(SensorData data)
		{
			// Data axis
			double[] vals = data.timeSeries();
			
			ArrayList<Double> diffs = new ArrayList<>();
			
			for (int i = 1; i < vals.length; ++i)
			{
				diffs.add(Math.abs(vals[i] - vals[i - 1]));
			}
			
			int num = 0;
			double sum = 0;
			
			for (int i = 1; i < diffs.size(); ++i)
			{
				++num;
				sum += Math.abs(diffs.get(i) - diffs.get(i - 1));
			}
			
			return sum / num;
		}
	};
	
	/**
	 * The Zero Crossing Rate.
	 */
	public static final Feature ZeroCrossingRate = new SingleAxisFeature("Zero_Crossing_Rate")
	{
		@Override
		public double calculate(SensorData data)
		{
			// Data axis
			double[] vals = data.timeSeries();
			
			double sum = 0;
			
			for (int i = 1; i < vals.length; ++i)
			{
				sum += (vals[i] * vals[i - 1] < 0.0) ? 1.0: 0.0; 
			}
			
			sum /= vals.length - 1;
			
			return sum;
		}
	};
}
