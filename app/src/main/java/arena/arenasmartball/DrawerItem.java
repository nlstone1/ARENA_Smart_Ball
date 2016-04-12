package arena.arenasmartball;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;

import java.util.HashMap;

import arena.arenasmartball.fragments.DetailsFragment;
import arena.arenasmartball.fragments.ScannerFragment;
import arena.arenasmartball.fragments.SimpleFragment;

/**
 * Contains information about each drawer Fragment as well as functions for transitionging
 * between them.
 *
 * Created by Nathaniel on 4/5/2016.
 */
public enum DrawerItem
{
    CONNECT_DRAWER (R.string.connect_drawer_name, R.drawable.ic_drawer_connect, ScannerFragment.class),
    DETAILS_DRAWER (R.string.details_drawer_name, R.drawable.ic_drawer_details, DetailsFragment.class),
    COMMAND_DRAWER (R.string.command_drawer_name, R.drawable.ic_drawer_commands, null),
    CAPTURE_DRAWER (R.string.capture_drawer_name, R.drawable.ic_drawer_capture, null),
    RESULTS_DRAWER (R.string.results_drawer_name, R.drawable.ic_drawer_results, null),
    DATA_DRAWER (R.string.data_drawer_name, R.drawable.ic_drawer_data, null);

    // Log tag String
    private static final String TAG = "DrawerItem";

    // The current DrawerItem
    private static DrawerItem currentDrawerItem;
    // The current Fragment
    private static SimpleFragment currentFragment;
    // Map of saved fragments
    private static final HashMap<DrawerItem, Bundle> SAVED_FRAGMENTS = new HashMap<>();

    /** String resource id for the name of this Drawer */
    public final int NAME_RES_ID;

    /** Drawable resource id for the icon of this Drawer */
    public final int ICON_RES_ID;

    // Fragment represented by this drawer
    private final Class<? extends SimpleFragment> FRAGMENT_CLASS;

    /*
     * Creates a DrawerItem.
     */
    DrawerItem(@StringRes int nameResId, @DrawableRes int iconResId,
               Class<? extends SimpleFragment> fragmentClass)
    {
        NAME_RES_ID = nameResId;
        ICON_RES_ID = iconResId;
        FRAGMENT_CLASS = fragmentClass;
    }

    /**
     * Opens the Fragment this DrawerItem represents.
     * @param activity The parent Activity
     */
    public void openDrawer(MainActivity activity)
    {
        Log.d(TAG, "Opening " + this);

        activity.setActionBarTitle(activity.getString(NAME_RES_ID));

        if (FRAGMENT_CLASS != null && currentDrawerItem != this)
        {
            // Save the old fragment
            if (currentFragment != null)
            {
                Bundle bundle = new Bundle();
                currentFragment.save(bundle);
                SAVED_FRAGMENTS.put(currentDrawerItem, bundle);
            }

            // Update the current Drawer reference
            currentDrawerItem = this;

            // Create and commit the new DrawerItem's fragment
            try
            {
                if (currentFragment != null)
                    MainActivity.getBluetoothBridge().removeBluetoothBridgeStateChangeListener(currentFragment);

                currentFragment = FRAGMENT_CLASS.newInstance();
                MainActivity.getBluetoothBridge().addBluetoothBridgeStateChangeListener(currentFragment);
//                if (SAVE_FRAGMENT)
//                {
                    if (SAVED_FRAGMENTS.containsKey(this))
                    {
                        Log.d(TAG, "loading fragment from hashmap");
                        currentFragment.load(SAVED_FRAGMENTS.remove(this));
                    }
//                    if (SAVED_FRAGMENTS.containsKey(this))
//                    {
//                        Log.d(TAG, "grabbed fragment from hashmap");
//                        currentFragment = SAVED_FRAGMENTS.get(this);
//                    }
//                    else
//                    {
//                        SAVED_FRAGMENTS.put(this, currentFragment);
//                    }
//                }

                FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();

                transaction.replace(R.id.content_main, currentFragment);
                transaction.addToBackStack(null);

                transaction.commit();
            }
            catch (InstantiationException | IllegalAccessException e)
            {
                Log.e(TAG, "Unable to open drawer for " + this + "!", e);
            }
        }
    }

    /**
     * Called when a saved instance state is loaded.
     * @param bundle The Bundle containing the the saved instance state
     */
    public static void onLoad(@NonNull Bundle bundle)
    {
        if (currentFragment != null)
            currentFragment.load(bundle);
    }

    /**
     * Called when the instance state should be saved.
     * @param bundle The Bundle to which to save
     */
    public static void onSave(@NonNull Bundle bundle)
    {
        if (currentFragment != null)
            currentFragment.save(bundle);

        currentDrawerItem = null;
    }

    /**
     * Returns the currently selected DrawerItem.
     * @return The current DrawerItem
     */
    public static DrawerItem getCurrent()
    {
        return currentDrawerItem;
    }
}
