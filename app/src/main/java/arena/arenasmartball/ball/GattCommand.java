package arena.arenasmartball.ball;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import java.util.TreeSet;
import java.util.UUID;

/**
 * A command to send to the SmartBall through the BluetoothGatt.
 * @param <T> either Characteristic or Descriptor.
 *
 * Created by Theodore on 5/12/2015.
 */

public class GattCommand<T>
{
    /** The tag for this class. */
    private static final String TAG = "GattCommand";

    /** Keeps track of which Characteristics have been enabled. */
    private static TreeSet<UUID> enabledCharacteristics = new TreeSet<>();

    /** The GattObject attached to this GattCommand. */
    public final T GATT_OBJECT;

    /**
     * Constructs a new GattCommand.
     * @param gattObject The Characteristic/Descriptor being accessed
     */
    private GattCommand(T gattObject)
    {
        GATT_OBJECT = gattObject;

        // Make sure that types agree
        boolean objectIsChar = gattObject instanceof BluetoothGattCharacteristic;
        boolean objectIsDesc = gattObject instanceof BluetoothGattDescriptor;

        if (!objectIsChar && !objectIsDesc)
            throw new IllegalArgumentException("GattObject must be either BluetoothGattCharacteristic or BluetoothGattDescriptor");
    }

    /**
     * Resets the global enabled characteristic set.
     */
    public static void reset()
    {
        enabledCharacteristics = new TreeSet<>();
    }

    /**
     * Enables notify on the specified gatt object of the given GattCommand.
     * @param gatt The BluetoothGatt
     * @param characteristic Will be non-null if the given GattCommand is of type Characteristic
     * @param descriptor Will be non-null if the given GattCommand is of type Descriptor
     * @return True if successful
     */
    public static boolean enableNotifies(BluetoothGatt gatt,
                                       BluetoothGattCharacteristic characteristic,
                                       BluetoothGattDescriptor descriptor)
    {
        boolean success = false;

        // Grab the characteristic if only descriptor held
        if (characteristic == null && descriptor != null)
            characteristic = descriptor.getCharacteristic();

        // Enable notifies
        if (characteristic != null)
        {
            // If not in the list -> not enabled
            if (enabledCharacteristics.add(characteristic.getUuid()))
            {
                success = gatt.setCharacteristicNotification(characteristic, true);
                Log.d(TAG, "Enabled characteristic " + characteristic.getUuid() + ": " + success);

                // Failed to enable
                if (!success)
                    enabledCharacteristics.remove(characteristic.getUuid());
            }
            // Already in the list -> already enabled
            else
                return true;
        }

        return success;
    }

    /**
     * Called when this GattCommand needs to be executed.
     * @param gatt The BluetoothGatt of the SmartBall
     * @param sequenceName The name of the sequence being executed
     * @return true on success
     */
    protected boolean execute(BluetoothGatt gatt, String sequenceName)
    {
        BluetoothGattCharacteristic characteristic = null;
        BluetoothGattDescriptor descriptor = null;

        // Initialize fields
        if (GATT_OBJECT instanceof BluetoothGattCharacteristic)
            characteristic = (BluetoothGattCharacteristic) GATT_OBJECT;
        else if (GATT_OBJECT instanceof BluetoothGattDescriptor)
            descriptor = (BluetoothGattDescriptor) GATT_OBJECT;
        else
        {
            Log.w(TAG, sequenceName + ": Attempted to execute a command with gatt object of unknown type: " +
                    GATT_OBJECT.getClass());
            return false;
        }

        // Enable notifies
        if (!enableNotifies(gatt, characteristic, descriptor))
        {
            Log.w(TAG, sequenceName + ": Failed to execute because enabled notifies failed");
            return false;
        }

        boolean result;

        // Handle each type of Command
        if (this instanceof ReadGattCommand)
        {
            if (GATT_OBJECT instanceof BluetoothGattCharacteristic)
            {
                result = gatt.readCharacteristic(characteristic);
                if (!result)
                    Log.w(TAG, "Failed to execute GattCommand: Unable to read characteristic");
                return result;
            }
            else
            {
                result = gatt.readDescriptor(descriptor);
                if (!result)
                    Log.w(TAG, "Failed to execute GattCommand: Unable to read descriptor");
                return result;
            }
        }
        else if (this instanceof WriteGattCommand)
        {
            if (GATT_OBJECT instanceof BluetoothGattCharacteristic)
            {
                characteristic.setValue(((WriteGattCommand) this).WRITE_VALUE);
                result = gatt.writeCharacteristic(characteristic);
                if (!result)
                    Log.w(TAG, "Failed to execute GattCommand: Unable to write characteristic");
                return result;
            }
            else
            {
                descriptor.setValue(((WriteGattCommand) this).WRITE_VALUE);
                result = gatt.writeDescriptor(descriptor);
                if (!result)
                    Log.w(TAG, "Failed to execute GattCommand: Unable to write descriptor");
                return result;
            }
        }
        return false;
    }

    /**
     * A GattCommand with a write value.
     * @param <T> either Characteristic or Descriptor.
     */
    public static class WriteGattCommand<T> extends GattCommand<T>
    {
        /** The write value of this WriteGattCommand. */
        public final byte[] WRITE_VALUE;

        /**
         * Constructs a new WriteGattCommand.
         * @param gattObject The Characteristic/Descriptor being accessed
         * @param writeValue The value to write
         */
        public WriteGattCommand(T gattObject, byte[] writeValue)
        {
            super (gattObject);
            WRITE_VALUE = writeValue;
        }
    }

    /**
     * A GattCommand to read a value.
     * @param <T> either Characteristic or Descriptor.
     */
    public static class ReadGattCommand<T> extends GattCommand<T>
    {
        /** The id of this ReadGattCommand. */
        public final String ID;

        /** The callback attached to this ReadGattCommand. */
        public final ReadGattCommandCallback CALLBACK;

        /**
         * Constructs a new ReadGattCommand.
         * @param gattObject The Characteristic/Descriptor being accessed
         * @param id The identifier for this ReadGattCommand
         * @param callback The ReadGattCallback listening for this ReadGattCommand
         */
        public ReadGattCommand(T gattObject, String id, ReadGattCommandCallback callback)
        {
            super (gattObject);
            ID = id;
            CALLBACK = callback;
        }

        /**
         * Callback for classes listening for ReadGattCommands.
         */
        public interface ReadGattCommandCallback
        {
            /**
             * Called when the requested value is read.
             * @param id The id of the read command which issued this request
             * @param data The read data
             * @param status The status of the callback
             */
            void onCommandRead(String id, byte[] data, int status);
        }
    }
}
