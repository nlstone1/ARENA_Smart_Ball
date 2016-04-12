package arena.arenasmartball.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;

import arena.arenasmartball.BluetoothBridge;
import arena.arenasmartball.MainActivity;

/**
 * Base Fragment for working in the MainActivity.
 *
 * Created by Nathaniel on 4/5/2016.
 */
public class SimpleFragment extends Fragment implements BluetoothBridge.BluetoothBridgeStateChangeListener
{
    /**
     * Convenience method to get this Fragment's parent Activity as a MainActivity.
     * @return This Fragment's parent Activity as a MainActivity
     */
    public MainActivity getMainActivity()
    {
        return (MainActivity) getActivity();
    }

    /**
     * Load any saved instance state here.
     * @param bundle The Bundle from which to load
     */
    public void load(@NonNull Bundle bundle)
    {   }

    /**
     * Save any state information here.
     * @param bundle The Bundle to which to save
     */
    public void save(@NonNull Bundle bundle)
    {   }

    /**
     * Called when the state of the BluetoothBridge changes.
     *
     * @param bridge   The BluetoothBridge whose state changed
     * @param newState The new state
     * @param oldState The old state
     */
    @Override
    public void onBluetoothBridgeStateChanged(BluetoothBridge bridge, BluetoothBridge.State newState, BluetoothBridge.State oldState)
    {    }

    /**
     * Concatenates a resource string and a String Object.
     * @param stringResId The id of the resource String
     * @param string The String Object
     * @return The result of the concatenation
     */
    protected String concat(int stringResId, String string)
    {
        return getString(stringResId) + string;
    }

    /**
     * Concatenates two resource strings.
     * @param stringResIdA The id of the first resource String
     * @param stringResIdB The id of the second resource String
     * @return The result of the concatenation
     */
    protected String concat(int stringResIdA, int stringResIdB)
    {
        return getString(stringResIdA) + getString(stringResIdB);
    }
}
