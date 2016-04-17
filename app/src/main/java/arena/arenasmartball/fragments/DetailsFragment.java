package arena.arenasmartball.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import arena.arenasmartball.R;
import arena.arenasmartball.adapters.ServicesListAdapter;

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

        return view;
    }
}
