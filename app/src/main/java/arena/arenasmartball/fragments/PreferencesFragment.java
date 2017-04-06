package arena.arenasmartball.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;

import arena.arenasmartball.MainActivity;
import arena.arenasmartball.R;
import arena.arenasmartball.adapters.ServicesListAdapter;

/**
 * Fragment for showing app settings.
 *
 * Created by Nathaniel on 4/6/17.
 */

public class PreferencesFragment extends SimpleFragment
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_preferences, container, false);

        getFragmentManager().beginTransaction().replace(
                R.id.framelayout_fragment_preferences,
                new PreferenceSubFragment()).commit();

        return view;
    }

    /**
     * Gets the value of the automatic download preference.
     * @return The value
     */
    public static boolean isAutomaticDownload()
    {
        SharedPreferences prefs = MainActivity.getPrefs();

        if (prefs != null)
        {
            return prefs.getBoolean(
                    MainActivity.getStringRes(R.string.pref_automatic_download_id),
                    MainActivity.getBoolRes(R.bool.automatic_download_default));
        }

        return MainActivity.getBoolRes(R.bool.automatic_download_default);
    }

    /**
     * PreferenceFragment nested within this SimpleFragment.
     */
    public static class PreferenceSubFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }

    }
}
