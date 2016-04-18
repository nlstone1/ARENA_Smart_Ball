package arena.arenasmartball.correlation;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


/**
 * Abstract class representing time series data from some number of axes read from a csv file.
 * @author Theodore Stone
 */

@Deprecated
public class SensorDataFileThingy implements FeatureExtractor.DataSeriesFeaturable
{		
	/** The number of data axes of this TimeSeriesFile. */
	public final int NUM_AXES;
	
	/** The directory containing the data of this TimeSeriesFile. */
	public final File PARENT_FILE;
	
	/** The data of this DataFile for each axis. */
	public SensorData[] DATA;
	
	/**
	 * Constructs a new TimeSeriesFile.
	 * @param numAxes The number of data axes
	 */
	public SensorDataFileThingy(File dataFile, int numAxes)
	{
		NUM_AXES = numAxes;
		PARENT_FILE = dataFile.getParentFile();
		DATA = new SensorData[NUM_AXES];
		read(dataFile);
	}
	
	/**
	 * Gets an array of SensorData objects for each axis of data of this Featurable.
	 * @return An array of SensorData objects for each axis of data of this Featurable
	 */
	@Override
	public SensorData[] getAxes() 
	{
		return DATA;
	}
	
	/**
	 * Calculates the given features for this TimeSeriesFile.
	 * @param features The Features to calculate
	 * @return A FeatureSet containing the calculated features
	 */
	public FeatureSet calculateFeatures(FeatureExtractor.Feature[] features)
	{
		return FeatureExtractor.getFeatureValues(this, features); 
	}
	
	/**
	 * Reads this TimeSeries file.
	 * @param csvFile The file to read from
	 */
	private void read(File csvFile)
	{
		Scanner in;
		
		try 
		{
			in = new Scanner(csvFile);
		} 
		catch (FileNotFoundException e) 
		{
			System.err.println("Error reading TimeSeriesFile from " + PARENT_FILE.getAbsolutePath() + ": " + e.getMessage());
			return;
		}
		
		@SuppressWarnings("unchecked")
		List<Double>[] values = (ArrayList<Double>[])new ArrayList<?>[NUM_AXES];
		
		for (int i = 0; i < NUM_AXES; ++i)
			values[i] = new ArrayList<Double>();
		
		// Read the file
		String[] line;
		
		while (in.hasNextLine())
		{
			line = in.nextLine().trim().split(","); 
			
			try
			{
				for (int i = 0; i < NUM_AXES; ++i)
					values[i].add(Double.parseDouble(line[i]));  
			}
			catch (Exception e)
			{
				System.err.println("Error parsing TimeSeriesFile from " + PARENT_FILE.getAbsolutePath() + ": " + e.getMessage());
			}
		}
		
		in.close();
		
		// Create data
		for (int i = 0; i < NUM_AXES; ++i)
			DATA[i] = new SensorData(values[i]); 
	}
}
