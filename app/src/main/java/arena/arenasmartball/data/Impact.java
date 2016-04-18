package arena.arenasmartball.data;

import android.util.Log;

import arena.arenasmartball.ball.SmartBall;

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

    // Records the last data type that this Impact read
    private int lastDataTypeRead;

    // The time of the impact, in milliseconds from Jan 1, 1970
    private long time;

    // The type 1 data of this Impact
    private TypeOneData typeOneData;

    // The type 2 data of this Impact
    private TypeTwoData typeTwoData;

    /**
     * Creates a new Impact.
     */
    public Impact()
    {
        time = System.currentTimeMillis();
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
        if (typeOneData == null && typeTwoData == null)
            return false;

        boolean complete = true;

        if (typeOneData != null && !typeOneData.isComplete())
            complete = false;

        if (typeTwoData != null && !typeTwoData.isComplete())
            complete = false;

        return complete;
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
     * Gets the TypeOneData of this Impact, may be null.
     * @return The TypeOneData of this Impact, may be null
     */
    public TypeOneData getTypeOneData()
    {
        return typeOneData;
    }

    /**
     * Gets the TypeTwoData of this Impact, may be null.
     * @return The TypeTwoData of this Impact, may be null
     */
    public TypeTwoData getTypeTwoData()
    {
        return typeTwoData;
    }

    /**
     * Gets the KickData of this Impact that is still being read from the SmartBall.
     * @return The KickData of this Impact that is still being read from the SmartBall, or null if this Impact is not reading
     */
    public RawImpactData getDataInTransit()
    {
        if (isReading)
        {
            if (lastDataTypeRead == 1)
            {
                if (!typeOneData.isComplete())
                    return typeOneData;
            }
            else if (lastDataTypeRead == 2)
            {
                if (!typeTwoData.isComplete())
                    return typeTwoData;
            }

            return null;
        }
        else
            return null;
    }

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
        lastDataTypeRead = type;
        RawImpactData idata = getDataInTransit();

        if (idata != null && !idata.isComplete())
            idata.addLine(data, start, end);
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
//        if (dataType == 1 && typeOneData != null && typeOneData.isComplete())
//            return;
//
//        if (dataType == 2 && typeTwoData != null && typeTwoData.isComplete())
//            return;

        if (event == SmartBall.DataEvent.TRANSMISSION_REQUESTED)
            isReading = true;
        else if (event == SmartBall.DataEvent.TRANSMISSION_BEGUN)
        {
            wasCancelled = false;
            isReading = true;

            if (dataType == 1)
                typeOneData = new TypeOneData(numSamples);
            else if (dataType == 2)
                typeTwoData = new TypeTwoData(numSamples);
        }
        else if (event == SmartBall.DataEvent.TRANSMISSION_ENDED)
        {
            isReading = false;

            if (dataType == 1)
            {
                if (typeOneData != null)
                    typeOneData.setComplete(true);
                else
                    Log.w(TAG, "Type 1 data transmit ended but type 1 data is null");
            }
            else if (dataType == 2)
            {
                if (typeTwoData != null)
                    typeTwoData.setComplete(true);
                else
                    Log.w(TAG, "Type 2 data transmit ended but type 1 data is null");
            }
        }
        else if (event == SmartBall.DataEvent.TRANSMISSION_CANCELLED)
        {
            if (dataType == 1)
            {
                if (typeOneData != null)
                    typeOneData.setComplete(false);
                else
                    Log.w(TAG, "Type 1 data transmit cancelled but type 1 data is null");
            }
            else if (dataType == 2)
            {
                if (typeTwoData != null)
                    typeTwoData.setComplete(false);
                else
                    Log.w(TAG, "Type 2 data transmit cancelled but type 1 data is null");
            }

            isReading = false;
            wasCancelled = true;
        }
    }
}
