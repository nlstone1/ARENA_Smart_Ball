package arena.arenasmartball.ball;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.StringRes;

import java.util.UUID;

import arena.arenasmartball.R;

/**
 * Static class containing Service and Characteristic enums.
 *
 * Created by Nathaniel on 4/13/2016.
 */
public class Services
{
    /** Identifiers for principal Services used by the SmartBall */
    public enum Service
    {
        GENERIC_ACCESS ("1800", R.string.generic_access),
        GENERIC_ATTRIBUTE ("1801", R.string.generic_attribute),
        DEVICE_INFORMATION ("180A", R.string.device_information),
        BATTERY_SERVICE ("180F", R.string.battery_service),
        SMART_BALL_SERVICE ("AD04", R.string.smart_ball_service);

        /** The UUID of the service containing this Characteristic. */
        public final UUID _UUID;

        /** String resource for the name of this Service */
        public final int NAME_RES_ID;

        /**
         * Service constructor.
         * @param serviceUUID The four character representation
         * @param nameResId The name
         */
        Service(String serviceUUID, @StringRes int nameResId)
        {
            _UUID = createSmartBallUUID(serviceUUID);
            NAME_RES_ID = nameResId;
        }
    }

    /** Identifiers for useful characteristics on the SmartBall. */
    public enum Characteristic
    {
        DEVICE_NAME (Service.GENERIC_ACCESS, "2A00", R.string.device_name),
        APPEARANCE (Service.GENERIC_ACCESS, "2A01", R.string.appearance),
        PPCP (Service.GENERIC_ACCESS, "2A04", R.string.ppcp),

        SYSTEM_ID (Service.DEVICE_INFORMATION, "2A23", R.string.system_id),
        MODEL_NUMBER (Service.DEVICE_INFORMATION, "2A24", R.string.model_number),
        SERIAL_NUMBER (Service.DEVICE_INFORMATION, "2A25", R.string.serial_number),
        FIRMWARE_VERSION (Service.DEVICE_INFORMATION, "2A26", R.string.firmware_revision),
        SOFTWARE_REVISION (Service.DEVICE_INFORMATION, "2A28", R.string.software_revision),
        MANUFACTURER_NAME (Service.DEVICE_INFORMATION, "2A29", R.string.manufacturer_name),

        BATTERY (Service.BATTERY_SERVICE, "2A19", R.string.battery_service),

        KICK_EVENT (Service.SMART_BALL_SERVICE, "AD12", R.string.kick_event),
        KICK_BIT (Service.SMART_BALL_SERVICE, "AD14", R.string.logging),
        COMMAND_FIELD (Service.SMART_BALL_SERVICE, "AD15", R.string.command_field),
        FIRMWARE_WRITE_FIELD (Service.SMART_BALL_SERVICE, "AD16", R.string.firmware_write_field),
        DATA_CALLBACK (Service.SMART_BALL_SERVICE, "AD17", R.string.response_buffer),
        SAMPLE_RATE (Service.SMART_BALL_SERVICE, "AD20", R.string.sample_rate),
        CHARGING_STATE (Service.SMART_BALL_SERVICE, "AD1F", R.string.status),
        COMMAND_CALLBACK (Service.SMART_BALL_SERVICE, "ADFE", R.string.error),
        TIMEOUT_COUNTER (Service.SMART_BALL_SERVICE, "AD33", R.string.file_size);

        /** The Service containing this Characteristic. */
        public final Service SERVICE;

        /** The UUID of this Characteristic. */
        public final UUID _UUID;

        /** The String resource id for the name of this Characteristic */
        public final int NAME_RES_ID;

        /**
         * Characteristic constructor.
         * @param service The Service
         * @param charUUID The four character representation
         * @param nameResId The name
         */
        Characteristic(Service service, String charUUID, @StringRes int nameResId)
        {
            SERVICE = service;
            _UUID = createSmartBallUUID(charUUID);
            NAME_RES_ID = nameResId;
        }
    }

    /**
     * Takes the four unique characters of the _UUID String and creates a _UUID for a SmartBall (per the Bluetooth specs).
     * @param chars The four unique characters specifying the _UUID
     * @return A smart ball _UUID
     */
    public static UUID createSmartBallUUID(String chars)
    {
        return UUID.fromString("0000" + chars + "-0000-1000-8000-00805F9B34FB");
    }

    /**
     * Tests whether a Characteristic is read-only.
     * @param characteristic The Characteristic
     * @return True if the Characteristic can be read
     */
    public static boolean canRead(BluetoothGattCharacteristic characteristic)
    {
        return true;
//        return (characteristic.getPermissions() & BluetoothGattCharacteristic.PERMISSION_READ)
//                == BluetoothGattCharacteristic.PERMISSION_READ;
    }
}
