package arena.arenasmartball.fragments;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import arena.arenasmartball.MainActivity;
import arena.arenasmartball.R;
import arena.arenasmartball.adapters.ServicesListAdapter;
import arena.arenasmartball.ball.SmartBall;

/**
 * Fragment containing the details of the currently connected Smart Ball.
 *
 * Created by Theodore on 4/8/2016.
 */
public class DetailsFragment extends SimpleFragment
{
    // The tag for this class
    @SuppressWarnings("unused")
    private static final String TAG = "DetailsFragment";

    // Directory to which to save ball images
    private static final String IMAGE_SAVE_DIR = "Ball_Images";

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

        // Initialize the service/characteristic list
        ServicesListAdapter adapter = new ServicesListAdapter(getActivity());
        ExpandableListView listView = (ExpandableListView) view.findViewById(R.id.expandablelistview_services_list);
        listView.setAdapter(adapter);

        TextView services = (TextView) view.findViewById(R.id.textview_services);
        services.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try
                {
                    writeBallImage();
                    Toast.makeText(MainActivity.getCurrent(), "Ball image saved!", Toast.LENGTH_SHORT).show();
                }
                catch (FileNotFoundException e)
                {
                    Toast.makeText(MainActivity.getCurrent(), "Failed to write ball image!", Toast.LENGTH_SHORT).show();

                    Log.e(TAG, "", e);
                }
            }
        });

        return view;
    }

    /**
     * Creates an XML image of the smart ball's services and characteristics for emulation.
     */
    private static void writeBallImage() throws FileNotFoundException
    {
        if (MainActivity.getBluetoothBridge().getSmartBall() == null)
            return;

        SmartBall ball = MainActivity.getBluetoothBridge().getSmartBall();

        // Get the file
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        file = new File(file, IMAGE_SAVE_DIR);
        //noinspection ResultOfMethodCallIgnored
        file.mkdirs();
        file = new File(file, ball.DEVICE.getName() + "_image.xml");

        // Open the PrintWriter
        PrintWriter out = new PrintWriter((file));

        // Write the main tags
        out.printf("<server name=\"%s\">\n", ball.DEVICE.getName());

        BluetoothGatt gatt = ball.CONNECTION.getBluetoothGatt();

        // Service settings
        String uuid, value;
        boolean primary;

        // Characteristic settings
        String properties, permissions, writeType;

        for (BluetoothGattService service: gatt.getServices())
        {
            Log.d(TAG, "Service: " + service.getUuid());

            // Write the service tag
            uuid = service.getUuid().toString().substring(4, 8);
            primary = service.getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY;

            out.printf("\t<service uuid=\"%s\" primary=\"%s\"", uuid, Boolean.toString(primary));

            // TODO services could also have sub-services (but not in a SmartBall)

            if (service.getCharacteristics().isEmpty())
                out.println("/>");
            else
            {
                out.println(">");

                // Write the characteristics
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics())
                {
                    Log.d(TAG, "\tCharacteristic: " + Arrays.toString(characteristic.getValue()));

                    uuid = characteristic.getUuid().toString().substring(4, 8);
                    permissions = getPermissionsString(characteristic.getPermissions());
                    properties = getPropertiesString(characteristic.getProperties());
                    writeType = getWriteType(characteristic.getWriteType());
                    // TODO infer string and int values
                    value = "raw:" + Arrays.toString(characteristic.getValue()).replaceAll("[\\[\\]\\s]", "");

                    // Write the characteristic tag
                    out.printf("\t\t<characteristic uuid=\"%s\"", uuid);

                    if (!permissions.isEmpty())
                        out.print(" permissions=\"" + permissions + "\"");
                    if (!properties.isEmpty())
                        out.print(" properties=\"" + properties + "\"");
                    if (!writeType.equals("default"))
                        out.print(" writetype=\"" + writeType + "\"");
                    if (!value.equals("raw:null"))
                        out.print(" value=\"" + value + "\"");

                    // Write the descriptors
                    if (characteristic.getDescriptors().isEmpty())
                        out.println("/>");
                    else
                    {
                        out.println(">");

                        for (BluetoothGattDescriptor descriptor: characteristic.getDescriptors())
                        {
                            uuid = descriptor.getUuid().toString().substring(4, 8);
                            permissions = getPermissionsString(descriptor.getPermissions());
                            // TODO infer string and int values
                            value = "raw:" + Arrays.toString(descriptor.getValue()).replaceAll("[\\[\\]\\s]", "");

                            out.printf("\t\t\t<descriptor uuid=\"%s\"", uuid);

                            if (!permissions.isEmpty())
                                out.print(" permissions=\"" + permissions + "\"");
                            if (!value.equals("raw:null"))
                                out.print(" value=\"" + value + "\"");

                            out.println("/>");
                        }

                        out.println("\t\t</characteristic>");
                    }
                }

                out.println("\t</service>");
            }
        }

        out.println("</server>");

        // Finish writing
        out.close();
    }

    /*
     * Creates a string representation of the specified permissions.
     */
    private static String getPermissionsString(int perm)
    {
        List<String> p = new ArrayList<>();

        if ((perm & BluetoothGattCharacteristic.PERMISSION_READ) != 0)
            p.add("r");
        if ((perm & BluetoothGattCharacteristic.PERMISSION_WRITE) != 0)
            p.add("w");
        if ((perm & BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED) != 0)
            p.add("re");
        if ((perm & BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED) != 0)
            p.add("we");
        if ((perm & BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM) != 0)
            p.add("rem");
        if ((perm & BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM) != 0)
            p.add("wem");
        if ((perm & BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED) != 0)
            p.add("ws");
        if ((perm & BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM) != 0)
            p.add("wsm");

        if (p.isEmpty())
            return "";

        String r = p.get(0);

        for (int i = 1; i < p.size(); ++i)
            r += "," + p.get(i);

        return r;
    }

    /*
     * Creates a string representation of the specified properties.
     */
    private static String getPropertiesString(int prop)
    {
        List<String> p = new ArrayList<>();

        if ((prop & BluetoothGattCharacteristic.PROPERTY_READ) != 0)
            p.add("r");
        if ((prop & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0)
            p.add("w");
        if ((prop & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0)
            p.add("wnr");
        if ((prop & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) != 0)
            p.add("sw");
        if ((prop & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0)
            p.add("i");
        if ((prop & BluetoothGattCharacteristic.PROPERTY_BROADCAST) != 0)
            p.add("b");
        if ((prop & BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) != 0)
            p.add("x");

        if (p.isEmpty())
            return "";

        String r = p.get(0);

        for (int i = 1; i < p.size(); ++i)
            r += "," + p.get(i);

        return r;
    }

    /*
     * Creates a String representation of a write type.
     */
    private static String getWriteType(int writeType)
    {
        if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            return "default";
        else if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
            return "no_response";
        else
            return "signed";
    }
}
