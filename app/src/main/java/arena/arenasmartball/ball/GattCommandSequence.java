package arena.arenasmartball.ball;

import android.bluetooth.BluetoothGatt;
import android.util.Log;

import java.util.LinkedList;

/**
 * A sequence of GattCommands to be executed. Callbacks are triggered for each execution.
 *
 * Created by Theodore on 5/12/2015.
 */
public class GattCommandSequence
{
    /**
     * GattCommandSequence Event definitions.
     */
    public enum Event
    {
        BEGUN_EXECUTION,
        FINISHED_EXECUTION,
        FAILED_TO_BEGIN,
        ENDED_EARLY
    }

    /** The tag for this class */
    private static final String TAG = "GattCommandSequence";

    /** The name of this GattCommandSequence. */
    public final String NAME;

    /** The callback attached to this GattCommandSequence. */
    public final CommandSequenceCallback CALLBACK;

    /** Denotes whether or not this GattCommandSequence is currently executing. */
    private boolean isExecuting;

    /** The internal command queue of this GattCommandSequence. */
    private LinkedList<GattCommand<?>> commandQueue;

    /**
     * @param name The name of this GattCommandSequence
     * @param callback optional callback
     */
    public GattCommandSequence(String name, CommandSequenceCallback callback)
    {
        isExecuting = false;
        NAME = name;
        CALLBACK = callback;
        commandQueue = new LinkedList<>();
    }

    /**
     * Method to determine whether or not this GattCommandSequence is in the process of executing.
     * @return True if this GattCommandSequence is in the process of executing
     */
    public boolean isExecuting()
    {
        return isExecuting;
    }

    /**
     * Method to fragment_kick whether this GattCommandSequence is empty.
     * @return True if this GattCommandSequence is empty, false otherwise
     */
    public boolean isEmpty()
    {
        return commandQueue.isEmpty();
    }

    /**
     * Method to see the Command at the top of this GattCommandSequence without removing it.
     * @return The Command at the top of this GattCommandSequence
     */
    public GattCommand<?> peek()
    {
        return commandQueue.peekFirst();
    }

    /**
     * Method to see the Command at the top of this GattCommandSequence after removing it.
     * @return The Command at the top of this GattCommandSequence
     */
    public GattCommand<?> pop()
    {
        if (commandQueue.size() == 1)
            isExecuting = false;

        return commandQueue.removeFirst();
    }

    /**
     * Method to add a GattCommand to this GattCommandSequence.
     * @param command The GattCommand to add
     */
    public void addCommand(GattCommand<?> command)
    {
        commandQueue.add(command);
    }

    /**
     * Method to execute the top command in this GattCommandSequence.
     * @param gatt The BluetoothGatt of the SmartBall
     * @return True if the top command was executed successfully
     */
    protected boolean executeTop(BluetoothGatt gatt)
    {
        if (commandQueue.isEmpty())
        {
            return false;
        }

        GattCommand<?> head = commandQueue.getFirst();

        Log.d(TAG, "Attempting to execute top command in sequence " + NAME + "...");

        if (gatt != null && !head.execute(gatt, NAME))
        {
            Log.w(TAG, "Failed to execute top command of GattCommandSequence " + NAME);
            isExecuting = false;
            return false;
        }
        else
        {
            Log.d(TAG, "Executed top command of GattCommandSequence " + NAME);
            isExecuting = true;
            return true;
        }
    }

    /**
     * Callback used by classes that want te be notified of important events during the execution of a GattCommandSequence.
     */
    public interface CommandSequenceCallback
    {
        /**
         * Called when the GattCommandSequence experiences some event.
         * @param sequence The GattCommandSequence that has experienced the event
         * @param event The Event describing the state of the command sequence
         */
        void onCommandSequenceEvent(GattCommandSequence sequence, Event event);
    }
}
