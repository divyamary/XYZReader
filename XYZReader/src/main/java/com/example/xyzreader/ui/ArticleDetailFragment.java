package com.example.xyzreader.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Picasso;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>{
    private static final String TAG = "ArticleDetailFragment";
    public static final String ARG_ITEM_ID = "item_id";
    public static final String VIEW_NAME_HEADER_IMAGE = "detail:header:image";
    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private ImageView mPhotoView;
    private Toolbar mToolbar;
    private String articleTitle;
    private String bodyText;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private FloatingActionButton mFloatingActionButton;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mToolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(mToolbar);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);
        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        ViewCompat.setTransitionName(mPhotoView, "image"+mItemId);
        Log.d("TNAME 2", ""+ViewCompat.getTransitionName(mPhotoView));
        bindViews();
        collapsingToolbarLayout = (CollapsingToolbarLayout) mRootView.findViewById(R.id.collapsing_toolbar);
        AppBarLayout appBarLayout = (AppBarLayout) mRootView.findViewById(R.id.appBar);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = false;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbarLayout.setTitle(articleTitle);
                    isShow = true;
                } else if(isShow) {
                    collapsingToolbarLayout.setTitle("");
                    isShow = false;
                }
            }
        });
        mFloatingActionButton = (FloatingActionButton)mRootView.findViewById(R.id.share_fab);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent shareIntent = new Intent(
                        android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(
                        android.content.Intent.EXTRA_SUBJECT, articleTitle);
                shareIntent.putExtra(
                        android.content.Intent.EXTRA_TITLE, articleTitle);
                shareIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(bodyText));
                getContext().startActivity(Intent.createChooser(shareIntent,
                        getString(R.string.share_chooser)));
            }
        });
        return mRootView;
    }

    private void scheduleStartPostponedTransition(final View sharedElement) {
        sharedElement.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            getActivity().startPostponedEnterTransition();
                        }
                        return true;
                    }
                });
    }


    private void bindViews() {
        if (mRootView == null) {
            return;
        }
        final TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        final TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);
        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            articleTitle = mCursor.getString(ArticleLoader.Query.TITLE);
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            bylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            bodyText = mCursor.getString(ArticleLoader.Query.BODY);
            bodyView.setText(Html.fromHtml(bodyText));
            Picasso.with(getContext())
                    .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                    .transform(PaletteTransformation.instance())
                    .into(mPhotoView, new PaletteTransformation.PaletteCallback(mPhotoView) {
                        @Override
                        public void onError() {
                            Log.e("DetailFragment", "Palette Error");
                        }

                        @Override
                        public void onSuccess(Palette palette) {
                            scheduleStartPostponedTransition(mPhotoView);
                            int grey900Color = ContextCompat.getColor(getContext(), R.color.grey_900);
                            int grey50Color = ContextCompat.getColor(getContext(), R.color.grey_50);
                            int darkMutedColor = palette.getDarkMutedColor(grey50Color);
                            int lightVibrantColor = palette.getLightVibrantColor(grey50Color);
                            int fabBackgroundColor = palette.getVibrantColor(ContextCompat.getColor(getContext(), R.color.accent));
                            int lightMutedColor= palette.getLightMutedColor(grey50Color);
                            mRootView.findViewById(R.id.meta_bar)
                                    .setBackgroundColor(lightMutedColor);
                            collapsingToolbarLayout.setContentScrimColor(fabBackgroundColor);
                            collapsingToolbarLayout.setCollapsedTitleTextColor(grey50Color);
                            titleView.setTextColor(grey900Color);
                            bylineView.setTextColor(darkMutedColor);
                            //mFloatingActionButton.getDrawable().setColorFilter(lightVibrantColor, PorterDuff.Mode.MULTIPLY);
                            mFloatingActionButton.setBackgroundTintList(ColorStateList.valueOf(fabBackgroundColor));
                            mFloatingActionButton.setRippleColor(lightVibrantColor);
                        }
                    });
        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
            bodyView.setText("N/A");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }
        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }


}
