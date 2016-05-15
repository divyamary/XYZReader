package com.example.xyzreader.ui;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private ViewGroup mRootView;
    private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isInternetConnected()) {
                Snackbar snackbar = Snackbar.make(mRootView, getString(R.string.empty_feed), Snackbar.LENGTH_LONG);
                snackbar.show();
                View sbView = snackbar.getView();
                TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
                textView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.accent));
                snackbar.show();
            }
        }
    };
    private IntentFilter iFilter;
    private boolean mIsRefreshing = false;
    private boolean refreshDone;
    private TextView emptyView;
    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                if (mIsRefreshing) {
                    refreshDone = true;
                }
                updateRefreshingUI();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        mRootView = (ViewGroup) findViewById(R.id.main_content);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        emptyView = (TextView) findViewById(R.id.text_empty);
        getSupportLoaderManager().initLoader(0, null, this);
        iFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        if (savedInstanceState == null && !refreshDone) {
            refresh();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
        unregisterReceiver(networkReceiver);
    }

    private boolean isInternetConnected() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null) {
            if (netInfo.getType() == ConnectivityManager.TYPE_WIFI)
                if (netInfo.isConnected())
                    haveConnectedWifi = true;
            if (netInfo.getType() == ConnectivityManager.TYPE_MOBILE)
                if (netInfo.isConnected())
                    haveConnectedMobile = true;
        }

        return haveConnectedWifi || haveConnectedMobile;
    }

    private void refresh() {
        if (isInternetConnected()) {
            if (mRecyclerView.getVisibility() == View.GONE) {
                mRecyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.INVISIBLE);
            }
            startService(new Intent(this, UpdaterService.class));
        } else {
            Snackbar snackbar = Snackbar.make(mRootView, getString(R.string.empty_feed), Snackbar.LENGTH_LONG);
            snackbar.show();
            // Changing snackbar text color
            View sbView = snackbar.getView();
            TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
            textView.setTextColor(ContextCompat.getColor(this, R.color.accent));
            snackbar.show();

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
        registerReceiver(networkReceiver, iFilter);
    }

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        if (cursor != null && cursor.getCount() > 0) {
            mRecyclerView.setAdapter(adapter);
        } else  {
            if(!isInternetConnected()) {
                mRecyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            }
        }
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == android.R.id.home) {
            ActivityCompat.finishAfterTransition(this);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;
        public CardView cardView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (ImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
            cardView = (CardView) view.findViewById(R.id.card);
        }
    }


    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private static final int PHOTO_ANIMATION_DELAY = 600;
        private final Interpolator INTERPOLATOR = new DecelerateInterpolator();
        private Cursor mCursor;
        private boolean lockedAnimations = false;
        private int lastAnimatedItem = -1;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityOptionsCompat options = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        options = ActivityOptionsCompat.makeSceneTransitionAnimation(ArticleListActivity.this,
                                new Pair<View, String>(view.findViewById(R.id.thumbnail),
                                        view.findViewById(R.id.thumbnail).getTransitionName()));
                        ActivityCompat.startActivity(ArticleListActivity.this, new Intent(Intent.ACTION_VIEW,
                                ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))), options.toBundle());
                    } else {

                        startActivity(new Intent(Intent.ACTION_VIEW,
                                ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))));
                    }
                }
            });

            return vh;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            Context context = holder.thumbnailView.getContext();
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            holder.subtitleView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));
            Picasso.with(context)
                    .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                    .into(holder.thumbnailView, new Callback() {
                        @Override
                        public void onSuccess() {
                            animatePhoto(holder);
                        }

                        @Override
                        public void onError() {

                        }
                    });
            ViewCompat.setTransitionName(holder.thumbnailView, "image" + mCursor.getLong(ArticleLoader.Query._ID));
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }

        private void animatePhoto(ViewHolder viewHolder) {
            if (!lockedAnimations) {
                if (lastAnimatedItem == viewHolder.getLayoutPosition()) {
                    setLockedAnimations(true);
                }

                long animationDelay = PHOTO_ANIMATION_DELAY + viewHolder.getLayoutPosition() * 30;
                viewHolder.cardView.setScaleY(0.8f);
                viewHolder.cardView.setAlpha(0.4f);
                viewHolder.cardView.setScaleX(0.8f);
                viewHolder.cardView.animate()
                        .scaleY(1)
                        .scaleX(1)
                        .alpha(1f)
                        .setDuration(100)
                        .setInterpolator(INTERPOLATOR)
                        .setStartDelay(animationDelay)
                        .start();
            }
        }

        public void setLockedAnimations(boolean lockedAnimations) {
            this.lockedAnimations = lockedAnimations;
        }

    }
}
