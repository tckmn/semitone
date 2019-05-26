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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override public void onCreatePreferences(Bundle state, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
    }

}
