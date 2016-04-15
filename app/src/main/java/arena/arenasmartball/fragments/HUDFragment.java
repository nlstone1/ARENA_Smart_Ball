package arena.arenasmartball.fragments;

import android.app.Fragment;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import arena.arenasmartball.BluetoothBridge;
import arena.arenasmartball.MainActivity;
import arena.arenasmartball.PeriodicUpdateThread;
import arena.arenasmartball.R;
import arena.arenasmartball.Utils;
import arena.arenasmartball.ball.GattCommand;
import arena.arenasmartball.ball.GattCommandSequence;
import arena.arenasmartball.ball.Services;
import arena.arenasmartball.ball.SmartBall;
import arena.arenasmartball.ball.SmartBallConnection;

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

//    // The charging flag bundle id
//    private static String CHARGING_FLAG_BUNDLE_ID = "HUDFragment.CF";
//
//    // The batter level bundle id
//    private static String BATTERY_LEVEL_BUNDLE_ID = "HUDFragment.BL";

    // Denotes whether or not the ball is charging
    private static boolean ballIsCharging;

    // Records the battery level of the ball
    private static double batteryLevel;

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

    // Thread to handle periodically updating
    private StatsUpdater updaterThread;

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
        updaterThread = new StatsUpdater();
        updaterThread.start();

        // Set Default values
        setValuesForCurrentState(MainActivity.getBluetoothBridge());
        MainActivity.getBluetoothBridge().addBluetoothBridgeStateChangeListener(this);

        return view;
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

        BluetoothBridge bridge = MainActivity.getBluetoothBridge();
        bridge.removeBluetoothBridgeStateChangeListener(this);

        if (bridge.getSmartBallConnection() != null)
            bridge.getSmartBallConnection().removeSmartBallConnectionListener(this);

        updaterThread.kill();
        updaterThread = null;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        updaterThread.block();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (MainActivity.getBluetoothBridge().getState() == BluetoothBridge.State.CONNECTED)
            updaterThread.reset();

        // Set values
        BluetoothBridge bridge = MainActivity.getBluetoothBridge();
        bridge.addBluetoothBridgeStateChangeListener(this);
        setValuesForCurrentState(bridge);
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
        final int level = Utils.getRSSISignalStrength(rssi);
        setRSSIViews(level);
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
        if (sequence.NAME.equals(READ_BALL_BATTERY_COM_SEQ_NAME))
        {
            // In the event of failure try to read again
            if (event == GattCommandSequence.Event.ENDED_EARLY || event == GattCommandSequence.Event.FAILED_TO_BEGIN)
            {
                Log.w(TAG, "Error starting command sequence to read ball battery info: " + event);
            }
        }
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
        if (status == BluetoothGatt.GATT_FAILURE)
            return;

        if (id.equals(Services.Characteristic.BATTERY.name()))
        {
            byte batteryLevelByte = (data == null || data.length == 0 ? 0 : data[0]);
            batteryLevel = Math.ceil(0.0085 * batteryLevelByte * batteryLevelByte + 0.143 * batteryLevelByte);
        }
        else if (id.equals(Services.Characteristic.CHARGING_STATE.name()))
        {
            ballIsCharging = (data[0] > 0 && data[1] == 0);
        }

        final boolean charging = ballIsCharging;
        final double level = batteryLevel;

        try
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    setBatteryViews(level, charging);
                }
            });
        }
        catch (Exception e)
        { /* Ignore */ }
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

        if (bridge.getState() == BluetoothBridge.State.CONNECTED)
            updaterThread.reset();
    }

    /*
     * Sets the values for the views of this Fragment for the current state
     */
    private void setValuesForCurrentState(BluetoothBridge bridge)
    {
        final SmartBallConnection connection = bridge.getSmartBallConnection();

        if (connection == null)
        {
            setValuesForNullConnection();
            updaterThread.block();
            return;
        }

        connection.addSmartBallConnectionListener(this);

        if (connection.getConnectionState() == SmartBallConnection.ConnectionState.CONNECTED)
            updaterThread.unblock();
        else
            updaterThread.block();

        if (getActivity() != null)
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    // Set Background Color
                    if (connection.getConnectionState() == SmartBallConnection.ConnectionState.DISCONNECTED)
                        view.setBackgroundResource(R.color.colorStatusFailure);
                    else if (connection.getConnectionState() == SmartBallConnection.ConnectionState.CONNECTED)
                        view.setBackgroundResource(R.color.colorStatusSuccess);
                    else
                        view.setBackgroundResource(R.color.colorStatusNeutral);

                    // Set name
                    nameView.setText(connection.getSmartBall().DEVICE.getName());

                    // Set connection status
                    connectionView.setText(connection.getConnectionState().displayName);
                    connectionView.setVisibility(View.VISIBLE);

                    // Show progress circle if needed
                    progressView.setVisibility(
                            connection.getConnectionState() == SmartBallConnection.ConnectionState.CONNECTING ||
                            connection.getConnectionState() == SmartBallConnection.ConnectionState.DISCONNECTING ?
                            View.VISIBLE : View.GONE);

                    // Hide stats view if not connected
                    statsView.setVisibility(
                            connection.getConnectionState() == SmartBallConnection.ConnectionState.CONNECTED ?
                            View.VISIBLE : View.GONE);

                }
            });
    }

    /**
     * Sets the view values for when there is a null connection.
     */
    private void setValuesForNullConnection()
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
     * Sets the values of the battery related views.
     */
    private void setBatteryViews(final double batteryLevel, final boolean ballIsCharging)
    {
        Log.w(TAG, "Battery View set: level = " + batteryLevel + ", Charging = " + ballIsCharging);

        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                batteryView.setText(Utils.getPercentString(batteryLevel));
                batteryIcon.setImageLevel((int) Math.round(batteryLevel) + (ballIsCharging ? 1000 : 0));
            }
        });
    }

    /*
     * Sets the values of the rssi related views.
     */
    private void setRSSIViews(final int level)
    {
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

    /*
     * Creates and executes a CommandSequence to read the rssi of the SmartBall.
     */
    private void requestReadBallRSSI()
    {
        // Read rssi
        SmartBallConnection connection = MainActivity.getBluetoothBridge().getSmartBallConnection();

        if (connection != null && connection.getBluetoothGatt() != null)
            connection.getBluetoothGatt().readRemoteRssi();
    }

    /*
     * Creates and executes a CommandSequence to read the battery level of the SmartBall.
     * @param ball The SmartBall whose info to read
     */
    private void requestReadBallBattery()
    {
        SmartBall ball = MainActivity.getBluetoothBridge().getSmartBall();

        if (ball == null)
            return;

        // Create the command sequence
        GattCommandSequence sequence = new GattCommandSequence(READ_BALL_BATTERY_COM_SEQ_NAME, this);

        // Get the characteristics
        BluetoothGattCharacteristic battChar = ball.getCharacteristic(Services.Characteristic.BATTERY);
        BluetoothGattCharacteristic chargeChar = ball.getCharacteristic(Services.Characteristic.CHARGING_STATE);

        // Add the command to read the battery level
        if (battChar != null)
            sequence.addCommand(new GattCommand.ReadGattCommand<>
                (battChar, Services.Characteristic.BATTERY.name(), this));

        // Add the command to read the charging state
        if (chargeChar != null)
            sequence.addCommand(new GattCommand.ReadGattCommand<>
                (chargeChar, Services.Characteristic.CHARGING_STATE.name(), this));

        // Execute the sequence
        ball.addCommandSequenceToQueue(sequence);
    }

    /**
     * Thread for periodically updating the HUDFragment.
     *
     * Created by Theodore on 4/6/2016.
     */
    public class StatsUpdater extends PeriodicUpdateThread
    {
        // The delay for reading the battery
        private final int BATTERY_READ_DELAY_S = 10;

        // The timer for reading the battery level
        private int batteryReadTimer;

        /**
         * Constructor.
         */
        public StatsUpdater()
        {
            super(1000L);
        }

        /**
         * Called when this Thread is updates.
         */
        @Override
        public void onUpdate()
        {
            // Read battery
            if (--batteryReadTimer <= 0)
            {
                batteryReadTimer = BATTERY_READ_DELAY_S;
                requestReadBallBattery();
            }

            // Read rssi
            requestReadBallRSSI();
        }

        /**
         * Resets the delay timer and blocked status (to unblocked) of this Thread.
         */
        @Override
        public void reset()
        {
            batteryReadTimer = 0;
            super.reset();
        }
    }
}
