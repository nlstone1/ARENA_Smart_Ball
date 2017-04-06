package arena.arenasmartball.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import arena.arenasmartball.BluetoothBridge;
import arena.arenasmartball.MainActivity;
import arena.arenasmartball.R;
import arena.arenasmartball.Utils;
import arena.arenasmartball.ball.GattCommandUtils;
import arena.arenasmartball.ball.SmartBall;
import arena.arenasmartball.data.Impact;
import arena.arenasmartball.views.DataView;

/**
 * Fragment for the data download screen.
 *
 * Created by Theodore on 4/14/2016.
 */
public class DownloadFragment extends SimpleFragment implements View.OnClickListener, SmartBall.DataListener
{
    /** The name of the directory in which to save impacts. */
    public static final String DATA_SAVE_DIRECTORY = "SmartBallData";

    // The tag for this class
    private static final String TAG = "DownloadFragment";

    // Denotes whether or not a data transmission was begun
    private static boolean transmissionBegun;

    // Whether download should begin automatically
    private static boolean shouldBeginDownload;

//    // Records the time of the last data download request
//    private static long timeOfLastDownload;

    // Views
    private TextView titleView;
    private TextView statusView;
    private ProgressBar progressBar;
    private Button resetButton;
    private Button saveButton;
    private DataView dataView;

    /**
     * Required empty public constructor.
     */
    public DownloadFragment()
    {    }

//    /**
//     * Creates a file name for the given Impact.
//     * @param impact The Impact for which to get a file name
//     * @return The file name
//     */
//    public static String createFileName(Impact impact)
//    {
//        Calendar c = Calendar.getInstance();
//        c.setTimeInMillis(impact.getTime());
//
//        return String.format(Locale.ENGLISH, "SBDATA_%04d%02d%02d_%02d%02d%02d", c.get(Calendar.YEAR),
//                c.get(Calendar.DAY_OF_MONTH), 1 + c.get(Calendar.MONTH),
//                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
//    }

//    @Override
//    public void load(@NonNull Bundle bundle)
//    {
//        dataView.load(bundle);
//    }
//
//    @Override
//    public void save(@NonNull Bundle bundle)
//    {
//        dataView.save(bundle);
//    }

    @Override
    public void onOpen()
    {
        super.onOpen();

        if (shouldBeginDownload)
        {
            shouldBeginDownload = false;
            beginDownload();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_download, container, false);

        // Get Views
        titleView = (TextView) view.findViewById(R.id.textview_download_title);
        statusView = (TextView) view.findViewById(R.id.textview_download_status);
        progressBar = (ProgressBar) view.findViewById(R.id.progressbar_download);
        resetButton = (Button) view.findViewById(R.id.button_download_read);
        saveButton = (Button) view.findViewById(R.id.button_download_save);
        dataView = (DataView) view.findViewById(R.id.dataview_dataview);

        // Set defaults
        setValuesForCurrentState(MainActivity.getBluetoothBridge());

        // Add listeners
        resetButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);

        SmartBall ball = MainActivity.getBluetoothBridge().getSmartBall();

        if (ball != null)
            ball.addDataListener(this);

        return view;
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

        SmartBall ball = MainActivity.getBluetoothBridge().getSmartBall();

        if (ball != null)
            ball.removeDataListener(this);
    }

    /*
     * Sets the values for the views of this Fragment for the current state
     */
    private void setValuesForCurrentState(BluetoothBridge bridge)
    {
        long timeOfLastImpact = bridge.getTimeOfLastImpact();

        if (timeOfLastImpact == 0)
            titleView.setText(concat(R.string.last_impact, R.string.blank_symbol));
        else
            titleView.setText(concat(R.string.last_impact, Utils.getDateString(timeOfLastImpact)));

        if (bridge.getSmartBallConnection() == null || bridge.getState() != BluetoothBridge.State.CONNECTED)
            setValuesForNoConnection();
        else
        {
            SmartBall ball = bridge.getSmartBall();
            Impact impact = bridge.getLastImpact();

            saveButton.setEnabled(false);
            resetButton.setEnabled(impact != null);
            resetButton.setText(R.string.download);
            statusView.setVisibility(timeOfLastImpact == 0 ? View.GONE : View.VISIBLE);

            if (impact == null)
                return;

            if (ball.isDataTransmitInProgress())
            {
                resetButton.setText(R.string.cancel_download);
                resetButton.setEnabled(transmissionBegun);
                statusView.setText(String.format(getString(R.string.downloading_data_with_type), ball.getDataTypeInTransit()));
            }
            else if (impact.wasCancelled())
                statusView.setText(R.string.download_cancelled);
            else if (impact.isComplete())
            {
                statusView.setText(R.string.download_complete);
                saveButton.setEnabled(true);
            }
            else
                statusView.setText(R.string.no_current_download);

            updateDataViews(impact);
        }
    }

    /*
     * Sets the values for the views of this Fragment for when there is no connection.
     */
    private void setValuesForNoConnection()
    {
        statusView.setVisibility(View.GONE);
        resetButton.setEnabled(false);
        resetButton.setText(R.string.download);
        progressBar.setProgress(0);
        saveButton.setEnabled(false);
    }

    /*
     * Updates the views in this Fragment concerning the data of the impact.
     */
    private void updateDataViews(Impact impact)
    {
        // Set the progress
        if (impact.getImpactData() != null)
            progressBar.setProgress(impact.getImpactData().getPercentComplete());
        else
            progressBar.setProgress(0);

//        if (impact.getDataInTransit() != null)
//        {
//            progressBar.setProgress(impact.getDataInTransit().getTransmissionProgress());
//
////            Log.d(TAG, "Progress: " + impact.getDataInTransit().getTransmissionProgress());
//        }
//        else
//        {
//            progressBar.setProgress(0);
////            Log.d(TAG, "Progress Reset");
//        }

        // Invalidate the DataView
        dataView.getViewUpdater().redraw(true);
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
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v)
    {
        SmartBall ball = MainActivity.getBluetoothBridge().getSmartBall();

        if (ball == null)
            return;

        // Download requested
        if (v.getId() == R.id.button_download_read)
        {
            beginDownload();
        }
        // Save requested
        else if (v.getId() == R.id.button_download_save)
        {
            // Get the Impact
            Impact impact = MainActivity.getBluetoothBridge().getLastImpact();

            if (impact == null)
            {
                Log.e(TAG, "Attempting to save a null Impact");
                return;
            }

            saveImpact(impact);

//            // Create the directory in which to save the impact data
//            File impactDir = new File(file, createFileName(impact));
//
//            if (!impactDir.mkdirs() && !file.isDirectory())
//                Log.w(TAG, "Error making smart ball data impacy directory: " + impactDir.getAbsolutePath());
//
//            // Save the data
//            int numSaved = 0;
//            if (impact.getTypeOneData() != null)
//                numSaved += impact.getTypeOneData().save(impactDir);
//
//
//            if (impact.getTypeTwoData() != null)
//                numSaved += impact.getTypeTwoData().save(impactDir);
//
//            // Print Toast
//            Toast.makeText(getMainActivity(), "Saved " + numSaved + " file(s) to " + impactDir.getName(), Toast.LENGTH_SHORT).show();
        }

        setValuesForCurrentState(MainActivity.getBluetoothBridge());
    }

    /**
     * Begins download of data from the ball.
     */
    public void beginDownload()
    {
        SmartBall ball = MainActivity.getBluetoothBridge().getSmartBall();

        if (ball == null)
            return;

        ball.addDataListener(this);

        // Cancel the transmission if one is in progress
        if (ball.isDataTransmitInProgress())
            GattCommandUtils.executeEndTransmissionCommandSequence(ball, null);
        else
        {
            transmissionBegun = false;
            GattCommandUtils.executeDataTransmitCommandSequence(ball, 1096, 2, null); // TODO, 1096 is max
        }
    }

    /**
     * Sets whether the next download should begin automatically.
     * @param automaticDownload Whether to begin download automatically
     */
    public static void setAutomaticDownload(boolean automaticDownload)
    {
        shouldBeginDownload = automaticDownload;
    }

    /**
     * Saves an Impact.
     */
    public static void saveImpact(Impact impact)
    {
        // Check SD card state
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state) || !Environment.MEDIA_MOUNTED.equals(state))
            Log.e(TAG, "Error: external storage is read only or unavailable");
        else
            Log.d(TAG, "External storage is not read only or unavailable");

        // Get the parent directory in which to save impacts, which will contain folders for each impact
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        file = new File(file, DATA_SAVE_DIRECTORY);

        if (!file.mkdirs() && !file.isDirectory())
            Log.w(TAG, "Error making smart ball data directory: " + file.getAbsolutePath());

        // Save the data and print a Toast
        if (impact.save(file, true, false))
            Toast.makeText(MainActivity.getCurrent(), "Saved impact to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(MainActivity.getCurrent(), "Error saving impact", Toast.LENGTH_SHORT).show();
    }

    /**
     * Called when kick data is read.
     *
     * @param ball  The SmartBall
     * @param data  The read data
     * @param start true if this value is the data start code
     * @param end   true if this value is the data end code
     * @param type  The type of data read
     */
    @Override
    public void onSmartBallDataRead(SmartBall ball, byte[] data, boolean start, boolean end, byte type)
    {
        updateDataViews(MainActivity.getBluetoothBridge().getLastImpact());
    }

    /**
     * Called on a data transmission event.
     *
     * @param ball     The SmartBall
     * @param dataType The type of data in the transmission
     * @param event    The DataEvent that occurred
     */
    @Override
    public void onSmartBallDataTransmissionEvent(SmartBall ball, byte dataType, SmartBall.DataEvent event, int numSamples)
    {
        Log.w(TAG, "Data Transmission Event: " + event.name() + ", DataType = " + dataType);

        if (event == SmartBall.DataEvent.TRANSMISSION_BEGUN)
            transmissionBegun = true;

        getMainActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                setValuesForCurrentState(MainActivity.getBluetoothBridge());
            }
        });
    }
}
