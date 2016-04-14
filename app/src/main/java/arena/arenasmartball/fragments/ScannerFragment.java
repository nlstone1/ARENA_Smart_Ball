package arena.arenasmartball.fragments;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Comparator;
import java.util.PriorityQueue;

import arena.arenasmartball.BluetoothBridge;
import arena.arenasmartball.MainActivity;
import arena.arenasmartball.R;
import arena.arenasmartball.Utils;
import arena.arenasmartball.ball.SmartBall;
import arena.arenasmartball.ball.SmartBallConnection;
import arena.arenasmartball.ball.SmartBallScanner;
import arena.arenasmartball.views.ScannerView;

/**
 * Fragment for the Scanner screen.
 *
 * Created by Nathaniel on 4/5/2016.
 */
public class ScannerFragment extends SimpleFragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener,
        SmartBallScanner.SmartBallScannerListener, AdapterView.OnItemClickListener
{
    // The bundle id for the selected result
    private static final String SELECTED_RESULT_BUNDLE_ID = "ScannerFragment.SR";

    // The currently selected ScanResult
    private ScanResult selectedScanResult;

    // A sorted list of ScanResults
    private PriorityQueue<ScanResult> resultQueue;

    // The name of the selected result
    private TextView selectedResultNameView;

    // The scanning status
    private TextView scanStatusView;

    // The connect / disconnect button
    private Button connectButton;

    // The scan switch
    private Switch scanSwitch;

    // The ListView of results
    private ListView resultList;

    /**
     * Required empty public constructor.
     */
    public ScannerFragment()
    {
        resultQueue = new PriorityQueue<>(4, new Comparator<ScanResult>()
        {
            @Override
            public int compare(ScanResult lhs, ScanResult rhs)
            {
                if (Utils.areEqual(lhs, selectedScanResult))
                    return -100;
                else if (Utils.areEqual(rhs, selectedScanResult))
                    return 100;
                else
                    return lhs.getRssi() - rhs.getRssi();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_scanner, container, false);

        // Get views
        selectedResultNameView = (TextView) view.findViewById(R.id.textview_scanner_ball_name);
        scanStatusView = (TextView) view.findViewById(R.id.textview_scanner_scan_status);
        connectButton = (Button) view.findViewById(R.id.button_scanner_connect);
        scanSwitch = (Switch) view.findViewById(R.id.switch_scanner_scan);
        resultList = (ListView) view.findViewById(R.id.listview_scanner_results);

        // Set listeners
        connectButton.setOnClickListener(this);
        scanSwitch.setOnCheckedChangeListener(this);

        // Attach to Scanner
        MainActivity.getBluetoothBridge().getSmartBallScanner().addSmartBallScannerListener(this);

        // Initialize
        setValuesForCurrentState(MainActivity.getBluetoothBridge());

        return view;
    }

    /**
     * Load any saved instance state here.
     * @param bundle The Bundle from which to load
     */
    @Override
    public void load(@NonNull Bundle bundle)
    {
        selectedScanResult = bundle.getParcelable(SELECTED_RESULT_BUNDLE_ID);
    }

    /**
     * Save any state information here.
     * @param bundle The Bundle to which to save
     */
    @Override
    public void save(@NonNull Bundle bundle)
    {
        if (selectedScanResult != null)
            bundle.putParcelable(SELECTED_RESULT_BUNDLE_ID, selectedScanResult);
    }

    /*
     * Sets the values for the views of this Fragment for the current state
     */
    private void setValuesForCurrentState(BluetoothBridge bridge)
    {
        // Set displayed result views
        if (selectedScanResult == null)
        {
            selectedResultNameView.setText(R.string.no_device_chosen);
            connectButton.setText(R.string.connect);
            connectButton.setEnabled(false);
        }
        else
        {
            selectedResultNameView.setText(selectedScanResult.getDevice().getName());
            connectButton.setEnabled(true);

            SmartBall globalBall = bridge.getSmartBall();

            if (globalBall != null && Utils.areEqual(globalBall.DEVICE, selectedScanResult.getDevice()))
            {
                if (bridge.getSmartBallConnection().getConnectionState() == SmartBallConnection.ConnectionState.CONNECTED)
                    connectButton.setText(R.string.disconnect);
                else if (bridge.getSmartBallConnection().getConnectionState() == SmartBallConnection.ConnectionState.CONNECTING)
                    connectButton.setText(R.string.cancel_connection);
                else
                    connectButton.setText(R.string.connect);
            }
            else
                connectButton.setText(R.string.connect);
        }

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

        // Reset list TODO maybe reuse old adapter?
        populateScanResults(bridge);
    }

    /*
     * Populates the list of ScanResults on this ScannerFragment.
     */
    private void populateScanResults(BluetoothBridge bridge)
    {
        ScanResultArrayAdapter results = new ScanResultArrayAdapter();
        resultQueue.clear();

        // Add results from scanner
        resultQueue.addAll(bridge.getSmartBallScanner().getFoundResults().values());

        // Add results to adapter
        results.addAll(resultQueue);

        // Set adapter
        resultList.setAdapter(results);
        resultList.setOnItemClickListener(this);
    }

    /**
     * Called when the state of the BluetoothBridge changes.
     *
     * @param bridge   The BluetoothBridge whose state changed
     * @param newState The new state
     * @param oldState The old state
     */
    @Override
    public void onBluetoothBridgeStateChanged(final BluetoothBridge bridge, BluetoothBridge.State newState, BluetoothBridge.State oldState)
    {
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                setValuesForCurrentState(bridge);
            }
        });
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
        if (isChecked)
            MainActivity.getBluetoothBridge().startScanning();
        else
            MainActivity.getBluetoothBridge().stopScanning();
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v)
    {
        BluetoothBridge bridge = MainActivity.getBluetoothBridge();

        if (bridge.getSmartBall() != null && Utils.areEqual(selectedScanResult.getDevice(), bridge.getSmartBall().DEVICE))
        {
            if (bridge.getSmartBallConnection().getConnectionState() == SmartBallConnection.ConnectionState.CONNECTED ||
                bridge.getSmartBallConnection().getConnectionState() == SmartBallConnection.ConnectionState.CONNECTING)
                bridge.disconnect();
            else
                bridge.reconnect();
        }
        else
            bridge.connect(selectedScanResult);
    }

    /**
     * Callback method to be invoked when an item in this AdapterView has
     * been clicked.
     * <p>
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
        selectedScanResult = (ScanResult)parent.getItemAtPosition(position);

        Log.e("DEBUG", "Item Click: Item = " + selectedScanResult);

        setValuesForCurrentState(MainActivity.getBluetoothBridge()); // TODO make more efficient?
    }

    /**
     * Called when scanning is started.
     */
    @Override
    public void onScanStarted()
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

    /**
     * Called when scanning is stopped.
     *
     * @param failed    True if the scan stopped due to a failure
     * @param errorCode The error code of the failure if one did occur
     */
    @Override
    public void onScanStopped(boolean failed, int errorCode)
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

    /**
     * Called when a SmartBall has been found.
     *
     * @param result The ScanResult representing the found SmartBall.
     */
    @Override
    public void onSmartBallFound(ScanResult result)
    {
        populateScanResults(MainActivity.getBluetoothBridge()); // TODO make more efficient
    }

    /**
     * Called when a SmartBall has been lost.
     *
     * @param result The ScanResult representing the lost SmartBall.
     */
    @Override
    public void onSmartBallLost(ScanResult result)
    {
        populateScanResults(MainActivity.getBluetoothBridge()); // TODO make more efficient
    }

    /**
     * Implementation of an ArrayAdapter containing views for displaying ScanResults.
     */
    public class ScanResultArrayAdapter extends ArrayAdapter<ScanResult>
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
                view = LayoutInflater.from(getContext()).inflate(R.layout.component_scan_result, parent, false);

            // Get components on View
            ScanResult item = getItem(position);
            TextView nameView = (TextView)view.findViewById(R.id.textview_scanresult_name);
            TextView statusView = (TextView)view.findViewById(R.id.textview_scanresult_status);
            ImageView rssiView = (ImageView)view.findViewById(R.id.imageview_scanresult_rssi);

            // Set views
            nameView.setText(item.getDevice().getName());
            nameView.setTypeface(null, Utils.areEqual(item, selectedScanResult) ? Typeface.BOLD : Typeface.NORMAL);
            rssiView.setImageLevel(Utils.getRSSISignalStrength(item.getRssi()));

            SmartBall globalBall = MainActivity.getBluetoothBridge().getSmartBall();

            if (globalBall != null && Utils.areEqual(item.getDevice(), globalBall.DEVICE))
            {
                statusView.setVisibility(View.VISIBLE);
                statusView.setText(MainActivity.getBluetoothBridge().getSmartBallConnection().getConnectionState().displayName);
            }
            else
                statusView.setVisibility(View.GONE);

            return view;
        }
    }
}
