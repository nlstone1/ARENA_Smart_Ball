package arena.arenasmartball;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.Date;

/**
 * Class containing useful functions.
 *
 * Created by Nathaniel on 4/5/2016.
 */
public class Utils
{
    /** Constant to convert degrees to radians through multiplication */
    public static final float DEG_2_RAD = (float) Math.toRadians(1.0);

    // Log tag String
    private static final String TAG = "Utils";

    // Date reference for convenience
    private static final Date DATE = new Date();

    // The DateFormatter
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();

    /**
     * Calculates the signal strength of the given rssi measurement.
     * @param rssi The rssi
     * @return The signal strength, in the range [0, 100]
     */
    public static int getRSSISignalStrength(int rssi)
    {
        return WifiManager.calculateSignalLevel(rssi, 101);
    }

    /**
     * Gets a formatted date String from a time in milliseconds from Jan 1, 1970.
     * @param timeMillis The time in milliseconds from Jan 1, 1970
     * @return A formatted date String from a time in milliseconds from Jan 1, 1970
     */
    public static String getDateString(long timeMillis)
    {
        DATE.setTime(timeMillis);
        return DATE_FORMAT.format(DATE);
    }

    /**
     * Tests two ScanResults for equality, comparing device addresses.
     * @param a The first ScanResult to test
     * @param b The second ScanResult to test
     * @return Whether or not the two ScanResults represent the same device
     */
    public static boolean areEqual(ScanResult a, ScanResult b)
    {
        return !(a == null || b == null) && areEqual(a.getDevice(), b.getDevice());
    }

    /**
     * Tests two BluetoothDevice for equality, comparing addresses.
     * @param a The first BluetoothDevice to test
     * @param b The second BluetoothDevice to test
     * @return Whether or not the two BluetoothDevice represent the same device
     */
    public static boolean areEqual(BluetoothDevice a, BluetoothDevice b)
    {
        return !(a == null || b == null) && a.getAddress().contentEquals(b.getAddress());
    }

    /**
     * Calculates the squared distance between the two points.
     * @param x1 The x position of the first point
     * @param y1 The y position of the first point
     * @param x2 The x position of the second point
     * @param y2 The y position of the second point
     * @return The squared distance between the two points
     */
    public static float distanceSquared(float x1, float y1, float x2, float y2)
    {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }

    /**
     * Calculates the distance between the two points.
     * @param x1 The x position of the first point
     * @param y1 The y position of the first point
     * @param x2 The x position of the second point
     * @param y2 The y position of the second point
     * @return The squared distance between the two points
     */
    public static float distance(float x1, float y1, float x2, float y2)
    {
        return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    /**
     * Returns the direction from (x1, y1) to (x2, y2).
     * @param x1 The x position of the first point
     * @param y1 The y position of the first point
     * @param x2 The x position of the second point
     * @param y2 The y position of the second point
     * @return The direction from (x1, y1) to (x2, y2), in degrees
     */
    public static float pointDirection(float x1, float y1, float x2, float y2)
    {
        return (float)Math.toDegrees(Math.atan2(y1-y2, x2-x1));
    }

    /**
     * Returns the difference in degrees between the given directions. All directions are assumed to be measured with respect to the positive x axis.
     * @param direction1 The first direction
     * @param direction2 The second direction
     * @return The angle in degrees, equivalent to direction1 - direction2.
     */
    public static float angleDifference(float direction1, float direction2)
    {
        return ((((direction1 - direction2) % 360.0f) + 540.0f) % 360.0f) - 180.0f;
    }

    /**
     * Gets a percentage display String for a number.
     * @param num The number
     * @return A percentage display String for a number
     */
    public static String getPercentString(double num)
    {
        return Math.round(num) + " %";
    }

    /**
     * Reads a text file as a String.
     * @param context The current Context
     * @param resId The resource id of the text file
     * @return The String contents of the File or an empty String if an error occurs
     */
    public static String readTextFile(Context context, int resId)
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getResources().openRawResource(resId)));

        String string = "";
        try
        {
            String line = reader.readLine();

            while (line != null)
            {
                string += line + "\n";
                line = reader.readLine();
            }
        }
        catch (IOException e)
        {
            Log.e(TAG, "Error reading text file", e);
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch (IOException ignore) {}
        }

        return string;
    }
}
