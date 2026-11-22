package com.example.walok_app;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class Pedido extends WalokActivity {
    private Spinner mesa, plato;
    private EditText cantidad;
    private TextView precio, total;
    private UserDatabaseHelper helper;
    private final List<Plato> platos = new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!showScreen(R.layout.activity_pedido, true)) return;
        helper = new UserDatabaseHelper(this);
        mesa = findViewById(R.id.spMesa);
        plato = findViewById(R.id.spPlato);
        cantidad = findViewById(R.id.etCantidad);
        precio = findViewById(R.id.tvPrecio);
        total = findViewById(R.id.tvTotalPedido);
        List<String> mesas = new ArrayList<>();
        mesas.add(getString(R.string.select_table));
        for (int n = 1; n <= 5; n++) mesas.add(getString(R.string.table_number, n));
        ArrayAdapter<String> tableAdapter = new ArrayAdapter<>(this, R.layout.walok_spinner_item, mesas);
        tableAdapter.setDropDownViewResource(R.layout.walok_spinner_item);
        mesa.setAdapter(tableAdapter);
        String[] names = getResources().getStringArray(R.array.platos_array);
        String[] prices = getResources().getStringArray(R.array.precios_array);
        for (int i = 0; i < names.length; i++) platos.add(new Plato(names[i], Double.parseDouble(prices[i])));
        ArrayAdapter<Plato> dishAdapter = new ArrayAdapter<>(this, R.layout.walok_spinner_item, platos);
        dishAdapter.setDropDownViewResource(R.layout.walok_spinner_item);
        plato.setAdapter(dishAdapter);
        plato.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { updateAmount(); }
            @Override public void onNothingSelected(AdapterView<?> parent) { updateAmount(); }
        });
        watch(cantidad, this::updateAmount);
        findViewById(R.id.btnGenerar).setOnClickListener(v -> registerOrder());
    }

    private int quantity() {
        try { return Integer.parseInt(cantidad.getText().toString().trim()); }
        catch (NumberFormatException error) { return 0; }
    }

    private void updateAmount() {
        Plato selected = (Plato) plato.getSelectedItem();
        double unit = selected == null ? 0 : selected.precio;
        int count = quantity();
        precio.setText(money(unit));
        total.setText(money(count > 0 && count <= 999 ? unit * count : 0));
    }

    private void registerOrder() {
        if (mesa.getSelectedItemPosition() == 0 || plato.getSelectedItemPosition() == 0
                || cantidad.getText().toString().trim().isEmpty()) {
            feedback(getString(R.string.order_missing), false);
            return;
        }
        int count = quantity();
        if (count < 1 || count > 999) {
            feedback(getString(R.string.quantity_invalid), false);
            return;
        }
        Plato selected = (Plato) plato.getSelectedItem();
        double amount = selected.precio * count;
        ContentValues values = new ContentValues();
        values.put("mesa", String.valueOf(mesa.getSelectedItemPosition()));
        values.put("plato", selected.nombre);
        values.put("cantidad", count);
        values.put("precio", selected.precio);
        values.put("total", amount);
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            long id = db.insertOrThrow("pedidos", null, values);
            mesa.setSelection(0);
            plato.setSelection(0);
            cantidad.setText("");
            feedback(getString(R.string.order_success, id, money(amount)), true);
        } catch (Exception error) {
            feedback(getString(R.string.order_error), false);
        }
    }

    @Override protected void onDestroy() {
        if (helper != null) helper.close();
        super.onDestroy();
    }
}
