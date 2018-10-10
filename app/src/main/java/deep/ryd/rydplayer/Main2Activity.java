package deep.ryd.rydplayer;


import android.app.Activity;
import android.app.ActionBar;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Main2Activity extends MainActivity implements android.support.v7.app.ActionBar.TabListener {


    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main2);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // Set up the action bar.
        final android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));

        }


        ready();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.new_main, menu);

        final MenuItem searchItem = menu.findItem(R.id.action_search);

        final SearchView searchView;
        if (searchItem != null) {
            searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            searchView.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    //some operation
                    return true;
                }
            });
            searchView.setOnSearchClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //some operation
                }
            });
            EditText searchPlate = (EditText) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
            searchPlate.setHint("Search");
            View searchPlateView = searchView.findViewById(android.support.v7.appcompat.R.id.search_plate);
            searchPlateView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
            // use this method for search process
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    // use this method when query submitted
                    Toast.makeText(self, query, Toast.LENGTH_SHORT).show();
                    searchItem.collapseActionView();
                    Intent intent=new Intent(self,SearchActivity.class);
                    intent.putExtra("SearchText",query);
                    startActivityForResult(intent,MYCHILD);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    // use this method for auto complete search process
                    return false;
                }
            });
            SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        }
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onTabSelected(android.support.v7.app.ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());

    }

    @Override
    public void onTabUnselected(android.support.v7.app.ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabReselected(android.support.v7.app.ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static SectionsPagerAdapter pagerAdapter;
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber,SectionsPagerAdapter adapter) {
            pagerAdapter=adapter;
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);

            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main2, container, false);

            if (getArguments().getInt(ARG_SECTION_NUMBER)==1){
                rootView = inflater.inflate(R.layout.fragment_home,container,false);
                {
                    RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.tracksrecycler);
                    recyclerView.setHasFixedSize(true);
                    RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(rootView.getContext(), LinearLayoutManager.HORIZONTAL, false);

                    recyclerView.setLayoutManager(mLayoutManager);


                    List<StreamInfo> songs = loadInfofromDB(true,false);


                    SongsListAdaptor mAdapter = new SongsListAdaptor(songs, (Activity) rootView.getContext(), R.layout.top_track_fragments, false);
                    recyclerView.setAdapter(mAdapter);
                }
                {
                    RecyclerView recyclerView=(RecyclerView)rootView.findViewById(R.id.artistRecycler);
                    recyclerView.setHasFixedSize(true);
                    RecyclerView.LayoutManager mLayoutManager=new LinearLayoutManager(rootView.getContext(),LinearLayoutManager.HORIZONTAL,false);
                    recyclerView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
                    recyclerView.setLayoutManager(mLayoutManager);


                    List<StreamInfo> songs = loadInfofromDB(true,true);


                    SongsListAdaptor mAdapter=new SongsListAdaptor(songs,(Activity)rootView.getContext(),R.layout.top_track_fragments,false);
                    mAdapter.artist_thumb=true;
                    recyclerView.setAdapter(mAdapter);
                }
            }


            if(getArguments().getInt(ARG_SECTION_NUMBER)==2){
                rootView = inflater.inflate(R.layout.fragment_main2, container, false);

                RecyclerView recyclerView=(RecyclerView)rootView.findViewById(R.id.listRecycler);
                recyclerView.setHasFixedSize(true);
                RecyclerView.LayoutManager mLayoutManager=new LinearLayoutManager(rootView.getContext());
                recyclerView.setLayoutManager(mLayoutManager);

                recyclerView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);

                List<StreamInfo> songs = loadInfofromDB(false,false);


                SongsListAdaptor mAdapter=new SongsListAdaptor(songs,(Activity)rootView.getContext(),R.layout.resultlist,true);
                recyclerView.setAdapter(mAdapter);
            }

            return rootView;
        }
    }

    public static List<StreamInfo> loadInfofromDB(boolean sorted,boolean artist){
        List<StreamInfo> songs=new ArrayList<>();
        dbManager.open();
        Cursor cursor;
        if (artist)
            cursor= dbManager.fetch_top_artist();
        else if (sorted)
            cursor = dbManager.fetch_sorted();
        else
            cursor=dbManager.fetch();

        int sid=NewPipe.getIdOfService("YouTube");
        for(int i=0;i<cursor.getCount();i++){
            StreamInfo streamInfo =new StreamInfo(
                    sid,
                    cursor.getString(cursor.getColumnIndex(DatabasHelper.URL)),
                    cursor.getString(cursor.getColumnIndex(DatabasHelper.URL)),
                    StreamType.AUDIO_STREAM,
                    "",
                    cursor.getString(cursor.getColumnIndex(DatabasHelper.TITLE)),
                    0
            );
            streamInfo.setThumbnailUrl(cursor.getString(cursor.getColumnIndex(DatabasHelper.THUMBNAIL_URL)));
            streamInfo.setUploaderName(cursor.getString(cursor.getColumnIndex(DatabasHelper.ARTIST)));
            streamInfo.setUploaderUrl(cursor.getString(cursor.getColumnIndex(DatabasHelper.ARTIST_URL)));
            streamInfo.setUploaderAvatarUrl(cursor.getString(cursor.getColumnIndex(DatabasHelper.ARTIST_THUMBNAIL_URL)));
            List<AudioStream> audioStreams = new ArrayList<>();
            audioStreams.add(core.StringtoAudioStream(cursor.getString(cursor.getColumnIndex(DatabasHelper.STREAM_URL_1))));
            Log.i("ryd","AUDIO STREAM LOADED "+audioStreams.get(0).getUrl());
            streamInfo.setAudioStreams(audioStreams);

            songs.add(streamInfo);
            cursor.moveToNext();
        }
        dbManager.close();
        return songs;
    }
    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1,this);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Home";
                case 1:
                    return "Tracks";
                case 2:
                    return "Artists";
                case 3:
                    return "PlayLists";
            }
            return null;
        }
    }
}


class SongsListAdaptor extends RecyclerView.Adapter<SongsListAdaptor.MyViewHolder>{

    public static boolean returner=false;
    List<StreamInfo> infoItems;
    Activity activity;
    Cursor cursor;
    Main2Activity.SectionsPagerAdapter notifyadaptor;
    int fragmentID;
    boolean vertical;
    boolean artist_thumb=false;

    public SongsListAdaptor(List<StreamInfo> infoItems,Activity activity, int fragmentID,boolean vertical){
        this.infoItems = infoItems;
        this.activity=activity;
        this.fragmentID=fragmentID;
        this.vertical=vertical;
        //downloaditeminfos();
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

        view.addView((ConstraintLayout) LayoutInflater.from(viewGroup.getContext())
                .inflate(fragmentID,viewGroup,false));

        MyViewHolder vh=new MyViewHolder(view);


        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, final int i) {

        myViewHolder.streamInfo=(StreamInfo)infoItems.get(i);
        CardView cardView=myViewHolder.cardView;
        ConstraintLayout constraintLayout=(ConstraintLayout)cardView.getChildAt(0);
        ImageView img=(ImageView)constraintLayout.findViewById(R.id.thumbHolder);
        TextView title=(TextView)constraintLayout.findViewById(R.id.queueContent);

        //CachedImageDownloader cachedImageDownloader;
        if(artist_thumb) {
            //cachedImageDownloader = new CachedImageDownloader(infoItems.get(i).getUploaderAvatarUrl(), img);
            title.setText(infoItems.get(i).getUploaderName());
            Picasso.get()
                    .load(infoItems.get(i).getUploaderAvatarUrl())
                    .into(img);
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
                MainActivity ma=(MainActivity)activity;
                ma.playStream(infoItems,i);
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

class ListThumbDownloaderold extends AsyncTask<SongsListAdaptor.MyViewHolder,Integer,Integer>{

    boolean artist_thumb=false;
    @Override
    protected Integer doInBackground(SongsListAdaptor.MyViewHolder... viewHolders) {
        try {
            URL urlConnection;
            if(artist_thumb)
                urlConnection = new URL(viewHolders[0].streamInfo.getUploaderAvatarUrl());
            else
                urlConnection = new URL(viewHolders[0].streamInfo.getThumbnailUrl());
            Log.i("rypd","Thumbnail URL downloading "+urlConnection.toString());
            HttpURLConnection connection = (HttpURLConnection) urlConnection
                    .openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            final Bitmap myBitmap = BitmapFactory.decodeStream(input);
            ConstraintLayout constraintLayout = (ConstraintLayout)viewHolders[0].cardView.getChildAt(0);
            final ImageView img=(ImageView)constraintLayout.findViewById(R.id.thumbHolder);
            ((Activity)img.getContext()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    img.setImageBitmap(myBitmap);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}