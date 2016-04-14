package arena.arenasmartball.ball;

import java.util.UUID;

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
        GENERIC_ACCESS ("1800"),
        GENERIC_ATTRIBUTE ("1801"),
        DEVICE_INFORMATION ("180A"),
        BATTERY_SERVICE ("180F"),
        SMART_BALL_SERVICE ("AD04");

        /** The UUID of the service containing this Characteristic. */
        public final UUID _UUID;

        /**
         * Service constructor.
         * @param serviceUUID The four character representation
         */
        Service(String serviceUUID)
        {
            _UUID = createSmartBallUUID(serviceUUID);
        }
    }

    /** Identifiers for useful characteristics on the SmartBall. */
    public enum Characteristic
    {
        DEVICE_NAME (Service.GENERIC_ACCESS, "2A00"),
        MODEL_NUMBER (Service.DEVICE_INFORMATION, "2A24"),
        SERIAL_NUMBER (Service.DEVICE_INFORMATION, "2A25"),
        FIRMWARE_VERSION (Service.DEVICE_INFORMATION, "2A26"),
        SOFTWARE_REVISION (Service.DEVICE_INFORMATION, "2A28"),
        BATTERY (Service.BATTERY_SERVICE, "2A19"),
        KICK_BIT (Service.SMART_BALL_SERVICE, "AD14"),
        COMMAND_FIELD (Service.SMART_BALL_SERVICE, "AD15"),
        //FIRMWARE_WRITE_FIELD ("AD04", "AD16"),
        DATA_CALLBACK (Service.SMART_BALL_SERVICE, "AD17"),
        SAMPLE_RATE (Service.SMART_BALL_SERVICE, "AD20"),
        CHARGING_STATE (Service.SMART_BALL_SERVICE, "AD1F"),
        COMMAND_CALLBACK (Service.SMART_BALL_SERVICE, "ADFE"),
        TIMEOUT_COUNTER (Service.SMART_BALL_SERVICE, "AD33");

        /** The Service containing this Characteristic. */
        public final Service SERVICE;

        /** The UUID of this Characteristic. */
        public final UUID _UUID;

        /**
         * Characteristic constructor.
         * @param service The Service
         * @param charUUID The four character representation
         */
        Characteristic(Service service, String charUUID)
        {
            SERVICE = service;
            _UUID = createSmartBallUUID(charUUID);
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
}
