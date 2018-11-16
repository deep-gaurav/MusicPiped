package deep.ryd.rydplayer;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.bullhead.equalizer.EqualizerFragment;

public class Equaliser extends AppCompatActivity {

    public static String MediaPlayerIDKey = "MediaPlayerSessionId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equaliser);
        int sessionId = getIntent().getIntExtra(MediaPlayerIDKey,0);
        EqualizerFragment equalizerFragment = EqualizerFragment.newBuilder()
                .setAccentColor(Color.parseColor("#4caf50"))
                .setAudioSessionId(sessionId)
                .build();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.equaliserFrame, equalizerFragment)
                .commit();
    }
}
