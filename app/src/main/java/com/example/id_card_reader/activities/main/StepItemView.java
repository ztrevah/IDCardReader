package com.example.id_card_reader.activities.main;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.id_card_reader.R;

public class StepItemView {

    private final LinearLayout rootView;
    private final TextView stepNumberTextView;
    private final TextView stepTextView;
    private final ImageView tickImageView;
    private final float defaultTextSize;

    public StepItemView(LinearLayout rootView) {
        this.rootView = rootView;
        this.stepNumberTextView = rootView.findViewById(R.id.stepNumberTextView);
        this.stepTextView = rootView.findViewById(R.id.stepTextView);
        this.tickImageView = rootView.findViewById(R.id.tickImageView);
        this.defaultTextSize = stepTextView.getTextSize();
    }

    public void setStepNumber(String number) {
        this.stepNumberTextView.setText(number);
    }

    public void setStepDescription(String description) {
        this.stepTextView.setText(description);
    }

    public void showTick(boolean show) {
        this.tickImageView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void emphasize(boolean emphasize) {
        int style = emphasize ? Typeface.BOLD : Typeface.NORMAL;
        float size = emphasize ? defaultTextSize * 1.1f : defaultTextSize;
        setTextStyle(stepNumberTextView, style, size);
        setTextStyle(stepTextView, style, size);
    }

    public void setVisibility(int visibility) {
        this.rootView.setVisibility(visibility);
    }

    public LinearLayout getRootView() {
        return rootView;
    }

    private void setTextStyle(TextView textView, int style, float size) {
        textView.setTypeface(null, style);
        textView.setTextSize(0, size);
    }
}