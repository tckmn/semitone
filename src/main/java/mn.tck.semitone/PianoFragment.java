package mn.tck.semitone;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceManager;

public class PianoFragment extends Fragment {

    NumBox rowsBox, keysBox, pitchBox;
    PianoView piano;

    View view;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.piano, container, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        this.view = view;

        PianoEngine.create(getContext());

        rowsBox = (NumBox) view.findViewById(R.id.rows);
        keysBox = (NumBox) view.findViewById(R.id.keys);
        pitchBox = (NumBox) view.findViewById(R.id.pitch);
        piano = (PianoView) view.findViewById(R.id.piano);

        rowsBox.cb = new NumBox.Callback() {
            @Override public void onChange(int val) {
                piano.rows = val;
                piano.updateParams(true);
            }
        };

        keysBox.cb = new NumBox.Callback() {
            @Override public void onChange(int val) {
                piano.keys = val;
                piano.updateParams(true);
            }
        };

        pitchBox.cb = new NumBox.Callback() {
            @Override public void onChange(int val) {
                piano.pitch = val;
                piano.updateParams(true);
            }
        };

        onSettingsChanged();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        PianoEngine.destroy();
    }

    public void onSettingsChanged() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        try {
            piano.concert_a = Integer.parseInt(sp.getString("concert_a", "440"));
        } catch (NumberFormatException e) {
            piano.concert_a = 440;
        }
    }

}
