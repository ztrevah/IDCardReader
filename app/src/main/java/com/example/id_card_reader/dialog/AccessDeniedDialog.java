package com.example.id_card_reader.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.id_card_reader.R;

public class AccessDeniedDialog extends Dialog {
    public AccessDeniedDialog(@NonNull Context context) { // Changed parameter type
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_access_denied);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(getWindow().getAttributes());

            // Set width to a percentage of the screen width (e.g., 90%)
            // You can adjust this percentage as needed.
            layoutParams.width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.90);
            // Set height to wrap content, allowing the dialog to size itself based on content.
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

            getWindow().setAttributes(layoutParams);
        }
        ImageButton closeButton = findViewById(R.id.closeButton);

        closeButton.setOnClickListener(v -> dismiss());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isShowing()) {
                dismiss();
            }
        }, 3000);
    }
}