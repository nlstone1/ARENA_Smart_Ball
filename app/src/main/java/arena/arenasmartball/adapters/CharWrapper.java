package arena.arenasmartball.adapters;

import android.widget.TextView;

import arena.arenasmartball.MainActivity;
import arena.arenasmartball.R;
import arena.arenasmartball.ball.GattCommand;
import arena.arenasmartball.ball.GattCommandSequence;
import arena.arenasmartball.ball.GattCommandUtils;
import arena.arenasmartball.ball.Services;
import arena.arenasmartball.ball.SmartBall;

/**
 * Wrapper class for Characteristics used by the ServicesListAdapter.
 * TODO different value types built in
 *
 * Created by Nathaniel on 4/14/2016.
 */
class CharWrapper
{
    /** The wrapped Characteristic */
    public final Services.Characteristic CHARACTERISTIC;

    private boolean isRead;
    private byte[] value;

    /**
     * Creates a CharWrapper for the specified Characteristic.
     * @param characteristic The Characteristic
     */
    public CharWrapper(Services.Characteristic characteristic)
    {
        CHARACTERISTIC = characteristic;

        if (CHARACTERISTIC == null)
            throw new NullPointerException("CHARACTERISTIC can not be null!");

        isRead = false;
        value = null;
    }

    /**
     * Sets TextView's value to that of the wrapped Characteristic.
     * @param textView The TextView
     */
    public void assignValueTo(final TextView textView)
    {
        final SmartBall ball = MainActivity.getBluetoothBridge().getSmartBall();

        if (ball == null)
        {
            textView.setText(R.string.blank_symbol);
            return;
        }

        if (value == null && !isRead)
        {
            isRead = true;
            textView.setText(R.string.requesting_value);

            GattCommandUtils.executeCommand(ball, new GattCommand.ReadGattCommand<>(
                    ball.getCharacteristic(CHARACTERISTIC), null,
                new GattCommand.ReadGattCommand.ReadGattCommandCallback()
                {
                    @Override
                    public void onCommandRead(String id, final byte[] data, int status)
                    {
                        value = data;
                        textView.post(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                textView.setText(new String(data));
                            }
                        });
                    }
                }), CHARACTERISTIC.name(), new GattCommandSequence.CommandSequenceCallback()
                {
                    @Override
                    public void onCommandSequenceEvent(GattCommandSequence sequence, GattCommandSequence.Event event)
                    {
                        if (event == GattCommandSequence.Event.FAILED_TO_BEGIN
                                || event == GattCommandSequence.Event.ENDED_EARLY)
                        {
                            textView.post(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    textView.setText(R.string.unable_to_read);
                                }
                            });
                        }
                        else
                        {
                            textView.post(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    textView.setText(R.string.value_received);
                                }
                            });
                        }
                    }
                });
        }
        else if (value != null)
        {
            textView.setText(new String(value));
        }
        else
        {
            textView.setText(R.string.requesting_value);
        }
    }
}
