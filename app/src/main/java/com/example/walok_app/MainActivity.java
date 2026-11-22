package com.example.walok_app;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

public class MainActivity extends WalokActivity {
    private EditText email, password;
    private UserDatabaseHelper helper;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!currentUser().isEmpty()) {
            openMenu();
            return;
        }
        showScreen(R.layout.activity_main, false);
        helper = new UserDatabaseHelper(this);
        email = findViewById(R.id.etUsername);
        password = findViewById(R.id.etPassword);
        findViewById(R.id.btnLogIn).setOnClickListener(v -> login());
        password.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE) { login(); return true; }
            return false;
        });
    }

    private void login() {
        String account = email.getText().toString().trim();
        String secret = password.getText().toString();
        if (account.isEmpty() || secret.isEmpty()) {
            feedback(getString(R.string.login_required), false);
            return;
        }
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            try (Cursor cursor = db.rawQuery("SELECT id FROM users WHERE email = ? AND password = ?",
                    new String[]{account, secret})) {
                if (!cursor.moveToFirst()) {
                    feedback(getString(R.string.login_invalid), false);
                    return;
                }
            }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("usuario", account).apply();
            openMenu();
        } catch (Exception error) {
            feedback(getString(R.string.login_error), false);
        }
    }

    private void openMenu() {
        startActivity(new Intent(this, Menu.class));
        finish();
    }

    @Override protected void onDestroy() {
        if (helper != null) helper.close();
        super.onDestroy();
    }
}
