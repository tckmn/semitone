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

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.util.TypedValue;

import androidx.preference.PreferenceManager;

import java.util.Arrays;

public class TunerFragment extends SemitoneFragment implements RecordEngine.Callback {

    final static int REQUEST_MIC = 123;

    final static int HIST_SIZE = 16;

    View view;
    TextView notename;
    int notenamesize;
    CentErrorView centerror;

    int concert_a;

    double[] dbuf, hist, sorted;

    public TunerFragment() {
        super();
        MainActivity.tf = this;
        RecordEngine.cb = this;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.tuner, container, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        this.view = view;

        if (RecordEngine.created) dbuf = new double[DSP.fftlen];
        hist = new double[HIST_SIZE];
        sorted = new double[HIST_SIZE];

        notename = (TextView) view.findViewById(R.id.notename);
        centerror = (CentErrorView) view.findViewById(R.id.centerror);

        notename.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                notename.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                notenamesize = Util.maxTextSize("G#000", notename.getWidth());
                if (RecordEngine.created) {
                    notename.setTextSize(TypedValue.COMPLEX_UNIT_PX, notenamesize);
                } else {
                    notename.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                    notename.setText(getResources().getString(R.string.micperm));
                    notename.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            if (RecordEngine.created) return;
                            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
                        }
                    });
                }
            }
        });

        onSettingsChanged();
    }

    @Override public void onSettingsChanged() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        try {
            concert_a = Integer.parseInt(sp.getString("concert_a", "440"));
        } catch (NumberFormatException e) {
            concert_a = 440;
        }
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        switch (code) {
        case REQUEST_MIC:
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) {
                notename.setText("");
                notename.setTextSize(TypedValue.COMPLEX_UNIT_PX, notenamesize);
                RecordEngine.create(getActivity());
                dbuf = new double[DSP.fftlen];
            }
            break;
        }
    }

    @Override public void onRecordUpdate(short[] buf) {
        // this can happen after the fragment has been instantiated but before
        // onViewCreated has had a chance to run
        if (dbuf == null) return;

        // copy data to fft buffer - scale down to avoid huge numbers
        for (int i = 0; i < DSP.fftlen; ++i) dbuf[i] = buf[i] / 1024.0;

        // calculate frequency and note
        double freq = DSP.freq(dbuf, RecordEngine.SAMPLE_RATE),
                semitone = 12 * Math.log(freq/concert_a)/Math.log(2);

        // insert into moving average history
        for (int i = 1; i < HIST_SIZE; ++i) sorted[i-1] = hist[i-1] = hist[i];
        sorted[HIST_SIZE-1] = hist[HIST_SIZE-1] = semitone;

        // find median
        Arrays.sort(sorted);
        final double median = (sorted[HIST_SIZE/2-1]+sorted[HIST_SIZE/2])/2;

        final int rounded = (int)Math.round(median);
        final boolean shift = rounded < 0 && rounded % 12 != 0;
        final int note   = rounded % 12 + (shift ? 12 : 0);
        final int octave = rounded / 12 - (shift ? 1  : 0);

        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override public void run() {
                    notename.setText(Util.notenames[note] +
                        (octave + 5 - (note <= 2 ? 1 : 0)));
                    centerror.setError(median - rounded);
                }
            });
        }
    }

}
