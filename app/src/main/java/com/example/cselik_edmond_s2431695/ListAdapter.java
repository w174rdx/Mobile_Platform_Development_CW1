package com.example.cselik_edmond_s2431695;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class ListAdapter extends BaseAdapter {

    // Member Variables
    Context context;
    ArrayList<Currencies> currList;
    LayoutInflater inflater;

    // Constructor
    public ListAdapter(Context context, ArrayList<Currencies> currList) {
        this.context = context;
        this.currList = currList;
        inflater = (LayoutInflater.from(context));
    }

    // Returns the total number of items in the list
    @Override
    public int getCount() {
        return currList.size();
    }

    // Returns the data object at the specified position
    @Override
    public Object getItem(int position) {
        return currList.get(position);
    }

    // Returns the ID of the item (using position as ID here)
    @Override
    public long getItemId(int position) {
        return position;
    }

    // View Generation Method

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        // Inflate Layout if the view is not being recycled
        if (view == null) {
            view = inflater.inflate(R.layout.listview, null);
        }

        // Initialise Views
        LinearLayout currContainer = view.findViewById(R.id.currContainer);
        TextView currCode = view.findViewById(R.id.currCodeText);
        TextView currExchangeRate = view.findViewById(R.id.currExchangeRate);
        ImageView currImage = view.findViewById(R.id.currencyImg);

        // Safety check to ensure index is within bounds
        if (currList == null || i >= currList.size()) {
            return view;
        }

        Currencies currentItem = currList.get(i);

        // Set Background
        currContainer.setBackgroundColor(Color.WHITE);

        // Safe Text Setting
        String name = currentItem.getCurrencyName();
        if (name != null) {
            currCode.setText(name);
        } else {
            currCode.setText("Unknown");
        }

        String code = currentItem.getCurrencyCode();
        if (code == null) code = "???";

        currExchangeRate.setText("1 GBP = " + currentItem.getExchangeRate() + " " + code);

        // Flag Logic (Prevents recycling issues)
        if (currentItem.getFlagId() != 0) {
            // Flag exists: set the image resource
            currImage.setImageResource(currentItem.getFlagId());
            currImage.setVisibility(View.VISIBLE);
        } else {
            // No flag
            currImage.setImageResource(android.R.color.transparent);
        }

        return view;
    }
}