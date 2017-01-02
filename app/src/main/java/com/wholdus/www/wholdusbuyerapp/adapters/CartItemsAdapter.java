package com.wholdus.www.wholdusbuyerapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.wholdus.www.wholdusbuyerapp.R;
import com.wholdus.www.wholdusbuyerapp.databaseContracts.CartContract;
import com.wholdus.www.wholdusbuyerapp.databaseContracts.CatalogContract;
import com.wholdus.www.wholdusbuyerapp.helperClasses.Constants;
import com.wholdus.www.wholdusbuyerapp.models.CartItem;
import com.wholdus.www.wholdusbuyerapp.models.Product;
import com.wholdus.www.wholdusbuyerapp.services.CartService;

import java.util.ArrayList;

/**
 * Created by kaustubh on 30/12/16.
 */

public class CartItemsAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<CartItem> mData;

    public CartItemsAdapter(Context context, ArrayList<CartItem> data) {
        mContext = context;
        mData = data;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public Object getItem(int i) {
        return mData.get(i);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        final ViewHolder holder;

        if(convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item_layout_cart_items, viewGroup, false);
            holder = new ViewHolder();
            holder.productName = (TextView) convertView.findViewById(R.id.cart_item_product_name_text_view);
            holder.pricePerPiece = (TextView) convertView.findViewById(R.id.cart_item_product_price_per_piece_text_view);
            holder.total = (TextView) convertView.findViewById(R.id.cart_item_final_price_text_view);
            holder.pieces = (Spinner) convertView.findViewById(R.id.cart_item_pieces_spinner);
            holder.progressBar = (ProgressBar) convertView.findViewById(R.id.loading_indicator);
            holder.productImage = (ImageView) convertView.findViewById(R.id.product_image);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        CartItem cartItem = mData.get(position);
        final Product product = cartItem.getProduct();

        holder.productName.setText(product.getName());
        holder.pricePerPiece.setText("Rs. " + String.format("%.0f", product.getMinPricePerUnit()));
        holder.total.setText("Rs. " + String.format("%.0f", cartItem.getFinalPrice()));

        ArrayAdapter<Integer> piecesAdapter = new ArrayAdapter<>(mContext, R.layout.cart_dialog_spinner_text_view);
        for (int j = 1; j < 11; j++) {
            piecesAdapter.add(product.getLotSize() * j);
        }
        piecesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.pieces.setAdapter(piecesAdapter);
        final int oldSelectionPosition = piecesAdapter.getPosition(cartItem.getLots() * product.getLotSize());
        holder.pieces.setSelection(oldSelectionPosition);

        Glide.with(mContext)
                .load(product.getImageUrl(Constants.SMALL_IMAGE, "1"))
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                .into(holder.productImage);

        holder.pieces.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (oldSelectionPosition != i) {
                    holder.pieces.setSelection(i);
                    addProductToCart(i, ((int) holder.pieces.getSelectedItem()) / product.getLotSize(), product);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        return convertView;
    }

    public void addProductToCart(int i, int lots, Product product) {
        Intent intent = new Intent(mContext, CartService.class);
        intent.putExtra("TODO", R.string.write_cart_item);
        intent.putExtra(CatalogContract.ProductsTable.COLUMN_PRODUCT_ID, product.getProductID());
        intent.putExtra(CartContract.CartItemsTable.COLUMN_LOTS, lots);
        intent.putExtra(CartContract.CartItemsTable.COLUMN_PIECES, product.getLotSize() * lots);
        intent.putExtra(CartContract.CartItemsTable.COLUMN_LOT_SIZE, product.getLotSize());
        intent.putExtra(CartContract.CartItemsTable.COLUMN_RETAIL_PRICE_PER_PIECE, product.getPricePerUnit());
        intent.putExtra(CartContract.CartItemsTable.COLUMN_CALCULATED_PRICE_PER_PIECE, product.getMinPricePerUnit());
        intent.putExtra(CartContract.CartItemsTable.COLUMN_FINAL_PRICE, product.getMinPricePerUnit() * lots * product.getLotSize());
        mContext.startService(intent);
    }

    class ViewHolder{
        int id;
        TextView productName;
        TextView pricePerPiece;
        Spinner pieces;
        TextView total;
        ImageView productImage;
        ProgressBar progressBar;
    }
}