package arena.arenasmartball;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;

import arena.arenasmartball.fragments.CaptureFragment;
import arena.arenasmartball.fragments.DetailsFragment;
import arena.arenasmartball.fragments.DownloadFragment;
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
    CAPTURE_DRAWER (R.string.capture_drawer_name, R.drawable.ic_drawer_capture, CaptureFragment.class),
    DOWNLOAD_DRAWER(R.string.download_drawer_name, R.drawable.ic_drawer_download, DownloadFragment.class),
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

    // Back stack for the Back button
    private static final int BACK_STACK_SIZE = 128;
    private static final int[] BACK_STACK = new int[BACK_STACK_SIZE];
    private static int backStackIdx = -1;

    static
    {
        Arrays.fill(BACK_STACK, -1);
    }

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

        if (currentDrawerItem == null)
            Log.w(TAG, "currentDrawerItem is null");

        if (FRAGMENT_CLASS != null && currentDrawerItem != this)
        {
            // Save the old fragment
            if (currentFragment != null)
            {
                Bundle bundle = new Bundle();
                currentFragment.save(bundle);
                currentFragment.onClose();

                if (currentDrawerItem != null)
                {
                    SAVED_FRAGMENTS.put(currentDrawerItem, bundle);
                    addToBackStack(currentDrawerItem.ordinal());
                }
            }

            // Update the current Drawer reference
            currentDrawerItem = this;

            // Update the selected navigation drawer icon
            MainActivity.getCurrent().selectNavigationBarIcon(ordinal());

            // Create and commit the new DrawerItem's fragment
            try
            {
                if (currentFragment != null)
                    MainActivity.getBluetoothBridge().removeBluetoothBridgeStateChangeListener(currentFragment);

                currentFragment = FRAGMENT_CLASS.newInstance();
                currentFragment.onOpen();
                MainActivity.getBluetoothBridge().addBluetoothBridgeStateChangeListener(currentFragment);

                if (SAVED_FRAGMENTS.containsKey(this))
                {
                    Log.d(TAG, "loading fragment from hashmap");

                    final Bundle b = SAVED_FRAGMENTS.remove(this);

                    currentFragment.runInOnResume(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            currentFragment.load(b);
                        }
                    });
                }

                FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();

                transaction.replace(R.id.framelayout_content_main, currentFragment);
//                transaction.replace(R.id.content_main, currentFragment);
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
     * Opens the drawer on the top of the back stack.
     * @param mainActivity The MainActivity
     * @return False if the back stack is empty, true otherwise
     */
    public static boolean back(MainActivity mainActivity)
    {
        int idx = popOffBackStack();

        if (idx == -1)
            return false;
        else
        {
            values()[idx].openDrawer(mainActivity);
            popOffBackStack();
            return true;
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
     * Returns the Fragment for the open drawer.
     * @return The Fragment for the open Drawer
     */
    public static Fragment getCurrentFragment()
    {
        return currentFragment;
    }

    /**
     * Sets the current DrawerFragment.
     * @param fragment The new Fragment
     */
    static void setCurrentFragment(int drawerIndex, Fragment fragment)
    {
        currentDrawerItem = DrawerItem.values()[drawerIndex];
        currentFragment = (SimpleFragment) fragment;
    }

    /**
     * Returns the currently selected DrawerItem.
     * @return The current DrawerItem
     */
    public static DrawerItem getCurrent()
    {
        return currentDrawerItem;
    }

    /**
     * Adds an integer to the back stack.
     * @param idx The integer
     */
    private static void addToBackStack(int idx)
    {
        ++backStackIdx;

        BACK_STACK[backStackIdx % BACK_STACK_SIZE] = idx;
    }

    /**
     * Pops the next value off the back stack.
     * @return The next value or -1 for no value
     */
    private static int popOffBackStack()
    {
        if (backStackIdx < 0)
        {
            return -1;
        }

        int r = BACK_STACK[backStackIdx % BACK_STACK_SIZE];
        BACK_STACK[backStackIdx % BACK_STACK_SIZE] = -1;
        --backStackIdx;

        return r;
    }
}