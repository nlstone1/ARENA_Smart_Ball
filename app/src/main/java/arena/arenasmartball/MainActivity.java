package arena.arenasmartball;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import arena.arenasmartball.correlation.Correlator;
import arena.arenasmartball.fragments.AboutFragment;
import arena.arenasmartball.fragments.HUDFragment;

/**
 * Main Activity for the Application.
 *
 *
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
{
    // Log tag String
    private static final String TAG = "MainActivity";

    //  Constant representing the coarse location access permission request
    private static final int PERMISSIONS_REQUEST_COARSE_LOCATION = 1;

    //  Constant representing the external storage permission request
    private static final int PERMISSIONS_REQUEST_STORAGE = 2;

    // Static reference to the MainActivity
    private static MainActivity mainActivity;

    // BluetoothBridge for facilitating BT functions
    private static BluetoothBridge bluetoothBridge = new BluetoothBridge();

    // TextToSpeech for speech synthesis
    private TextToSpeech textToSpeech;
    private volatile boolean speechReady;
    private static int utteranceNum = 0;

    // Used for the App Indexing API
    private GoogleApiClient client;

    // The DrawerLayout
    private DrawerLayout drawerLayout;
    // The NavigationView
    private NavigationView navigationView;

    // Bundle key for saving the index of the current drawer
    private static final String DRAWER_INDEX_BUNDLE_KEY = "arena.arenasmartball.MainActivity.drawerIndex";
    private static final String DRAWER_FRAGMENT_BUNDLE_KEY = "arena.arenasmartball.MainActivity.drawerFragment";

    // Required permissions
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN };

    // Permission reasons
    private static final int[] PERMISSION_MESSAGE = {
            R.string.access_location_explanation,
            R.string.write_ext_storage_explanation,
            R.string.access_bluetooth_explanation,
            R.string.access_bluetooth_explanation};

    // Permission denied messages
    private static final int[] PERMISSION_DENIED_MESSAGE = {
            R.string.access_location_denied_explanation,
            R.string.write_ext_storage_denied_explanation,
            R.string.access_bluetooth_denied_explanation,
            R.string.access_bluetooth_denied_explanation};

    // Permission Titles
    private static final int[] PERMISSION_TITLES = {
            R.string.location_permission,
            R.string.write_ext_storage_permission,
            R.string.bluetooth_permission,
            R.string.bluetooth_permission
    };

    /**
     * Gets the HUDFragment.
     * @return The HUDFragment
     */
    public HUDFragment getHUD()
    {
        return (HUDFragment)getFragmentManager().findFragmentById(R.id.fragment_hud);
    }

    /**
     * Called when the Activity is first created.
     *
     * @param savedInstanceState The saved instance state Bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mainActivity = this;

        // Set the content view
        setContentView(R.layout.activity_main);

        // Initialize the Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize the DrawerLayout
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.setDrawerListener(toggle);
        toggle.syncState();

        // Initialize the NavigationView
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null)
            navigationView.setNavigationItemSelectedListener(this);
        else
            Log.w(TAG, "NavigationView could not be found!");

        // Populate the Drawer items
        populateNavigationDrawer(navigationView);

        // Check permissions
        acquirePermissions();

        // Create the BluetoothBridge
        bluetoothBridge.setActivity(this);

        if (!bluetoothBridge.isReady() && bluetoothBridge.initialize() == BluetoothBridge.BT_NOT_AVAILABLE)
        {
            // BT is not supported on this device, show a message and close the App
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Oops!");
            alertDialog.setMessage("This device does not support Bluetooth; the app will be closed");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {}
                    });
            alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener()
            {
                @Override
                public void onDismiss(DialogInterface dialog)
                {
                    finish();
                }
            });
            alertDialog.show();
        }

        // Implement the App Indexing API
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        // Load instance state
        if (savedInstanceState != null)
        {
            Log.d(TAG, "Loading Instance State");

            // Show the saved drawer item
            int drawerIndex = savedInstanceState.getInt(DRAWER_INDEX_BUNDLE_KEY, 0);
            DrawerItem.setCurrentFragment(drawerIndex, getFragmentManager().getFragment(savedInstanceState,
                    DRAWER_FRAGMENT_BUNDLE_KEY));

//            // Select the drawer in the navigation view
//            navigationView.setCheckedItem(drawerIndex);
        }
        else
        {
            // Show the first drawer item
            DrawerItem.values()[0].openDrawer(this);
        }

        // TODO Init Correlator
        Correlator.initialize(this);
    }

    /**
     * Called when the Back button is pressed.
     */
    @Override
    public void onBackPressed()
    {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START))
        {
            drawer.closeDrawer(GravityCompat.START);
        }
        else
        {
            if (!DrawerItem.back(this))
                super.onBackPressed();
//            else
//            {
//                navigationView.setCheckedItem(DrawerItem.getCurrent().ordinal());
//            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called to save instance state.
     * @param bundle The Bundle to which to save
     */
    @Override
    public void onSaveInstanceState(Bundle bundle)
    {
        super.onSaveInstanceState(bundle);

        Log.d(TAG, "onSaveInstanceState");

        // Save the current drawer index
        if (DrawerItem.getCurrent() != null)
        {
            try
            {
                bundle.putInt(DRAWER_INDEX_BUNDLE_KEY, DrawerItem.getCurrent().ordinal());
                getFragmentManager().putFragment(bundle, DRAWER_FRAGMENT_BUNDLE_KEY, DrawerItem.getCurrentFragment());
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "Error saving the current drawer fragment!", e);
            }
        }
        else
            Log.w(TAG, "Current drawer is null when saving instance state");

        // Save the current drawer
        DrawerItem.onSave(bundle);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        Log.d(TAG, "onRestoreInstanceState");

        // Load the saved state
        DrawerItem.onLoad(savedInstanceState);
    }

    /**
     * Called when a Drawer item is selected.
     *
     * @param item The selected item
     * @return True
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        // Open a drawer item drawer
        if (id >= 0 && id < DrawerItem.values().length)
        {
            DrawerItem.values()[id].openDrawer(this);
        }
        else // Some other item was selected
        {
            if (id == R.string.text_about) // Show the about Dialog
            {
                new AboutFragment().show(getFragmentManager(), null);
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null)
            drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            // Check if BT has been enabled
            case BluetoothBridge.REQUEST_ENABLE_BT:
                bluetoothBridge.setReady(resultCode == RESULT_OK);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        Log.d(TAG, "RequestResult: " + Arrays.toString(permissions) + Arrays.toString(grantResults));

        // Happens if permission request is cancelled
        if (permissions.length == 0)
        {
            // Simply request them again
            acquirePermissions();
            return;
        }

        // Determine which permissions were denied
        ArrayList<Integer> deniedCodes = new ArrayList<>();
        String permission;
        int code;

        for (int i = 0; i < permissions.length; ++i)
        {
            permission = permissions[i];
            code = getPermissionCode(permission);

            if (code == -1)
                Log.e(TAG, "Unrecognized permission requested: " + permission);
            else if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                deniedCodes.add(code);
        }

        if (!deniedCodes.isEmpty())
        {
            // Tell the user why denied permission matter
            String msg = "Some permissions were denied:";

            for (int c : deniedCodes)
            {
                msg += "\n " + getString(PERMISSION_DENIED_MESSAGE[c]);
            }

            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setTitle(getString(R.string.warning));
                    alertDialog.setMessage(msg);
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {}
                            });
                    alertDialog.show();
        }
    }
// TODO
//    /**
//     * Tests whether writing to external storage is permitted.
//     * @return Whether or not writing to external storage is permitted
//     */
//    public boolean storagePermitted()
//    {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
//            return true;
//
//        ActivityCompat.requestPermissions(this,
//                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                PERMISSIONS_REQUEST_STORAGE);
//        return false;
//    }

    /**
     * Locks/Unlocks the NavigationDrawer.
     * @param lock Whether to lock or unlock the NavigationDrawer
     */
    public void lockNavigationDrawer(boolean lock)
    {
        drawerLayout.setDrawerLockMode(lock ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED:
            DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    /**
     * Sets the checked item on the NavigationView.
     * @param id The index of the item to check
     */
    public void selectNavigationBarIcon(int id)
    {
        navigationView.setCheckedItem(id);
    }

    /**
     * Sets the title for the Main Activity.
     * @param title The new title
     */
    public void setActionBarTitle(String title)
    {
        setTitle(getString(R.string.app_name) + ((title != null) ? (" ~ " + title): ""));
    }

    /**
     * Returns the current MainActivity instance.
     * @return The current MainActivity
     */
    public static MainActivity getCurrent()
    {
        return mainActivity;
    }

    /*
     * Populates the Navigation Drawer for the first time.
     */
    private void populateNavigationDrawer(NavigationView navigationView)
    {
        DrawerItem item;
        MenuItem menuItem;
        for (int i = 0; i < DrawerItem.values().length; ++i)
        {
            item = DrawerItem.values()[i];
            menuItem = navigationView.getMenu().add(R.id.main_drawer_group, i, Menu.NONE, item.NAME_RES_ID);
            menuItem.setIcon(item.ICON_RES_ID);

            if (i == 0)
                menuItem.setChecked(true);
        }
        navigationView.getMenu().setGroupCheckable(R.id.main_drawer_group, true, true);

        // Add the about button // TODO better icon
        MenuItem aboutMenuItem = navigationView.getMenu().add(R.id.other_drawer_group, R.string.text_about,
                Menu.NONE, R.string.text_about);
        aboutMenuItem.setIcon(R.drawable.ic_menu_send);
    }

    /**
     * Returns the App's BluetoothBridge.
     *
     * @return The BluetoothBridge
     */
    public static BluetoothBridge getBluetoothBridge()
    {
        return bluetoothBridge;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        // Implement the App Indexing API
        client.connect();
        Action viewAction = Action.newAction(Action.TYPE_VIEW, "Main Page",
                Uri.parse("http://host/path"),
                Uri.parse("android-app://arena.arenasmartball/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Create the TextToSpeech
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {
                speechReady = status == TextToSpeech.SUCCESS;
            }
        });

        textToSpeech.setPitch(0.4f);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        // Destroy the TextToSpeech
        if (textToSpeech != null)
        {
            speechReady = false;
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();

        // Implement the App Indexing API
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW,
                "Main Page",
                Uri.parse("http://host/path"),
                Uri.parse("android-app://arena.arenasmartball/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    /**
     * Attempts to speak the specified text.
     * @param text The text to speak
     * @param flush Whether to flush the queue of text
     */
    public void speak(String text, boolean flush)
    {
        if (speechReady)
        {
            textToSpeech.speak(text, flush ? TextToSpeech.QUEUE_FLUSH: TextToSpeech.QUEUE_ADD,
                    null, "utterance" + ++utteranceNum);
        }
    }

    /**
     * Checks whether location services are enabled.
     * For some reason this is required to get scan results.
     */
    public boolean checkLocation()
    {
        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle(getString(R.string.warning));
            alertDialog.setMessage(getString(R.string.location_explanation));
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {}
                    });
            alertDialog.show();

            return false;
        }

        return true;
    }

    /*
     * Checks for permissions.
     */
    private void acquirePermissions()
    {
        // Permissions to request without explanation
        ArrayList<Integer> perms = new ArrayList<>();

        for (int i = 0; i < PERMISSIONS.length; ++i)
        {
            if (ContextCompat.checkSelfPermission(this, PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED)
            {
                final int code = i;

                // Need to request permission

                // Check whether explanation is first needed
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSIONS[i]))
                {
                    Log.d(TAG, "Showing explanation for " + PERMISSIONS[i]);

                    // Show explanation explaining why the app needs to access coarse location
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setTitle(getString(PERMISSION_TITLES[i]));
                    alertDialog.setMessage(getString(PERMISSION_MESSAGE[i]));
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Ok",
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                // Only request the permission if the user allows
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{PERMISSIONS[code]}, code);
                            }
                        });
                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {}
                        });
                    alertDialog.show();
                }
                else
                {
                    Log.d(TAG, "Need to request " + PERMISSIONS[i]);
                    // No explanation needed, request the permission
                    perms.add(i);
                    //ActivityCompat.requestPermissions(MainActivity.this, new String[]{PERMISSIONS[i]}, i);
                }
            }
            else
            {
                Log.d(TAG, "Has permission: " + PERMISSIONS[i]);
            }
        }

        // Finally request all permissions for which no explanation is needed
        if (!perms.isEmpty())
        {
            String[] permsToRequest = new String[perms.size()];
            for (int i = 0; i < perms.size(); ++i)
                permsToRequest[i] = PERMISSIONS[perms.get(i)];

            Log.d(TAG, "Requesting: " + Arrays.toString(permsToRequest));
            ActivityCompat.requestPermissions(this, permsToRequest, 0xFF);
        }
    }

    /*
     * Finds integer code for specified permission.
     */
    private static int getPermissionCode(String permission)
    {
        for (int i = 0; i < PERMISSIONS.length; ++i)
            if (permission.equals(PERMISSIONS[i]))
                return i;

        return -1;
    }
}