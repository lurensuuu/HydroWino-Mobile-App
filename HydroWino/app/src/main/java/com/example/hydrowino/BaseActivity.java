package com.example.hydrowino;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    private String languageAtCreation;

    @Override
    protected void attachBaseContext(Context newBase) {
        // This ensures the activity starts with the correct language
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Store the language used when this activity was created
        languageAtCreation = LocaleHelper.getSavedLanguage(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // When returning to this activity, check if the language preference has changed
        String currentLanguage = LocaleHelper.getSavedLanguage(this);
        if (!currentLanguage.equals(languageAtCreation)) {
            // If it changed, refresh the activity immediately
            recreate();
        }
    }

}