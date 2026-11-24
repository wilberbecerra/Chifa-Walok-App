package com.example.walok_app;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.Locale;

/** Shared presentation and navigation; it never clears application data. */
public abstract class WalokActivity extends AppCompatActivity {
    protected static final String PREFS = "walok_prefs";

    protected boolean showScreen(int layout, boolean sessionRequired) {
        if (sessionRequired && currentUser().isEmpty()) {
            leaveSession();
            return false;
        }
        setContentView(layout);
        boolean loginScreen = layout == R.layout.activity_main;
        boolean lightNavigation = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
        int backgroundColor = getColor(loginScreen ? R.color.walok_card : R.color.walok_background);
        int statusColor = getColor(loginScreen ? R.color.walok_card : R.color.walok_header);
        int navigationColor = lightNavigation ? backgroundColor
                : getColor(loginScreen ? R.color.walok_primary : R.color.walok_header);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(loginScreen);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightNavigationBars(lightNavigation);
        getWindow().setStatusBarColor(statusColor);
        getWindow().setNavigationBarColor(navigationColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        View root = findViewById(R.id.screenRoot);
        LayerDrawable backdrop = new LayerDrawable(new Drawable[]{
                new ColorDrawable(backgroundColor),
                new ColorDrawable(statusColor),
                new ColorDrawable(navigationColor)});
        backdrop.setLayerGravity(1, Gravity.TOP);
        backdrop.setLayerHeight(1, 0);
        backdrop.setLayerGravity(2, Gravity.BOTTOM);
        backdrop.setLayerHeight(2, 0);
        root.setBackground(backdrop);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            if (backdrop.getLayerHeight(1) != bars.top) backdrop.setLayerHeight(1, bars.top);
            if (backdrop.getLayerHeight(2) != bars.bottom) backdrop.setLayerHeight(2, bars.bottom);
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard.bottom));
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
        View back = findViewById(R.id.btnCerrar);
        if (back != null) back.setOnClickListener(v -> finish());
        return true;
    }

    protected String currentUser() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getString("usuario", "");
    }

    protected void leaveSession() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove("usuario").apply();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    protected void feedback(CharSequence message, boolean success) {
        TextView view = findViewById(R.id.tvFeedback);
        view.setText(message);
        view.setTextColor(getColor(success ? R.color.walok_success : R.color.walok_primary));
        view.setBackgroundResource(success ? R.drawable.walok_success : R.drawable.walok_error);
        view.setVisibility(View.VISIBLE);
    }

    protected void hideFeedback() {
        findViewById(R.id.tvFeedback).setVisibility(View.GONE);
    }

    protected static String money(double value) {
        return String.format(Locale.US, "S/ %.2f", value);
    }

    protected void watch(EditText input, Runnable action) {
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable text) { action.run(); }
        });
    }
}
