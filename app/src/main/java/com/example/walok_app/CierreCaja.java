package com.example.walok_app;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CierreCaja extends WalokActivity {
    private UserDatabaseHelper helper;
    private String fecha;
    private int pedidos;
    private double importe;
    private boolean saving;
    private Button save;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!showScreen(R.layout.activity_cierre_caja, true)) return;
        helper = new UserDatabaseHelper(this);
        save = findViewById(R.id.btnCerrarCaja);
        save.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_close_title)
                .setMessage(R.string.confirm_close_description)
                .setNegativeButton(R.string.cancel_close, null)
                .setPositiveButton(R.string.confirm_close, (dialog, which) -> saveClose())
                .show());
    }

    @Override protected void onResume() {
        super.onResume();
        if (helper != null) loadSummary();
    }

    private void loadSummary() {
        Date now = new Date();
        fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now);
        ((TextView) findViewById(R.id.etFecha)).setText(
                new SimpleDateFormat("d 'de' MMMM 'de' yyyy", new Locale("es", "PE")).format(now));
        ((TextView) findViewById(R.id.etResponsable)).setText(currentUser());
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            try (Cursor cursor = db.rawQuery("SELECT COUNT(*), COALESCE(SUM(total), 0) FROM pedidos", null)) {
                if (cursor.moveToFirst()) {
                    pedidos = cursor.getInt(0);
                    importe = cursor.getDouble(1);
                    ((TextView) findViewById(R.id.etBoletas)).setText(String.valueOf(pedidos));
                    ((TextView) findViewById(R.id.etVentas)).setText(money(importe));
                }
            }
            save.setEnabled(true);
        } catch (Exception error) {
            save.setEnabled(false);
            feedback(getString(R.string.summary_error), false);
        }
    }

    private void saveClose() {
        if (saving) return;
        saving = true;
        save.setEnabled(false);
        try {
            ContentValues values = new ContentValues();
            values.put("responsable", currentUser());
            values.put("fecha", fecha);
            values.put("boletas", pedidos);
            values.put("ventas", importe);
            helper.getWritableDatabase().insertOrThrow("cierres", null, values);
            Toast.makeText(this, R.string.close_success, Toast.LENGTH_SHORT).show();
            leaveSession();
        } catch (Exception error) {
            saving = false;
            save.setEnabled(true);
            feedback(getString(R.string.close_error), false);
        }
    }

    @Override protected void onDestroy() {
        if (helper != null) helper.close();
        super.onDestroy();
    }
}
