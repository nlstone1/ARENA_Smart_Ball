package arena.arenasmartball.ball;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

/**
 * Class representing a SmartBall.
 *
 * Created by Theodore on 5/11/2015.
 */

public class SmartBall
{
    /** Represents different kick events that the SmartBall may experience. */
    public enum KickEvent
    {
        READY_TO_KICK,
        KICKED
    }

    /** Represents different data transmission events. */
    public enum DataEvent
    {
        TRANSMISSION_BEGUN,
        TRANSMISSION_ENDED,
        TRANSMISSION_CANCELLED
    }

    /** The tag for this class. */
    private static final String TAG = "SmartBall";

    /** The SmartBall's BluetoothDevice. */
    public final BluetoothDevice DEVICE;

    /** The connection to the SmartBall. */
    public final SmartBallConnection CONNECTION;

    /** Records the kickBit value of this SmartBall. */
    private boolean kickBit;

    /** Flag to keep track of when data transmission has ended/begun. */
    private boolean dataTransmitInProgress;

    /** Denotes which type fo data is currently being transmitted. */
    private byte dataTypeInTransit;

    /** The command sequence currently in progress. */
    private LinkedList<GattCommandSequence> commandSequenceQueue;

    /**
     * A repository of all characteristics contained in the SmartBall, populated on service discovery with the key
     * SmartBall characteristics defined in Characteristic.
     */
    private HashMap<Services.Characteristic, BluetoothGattCharacteristic> characteristicRepository;

    /**
     * The array of CharacteristicListeners. Each Characteristic defined in the enum in this class may have a
     * corresponding List of listeners.
     */
    private HashMap<UUID, Set<CharacteristicListener>> characteristicListeners;

    /** Integer to record the number of characteristics that have been found. */
    private int numCharacteristicsFound;

    /** The set of event listeners attached to this SmartBall. */
    private Set<EventListener> eventListeners;

    /** The set of data listeners attached to this SmartBall. */
    private Set<DataListener> dataListeners;

    /**
     * Constructs a new SmartBall.
     * @param connection The SmartBallConnection to use to connect
     * @param device The BluetoothDevice representing the actual ball; assumed to be an actual Smart Ball
     */
    protected SmartBall(SmartBallConnection connection, BluetoothDevice device)
    {
        // Initialize
        DEVICE = device;
        CONNECTION = connection;
        eventListeners = new HashSet<>();
        dataListeners = new HashSet<>();
        commandSequenceQueue = new LinkedList<>();
        characteristicRepository = new HashMap<>();
        characteristicListeners = new HashMap<>();

        // Add CharacteristicListeners to listen for events
        createKickBitListener(this);
        createDataTransmitListener(this);
    }

    /**
     * Method to get the Descriptor belonging to the given Characteristic with the _UUID specified by the four unique
     * characters of a SmartBall _UUID.
     * @param characteristic The BluetoothGattCharacteristic whose descriptor to get
     * @param uuidIdChars The four unique characters specifying the _UUID
     * @return The Descriptor belonging to the given Characteristic
     */
    public static BluetoothGattDescriptor getDescriptor(BluetoothGattCharacteristic characteristic, String uuidIdChars)
    {
        if (characteristic != null)
            return characteristic.getDescriptor(Services.createSmartBallUUID(uuidIdChars));
        else
        {
            Log.e(TAG, "getDescriptor() passed a null characteristic");
            return null;
        }
    }

    /**
     * Gets the kick bit value of this SmartBall.
     * @return The kick bit value of this SmartBall
     */
    public boolean getKickBit()
    {
        return kickBit;
    }

    /**
     * Method to get the List of CharacteristicListeners listening on the Characteristic with the given _UUID.
     * @return The List of CharacteristicListeners listening on the Characteristic with the given _UUID
     */
    public Set<CharacteristicListener> getCharacteristicListeners(UUID uuid)
    {
        return characteristicListeners.get(uuid);
    }

    /**
     * Method to get the list of SmartBallEventListeners attached to this SmartBall.
     * @return The list of SmartBallEventListeners attached to this SmartBall
     */
    public Set<EventListener> getEventListeners()
    {
        return eventListeners;
    }

    /**
     * Method to determine whether or not this SmartBall is transmitting data.
     * @return Whether or not this SmartBall is transmitting data
     */
    public boolean isDataTransmitInProgress()
    {
        return dataTransmitInProgress;
    }

    /**
     * Method to flush all commands in the command queue of this SmartBall.
     */
    public void flushCommandQueue()
    {
        GattCommandSequence sequence;
        Iterator<GattCommandSequence> it = commandSequenceQueue.iterator();

        while (it.hasNext())
        {
            sequence = it.next();
            if (sequence.CALLBACK != null)
                sequence.CALLBACK.onCommandSequenceEvent(sequence, GattCommandSequence.Event.ENDED_EARLY);

            it.remove();
        }
    }

    /**
     * Clears the dataTransmitInProgress flag and notifies listeners.
     */
    public void clearDataTransmitInProgressFlag()
    {
        if (dataTransmitInProgress)
        {
            for (DataListener listener : dataListeners)
                listener.onSmartBallDataTransmissionEvent(this, dataTypeInTransit, DataEvent.TRANSMISSION_CANCELLED);
        }
        dataTransmitInProgress = false;
    }

    /**
     * Method to get the current data type in transit. If no data is being transmitted, -1 is returned.
     * @return The current data type in transit
     */
    public byte getDataTypeInTransit()
    {
        if (dataTransmitInProgress)
            return dataTypeInTransit;
        else
            return -1;
    }

    /**
     * Method to add a CharacteristicListener to listen for the given Characteristic.
     * @param listener The CharacteristicListener to add
     * @param characteristic The Characteristic for which to listen
     */
    public void addCharacteristicListener(CharacteristicListener listener, Services.Characteristic characteristic)
    {
        UUID uuid = characteristic._UUID;
        Set<CharacteristicListener> listeners = characteristicListeners.get(uuid);

        if (listeners == null)
        {
            listeners = new HashSet<>();
            characteristicListeners.put(uuid, listeners);
        }

        if (!listeners.contains(listener))
        {
            Log.d(TAG, "Added characteristic listener to existing listeners: " + characteristic.name());
            listeners.add(listener);
        }
        else
            Log.d(TAG, "Attempted to add characteristic listener already contained in list: " + characteristic.name());
    }

    /**
     * Method to remove a CharacteristicListener to listener from this SmartBall's list of CharacteristicListeners.
     * @param listener The CharacteristicListener to remove
     */
    public void removeCharacteristicListener(CharacteristicListener listener)
    {
        for (Set<CharacteristicListener> set: characteristicListeners.values())
            set.remove(listener);
    }

    /**
     * Method to add a EventListener.
     * @param listener The EventListener to add
     */
    public void addEventListener(EventListener listener)
    {
        eventListeners.add(listener);
    }

    /**
     * Method to remove the given SmartBallEventListener.
     * @param listener The SmartBallEventListener to remove
     */
    public void removeEventListener(EventListener listener)
    {
        eventListeners.remove(listener);
    }

    /**
     * Method to add a DataListener.
     * @param listener The DataListener to add
     */
    public void addDataListener(DataListener listener)
    {
        dataListeners.add(listener);
    }

    /**
     * Method to remove the given DataListener.
     * @param listener The DataListener to remove
     */
    public void removeDataListener(DataListener listener)
    {
        dataListeners.remove(listener);
    }

    /**
     * Gets the number of characteristics that have been found by this SmartBall.
     * @return The number of characteristics that have been found by this SmartBall
     */
    public int getNumCharacteristicsFound()
    {
        return characteristicRepository.size();
    }

    /**
     * Method to get a BluetoothGattCharacteristic from this SmartBall.
     * @param characteristic The Characteristic representing the BluetoothGattCharacteristic to get
     * @return The BluetoothGattCharacteristic corresponding to characteristic
     */
    public BluetoothGattCharacteristic getCharacteristic(Services.Characteristic characteristic)
    {
        return characteristicRepository.get(characteristic);
    }

    /**
     * Adds a GattCommandSequence to the queue of command sequences waiting to execute. Only one GattCommandSequence may
     * execute at a time so a queue is used. This method will begin the sequence if the queue is empty.
     * @param sequence The GattCommandSequence to add
     */
    public void addCommandSequenceToQueue(GattCommandSequence sequence)
    {
        // Add the sequence
        commandSequenceQueue.add(sequence);
        Log.d(TAG, "Command Sequence added to queue: " + sequence.NAME + " @ position " + (commandSequenceQueue.size() - 1));

        // Attempt to execute the next sequence (this method will ensure that a sequence already executing is not restarted)
        while (!executeTopCommandInSequence()) {/* Do nothing */}
    }

    /**
     * Gets the current GattCommandSequence on te queue.
     * @return The current GattCommandSequence on te queue
     */
    public GattCommandSequence getCurrentCommandSequence()
    {
        return commandSequenceQueue.peekFirst();
    }

    /**
     * Ends the current GattCommandSequence and starts the next sequence if applicable.
     */
    public void endCurrentCommandSequence()
    {
        GattCommandSequence sequence = commandSequenceQueue.removeFirst();

        if (sequence.isExecuting())
        {
            Log.d(TAG, "Command Sequence ended early: " + sequence.NAME);
            if (sequence.CALLBACK != null)
                sequence.CALLBACK.onCommandSequenceEvent(sequence, GattCommandSequence.Event.ENDED_EARLY);
        }
        else
        {
            Log.d(TAG, "Command Sequence finished: " + sequence.NAME);
            if (sequence.CALLBACK != null)
                sequence.CALLBACK.onCommandSequenceEvent(sequence, GattCommandSequence.Event.FINISHED_EXECUTION);
        }

        // Attempt to execute the next sequence (this method will ensure that a sequence already executing is not restarted)
        while (!executeTopCommandInSequence());
    }

    /**
     * Prints the current state of this SmartBall to the Log.
     */
    public void printState()
    {
        Log.d(TAG, "State:");

        // Connection
        Log.d(TAG, "Connection State: " + CONNECTION.getConnectionState().name());

        // Command Queue
        GattCommandSequence sequence = commandSequenceQueue.peekFirst();

        if (sequence != null)
        {
            Log.d(TAG, "Current GattCommandSequence in execution: " + sequence.NAME + " -> " + sequence.isExecuting());
            if (sequence.isEmpty())
                Log.d(TAG, "GattCommandSequence is empty");
            else
                Log.d(TAG, "\tCurrent GattCommand in execution: " + sequence.peek());
        }
        else
            Log.d(TAG, "No GattCommandSequence currently in execution");

        // Data transmit state
        Log.d(TAG, "Is Transmiting Data: " + isDataTransmitInProgress() + " -> " + getDataTypeInTransit());

        // Listeners
        Log.d(TAG, "Data Listeners: (" + dataListeners.size() + ")");
        for (DataListener listener: dataListeners)
            Log.d(TAG, "\t" + listener);

        Log.d(TAG, "Event Listeners: (" + eventListeners.size() + ")");
        for (EventListener listener: eventListeners)
            Log.d(TAG, "\t" + listener);

        Log.d(TAG, "Characteristic Listeners: (" + characteristicListeners.size() + " characteristics)");
        Collection<UUID> characteristics = characteristicListeners.keySet();

        for (UUID uuid: characteristics)
        {
            Log.d(TAG, "\tListeners on " + uuid + ": (" + getCharacteristicListeners(uuid).size() + ")");
            for (CharacteristicListener listener: getCharacteristicListeners(uuid))
                Log.d(TAG, "\t\t" + listener);
        }
    }

    /**
     * Gets a String representation of this SmartBall.
     * @return AString representation of this SmartBall
     */
    @Override
    public String toString()
    {
        String str = "SmartBall:";

        str += "\tDevice = " + DEVICE.getName();
        str += "\tConnection State = " + CONNECTION.getConnectionState().name();

        return str;
    }

    /**
     * Adds a Characteristic to the Characteristic repository.
     * @param characteristic The Characteristic to add
     */
    protected void addCharacteristicToRepository(Services.Characteristic characteristic,
                                                 BluetoothGattCharacteristic bluetoothGattCharacteristic)
    {
        // Add the characteristic to the repository and notify listeners if the characteristic is new
        if (characteristicRepository.put(characteristic, bluetoothGattCharacteristic) == null)
        {
            // Increment the number of found characteristics
            numCharacteristicsFound++;
            Log.d(TAG, "Characteristic added to repository: " + characteristic.name());
            Log.d(TAG, "Num Characteristics found = : " + numCharacteristicsFound);

            // If all characteristics have been discovered, notify listeners
            if (numCharacteristicsFound == Services.Characteristic.values().length)
            {
                Log.d(TAG, "All Characteristics have been discovered, notifying listeners");
                for (EventListener listener: eventListeners)
                    listener.onBallCharacteristicDiscoveryCompleted(this);
            }
        }
    }

    /**
     * Executes the top GattCommand in the sequence. If the sequence fails to begin execution, it is removed.
     * @return False if the top command failed to start and was removed, true in any other situation
     */
    protected synchronized boolean executeTopCommandInSequence()
    {
        if (commandSequenceQueue.isEmpty())
            return true;

        // Get the top sequence in the queue
        GattCommandSequence sequence = commandSequenceQueue.getFirst();

        // Return if the sequence is already executing
        if (sequence.isExecuting())
        {
            Log.w(TAG, "Attempted to execute a GattCommandSequence already in execution: " + sequence.NAME);
            return true;
        }

        Log.d(TAG, "Attempting to begin execution of top sequence in command queue: " + sequence.NAME + "...");

        // TODO This line (below) causes confusing Log traces, due to the method call in the condition
        if (sequence.executeTop(CONNECTION.getBluetoothGatt())) // If false, the sequence failed to start
        {
            Log.d(TAG, "Command Sequence begun: " + sequence.NAME);

            // Invoke callback if needed
            if (sequence.CALLBACK != null)
                sequence.CALLBACK.onCommandSequenceEvent(sequence, GattCommandSequence.Event.BEGUN_EXECUTION);

            // Check for specific sequences and act accordingly
            switch (sequence.NAME)
            {
                // Is data 1 transmit sequence
                case GattCommandUtils.DATA_TRANSMIT_SEQUENCE_1:
                    dataTransmitInProgress = true;
                    dataTypeInTransit = 1;
                    break;

                // Is data 2 transmit sequence
                case GattCommandUtils.DATA_TRANSMIT_SEQUENCE_2:
                    dataTransmitInProgress = true;
                    dataTypeInTransit = 2;
                    break;

                // Is end data transmit sequence flag
                case GattCommandUtils.END_DATA_TRANSMIT_SEQUENCE:
                    clearDataTransmitInProgressFlag();
                    break;
            }

            return true;
        }
        else
        {
            Log.d(TAG, "Command Sequence failed to start and was removed: " + sequence.NAME);

            // Invoke the callback if needed
            if (sequence.CALLBACK != null)
                sequence.CALLBACK.onCommandSequenceEvent(sequence, GattCommandSequence.Event.FAILED_TO_BEGIN);

            // Remove the command
            if (!commandSequenceQueue.isEmpty())
                commandSequenceQueue.removeFirst();

            return false;
        }
    }

    /**
     * Clears the repository of found characteristics.
     */
    private void clearFoundCharacteristics()
    {
        Log.d(TAG, "Characteristic repository cleared");
        if (characteristicRepository != null)
            characteristicRepository.clear();
        numCharacteristicsFound = 0;
    }

    /**
     * Helper method to initialize the kick bit listener.
     * @param ball A constant reference to this SmartBall for convenience
     */
    private void createKickBitListener(final SmartBall ball)
    {
        addCharacteristicListener(new CharacteristicListener()
        {
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
            {
                byte[] value = characteristic.getValue();

                if (value[0] == 0) // Been kicked
                {
                    kickBit = false;
                    Log.d(TAG, "Ball has been kicked");
                    for (EventListener listener: eventListeners)
                        listener.onBallKickEvent(ball, KickEvent.KICKED);
                }
                else if (value[0] == 1) // Ready to kick
                {
                    kickBit = true;
                    Log.d(TAG, "Ball is ready to be kicked");
                    for (EventListener listener: eventListeners)
                        listener.onBallKickEvent(ball, KickEvent.READY_TO_KICK);
                }
            }
        }, Services.Characteristic.KICK_BIT);
    }

    /**
     * Helper method to initialize the data transmit listener.
     * @param ball A constant reference to this SmartBall for convenience
     */
    private void createDataTransmitListener(final SmartBall ball)
    {
        addCharacteristicListener(new CharacteristicListener()
        {
            private byte[] previousLine;

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
            {
                byte[] value = characteristic.getValue();

                if (dataTransmitInProgress && value.length > 3) // Data transmission currently in progress
                {
                    if (value[0] == -118 && value[1] == 10 && value[2] == 0 && value[3] == 0) // Start
                    {
                        for (DataListener listener: dataListeners)
                        {
                            listener.onSmartBallDataTransmissionEvent(ball, dataTypeInTransit, DataEvent.TRANSMISSION_BEGUN);
                            listener.onSmartBallDataRead(ball, value, true, false, dataTypeInTransit);
                        }
                    }
                    else if (previousLine != null && (value[0] == -102) && (previousLine[0] != -103)) // Finished
                    {
                        dataTransmitInProgress = false;
                        previousLine = null;

                        for (DataListener listener: dataListeners)
                        {
                            listener.onSmartBallDataRead(ball, value, false, true, dataTypeInTransit);
                            listener.onSmartBallDataTransmissionEvent(ball, dataTypeInTransit, DataEvent.TRANSMISSION_ENDED);
                        }
                    }
                    else
                    {
                        for (DataListener listener: dataListeners)
                            listener.onSmartBallDataRead(ball, value, false, false, dataTypeInTransit);

                        previousLine = value;
                    }
                }
            }
        }, Services.Characteristic.DATA_CALLBACK);
    }

    /**
     * Interface for any class listening for characteristic changes on this SmartBall.
     *
     * Created by Theodore on 5/14/2015.
     */
    public interface CharacteristicListener
    {
        /**
         * Called when the Characteristic for which this CharacteristicListener is listening changes.
         * @param gatt The BluetoothGatt
         * @param characteristic The changed Characteristic
         */
        void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
    }

    /**
     * Interface for classes listening for impact data from the SmartBall.
     *
     * Created by Theodore on 5/26/2015.
     */
    public interface DataListener
    {
        /**
         * Called when kick data is read.
         * @param ball The SmartBall
         * @param data The read data
         * @param start true if this value is the data start code
         * @param end true if this value is the data end code
         * @param type The type of data read
         */
        void onSmartBallDataRead(SmartBall ball, byte[] data, boolean start, boolean end, byte type);

        /**
         * Called on a data transmission event.
         * @param ball The SmartBall
         * @param dataType The type of data in the transmission
         * @param event The DataEvent that occurred
         */
        void onSmartBallDataTransmissionEvent(SmartBall ball, byte dataType, DataEvent event);

    }

    /**
     * Interface for any class monitoring a SmartBall.
     *
     * Created by Theodore on 5/11/2015.
     */
    public interface EventListener
    {
        /**
         * Called when the SmartBall experiences a kick event.
         * @param ball The SmartBall
         * @param event The kick event
         */
        void onBallKickEvent(SmartBall ball, SmartBall.KickEvent event);

        /**
         * Called when all BLE characteristics have been discovered.
         * @param ball The SmartBall
         */
        void onBallCharacteristicDiscoveryCompleted(SmartBall ball);
    }
}