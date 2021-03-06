package com.wholdus.www.wholdusbuyerapp.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;

import com.daprlabs.aaron.swipedeck.SwipeDeck;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.wholdus.www.wholdusbuyerapp.R;
import com.wholdus.www.wholdusbuyerapp.activities.CartActivity;
import com.wholdus.www.wholdusbuyerapp.activities.CategoryProductActivity;
import com.wholdus.www.wholdusbuyerapp.adapters.ProductSwipeDeckAdapter;
import com.wholdus.www.wholdusbuyerapp.databaseContracts.CatalogContract;
import com.wholdus.www.wholdusbuyerapp.helperClasses.APIConstants;
import com.wholdus.www.wholdusbuyerapp.helperClasses.CartMenuItemHelper;
import com.wholdus.www.wholdusbuyerapp.helperClasses.Constants;
import com.wholdus.www.wholdusbuyerapp.helperClasses.FilterClass;
import com.wholdus.www.wholdusbuyerapp.helperClasses.IntentFilters;
import com.wholdus.www.wholdusbuyerapp.helperClasses.ShortListMenuItemHelper;
import com.wholdus.www.wholdusbuyerapp.helperClasses.TODO;
import com.wholdus.www.wholdusbuyerapp.helperClasses.TrackingHelper;
import com.wholdus.www.wholdusbuyerapp.interfaces.HandPickedListenerInterface;
import com.wholdus.www.wholdusbuyerapp.interfaces.ItemClickListener;
import com.wholdus.www.wholdusbuyerapp.interfaces.ProductCardListenerInterface;
import com.wholdus.www.wholdusbuyerapp.loaders.ProductsLoader;
import com.wholdus.www.wholdusbuyerapp.models.Product;
import com.wholdus.www.wholdusbuyerapp.services.BuyerProductService;
import com.wholdus.www.wholdusbuyerapp.services.CatalogService;

import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by kaustubh on 17/12/16.
 */

public class HandPickedFragment extends Fragment implements ProductCardListenerInterface, ItemClickListener {

    private HandPickedListenerInterface mListener;
    private final int PRODUCTS_DB_LOADER = 30;
    private BroadcastReceiver mResponseReceiver;
    private ProductsLoaderManager mProductsLoader;
    private ArrayList<Product> mProductsArrayList;
    private ArrayList<Integer> mExcludeProductIDs, mProductIDs;
    private SwipeDeck mSwipeDeck;
    private ImageButton mLikeButton, mDislikeButton, mAddToCartButton, mFilterButton;
    private Button mNoProductsLeftButton;
    private ProductSwipeDeckAdapter mProductSwipeDeckAdapter;
    private LinearLayout mNoProductsLeft;
    private TextView mNoProductsLeftTextView;
    private ProgressBar mNoProductsLeftProgressBar;
    private int mPosition = 0, mProductsLeft = 0, mProductBuffer = 8, mProductsPageNumber, mTotalProductPages;
    private boolean mHasSwiped = true, mFirstLoad = true, mButtonEnabled, mCartButtonEnabled = true;

    private static final int mProductsLimit = 20;

    private static final int ANIMATION_DURATION = 400;
    private CartMenuItemHelper mCartMenuItemHelper;
    private ShortListMenuItemHelper mShortListMenuItemHelper;

    private static final String
            INSTRUCTIONS_SHARED_PREFERENCES = "InstructionsSharedPreference",
            LIKED_DISLIKED_BUTTONS_KEY = "LikedDislikedButtonsKey",
            SHORTLIST_ICON_KEY = "ShortlistIconKey",
            FILTER_ICON_KEY = "FilterIconKey";
    private boolean mShowLikedDislikedInstructions, mShowShortlistInstructions, mShowFilterInstructions;

    private Animation mAnimButtonScale, mAnimButtonScaleProminent;
    private FrameLayout mLikeButtonLayout, mDislikeButtonLayout;

    public HandPickedFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (HandPickedListenerInterface) context;
        } catch (ClassCastException cee) {
            cee.printStackTrace();
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle mArgs = getArguments();
        try {
            mProductIDs = mArgs.getIntegerArrayList(CatalogContract.ProductsTable.TABLE_NAME);
        } catch (Exception e) {}
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_handpicked, container, false);
        initReferences(rootView);
        mResponseReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String intentAction = intent.getAction();
                if (intentAction == null){
                    return;
                }
                switch (intentAction){
                    case IntentFilters.BUYER_PRODUCT_DATA_UPDATED:
                        handleBuyerProductAPIResponse();
                        break;
                    case IntentFilters.PRODUCT_DATA:
                        handleProductAPIResponse(intent);
                        break;
                    case IntentFilters.SPECIFIC_PRODUCT_DATA:
                        handleBuyerProductAPIResponse();
                        break;
                    case IntentFilters.CART_ITEM_WRITTEN:
                        refreshCartMenuItemHelper();
                        break;
                }

            }
        };
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListener.fragmentCreated("Wholdus");
        updateUnsyncedResponses();
    }

    @Override
    public void onResume() {
        super.onResume();

        mProductsLoader = new ProductsLoaderManager();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IntentFilters.BUYER_PRODUCT_DATA_UPDATED);
        intentFilter.addAction(IntentFilters.PRODUCT_DATA);
        if (mProductIDs != null) {
            intentFilter.addAction(IntentFilters.SPECIFIC_PRODUCT_DATA);
        }
        intentFilter.addAction(IntentFilters.CART_ITEM_WRITTEN);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mResponseReceiver, intentFilter);

        if (mProductsArrayList.isEmpty()) {
            updateProducts();
        }

        dismissDialog();
        refreshCartMenuItemHelper();
        if (mShortListMenuItemHelper != null){
            mShortListMenuItemHelper.refreshShortListCount();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mResponseReceiver);
        } catch (Exception e) {
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mResponseReceiver = null;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.default_action_buttons, menu);
        mCartMenuItemHelper = new CartMenuItemHelper(getContext(), menu.findItem(R.id.action_bar_checkout), getActivity().getSupportLoaderManager());
        mShortListMenuItemHelper = new ShortListMenuItemHelper(getContext(), menu.findItem(R.id.action_bar_shortlist));
        refreshCartMenuItemHelper();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_bar_checkout:
                startActivity(new Intent(getContext(), CartActivity.class));
                break;
            case R.id.action_bar_shortlist:

                TrackingHelper.getInstance(getContext())
                        .logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, "hand_picked_shortlist", "action_button");

                Intent shortlistIntent = new Intent(getContext(), CategoryProductActivity.class);
                shortlistIntent.putExtra(Constants.TYPE, Constants.FAV_PRODUCTS);
                getContext().startActivity(shortlistIntent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void dismissDialog() {
        refreshCartMenuItemHelper();
    }

    private void handleBuyerProductAPIResponse() {
        if (getActivity() != null && mProductsLeft < mProductBuffer) {
            getActivity().getSupportLoaderManager().restartLoader(PRODUCTS_DB_LOADER, null, mProductsLoader);
        }
    }

    private void initReferences(ViewGroup rootView) {
        SwipeDeck.ANIMATION_DURATION = ANIMATION_DURATION;
        mSwipeDeck = (SwipeDeck) rootView.findViewById(R.id.product_swipe_deck);

        mSwipeDeck.setLeftImage(R.id.left_image);
        mSwipeDeck.setRightImage(R.id.right_image);
        mSwipeDeck.setCallback(new SwipeDeck.SwipeDeckCallback() {
            @Override
            public void cardSwipedLeft(long l) {
                actionAfterSwipe(false);
            }

            @Override
            public void cardSwipedRight(long l) {
                actionAfterSwipe(true);
            }
        });

        mAnimButtonScale = AnimationUtils.loadAnimation(getContext(), R.anim.button_scale_down_up);
        mAnimButtonScaleProminent = AnimationUtils.loadAnimation(getContext(), R.anim.button_scale_down_up_prominent);
        mLikeButtonLayout = (FrameLayout) rootView.findViewById(R.id.hand_picked_like_button_layout);
        mLikeButton = (ImageButton) rootView.findViewById(R.id.hand_picked_like_button);
        mLikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLikeButtonLayout.startAnimation(mAnimButtonScale);
                if (mProductsLeft > 0 && mButtonEnabled) {
                    bringFeedbackImageToFront(true);
                    mHasSwiped = false;
                    mButtonEnabled = false;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mSwipeDeck.swipeTopCardRight(ANIMATION_DURATION);
                        }
                    }, 300);
                }
            }
        });
        mLikeButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                TapTargetView.showFor(getActivity(), getLikeTapTarget());
                return true;
            }
        });
        mDislikeButtonLayout = (FrameLayout) rootView.findViewById(R.id.hand_picked_dislike_button_layout);
        mDislikeButton = (ImageButton) rootView.findViewById(R.id.hand_picked_dislike_button);
        mDislikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDislikeButtonLayout.startAnimation(mAnimButtonScale);
                if (mProductsLeft > 0 && mButtonEnabled) {
                    bringFeedbackImageToFront(false);
                    mHasSwiped = false;
                    mButtonEnabled = false;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mSwipeDeck.swipeTopCardLeft(ANIMATION_DURATION);
                        }
                    }, 300);
                }
            }
        });
        mDislikeButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                TapTargetView.showFor(getActivity(), getDislikeTapTarget());
                return true;
            }
        });
        final FrameLayout addToCartButtonLayout = (FrameLayout) rootView.findViewById(R.id.hand_picked_cart_button_layout);
        mAddToCartButton = (ImageButton) rootView.findViewById(R.id.hand_picked_cart_button);
        mAddToCartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addToCartButtonLayout.startAnimation(mAnimButtonScale);
                if (mProductsLeft > 0 && mButtonEnabled && mCartButtonEnabled) {
                    mCartButtonEnabled = false;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mCartButtonEnabled = true;
                        }
                    }, 300);
                    FragmentManager fragmentManager = getFragmentManager();
                    CartDialogFragment dialogFragment = new CartDialogFragment();
                    Bundle args = new Bundle();
                    args.putInt(CatalogContract.ProductsTable.COLUMN_PRODUCT_ID, mProductsArrayList.get(mPosition).getProductID());
                    dialogFragment.setArguments(args);
                    dialogFragment.show(fragmentManager, dialogFragment.getClass().getSimpleName());
                }
            }
        });
        mAddToCartButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                TapTargetView.showFor(getActivity(), getCartTapTarget());
                return true;
            }
        });
        final FrameLayout filterButtonLayout = (FrameLayout) rootView.findViewById(R.id.hand_picked_filter_button_layout);
        mFilterButton = (ImageButton) rootView.findViewById(R.id.hand_picked_filter_button);
        mFilterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TrackingHelper.getInstance(getContext()).logEvent(
                        FirebaseAnalytics.Event.SELECT_CONTENT,
                        "hand_picked_filter_button",
                        "filter"
                );
                filterButtonLayout.startAnimation(mAnimButtonScale);
                if (mShowFilterInstructions) {
                    showFilterInstructions();
                } else {
                    mListener.openFilter(true);
                    mListener.fragmentCreated("Filter");
                }
            }
        });
        mFilterButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                TapTargetView.showFor(getActivity(), getFilterTapTarget());
                return true;
            }
        });

        mProductsArrayList = new ArrayList<>();
        mExcludeProductIDs = new ArrayList<>();
        mProductSwipeDeckAdapter = new ProductSwipeDeckAdapter(getContext(), mProductsArrayList, this, this);
        mSwipeDeck.setAdapter(mProductSwipeDeckAdapter);

        mNoProductsLeft = (LinearLayout) rootView.findViewById(R.id.no_products_left);
        mNoProductsLeftTextView = (TextView) rootView.findViewById(R.id.no_products_left_text_view);
        mNoProductsLeftButton = (Button) rootView.findViewById(R.id.hand_picked_no_products_left_button);
        mNoProductsLeftProgressBar = (ProgressBar) rootView.findViewById(R.id.loading_indicator);

        resetProducts();

        SharedPreferences instructionsPreferences = getActivity().getSharedPreferences(INSTRUCTIONS_SHARED_PREFERENCES, MODE_PRIVATE);
        mShowLikedDislikedInstructions = instructionsPreferences.getBoolean(LIKED_DISLIKED_BUTTONS_KEY, true);
        mShowShortlistInstructions = instructionsPreferences.getBoolean(SHORTLIST_ICON_KEY, true);
        mShowFilterInstructions = instructionsPreferences.getBoolean(FILTER_ICON_KEY, true);

        if (mShowLikedDislikedInstructions) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startLikeDislikeInstructions();
                }
            }, 800);
        }
    }

    private void fetchBuyerProducts() {
        Intent intent = new Intent(getContext(), BuyerProductService.class);
        intent.putExtra("TODO", TODO.FETCH_BUYER_PRODUCTS);
        getContext().startService(intent);
    }

    private void updateUnsyncedResponses() {
        Intent intent = new Intent(getContext(), BuyerProductService.class);
        intent.putExtra("TODO", TODO.UPDATE_UNSYNCED_BUYER_RESPONSES);
        getContext().startService(intent);
    }

    private void bringFeedbackImageToFront(boolean liked) {
        try {
            int adapterIndex;
            if (mProductsLeft > 2) {
                adapterIndex = 2;
            } else if (mProductsLeft == 2) {
                adapterIndex = 1;
            } else if (mProductsLeft == 1) {
                adapterIndex = 0;
            } else {
                return;
            }
            int resourceID;
            if (liked) {
                resourceID = R.id.right_image;
            } else {
                resourceID = R.id.left_image;
            }
            View convertView = mSwipeDeck.getChildAt(adapterIndex);
            if (convertView == null) {
                return;
            }
            final Animation animFeedbackImage = AnimationUtils.loadAnimation(getContext(), R.anim.feedback_image_fade_in);
            final ImageView imageView = (ImageView) convertView.findViewById(resourceID);
            imageView.setVisibility(View.INVISIBLE);
            imageView.setAlpha((float) 1);
            animFeedbackImage.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    imageView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            imageView.startAnimation(animFeedbackImage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendBuyerProductResponse(boolean liked) {
        Intent intent = new Intent(getContext(), BuyerProductService.class);
        intent.putExtra("TODO", TODO.UPDATE_PRODUCT_RESPONSE);
        intent.putExtra(CatalogContract.ProductsTable.COLUMN_PRODUCT_ID, mProductsArrayList.get(mPosition).getProductID());
        intent.putExtra(CatalogContract.ProductsTable.COLUMN_RESPONDED_FROM, 0);
        intent.putExtra(CatalogContract.ProductsTable.COLUMN_HAS_SWIPED, mHasSwiped);
        intent.putExtra(CatalogContract.ProductsTable.COLUMN_RESPONSE_CODE, liked ? 1 : 2);
        getContext().startService(intent);
        if (mShortListMenuItemHelper != null && liked) {
            mShortListMenuItemHelper.incrementShortListCount();
        }
    }

    private void actionAfterSwipe(boolean liked) {

        if (mHasSwiped){
            if (liked){
                mLikeButtonLayout.startAnimation(mAnimButtonScaleProminent);
            } else {
                mDislikeButtonLayout.startAnimation(mAnimButtonScaleProminent);
            }
        }

        if (liked && mShowShortlistInstructions){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    showShortlistInstructions();
                }
            }, 500);
        }

        sendBuyerProductResponse(liked);

        if (getActivity() == null) {
            return;
        }

        mProductsLeft -= 1;
        if (mProductsLeft < 1) {
            setNoProductsLeftView();
            return;
        }

        mPosition += 1;
        try {
            mListener.fragmentCreated(mProductsArrayList.get(mPosition).getName());
        } catch (Exception e) {
            e.printStackTrace();
            setNoProductsLeftView();
        }

        if (mProductsLeft < mProductBuffer) {
            getActivity().getSupportLoaderManager().restartLoader(PRODUCTS_DB_LOADER, null, mProductsLoader);
            updateProducts();
        }

        mHasSwiped = true;
        mButtonEnabled = true;
    }

    @Override
    public void cardCreated() {
        mNoProductsLeft.setVisibility(View.GONE);
    }

    private void setViewForProducts(ArrayList<Product> data) {
        //TODO: Add products to adapter correctly(not ones already present)

        if (data.size() == 0) {
            if (!mFirstLoad && mProductsArrayList.size() == 0 && mProductIDs == null) {
                setNoProductsLeftView();
            }
            if (mFirstLoad) {
                mFirstLoad = false;
            }
            return;
        }

        if (mFirstLoad || mProductsArrayList.size() == 0) {
            mFirstLoad = false;
            mListener.fragmentCreated(data.get(0).getName());
            mButtonEnabled = true;
            //setButtonsState(true);
        }
        mProductsArrayList.addAll(data);
        for (Product product : data) {
            mExcludeProductIDs.add(product.getProductID());
            if (mProductIDs != null) {
                mProductIDs.remove(Integer.valueOf(product.getProductID()));
            }
        }

        if (mProductIDs != null && mProductIDs.size() == 0) {
            mProductIDs = null;
        }
        mProductSwipeDeckAdapter.notifyDataSetChanged();
        mProductsLeft += data.size();

        if (mProductsLeft < mProductBuffer) {
            updateProducts();
        }
    }

    private void setNoProductsLeftView() {
        //setButtonsState(false);
        mListener.fragmentCreated("Hand Picked For You");
        mSwipeDeck.setVisibility(View.GONE);
        mNoProductsLeft.setVisibility(View.VISIBLE);
        mNoProductsLeftProgressBar.setVisibility(View.GONE);
        mNoProductsLeftTextView.setVisibility(View.VISIBLE);
        if (!FilterClass.isFilterApplied()) {
            mNoProductsLeftTextView.setText(R.string.no_products_left);
        } else {
            mNoProductsLeftTextView.setText(R.string.filter_no_products);
            mNoProductsLeftButton.setVisibility(View.VISIBLE);
            mNoProductsLeftButton.setText(R.string.reset_filter);
            mNoProductsLeftButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.openFilter(true);
                    mListener.fragmentCreated("Filter");
                }
            });
        }
    }

    private void setButtonsState(boolean state) {
        mLikeButton.setEnabled(state);
        mDislikeButton.setEnabled(state);
        mAddToCartButton.setEnabled(state);
        mFilterButton.setEnabled(true);
    }

    private class ProductsLoaderManager implements LoaderManager.LoaderCallbacks<ArrayList<Product>> {

        @Override
        public void onLoaderReset(Loader<ArrayList<Product>> loader) {
        }

        @Override
        public void onLoadFinished(Loader<ArrayList<Product>> loader, ArrayList<Product> data) {
            if (data != null && mListener != null) {
                setViewForProducts(data);
            }
        }


        @Override
        public Loader<ArrayList<Product>> onCreateLoader(final int id, Bundle args) {
            int productLimit;
            ArrayList<Integer> responseCodes = new ArrayList<>();
            responseCodes.add(0);
            // TODO : ?? Also add condition so that buyer product Id is not 0
            if (mProductIDs != null) {
                responseCodes.add(1);
                responseCodes.add(2);
                productLimit = mProductIDs.size();
            } else {
                productLimit = 15;
            }
            return new ProductsLoader(getContext(), mProductIDs, mExcludeProductIDs, responseCodes, null, productLimit);
        }
    }

    @Override
    public void itemClicked(View view, int position, int id) {
        mListener.openProductDetails(mProductsArrayList.get(position).getProductID());
    }

    public void resetProducts() {

        mNoProductsLeft.setVisibility(View.VISIBLE);
        mNoProductsLeftTextView.setVisibility(View.GONE);
        mNoProductsLeftButton.setVisibility(View.GONE);
        mNoProductsLeftProgressBar.setVisibility(View.VISIBLE);
        mProductsArrayList.clear();
        mExcludeProductIDs.clear();
        //mProductSwipeDeckAdapter.notifyDataSetChanged();
        mFirstLoad = true;
        mHasSwiped = true;
        mPosition = 0;
        mProductsLeft = 0;
        mProductsPageNumber = 1;
        mTotalProductPages = -1;
        //setButtonsState(false);
        mButtonEnabled = false;
    }

    public void updateProducts() {
        fetchBuyerProducts();
        getActivity().getSupportLoaderManager().restartLoader(PRODUCTS_DB_LOADER, null, mProductsLoader);
        if (FilterClass.isFilterApplied()) {
            fetchProducts();
        }
        if (mProductIDs != null) {
            fetchSpecificProducts();
        }
    }

    private void fetchSpecificProducts() {
        Intent intent = new Intent(getContext(), CatalogService.class);
        intent.putExtra("TODO", R.integer.fetch_specific_products);
        intent.putExtra("productIDs", TextUtils.join(",", mProductIDs));
        intent.putExtra("items_per_page", String.valueOf(mProductIDs.size()));
        getActivity().startService(intent);
    }

    private void fetchProducts() {
        if (mTotalProductPages == -1 || mTotalProductPages >= mProductsPageNumber) {
            Intent intent = new Intent(getContext(), CatalogService.class);
            intent.putExtra("TODO", R.integer.fetch_products);
            intent.putExtra("page_number", mProductsPageNumber);
            intent.putExtra("items_per_page", mProductsLimit);
            getActivity().startService(intent);
        }
    }

    private void handleProductAPIResponse(final Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras.getString(Constants.ERROR_RESPONSE) != null) {
            return;
        }
        final int pageNumber = intent.getIntExtra(APIConstants.API_PAGE_NUMBER_KEY, -1);
        final int totalPages = intent.getIntExtra(APIConstants.API_TOTAL_PAGES_KEY, 1);
        final int updatedInserted = intent.getIntExtra(Constants.INSERTED_UPDATED, 0);
        if (updatedInserted > 0) {
            if (mProductsPageNumber == 1 && pageNumber == 1) {
                handleBuyerProductAPIResponse();
            } else if (mProductsPageNumber > pageNumber) {
                handleBuyerProductAPIResponse();
            } else if (mProductsPageNumber == pageNumber) {
                handleBuyerProductAPIResponse();
                mProductsPageNumber++;
            }
        }
        mTotalProductPages = totalPages;
    }

    public TapTarget getLikeTapTarget(){
        return TapTarget.forView(
                getView().findViewById(R.id.hand_picked_like_button),
                getResources().getString(R.string.liked_help_title),
                getResources().getString(R.string.liked_help_description)
        )
                .outerCircleColor(R.color.accent)
                .descriptionTextColor(R.color.primary)
                .transparentTarget(false);
    }

    public TapTarget getDislikeTapTarget(){
        return TapTarget.forView(
                getView().findViewById(R.id.hand_picked_dislike_button),
                getResources().getString(R.string.disliked_help_title),
                getResources().getString(R.string.disliked_help_description)
        )
                .outerCircleColor(R.color.primary_text)
                .descriptionTextColor(R.color.primary)
                .transparentTarget(false);
    }

    public void startLikeDislikeInstructions() {
        if (getView() == null || !mShowLikedDislikedInstructions) {
            return;
        }
        new TapTargetSequence(getActivity())
                .targets(getLikeTapTarget(),
                        getDislikeTapTarget()
                )
                .continueOnCancel(true)
                .listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        SharedPreferences instructionsPreferences = getActivity().getSharedPreferences(INSTRUCTIONS_SHARED_PREFERENCES, MODE_PRIVATE);
                        SharedPreferences.Editor editor = instructionsPreferences.edit();
                        editor.putBoolean(LIKED_DISLIKED_BUTTONS_KEY, false);
                        editor.apply();
                        mShowLikedDislikedInstructions = false;
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget) {

                    }

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {

                    }
                })
                .start();
    }

    public TapTarget getShortListTapTarget(){
        Rect shortListIconView = null;
        if (mShortListMenuItemHelper != null){
            shortListIconView = mShortListMenuItemHelper.getBounds();
        }
        if (shortListIconView == null) {
            int iconWidth = 56;
            int toolbarHeight = 56;
            int iconSize = 24;

            toolbarHeight = getPixelsFromDP((toolbarHeight - iconSize) / 2);

            Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.default_toolbar);
            if (toolbar != null) {
                toolbarHeight = toolbar.getHeight() - getPixelsFromDP(iconSize / 2);
            }

            shortListIconView = new Rect(0, 0, getPixelsFromDP(iconSize), getPixelsFromDP(iconSize));

            DisplayMetrics displaymetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int width = displaymetrics.widthPixels;
            shortListIconView.offset(width - getPixelsFromDP(iconWidth * 3 / 2) - getPixelsFromDP(iconSize / 2), toolbarHeight);
        }

        return TapTarget.forBounds(
                shortListIconView,
                getResources().getString(R.string.shortlist_help_title),
                getResources().getString(R.string.shortlist_help_description))
                .outerCircleColor(R.color.accent)
                .descriptionTextColor(R.color.primary)
                .transparentTarget(true);
    }

    public void showShortlistInstructions() {
        if (!mShowShortlistInstructions) {
            return;
        }


        TapTargetView.showFor(
                getActivity(),
                getShortListTapTarget()
        );

        SharedPreferences instructionsPreferences = getActivity().getSharedPreferences(INSTRUCTIONS_SHARED_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = instructionsPreferences.edit();
        editor.putBoolean(SHORTLIST_ICON_KEY, false);
        editor.apply();
        mShowShortlistInstructions = false;
    }

    public TapTarget getCartTapTarget(){
        return TapTarget.forView(
                getView().findViewById(R.id.hand_picked_cart_button),
                getResources().getString(R.string.cart_help_title),
                getResources().getString(R.string.cart_help_description)
        )
                .outerCircleColor(R.color.primary_text)
                .descriptionTextColor(R.color.primary)
                .transparentTarget(false);
    }

    public TapTarget getFilterTapTarget(){
        return TapTarget.forView(
                getView().findViewById(R.id.hand_picked_filter_button),
                getResources().getString(R.string.filter_help_title),
                getResources().getString(R.string.filter_help_description)
        )
                .outerCircleColor(R.color.primary_text)
                .descriptionTextColor(R.color.primary)
                .transparentTarget(false);
    }

    public void showFilterInstructions(){
        if (getView() == null || !mShowFilterInstructions) {
            return;
        }
        new TapTargetSequence(getActivity())
                .targets(
                        getFilterTapTarget()
                )
                .continueOnCancel(true)
                .listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        SharedPreferences instructionsPreferences = getActivity().getSharedPreferences(INSTRUCTIONS_SHARED_PREFERENCES, MODE_PRIVATE);
                        SharedPreferences.Editor editor = instructionsPreferences.edit();
                        editor.putBoolean(FILTER_ICON_KEY, false);
                        editor.apply();
                        mShowFilterInstructions = false;

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mListener.openFilter(true);
                                mListener.fragmentCreated("Filter");
                            }
                        }, 500);

                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget) {

                    }

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {

                    }
                })
                .start();
    }

    private int getPixelsFromDP(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    public int getDPFromPixels(int px) {
        return Math.round(px / (getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private void refreshCartMenuItemHelper(){
        if (mCartMenuItemHelper != null) {
            mCartMenuItemHelper.restartLoader();
        }
    }

}
