package arena.arenasmartball.data;

import arena.arenasmartball.ball.SmartBall;

/**
 * Class representing a SmartBall impact.
 * Created by Theodore on 4/16/2016.
 */
public class Impact implements SmartBall.DataListener
{
    // Whether or not this Impact was cancelled
    private boolean wasCancelled;

    // Whether or not this Impact is currently reading data from the SmartBall
    private boolean isReading;

    // The time of the impact, in milliseconds from Jan 1, 1970
    private long time;

    // The type 1 data of this Impact
    private TypeOneData typeOneData;

    // The type 2 data of this Impact
    private TypeTwoData typeTwoData;

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
    public KickData getDataInTransit()
    {
        if (isReading)
        {
            if (typeOneData != null)
            {
                if (!typeOneData.isComplete())
                    return typeOneData;
            }

            if (typeTwoData != null)
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
                typeOneData.setComplete(true);
            else if (dataType == 2)
                typeTwoData.setComplete(true);
        }
        else if (event == SmartBall.DataEvent.TRANSMISSION_CANCELLED)
        {
            if (dataType == 1)
                typeOneData.setComplete(false);
            else if (dataType == 2)
                typeTwoData.setComplete(false);

            isReading = false;
            wasCancelled = true;
        }
    }
}
