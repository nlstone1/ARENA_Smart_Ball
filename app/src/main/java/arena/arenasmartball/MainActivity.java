package arena.arenasmartball;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

    // Static reference to the MainActivity
    private static MainActivity mainActivity;

    // BluetoothBridge for facilitating BT functions
    private static BluetoothBridge bluetoothBridge = new BluetoothBridge();

    // Used for the App Indexing API
    private GoogleApiClient client;

    // The DrawerLayout
    private DrawerLayout drawerLayout;
    // The NavigationView
    private NavigationView navigationView;

    // Bundle key for saving the index of the current drawer
    private static final String DRAWER_INDEX_BUNDLE_KEY = "arena.arenasmartball.MainActivity.drawerIndex";
    private static final String DRAWER_FRAGMENT_BUNDLE_KEY = "arena.arenasmartball.MainActivity.drawerFragment";

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
        navigationView.setNavigationItemSelectedListener(this);
        // Populate the Drawer items
        populateNavigationDrawer(navigationView);

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

        // Request dangerous permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))
            {
                // Show explanation explaining why the app needs to access coarse location
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle(getString(R.string.location_permission));
                alertDialog.setMessage(getString(R.string.access_location_explanation));
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
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
                // No explanation needed, we can request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSIONS_REQUEST_COARSE_LOCATION);
            }
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
        switch (requestCode)
        {
            case PERMISSIONS_REQUEST_COARSE_LOCATION:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length == 0 || (grantResults[0] != PackageManager.PERMISSION_GRANTED))
                {
                    // Explain to the user that the app will not work without the permissions
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setTitle(getString(R.string.location_permission));
                    alertDialog.setMessage(getString(R.string.access_location_denied_explanation));
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {}
                            });
                    alertDialog.show();
                }
                break;
            }
        }
    }

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
}