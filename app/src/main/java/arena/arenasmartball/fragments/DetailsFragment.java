package arena.arenasmartball.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import arena.arenasmartball.MainActivity;
import arena.arenasmartball.R;
import arena.arenasmartball.adapters.ServicesListAdapter;
import arena.arenasmartball.ball.GattCommand;
import arena.arenasmartball.ball.GattCommandSequence;
import arena.arenasmartball.ball.SmartBall;

/**
 * Fragment containing the details of the currently connected Smart Ball.
 *
 * Created by Theodore on 4/8/2016.
 */
public class DetailsFragment extends SimpleFragment implements GattCommand.ReadGattCommand.ReadGattCommandCallback,
        GattCommandSequence.CommandSequenceCallback
{
    // The tag for this class
    private static final String TAG = "DetailsFragment";

    // The name of the command sequence used to read the SmartBall info
    private static final String READ_BALL_INFO_COM_SEQ_NAME = "DetailsFragReadBallInfo";

//    // TextView displaying the name of the ball
//    private TextView nameView;
//
//    // TextView displaying the Bluetooth address of the ball
//    private TextView addressView;

    /**
     * Required empty public constructor.
     */
    public DetailsFragment()
    {    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_details, container, false);

//        // Assign views
//        nameView = (TextView) view.findViewById(R.id.textview_details_ball_name);
//        addressView = (TextView) view.findViewById(R.id.textview_details_ball_address);

//        // Refresh
//        refreshTitleViews();

        // Initialize the service/characteristic list
        ServicesListAdapter adapter = new ServicesListAdapter(getActivity(), MainActivity.getBluetoothBridge().getSmartBall());
        ExpandableListView listView = (ExpandableListView) view.findViewById(R.id.expandablelistview_services_list);
        listView.setAdapter(adapter);

        return view;
    }

    /**
     * Called when the requested value is read.
     *
     * @param id     The id of the read command which issued this request
     * @param data   The read data
     * @param status The status of the callback
     */
    @Override
    public void onCommandRead(String id, byte[] data, int status)
    {

    }

    /**
     * Called when the GattCommandSequence experiences some event.
     *
     * @param sequence The GattCommandSequence that has experienced the event
     * @param event    The Event describing the state of the command sequence
     */
    @Override
    public void onCommandSequenceEvent(GattCommandSequence sequence, GattCommandSequence.Event event)
    {
        if (sequence.NAME.equals(READ_BALL_INFO_COM_SEQ_NAME))
        {
            // In the event of failure try to read again
            if (event == GattCommandSequence.Event.ENDED_EARLY || event == GattCommandSequence.Event.FAILED_TO_BEGIN)
            {
                Log.w(TAG, "Error starting command sequence to read ball info: " + event + ", trying again...");
                SmartBall ball = MainActivity.getBluetoothBridge().getSmartBall();

//                if (ball != null)
//                    requestReadSerialAndModelNumbers(ball);
//                else
//                    Log.w(TAG, "Error requesting ball info: ball is null");
            }
        }
    }

//    /**
//     * Sets the text in the title views of this Fragment using the currently active SmartBall.
//     */
//    private void refreshTitleViews()
//    {
//        // If there is a smart ball, re-assign views
//        SmartBall smartBall = MainActivity.getBluetoothBridge().getSmartBall();
//
//        if (smartBall != null)
//        {
//            // Set the name and address views
//            nameView.setText(concat(R.string.name, smartBall.DEVICE.getName()));
//            addressView.setText(concat(R.string.address, smartBall.DEVICE.getAddress()));
//        }
//        else
//        {
//            nameView.setText(concat(R.string.name, R.string.blank_symbol));
//            addressView.setText(concat(R.string.address, R.string.blank_symbol));
//        }
//    }

//    /*
//     * Creates and executes a CommandSequence to read the serial and model numbers from the given SmartBall.
//     * @param ball The SmartBall whose info to read
//     */
//    private void requestReadSerialAndModelNumbers(SmartBall ball)
//    {
//        Log.d(TAG, "Requesting to read SmartBall serial and model number...");
//
//        // Create the command sequence
//        GattCommandSequence sequence = new GattCommandSequence(READ_BALL_INFO_COM_SEQ_NAME, this);
//
//        // Add the command to read the serial number
//        sequence.addCommand(new GattCommand.ReadGattCommand<>
//                (ball.getCharacteristic(SmartBall.Characteristic.SERIAL_NUMBER),
//                 SmartBall.Characteristic.SERIAL_NUMBER.name(),
//                 this));
//
//        // Add the command to read the model number
//        sequence.addCommand(new GattCommand.ReadGattCommand<>
//                (ball.getCharacteristic(SmartBall.Characteristic.MODEL_NUMBER),
//                 SmartBall.Characteristic.MODEL_NUMBER.name(),
//                 this));
//
//        // Execute the sequence
//        ball.addCommandSequenceToQueue(sequence);
//    }
}
