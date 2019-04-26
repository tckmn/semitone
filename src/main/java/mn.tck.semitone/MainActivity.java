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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Window;
import android.view.View;
import android.widget.ImageView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.preference.PreferenceManager;

public class MainActivity extends FragmentActivity {

    ImageView fullscreen, settings;

    static TunerFragment tf;
    static MetronomeFragment mf;
    static PianoFragment pf;
    static String tt, mt, pt;

    static final int SETTINGS_INTENT_CODE = 123;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor e = sp.edit();
        if (!sp.contains("concert_a")) e.putString("concert_a", "440");
        if (!sp.contains("sustain")) e.putBoolean("sustain", false);
        e.commit();

        tt = getResources().getString(R.string.tuner_title);
        mt = getResources().getString(R.string.metronome_title);
        pt = getResources().getString(R.string.piano_title);

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        TabLayout tabs = (TabLayout) findViewById(R.id.tabs);
        SemitoneAdapter adapter = new SemitoneAdapter(getSupportFragmentManager());

        pager.setAdapter(adapter);
        tabs.setupWithViewPager(pager);

        fullscreen = (ImageView) findViewById(R.id.fullscreen);
        settings = (ImageView) findViewById(R.id.settings);

        settings.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivityForResult(intent, SETTINGS_INTENT_CODE);
            }
        });
    }

    @Override public void onActivityResult(int code, int res, Intent data) {
        super.onActivityResult(code, res, data);
        switch (code) {
        case SETTINGS_INTENT_CODE:
            if (tf != null) tf.onSettingsChanged();
            if (mf != null) mf.onSettingsChanged();
            if (pf != null) pf.onSettingsChanged();
            break;
        }
    }

    private static class SemitoneAdapter extends FragmentPagerAdapter {
        public SemitoneAdapter(FragmentManager fm) { super(fm); }
        @Override public int getCount() { return 3; }
        @Override public Fragment getItem(int pos) {
            switch (pos) {
            case 0: tf = new TunerFragment();     return tf;
            case 1: mf = new MetronomeFragment(); return mf;
            case 2: pf = new PianoFragment();     return pf;
            default: return null;
            }
        }
        @Override public CharSequence getPageTitle(int pos) {
            switch (pos) {
            case 0: return tt;
            case 1: return mt;
            case 2: return pt;
            default: return null;
            }
        }
    }

}
