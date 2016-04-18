package arena.arenasmartball.correlation;

import java.util.Iterator;
import java.util.List;

/**
 * Class containing generic sensor data.
 * @author Theodore Stone
 */

public class SensorData 
{
	/** The data time series */
	private double[] timeSeries;
	
	/** The data in the frequency domain */
	private DFT frequencySeries;
	
	/**
	 * Creates a new SensorData.
	 * @param timeSeries The data times series
	 */
	public SensorData(List<Double> timeSeries)
	{
		this (toArray(timeSeries)); 
	}
	
	/**
	 * Creates a new SensorData.
	 * @param timeSeries The data times series
	 */
	public SensorData(double[] timeSeries)
	{
		this.timeSeries = timeSeries;
	}
	
	/**
	 * Converts a List of Doubles to an array of doubles.
	 * @param values The List to convert
	 * @return An array containing the same values as the given List
	 */
	public static double[] toArray(List<Double> values)
	{
		int i = 0;
		double[] array = new double[values.size()];
		Iterator<Double> it = values.iterator();
		
		while (it.hasNext())
			array[i++] = it.next();
		
		return array;
	}
	
	/**
	 * Creates a single data time series by taking the rms value of time series data from several axes.
	 * @param timeSeriesAxes Several times series representing data from multiple axes, must be the same length
	 * @return The rms value of the data times series
	 */
	public static double[] createRMSFromAxes(double[] ... timeSeriesAxes)
	{
		if (timeSeriesAxes == null || timeSeriesAxes.length == 0)
			throw new RuntimeException("Error creating SensorData from multi-axis data: Invalid axes data");
		
		// Length check
		final int length = timeSeriesAxes[0].length;
		
		for (int i = 1; i < timeSeriesAxes.length; ++i)
			if (timeSeriesAxes[i].length != length)
				throw new RuntimeException("Error creating SensorData from multi-axis data: Axes data of non uniform length");
			
		// Create
		double[] timeSeries = new double[length];
		double sumOfSquares;
		for (int i = 0; i < length; ++i)
		{
			// Calculate sum of squares
			sumOfSquares = 0.0;
			for (int j = 0; j < timeSeriesAxes.length; ++j)
				sumOfSquares += Math.pow(timeSeriesAxes[j][i], 2.0);
				
			timeSeries[i] = Math.sqrt(sumOfSquares);
		}
		
		return timeSeries;
	}
	
	/**
	 * Creates a single data time series by taking the sum of time series data from several axes.
	 * @param timeSeriesAxes Several times series representing data from multiple axes, must be the same length
	 * @return The sum of the data times series
	 */
	public static double[] createSUMFromAxes(double[] ... timeSeriesAxes)
	{
		if (timeSeriesAxes == null || timeSeriesAxes.length == 0)
			throw new RuntimeException("Error creating SensorData from multi-axis data: Invalid axes data");
		
		// Length check
		final int length = timeSeriesAxes[0].length;
		
		for (int i = 1; i < timeSeriesAxes.length; ++i)
			if (timeSeriesAxes[i].length != length)
				throw new RuntimeException("Error creating SensorData from multi-axis data: Axes data of non uniform length");
			
		// Create
		double[] timeSeries = new double[length];
		double sum;
		for (int i = 0; i < length; ++i)
		{
			// Calculate sum of squares
			sum = 0.0;
			for (int j = 0; j < timeSeriesAxes.length; ++j)
				sum += timeSeriesAxes[j][i];
				
			timeSeries[i] = sum;
		}
		
		return timeSeries;
	}
	
	/**
	 * Gets the size of the time series data of this SensorData.
	 * @return The size of the time series data of this SensorData
	 */
	public int size()
	{
		return timeSeries.length;
	}
	
	/**
	 * Gets the time series of this SensorData.
	 * @return The time series of this SensorData
	 */
	public double[] timeSeries()
	{
		return timeSeries;
	}
	
	/**
	 * Gets the frequency series of this SensorData.
	 * @return The frequency series of this SensorData
	 */
	public DFT frequencySeries()
	{
		if (frequencySeries == null) 
			frequencySeries = new DFT(timeSeries);
		
		return frequencySeries;
	}
	
	/**
	 * Gets a new SensorData containing the given region of this SensorData.
	 * @param start The first index of the region
	 * @param end The last index of the region
	 * @return A new SensorData containing the given region of this SensorData
	 */
	public SensorData getRegion(int start, int end)
	{
		if (start < 0 || start > end || end > timeSeries.length - 1)
			throw new RuntimeException("Error getting region of SensorData: Start = " + start + ", End = " + end);
		
		double[] newSeries = new double[end - start + 1];
		
		for (int i = start; i <= end; ++i)
			newSeries[i - start] = timeSeries[i];
		
		return new SensorData(newSeries); 
	}
}
