package arena.arenasmartball.ball;

import android.util.Log;

import java.util.Arrays;

import arena.arenasmartball.MainActivity;
import arena.arenasmartball.data.Impact;
import arena.arenasmartball.data.ImpactData;
import arena.arenasmartball.fragments.DownloadFragment;

/**
 * Thread for managing simulated continuous reads from the Smart Ball.
 *
 * Created by Nathaniel on 10/28/2016.
 */

public class ContinuousReadController implements SmartBall.EventListener,
        GattCommandSequence.CommandSequenceCallback, SmartBall.DataListener
{
    // Whether reading continuously from the ball
    private boolean isRecording;

    // The currently read data
    private ImpactData data;

    // Target SmartBall
    private final SmartBall smartBall;

    // Log TAG String
    private static final String TAG = "CRC";

    // Number of samples to request at once
    private int numSamples;

    // Delay between kick event and data request
    private long delay;

    // Optional additional DataListener
    private SmartBall.DataListener listener;

    /**
     * Creates a ContinuousReadController for the specified SmartBall.
     * @param smartBall The SmartBall from which to read continuously
     */
    public ContinuousReadController(SmartBall smartBall)
    {
        super();

        this.smartBall = smartBall;

        data = new ImpactData(-1, true);

        numSamples = 1096;
        delay = 10L;
    }

    /**
     * Sets the optional DataListener.
     */
    public void setDataListener(SmartBall.DataListener listener)
    {
        this.listener = listener;
    }

    /**
     * Starts recording.
     */
    public void startRecording()
    {
        if (!isRecording)
        {
            isRecording = true;

            Log.d(TAG, "Start Recording");
            smartBall.addEventListener(this);
            smartBall.addDataListener(this);
            GattCommandUtils.executeKickCommandSequence(smartBall, this);
        }
    }

    /**
     * Stops recording.
     */
    public void stopRecording()
    {
        if (isRecording)
        {
            // Stop any active transmissions
            GattCommandUtils.executeEndTransmissionCommandSequence(smartBall, this);

            isRecording = false;
            smartBall.removeEventListener(this);
            smartBall.removeDataListener(this);
        }
    }

    /**
     * @return True if recording, false otherwise
     */
    public boolean isRecording()
    {
        return isRecording;
    }

    /**
     * Clears the currently read data.
     */
    public void clear()
    {
        data = new ImpactData(-1, true);
        if (listener != null)
        {
            listener.onSmartBallDataRead(smartBall, null, false, false, (byte)2);
        }
    }

    /**
     * Saves the current data.
     */
    public void save()
    {
        if (data.getNumSamples() > 0)
        {
            Impact impact = new Impact(data, (long)(data.SAMPLES.get(0).time * 1000.0),
                    MainActivity.getBluetoothBridge().getSmartBallConnection().getBluetoothGatt().getDevice().getName());
            DownloadFragment.saveImpact(impact);
        }
    }

    /**
     * @return ImpactData received thus far
     */
    public ImpactData getData()
    {
        return data;
    }

    @Override
    public void onCommandSequenceEvent(GattCommandSequence sequence, GattCommandSequence.Event event)
    {
        Log.d(TAG, "Command Sequence Event: " + sequence + ": " + event);
    }

    @Override
    public void onBallKickEvent(SmartBall ball, SmartBall.KickEvent event)
    {
        Log.d(TAG, "Smart Ball Event: " + event);

        if (isRecording && event == SmartBall.KickEvent.KICKED)
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try
                    {
                        Thread.sleep(delay);
                        GattCommandUtils.executeDataTransmitCommandSequence(smartBall, numSamples, 2, ContinuousReadController.this);
                        data.mark(System.currentTimeMillis() / 1000.0);
                    }
                    catch (InterruptedException ignore)
                    {   }
                }
            }).start();
        }
    }

    @Override
    public void onBallCharacteristicDiscoveryCompleted(SmartBall ball)
    {

    }

    @Override
    public void onSmartBallDataRead(final SmartBall ball, byte[] data, boolean start, boolean end, byte type)
    {
//        Log.d(TAG, "Data Read!: " + Arrays.toString(data));
        if (isRecording)
        {
            if (!(start || end))
            {
                this.data.addLine(data);
            }

            if (listener != null)
            {
                listener.onSmartBallDataRead(ball, data, start, end, type);
            }

            if (end)
            {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try
                        {
                            Thread.sleep(5L);
                            GattCommandUtils.executeKickCommandSequence(ball, ContinuousReadController.this);
                        }
                        catch (InterruptedException ignore)
                        {   }
                    }
                }).start();
            }
        }
    }

    @Override
    public void onSmartBallDataTransmissionEvent(SmartBall ball, byte dataType, SmartBall.DataEvent event, int numSamples)
    {

    }
}
