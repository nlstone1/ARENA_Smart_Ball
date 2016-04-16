package arena.arenasmartball;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.util.Log;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import arena.arenasmartball.ball.Services;
import arena.arenasmartball.ball.SmartBall;
import arena.arenasmartball.ball.SmartBallConnection;
import arena.arenasmartball.ball.SmartBallScanner;

/**
 * A bridge from the Applications UI to the required Bluetooth Functionality.
 *
 * Created by Nathaniel on 4/5/2016.
 */
public class BluetoothBridge implements SmartBallConnection.SmartBallConnectionListener, SmartBall.EventListener
{
    /** The possible states of the BluetoothBridge. */
    public enum State
    {
        CONNECTION_CHANGING,
        CONNECTED,
        DISCONNECTED
    }

    /** The tag for this class. */
    public static final String TAG = "BluetoothBridge";

    /** Intent request code for enabling Bluetooth */
    public static final int REQUEST_ENABLE_BT = 0x1111;

    /** Constant indicating BT is not available on the current device */
    public static final int BT_NOT_AVAILABLE = -1;

    // The parent Activity
    private MainActivity activity;

    // The BluetoothAdapter
    private BluetoothAdapter bluetoothAdapter;

    // The SmartBallScanner to use to find balls
    private SmartBallScanner smartBallScanner;

    // The SmartBallConnection handle
    private SmartBallConnection smartBallConnection;

    // Records the time of the last impact recorded by the app, in milliseconds from Jan 1 1970
    private long timeOfLastImpact;

    // Records the time of the last download time from app, in milliseconds from Jan 1 1970
    private long timeOfLastDownload;

    // The BluetoothBridgeStateChangeListener attached to this BluetoothBridge
    private Set<BluetoothBridgeStateChangeListener> listeners;

    // Simply indicates if BT is enabled or not
    private boolean isReady;

    // The current state of this BluetoothBridge
    private State state;

    /**
     * Creates a BluetoothBridge.
     */
    public BluetoothBridge()
    {
        state = State.DISCONNECTED;
        listeners = new HashSet<>();
    }

    /**
     * Gets the time of the last impact, in milliseconds from Jan 1 1970.
     * @return The time of the last impact, or 0 if no impact has been recorded yet
     */
    public long getTimeOfLastImpact()
    {
        return timeOfLastImpact;
    }

    /**
     * Gets the time of the last download, in milliseconds from Jan 1 1970.
     * @return The time of the last download, or 0 if no impact has been recorded yet
     */
    public long getTimeOfLastDownload()
    {
        return timeOfLastDownload;
    }

    /**
     * Gets the current state of this BluetoothBridge.
     * @return The current state of this BluetoothBridge
     */
    public State getState()
    {
        return state;
    }

    /**
     * Adds a BluetoothBridgeStateChangeListener to this BluetoothBridge.
     * @param listener The BluetoothBridgeStateChangeListener to add
     */
    public void addBluetoothBridgeStateChangeListener(BluetoothBridgeStateChangeListener listener)
    {
        listeners.add(listener);
    }

    /**
     * Removes a BluetoothBridgeStateChangeListener from this BluetoothBridge.
     * @param listener The BluetoothBridgeStateChangeListener to remove
     */
    public void removeBluetoothBridgeStateChangeListener(BluetoothBridgeStateChangeListener listener)
    {
        listeners.remove(listener);
    }

    /**
     * Sets the Activity of this BluetoothBridge.
     * @param activity The new Activity
     */
    public void setActivity(MainActivity activity)
    {
        this.activity = activity;
    }

    /**
     * Gets the BluetoothAdapter of the device.
     * @return The BluetoothAdapter of the device
     */
    public BluetoothAdapter getBluetoothAdapter()
    {
        return bluetoothAdapter;
    }

    /**
     * Gets the SmartBallScanner attached to this BluetoothBridge.
     * @return The SmartBallScanner attached to this BluetoothBridge
     */
    public SmartBallScanner getSmartBallScanner()
    {
        return smartBallScanner;
    }

    /**
     * Gets the current SmartBallConnection.
     * @return The current SmartBallConnection
     */
    public SmartBallConnection getSmartBallConnection()
    {
        return smartBallConnection;
    }

    /**
     * Gets the SmartBall from the current SmartBallConnection, if it is non-null.
     * @return The SmartBall from the current SmartBallConnection, if it is non-null
     */
    public SmartBall getSmartBall()
    {
        if (smartBallConnection == null)
            return null;
        else
            return smartBallConnection.getSmartBall();
    }

    /**
     * Starts scanning.
     */
    public void startScanning()
    {
        Log.d(TAG, "Start Scanning called");
        smartBallScanner.startScanning();
    }

    /**
     * Stop scanning.
     */
    public void stopScanning()
    {
        Log.d(TAG, "Stop Scanning called");
        smartBallScanner.stopScanning();
    }

    /**
     * Opens a new connection to a ScanResult.
     * @param result The ScanResult
     */
    public void connect(ScanResult result)
    {
        // Make sure scanning has stopped
        smartBallScanner.stopScanning();

        // Close old connections if applicable
        if (smartBallConnection != null &&
            smartBallConnection.getConnectionState() == SmartBallConnection.ConnectionState.CONNECTED)
        {
            Log.w(TAG, "Forced to cancel old connection before opening new connection");
            smartBallConnection.disconnect();
        }

        // Connect
        smartBallConnection = new SmartBallConnection(result);
        smartBallConnection.connect(activity, this);
        smartBallConnection.getSmartBall().getEventListeners().add(this);
    }

    /**
     * Re-opens a previously closed connection if possible.
     */
    public void reconnect()
    {
        // Make sure scanning has stopped
        smartBallScanner.stopScanning();

        if (smartBallConnection == null)
        {
            Log.e(TAG, "Called reconnect but no previous connection exists!");
            return;
        }

        if (smartBallConnection.getConnectionState() == SmartBallConnection.ConnectionState.CONNECTED)
        {
            Log.w(TAG, "Forced to cancel old connection before reopening the connection");
            smartBallConnection.disconnect();
        }

        // Disconnect
        smartBallConnection.connect(activity, this);
        smartBallConnection.getSmartBall().getEventListeners().add(this);
    }

    /**
     * Disconnects from the current connection if applicable.
     */
    public void disconnect()
    {
        // Make sure scanning has stopped
        smartBallScanner.stopScanning();

        if (smartBallConnection == null)
        {
            Log.e(TAG, "Called disconnect but no previous connection exists!");
            return;
        }

        if (smartBallConnection.getConnectionState() == SmartBallConnection.ConnectionState.DISCONNECTED ||
            smartBallConnection.getConnectionState() == SmartBallConnection.ConnectionState.NOT_CONNECTED)
        {
            Log.w(TAG, "Called disconnect on an already closed connection");
        }

        // Disconnect
        smartBallConnection.disconnect();
    }

//    /**
//     * Sets the current SmartBallConnection.
//     * @param connection The new SmartBallConnection
//     */
//    public void setSmartBallConnection(SmartBallConnection connection)
//    {
//        smartBallConnection = connection;
//    }

    /**
     * Initializes the BluetoothBridge.
     * @return 0 on success or BT_NOT_AVAILABLE on failure if the device does not support bluetooth
     */
    public int initialize()
    {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null)
            return BT_NOT_AVAILABLE;

        // Create Scanner
        smartBallScanner = new SmartBallScanner(bluetoothAdapter);

        checkReady();

        return 0;
    }

    /**
     * Checks whether or not Bluetooth has been initialized.
     * @return Whether or not Bluetooth has been initialized
     */
    public boolean isReady()
    {
        return isReady;
    }

    /**
     * Sets the readiness of the BluetoothBridge.
     * @param isReady Whether BT is ready or not
     */
    public void setReady(boolean isReady)
    {
        this.isReady = isReady;

        if (isReady)
        {
            smartBallScanner.reinit(bluetoothAdapter);
        }
    }

    /**
     * Called when the state of this SmartBallConnection changes.
     *
     * @param connection The SmartBallConnection
     * @param state      The new state
     */
    @Override
    public void onConnectionStateChanged(SmartBallConnection connection, SmartBallConnection.ConnectionState state)
    {
        State oldState = this.state;

        // Make sure scanning has ended
        smartBallScanner.stopScanning();

        if (state == SmartBallConnection.ConnectionState.CONNECTED)
        {
//            this.state = State.CONNECTED;

            if (connection.getSmartBall().getNumCharacteristicsFound() == Services.Characteristic.values().length)
                this.state = State.CONNECTED;
            else
                return;
        }
        else if (state == SmartBallConnection.ConnectionState.DISCONNECTED ||
                 state == SmartBallConnection.ConnectionState.NOT_CONNECTED)
        {
            this.state = State.DISCONNECTED;
            // TODO make connection null?
        }
        else if (state == SmartBallConnection.ConnectionState.CONNECTING ||
                 state == SmartBallConnection.ConnectionState.DISCONNECTING)
        {
            this.state = State.CONNECTION_CHANGING;
        }

        // Notify Listeners
        for (BluetoothBridgeStateChangeListener listener: listeners)
            listener.onBluetoothBridgeStateChanged(this, this.state, oldState);
    }

    /**
     * Called when the rssi has been read.
     *
     * @param connection The SmartBallConnection
     * @param rssi       The read rssi (in dbm)
     */
    @Override
    public void onRssiRead(SmartBallConnection connection, int rssi)
    {    }

    /**
     * Called when the SmartBall experiences a kick event.
     *
     * @param ball  The SmartBall
     * @param event The kick event
     */
    @Override
    public void onBallKickEvent(SmartBall ball, SmartBall.KickEvent event)
    {
        if (event == SmartBall.KickEvent.KICKED)
        {
            timeOfLastImpact = System.currentTimeMillis();
        }
    }

    /**
     * Called when all BLE characteristics have been discovered.
     *
     * @param ball The SmartBall
     */
    @Override
    public void onBallCharacteristicDiscoveryCompleted(SmartBall ball)
    {
        State oldState = this.state;
        this.state = State.CONNECTED;

        // Notify Listeners
        for (BluetoothBridgeStateChangeListener listener: listeners)
            listener.onBluetoothBridgeStateChanged(this, this.state, oldState);
    }

    /*
     * Verifies that BT is ready, launching the request to turn BT on dialog if otherwise.
     */
    private boolean checkReady()
    {
        if (!(isReady || bluetoothAdapter.isEnabled()))
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

            return false;
        }

        return isReady = true;
    }

    /**
     * Interface for listening for changes to the BluetoothBridge.
     */
    public interface BluetoothBridgeStateChangeListener
    {
        /**
         * Called when the state of the BluetoothBridge changes.
         * @param bridge The BluetoothBridge whose state changed
         * @param newState The new state
         * @param oldState The old state
         */
        void onBluetoothBridgeStateChanged(BluetoothBridge bridge, State newState, State oldState);
    }
}
