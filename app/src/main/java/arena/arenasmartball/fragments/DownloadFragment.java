package arena.arenasmartball.fragments;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import arena.arenasmartball.BluetoothBridge;
import arena.arenasmartball.MainActivity;
import arena.arenasmartball.PeriodicUpdateThread;
import arena.arenasmartball.R;
import arena.arenasmartball.Utils;
import arena.arenasmartball.ball.GattCommand;
import arena.arenasmartball.ball.GattCommandSequence;
import arena.arenasmartball.ball.GattCommandUtils;
import arena.arenasmartball.ball.Services;
import arena.arenasmartball.ball.SmartBall;

/**
 * Fragment for the data download screen.
 *
 * Created by Theodore on 4/14/2016.
 */
public class DownloadFragment extends SimpleFragment
{
    // The tag for this class
    private static final String TAG = "DownloadFragment";

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


        return view;
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

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
            long timeOfLastDownload = bridge.getTimeOfLastDownload();
            SmartBall ball = bridge.getSmartBall();

            resetButton.setEnabled(true);
            resetButton.setText(R.string.download);
            statusView.setVisibility(timeOfLastImpact == 0 ? View.GONE : View.VISIBLE);

            if (ball.isDataTransmitInProgress())
            {
                resetButton.setText(R.string.cancel_download);
                statusView.setText(String.format(getString(R.string.downloading_data_with_type), ball.getDataTypeInTransit()));
            }
            else if (timeOfLastDownload == timeOfLastImpact)
                statusView.setText(R.string.download_complete);
            else
                statusView.setText(R.string.no_current_download);

            // TODO progress bar
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
}
