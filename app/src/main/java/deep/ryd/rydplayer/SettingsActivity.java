package deep.ryd.rydplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import com.danikula.videocache.HttpProxyCacheServer;
import com.squareup.picasso.Picasso;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    Switch audiofocus;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        audiofocus=findViewById(R.id.audioFocus);


        audiofocus.setChecked(getSharedPreferences("Settings",Context.MODE_PRIVATE).getBoolean("respectAudioFocus",true));
        audiofocus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor =getSharedPreferences("Settings",Context.MODE_PRIVATE).edit() ;
                editor.remove("respectAudioFocus");
                editor.putBoolean("respectAudioFocus",isChecked);
                editor.commit();
            }
        });

    }

}
