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
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import arena.arenasmartball.BluetoothBridge;
import arena.arenasmartball.DrawerItem;
import arena.arenasmartball.MainActivity;
import arena.arenasmartball.PeriodicUpdateThread;
import arena.arenasmartball.R;
import arena.arenasmartball.ball.GattCommand;
import arena.arenasmartball.ball.GattCommandSequence;
import arena.arenasmartball.ball.GattCommandUtils;
import arena.arenasmartball.ball.Services;
import arena.arenasmartball.ball.SmartBall;

/**
 * Fragment for the capture impact screen.
 *
 * Created by Theodore on 4/14/2016.
 */
public class CaptureFragment extends SimpleFragment implements View.OnClickListener, SmartBall.EventListener,
        GattCommandSequence.CommandSequenceCallback, SmartBall.CharacteristicListener
{
    // The tag for this class
    private static final String TAG = "CaptureFragment";

    // The name of the command sequence used to read the SmartBall countdown timer
    private static final String READ_BALL_COUNTDOWN_COM_SEQ_NAME = "CaptureFragReadBallInfo";

    // The amount of time to wait for the kick bit to be set before announcing an error
    private static final long KICK_BIT_RESPONSE_DELAY = 4000L;

    // The maximum value of the countdown timer
    private static final int MAX_COUNTDOWN_TIMER_VALUE = 90;

    // Reset flag
    private static boolean resetCalled;

    // Countdown timer
    private static int countdownTimer;

    // Time of last button press
//    private static long timeOfLastCapture = System.currentTimeMillis();

    // Views
    private TextView readyView;
    private TextView countdownView;
    private Button captureButton;
    private Button resetButton;
    private Button downloadButton;

    // The Thread to read the ball timer
    private TimerReader timerReader;

    /**
     * Required empty public constructor.
     */
    public CaptureFragment()
    {    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_capture, container, false);

        // Get Views
        readyView = (TextView) view.findViewById(R.id.textview_capture_ready);
        countdownView = (TextView) view.findViewById(R.id.textview_capture_countdown);
        captureButton = (Button) view.findViewById(R.id.button_capture_capture);
        resetButton = (Button) view.findViewById(R.id.button_capture_reset);
        downloadButton = (Button) view.findViewById(R.id.button_capture_download);

        // Set defaults
        setValuesForCurrentState(MainActivity.getBluetoothBridge());

        // Add listeners
        captureButton.setOnClickListener(this);
        resetButton.setOnClickListener(this);
        downloadButton.setOnClickListener(this);

        // Create timer reader
        if (timerReader != null)
        {
            timerReader.kill();
            timerReader.unblock();
        }
        timerReader = new TimerReader();
        timerReader.start();

        return view;
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

        if (timerReader != null)
        {
            timerReader.kill();
            timerReader.unblock();
        }

        if (MainActivity.getBluetoothBridge().getSmartBall() != null)
        {
            MainActivity.getBluetoothBridge().getSmartBall().removeCharacteristicListener(this);
            MainActivity.getBluetoothBridge().getSmartBall().removeEventListener(this);
        }
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
            SmartBall ball = bridge.getSmartBall();

            if (ball.getKickBit())
            {
                readyView.setBackgroundResource(R.color.colorReadyOn);
                captureButton.setEnabled(false);
            }
            else
            {
                readyView.setBackgroundResource(R.color.colorReadyOff);
                captureButton.setEnabled(!ball.isDataTransmitInProgress() &&
                        System.currentTimeMillis() - bridge.getTimeOfLastImpact() > 1000L); // TODO
            }

            countdownView.setText("" + countdownTimer);
            resetButton.setEnabled(true);
            downloadButton.setEnabled(true); // TODO
        }
    }

    /*
     * Sets the values for the views of this Fragment for when there is no connection.
     */
    private void setValuesForNoConnection()
    {
        readyView.setBackgroundResource(R.color.colorReadyOff);
        readyView.setTextColor(getResources().getColor(R.color.colorVDkGray));

        countdownView.setText("0");
        countdownView.setTextColor(getResources().getColor(R.color.colorLtGray));

        captureButton.setEnabled(false);
        resetButton.setEnabled(false);
        downloadButton.setEnabled(false);
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
        if (v.getId() == R.id.button_capture_capture)
        {
            final SmartBall ball = MainActivity.getBluetoothBridge().getSmartBall();

            if (ball != null)
            {
                resetCalled = false;
                ball.getEventListeners().add(this);
                ball.addCharacteristicListener(this, Services.Characteristic.TIMEOUT_COUNTER);
                GattCommandUtils.executeKickCommandSequence(ball, this);

                // Check after KICK_BIT_RESPONSE_DELAY ms whether the kick bit has been set. If not, cancel: there is an error.
                v.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        // TODO removed a ! from in front of KickBit
                        if (!resetCalled && ball.getKickBit() /*&& (requestedTypeTwoData || requestedTypeOneData)*/) // Error
                        {
                            ball.flushCommandQueue();
                            ball.clearDataTransmitInProgressFlag();
                            GattCommandUtils.executeEndTransmissionCommandSequence(ball, null);

                            // Reset the permissions
                            GattCommand.reset();

                            // Re-enable notifies on the KickBit
                            BluetoothGattCharacteristic characteristic;
                            BluetoothGattDescriptor descriptor;
                            characteristic = ball.getCharacteristic(Services.Characteristic.KICK_BIT);
                            descriptor = SmartBall.getDescriptor(characteristic, "2902");
                            GattCommand.enableNotifies(ball.CONNECTION.getBluetoothGatt(), characteristic, descriptor);

                            getMainActivity().runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    setValuesForCurrentState(MainActivity.getBluetoothBridge());
                                    Toast.makeText(getMainActivity(), "Ball Response Timeout", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                }, KICK_BIT_RESPONSE_DELAY);
            }
        }
        else if (v.getId() == R.id.button_capture_reset)
        {
            final SmartBall ball = MainActivity.getBluetoothBridge().getSmartBall();

            if (ball != null)
            {
                resetCalled = true;
                countdownTimer = 0;
                ball.getEventListeners().add(this);
                GattCommandUtils.executeEndTransmissionCommandSequence(ball, null);
            }
        }
        else if (v.getId() == R.id.button_capture_download)
        {
            DrawerItem.DOWNLOAD_DRAWER.openDrawer(getMainActivity());
            return;
        }

        setValuesForCurrentState(MainActivity.getBluetoothBridge());
    }

    /**
     * Called when the SmartBall experiences a kick event.
     *
     * @param ball  The SmartBall
     * @param event The kick event
     */
    @Override
    public void onBallKickEvent(SmartBall ball, SmartBall.KickEvent event)
    {
        Log.d(TAG, "onBallKickEvent(): Event = " + event.name());

        if (event == SmartBall.KickEvent.READY_TO_KICK)
        {
            countdownTimer = MAX_COUNTDOWN_TIMER_VALUE;
            timerReader.unblock();
            getMainActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Toast.makeText(getMainActivity(), getString(R.string.ready), Toast.LENGTH_SHORT).show();
                }
            });
        }
        else if (event == SmartBall.KickEvent.KICKED)
        {
            countdownTimer = 0;
            timerReader.block();

            if (!resetCalled)
            {
                getMainActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText(getMainActivity(), getString(R.string.impact_detected), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else
            {
                getMainActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText(getMainActivity(), getString(R.string.ball_reset), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        // Update
        getMainActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                setValuesForCurrentState(MainActivity.getBluetoothBridge());
            }
        });

    }

    /**
     * Called when all BLE characteristics have been discovered.
     *
     * @param ball The SmartBall
     */
    @Override
    public void onBallCharacteristicDiscoveryCompleted(SmartBall ball)
    {    }

    /**
     * Called when the GattCommandSequence experiences some event.
     *
     * @param sequence The GattCommandSequence that has experienced the event
     * @param event    The Event describing the state of the command sequence
     */
    @Override
    public void onCommandSequenceEvent(GattCommandSequence sequence, GattCommandSequence.Event event)
    {
//        GattCommandSequence.Event.
    }

    /**
     * Called when the Characteristic for which this CharacteristicListener is listening changes.
     *
     * @param gatt           The BluetoothGatt
     * @param characteristic The changed Characteristic
     */
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
    {
        byte[] data = characteristic.getValue();
        Log.d(TAG, "Countdown Timer Characteristic changed: " + Arrays.toString(data));
        countdownTimer = MAX_COUNTDOWN_TIMER_VALUE - 10 * (data[0] - 1);
    }

//    /**
//     * Called when the requested value is read.
//     *
//     * @param id     The id of the read command which issued this request
//     * @param data   The read data
//     * @param status The status of the callback
//     */
//    @Override
//    public void onCommandRead(String id, byte[] data, int status)
//    {
//        if (status == BluetoothGatt.GATT_FAILURE)
//            return;
//
//        if (id.equals(Services.Characteristic.TIMEOUT_COUNTER.name()))
//        {
//            countdownTimer = MAX_COUNTDOWN_TIMER_VALUE - 10 * (data[0] - 1);
//        }
//
//        try
//        {
//            getActivity().runOnUiThread(new Runnable()
//            {
//                @Override
//                public void run()
//                {
//                    setValuesForCurrentState(MainActivity.getBluetoothBridge()); // TODO more efficient
//                }
//            });
//        }
//        catch (Exception e)
//        { /* Ignore */ }
//    }

    /*
     * Updates the timer view.
     */
    private void updateTimerView()
    {
        try
        {
            getMainActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    countdownView.setText("" + countdownTimer);
                }
            });
        }
        catch (Exception e)
        { /* Ignore */ }
    }

//    /*
//    * Creates and executes a CommandSequence to read the countdown timer of the ball.
//    * @param ball The SmartBall whose info to read
//    */
//    private void requestReadCountdownTimer()
//    {
//        SmartBall ball = MainActivity.getBluetoothBridge().getSmartBall();
//
//        if (ball == null)
//            return;
//
//        // Create the command sequence
//        GattCommandSequence sequence = new GattCommandSequence(READ_BALL_COUNTDOWN_COM_SEQ_NAME, this);
//
//        // Get the characteristic
//        BluetoothGattCharacteristic countdownChar = ball.getCharacteristic(Services.Characteristic.TIMEOUT_COUNTER);
//
//        // Add the command to read the battery level
//        if (countdownChar != null)
//            sequence.addCommand(new GattCommand.ReadGattCommand<>
//                    (countdownChar, Services.Characteristic.TIMEOUT_COUNTER.name(), this));
//
//        // Execute the sequence
//        ball.addCommandSequenceToQueue(sequence);
//    }

    /**
     * Thread for reading the countdown timer.
     *
     * Created by Theodore on 4/14/2016.
     */
    public class TimerReader extends PeriodicUpdateThread
    {
//        // The delay for reading the timer
//        private final int TIMER_READ_DELAY_S = 9;
//
//        // The delay to read the timer
//        private int readTimerDelay;

        /**
         * Constructor.
         */
        public TimerReader()
        {
            super(1000L);
        }

        /**
         * Called when this Thread is updates.
         */
        @Override
        public void onUpdate()
        {
            --countdownTimer;

            if (countdownTimer < 0)
                countdownTimer = 0;

            updateTimerView();
        }

//        /**
//         * Resets the delay timer and blocked status (to unblocked) of this Thread.
//         */
//        @Override
//        public void reset()
//        {
//            readTimerDelay = 0;
//            super.reset();
//        }
    }
}
