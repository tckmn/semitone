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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;

public class MetronomeFragment extends Fragment {

    final int MIN_TEMPO = 40;
    final int MAX_TEMPO = 400;

    int tempo, beats, subdiv;
    boolean enabled;

    ArrayList<ImageView> dots;
    int activeDot;

    NumBox tempoBox, beatsBox, subdivBox;
    SeekBar tempoBar;
    Button startBtn, tapBtn;

    View view;
    ShapeDrawable dotOn, dotOff;
    LinearLayout.LayoutParams dotParams;

    Thread tick;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.metronome, container, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        this.view = view;

        tempo = 120; beats = 4; subdiv = 1; enabled = false;
        tempoBox = (NumBox) view.findViewById(R.id.tempo);
        beatsBox = (NumBox) view.findViewById(R.id.beats);
        subdivBox = (NumBox) view.findViewById(R.id.subdiv);
        tempoBar = (SeekBar) view.findViewById(R.id.tempobar);
        startBtn = (Button) view.findViewById(R.id.start);
        tapBtn = (Button) view.findViewById(R.id.tap);

        tempoBox.cb = new NumBox.Callback() {
            @Override public void onChange(int val) {
                tempo = val;
                tempoBar.setProgress(tempo - MIN_TEMPO);
                if (enabled) toggle();
            }
        };
        tempoBar.setProgress(tempo - MIN_TEMPO);

        beatsBox.cb = new NumBox.Callback() {
            @Override public void onChange(int val) {
                beats = val;
                if (enabled) toggle();
            }
        };

        subdivBox.cb = new NumBox.Callback() {
            @Override public void onChange(int val) {
                subdiv = val;
                if (enabled) toggle();
            }
        };

        tempoBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int val, boolean fromUser) {
                tempo = val + MIN_TEMPO;
                tempoBox.setValue(tempo);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        startBtn.setOnClickListener(new Button.OnClickListener() {
            @Override public void onClick(View v) { toggle(); }
        });

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

        dots = new ArrayList<ImageView>();
        for (int i = 0; i < beats; ++i) {
            ImageView img = new ImageView(getContext());
            img.setImageDrawable(dotOff);
            img.setLayoutParams(dotParams);
            ((LinearLayout)view.findViewById(R.id.dots)).addView(img);
            dots.add(img);
        }
    }

    private void toggle() {
        enabled = !enabled;
        if (enabled) {
            startBtn.setText("Stop");
            activeDot = -1;
            tick = new Thread(new Tick(tempo));
            tick.start();
        } else {
            startBtn.setText("Start");
            if (tick != null) tick.interrupt();
            removeDot();
        }
    }

    protected void removeDot() {
        if (activeDot >= 0 && activeDot < beats) {
            dots.get(activeDot).setImageDrawable(dotOff);
        }
    }

    class Tick implements Runnable {
        int tempo, nTicks;
        long startTime, nextTime;
        public Tick(int tempo) {
            this.tempo = tempo;
        }
        @Override public void run() {
            nTicks = 0;
            startTime = System.currentTimeMillis();
            nextTime = startTime;
            while (!Thread.interrupted()) {
                long diff = nextTime - System.currentTimeMillis();
                if (diff <= 0) {}
                // else if (diff <= 5) {
                //     // 5ms - arbitrary cutoff for when to busyloop
                //     while (System.currentTimeMillis() < nextTime);
                // }
                else {
                    // we have a while - sleep and check again
                    try { Thread.sleep(diff); }
                    catch (InterruptedException e) { return; }
                    continue;
                }

                // time for another tick
                getActivity().runOnUiThread(new Runnable() {
                    @Override public void run() {
                        removeDot();
                        activeDot = (activeDot + 1) % beats;
                        dots.get(activeDot).setImageDrawable(dotOn);
                        // TODO play sound
                    }
                });

                // queue the next tick
                nextTime = startTime + Math.round((++nTicks) * 1000 * (60.0 / tempo));
            }
        }
    }

}
