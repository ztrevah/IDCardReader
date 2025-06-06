package com.example.id_card_reader.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap; // Import Bitmap
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

public class AccessGrantedDialog extends Dialog {

    private final String userId;
    private final String userName;
    private final String userDob;
    private final String userGender;
    private final Bitmap profileBitmap; // Changed from Integer to Bitmap

    /**
     * Constructor for the AccessGrantedDialog.
     *
     * @param context The context from which the dialog is being shown.
     * @param userId The ID of the user.
     * @param userName The name of the user.
     * @param userDob The date of birth of the user.
     * @param userGender The gender of the user.
     * @param profileBitmap An optional Bitmap object for the profile image.
     * Pass null if no image is to be set initially.
     */
    public AccessGrantedDialog(@NonNull Context context,
                               String userId,
                               String userName,
                               String userDob,
                               String userGender,
                               @Nullable Bitmap profileBitmap) { // Changed parameter type
        super(context);
        this.userId = userId;
        this.userName = userName;
        this.userDob = userDob;
        this.userGender = userGender;
        this.profileBitmap = profileBitmap; // Assign the Bitmap
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_access_granted);

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

        // Initialize UI components from the dialog layout.
        ImageButton closeButton = findViewById(R.id.closeButton);
        ImageView profileImage = findViewById(R.id.profileImage);
        TextView idValue = findViewById(R.id.idValue);
        TextView nameValue = findViewById(R.id.nameValue);
        TextView dobValue = findViewById(R.id.dobValue);
        TextView genderValue = findViewById(R.id.genderValue);

        idValue.setText(userId);
        nameValue.setText(userName);
        dobValue.setText(userDob);
        genderValue.setText(userGender);

        if (profileBitmap != null) {
            profileImage.setImageBitmap(profileBitmap); // Set Bitmap directly
        } else {
             profileImage.setVisibility(View.GONE);
        }

        closeButton.setOnClickListener(v -> dismiss());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isShowing()) {
                dismiss();
            }
        }, 3000);
    }
}