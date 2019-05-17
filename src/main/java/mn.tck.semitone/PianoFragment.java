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
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.preference.PreferenceManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class PianoFragment extends SemitoneFragment {

    static final String PREF_SUSTAIN = "sustain";
    static final String PREF_LABELNOTES = "labelnotes";
    static final String PREF_LABELNOTESLIGHT = "labelnoteslight";
    static final String PREF_CONCERT_A = "concert_a";

    static final boolean PREF_SUSTAIN_DEFAULT = false;
    static final boolean PREF_LABELNOTESLIGHT_DEFAULT = false;
    static final boolean PREF_LABELNOTES_DEFAULT = false;
    static final String PREF_CONCERT_A_DEFAULT = "440";

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


        // Setup the root note spinner
        final Spinner rootNoteSpinner = view.findViewById(R.id.root_note_spinner);
        final ArrayAdapter<CharSequence> rootNoteAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.noteNames, android.R.layout.simple_spinner_item);
        rootNoteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rootNoteSpinner.setAdapter(rootNoteAdapter);
        rootNoteSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                piano.setScale(piano.scale, i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // NOP
            }
        });

        // Setup the scale spinner
        final Spinner scaleSpinner = view.findViewById(R.id.scale_spinner);
        ArrayAdapter<CharSequence> scaleAdapter = ArrayAdapter.createFromResource(getContext(),
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? R.array.scaleNamesShort : R.array.scaleNames,
                android.R.layout.simple_spinner_item);
        scaleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scaleSpinner.setAdapter(scaleAdapter);
        scaleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                piano.setScale(i, piano.rootNote);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // NOP
            }
        });


        view.findViewById(R.id.reset_view).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                scaleSpinner.setSelection(PianoView.PREF_SCALE_DEFAULT);
                rootNoteSpinner.setSelection(PianoView.PREF_SCALE_ROOT_DEFAULT);
                piano.setDefaults();
            }
        });
        onSettingsChanged();
    }

    @Override public void onSettingsChanged() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        try {
            piano.concert_a = Integer.parseInt(sp.getString(PREF_CONCERT_A, PREF_CONCERT_A_DEFAULT));
        } catch (NumberFormatException e) {
            piano.concert_a = 440;
        }
        piano.sustain = sp.getBoolean(PREF_SUSTAIN, PREF_SUSTAIN_DEFAULT);
        piano.labelnotes = sp.getBoolean(PREF_LABELNOTES, PREF_LABELNOTES_DEFAULT);
        piano.labelnoteslight = sp.getBoolean(PREF_LABELNOTESLIGHT, PREF_LABELNOTESLIGHT_DEFAULT);
    }

}
