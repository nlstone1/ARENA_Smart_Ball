package arena.arenasmartball.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import arena.arenasmartball.BluetoothBridge;
import arena.arenasmartball.MainActivity;
import arena.arenasmartball.R;
import arena.arenasmartball.Utils;
import arena.arenasmartball.ball.GattCommandUtils;
import arena.arenasmartball.ball.SmartBall;
import arena.arenasmartball.data.Impact;

/**
 * Fragment for the data download screen.
 *
 * Created by Theodore on 4/14/2016.
 */
public class DownloadFragment extends SimpleFragment implements View.OnClickListener, SmartBall.DataListener
{
    // The tag for this class
    private static final String TAG = "DownloadFragment";

    // Denotes whether or not a data transmission was begun
    private static boolean transmissionBegun;

//    // Records the time of the last data download request
//    private static long timeOfLastDownload;

    // Views
    private TextView titleView;
    private TextView statusView;
    private ProgressBar progressBar;
    private Button resetButton;

    /**
     * Required empty public constructor.
     */
    public DownloadFragment()
    {    }

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

        // Set defaults
        setValuesForCurrentState(MainActivity.getBluetoothBridge());

        // Add listeners
        resetButton.setOnClickListener(this);

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
//            long timeOfLastDownload = bridge.getTimeOfLastDownload();
            SmartBall ball = bridge.getSmartBall();
            Impact impact = bridge.getLastImpact();

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
            else if (impact.isComplete())
                statusView.setText(R.string.download_complete);
            else if (impact.wasCancelled())
                statusView.setText(R.string.download_cancelled);
            else
                statusView.setText(R.string.no_current_download);

            if (impact.getDataInTransit() != null)
                progressBar.setProgress(impact.getDataInTransit().getTransmissionProgress());
            else
                progressBar.setProgress(0);
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

        if (v.getId() == R.id.button_download_read)
        {
            ball.addDataListener(this);

            // Cancel the transmission if one is in progress
            if (ball.isDataTransmitInProgress())
                GattCommandUtils.executeEndTransmissionCommandSequence(ball, null);
            else
            {
                transmissionBegun = false;
                GattCommandUtils.executeDataTransmitCommandSequence(ball, 1096, 2, null); // TODO
            }

        }

        setValuesForCurrentState(MainActivity.getBluetoothBridge());
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
