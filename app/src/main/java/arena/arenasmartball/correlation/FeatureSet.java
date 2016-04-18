package arena.arenasmartball.correlation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;

/**
 * Class representing a set of Features.
 * @author Theodore Stone
 */

public class FeatureSet 
{
	/** The array of feature values. */
	private ArrayList<Double> featureArray;
	
	/** A Map from feature names to their indices in the array. */
	private Map<String, Integer> featureMap;
	
	/**
	 * Default constructor.
	 */
	public FeatureSet()
	{
		featureArray = new ArrayList<Double>();
		featureMap = new HashMap<String, Integer>();
	}
	
	/**
	 * Gets an array containing the features of this FeatureSet.
	 * @return An array containing the features of this FeatureSet
	 */
	public double[] toArray()
	{
		double[] array = new double[featureArray.size()];
		
		for (int i = 0; i < array.length; ++i)
			array[i] = featureArray.get(i);
		
		return array;
	}
	
	/**
	 * Gets the feature of the given name in this FeatureSet.
	 * @param name The name of the feature to get
	 * @return The feature of the given name in this FeatureSet, or 0 if the feature is not contained
	 */
	public double get(String name)
	{
		Integer featureIndex = featureMap.get(name);
		
		if (featureIndex == null)
			return 0.0;
		else
			return featureArray.get(featureIndex.intValue());
	}
	
//	/**
//	 * Gets the values of this FeatureSet and puts them into the given FeatureScaler.
//	 * @param scaler The FeatureScaler in which to put values
//	 */
//	public void get(FeatureScaler scaler)
//	{
//		for (Entry<String, Integer> entry: featureMap.entrySet())
//			scaler.addFeatureValue(entry.getKey(), featureArray.get(entry.getValue()));
//	}
	
	/**
	 * Adds a feature to this FeatureSet.
	 * @param name The name of the feature
	 * @param value The value of the feature
	 */
	public void put(String name, double value)
	{
		featureMap.put(name,  featureArray.size());
		featureArray.add(value); 
	}
	
//	/**
//	 * Normalizes this FeatureSet using the given FeatureScaler.
//	 * @param scaler The FeatureScaler to use to normalize
//	 */
//	public void normalize(FeatureScaler scaler)
//	{
//		for (Entry<String, Integer> entry: featureMap.entrySet())
//		{
//			featureArray.set(entry.getValue(), scaler.normalize(entry.getKey(), featureArray.get(entry.getValue())));
//		}
//	}
//
//	/**
//	 * Denormalizes this FeatureSet using the given FeatureScaler.
//	 * @param scaler The FeatureScaler to use to denormalize
//	 */
//	public void denormalize(FeatureScaler scaler)
//	{
//		for (Entry<String, Integer> entry: featureMap.entrySet())
//		{
//			featureArray.set(entry.getValue(), scaler.denormalize(entry.getKey(), featureArray.get(entry.getValue())));
//		}
//	}
	
//	/**
//	 * Writes this FeatureSet to the given File in a human readable format.
//	 * @param file The file to which to write
//	 */
//	public void write(File file)
//	{
//		PrintWriter out;
//
//		try
//		{
//			out = new PrintWriter(file);
//		}
//		catch (FileNotFoundException e)
//		{
//			System.err.println("Error writing FeatureSet to file " +
//							(file == null ? "NULL" : file.getName()) + ": " + e.getMessage());
//			return;
//		}
//
//
//		// Write feature values
//		for (Entry<String, Integer> entry: featureMap.entrySet())
//		{
//			out.println(entry.getKey() + "," + featureArray.get(entry.getValue()));
//		}
//
//		out.close();
//	}
//
//	/**
//	 * Reads this FeatureSet from the given File saved by write().
//	 * @param file The file to which to write
//	 */
//	public void read(File file)
//	{
//		Scanner in;
//
//		try
//		{
//			in = new Scanner(file);
//		}
//		catch (FileNotFoundException e)
//		{
//			System.err.println("Error reading FeatureSet from file " +
//							(file == null ? "NULL" : file.getName()) + ": " + e.getMessage());
//			return;
//		}
//
//		// Read points
//		String[] line;
//
//		while (in.hasNextLine())
//		{
//			line = in.nextLine().trim().split(",");
//
//			try
//			{
//				put(line[0].trim(), Double.parseDouble(line[1].trim()));
//			}
//			catch (Exception e)
//			{
//				System.err.println("Error parsing CorrelationPoint file " +
//						(file == null ? "NULL" : file.getName()) + ": " + e.getMessage());
//			}
//		}
//
//		in.close();
//	}
}
