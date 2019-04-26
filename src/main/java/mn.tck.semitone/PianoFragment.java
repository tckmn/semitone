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

    public void onSettingsChanged() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        try {
            piano.concert_a = Integer.parseInt(sp.getString("concert_a", "440"));
        } catch (NumberFormatException e) {
            piano.concert_a = 440;
        }
        piano.sustain = sp.getBoolean("sustain", false);
    }

}
