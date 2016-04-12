package arena.arenasmartball.fragments;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import arena.arenasmartball.R;
import arena.arenasmartball.Utils;

/**
 * Dialog Fragment for showing About information.
 *
 * Created by Nathaniel on 4/5/2016.
 */
public class AboutFragment extends DialogFragment
{
    // The contents of the About dialog
    private static String aboutText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_about_dialog, container, false);

        if (aboutText == null) // TODO load async
            aboutText = Utils.readTextFile(getActivity(), R.raw.about);

        WebView webView = (WebView) v.findViewById(R.id.webview_about);
        webView.loadData(aboutText, "text/html", null);

        return v;
    }
}
