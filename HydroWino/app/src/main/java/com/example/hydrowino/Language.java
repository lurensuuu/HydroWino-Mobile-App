package com.example.hydrowino;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.core.content.ContextCompat;

public class Language extends BaseActivity {

    private LinearLayout layoutEnglish, layoutTagalog;
    private RadioButton rbEnglish, rbTagalog;
    private ImageView backLang;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        layoutEnglish = findViewById(R.id.layoutEnglish);
        layoutTagalog = findViewById(R.id.layoutTagalog);
        rbEnglish = findViewById(R.id.rbEnglish);
        rbTagalog = findViewById(R.id.rbTagalog);
        backLang = findViewById(R.id.backLang);

        backLang.setOnClickListener(v -> finish());

        layoutEnglish.setOnClickListener(v -> changeLanguage("en"));
        layoutTagalog.setOnClickListener(v -> changeLanguage("tl"));

        // Update UI based on current selection
        updateUI();
    }

    private void changeLanguage(String newLang) {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String currentLang = prefs.getString("My_Lang", "en");

        if (!currentLang.equals(newLang)) {
            LocaleHelper.setLocale(this, newLang);
            
            // Save the language preference
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("My_Lang", newLang);
            editor.apply();

            // Recreate to apply changes
            recreate();
        }
    }

    private void updateUI() {
        String lang = getSharedPreferences("Settings", MODE_PRIVATE).getString("My_Lang", "en");
        boolean isTagalog = lang.equals("tl");

        rbEnglish.setChecked(!isTagalog);
        rbTagalog.setChecked(isTagalog);

        // Update backgrounds
        layoutEnglish.setBackgroundResource(!isTagalog ? R.drawable.selected_language : R.drawable.edit_text_border);
        layoutTagalog.setBackgroundResource(isTagalog ? R.drawable.selected_language : R.drawable.edit_text_border);

        // Update RadioButton tints
        int selectedColor = ContextCompat.getColor(this, R.color.primary_color);
        int unselectedColor = ContextCompat.getColor(this, R.color.gray);

        rbEnglish.setButtonTintList(ColorStateList.valueOf(!isTagalog ? selectedColor : unselectedColor));
        rbTagalog.setButtonTintList(ColorStateList.valueOf(isTagalog ? selectedColor : unselectedColor));
    }
}
