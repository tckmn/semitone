package mn.tck.semitone;

import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;

public class MetronomeFragment extends Fragment {

    final int MIN_TEMPO = 40;
    final int MAX_TEMPO = 400;

    int tempo, beats, subdiv;

    View view;
    ShapeDrawable dotOn, dotOff;
    LinearLayout.LayoutParams dotParams;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.metronome, container, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        this.view = view;

        tempo = 120;
        beats = 4;
        subdiv = 1;

        // TODO don't hardcode 100 here (and 200px in the layout)
        dotOn = new ShapeDrawable(new OvalShape());
        dotOn.setIntrinsicWidth(100);
        dotOn.setIntrinsicHeight(100);
        dotOn.getPaint().setColor(ContextCompat.getColor(getContext(), R.color.white));
        dotOff = new ShapeDrawable(new OvalShape());
        dotOff.setIntrinsicWidth(100);
        dotOff.setIntrinsicHeight(100);
        dotOff.getPaint().setColor(ContextCompat.getColor(getContext(), R.color.grey1));

        dotParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);

        for (int i = 0; i < beats; ++i) {
            ImageView img = new ImageView(getContext());
            img.setImageDrawable(dotOff);
            img.setLayoutParams(dotParams);
            ((LinearLayout)view.findViewById(R.id.dots)).addView(img);
        }
    }

}
