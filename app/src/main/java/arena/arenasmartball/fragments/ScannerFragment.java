package arena.arenasmartball.fragments;

import android.bluetooth.le.ScanResult;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Comparator;
import java.util.PriorityQueue;

import arena.arenasmartball.BluetoothBridge;
import arena.arenasmartball.MainActivity;
import arena.arenasmartball.PeriodicUpdateThread;
import arena.arenasmartball.R;
import arena.arenasmartball.Utils;
import arena.arenasmartball.ball.SmartBall;
import arena.arenasmartball.ball.SmartBallConnection;
import arena.arenasmartball.ball.SmartBallScanner;

/**
 * Fragment for the Scanner screen.
 *
 * Created by Nathaniel on 4/5/2016.
 */
public class ScannerFragment extends SimpleFragment implements CompoundButton.OnCheckedChangeListener,
        SmartBallScanner.SmartBallScannerListener
{
    // The tag for this class
    private static final String TAG = "ScannerFragment";

    // A sorted list of ScanResults
    private PriorityQueue<ScanResult> resultQueue;

    // The scanning status
    private TextView scanStatusView;

    // The scan switch
    private Switch scanSwitch;

    // The ListView of results
    private ListView resultList;

//    // The Thread to handle periodic rescanning
//    private PeriodicRescanner rescanner;

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
                SmartBall globalBall = MainActivity.getBluetoothBridge().getSmartBall();

                if (globalBall != null)
                {
                    if (Utils.areEqual(lhs.getDevice(), globalBall.DEVICE))
                        return -1;
                    else if (Utils.areEqual(rhs.getDevice(), globalBall.DEVICE))
                        return 1;
                }

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
        scanStatusView = (TextView) view.findViewById(R.id.textview_scanner_scan_status);
        scanSwitch = (Switch) view.findViewById(R.id.switch_scanner_scan);
        resultList = (ListView) view.findViewById(R.id.listview_scanner_results);

        // Attach to Scanner
        MainActivity.getBluetoothBridge().getSmartBallScanner().addSmartBallScannerListener(this);

//        // Create Thread
//        if (rescanner != null)
//        {
//            rescanner.kill();
//            rescanner.unblock();
//        }
//
//        rescanner = new PeriodicRescanner();
//        rescanner.start();

        // Initialize
        setValuesForCurrentState(MainActivity.getBluetoothBridge());

        // Set listeners
        scanSwitch.setOnCheckedChangeListener(this);

        return view;
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

        MainActivity.getBluetoothBridge().getSmartBallScanner().removeSmartBallScannerListener(this);
        MainActivity.getBluetoothBridge().removeBluetoothBridgeStateChangeListener(this);

//        if (rescanner != null)
//        {
//            rescanner.kill();
//            rescanner.unblock();
//        }
    }

    /*
     * Sets the values for the views of this Fragment for the current state
     */
    private void setValuesForCurrentState(BluetoothBridge bridge)
    {
        // Set scanning views
        if (bridge.getSmartBallScanner().isScanningStarted())
        {
//            rescanner.unblock();
            scanSwitch.setChecked(true);
            scanStatusView.setText(R.string.scanning);
        }
        else
        {
//            rescanner.block();
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
        if (isChecked)
            MainActivity.getBluetoothBridge().startScanning();
        else
            MainActivity.getBluetoothBridge().stopScanning();
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
    public class ScanResultArrayAdapter extends ArrayAdapter<ScanResult> implements View.OnClickListener
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
            TextView nameView = (TextView) view.findViewById(R.id.textview_scanresult_name);
            TextView statusView = (TextView) view.findViewById(R.id.textview_scanresult_status);
            ImageView rssiView = (ImageView) view.findViewById(R.id.imageview_scanresult_rssi);
            Button connectButton = (Button) view.findViewById(R.id.button_scanresult_connect);
            connectButton.setTag(position);
            connectButton.setOnClickListener(this);

            // Set views
            SmartBall globalBall = MainActivity.getBluetoothBridge().getSmartBall();

            nameView.setText(item.getDevice().getName());
            rssiView.setImageLevel(Utils.getRSSISignalStrength(item.getRssi()));

            if (globalBall != null && Utils.areEqual(item.getDevice(), globalBall.DEVICE))
            {
                nameView.setTypeface(null, Typeface.BOLD);
                statusView.setVisibility(View.VISIBLE);
                statusView.setText(MainActivity.getBluetoothBridge().getSmartBallConnection().getConnectionState().displayName);

                if (globalBall.CONNECTION.getConnectionState() == SmartBallConnection.ConnectionState.CONNECTED)
                    connectButton.setText(R.string.disconnect);
                else if (globalBall.CONNECTION.getConnectionState() == SmartBallConnection.ConnectionState.CONNECTING)
                    connectButton.setText(R.string.cancel_connection);
                else
                    connectButton.setText(R.string.connect);
            }
            else
            {
                nameView.setTypeface(null, Typeface.NORMAL);
                connectButton.setText(R.string.connect);
                statusView.setVisibility(View.GONE);
            }

            return view;
        }

        /**
         * Called when a view has been clicked.
         *
         * @param v The view that was clicked.
         */
        @Override
        public void onClick(View v)
        {
            Button connectButton;

            if (v instanceof Button)
                connectButton = (Button) v;
            else
                return;

            ScanResult item = getItem((Integer)connectButton.getTag());
            BluetoothBridge bridge = MainActivity.getBluetoothBridge();
            SmartBall globalBall = bridge.getSmartBall();

            if (globalBall != null && Utils.areEqual(item.getDevice(), globalBall.DEVICE))
            {
                if (globalBall.CONNECTION.getConnectionState() == SmartBallConnection.ConnectionState.CONNECTED ||
                    globalBall.CONNECTION.getConnectionState() == SmartBallConnection.ConnectionState.CONNECTING)
                    bridge.disconnect();
                else
                    bridge.reconnect();
            }
            else
                bridge.connect(item);
        }
    }

//    /**
//     * Thread for periodically rescanning.
//     *
//     * Created by Theodore on 4/6/2016.
//     */
//    public class PeriodicRescanner extends PeriodicUpdateThread
//    {
//        // Cooldown timer
//        private int coolDown;
//
//        /**
//         * Constructor.
//         */
//        public PeriodicRescanner()
//        {
//            super(500L);
//            coolDown = 4;
//        }
//
//        @Override
//        public void unblock()
//        {
//            coolDown = 4;
//            super.unblock();
//        }
//
//        /**
//         * Called when this Thread is updates.
//         */
//        @Override
//        public void onUpdate()
//        {
//            BluetoothBridge bridge = MainActivity.getBluetoothBridge();
//            if (bridge.getSmartBallScanner().isScanningStarted() && --coolDown <= 0)
//            {
//                Log.d(TAG, "Periodic rescan called");
//                bridge.getSmartBallScanner().stopScanning();
//                bridge.getSmartBallScanner().startScanning();
////                bridge.stopScanning();
////                bridge.startScanning();
//            }
//        }
//    }
}
