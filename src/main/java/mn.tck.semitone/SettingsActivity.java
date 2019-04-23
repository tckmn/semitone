package mn.tck.semitone;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.support.v7.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_settings);

        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.settingsframe, new SettingsFragment())
            .commit();
    }

    @Override public void onBackPressed() {
        Intent data = new Intent();
        setResult(Activity.RESULT_OK, data);
        finish();
    }

    @Override public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

}
