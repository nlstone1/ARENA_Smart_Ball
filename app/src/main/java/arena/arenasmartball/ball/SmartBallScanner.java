package arena.arenasmartball.ball;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Class to handle scanning for SmartBalls.
 *
 * Created by Theodore on 4/5/2016.
 */
public class SmartBallScanner
{
    // The Tag for this class
    private static final String TAG = "SmartBallScanner";

    // The List of ScanFilters to use when scanning for devices.
    private static List<ScanFilter> scanFilters = new LinkedList<>();

    // Build ScanFilters
    static
    {
        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setServiceUuid(new ParcelUuid(UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")));
        builder.setServiceUuid(new ParcelUuid(UUID.fromString("0000ad04-0000-1000-8000-00805f9b34fb")));
        builder.setServiceUuid(new ParcelUuid(UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")));
        builder.setServiceUuid(new ParcelUuid(UUID.fromString("0000ad04-0000-1000-8000-00805f9b34fb")));
        scanFilters.add(builder.build());
    }

    // Denotes whether or not this SmartBallScanner has begun scanning
    private boolean isScanningStarted;

    // The BluetoothLeScanner to use to scan for BLE devices
    private BluetoothLeScanner scanner;

    // The set of all found results
    private Map<String, ScanResult> results;

    // The Set of SmartBallScannerListeners attached to this class
    private Set<SmartBallScannerListener> listeners;

    // The ScanCallback used internally for scanning
    private ScanCallback internalCallback = new ScanCallback()
    {
        /**
         * Callback when a BLE advertisement has been found.
         *
         * @param callbackType Determines how this callback was triggered.
         * @param result       A Bluetooth LE scan result.
         */
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            super.onScanResult(callbackType, result);

            // Process result
            if (callbackType == ScanSettings.CALLBACK_TYPE_MATCH_LOST)
                removeScanResult(result);
            else
                addScanResult(result);
        }

        /**
         * Callback when batch results are delivered.
         *
         * @param results List of scan results that are previously scanned.
         */
        @Override
        public void onBatchScanResults(List<ScanResult> results)
        {
            super.onBatchScanResults(results);

            // Process result
            for (ScanResult result: results)
                addScanResult(result);
        }

        /**
         * Callback when scan could not be started.
         *
         * @param errorCode Error code (one of SCAN_FAILED_*) for scan failure.
         */
        @Override
        public void onScanFailed(int errorCode)
        {
            super.onScanFailed(errorCode);

            // Set flag
            isScanningStarted = false;

            // Invoke callback
            for (SmartBallScannerListener listener: listeners)
                listener.onScanStopped(true, errorCode);
        }
    };

    /**
     * Constructs a new SmartBallScanner.
     * @param adapter  The BluetoothAdapter of the device
     */
    public SmartBallScanner(BluetoothAdapter adapter)
    {
        scanner = adapter.getBluetoothLeScanner();
        results = new HashMap<>();
        listeners = new HashSet<>();
    }

    /**
     * Gets the String representation of the given scanner error code.
     * @param errorCode The scanning error code
     * @return A String representation of the given scanner error code
     */
    public static String getScanErrorString(int errorCode)
    {
        switch (errorCode)
        {
            case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                return "Scan Failed Already Started";
            case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                return "Scan Failed Application Registration Failed";
            case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                return "Scan Failed Feature Unsupported";
            case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                return "Scan Failed Internal Error";
            default:
                return "Unknown Error (" + errorCode + ")";
        }
    }

    /**
     * Re-initializes the SmartBallScanner for the specified BluetoothAdapter.
     * @param adapter The BluetoothAdapter
     */
    public void reinit(BluetoothAdapter adapter)
    {
        scanner = adapter.getBluetoothLeScanner();
    }

    /**
     * Checks whether or not this SmartBallScanner has started scanning.
     * @return Whether or not this SmartBallScanner has started scanning
     */
    public boolean isScanningStarted()
    {
        return isScanningStarted;
    }

    /**
     * Gets the map of found ScanResults in this SmartBallScanner. Each ScanResult is mapped to the address
     * of it's BluetoothDevice.
     * @return The Set of found ScanResults in this SmartBallScanner
     */
    public Map<String, ScanResult> getFoundResults()
    {
        return results;
    }

    /**
     * Adds a SmartBallScannerListener to this SmartBallScanner.
     * @param listener The listener to add
     */
    public void addSmartBallScannerListener(SmartBallScannerListener listener)
    {
        if (listener == null)
            throw new NullPointerException("Attempted to add null SmartBallScannerListener");

        listeners.add(listener);
    }

    /**
     * Removes a SmartBallScannerListener from this SmartBallScanner.
     * @param listener The listener to remove
     */
    public void removeSmartBallScannerListener(SmartBallScannerListener listener)
    {
        listeners.remove(listener);
    }

    /**
     * Starts scanning for SmartBalls if no scan is currently in progress.
     */
    public void startScanning()
    {
        // Set flag
        isScanningStarted = true;

        // Invoke listener callbacks
        for (SmartBallScannerListener listener: listeners)
            listener.onScanStarted();

        // Start scan
        scanner.startScan(scanFilters, new ScanSettings.Builder().build(), internalCallback);
    }

    /**
     * Stops scanning for SmartBalls if there is a scan currently in progress.
     */
    public void stopScanning()
    {
        // Set flag
        isScanningStarted = false;

        // Invoke listener callbacks
        for (SmartBallScannerListener listener: listeners)
            listener.onScanStopped(false, 0);

        // Stop scan
        scanner.stopScan(internalCallback);
    }

    /*
     * Convenience method for getting the String to use as the storage key
     * for a ScanResult.
     */
    private String getResultKey(ScanResult result)
    {
        return result.getDevice().getAddress();
    }

    /*
     * Convenience method to process a found ScanResult.
     */
    private void addScanResult(ScanResult result)
    {
        // Update the result and notify listener callbacks if the result is new
        if (results.put(getResultKey(result), result) == null)
            for (SmartBallScannerListener listener : listeners)
                listener.onSmartBallFound(result);
    }

    /*
     * Convenience method to remove old ScanResults.
     */
    private void removeScanResult(ScanResult result)
    {
        // Check whether the new result is already contained
        if (results.containsKey(getResultKey(result)))
        {
            // Remove the result
            results.remove(getResultKey(result));

            // Invoke listener callbacks
            for (SmartBallScannerListener listener : listeners)
                listener.onSmartBallLost(result);
        }
    }

    /**
     * Interface for listening for SmartBall scan results.
     */
    public interface SmartBallScannerListener
    {
        /**
         * Called when scanning is started.
         */
        void onScanStarted();

        /**
         * Called when scanning is stopped.
         * @param failed True if the scan stopped due to a failure
         * @param errorCode The error code of the failure if one did occur
         */
        void onScanStopped(boolean failed, int errorCode);

        /**
         * Called when a SmartBall has been found.
         * @param result The ScanResult representing the found SmartBall.
         */
        void onSmartBallFound(ScanResult result);

        /**
         * Called when a SmartBall has been lost.
         * @param result The ScanResult representing the lost SmartBall.
         */
        void onSmartBallLost(ScanResult result);
    }
}
