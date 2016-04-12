package arena.arenasmartball.fragments;

import android.app.Fragment;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import arena.arenasmartball.BluetoothBridge;
import arena.arenasmartball.MainActivity;
import arena.arenasmartball.R;
import arena.arenasmartball.Utils;
import arena.arenasmartball.ball.GattCommand;
import arena.arenasmartball.ball.GattCommandSequence;
import arena.arenasmartball.ball.SmartBall;
import arena.arenasmartball.ball.SmartBallConnection;
import arena.arenasmartball.views.ScannerView;

/**
 * Fragment containing the heads up display.
 * Created by Theodore on 4/9/2016.
 */
public class HUDFragment extends Fragment implements BluetoothBridge.BluetoothBridgeStateChangeListener,
        SmartBallConnection.SmartBallConnectionListener,
        GattCommand.ReadGattCommand.ReadGattCommandCallback,
        GattCommandSequence.CommandSequenceCallback
{
    // The tag for this class
    private static final String TAG = "HUDFragment";

    // The name of the command sequence used to read the SmartBall battery info
    private static final String READ_BALL_BATTERY_COM_SEQ_NAME = "HUDFragReadBallInfo";

    // Lock for the reader thread
    private static final Object BALL_INFO_READER_LOCK = new Object();

    // The parent View
    private View view;

    // The View containing the connection stats
    private View statsView;
    // The View displaying the connection progress
    private View progressView;

    // The TextView containing the name of the SmartBall
    private TextView nameView;
    // The TextView containing the connection status of the SmartBall
    private TextView connectionView;
    // The TextView containing the signal strength
    private TextView signalView;
    // The TextView containing the battery level
    private TextView batteryView;

    // The signal strength icon
    private ImageView signalIcon;
    // The battery level icon
    private ImageView batteryIcon;

    // Denotes whether or not the ball is charging
    private boolean ballIsCharging;

    // Records the battery level of the ball
    private double batteryLevel;

    // Thread to handle periodically reading from the SmartBall
    private BallInfoReaderThread readerThread;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_hud, container, false);

        // Assign views
        statsView = view.findViewById(R.id.layout_hud_stats);
        progressView = view.findViewById(R.id.progress_hud);
        nameView = (TextView)view.findViewById(R.id.textview_hud_name);
        connectionView = (TextView)view.findViewById(R.id.textview_hud_connection);
        batteryView = (TextView)view.findViewById(R.id.textview_hud_battery);
        signalView = (TextView)view.findViewById(R.id.textview_hud_rssi);
        batteryIcon = (ImageView)view.findViewById(R.id.imageview_hud_battery);
        signalIcon = (ImageView)view.findViewById(R.id.imageview_hud_rssi);

        // Create Thread
        readerThread = new BallInfoReaderThread();
        readerThread.isAsleep = true;
        readerThread.start();

        // Set Default values
        setDefaultValues();
        MainActivity.getBluetoothBridge().addBluetoothBridgeStateChangeListener(this);

        return view;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        readerThread.sleep(true);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        readerThread.sleep(false);

        // Set values
        MainActivity.getBluetoothBridge().addBluetoothBridgeStateChangeListener(this);
        setValuesForCurrentState(MainActivity.getBluetoothBridge());
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
//        readerThread.isRunning = false;
    }

    /**
     * Called when the state of this SmartBallConnection changes.
     *
     * @param connection The SmartBallConnection
     * @param state      The new state
     */
    @Override
    public void onConnectionStateChanged(SmartBallConnection connection, SmartBallConnection.ConnectionState state)
    {    }

    /**
     * Called when the rssi has been read.
     *
     * @param connection The SmartBallConnection
     * @param rssi       The read rssi (in dbm)
     */
    @Override
    public void onRssiRead(SmartBallConnection connection, int rssi)
    {
        final int level = WifiManager.calculateSignalLevel(rssi, 101);

        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                signalView.setText(Utils.getPercentString(level));
                signalIcon.setImageLevel(level);
            }
        });
    }

    /**
     * Called when the GattCommandSequence experiences some event.
     *
     * @param sequence The GattCommandSequence that has experienced the event
     * @param event    The Event describing the state of the command sequence
     */
    @Override
    public void onCommandSequenceEvent(GattCommandSequence sequence, GattCommandSequence.Event event)
    {
//        if (sequence.NAME.equals(READ_BALL_BATTERY_COM_SEQ_NAME))
//        {
//            // In the event of failure try to read again
//            if (event == GattCommandSequence.Event.ENDED_EARLY || event == GattCommandSequence.Event.FAILED_TO_BEGIN)
//            {
//                Log.w(TAG, "Error starting command sequence to read ball battery info: " + event + ", trying again...");
//                SmartBall ball = MainActivity.getBluetoothBridge().getSmartBall();
//
//                if (ball != null)
//                    requestReadBallInfo(ball);
//                else
//                    Log.w(TAG, "Error requesting ball battery info: ball is null");
//            }
//        }
    }

    /**
     * Called when the requested value is read.
     *
     * @param id     The id of the read command which issued this request
     * @param data   The read data
     * @param status The status of the callback
     */
    @Override
    public void onCommandRead(String id, byte[] data, int status)
    {
        if (id.equals(SmartBall.Characteristic.BATTERY.name()))
        {
            byte batteryLevelByte = (data == null || data.length == 0 ? 0 : data[0]);
            batteryLevel = 0.0085 * batteryLevelByte * batteryLevelByte + 0.143 * batteryLevelByte;
        }
        else if (id.equals(SmartBall.Characteristic.CHARGING_STATE.name()))
        {
            ballIsCharging = (data[0] == 1); // TODO may be wrong
        }

        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                setBatteryViews();
            }
        });
    }

    /**
     * Called when the state of the BluetoothBridge changes.
     *
     * @param bridge   The BluetoothBridge whose state changed
     * @param newState The new state
     * @param oldState The old state
     */
    @Override
    public void onBluetoothBridgeStateChanged(BluetoothBridge bridge, BluetoothBridge.State newState,
                                              BluetoothBridge.State oldState)
    {
        // Set values
        setValuesForCurrentState(bridge);
    }

    /*
     * Sets the values for the views of this Fragment for the current state
     */
    private void setValuesForCurrentState(BluetoothBridge bridge)
    {
        switch (bridge.getState())
        {
            case SCANNING:
                setScanningValues();
                break;
            case CONNECTION_CHANGING:
                setConnectionPendingValues(bridge.getSmartBall());
                break;
            case CONNECTED:
                setConnectionStableValues(bridge.getSmartBall());
                break;
            case DISCONNECTED:
                if (bridge.getSmartBall() == null)
                    setDefaultValues();
                else
                    setConnectionStableValues(bridge.getSmartBall());
                break;
            default:
                setDefaultValues();
                break;
        }

        // Wake up thread
        readerThread.sleep(false);
    }

    /*
     * Sets the default values of the views of this Fragment.
     */
    private void setDefaultValues()
    {
        if (getActivity() == null)
            return;

        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                view.setBackgroundResource(R.color.colorStatusNeutral);
                nameView.setText(SmartBallConnection.ConnectionState.NOT_CONNECTED.displayName);
                connectionView.setVisibility(View.GONE);
                progressView.setVisibility(View.GONE);
                statsView.setVisibility(View.GONE);
                batteryIcon.setImageLevel(0);
                signalIcon.setImageLevel(0);
            }
        });
    }

    /*
     * Sets the values when scanning is taking place.
     */
    private void setScanningValues()
    {
        if (getActivity() == null)
            return;

        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                view.setBackgroundResource(R.color.colorStatusNeutral);
                nameView.setText(getString(R.string.scanning));
                connectionView.setVisibility(View.GONE);
                progressView.setVisibility(View.VISIBLE);
                statsView.setVisibility(View.GONE);
            }
        });
    }

    /*
     * Sets the values when there is a SmartBall that is in the process of changing its connection state.
     */
    private void setConnectionPendingValues(@NonNull final SmartBall ball)
    {
        if (getActivity() == null)
            return;

        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                view.setBackgroundResource(R.color.colorStatusNeutral);
                nameView.setText(ball.DEVICE.getName());
                connectionView.setVisibility(View.VISIBLE);
                connectionView.setText(ball.CONNECTION.getConnectionState().displayName);
                progressView.setVisibility(View.VISIBLE);
                statsView.setVisibility(View.GONE);
            }
        });
    }

    /*
     * Sets the values when there is a SmartBall that is in a stable connection state.
     */
    private void setConnectionStableValues(@NonNull final SmartBall ball)
    {
        if (getActivity() == null)
            return;

        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                nameView.setText(ball.DEVICE.getName());
                connectionView.setVisibility(View.VISIBLE);
                connectionView.setText(ball.CONNECTION.getConnectionState().displayName);
                progressView.setVisibility(View.GONE);

                if (ball.CONNECTION.getConnectionState() == SmartBallConnection.ConnectionState.CONNECTED)
                {
                    view.setBackgroundResource(R.color.colorStatusSuccess);
                    statsView.setVisibility(View.VISIBLE);

                    // Query stats // TODO Have thread running?
                }
                else
                {
                    view.setBackgroundResource(R.color.colorStatusFailure);
                    statsView.setVisibility(View.GONE);
                }
            }
        });
    }

    /**
     * Sets the values of the battery related views.
     */
    private void setBatteryViews()
    {
        batteryView.setText(Utils.getPercentString(batteryLevel));
        batteryIcon.setImageLevel((int)Math.round(batteryLevel) + (ballIsCharging ? 1000 : 0));
    }

    /*
     * Creates and executes a CommandSequence to read the battery level SmartBall.
     * @param ball The SmartBall whose info to read
     */
    private void requestReadBallInfo(SmartBall ball)
    {
        Log.d(TAG, "Requesting to read SmartBall info...");

        // Read rssi
        SmartBallConnection connection = MainActivity.getBluetoothBridge().getSmartBallConnection();

        if (connection != null && connection.getBluetoothGatt() != null)
            connection.getBluetoothGatt().readRemoteRssi();

        // Create the command sequence
        GattCommandSequence sequence = new GattCommandSequence(READ_BALL_BATTERY_COM_SEQ_NAME, this);

        // Get the characteristics
        BluetoothGattCharacteristic battChar = ball.getCharacteristic(SmartBall.Characteristic.BATTERY);
        BluetoothGattCharacteristic chargeChar = ball.getCharacteristic(SmartBall.Characteristic.CHARGING_STATE);

        // Add the command to read the battery level
        if (battChar != null)
            sequence.addCommand(new GattCommand.ReadGattCommand<>
                (battChar, SmartBall.Characteristic.BATTERY.name(), this));

        // Add the command to read the charging state
        if (chargeChar != null)
            sequence.addCommand(new GattCommand.ReadGattCommand<>
                (chargeChar, SmartBall.Characteristic.CHARGING_STATE.name(), this));

        // Execute the sequence
        ball.addCommandSequenceToQueue(sequence);
    }

    /**
     * Class to handle periodically reading from the SmartBall.
     */
    private class BallInfoReaderThread extends Thread
    {
        // Whether or not is sleeping
        private boolean isAsleep;

        // True when running
        private boolean isRunning;

        // The SmartBall
        private SmartBall ball;

        /**
         * Sets whether or not this Runnable should sleep
         * @param sleep Whether or not to sleep
         */
        public void sleep(boolean sleep)
        {
            isAsleep = sleep;

            if (!isAsleep)
            {
                synchronized (BALL_INFO_READER_LOCK)
                {
                    BALL_INFO_READER_LOCK.notifyAll();
                }
            }
        }

        @Override
        public void run()
        {
            isRunning = true;

//            while (isRunning)
//            {
//                Log.d(TAG, "Update Thread loop start...");
//
//                // Get the SmartBall
//                ball = MainActivity.getBluetoothBridge().getSmartBall();
//
//                // Block if asleep
//                while (isAsleep)
//                {
//                    synchronized (BALL_INFO_READER_LOCK)
//                    {
//                        try
//                        {
//                            Log.d(TAG, "Update Thread waiting while asleep");
//                            BALL_INFO_READER_LOCK.wait();
//                        }
//                        catch (InterruptedException e)
//                        { /* Ignore */ }
//                    }
//                }
//
//                // Query the data
//                if (ball != null && ball.CONNECTION.getConnectionState() == SmartBallConnection.ConnectionState.CONNECTED)
//                {
//                    Log.d(TAG, "Update Thread requesting ball info...");
//                    requestReadBallInfo(ball);
//                }
//                else
//                    Log.w(TAG, "Update Thread: Global ball is null");
//
//                // Sleep
//                try
//                {
//                    Thread.sleep(1000L);
//                }
//                catch (InterruptedException e)
//                {/* Ignore */ }
//            }
        }
    }
}
