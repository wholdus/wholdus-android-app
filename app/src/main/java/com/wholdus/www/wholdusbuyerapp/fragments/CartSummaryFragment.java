package com.wholdus.www.wholdusbuyerapp.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.crash.FirebaseCrash;
import com.wholdus.www.wholdusbuyerapp.R;
import com.wholdus.www.wholdusbuyerapp.activities.HandPickedActivity;
import com.wholdus.www.wholdusbuyerapp.adapters.SubCartAdapter;
import com.wholdus.www.wholdusbuyerapp.decorators.RecyclerViewSpaceItemDecoration;
import com.wholdus.www.wholdusbuyerapp.helperClasses.APIConstants;
import com.wholdus.www.wholdusbuyerapp.helperClasses.ContactsHelperClass;
import com.wholdus.www.wholdusbuyerapp.helperClasses.FilterClass;
import com.wholdus.www.wholdusbuyerapp.helperClasses.GlobalAccessHelper;
import com.wholdus.www.wholdusbuyerapp.helperClasses.HelperFunctions;
import com.wholdus.www.wholdusbuyerapp.helperClasses.OkHttpHelper;
import com.wholdus.www.wholdusbuyerapp.helperClasses.TODO;
import com.wholdus.www.wholdusbuyerapp.interfaces.CartListenerInterface;
import com.wholdus.www.wholdusbuyerapp.interfaces.CartSummaryListenerInterface;
import com.wholdus.www.wholdusbuyerapp.loaders.CartLoader;
import com.wholdus.www.wholdusbuyerapp.models.Cart;
import com.wholdus.www.wholdusbuyerapp.models.SubCart;
import com.wholdus.www.wholdusbuyerapp.services.BuyerContactsService;
import com.wholdus.www.wholdusbuyerapp.services.CartService;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import static android.content.Context.MODE_PRIVATE;
import static com.wholdus.www.wholdusbuyerapp.activities.CartActivity.CART_SELLER_MIN_PIECES_KEY;
import static com.wholdus.www.wholdusbuyerapp.activities.CartActivity.CART_SHARED_PREFERENCES;

/**
 * Created by kaustubh on 26/12/16.
 */

public class CartSummaryFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cart>, CartSummaryListenerInterface {

    private CartListenerInterface mListener;
    private BroadcastReceiver mCartServiceResponseReceiver;
    private Cart mCart;

    private TextView mOrderValueTextView, mShippingChargeTextView, mNoProductsLeftTextView;
    private CardView mTopSummary, mNoProducts;
    private Button mContinueShopping;
    private RecyclerView mSubCartListView;

    private SubCartAdapter mSubCartAdapter;
    private ArrayList<SubCart> mSubCarts;

    private static final int CART_DB_LOADER = 90;

    private static final int CONTACTS_PERMISSION = 0;

    private int mCartSellerMinPieces = 5;
    private boolean mCartPiecesCondtionSatisfied = false;

    public CartSummaryFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (CartListenerInterface) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.help_action_buttons, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_bar_call:
                callPhone(getString(R.string.phone1));
                break;
            case R.id.action_bar_chat:
                chatButtonClicked();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void callPhone(String number) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CONTACTS_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    chatButtonClicked();
                } else {
                    Toast.makeText(getContext(), "Permission needed to chat with us", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void chatButtonClicked() {

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ContactsHelperClass contactsHelperClass = new ContactsHelperClass(getActivity().getApplicationContext());
                        String savedNumber = contactsHelperClass.getSavedNumber();
                        if (savedNumber != null) {
                            openWhatsapp(savedNumber);
                        } else {
                            contactsHelperClass.saveWholdusContacts();
                            savedNumber = contactsHelperClass.getSavedNumber();
                            if (savedNumber != null) openWhatsapp(savedNumber);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        FirebaseCrash.report(e);
                    }
                }
            }).start();
            startBuyerContactsService();
        } else {
            requestPermissions(new String[]{Manifest.permission.WRITE_CONTACTS}, CONTACTS_PERMISSION);
        }
    }

    public void startBuyerContactsService(){
        Intent intent = new Intent(getContext(), BuyerContactsService.class);
        intent.putExtra("TODO", TODO.SEND_BUYER_CONTACTS);
        getContext().startService(intent);
    }

    private void openWhatsapp(final String number) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Uri uri = Uri.parse("smsto:" + number);
                    Intent i = new Intent(Intent.ACTION_SENDTO, uri);
                    i.putExtra("sms_body", "I need some help");
                    i.setPackage("com.whatsapp");
                    getContext().startActivity(i);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Whatsapp not installed", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_cart_summary, container, false);
        initReferences(rootView);
        mCartServiceResponseReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleAPIResponse();
            }
        };
        fetchDataFromServer();
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mListener.fragmentCreated("Cart Summary", true);
        getActivity().getSupportLoaderManager().restartLoader(CART_DB_LOADER, null, this);

        IntentFilter intentFilter = new IntentFilter(getString(R.string.cart_data_updated));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mCartServiceResponseReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mCartServiceResponseReceiver);
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
        mCartServiceResponseReceiver = null;
    }

    public void initReferences(ViewGroup view) {

        SharedPreferences cartPreferences = getActivity().getSharedPreferences(CART_SHARED_PREFERENCES, MODE_PRIVATE);
        mCartSellerMinPieces = cartPreferences.getInt(CART_SELLER_MIN_PIECES_KEY, 5);
        new CartSellerMinPiecesRequest().execute();

        mOrderValueTextView = (TextView) view.findViewById(R.id.cart_summary_order_value_text_view);
        mShippingChargeTextView = (TextView) view.findViewById(R.id.cart_summary_shipping_charge_text_view);
        mTopSummary = (CardView) view.findViewById(R.id.top_summary);

        mContinueShopping = (Button) view.findViewById(R.id.continue_shopping_button);
        mContinueShopping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), HandPickedActivity.class);
                FilterClass.resetFilter();
                FilterClass.resetCategoryFilter();
                startActivity(intent);
                getActivity().finish();
            }
        });

        mNoProducts = (CardView) view.findViewById(R.id.no_products);
        mNoProductsLeftTextView = (TextView) view.findViewById(R.id.no_products_left_text_view);

        mSubCartListView = (RecyclerView) view.findViewById(R.id.cart_summary_suborder_list_view);
        mSubCartListView.setItemAnimator(new DefaultItemAnimator());
        mSubCartListView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        mSubCartListView.addItemDecoration(new RecyclerViewSpaceItemDecoration(getResources().getDimensionPixelSize(R.dimen.card_margin_vertical), 0));
        mSubCarts = new ArrayList<>();
        mSubCartAdapter = new SubCartAdapter(getContext(), mSubCarts, mCartSellerMinPieces, this);
        mSubCartListView.setAdapter(mSubCartAdapter);
        mSubCartListView.setNestedScrollingEnabled(false);
    }

    @Override
    public void enableProgressBar() {
        mListener.enableProgressBar();
    }

    private void fetchDataFromServer() {
        syncCartItems();
        Intent cartServiceIntent = new Intent(getContext(), CartService.class);
        cartServiceIntent.putExtra("TODO", R.string.fetch_cart);
        getContext().startService(cartServiceIntent);
    }

    private void syncCartItems(){
        Intent intent = new Intent(getContext(), CartService.class);
        intent.putExtra("TODO", R.string.post_cart_item);
        getContext().startService(intent);
    }

    private void handleAPIResponse() {
        if (getActivity() != null) {
            getActivity().getSupportLoaderManager().restartLoader(CART_DB_LOADER, null, this);
        }
    }

    private void setViewForCart() {
        mNoProducts.setVisibility(View.GONE);
        mTopSummary.setVisibility(View.VISIBLE);

        mSubCarts.clear();
        mSubCarts.addAll(mCart.getSubCarts());
        setBasicCartView();

        mShippingChargeTextView.setText(String.format(getString(R.string.price_format), String.valueOf((int) Math.ceil(mCart.getShippingCharge()))));
        mOrderValueTextView.setText(String.format(getString(R.string.price_format), String.valueOf((int) Math.ceil(mCart.getCalculatedPrice()))));

    }

    private void setViewForEmptyCart() {
        mTopSummary.setVisibility(View.GONE);
        mNoProducts.setVisibility(View.VISIBLE);
        mListener.disableProgressBar();

        mSubCarts.clear();
        setBasicCartView();

        mShippingChargeTextView.setText("");
        mOrderValueTextView.setText("");

    }

    private void setViewForUnsyncedCart() {
        syncCartItems();
        mTopSummary.setVisibility(View.GONE);

        if (!HelperFunctions.isNetworkAvailable(getContext())) {
            mNoProducts.setVisibility(View.VISIBLE);
            mContinueShopping.setVisibility(View.GONE);
            mNoProductsLeftTextView.setText("You do not seem to have an active internet connection.\nPlease connect to the internet and try again");
        }

        mSubCarts.clear();
        setBasicCartView();
        mListener.enableProgressBar();

        mShippingChargeTextView.setText("");
        mOrderValueTextView.setText("");

    }

    private void setBasicCartView() {
        mSubCartAdapter.notifyDataSetChanged();
        //HelperFunctions.setListViewHeightBasedOnChildren(mSubCartListView);

        mListener.setCart(mCart, mCartPiecesCondtionSatisfied);

        mListener.disableProgressBar();
    }


    @Override
    public void onLoaderReset(Loader<Cart> loader) {
    }

    @Override
    public void onLoadFinished(Loader<Cart> loader, Cart data) {
        mCart = data;
        if (data != null && mListener != null) {
            if (data.getSynced() == 1) {
                if (data.getPieces() > 0) {
                    checkCartCondition();
                    setViewForCart();
                } else {
                    mCartPiecesCondtionSatisfied = false;
                    setViewForEmptyCart();
                }
            } else {
                mCartPiecesCondtionSatisfied = false;
                setViewForUnsyncedCart();
            }
        } else if (mListener != null) {
            mCartPiecesCondtionSatisfied = false;
            setViewForEmptyCart();
        }
    }

    @Override
    public Loader<Cart> onCreateLoader(int id, Bundle args) {
        return new CartLoader(getContext(), -1, true, true, true, true);
    }

    private void checkCartCondition(){
        boolean cartPiecesConditionSatisfied = true;
        for (SubCart subCart:mCart.getSubCarts()){
            if (subCart.getPieces() < mCartSellerMinPieces){
                cartPiecesConditionSatisfied = false;
            }
        }
        mCartPiecesCondtionSatisfied = cartPiecesConditionSatisfied;
    }

    private class CartSellerMinPiecesRequest extends AsyncTask<Void, Void, Integer> {

        protected Integer doInBackground(Void... par) {
            HashMap<String, String> params = new HashMap<>();
            String url = GlobalAccessHelper.generateUrl(APIConstants.CART_SELLER_MIN_PIECES_URL, params);
            try {
                okhttp3.Response response = OkHttpHelper.makeGetRequest(getActivity().getApplicationContext(), url);
                if (response.isSuccessful()) {
                    JSONObject responseJSON = new JSONObject(response.body().string());
                    Integer result = responseJSON.getInt("cart_seller_min_pieces");
                    response.body().close();
                    return result;
                } else {
                    return -1;
                }
            }catch (Exception e) {
                return -1;
            }
        }

        protected void onPostExecute(Integer result) {
            if (result != -1 && result != mCartSellerMinPieces){
                mCartSellerMinPieces = result;
                SharedPreferences cartPreferences = getActivity().getSharedPreferences(CART_SHARED_PREFERENCES, MODE_PRIVATE);
                SharedPreferences.Editor editor = cartPreferences.edit();
                editor.putInt(CART_SELLER_MIN_PIECES_KEY, result);
                editor.apply();
            }
        }
    }
}
