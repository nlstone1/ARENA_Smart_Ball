package arena.arenasmartball.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import arena.arenasmartball.R;
import arena.arenasmartball.ball.Services;
import arena.arenasmartball.ball.SmartBall;

/**
 * ExpandableListViewAdapter for showing a ball's services and characteristics.
 *
 * Created by Nathaniel on 4/10/2016.
 */
public class ServicesListAdapter extends BaseExpandableListAdapter
{
    // Parent Context
    private Context context;

    // List of services
//    private List<BluetoothGattService> services;
    // Map of characteristics for a SmartBall
//    private HashMap<_UUID, List<SmartBall.Characteristic>> characteristics;
    private List<List<Services.Characteristic>> data;

    // Log tag String
    private static final String TAG = "ServicesListAdapter";

    /**
     * Creates a ServicesListAdapter for the specified SmartBall.
     * @param smartBall The SmartBall
     */
    public ServicesListAdapter(Context context, SmartBall smartBall)
    {
        this.context = context;
        data = new ArrayList<>();

        init();

        Log.d(TAG, data.toString());
    }

    /**
     * Gets the number of groups.
     *
     * @return the number of groups
     */
    @Override
    public int getGroupCount()
    {
        return data.size();
    }

    /**
     * Gets the number of children in a specified group.
     *
     * @param groupPosition the position of the group for which the children
     *                      count should be returned
     * @return the children count in the specified group
     */
    @Override
    public int getChildrenCount(int groupPosition)
    {
        return data.get(groupPosition).size();
    }

    /**
     * Gets the data associated with the given group.
     *
     * @param groupPosition the position of the group
     * @return the data child for the specified group
     */
    @Override
    public Object getGroup(int groupPosition)
    {
        return data.get(groupPosition).get(0).SERVICE._UUID.toString();
    }

    /**
     * Gets the data associated with the given child within the given group.
     *
     * @param groupPosition the position of the group that the child resides in
     * @param childPosition the position of the child with respect to other
     *                      children in the group
     * @return the data of the child
     */
    @Override
    public Object getChild(int groupPosition, int childPosition)
    {
        return data.get(groupPosition).get(childPosition)._UUID.toString();
    }

    /**
     * Gets the ID for the group at the given position. This group ID must be
     * unique across groups. The combined ID (see
     * {@link #getCombinedGroupId(long)}) must be unique across ALL items
     * (groups and all children).
     *
     * @param groupPosition the position of the group for which the ID is wanted
     * @return the ID associated with the group
     */
    @Override
    public long getGroupId(int groupPosition)
    {
        return data.get(groupPosition).get(0).SERVICE._UUID.hashCode();
    }

    /**
     * Gets the ID for the given child within the given group. This ID must be
     * unique across all children within the group. The combined ID (see
     * {@link #getCombinedChildId(long, long)}) must be unique across ALL items
     * (groups and all children).
     *
     * @param groupPosition the position of the group that contains the child
     * @param childPosition the position of the child within the group for which
     *                      the ID is wanted
     * @return the ID associated with the child
     */
    @Override
    public long getChildId(int groupPosition, int childPosition)
    {
        return data.get(groupPosition).get(childPosition)._UUID.hashCode();
    }

    /**
     * Indicates whether the child and group IDs are stable across changes to the
     * underlying data.
     *
     * @return whether or not the same ID always refers to the same object
     */
    @Override
    public boolean hasStableIds()
    {
        return true;
    }

    /**
     * Gets a View that displays the given group. This View is only for the
     * group--the Views for the group's children will be fetched using
     * {@link #getChildView(int, int, boolean, View, ViewGroup)}.
     *
     * @param groupPosition the position of the group for which the View is
     *                      returned
     * @param isExpanded    whether the group is expanded or collapsed
     * @param convertView   the old view to reuse
     * @param parent        the parent that this view will eventually be attached to
     * @return the View corresponding to the group at the specified position
     */
    @SuppressLint("InflateParams")
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent)
    {
        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.details_service_list_group, null);
        }

        View view = convertView.findViewById(R.id.layout_service_group);

        TextView title = (TextView) view.findViewById(R.id.textview_service_group_title);
        title.setText((String) getGroup(groupPosition));

        return convertView;
    }

    /**
     * Gets a View that displays the data for the given child within the given
     * group.
     *
     * @param groupPosition the position of the group that contains the child
     * @param childPosition the position of the child (for which the View is returned) within the group
     * @param isLastChild   Whether the child is the last child within the group
     * @param convertView   the old view to reuse
     * @param parent        the parent that this view will eventually be attached to
     * @return the View corresponding to the child at the specified position
     */
    @SuppressLint("InflateParams")
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
    {
        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.details_service_list_item, null);
        }

        TextView textView = (TextView) convertView.findViewById(R.id.textview_group_item);
        textView.setText((String) getChild(groupPosition, childPosition));

        return convertView;
    }

    /**
     * Whether the child at the specified position is selectable.
     *
     * @param groupPosition the position of the group that contains the child
     * @param childPosition the position of the child within the group
     * @return whether the child is selectable.
     */
    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition)
    {
        return true;
    }

    /*
     * Initializes this Adapter.
     */
    private void init()
    {
        HashMap<UUID, List<Services.Characteristic>> characteristics = new HashMap<>();

        for (Services.Characteristic characteristic: Services.Characteristic.values())
        {
            addCharacteristic(characteristics, characteristic);
        }

        for (Map.Entry<UUID, List<Services.Characteristic>> entry: characteristics.entrySet())
        {
            data.add(entry.getValue());
        }
    }

    /*
     * Adds a Characteristic to the characteristics map.
     */
    private void addCharacteristic(HashMap<UUID, List<Services.Characteristic>> characteristics,
                                   Services.Characteristic characteristic)
    {
        if (!characteristics.containsKey(characteristic.SERVICE._UUID))
            characteristics.put(characteristic.SERVICE._UUID, new ArrayList<Services.Characteristic>(1));

        characteristics.get(characteristic.SERVICE._UUID).add(characteristic);
    }
}
