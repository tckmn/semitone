package mn.tck.semitone;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;

public class MetronomeFragment extends Fragment {

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.metronome, container, false);
    }

}
