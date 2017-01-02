package com.wholdus.www.wholdusbuyerapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.wholdus.www.wholdusbuyerapp.R;
import com.wholdus.www.wholdusbuyerapp.helperClasses.HelperFunctions;
import com.wholdus.www.wholdusbuyerapp.models.Suborder;

import java.util.ArrayList;

/**
 * Created by kaustubh on 31/12/16.
 */

public class SubOrderAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<Suborder> mData;

    public SubOrderAdapter(Context context, ArrayList<Suborder> data) {
        mContext = context;
        mData = data;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int i) {
        return mData.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        ViewHolder holder;

        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.list_view_subcart, viewGroup, false);
            holder = new ViewHolder();
            holder.sellerName = (TextView) view.findViewById(R.id.subcart_seller_name_text_view);
            holder.summary = (TextView) view.findViewById(R.id.subcart_summary_text_view);
            holder.orderItems = (ListView) view.findViewById(R.id.sub_cart_items_list_view);
            view.setTag(holder);
        }
        else {
            holder = (ViewHolder) view.getTag();
        }

        Suborder subOrder = mData.get(i);

        holder.sellerName.setText(subOrder.getSeller().getCompanyName());
        holder.summary.setText( String.valueOf(subOrder.getPieces()) + " pieces - Rs. " +
                String.format("%.0f",subOrder.getFinalPrice()));

        OrderItemsAdapter orderItemsAdapter = new OrderItemsAdapter(mContext, subOrder.getOrderItems());
        holder.orderItems.setAdapter(orderItemsAdapter);
        HelperFunctions.setListViewHeightBasedOnChildren(holder.orderItems);

        return view;
    }

    private class ViewHolder{
        int id;
        TextView sellerName;
        TextView summary;
        ListView orderItems;
    }
}