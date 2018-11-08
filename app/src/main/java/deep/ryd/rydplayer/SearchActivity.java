package deep.ryd.rydplayer;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.squareup.picasso.Picasso;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.services.youtube.YoutubeService;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeSearchExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import android.app.SearchManager;

public class SearchActivity extends AppCompatActivity {

    Activity self;
    ContextMenuRecyclerView resultRecycler;
    RecyclerView.Adapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;
    ActionBar actionBar;
    List<InfoItem> searchItems = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        self=this;

        resultRecycler = findViewById(R.id.resultRecycler);

        resultRecycler.setHasFixedSize(true);

        mLayoutManager=new LinearLayoutManager(this);
        resultRecycler.setLayoutManager(mLayoutManager);

        mAdapter=new MyAdapter(searchItems,this,true);

        resultRecycler.setAdapter(mAdapter);

        actionBar = getSupportActionBar();

        registerForContextMenu(resultRecycler);
        Intent intent = getIntent();
        if(intent.hasExtra(SearchManager.QUERY)){
            String st=intent.getStringExtra(SearchManager.QUERY);

            Search(st);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu,View v,ContextMenu.ContextMenuInfo menuInfo){
        super.onCreateContextMenu(menu,v,menuInfo);

        MenuInflater menuInflater = this.getMenuInflater();
        menuInflater.inflate(R.menu.songmenu,menu);
    }
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId()==R.id.addtocurrentqueue) {
            ContextMenuRecyclerView.RecyclerContextMenuInfo info = (ContextMenuRecyclerView.RecyclerContextMenuInfo) item.getMenuInfo();

            MyAdapter.MyViewHolder myViewHolder = (MyAdapter.MyViewHolder) resultRecycler.findViewHolderForAdapterPosition(info.position);

            StreamInfoItem streamInfoItem = myViewHolder.streamInfoItem;


            Intent result = new Intent();
            result.putExtra("newurl", streamInfoItem.getUrl());
            result.putExtra("addtoCurrent",true);
            result.setAction(MainActivity.MAINACTIVITYTBROADCASTACTION);
            Activity activity = this;
            sendBroadcast(result);

            activity.finish();

            return false;
        }
        return true;
    }


    public void Search(String st){

        SearchRecentSuggestions searchRecentSuggestions = new SearchRecentSuggestions(this,mySuggestionProvider.AUTHORITY,mySuggestionProvider.MODE);
        searchRecentSuggestions.saveRecentQuery(st,null);
        actionBar.setTitle(st);
        new Extractor(resultRecycler,searchItems,self,(ProgressBar)findViewById(R.id.loadingCircle)).execute(st);
    }
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }

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
                    Search(query);
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

}

class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder>{


    public static boolean returner=false;
    String searchq;
    List<InfoItem> infoItems;
    Activity activity;

    public MyAdapter(List<InfoItem> infoItems,Activity activity,boolean returner){
        this.infoItems = infoItems;
        this.activity=activity;
        this.returner=returner;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        CardView view= new CardView(viewGroup.getContext());
        ViewGroup.MarginLayoutParams params=new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(10,10,10,10);
        view.setLayoutParams(params);
        view.setPadding(10,10,10,10);

        view.addView((ConstraintLayout) LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.resultcard,viewGroup,false));

        MyViewHolder vh=new MyViewHolder(view);


        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {

        myViewHolder.itemView.setLongClickable(true);

        myViewHolder.streamInfoItem=(StreamInfoItem)infoItems.get(i);
        CardView cardView=myViewHolder.cardView;
        ConstraintLayout constraintLayout=(ConstraintLayout)cardView.getChildAt(0);
        ImageView img=(ImageView)constraintLayout.findViewById(R.id.cardThumb);
        TextView title=(TextView)constraintLayout.findViewById(R.id.queueContent);

        CardView view=cardView;
        TypedValue outValue = new TypedValue();
        view.getContext().getTheme().resolveAttribute(R.attr.selectableItemBackground, outValue, true);
        //view.setBackgroundResource(outValue.resourceId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setForeground(view.getContext().getDrawable(outValue.resourceId));
        }
        title.setText(infoItems.get(i).getName());
        //title.setText("TEST CARD "+new Integer(i).toString());
        //t.setText(new Integer(i).toString());
        //new setThumbCard().execute(myViewHolder);
        Picasso.get()
                .load(infoItems.get(i).getThumbnailUrl())
                .into(img);
    }

    @Override
    public int getItemCount() {
        return infoItems.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        public StreamInfoItem streamInfoItem;
        public CardView cardView;
        public  MyViewHolder(CardView cardView){
            super(cardView);
            this.cardView=cardView;
            if(returner) {
                cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent result = new Intent();
                        result.putExtra("newurl", streamInfoItem.getUrl());
                        result.putExtra("addtoCurrent",false);
                        result.setAction(MainActivity.MAINACTIVITYTBROADCASTACTION);
                        MyViewHolder.this.cardView.getContext().sendBroadcast(result);

                        ((Activity)MyViewHolder.this.cardView.getContext() ).finish();

                    }
                });
            }
        }
    }
}

class Extractor extends AsyncTask<String,Integer,Integer>{


    RecyclerView mRecycler;
    List<InfoItem> infoItems;
    Activity context;
    ProgressBar loader;

    public Extractor(RecyclerView mRecycler,List<InfoItem> infoItems,Activity context,ProgressBar loader){
        this.mRecycler=mRecycler;
        this.infoItems=infoItems;
        this.context=context;
        this.loader=loader;
    }

    @Override
    protected Integer doInBackground(String... strings) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loader.setVisibility(View.VISIBLE);
            }
        });
        String st=strings[0];
        Downloader.init(null);
        NewPipe.init(Downloader.getInstance());
        int serviceID=NewPipe.getIdOfService("YouTube");
        try {
            List<String> contentFilter = new ArrayList<>();
            contentFilter.add("videos");
            YoutubeService ys =(YoutubeService)NewPipe.getService(serviceID);
            YoutubeSearchExtractor ySE=(YoutubeSearchExtractor)ys.getSearchExtractor(st,contentFilter,null,"IN");


            ySE.onFetchPage(Downloader.getInstance());
            ListExtractor.InfoItemsPage<InfoItem> ife= ySE.getInitialPage();
            List<InfoItem> infoItemsList = ife.getItems();
            Log.i("ryd",infoItemsList.toString());
            Log.i("ryd","SEARCH SUGGESTIONS "+ySE.getSearchSuggestion());

            infoItems.clear();
            for(int i=0;i<infoItemsList.size();i++){
                infoItems.add(infoItemsList.get(i));
            }
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loader.setVisibility(View.INVISIBLE);
                    mRecycler.getAdapter().notifyDataSetChanged();
                }
            });

        } catch (ExtractionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
class setThumbCardold extends AsyncTask<MyAdapter.MyViewHolder,Integer,Integer>{

    @Override
    protected Integer doInBackground(MyAdapter.MyViewHolder... viewHolders) {
        try {
            URL urlConnection = new URL(viewHolders[0].streamInfoItem.getThumbnailUrl());
            Log.i("rypd","Thumbnail URL downloading "+urlConnection.toString());
            HttpURLConnection connection = (HttpURLConnection) urlConnection
                    .openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            final Bitmap myBitmap = BitmapFactory.decodeStream(input);
            ConstraintLayout constraintLayout = (ConstraintLayout)viewHolders[0].cardView.getChildAt(0);
            final ImageView img=(ImageView)constraintLayout.findViewById(R.id.cardThumb);
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


