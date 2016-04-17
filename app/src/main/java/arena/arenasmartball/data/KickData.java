package arena.arenasmartball.data;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * Parent class for both types of data.
 * Created by Theodore on 5/22/2015.
 */
public abstract class KickData
{
    /** The tag for this class */
    private static final String TAG = "KickData";

    // Whether or not this KickData is complete
    private boolean isComplete;

    private short maxValueMag;
    private int numSamplesAskedFor;
    private List<byte[]> rawData;
    private List<Sample> data;

    /**
     * Constructs a new KickData with the given parameters.
     * @param numSamplesAskedFor The number of samples of this data type asked for before transmit
     */
    protected KickData(int numSamplesAskedFor)
    {
        isComplete = false;
        rawData = new LinkedList<>();
        data = new ArrayList<>();
        this.numSamplesAskedFor = numSamplesAskedFor;
    }

    /**
     * Helper method to create a short from two bytes.
     * @param msb The most significant byte
     * @param lsb The least significant byte
     * @return A short created from the two bytes
     */
    public static short bytesToShort(byte msb, byte lsb)
    {
        return (short)((msb << 8) | lsb & 255);
    }

    /**
     * Method to add a line of data to this KickData.
     * @param data The raw data to add
     * @param start true if this is the first line of data
     * @param end true if this is the last line of data
     */
    public void addLine(byte[] data, boolean start, boolean end)
    {
        rawData.add(data);
    }

    /**
     * Gets whether or not this KickData is complete.
     * @return Whether or not this KickData is complete
     */
    public boolean isComplete()
    {
        return isComplete;
    }

    /**
     * Sets the is complete flag of this KickData.
     * @param complete The new value of the is complete flag
     */
    protected void setComplete(boolean complete)
    {
        isComplete = complete;
    }

    /**
     * Method to get the raw bytes of this KickData.
     * @return The raw bytes of this KickData
     */
    public List<byte[]> getRawData()
    {
        return rawData;
    }

    /**
     * Method to get the formatted data of this KickData.
     * @return The formatted data  of this KickData
     */
    public List<Sample> getData()
    {
        return data;
    }

    /**
     * Method to get the current maximum data value.
     * @return The current maximum data value
     */
    public short getMaxValueMag()
    {
        return maxValueMag;
    }

    /**
     * Method to get a transmission progress value for this KickData for use in a progress bar. Values
     * will be in the range 0 to 100 inclusive.
     * @return A transmission progress value for this KickData for use in a progress bar
     */
    public abstract int getTransmissionProgress();

    /**
     * Method to get the number of samples of this data type asked for before transmit.
     * @return The number of samples of this data type asked for before transmit
     */
    public int getNumSamplesAskedFor()
    {
        return numSamplesAskedFor;
    }

//    /**
//     * Saves this KickData in the given directory.
//     * @param dir The File in which to save this KickData
//     */
//    public abstract void save(File dir);

    /**
     * Adds a point to edit, if needed, the maximum data value.
     * @param point The point to add
     */
    protected void addPointToMaxMag(short point)
    {
        maxValueMag = (short) Math.max(maxValueMag, Math.abs(point));
    }

    /**
     * Helper method to write the raw data to a file in a human readable format.
     * @param file The file to which to write the data
     */
    protected void writeRawData(File file)
    {
        PrintWriter out;

        try
        {
            out = new PrintWriter(file);
        }
        catch (FileNotFoundException e)
        {
            Log.e(TAG, "Error writing raw data to file " + file.getName() + ": " + e.getMessage());
            return;
        }

        int i;
        for (byte[] data: rawData)
        {
            for (i = 0; i < data.length; ++i)
            {
                out.print(data[i] + (i == data.length - 1 ? "\n" : " "));
            }
        }

        out.flush();
        out.close();
    }

    /**
     * Helper method to read in raw data.
     * @param file The file from which to read the data
     */
    protected void loadRawData(File file)
    {
        Scanner in;

        try
        {
            in = new Scanner(file);
        }
        catch (FileNotFoundException e)
        {
            Log.w(TAG, "Did not load raw data file " + file.getName() + ": " + e.getMessage());
            return;
        }

        String line;
        String[] lines;
        byte[] data;

        while (in.hasNextLine())
        {
            line = in.nextLine();
            lines = line.split(" ");
            data = new byte[lines.length];

            for (int i = 0; i < data.length; ++i)
                data[i] = Byte.parseByte(lines[i]);

            rawData.add(data);
        }

        in.close();
    }

    /**
     * Helper method to write the data to a file in a human readable format.
     * @param file The file to which to write the data
     */
    protected void writeData(File file)
    {
        PrintWriter out;

        try
        {
            out = new PrintWriter(file);
        }
        catch (FileNotFoundException e)
        {
            Log.e(TAG, "Error writing data to file " + file.getName() + ": " + e.getMessage());
            return;
        }

        for (Sample v: data)
        {
            out.println(v.toString());
        }

        out.flush();
        out.close();
    }

    /**
     * Helper method to read in data.
     * @param file The file from which to read the data
     */
    protected void loadData(File file)
    {
        Scanner in;

        try
        {
            in = new Scanner(file);
        }
        catch (FileNotFoundException e)
        {
            Log.w(TAG, "Did not load data file " + file.getName() + ": " + e.getMessage());
            return;
        }

        String line;
        String[] lines;
        short x = 0, y = 0, z = 0;

        while (in.hasNextLine())
        {
            line = in.nextLine();
            lines = line.split(",");

            if (lines.length > 1)
            {
                x = Short.parseShort(lines[1]);
                addPointToMaxMag(x);
            }

            if (lines.length > 2)
            {
                y = Short.parseShort(lines[2]);
                addPointToMaxMag(y);
            }

            if (lines.length > 3)
            {
                z = Short.parseShort(lines[3]);
                addPointToMaxMag(z);
            }

            data.add(new Sample(x, y, z/*, timestamp*/));
        }

        in.close();
    }
}
