package com.example.hydrowino;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class display extends AppCompatActivity {

    private RadioButton lightmode, darkmode;
    private LinearLayout layoutLight, layoutDark;
    private ImageView backDisplay;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 1️⃣ Load saved mode BEFORE super.onCreate
        sharedPreferences = getSharedPreferences("MODE", MODE_PRIVATE);
        int nightMode = sharedPreferences.getInt("night", AppCompatDelegate.MODE_NIGHT_NO);
        AppCompatDelegate.setDefaultNightMode(nightMode);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        // 2️⃣ Initialize views
        lightmode = findViewById(R.id.lightMode);
        darkmode = findViewById(R.id.darkMode);
        layoutLight = findViewById(R.id.layoutlight);
        layoutDark = findViewById(R.id.layoutdark);
        backDisplay = findViewById(R.id.backDisplay);

        //Back button
        backDisplay.setOnClickListener(v -> finish());

        //Set click listeners
        layoutLight.setOnClickListener(v -> selectMode(true));
        layoutDark.setOnClickListener(v -> selectMode(false));

        lightmode.setOnClickListener(v -> selectMode(false));
        darkmode.setOnClickListener(v -> selectMode(true));

        //Apply saved selection
        selectMode(nightMode != AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void selectMode(boolean dark) {

        // Update RadioButtons
        lightmode.setChecked(!dark);
        darkmode.setChecked(dark);

        // Update layout backgrounds
        layoutLight.setBackgroundResource(dark ? R.drawable.edit_text_border : R.drawable.selected_language);
        layoutDark.setBackgroundResource(dark ? R.drawable.selected_language : R.drawable.edit_text_border);

        // Update button tints
        lightmode.setButtonTintList(ColorStateList.valueOf(
                getResources().getColor(dark ? R.color.gray : R.color.primary_color)
        ));
        darkmode.setButtonTintList(ColorStateList.valueOf(
                getResources().getColor(dark ? R.color.primary_color : R.color.gray)
        ));

        // Save preference
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("night", dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        editor.apply();

        // Apply theme
        AppCompatDelegate.setDefaultNightMode(dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
}