package mn.tck.semitone;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;

public class PianoFragment extends Fragment {

    NumBox rowsBox, keysBox, pitchBox;
    PianoView piano;

    View view;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.piano, container, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        this.view = view;

        rowsBox = (NumBox) view.findViewById(R.id.rows);
        keysBox = (NumBox) view.findViewById(R.id.keys);
        pitchBox = (NumBox) view.findViewById(R.id.pitch);
        piano = (PianoView) view.findViewById(R.id.piano);

        rowsBox.cb = new NumBox.Callback() {
            @Override public void onChange(int val) {
                piano.rows = val;
                piano.invalidate();
            }
        };

        keysBox.cb = new NumBox.Callback() {
            @Override public void onChange(int val) {
                piano.keys = val;
                piano.invalidate();
            }
        };

        pitchBox.cb = new NumBox.Callback() {
            @Override public void onChange(int val) {
                piano.pitch = val;
                piano.invalidate();
            }
        };
    }

}
