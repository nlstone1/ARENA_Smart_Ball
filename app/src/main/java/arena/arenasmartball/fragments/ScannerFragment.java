package arena.arenasmartball.fragments;

import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import arena.arenasmartball.BluetoothBridge;
import arena.arenasmartball.MainActivity;
import arena.arenasmartball.R;
import arena.arenasmartball.ball.SmartBallConnection;
import arena.arenasmartball.ball.SmartBallScanner;
import arena.arenasmartball.views.ScannerView;

/**
 * Fragment for the Scanner screen.
 *
 * Created by Nathaniel on 4/5/2016.
 */
public class ScannerFragment extends SimpleFragment implements ScannerView.ScannerViewListener,
        SmartBallScanner.SmartBallScannerListener
{
    // The ScannerView for this ScannerFragment
    private ScannerView scannerView;

    private View view;

    private Bundle load;

    /**
     * Default Constructor.
     */
    public ScannerFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        if (view == null)
        {
            // Inflate the layout for this fragment
            view = inflater.inflate(R.layout.fragment_scanner, container, false);

            // Find the ScannerView
            scannerView = (ScannerView) view.findViewById(R.id.scanner_view);
            scannerView.addScannerViewListener(this);

            if (load != null)
            {
                scannerView.load(load);
                load = null;
            }
        }

        return view;
    }

    @Override
    public void load(@NonNull Bundle bundle)
    {
        load = bundle;

        BluetoothBridge bridge = MainActivity.getBluetoothBridge();

        if (bridge.getState() == BluetoothBridge.State.DISCONNECTED ||
            bridge.getState() == BluetoothBridge.State.SCANNING)
        {
            bridge.startScanning();
            bridge.getSmartBallScanner().addSmartBallScannerListener(this);
        }
    }

    @Override
    public void save(@NonNull Bundle bundle)
    {
        scannerView.save(bundle);

        BluetoothBridge bridge = MainActivity.getBluetoothBridge();
        bridge.getSmartBallScanner().removeSmartBallScannerListener(this);
        bridge.stopScanning();
    }

    /**
     * Called when the state of the BluetoothBridge changes.
     *
     * @param bridge   The BluetoothBridge whose state changed
     * @param newState The new state
     * @param oldState The old state
     */
    @Override
    public void onBluetoothBridgeStateChanged(BluetoothBridge bridge, BluetoothBridge.State newState, BluetoothBridge.State oldState)
    {
        if (newState == BluetoothBridge.State.DISCONNECTED)
        {
            bridge.startScanning();
            bridge.getSmartBallScanner().addSmartBallScannerListener(this);
        }
    }

    /**
     * Called when the user wants to connect to a ScanResult.
     *
     * @param result The ScanResult to connect to
     */
    @Override
    public void onConnectTo(ScanResult result)
    {
        BluetoothBridge bridge = MainActivity.getBluetoothBridge();
        bridge.connect(result);
    }

    /**
     * Called when the user wants to disconnect from a ScanResult.
     *
     * @param result The ScanResult to disconnect from
     */
    @Override
    public void onDisconnectFrom(ScanResult result)
    {
        BluetoothBridge bridge = MainActivity.getBluetoothBridge();
        bridge.disconnect();
    }

    /**
     * Called when scanning is started.
     */
    @Override
    public void onScanStarted()
    {    }

    /**
     * Called when scanning is stopped.
     *
     * @param failed    True if the scan stopped due to a failure
     * @param errorCode The error code of the failure if one did occur
     */
    @Override
    public void onScanStopped(boolean failed, int errorCode)
    {    }

    /**
     * Called when a SmartBall has been found.
     *
     * @param result The ScanResult representing the found SmartBall.
     */
    @Override
    public void onSmartBallFound(ScanResult result)
    {
        if (scannerView != null)
            scannerView.addScanResult(result);
    }

    /**
     * Called when a SmartBall has been lost.
     *
     * @param result The ScanResult representing the lost SmartBall.
     */
    @Override
    public void onSmartBallLost(ScanResult result)
    {
        if (scannerView != null)
            scannerView.removeScanResult(result);
    }
}
