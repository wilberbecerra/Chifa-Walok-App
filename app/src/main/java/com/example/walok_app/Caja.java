package com.example.walok_app;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Caja extends WalokActivity {
    private EditText pedidoId;
    private Button pay;
    private UserDatabaseHelper helper;
    private long selectedId = -1;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!showScreen(R.layout.activity_caja, true)) return;
        helper = new UserDatabaseHelper(this);
        pedidoId = findViewById(R.id.etPedidoId);
        pay = findViewById(R.id.btnPagar);
        watch(pedidoId, () -> {
            selectedId = -1;
            pay.setEnabled(false);
            findViewById(R.id.orderDetails).setVisibility(View.GONE);
            TextView empty = findViewById(R.id.tvEmpty);
            empty.setText(R.string.cash_empty);
            empty.setVisibility(View.VISIBLE);
            hideFeedback();
        });
        findViewById(R.id.btnBuscar).setOnClickListener(v -> search());
        pedidoId.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) { search(); return true; }
            return false;
        });
        pay.setOnClickListener(v -> registerPayment());
        findViewById(R.id.btnCierreCaja).setOnClickListener(v -> startActivity(new Intent(this, CierreCaja.class)));
    }

    private void search() {
        hideFeedback();
        try {
            long id = Long.parseLong(pedidoId.getText().toString().trim());
            if (id <= 0) throw new NumberFormatException();
            loadOrder(id);
        } catch (NumberFormatException error) {
            feedback(getString(R.string.order_id_invalid), false);
        } catch (Exception error) {
            selectedId = -1;
            pay.setEnabled(false);
            feedback(getString(R.string.search_error), false);
        }
    }

    private boolean isPaid(SQLiteDatabase db, long id) {
        try (Cursor cursor = db.rawQuery("SELECT 1 FROM pagos WHERE pedidoId = ? LIMIT 1",
                new String[]{String.valueOf(id)})) {
            return cursor.moveToFirst();
        }
    }

    private void loadOrder(long id) {
        selectedId = -1;
        pay.setEnabled(false);
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT mesa, plato, cantidad, total FROM pedidos WHERE id = ?",
                new String[]{String.valueOf(id)})) {
            if (!cursor.moveToFirst()) {
                findViewById(R.id.orderDetails).setVisibility(View.GONE);
                TextView empty = findViewById(R.id.tvEmpty);
                empty.setText(R.string.cash_not_found);
                empty.setVisibility(View.VISIBLE);
                return;
            }
            selectedId = id;
            ((TextView) findViewById(R.id.tvPedidoNumero)).setText(getString(R.string.order_number, id));
            ((TextView) findViewById(R.id.etMesa)).setText(cursor.getString(0));
            ((TextView) findViewById(R.id.etPlato)).setText(cursor.getString(1));
            ((TextView) findViewById(R.id.etCantidad)).setText(String.valueOf(cursor.getInt(2)));
            ((TextView) findViewById(R.id.etTotal)).setText(money(cursor.getDouble(3)));
            boolean paid = isPaid(db, id);
            TextView status = findViewById(R.id.tvEstado);
            status.setText(paid ? R.string.paid : R.string.pending);
            status.setTextColor(getColor(paid ? R.color.walok_success : R.color.walok_primary));
            pay.setEnabled(!paid);
            findViewById(R.id.tvEmpty).setVisibility(View.GONE);
            findViewById(R.id.orderDetails).setVisibility(View.VISIBLE);
        }
    }

    private void registerPayment() {
        if (selectedId <= 0) return;
        long id = selectedId;
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            boolean alreadyPaid;
            try {
                alreadyPaid = isPaid(db, id);
                if (!alreadyPaid) {
                    try (Cursor cursor = db.rawQuery("SELECT total FROM pedidos WHERE id = ?",
                            new String[]{String.valueOf(id)})) {
                        if (!cursor.moveToFirst()) throw new IllegalStateException("Missing order");
                        ContentValues values = new ContentValues();
                        values.put("pedidoId", id);
                        values.put("total", cursor.getDouble(0));
                        db.insertOrThrow("pagos", null, values);
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            loadOrder(id);
            feedback(getString(alreadyPaid ? R.string.payment_exists : R.string.payment_success), !alreadyPaid);
        } catch (Exception error) {
            feedback(getString(R.string.payment_error), false);
        }
    }

    @Override protected void onDestroy() {
        if (helper != null) helper.close();
        super.onDestroy();
    }
}
