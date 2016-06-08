package arena.arenasmartball.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Encapsulates a data time series.
 * @author Nathaniel Stone
 *
 */
public class ImpactData implements DataDecompressor.DecompressedDataCallback
{
	/** List of Samples for this data */
	public final ArrayList<Sample> SAMPLES;

    /** The number of samples that were requested. */
    public final int NUM_SAMPLES_REQUESTED;

    // The DataDecompressor to use to decompress data
    private DataDecompressor dataDecompressor;

    /**
     * Creates an empty ImpactData.
     * @param numSamplesRequested The number of samples that were requested
     */
    public ImpactData(int numSamplesRequested)
    {
        NUM_SAMPLES_REQUESTED = numSamplesRequested;
        SAMPLES = new ArrayList<>();
        dataDecompressor = new DataDecompressor(this);
    }

//	/**
//	 * Creates a Data with the specified Samples.
//	 * @param samples List of Samples for this data
//	 */
//	public ImpactData(ArrayList<Sample> samples)
//	{
//		SAMPLES = samples;
//	}

    /**
     * Adds a line of raw impact data to this ImpactData.
     * @param data The data to add
     */
    public void addLine(byte[] data)
    {
        dataDecompressor.addLine(data);
    }

    /**
     * Gets the number of Samples contained in this ImpactData.
     * @return The number of Samples contained in this ImpactData
     */
    public int getNumSamples()
    {
        return SAMPLES.size();
    }

    /**
     * Gets the percentage amount as an integer from 0 to 100 denoting the completion of this ImpactData,
     * or how many samples it currently contains out of the maximum sample capacity.
     * @return A transmission progress value for this KickData for use in a progress bar
     */
    public int getPercentComplete()
    {
        if (NUM_SAMPLES_REQUESTED > 0)
            return Math.round((100.0f * SAMPLES.size()) / NUM_SAMPLES_REQUESTED);
        else
            return 0;
    }

//	/**
//	 * Returns the Sample at the specified position, or null of the index is out of bounds.
//	 * @param i The index
//	 * @return The Sample at the index or null
//	 */
//	public Sample getSafe(int i)
//	{
//		if (i < 0 || i >= SAMPLES.size())
//			return null;
//		else
//			return SAMPLES.get(i);
//	}
	
	/**
	 * Writes this Data to a comma separated values file.
	 * @param dst The File to which to write
	 */
	public void toCSVFile(File dst, boolean raw) throws FileNotFoundException
	{
        PrintWriter out = new PrintWriter(dst);

        for (Sample s: SAMPLES)
        {
            if (raw)
                out.println(s.toRawString());
            else
                out.println(s.toString());
        }

        out.close();
	}
	
//	/**
//	 * Writes this Data to a comma separated values file.
//	 * @param dst The File to which to write
//	 */
//	public void toCSVFile(File dst)
//	{
//		toCSVFile(dst, false);
//	}
	
	@Override
	public String toString()
	{
		String r = "";
		
		for (Sample sample: SAMPLES)
			r += sample + "\n";
		
		return r;
	}
	
//	/**
//	 * Creates a Data from a comma separated values file.
//	 * @param file The File from which to read
//	 * @return The read Data
//	 */
//	public static ImpactData readFromCSV(File file, boolean inGs, boolean timesIncluded)
//	{
//		ArrayList<Sample> samples = new ArrayList<>();
//		ImpactData data = new ImpactData(samples);
//
//		int off = timesIncluded ? 1: 0;
//		Sample s;
//
//		Scanner scan;
//		try
//		{
//			scan = new Scanner(file);
//
//			String str[];
//
//			while (scan.hasNextLine())
//			{
//				str = scan.nextLine().split(",");
//
//				if (inGs)
//				{
//					samples.add(s = new Sample(Double.parseDouble(str[off].trim()),
//							   Double.parseDouble(str[off + 1].trim()),
//							   Double.parseDouble(str[off + 2].trim())));
//				}
//				else
//				{
//					samples.add(s = new Sample(Short.parseShort(str[off].trim()),
//								   Short.parseShort(str[off + 1].trim()),
//								   Short.parseShort(str[off + 2].trim())));
//				}
//
//				if (timesIncluded)
//					s.setTime(Double.parseDouble(str[0].trim()));
//			}
//
//			scan.close();
//		}
//		catch (FileNotFoundException e)
//		{
//			System.err.println("Could not find file " + file + "! (" + e.getMessage() + ")");
//		}
//
//		return data;
//	}

    /**
     * Called when this DecompressedDataCallback has received a new decompressed Sample.
     *
     * @param sample The newly received decompressed Sample
     */
    @Override
    public void onNewSample(Sample sample)
    {
        if (SAMPLES.size() < NUM_SAMPLES_REQUESTED)
            SAMPLES.add(sample);
    }
}
