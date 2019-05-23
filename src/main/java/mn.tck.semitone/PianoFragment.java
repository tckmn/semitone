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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.preference.PreferenceManager;

public class PianoFragment extends SemitoneFragment {

    PianoView piano;
    View view;

    public PianoFragment() {
        MainActivity.pf = this;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.piano, container, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        this.view = view;

        piano = (PianoView) view.findViewById(R.id.piano);

        view.findViewById(R.id.add_row).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (piano.rows < 5) ++piano.rows;
                piano.updateParams(true);
            }
        });
        view.findViewById(R.id.remove_row).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (piano.rows > 1) --piano.rows;
                piano.updateParams(true);
            }
        });
        view.findViewById(R.id.add_col).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (piano.keys < 21) ++piano.keys;
                piano.updateParams(true);
            }
        });
        view.findViewById(R.id.remove_col).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (piano.keys > 7) --piano.keys;
                piano.updateParams(true);
            }
        });
        view.findViewById(R.id.left_octave).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                piano.pitch -= 7;
                if (piano.pitch < 7) piano.pitch = 7;
                piano.updateParams(true);
            }
        });
        view.findViewById(R.id.left_key).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (piano.pitch > 7) --piano.pitch;
                piano.updateParams(true);
            }
        });
        view.findViewById(R.id.right_key).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (piano.pitch < 49) ++piano.pitch;
                piano.updateParams(true);
            }
        });
        view.findViewById(R.id.right_octave).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                piano.pitch += 7;
                if (piano.pitch > 49) piano.pitch = 49;
                piano.updateParams(true);
            }
        });
        view.findViewById(R.id.reset_view).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                piano.rows = 2;
                piano.keys = 7;
                piano.pitch = 28;
                piano.updateParams(true);
            }
        });

        onSettingsChanged();
    }

    @Override public void onSettingsChanged() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        try {
            piano.concert_a = Integer.parseInt(sp.getString("concert_a", "440"));
        } catch (NumberFormatException e) {
            piano.concert_a = 440;
        }
        piano.sustain = sp.getBoolean("sustain", false);
        piano.labelnotes = sp.getBoolean("labelnotes", true);
        piano.labelc = sp.getBoolean("labelc", true);
    }

}
