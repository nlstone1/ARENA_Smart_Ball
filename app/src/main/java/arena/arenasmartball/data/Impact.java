package arena.arenasmartball.data;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import arena.arenasmartball.ball.SmartBall;
import arena.arenasmartball.correlation.Correlator;

import static arena.arenasmartball.data.ImpactRegionExtractor.*;

/**
 * Class representing a SmartBall impact.
 * Created by Theodore on 4/16/2016.
 */
public class Impact implements SmartBall.DataListener
{
    // The tag for this class
    private static final String TAG = "Impact";

    // Whether or not this Impact was cancelled
    private boolean wasCancelled;

    // Whether or not this Impact is currently reading data from the SmartBall
    private boolean isReading;

//    // Records the number of lines that have been read
//    private int numLinesRead;

//    // The number of lines of data that were requested
//    private int numLinesAskedFor;

//    // Records the last data type that this Impact read
//    private int lastDataTypeRead;

    // The time of the impact, in milliseconds from Jan 1, 1970
    private long time;

    // The name of the SmartBall that recorded this Impact
    private String ballName;

    // The ImpactData of this Impact
    private ImpactData impactData;

//    // The type 1 data of this Impact
//    private TypeOneData typeOneData;
//
//    // The type 2 data of this Impact
//    private TypeTwoData typeTwoData;

    /**
     * Creates a new Impact.
     */
    public Impact()
    {
        time = System.currentTimeMillis();
    }

    public Impact(ImpactData data, long time, String ballName)
    {
        this.impactData = data;
        this.time = time;
        this.ballName = ballName;
    }

//    /**
//     * Clears the data in this Impact.
//     */
//    public void clearData()
//    {
//        wasCancelled = false;
//        typeOneData = null;
//        typeTwoData = null;
//    }

    /**
     * Gets whether or not this Impact was cancelled.
     * @return Whether or not this Impact was cancelled
     */
    public boolean wasCancelled()
    {
        return wasCancelled;
    }

    /**
     * Gets whether or not this Impact is currently reading data from the SmartBall.
     * @return Whether or not this Impact is currently reading data from the SmartBall
     */
    public boolean isReading()
    {
        return isReading;
    }

    /**
     * Gets whether or not this Impact is complete.
     * @return Whether or not this Impact is complete
     */
    public boolean isComplete()
    {
        return impactData != null;
//        if (typeOneData == null && typeTwoData == null)
//            return false;
//
//        boolean complete = true;
//
//        if (typeOneData != null && !typeOneData.isComplete())
//            complete = false;
//
//        if (typeTwoData != null && !typeTwoData.isComplete())
//            complete = false;
//
//        return complete;
    }

    /**
     * Gets the time of this Impact, in milliseconds from Jan 1, 1970.
     * @return The time of this Impact, in milliseconds from Jan 1, 1970
     */
    public long getTime()
    {
        return time;
    }

    /**
     * Gets the ImpactData of this Impact, may be null.
     * @return The ImpactData of this Impact, may be null
     */
    public ImpactData getImpactData()
    {
        return impactData;
    }

//    /**
//     * Gets the TypeOneData of this Impact, may be null.
//     * @return The TypeOneData of this Impact, may be null
//     */
//    public TypeOneData getTypeOneData()
//    {
//        return typeOneData;
//    }
//
//    /**
//     * Gets the TypeTwoData of this Impact, may be null.
//     * @return The TypeTwoData of this Impact, may be null
//     */
//    public TypeTwoData getTypeTwoData()
//    {
//        return typeTwoData;
//    }

//    /**
//     * Gets the KickData of this Impact that is still being read from the SmartBall.
//     * @return The KickData of this Impact that is still being read from the SmartBall, or null if this Impact is not reading
//     */
//    public ImpactData getDataInTransit()
//    {
//        if (isReading)
//        {
//            return impactData;
////            if (lastDataTypeRead == 1)
////            {
////                if (!typeOneData.isComplete())
////                    return typeOneData;
////            }
////            else if (lastDataTypeRead == 2)
////            {
////                if (!typeTwoData.isComplete())
////                    return typeTwoData;
////            }
////
////            return null;
//        }
//        else
//            return null;
//    }

    /**
     * Called when kick data is read.
     *
     * @param ball  The SmartBall
     * @param data  The read data
     * @param start true if this value is the data start code
     * @param end   true if this value is the data end code
     * @param type  The type of data read
     */
    @Override
    public void onSmartBallDataRead(SmartBall ball, byte[] data, boolean start, boolean end, byte type)
    {
        // Only type 2 data is considered
        if (type != 2)
        {
            Log.e(TAG, "Reading a data type other than 2: " + type);
            return;
        }

        // Otherwise add the new line to the ImpactData if it is non-null
        if (impactData == null)
            Log.w(TAG, "Reading data but ImpactData is null");
        else if (!start && !end)
            impactData.addLine(data);

//        lastDataTypeRead = type;
//        ImpactData idata = getDataInTransit();
//
//        if (idata != null && !idata.isComplete())
//            idata.addLine(data, start, end);
    }

    /**
     * Called on a data transmission event.
     *
     * @param ball     The SmartBall
     * @param dataType The type of data in the transmission
     * @param event    The DataEvent that occurred
     * @param numSamples The number of samples of data requested
     */
    @Override
    public void onSmartBallDataTransmissionEvent(SmartBall ball, byte dataType, SmartBall.DataEvent event, int numSamples)
    {
        if (event == SmartBall.DataEvent.TRANSMISSION_REQUESTED)
            isReading = true;
        else if (event == SmartBall.DataEvent.TRANSMISSION_BEGUN)
        {
            wasCancelled = false;
            isReading = true;

            // Set the ball name
            ballName = ball.DEVICE.getName();

            // Create a new ImpactData object to hold the data
            if (dataType == 2)
                impactData = new ImpactData(numSamples);
            else
                Log.w(TAG, "Data Transmission begun with a data type other than 2: " + dataType);

//            if (dataType == 1)
//                typeOneData = new TypeOneData(numSamples);
//            else if (dataType == 2)
//                typeTwoData = new TypeTwoData(numSamples);
        }
        else if (event == SmartBall.DataEvent.TRANSMISSION_ENDED)
        {
            isReading = false;

//            classifyImpacts();

//            if (dataType == 1)
//            {
//                if (typeOneData != null)
//                    typeOneData.setComplete(true);
//                else
//                    Log.w(TAG, "Type 1 data transmit ended but type 1 data is null");
//            }
//            else if (dataType == 2)
//            {
//                if (typeTwoData != null)
//                    typeTwoData.setComplete(true);
//                else
//                    Log.w(TAG, "Type 2 data transmit ended but type 1 data is null");
//            }
        }
        else if (event == SmartBall.DataEvent.TRANSMISSION_CANCELLED)
        {
//            if (dataType == 1)
//            {
//                if (typeOneData != null)
//                    typeOneData.setComplete(false);
//                else
//                    Log.w(TAG, "Type 1 data transmit cancelled but type 1 data is null");
//            }
//            else if (dataType == 2)
//            {
//                if (typeTwoData != null)
//                    typeTwoData.setComplete(false);
//                else
//                    Log.w(TAG, "Type 2 data transmit cancelled but type 1 data is null");
//            }

            isReading = false;
            wasCancelled = true;
        }
    }

    /**
     * Writes this Impact to the given directory. A new File will be created for this Impact in the given directory.
     * @param dir The directory to write to
     * @param saveGs Whether or not to save the data converted to Gs
     * @param saveRaw Whether or not to save the raw data values
     * @return Whether or not this Impact was successfully saved
     */
    public boolean save(File dir, boolean saveGs, boolean saveRaw)
    {
        if (impactData == null)
        {
            Log.e(TAG, "Could not save Impact: ImpactData is null");
            return false;
        }
        else if (!dir.isDirectory())
        {
            Log.e(TAG, "Could not save Impact: Save directory is not valid");
            return false;
        }
        else
        {
            try
            {
                File file;

                if (saveGs)
                {
                    file = new File(dir, createFileName(false));
                    impactData.toCSVFile(file, false);
                }

                if (saveRaw)
                {
                    file = new File(dir, createFileName(true));
                    impactData.toCSVFile(file, true);
                }
            }
            catch (FileNotFoundException e)
            {
                Log.e(TAG, "Error saving Impact: " + e.getMessage());
                return false;
            }

            return true;
        }
    }

    /**
     * Creates a file name for this Impact. Names are formatted SBDATA_BallId_DateYYYYDDMMHHMMSS.
     * @param raw Whether or not the file name will refer to a file containing raw data
     * @return The file name
     */
    private String createFileName(boolean raw)
    {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(getTime());
        String name = (raw ? "RAW_" : "");

        if (impactData.NUM_SAMPLES_REQUESTED < 0)
            name = "CONT_" + name;

        return name + "SBDATA_" + ballName + String.format(Locale.ENGLISH, "_%04d%02d%02d_%02d%02d%02d.csv", c.get(Calendar.YEAR),
                c.get(Calendar.DAY_OF_MONTH), 1 + c.get(Calendar.MONTH),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
    }

//    private void classifyImpacts()
//    {
//        if (impactData == null)
//            return;
//
//        ArrayList<ImpactRegion> regions = findImpactRegions(impactData);
//
//        Log.d(TAG, String.format(Locale.ENGLISH, "Found %d regions", regions.size()));
//
//        // hardSoftValue, hitDropValue
//        double[] result;
//
//        for (ImpactRegion region: regions)
//        {
//            result = Correlator.evaluate(impactData.toDataSeriesFeaturable(region));
//
//            Log.d(TAG, String.format(Locale.ENGLISH, "Hard Soft = %f, Hit Drop = %f", result[0], result[1]));
//        }
//    }
}
