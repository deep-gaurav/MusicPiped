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
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.danikula.videocache.HttpProxyCacheServer;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.MobileAds;
//import com.google.android.gms.ads.reward.RewardItem;
//import com.google.android.gms.ads.reward.RewardedVideoAd;
//import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.squareup.picasso.Picasso;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    Switch audiofocus;
    Switch updateCheck;
    Switch showAds;
    EditText cacheSize;

    //private RewardedVideoAd rewardedVideoAd;

    @Override
    public void onCreate(Bundle savedInstanceState){

        //rewardedVideoAd= MobileAds.getRewardedVideoAdInstance(this);


        super.onCreate(savedInstanceState);
        setTheme(this);
        setContentView(R.layout.settings_layout);

        audiofocus=findViewById(R.id.audioFocus);
        cacheSize=findViewById(R.id.cacheS);
        updateCheck = findViewById(R.id.updateCheck);
        showAds = findViewById(R.id.showAds);


        updateCheck.setChecked(getSharedPreferences("Settings",MODE_PRIVATE).getBoolean("CheckUpdate",true));
        if(getSharedPreferences("Settings",MODE_PRIVATE).getLong("ShowAds",0)<System.currentTimeMillis())
            showAds.setChecked(true);
        else
            showAds.setChecked(false);
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
        /*
        showAds.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                rewardedVideoAd.setRewardedVideoAdListener(new RewardedVideoAdListener() {
                    @Override
                    public void onRewardedVideoAdLoaded() {
                        rewardedVideoAd.show();
                    }

                    @Override
                    public void onRewardedVideoAdOpened() {

                    }

                    @Override
                    public void onRewardedVideoStarted() {
                    }

                    @Override
                    public void onRewardedVideoAdClosed() {
                    }

                    @Override
                    public void onRewarded(RewardItem rewardItem) {
                        SharedPreferences.Editor editor =getSharedPreferences("Settings",Context.MODE_PRIVATE).edit() ;
                        editor.remove("ShowAds");
                        editor.putLong("ShowAds",System.currentTimeMillis()+1000*60*60*24*2);
                        editor.commit();
                        Toast.makeText(SettingsActivity.this, "Ads wont show for next 2 days now", Toast.LENGTH_SHORT).show();
                        SettingsActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showAds.setChecked(false);
                            }
                        });

                    }

                    @Override
                    public void onRewardedVideoAdLeftApplication() {

                    }

                    @Override
                    public void onRewardedVideoAdFailedToLoad(int i) {
                        Log.i("ryd","ERROR in reward video "+i);
                        Toast.makeText(SettingsActivity.this, "Failed to load reward, try again in a few minutes", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onRewardedVideoCompleted() {

                    }
                });
                if(!isChecked){
                    if(getSharedPreferences("Settings",MODE_PRIVATE).getLong("ShowAds",0)<System.currentTimeMillis()) {
                        Toast.makeText(SettingsActivity.this, "Loading reward", Toast.LENGTH_LONG).show();
                        showAds.setChecked(true);
                        rewardedVideoAd.loadAd("ca-app-pub-3290942482576912/9991560079", new AdRequest.Builder().build());
                    }
                }
                else{
                    SharedPreferences.Editor editor =getSharedPreferences("Settings",Context.MODE_PRIVATE).edit() ;
                    editor.remove("ShowAds");
                    editor.putLong("ShowAds",0);
                    editor.commit();
                }
            }
        });
        */
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




        //THEME SETTER
        Button dark = findViewById(R.id.dark);
        Button blue = findViewById(R.id.blue);

        dark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor =getSharedPreferences("Settings",Context.MODE_PRIVATE).edit() ;
                editor.remove("THEME");
                editor.putString("THEME","dark");
                editor.commit();

                Toast.makeText(SettingsActivity.this, "Restart App to see changes", Toast.LENGTH_SHORT).show();
            }
        });
        blue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor =getSharedPreferences("Settings",Context.MODE_PRIVATE).edit() ;
                editor.remove("THEME");
                editor.putString("THEME","blue");
                editor.commit();

                Toast.makeText(SettingsActivity.this, "Restart App to see changes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void setTheme(Context context){
        String theme=context.getSharedPreferences("Settings",context.MODE_PRIVATE).getString("THEME","dark");
        if(theme.equals("dark"))
            context.setTheme(R.style.AppTheme);
        else if(theme.equals("blue"))
            context.setTheme(R.style.AppThemeDefaultBlue);
    }
}
