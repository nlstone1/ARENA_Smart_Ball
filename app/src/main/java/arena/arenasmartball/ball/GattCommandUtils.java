package arena.arenasmartball.ball;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Utility class for dealing with GattCommands and GattCommandSequences.
 *
 * Created by Theodore on 5/14/2015.
 */
public class GattCommandUtils
{
    /** The tag for this class */
    private static final String TAG = "GattCommandUtils";

    /** The identifier for the data read GattCommandSequence */
    public static final String END_DATA_TRANSMIT_SEQUENCE = "End Data Transmit Sequence";

    /** The identifier for the type 1 data read GattCommandSequence */
    public static final String DATA_TRANSMIT_SEQUENCE_1 = "Data Transmit Sequence 1";

    /** The identifier for the type 2 data read GattCommandSequence */
    public static final String DATA_TRANSMIT_SEQUENCE_2 = "Data Transmit Sequence 2";

    /** The identifier for the kick GattCommandSequence */
    public static final String KICK_SEQUENCE = "Kick Sequence";

    /**
     * Static class.
     */
    private GattCommandUtils()
    {   }

    /**
     * Convenience method to create and execute a single GattCommand.
     * @param ball The SmartBall on which to execute
     * @param command The command to execute
     * @param name The name for the command sequence
     * @param callback An optional callback to listen for GattCommandSequence events
     */
    public static void executeCommand(final SmartBall ball, GattCommand<?> command, @NonNull String name,
                                      GattCommandSequence.CommandSequenceCallback callback)
    {
        GattCommandSequence sequence = new GattCommandSequence(name, callback);
        sequence.addCommand(command);
        ball.addCommandSequenceToQueue(sequence);
    }

//    /**
//     * Creates and executes the command to read the data field of the SmartBall. Data will be sent via modification of
//     * Characteristic AD17. This method requests 566 samples of type 1 data.
//     * @param ball The SmartBall whose data field to read
//     */
//    public static void executeDataTransmitCommandSequence(final SmartBall ball,
//                                                          GattCommandSequence.CommandSequenceCallback callback)
//    {
//        executeDataTransmitCommandSequence(ball, ApplicationData.getNumType1PointsToCollect(), (byte) 1, callback);
//    }

    /**
     * Creates and executes the command to begin transmission of kick data from the SmartBall. Data will be sent
     * via modification of Characteristic AD17.
     * @param ball The SmartBall whose data field to read
     * @param numSamples The number of samples to read, capped at 1096
     * @param dataType The type of data to request
     */
    public static void executeDataTransmitCommandSequence(final SmartBall ball, int numSamples, byte dataType,
                                                          GattCommandSequence.CommandSequenceCallback callback)
    {
        // Calculate the num samples
        if (numSamples > 1096)
            numSamples = 1096;

        byte lower = (byte)(numSamples & 255);
        byte upper = (byte)((numSamples >> 8) & 255);

        GattCommandSequence sequence = new GattCommandSequence(
                dataType == 1 ? DATA_TRANSMIT_SEQUENCE_1 : DATA_TRANSMIT_SEQUENCE_2,
                callback);
        sequence.addCommand(new GattCommand.WriteGattCommand<>(
                ball.getCharacteristic(Services.Characteristic.COMMAND_FIELD),
                new byte[] {10, 0, 0, 0, 0, lower, upper, 0, 0, dataType}
        ));

        ball.addCommandSequenceToQueue(sequence);
    }

    /**
     * Creates and executes a new GattCommandSequence sequence to end data transmission.
     * @param ball The SmartBall to execute the sequence
     */
    public static void executeEndTransmissionCommandSequence(final SmartBall ball,
                                                             GattCommandSequence.CommandSequenceCallback callback)
    {
        GattCommandSequence sequence = new GattCommandSequence(END_DATA_TRANSMIT_SEQUENCE, callback);

        // Write a 3 to CommandField
        sequence.addCommand(new GattCommand.WriteGattCommand<>(
                ball.getCharacteristic(Services.Characteristic.COMMAND_FIELD),
                new byte[] {6}));

        ball.addCommandSequenceToQueue(sequence);
    }

    /**
     * Creates and executes a new GattCommandSequence kick sequence.
     * @param ball The SmartBall to execute the sequence
     */
    public static void executeKickCommandSequence(final SmartBall ball,
                                                  GattCommandSequence.CommandSequenceCallback callback)
    {
        // The  main sequence
        GattCommandSequence sequence = new GattCommandSequence(KICK_SEQUENCE, callback);

        // Temp vars
        BluetoothGattDescriptor descriptor;

        // Write Descriptor 2902 - KickBit
        descriptor = SmartBall.getDescriptor(ball.getCharacteristic(Services.Characteristic.KICK_BIT), "2902");

        if (descriptor != null)
            sequence.addCommand(new GattCommand.WriteGattCommand<>(descriptor, new byte[]{1, 0}));
        else
        {
            Log.w(TAG, "Could not find Descriptor 2902 in KICK_BIT characteristic");
            return;
        }

        // Write Descriptor 2902 - DataCallback
        descriptor = SmartBall.getDescriptor(ball.getCharacteristic(Services.Characteristic.DATA_CALLBACK), "2902");

        if (descriptor != null)
            sequence.addCommand(new GattCommand.WriteGattCommand<>(descriptor, new byte[]{1, 0}));
        else
        {
            Log.w(TAG, "Could not find Descriptor 2902 in DATA_CALLBACK characteristic");
            return;
        }

        // Write a 6 to CommandField
        sequence.addCommand(new GattCommand.WriteGattCommand<>(
                ball.getCharacteristic(Services.Characteristic.COMMAND_FIELD),
                new byte[] {6}));

        // Write a 3 to CommandField
        sequence.addCommand(new GattCommand.WriteGattCommand<>(
                ball.getCharacteristic(Services.Characteristic.COMMAND_FIELD),
                new byte[] {3}));

        ball.addCommandSequenceToQueue(sequence);
    }

    /**
     * Convenience method to create a WriteGattCommand.
     * @param characteristic The Characteristic to which to write
     * @param value The value to write
     * @return A new WriteGattCommand with the given parameters
     */
   /* public static GattCommand.WriteGattCommand newWriteCharacteristicCommand
                                                (BluetoothGattCharacteristic characteristic, byte[] value)
    {
        return new GattCommand.WriteGattCommand();
    }*/

    /**
     * Creates and executes the command to disconnect
     * @param ball The SmartBall whose data field to access
     */
    public static void executeDisconnectCommandSequence(final SmartBall ball, GattCommandSequence.CommandSequenceCallback callback)
    {
        GattCommandSequence sequence = new GattCommandSequence("Disconnect", callback);

        BluetoothGattCharacteristic characteristic = ball.getCharacteristic(Services.Characteristic.COMMAND_FIELD);

        if (characteristic != null)
            sequence.addCommand(new GattCommand.WriteGattCommand<>(
                    characteristic,
                    new byte[] {21}
            ));

        ball.addCommandSequenceToQueue(sequence);
    }
}
