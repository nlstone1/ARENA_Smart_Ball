package arena.arenasmartball.old_data;

import android.util.Log;

import java.io.File;

/**
 * Class representation of type 1 data.
 * Created by Theodore on 5/22/2015.
 */
public class TypeOneData extends RawImpactData
{
    /** The tag for this class */
    private static final String TAG = "TypeOneData";

    /** The save name of the type 1 data file. */
    public static final String TYPE1_DATA_FILE_NAME = "data1.csv";

    /** The save name of the type 1 raw data file. */
    public static final String TYPE1_RAW_DATA_FILE_NAME = "data1_raw.txt";

    /**
     * Constructor.
     * @param numSamples  The number of samples asked for
     */
    public TypeOneData(int numSamples)
    {
        super (numSamples);
    }

    /**
     * Method to add a line of data to this KickData.
     * @param data The raw data to add
     * @param start true if this is the first line of data
     * @param end true if this is the last line of data
     */
    @Override
    public void addLine(byte[] data, boolean start, boolean end)
    {
        if (data == null || data.length < 20)
            return;

        if (!start && !end) // Ignore start and end lines
        {
            // Parse shorts
            int index = 2;
            short x, y, z;

            for (int i = 0; i < 3; ++i, index += 6)
            {
                x = bytesToShort(data[index + 1], data[index]);
                y = bytesToShort(data[index + 3], data[index + 2]);
                z = bytesToShort(data[index + 5], data[index + 4]);
                addPointToMaxMag(x);
                addPointToMaxMag(y);
                addPointToMaxMag(z);

                this.getData().add(new Sample(x, y, z));
            }
        }

        super.addLine(data, start, end);
    }

    /**
     * Method to get a transmission progress value for this KickData for use in a progress bar. Values
     * will be in the range 0 to 100 inclusive.
     * @return A transmission progress value for this KickData for use in a progress bar
     */
    @Override
    public int getTransmissionProgress()
    {
        int value = (int)((300.0f * getRawData().size()) / getNumSamplesAskedFor()) + 1;

        if (value < 0)
            return 0;
        else if (value > 100)
            return 100;
        else
            return value;
    }

    /**
     * Saves this KickData in the given directory.
     * @param dir The File in which to save this KickData
     * @return The number of files that were saved successfully
     */
    public int save(File dir)
    {
        File dataFile = new File(dir, TYPE1_DATA_FILE_NAME);
        File rawDataFile = new File(dir, TYPE1_RAW_DATA_FILE_NAME);

        int numSaved = 0;
        boolean rawDataSaved = writeData(dataFile);
        boolean dataSaved = writeRawData(rawDataFile);

        if (rawDataSaved)
        {
            Log.d(TAG, "Saved raw data to " + rawDataFile.getAbsolutePath());
            ++numSaved;
        }
        else
            Log.e(TAG, "Error saving raw data to " + rawDataFile.getAbsolutePath());

        if (dataSaved)
        {
            Log.d(TAG, "Saved data to " + dataFile.getAbsolutePath());
            ++numSaved;
        }
        else
            Log.e(TAG, "Error saving data to " + dataFile.getAbsolutePath());

        return numSaved;
    }

//    /**
//     * Helper method to read in raw data.
//     * @param file The file from which to read the data
//     */
//    @Override
//    protected void loadRawData(File file)
//    {
//        super.loadRawData(new File(file, TYPE1_RAW_DATA_FILE_NAME));
//    }
//
//    /**
//     * Helper method to read in data.
//     * @param file The file from which to read the data
//     */
//    @Override
//    protected void loadData(File file)
//    {
//        super.loadData(new File(file, TYPE1_DATA_FILE_NAME));
//    }
}
