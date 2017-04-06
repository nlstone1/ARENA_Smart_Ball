package arena.arenasmartball.fragments;

import android.bluetooth.le.ScanResult;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

import arena.arenasmartball.BluetoothBridge;
import arena.arenasmartball.MainActivity;
import arena.arenasmartball.R;
import arena.arenasmartball.Utils;
import arena.arenasmartball.ball.SmartBall;
import arena.arenasmartball.ball.SmartBallConnection;
import arena.arenasmartball.ball.SmartBallScanner;

/**
 * Fragment for the Scanner screen.
 *
 * Created by Theodore on 10/1/2016.
 */
public class ScannerFragment2 extends SimpleFragment implements CompoundButton.OnCheckedChangeListener,
        SmartBallScanner.SmartBallScannerListener, ListView.OnItemClickListener
{
    // The tag for this class
    private static final String TAG = "ScannerFragment2";

    // The bundle id for the currently selected result
    private static final String BUNDLE_ID_SELECTED = "ScannerFragment2.selected";

    // The scanning status
    private TextView scanStatusView;

    // The ListView containing the scan results
    private ListView scanResultsListView;

    // The Adapter containing the ScanResults
    private ScanResultArrayAdapter scanResults;

    // The scan switch
    private Switch scanSwitch;

    // The currently selected scan result
    private ScanResult selected;

    // The set of default scan results to display
    private Set<ScanResult> defaultResults;

    /**
     * Required empty public constructor.
     */
    public ScannerFragment2()
    {    }

    /**
     * Called to create the view on this Fragment.
     * @param inflater The LayoutInflater to use
     * @param container The container containing the view
     * @param savedInstanceState The Bundle containing saved instance state
     * @return The created view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_scanner2, container, false);

        // Get views
        scanStatusView = (TextView) view.findViewById(R.id.textview_scanner_scan_status);
        scanSwitch = (Switch) view.findViewById(R.id.switch_scanner_scan);
        scanResultsListView = (ListView) view.findViewById(R.id.listview_scan_results);

        // Attach to Scanner
        MainActivity.getBluetoothBridge().getSmartBallScanner().addSmartBallScannerListener(this);

        // Set listeners
        scanSwitch.setOnCheckedChangeListener(this);

        // Create scan result adapter
        scanResults = new ScanResultArrayAdapter();

        // TODO add default balls?

        // Set the scan result adapter
        scanResultsListView.setAdapter(scanResults);
        scanResultsListView.setOnItemClickListener(this);

        // Initialize
        setValuesForCurrentState(MainActivity.getBluetoothBridge());

        return view;
    }

    /**
     * Called when the view on this Fragment is destroyed.
     */
    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

        MainActivity.getBluetoothBridge().getSmartBallScanner().removeSmartBallScannerListener(this);
        MainActivity.getBluetoothBridge().removeBluetoothBridgeStateChangeListener(this);

    }

    @Override
    public void load(@NonNull Bundle bundle)
    {
        // Load the selected ball
        selected = bundle.getParcelable(BUNDLE_ID_SELECTED);
    }

    @Override
    public void save(@NonNull Bundle bundle)
    {
        // Save the selected ball
        bundle.putParcelable(BUNDLE_ID_SELECTED, selected);
    }

    /*
     * Sets the values for the views of this Fragment for the current state
     */
    private void setValuesForCurrentState(BluetoothBridge bridge)
    {
        // Set scanning views
        if (bridge.getSmartBallScanner().isScanningStarted())
        {
            scanSwitch.setChecked(true);
            scanStatusView.setText(R.string.scanning);
        }
        else
        {
            scanSwitch.setChecked(false);
            scanStatusView.setText(R.string.enable_scanning);
        }

        // Reset list
        populateScanResults(bridge);
    }

    /*
     * Populates the results from the list of ScanResults.
     */
    private void populateScanResults(BluetoothBridge bridge)
    {
        scanResults.clear();

        // Add results from scanner
        scanResults.addAll(bridge.getSmartBallScanner().getFoundResults().values());
    }

    /**
     * Called when the state of the BluetoothBridge changes.
     *
     * @param bridge   The BluetoothBridge whose state changed
     * @param newState The new state
     * @param oldState The old state
     */
    @Override
    public void onBluetoothBridgeStateChanged(final BluetoothBridge bridge,
                                              BluetoothBridge.State newState, BluetoothBridge.State oldState)
    {
        try
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    setValuesForCurrentState(MainActivity.getBluetoothBridge());
                }
            });
        }
        catch (Exception e)
        { /* Ignore */ }
    }

    /**
     * Called when the checked state of a compound button has changed.
     *
     * @param buttonView The compound button view whose state has changed.
     * @param isChecked  The new checked state of buttonView.
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        if (MainActivity.getCurrent().checkLocation())
        {
            if (isChecked)
                MainActivity.getBluetoothBridge().startScanning();
            else
                MainActivity.getBluetoothBridge().stopScanning();
        }
        else
            scanSwitch.setChecked(false);
    }

    /**
     * Called when scanning is started.
     */
    @Override
    public void onScanStarted()
    {
        try
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    setValuesForCurrentState(MainActivity.getBluetoothBridge());
                }
            });
        }
        catch (Exception e)
        { /* Ignore */ }
    }

    /**
     * Called when scanning is stopped.
     *
     * @param failed    True if the scan stopped due to a failure
     * @param errorCode The error code of the failure if one did occur
     */
    @Override
    public void onScanStopped(boolean failed, int errorCode)
    {
        if (failed)
        {
            Log.e(TAG, "Scanning stopped with error: " + SmartBallScanner.getScanErrorString(errorCode));

            if (getMainActivity() != null)
                Toast.makeText(getMainActivity(), "Scanning Failed", Toast.LENGTH_SHORT).show();
        }

        try
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    setValuesForCurrentState(MainActivity.getBluetoothBridge());
                }
            });
        }
        catch (Exception e)
        { /* Ignore */ }
    }

    /**
     * Called when a SmartBall has been found.
     *
     * @param result The ScanResult representing the found SmartBall.
     */
    @Override
    public void onSmartBallFound(ScanResult result)
    {
        populateScanResults(MainActivity.getBluetoothBridge());
    }

    /**
     * Called when a SmartBall has been lost.
     *
     * @param result The ScanResult representing the lost SmartBall.
     */
    @Override
    public void onSmartBallLost(ScanResult result)
    {
        populateScanResults(MainActivity.getBluetoothBridge());
    }

    /**
     * Callback method to be invoked when an item in this AdapterView has
     * been clicked.
     * <p/>
     * Implementers can call getItemAtPosition(position) if they need
     * to access the data associated with the selected item.
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this
     *                 will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        ScanResult result = scanResults.getItem(position);

        // Is currently selected
        if (result != null && Utils.areEqual(selected, result))
        {
            // Get the global ball
            SmartBall globalBall = MainActivity.getBluetoothBridge().getSmartBall();

            // If there is no global ball connect to the result
            if (globalBall == null ||
                globalBall.CONNECTION.getConnectionState() == SmartBallConnection.ConnectionState.DISCONNECTED ||
                globalBall.CONNECTION.getConnectionState() == SmartBallConnection.ConnectionState.NOT_CONNECTED)
                MainActivity.getBluetoothBridge().connect(result);
            // Else if it is the global ball connect or disconnect depending on the current state
            else if (Utils.areEqual(globalBall.DEVICE, result.getDevice()))
            {
                if (globalBall.CONNECTION.getConnectionState() == SmartBallConnection.ConnectionState.CONNECTED ||
                    globalBall.CONNECTION.getConnectionState() == SmartBallConnection.ConnectionState.CONNECTING)
                {
                    MainActivity.getBluetoothBridge().disconnect();
                    selected = null;
                }
                else
                    MainActivity.getBluetoothBridge().connect(result);
            }
            // Else swap the connection
            else// if (globalBall.CONNECTION.getConnectionState() != SmartBallConnection.ConnectionState.CONNECTING)
            {
                MainActivity.getBluetoothBridge().disconnect();
                MainActivity.getBluetoothBridge().connect(result);
            }
        }
        else
            selected = result;

        // Update the list
        scanResults.notifyDataSetChanged();
    }

    /**
     * Implementation of an ArrayAdapter containing views for displaying ScanResults.
     */
    public class ScanResultArrayAdapter extends ArrayAdapter<ScanResult>// implements View.OnClickListener
    {
        /**
         * Constructor.
         */
        public ScanResultArrayAdapter()
        {
            super(getActivity(), android.R.layout.simple_list_item_1);
        }

        /**
         * Get the view that displays the data at the specified position.
         * @param position The position of the item
         * @param convertView The old view
         * @param parent The parent viewgroup
         * @return The view that displays the data
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View view;

            // Reuse old view if possible
            if (convertView != null && convertView.getId() == R.id.component_scan_result)
                view = convertView;
            else
                view = LayoutInflater.from(getContext()).inflate(R.layout.component_scan_result2, parent, false);

            // Get components on View
            ScanResult item = getItem(position);
            TextView nameView = (TextView) view.findViewById(R.id.textview_scanresult_name);
            TextView statusView = (TextView) view.findViewById(R.id.textview_scanresult_status);
//            Button actionButton = (Button) view.findViewById(R.id.button_scanresult_action);

            // Set views
            SmartBall globalBall = MainActivity.getBluetoothBridge().getSmartBall();

            // Check whether selected
            boolean isSelected = Utils.areEqual(item, selected);
            boolean isGlobalBall = globalBall != null && Utils.areEqual(item.getDevice(), globalBall.DEVICE);

            // Set name
            nameView.setText(item.getDevice().getName());
            nameView.setTypeface(null, isGlobalBall ? Typeface.BOLD : Typeface.NORMAL);
            nameView.setTextColor(getResources().getColor(isSelected ? R.color.colorVLtGray : R.color.colorVDkGray));

            // Set status
            if (isSelected)
            {
                statusView.setVisibility(View.VISIBLE);

                // If there is no global ball connect to the result
                if (globalBall == null ||
                        globalBall.CONNECTION.getConnectionState() == SmartBallConnection.ConnectionState.DISCONNECTED ||
                        globalBall.CONNECTION.getConnectionState() == SmartBallConnection.ConnectionState.NOT_CONNECTED)
                    statusView.setText(R.string.tap_to_connect);
                    // Else if it is the global ball connect or disconnect depending on the current state
                else if (isGlobalBall)
                {
                    if (globalBall.CONNECTION.getConnectionState() == SmartBallConnection.ConnectionState.CONNECTED ||
                            globalBall.CONNECTION.getConnectionState() == SmartBallConnection.ConnectionState.CONNECTING)
                        statusView.setText(R.string.tap_to_disconnect);
                    else
                        statusView.setText(R.string.tap_to_connect);
                }
                // Else swap the connection
                else// if (globalBall.CONNECTION.getConnectionState() != SmartBallConnection.ConnectionState.CONNECTING)
                    statusView.setText(R.string.tap_to_connect);
            }
            else
                statusView.setVisibility(View.GONE);

            // Set background color
            view.setBackgroundColor(getResources().getColor(isSelected ? R.color.colorSelected : R.color.colorClear));

            // Set action button
//            actionButton.setTag(position);
//            actionButton.setOnClickListener(this);
//
//            if (isGlobalBall)
//            {
//                actionButton.setVisibility(View.VISIBLE);
//                actionButton.setEnabled(true);
//
//                if (globalBall.CONNECTION.getConnectionState() != SmartBallConnection.ConnectionState.CONNECTED)
//                    actionButton.setText(R.string.disconnect);
//                else
//                    actionButton.setText(R.string.connect);
//            }
//            else if (isSelected)
//            {
//                actionButton.setVisibility(View.VISIBLE);
//                actionButton.setEnabled(globalBall == null);
//                actionButton.setText(R.string.connect);
//            }
//            else
//                actionButton.setVisibility(View.GONE);

            return view;
        }

//        /**
//         * Called when a view has been clicked.
//         *
//         * @param v The view that was clicked.
//         */
//        @Override
//        public void onClick(View v)
//        {
//            Log.w(TAG, "Item clicked: " + v);
//
//            Button clicked;
//
//            if (v instanceof Button)
//                clicked = (Button) v;
//            else
//                return;
//
//            ScanResult item = getItem((Integer)clicked.getTag());
//
//            BluetoothBridge bridge = MainActivity.getBluetoothBridge();
//            SmartBall globalBall = bridge.getSmartBall();
//
//            if (globalBall != null && Utils.areEqual(item.getDevice(), globalBall.DEVICE))
//            {
//                if (globalBall.CONNECTION.getConnectionState() == SmartBallConnection.ConnectionState.CONNECTED ||
//                    globalBall.CONNECTION.getConnectionState() == SmartBallConnection.ConnectionState.CONNECTING)
//                    bridge.disconnect();
//                else
//                    bridge.reconnect();
//            }
//            else
//                bridge.connect(item);
//        }
    }
}
