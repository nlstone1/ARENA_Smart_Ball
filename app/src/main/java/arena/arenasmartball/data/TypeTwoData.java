package arena.arenasmartball.data;

import android.util.Log;

/**
 * Class representation of type 2 data. Source from decompiled Adidas SmartBall app.
 * Created by Theodore on 5/22/2015.
 */
public class TypeTwoData extends KickData
{
    /** The tag for this class */
    private static final String TAG = "TypeTwoData";

    // Fields for data parser
    private short[] values;
    private int numLinesCollected;
    private boolean c;
    private int d;
    private short e;
    private short f;
    private short g;
    private boolean h;

    /**
     * Constructor.
     * @param numSamples  The number of samples asked for
     */
    public TypeTwoData(int numSamples)
    {
        super (numSamples);
        values = new short[numSamples * 3];
    }

    /**
     * Methods from decompiled code
     */

    private static boolean comparisonWithCast(final byte b, final int n)
    {
        return b == (byte) n;
    }

    private boolean extractSamplesFromLine(final byte[] array, final int n, final boolean b)
    {
        if (b)
        {
            this.e += (short)array[n];
            this.f += (short)array[n + 1];
            this.g += (short)array[n + 2];

            if (this.e())
            {
                this.e += (short)array[n + 3];
                this.f += (short)array[n + 4];
                this.g += (short)array[n + 5];
                if (this.e())
                {
                    return false;
                }
            }
            return true;
        }
        this.e = bytesToShort(array[n + 1], array[n]);
        this.f = bytesToShort(array[n + 3], array[n + 2]);
        this.g = bytesToShort(array[n + 5], array[n + 4]);
        return !this.e();
    }

    private static boolean c(final byte[] array)
    {
        boolean a = comparisonWithCast(array[0], 154);
        for (int i = 9; i <= 19; ++i)
        {
            a = (a && comparisonWithCast(array[i], 0));
        }
        return a;
    }

    private boolean e()
    {
        if (this.numLinesCollected == getNumSamplesAskedFor())
        {
            return false;
        }

        final int n = this.numLinesCollected * 3;
        this.values[n] = this.e;
        this.values[n + 1] = this.f;
        this.values[n + 2] = this.g;

        addPointToMaxMag(this.values[n]);
        addPointToMaxMag(this.values[n + 1]);
        addPointToMaxMag(this.values[n + 2]);
        getData().add(new Sample(this.values[n], this.values[n + 1], this.values[n + 2]/*, "0")*/));

        ++this.numLinesCollected;
        return true;
    }

    /**
     * Method to add a line of data to this KickData.
     * @param data The raw data to add
     * @param start true if this is the first line of data
     * @param end true if this is the last line of data
//     * @param timestamp The timestamps of the samples in this line
     */
    @Override
    public void addLine(byte[] data, boolean start, boolean end)//, String... timestamp)
    {
        int i = 0;
        if (data.length != 20)
        {
            throw new IllegalArgumentException("Wrong packet size: " + data.length);
        }
        if (!this.h)
        {
            if (!this.c)
            {
                if (comparisonWithCast(data[0], 138) && comparisonWithCast(data[1], 10))
                {
                    while (i < data.length)
                    {
//                        System.out.println("BYTE\tvalue " + Integer.toBinaryString(data[i]));
                        ++i;
                    }
                    return;
                }
                this.c = true;
            }
            if (c(data))
            {
                this.h = true;
                return;
            }
            final int n = (data[0] & 0xFF) + ((data[1] & 0xFF) << 8);
            final int n2 = n & 0x1FFF;
            if (n2 != this.d)
            {
                Log.e(TAG, "Unexpected sequence number: " + n2 + ", expected " + this.d);
                return; // TODO took out exception for now - don't want app to crash
//                throw new IllegalArgumentException("Unexpected sequence number: " + n2 + ", expected " + this.d);
            }
            ++this.d;
            final boolean b = (0x8000 & n) != 0x0;
            final boolean b2 = (n & 0x4000) != 0x0;
            final boolean b3 = (n & 0x2000) != 0x0;
            if (this.extractSamplesFromLine(data, 2, b) || this.extractSamplesFromLine(data, 8, b2) || this.extractSamplesFromLine(data, 14, b3))
            {
                Log.d(TAG, "Done extracting samples");
            }
//            System.out.println(String.format("Sequence: %d --> %d {%s|%s|%s}", n2, this.numLinesCollected, b, b2, b3));
        }

        super.addLine(data, start, end);//, timestamp);
    }

    /*
     * Public methods
     */

//    /**
//     * Method to add a line of data to this KickData.
//     * @param data The raw data to add
//     * @param start true if this is the first line of data
//     * @param end true if this is the last line of data
//     * @param timestamp The timestamps of the samples in this line
//     */
//    @Override
//    public void addLine(byte[] data, boolean start, boolean end, String... timestamp)
//    {
//        if (data == null || data.length < 20)
//            return;
//
//        if (!start && !end) // TODO for now we won't use these (start, end)
//        {
//            // Check the count
//            int i = (data[0] & 0xFF) + ((data[1] & 0xFF) << 8);
//            int j = i & 0x1FFF;
//
//            if (j != count)
//            {
//                Log.w(TAG, "Unexpected sequence number: " + j + ", expected " + count);
//            }
//
//            count++;
//
//            // Each sample is parsed differently depending on the second byte of the line
//            boolean a, b, c;
//
//            a = ((0x8000 & i) != 0);
//            b = ((0x4000 & i) != 0);
//            c = ((0x2000 & i) != 0);
//
//            extractSample(data, 2, a, timestamp[0]);
//            extractSample(data, 8, b, timestamp[1]);
//            extractSample(data, 14, c, timestamp[2]);
//        }
//
//        super.addLine(data, start, end, timestamp);
//    }

    /**
     * Method to get a transmission progress value for this KickData for use in a progress bar. Values
     * will be in the range 0 to 100 inclusive.
     * @return A transmission progress value for this KickData for use in a progress bar
     */
    @Override
    public int getTransmissionProgress()
    {
        int value = (int)((100.0 * numLinesCollected) / getNumSamplesAskedFor());
//        int value = (int)((600.0f * getRawData().size()) / getNumSamplesAskedFor()) + 1;

        if (value < 0)
            return 0;
        else if (value > 100)
            return 100;
        else
            return value;
    }

//    /**
//     * Saves this KickData in the given directory.
//     * @param dir The File in which to save this KickData
//     */
//    @Override
//    public void save(File dir)
//    {
//        File dataFile = new File(dir, TYPE2_DATA_FILE_NAME);
//        File rawDataFile = new File(dir, TYPE2_RAW_DATA_FILE_NAME);
//
//        writeData(dataFile);
//        writeRawData(rawDataFile);
//    }
//
//    /**
//     * Helper method to read in raw data.
//     * @param file The file from which to read the data
//     */
//    @Override
//    protected void loadRawData(File file)
//    {
//        super.loadRawData(new File(file, TYPE2_RAW_DATA_FILE_NAME));
//    }
//
//    /**
//     * Helper method to read in data.
//     * @param file The file from which to read the data
//     */
//    @Override
//    protected void loadData(File file)
//    {
//        super.loadData(new File(file, TYPE2_DATA_FILE_NAME));
//    }
//
//    /*
//	 * Extracts a sample from the specified offset in the line.
//	 */
//    private void extractSample(byte[] data, int offset, boolean bool, String timestamp)
//    {
//        try
//        {
//            if (bool)
//            {
//                x += (short) data[offset];
//                y += (short) data[offset];
//                z += (short) data[offset];
//            }
//            else
//            {
//                x = bytesToShort(data[(offset + 1)], data[offset]);
//                y = bytesToShort(data[(offset + 3)], data[(offset + 2)]);
//                z = bytesToShort(data[(offset + 5)], data[(offset + 4)]);
//            }
//        }
//        catch (Exception e)
//        {/* Out of bounds, finished reading data */}
//
//        addPointToMaxMag(x);
//        addPointToMaxMag(y);
//        addPointToMaxMag(z);
//        getData().add(new Sample(x, y, z, timestamp));
//    }
}
