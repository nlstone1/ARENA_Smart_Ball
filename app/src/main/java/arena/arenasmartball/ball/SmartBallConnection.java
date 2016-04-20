package arena.arenasmartball.ball;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class representing a Bluetooth connection to a SmartBall. Instances of this class are created with a ScanResult
 * which is assumed to represent a valid SmartBall.
 *
 * Created by Theodore on 4/5/2016.
 */
public class SmartBallConnection extends BluetoothGattCallback
{
    /** Enum representing the state of this SmartBallConnection. */
    public enum ConnectionState
    {
        CONNECTED ("Connected"),
        DISCONNECTED ("Disconnected"),
        NOT_CONNECTED ("Not Connected"),
        CONNECTING ("Connecting…"),
        DISCONNECTING ("Disconnecting…");

        /** The display name of this ConnectionState. */
        public final String displayName;

        /**
         * Constructs a new ConnectionState.
         * @param displayName The display name of the ConnectionState
         */
        ConnectionState(String displayName)
        {
            this.displayName = displayName;
        }
    }

    // The tag for this class.
    private static final String TAG = "SmartBallConnection";

    // The state of this SmartBallConnection.
    private ConnectionState connectionState;

    // The SmartBall attached to this SmartBallConnection.
    private SmartBall smartBall;

    // The BluetoothGatt handling the connection.
    private BluetoothGatt bluetoothGatt;

    // The Set of SmartBallConnectionListener attached to this SmartBallConnection.
    private Set<SmartBallConnectionListener> listeners;

    /**
     * Constructs a new SmartBallConnection.
     * @param result The ScanResult to use to connect to
     */
    public SmartBallConnection(ScanResult result)
    {
        connectionState = ConnectionState.NOT_CONNECTED;
        smartBall = new SmartBall(this, result.getDevice());
        listeners = new HashSet<>();
    }

    /**
     * Gets the String representation of the given connection status code.
     * @param status The connection status code
     * @return A String representation of the given connection status code
     */
    public static String getConnectionStatusString(int status)
    {
        switch (status)
        {
            case BluetoothGatt.GATT_SUCCESS:
                return "Gatt Success";
            case BluetoothGatt.GATT_FAILURE:
                return "Gatt Failure";
            default:
                return "Unknown Status (" + status + ")";
        }
    }

    /**
     * Gets the String representation of the given connection state code.
     * @param state The connection state code
     * @return A String representation of the given connection state code
     */
    public static String getConnectionStateString(int state)
    {
        switch (state)
        {
            case BluetoothProfile.STATE_CONNECTED:
                return "State Connected";
            case BluetoothProfile.STATE_CONNECTING:
                return "State Connecting";
            case BluetoothProfile.STATE_DISCONNECTED:
                return "State Disconnected";
            case BluetoothProfile.STATE_DISCONNECTING:
                return "State Disconnecting";
            default:
                return "Unknown State (" + state + ")";
        }
    }

    /**
     * Gets the SmartBall attached to this SmartBallConnection.
     * @return The SmartBall attached to this SmartBallConnection
     */
    public SmartBall getSmartBall()
    {
        return smartBall;
    }

    /**
     * Gets the BluetoothGatt behind this SmartBallConnection. Will be null if not connected.
     * @return The bluetoothGatt behind this SmartBallConnection
     */
    public BluetoothGatt getBluetoothGatt()
    {
        return bluetoothGatt;
    }

    /**
     * Gets the current ConnectionState of this SmartBallConnection.
     * @return The current ConnectionState of this SmartBallConnection
     */
    public ConnectionState getConnectionState()
    {
        return connectionState;
    }

    /**
     * Adds a SmartBallConnectionListener to this SmartBallConnection.
     * @param listener The listener to add
     */
    public void addSmartBallConnectionListener(SmartBallConnectionListener listener)
    {
        listeners.add(listener);
    }

    /**
     * Removes a SmartBallConnectionListener from this SmartBallConnection.
     * @param listener The listener to remove
     */
    public void removeSmartBallConnectionListener(SmartBallConnectionListener listener)
    {
        listeners.remove(listener);
    }

    /**
     * Called when the user wants to open connection to the SmartBall.
     * @param context The current Context
     */
    public void connect(Context context)
    {
        connect(context, null);
    }

    /**
     * Called when the user wants to open connection to the SmartBall.
     * @param context The current Context
     * @param listener The SmartBallConnectionListener to use to listen for events, may be null
     */
    public void connect(Context context, SmartBallConnectionListener listener)
    {
        // Add listener
        if (listener != null)
            addSmartBallConnectionListener(listener);

        // Quit if already connected
        if (connectionState == ConnectionState.CONNECTED)
            return;

        // Begin connecting
        bluetoothGatt = smartBall.DEVICE.connectGatt(context, true, this);
        setConnectionState(ConnectionState.CONNECTING);
    }

    /**
     * Called when the user wants to close the connection to the SmartBall.
     */
    public void disconnect()
    {
        disconnect(null);
    }

    /**
     * Called when the user wants to close the connection to the SmartBall.
     * @param listener The SmartBallConnectionListener to use to listen for events, may be null
     */
    public void disconnect(SmartBallConnectionListener listener)
    {
        // Add listener
        if (listener != null)
            addSmartBallConnectionListener(listener);

        // Quit if already disconnected
        if (bluetoothGatt == null || connectionState == ConnectionState.DISCONNECTED)
            return;

        // Begin disconnecting
        ConnectionState oldState = getConnectionState();
        setConnectionState(ConnectionState.DISCONNECTING);
        bluetoothGatt.disconnect();

        // Set to disconnected if was not previously connected
        if (oldState != ConnectionState.CONNECTED)
            setConnectionState(ConnectionState.NOT_CONNECTED);
    }

    /**
     * Method to request the rssi of this SmartBallConnection.
     * @return true if the request was sent successfully
     */
    public boolean requestRssi()
    {
        return bluetoothGatt != null && bluetoothGatt.readRemoteRssi();
    }

    /**
     * Callback indicating when GATT client has connected/disconnected to/from a remote GATT server.
     * @param gatt GATT client
     * @param status Status of the connect or disconnect operation. GATT_SUCCESS if the operation succeeds.
     * @param newState Returns the new bluetoothGatt state. Can be one of STATE_DISCONNECTED or STATE_CONNECTED
     */
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
    {
        Log.d(TAG, "Connection State Changed: New State = " + getConnectionStateString(newState) +
                ", Status = " + getConnectionStatusString(status));

        // Check whether the state change was successful
        if (status != BluetoothGatt.GATT_SUCCESS)
        {
            closeGatt();
            setConnectionState(ConnectionState.DISCONNECTED);
            return;
        }

        // Determine whether or not the connection was successful
        boolean connected = (newState == BluetoothProfile.STATE_CONNECTED);

        if (connected)
        {
            setConnectionState(ConnectionState.CONNECTED);

            // Begin Service discovery
            boolean begun = this.bluetoothGatt.discoverServices();

            // Close the connection if services discovery fails to begin
            if (!begun)
                disconnect(); // TODO Is there a better way to handle this?
        }
        else
        {
            setConnectionState(ConnectionState.NOT_CONNECTED);
            closeGatt();
        }
    }

    /**
     * Callback invoked when the list of remote services, characteristics and descriptors for the remote device
     * have been updated, ie new services have been discovered.
     * @param gatt GATT client invoked discoverServices()
     * @param status GATT_SUCCESS if the remote device has been explored successfully.
     */
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status)
    {
        Log.d(TAG, "Services discovered: " + gatt.getServices().size());

        BluetoothGattCharacteristic characteristic;
        List<BluetoothGattService> services = gatt.getServices();

        // Add Characteristics to repository
        for (BluetoothGattService service: services)
        {
            for (Services.Characteristic sc: Services.Characteristic.values())
            {
                if (sc.SERVICE._UUID.equals(service.getUuid()))
                {
                    characteristic = service.getCharacteristic(sc._UUID);

                    if (characteristic == null)
                        Log.w(TAG, "Characteristic not found: " + sc.name() + ": Device may not be a SmartBall");
                    else
                        smartBall.addCharacteristicToRepository(sc, characteristic);
                }
            }
        }
    }

    /**
     * Callback triggered as a result of a remote characteristic notification.
     * @param gatt GATT client the characteristic is associated with
     * @param characteristic Characteristic that has been updated as a result of a remote notification event.
     */
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
    {
        Log.d(TAG, "onCharacteristicChanged: " + characteristic.getUuid() +
                "\tValue = " + Arrays.toString(characteristic.getValue()));

        SmartBall.CharacteristicListener listener;
        Iterator<SmartBall.CharacteristicListener> it = null;

        if (smartBall.getCharacteristicListeners(characteristic.getUuid()) != null)
            it = smartBall.getCharacteristicListeners(characteristic.getUuid()).iterator();

        while (it != null && it.hasNext())
        {
            listener = it.next();
            listener.onCharacteristicChanged(gatt, characteristic);
        }
    }

    /**
     * Callback reporting the result of a characteristic read operation.
     * @param gatt GATT client invoked readCharacteristic(BluetoothGattCharacteristic)
     * @param characteristic	Characteristic that was read from the associated remote device.
     * @param status GATT_SUCCESS if the read operation was completed successfully.
     */
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
    {
        Log.d(TAG, "onCharacteristicRead (" + status + "): " + characteristic.getUuid() +
                "\tValue = " + Arrays.toString(characteristic.getValue()));
        handleCallbackCommand(characteristic.getValue(), true, true, status);
    }

    /**
     * Callback indicating the result of a characteristic write operation.
     * If this callback is invoked while a reliable write transaction is in progress, the value of the characteristic
     * represents the value reported by the remote device. An application should compare this value to the desired value
     * to be written. If the values don't match, the application must abort the reliable write transaction.
     * @param gatt GATT client invoked writeCharacteristic(BluetoothGattCharacteristic)
     * @param characteristic Characteristic that was written to the associated remote device.
     * @param status The result of the write operation GATT_SUCCESS if the operation succeeds
     */
    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
    {
        Log.d(TAG, "onCharacteristicWrite (" + status + "): " + characteristic.getUuid() +
                "\tValue = " + Arrays.toString(characteristic.getValue()));
        handleCallbackCommand(characteristic.getValue(), true, false, status);

        // Notify that the Characteristic was changed
        onCharacteristicChanged(gatt, characteristic);
    }

    /**
     * Callback reporting the result of a descriptor read operation.
     * @param gatt GATT client invoked readDescriptor(BluetoothGattDescriptor)
     * @param descriptor Descriptor that was read from the associated remote device.
     * @param status GATT_SUCCESS if the read operation was completed successfully
     */
    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
    {
        Log.d(TAG, "onDescriptorRead (" + status + "): " + descriptor.getUuid() +
                "\tValue = " + Arrays.toString(descriptor.getValue()));
        handleCallbackCommand(descriptor.getValue(), false, true, status);
    }

    /**
     * Callback reporting the result of a descriptor write operation.
     * @param gatt GATT client invoked readDescriptor(BluetoothGattDescriptor)
     * @param descriptor Descriptor that was written to the associated remote device.
     * @param status GATT_SUCCESS if the write operation was completed successfully
     */
    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
    {
        Log.d(TAG, "onDescriptorWrite (" + status + "): " + descriptor.getUuid() +
                "\tValue = " + Arrays.toString(descriptor.getValue()));
        handleCallbackCommand(descriptor.getValue(), false, false, status);
    }

    /**
     * Callback invoked when the rssi has been read.
     * @param gatt GATT client invoked readRssi(BluetoothGattDescriptor)
     * @param rssi The read rssi (in dbm)
     * @param status GATT_SUCCESS if the write operation was completed successfully
     */
    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
    {
        for (SmartBallConnectionListener listener: listeners)
            listener.onRssiRead(this, rssi);
    }

    /**
     * Helper method to close the GATT and set it to null.
     */
    private void closeGatt()
    {
        if (this.bluetoothGatt != null)
        {
            this.bluetoothGatt.close();
            this.bluetoothGatt = null;
        }
    }

    /**
     * Sets the connection state of this SmartBallConnection.
     * @param connectionState The new connection state
     */
    private void setConnectionState(ConnectionState connectionState)
    {
        // Set the connection state
        this.connectionState = connectionState;

        // Notify listeners
        for (SmartBallConnectionListener listener: listeners)
            listener.onConnectionStateChanged(this, connectionState);

        // Stop doing stiff that requires a connection
        if (connectionState != ConnectionState.CONNECTED)
        {
            // Make sure the SmartBall data transmit flag is reset
            smartBall.clearDataTransmitInProgressFlag();

            // Flush unfinished commands from the SmartBall command queue
            smartBall.flushCommandQueue();

            // Reset enable permissions
            GattCommand.reset();
        }
    }

    /**
     * Method to handle the SmartBall command sequences when one of the BluetoothGattCallback methods in this class
     * is triggered.
     * @param readValue The read value (if applicable)
     * @param isChar True if a Characteristic was accessed, false for a Descriptor
     * @param wasRead True if the bluetoothGatt object was read, false if was written
     * @param status The callback status
     */
    private void handleCallbackCommand(byte[] readValue, boolean isChar, boolean wasRead, int status)
    {
        GattCommand<?> command;
        GattCommandSequence sequence = smartBall.getCurrentCommandSequence();

        if (sequence == null)
            return;

        command = sequence.peek();

        // Placeholder variable
        GattCommand.ReadGattCommand.ReadGattCommandCallback callback;

        // Check whether the command references the appropriate Gatt object
        if (isChar && command.GATT_OBJECT instanceof BluetoothGattCharacteristic)
        {
            if (wasRead && command instanceof GattCommand.ReadGattCommand) // Read Characteristic
            {
                // Trigger user callback
                callback = ((GattCommand.ReadGattCommand)command).CALLBACK;

                if (callback != null)
                    callback.onCommandRead(((GattCommand.ReadGattCommand) command).ID, readValue, status);
            }
        }
        else if ((!isChar) && command.GATT_OBJECT instanceof BluetoothGattDescriptor)
        {
            if (wasRead && command instanceof GattCommand.ReadGattCommand) // Read Descriptor
            {
                // Trigger user callback
                callback = ((GattCommand.ReadGattCommand)command).CALLBACK;

                if (callback != null)
                    callback.onCommandRead(((GattCommand.ReadGattCommand) command).ID, readValue, status);
            }
        }

        sequence.pop();
        Log.d(TAG, "Command Sequence popped current command, continuing: " + sequence.NAME);

        if (sequence.isEmpty())
            smartBall.endCurrentCommandSequence(); // This will begin the next sequence if applicable
        else
            sequence.executeTop(bluetoothGatt);
    }

    /**
     * Interface for listening for events from SmartBallConnection.
     * Created by Theodore on 5/11/2015.
     */
    public interface SmartBallConnectionListener
    {
        /**
         * Called when the state of this SmartBallConnection changes.
         * @param connection The SmartBallConnection
         * @param state The new state
         */
        void onConnectionStateChanged(SmartBallConnection connection, ConnectionState state);

        /**
         * Called when the rssi has been read.
         * @param connection The SmartBallConnection
         * @param rssi The read rssi (in dbm)
         */
        void onRssiRead(SmartBallConnection connection, int rssi);
    }
}
