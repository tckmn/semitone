/*
 * Semitone - tuner, metronome, and piano for Android
 * Copyright (C) 2019  Andy Tockman <andy@tck.mn>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package mn.tck.semitone;

import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MetronomeFragment extends SemitoneFragment {

    final int MIN_TEMPO = 40;
    final int MAX_TEMPO = 400;

    final int TAPS_MAX = 10;
    long[] taps;
    int ntaps;

    int tempo, beats, subdiv;
    boolean enabled;

    LinearLayout dotsView;
    ArrayList<Dot> dots;
    int activeDot;

    NumBox tempoBox, beatsBox, subdivBox;
    SeekBar tempoBar;
    Button startBtn, tapBtn;

    View view;
    ShapeDrawable dotOn, dotOnBig, dotOff, dotOffBig;
    LinearLayout.LayoutParams dotParams;

    Tick tick;
    int strong, weak;

    public MetronomeFragment() {
        MainActivity.mf = this;
    }

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
        dotsView = (LinearLayout) view.findViewById(R.id.dots);

        tempoBox.cb = new NumBox.Callback() {
            @Override public void onChange(int val) {
                tempo = val;
                tempoBar.setProgress(tempo - MIN_TEMPO);
                intermediateTempoChange();
            }
        };
        tempoBar.setProgress(tempo - MIN_TEMPO);

        beatsBox.cb = new NumBox.Callback() {
            @Override public void onChange(int val) {
                beats = val;
                intermediateBeatChange();
            }
        };

        subdivBox.cb = new NumBox.Callback() {
            @Override public void onChange(int val) {
                subdiv = val;
                intermediateTempoChange();
            }
        };

        tempoBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int val, boolean fromUser) {
                if (!fromUser) return;
                tempo = val + MIN_TEMPO;
                tempoBox.setValue(tempo);
                intermediateTempoChange();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        startBtn.setOnClickListener(new Button.OnClickListener() {
            @Override public void onClick(View v) { toggle(); }
        });

        taps = new long[TAPS_MAX];
        ntaps = 0;
        tapBtn.setOnClickListener(new Button.OnClickListener() {
            @Override public void onClick(View v) {
                long time = System.currentTimeMillis();
                if (ntaps > 0 && time - taps[ntaps-1] > 3000) {
                    // time out after 3 seconds
                    ntaps = 1;
                    taps[0] = time;
                } else if (ntaps == TAPS_MAX) {
                    for (int i = 0; i < TAPS_MAX-1; ++i) taps[i] = taps[i+1];
                    taps[TAPS_MAX-1] = time;
                } else {
                    taps[ntaps++] = time;
                }

                if (ntaps > 1) {
                    tempo = (int)(60000*(ntaps-1) / (taps[ntaps-1] - taps[0]));
                    tempoBox.setValue(tempo);
                    tempoBar.setProgress(tempo - MIN_TEMPO);
                    intermediateTempoChange();
                }
            }
        });

        final int smallSize = getResources().getDimensionPixelSize(R.dimen.small_dot),
              largeSize = getResources().getDimensionPixelSize(R.dimen.large_dot);
        dotOn     = makeDot(smallSize, R.color.white);
        dotOnBig  = makeDot(largeSize, R.color.white);
        dotOff    = makeDot(smallSize, R.color.grey1);
        dotOffBig = makeDot(largeSize, R.color.grey1);

        dotParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);

        dots = new ArrayList<Dot>();
        intermediateBeatChange();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (tick != null) {
            tick.keepGoing = false;
            tick.interrupt();
        }
    }

    @Override public void onSettingsChanged() {}

    private ShapeDrawable makeDot(int size, int color) {
        ShapeDrawable dot = new ShapeDrawable(new OvalShape());
        dot.setIntrinsicWidth(size);
        dot.setIntrinsicHeight(size);
        dot.getPaint().setColor(ContextCompat.getColor(getContext(), color));
        return dot;
    }

    private void toggle() {
        enabled = !enabled;
        if (enabled) {
            startBtn.setText(getString(R.string.stop_btn));
            activeDot = -1;
            tick = new Tick(tempo, subdiv);
            tick.start();
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            startBtn.setText(getString(R.string.start_btn));
            if (tick != null) {
                tick.keepGoing = false;
                tick.interrupt();
            }
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void intermediateTempoChange() {
        if (!enabled) return;
        long elapsedTime = System.currentTimeMillis() - tick.tickTime(tick.nTicks - 1);

        tick.tempo = tempo;
        tick.subdiv = subdiv;
        if (elapsedTime >= tick.delayTime()) {
            // immediate tick
            tick.nTicks = 0;
            tick.startTime = System.currentTimeMillis();
            tick.nextTime = tick.startTime;
        } else {
            // count the time since the last tick towards the next one
            tick.nTicks = 1;
            tick.startTime = System.currentTimeMillis() - elapsedTime;
            tick.nextTime = tick.tickTime(1);
        }

        // break out of any sleeps currently happening
        tick.interrupt();
    }

    private void intermediateBeatChange() {
        for (Dot dot : dots) {
            dotsView.removeView(dot);
        }
        dots.clear();

        for (int i = 0; i < beats; ++i) {
            Dot dot = new Dot(getContext(), i == 0);
            dotsView.addView(dot);
            dots.add(dot);
        }

        if (enabled && activeDot > beats-1) activeDot = beats-1;
    }

    class Tick extends Thread {
        protected int tempo, subdiv, nTicks;
        protected long startTime, nextTime;
        boolean keepGoing;
        public Tick(int tempo, int subdiv) {
            this.tempo = tempo;
            this.subdiv = subdiv;
            keepGoing = true;
        }

        @Override public void run() {
            nTicks = 0;
            startTime = System.currentTimeMillis();
            nextTime = startTime;
            while (keepGoing) {
                long diff = nextTime - System.currentTimeMillis();
                if (diff <= 0) {}
                // else if (diff <= 5) {
                //     // 5ms - arbitrary cutoff for when to busyloop
                //     while (System.currentTimeMillis() < nextTime);
                // }
                else {
                    // we have a while - sleep and check again
                    try { Thread.sleep(diff); } catch (InterruptedException e) {}
                    continue;
                }

                if (PianoEngine.paused) {
                    // the app was backgrounded and the setting to keep ticking
                    // is off, so stop the metronome
                    if (getActivity() != null) getActivity().runOnUiThread(new Runnable() {
                        @Override public void run() {
                            toggle();
                        }
                    });
                    break;
                }

                // time for another tick
                if (nTicks % subdiv == 0) activeDot = (activeDot + 1) % beats;
                PianoEngine.playFile(nTicks % subdiv == 0 && dots.get(activeDot).big ?
                        "strong.mp3" : "weak.mp3", 440);
                if (getActivity() != null) getActivity().runOnUiThread(new Runnable() {
                    @Override public void run() {
                        dots.get(activeDot).turnOn();
                    }
                });
                try { Thread.sleep(Math.min(100, (long)(delayTime()/2))); } catch (InterruptedException e) {}
                if (getActivity() != null) getActivity().runOnUiThread(new Runnable() {
                    @Override public void run() {
                        dots.get(activeDot).turnOff();
                    }
                });

                // queue the next tick
                nextTime = tickTime(++nTicks);
            }
        }

        protected long tickTime(int nTick) {
            return startTime + Math.round(nTick * delayTime());
        }

        protected double delayTime() { return 1000 * 60.0 / (tempo*subdiv); }
    }

    class Dot extends ImageView {
        boolean big;
        public Dot(Context context, boolean big) {
            super(context);
            this.big = big;
            turnOff();
            setLayoutParams(dotParams);
        }

        @Override public boolean onTouchEvent(MotionEvent ev) {
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
                big = !big;
                turnOff();
                return true;
            }
            return false;
        }

        public void turnOff() { setImageDrawable(big ? dotOffBig : dotOff); }
        public void turnOn()  { setImageDrawable(big ? dotOnBig  : dotOn);  }
    }

}
