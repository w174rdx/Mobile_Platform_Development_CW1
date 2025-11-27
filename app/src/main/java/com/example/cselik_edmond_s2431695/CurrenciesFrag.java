package com.example.cselik_edmond_s2431695;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class CurrenciesFrag extends Fragment {

    //  UI Elements
    private ImageView bottomCurr, topCurr;
    private EditText editTextAmount;
    private TextView result, currCodeTxtView, currCodeTxtViewTop;
    private Button convertBtn, swapBtn, exitBtn;

    //  Logic Variables
    private boolean swapped = false;
    private float currentExchangeRate;
    private String currCode;
    private int currencyImgId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the updated layout
        View v = inflater.inflate(R.layout.currency_fragment, container, false);

        // Retrieve passed data
        if (getArguments() != null) {
            currentExchangeRate = getArguments().getFloat("EXCHANGE_RATE");
            currCode = getArguments().getString("CURRENCY_CODE");
            currencyImgId = getArguments().getInt("IMAGE_ID");
        }

        // Initialise Views
        editTextAmount = v.findViewById(R.id.editTxtAmount);
        result = v.findViewById(R.id.resultTxt);
        convertBtn = v.findViewById(R.id.btnConvert);
        swapBtn = v.findViewById(R.id.btnSwap);
        exitBtn = v.findViewById(R.id.btnExit);

        currCodeTxtViewTop = v.findViewById(R.id.currCodeText_1);
        currCodeTxtView = v.findViewById(R.id.currCodeText_2);
        topCurr = v.findViewById(R.id.topImg);
        bottomCurr = v.findViewById(R.id.bottomImg);

        // Set Initial State (Default: GBP on top)
        currCodeTxtViewTop.setText("GBP");
        currCodeTxtView.setText(currCode);
        topCurr.setImageResource(R.drawable.gb);
        bottomCurr.setImageResource(currencyImgId);

        // Restore instance state if rotated (maintain result text)
        if (savedInstanceState != null) {
            result.setText(savedInstanceState.getString("CONVERSION_RESULT"));
        }


        // Exit Button
        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).closeFragment();
                }
            }
        });

        // Convert Button
        convertBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                result.setText("");
                String input = editTextAmount.getText().toString();

                // Validation
                if (input.isEmpty()) {
                    Toast.makeText(getActivity(), "This must not be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validation
                float amount = Float.parseFloat(input);
                if (amount < 0.01) {
                    Toast.makeText(getActivity(), "This must be greater than 0", Toast.LENGTH_SHORT).show();
                } else {
                    // Calculate based on swap state
                    if (!swapped) {
                        // GBP -> Foreign Currency
                        float conversion = amount * currentExchangeRate;
                        result.setText(String.format("%.2f GBP = %.2f %s", amount, conversion, currCode));
                    } else {
                        // Foreign Currency -> GBP
                        float conversion = amount / currentExchangeRate;
                        result.setText(String.format("%.2f %s = %.2f GBP", amount, currCode, conversion));
                    }
                }
            }
        });

        // Swap Button
        swapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!swapped) {
                    swapped = true;
                    // Update UI to show Foreign -> GBP
                    currCodeTxtView.setText("GBP");
                    currCodeTxtViewTop.setText(currCode);
                    editTextAmount.setHint("Enter amount (" + currCode + ")");
                    bottomCurr.setImageResource(R.drawable.gb);
                    topCurr.setImageResource(currencyImgId);
                } else {
                    swapped = false;
                    // Update UI to show GBP -> Foreign
                    currCodeTxtView.setText(currCode);
                    currCodeTxtViewTop.setText("GBP");
                    editTextAmount.setHint("Enter amount (GBP)");
                    topCurr.setImageResource(R.drawable.gb);
                    bottomCurr.setImageResource(currencyImgId);
                }
                // Clear previous results and focus on swap
                editTextAmount.setText("");
                result.setText("");
                editTextAmount.clearFocus();
            }
        });

        return v;
    }

    // Persist the result text during configuration changes
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (result != null) {
            outState.putString("CONVERSION_RESULT", result.getText().toString());
        }
    }
}