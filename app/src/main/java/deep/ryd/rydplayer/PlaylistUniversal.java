package deep.ryd.rydplayer;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.Image;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.services.youtube.YoutubeService;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeChannelExtractor;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubePlaylistExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.abs;

public class PlaylistUniversal extends AppCompatActivity {

    RecyclerView recyclerView;
    UniversalPlaylistAdapter adapter;
    RecyclerView.LayoutManager layoutManager;
    List<StreamInfo> streamInfoList;
    int playlistid=0;
    String artisturl;
    Timer timer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlistlayout);
        recyclerView=findViewById(R.id.universalplaylistrecycler);
        layoutManager=new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        Intent i = getIntent();
        if (i.hasExtra("name")){
            CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapsingToolbar);
            collapsingToolbarLayout.setTitle(i.getStringExtra("name"));
        }
        if (i.getStringExtra("playlisttype").equals("playlist")){
            final DBManager  dbManager = new DBManager(this);
            playlistid=i.getIntExtra("playlistid",0);
            streamInfoList = dbManager.songinList(playlistid);
            adapter = new UniversalPlaylistAdapter(streamInfoList,R.layout.resultlist,true);
            recyclerView.setAdapter(adapter);

            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                    int index = viewHolder.getLayoutPosition();
                    dbManager.open();
                    dbManager.removeFromPlaylist(streamInfoList.get(index).getUrl(),playlistid);
                    dbManager.close();
                    streamInfoList = dbManager.songinList(playlistid);
                    adapter.infoItems=streamInfoList;
                    adapter.notifyDataSetChanged();
                }
            });

            itemTouchHelper.attachToRecyclerView(recyclerView);
            imagerefresher();
        }
        else if(i.getStringExtra("playlisttype").equals("artist")){
            String channelurl = i.getStringExtra("channelurl");
            DBManager dbManager = new DBManager(this);
            streamInfoList = dbManager.artistlists(channelurl);
            artisturl=channelurl;
            adapter = new UniversalPlaylistAdapter(streamInfoList,R.layout.resultlist,true);
            recyclerView.setAdapter(adapter);
            new DownloadArtistInfo().execute(channelurl);

        }
    }

    public void imagerefresher(){
        if(timer==null){
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {

                    Random random = new Random();
                    if(streamInfoList.size()>0) {
                        final int nextimg = abs(random.nextInt()) % streamInfoList.size();
                        if (nextimg < streamInfoList.size()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ImageView imageView = findViewById(R.id.toolbarImage);

                                    Picasso.get()
                                            .load(streamInfoList.get(nextimg).getThumbnailUrl())
                                            .into(imageView);
                                    imageView.setImageAlpha(100);
                                }
                            });
                        }
                    }
                }
            },0,3000);
        }
    }
    class DownloadArtistInfo extends AsyncTask<String,String,String>{

        String BannerURL;
        @Override
        protected String doInBackground(String... strings) {
            Downloader.init(null);
            NewPipe.init(Downloader.getInstance());
            int sid = NewPipe.getIdOfService("YouTube");
            YoutubeService ys= null;
            try {
                ys = (YoutubeService) NewPipe.getService(sid);
            } catch (ExtractionException e) {
                e.printStackTrace();
            }

            try {
                YoutubeChannelExtractor yce = (YoutubeChannelExtractor)ys.getChannelExtractor(strings[0]);

                yce.fetchPage();
                BannerURL=yce.getBannerUrl();
            } catch (ExtractionException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return BannerURL;

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            ImageView imageView = (ImageView)PlaylistUniversal.this.findViewById(R.id.toolbarImage);
            Picasso.get()
                    .load(BannerURL)
                    .into(imageView);
            imageView.setImageAlpha(150);
            //imageView.setAdjustViewBounds(true);

        }
    }
}
class UniversalPlaylistAdapter extends RecyclerView.Adapter<UniversalPlaylistAdapter.MyViewHolder>{

    public static boolean returner=false;
    List<StreamInfo> infoItems;
    boolean vertical;
    boolean artist_thumb=false;
    int itemlayoutid;
    //boolean circular=false;

    public UniversalPlaylistAdapter(List<StreamInfo> infoItems, int itemlayoutid,boolean vertical){
        this.infoItems = infoItems;
        this.itemlayoutid=itemlayoutid;
        this.vertical=vertical;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        CardView view= new CardView(viewGroup.getContext());
        ViewGroup.MarginLayoutParams params;
        if(vertical)
            params=new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        else
            params=new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);

        params.setMargins(10,10,10,10);
        view.setLayoutParams(params);
        view.setPadding(10,10,10,10);

        TypedValue outValue = new TypedValue();
        view.getContext().getTheme().resolveAttribute(R.attr.selectableItemBackground, outValue, true);
        //view.setBackgroundResource(outValue.resourceId);
        view.setForeground(view.getContext().getDrawable(outValue.resourceId));

        view.addView( LayoutInflater.from(viewGroup.getContext())
                .inflate(itemlayoutid,viewGroup,false));

        MyViewHolder vh=new MyViewHolder(view);


        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder myViewHolder, final int i) {

        myViewHolder.itemView.setLongClickable(true);
        myViewHolder.streamInfo=(StreamInfo)infoItems.get(i);
        final CardView cardView=myViewHolder.cardView;
        View constraintLayout= cardView.getChildAt(0);
        ImageView img=(ImageView)constraintLayout.findViewById(R.id.thumbHolder);
        TextView title=(TextView)constraintLayout.findViewById(R.id.queueContent);

        //CachedImageDownloader cachedImageDownloader;
        if(artist_thumb) {
            //cachedImageDownloader = new CachedImageDownloader(infoItems.get(i).getUploaderAvatarUrl(), img);
            title.setText(infoItems.get(i).getUploaderName());
            Picasso.get()
                    .load(infoItems.get(i).getUploaderAvatarUrl())
                    .transform(new CircleTransform())
                    .into(img);
            cardView.setCardElevation(0);
            cardView.setCardBackgroundColor(Color.TRANSPARENT);
            //img.setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,200));
        }
        else {
            //cachedImageDownloader = new CachedImageDownloader(infoItems.get(i).getThumbnailUrl(), img);
            title.setText(infoItems.get(i).getName());
            Picasso.get()
                    .load(infoItems.get(i).getThumbnailUrl())
                    .into(img);
        }
        //title.setText("TEST CARD "+new Integer(i).toString());
        //t.setText(new Integer(i).toString());
        //cachedImageDownloader.execute();

        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO on click of playlist song
                Intent intent = new Intent();
                if(((PlaylistUniversal)cardView.getContext()).playlistid > 0){
                    intent.putExtra("playlisttype", "playlist");
                    intent.putExtra("playListid", ((PlaylistUniversal) cardView.getContext()).playlistid);
                    intent.putExtra("songindex", i);
                }
                else {
                    intent.putExtra("playlisttype", "artist");
                    intent.putExtra("playListid", ((PlaylistUniversal) cardView.getContext()).playlistid);
                    intent.putExtra("channelurl", ((PlaylistUniversal) cardView.getContext()).artisturl);
                    intent.putExtra("songindex", i);
                }

                intent.setAction(MainActivity.MAINACTIVITYTBROADCASTACTION);
                cardView.getContext().sendBroadcast(intent);
                ((PlaylistUniversal)cardView.getContext()).finish();
            }
        });

    }

    @Override
    public int getItemCount() {
        return infoItems.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        public StreamInfo streamInfo;
        public CardView cardView;
        public  MyViewHolder(CardView cardView){
            super(cardView);
            this.cardView=cardView;

        }

    }
}

