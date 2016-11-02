package arena.arenasmartball.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import java.io.File;

import arena.arenasmartball.BluetoothBridge;
import arena.arenasmartball.MainActivity;
import arena.arenasmartball.R;
import arena.arenasmartball.ball.ContinuousReadController;
import arena.arenasmartball.ball.GattCommandSequence;
import arena.arenasmartball.ball.SmartBall;
import arena.arenasmartball.data.ImpactData;
import arena.arenasmartball.views.DataView;

/**
 * Fragment for the continuous read screen.
 *
 * Created by Nathaniel on 10/28/2016.
 */

public class ContinuousReadFragment extends SimpleFragment implements View.OnClickListener, SmartBall.DataListener
{
    // Buttons
    private Button startButton;
    private Button clearButton;
    private Button saveButton;

    // Progress Bar
    private ProgressBar progressBar;

    // DataView
    private DataView dataView;

    // State info
    private static ContinuousReadController crc;
    private static File dstFile;

    private static final String KEY_IS_RECORDING = "arena.arenasmartball.fragments.ContinuousReadFragment.isRecording";

    /**
     * Required empty Constructor.
     */
    public ContinuousReadFragment()
    {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_continuousread, container, false);

        // Get views
        startButton = (Button) view.findViewById(R.id.button_contread_start_stop);
        clearButton = (Button) view.findViewById(R.id.button_contread_clear);
        saveButton = (Button) view.findViewById(R.id.button_contread_save);
        progressBar = (ProgressBar) view.findViewById(R.id.progressbar_download);
        dataView = (DataView) view.findViewById(R.id.dataview_dataview);

        // Setup DataView
        dataView.setDataSupplier(new DataView.Supplier<ImpactData>() {
            @Override
            public ImpactData get() {
                return crc.getData();
            }
        });

        // Set onClickListeners
        startButton.setOnClickListener(this);
        clearButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);

        // Set the CRC
        if (crc == null)
        {
            crc = new ContinuousReadController(MainActivity.getBluetoothBridge().getSmartBall());
            crc.setDataListener(this);
        }

        // Set defaults
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
    }

    /**
     * Save any state information here.
     * @param bundle The Bundle to which to save
     */
    @Override
    public void save(@NonNull Bundle bundle)
    {
    }

    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.button_contread_start_stop)
        {
            if (crc.isRecording())
                crc.stopRecording();
            else
                crc.startRecording();
        }
        else if (v.getId() == R.id.button_contread_clear)
        {
            crc.clear();
        }
        else if (v.getId() == R.id.button_contread_save)
        {
            crc.save();
        }

        setValuesForCurrentState(MainActivity.getBluetoothBridge());
    }

    /*
     * Sets the values for the views of this Fragment for the current state
     */
    private void setValuesForCurrentState(BluetoothBridge bridge)
    {
        if (bridge.getSmartBallConnection() == null || bridge.getState() != BluetoothBridge.State.CONNECTED)
            setValuesForNoConnection();
        else
        {
//            SmartBall ball = bridge.getSmartBall();

            startButton.setEnabled(true);
            startButton.setText(crc.isRecording() ? R.string.stop: R.string.start);

            if (!crc.isRecording() && crc.getData().getNumSamples() > 0)
            {
                clearButton.setEnabled(true);
                saveButton.setEnabled(true);
            }
            else
            {
                clearButton.setEnabled(false);
                saveButton.setEnabled(false);
            }

            if (crc.isRecording())
            {
                progressBar.setIndeterminate(true);
            }
            else
            {
                progressBar.setProgress(0);
                progressBar.setIndeterminate(false);
            }

        }
    }

    /*
     * Sets the values for the views of this Fragment for when there is no connection.
     */
    private void setValuesForNoConnection()
    {
        saveButton.setEnabled(false);
        startButton.setEnabled(false);
        clearButton.setEnabled(false);
    }

    @Override
    public void onSmartBallDataRead(SmartBall ball, byte[] data, boolean start, boolean end, byte type)
    {
        if (!(end || start))
        {
            // Invalidate the DataView
            dataView.getViewUpdater().redraw(true);
        }
    }

    @Override
    public void onSmartBallDataTransmissionEvent(SmartBall ball, byte dataType, SmartBall.DataEvent event, int numSamples)
    {

    }
}
