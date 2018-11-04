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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import com.danikula.videocache.HttpProxyCacheServer;
import com.squareup.picasso.Picasso;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    Switch audiofocus;
    Switch updateCheck;
    Switch showAds;
    EditText cacheSize;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        audiofocus=findViewById(R.id.audioFocus);
        cacheSize=findViewById(R.id.cacheS);
        updateCheck = findViewById(R.id.updateCheck);
        showAds = findViewById(R.id.showAds);


        updateCheck.setChecked(getSharedPreferences("Settings",MODE_PRIVATE).getBoolean("CheckUpdate",true));
        showAds.setChecked(getSharedPreferences("Settings",MODE_PRIVATE).getBoolean("ShowAds",true));
        cacheSize.setText(String.valueOf(getSharedPreferences("Settings",Context.MODE_PRIVATE).getInt("cacheSize",100)));
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
        showAds.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor =getSharedPreferences("Settings",Context.MODE_PRIVATE).edit() ;
                editor.remove("ShowAds");
                editor.putBoolean("ShowAds",isChecked);
                editor.commit();
            }
        });
        updateCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor =getSharedPreferences("Settings",Context.MODE_PRIVATE).edit() ;
                editor.remove("CheckUpdate");
                editor.putBoolean("CheckUpdate",isChecked);
                editor.commit();
            }
        });

        cacheSize.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    String s1 = s.toString();
                    int cach = Integer.parseInt(s1);
                    if(cach>=0) {
                        SharedPreferences sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.remove("cacheSize");
                        editor.putInt("cacheSize", cach);
                        editor.commit();
                    }
                }
                catch (Exception e){

                }
            }
        });

    }

}
