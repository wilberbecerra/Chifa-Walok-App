package com.example.walok_app;

import android.content.Intent;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
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
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightNavigationBars(true);
        getWindow().setStatusBarColor(getColor(R.color.walok_background));
        getWindow().setNavigationBarColor(getColor(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? R.color.walok_background : R.color.walok_ink));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        View root = findViewById(R.id.screenRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
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
