package com.example.walok_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class Menu extends WalokActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!showScreen(R.layout.activity_menu, true)) return;
        ((TextView) findViewById(R.id.tvUsuario)).setText(getString(R.string.session_user, currentUser()));
        findViewById(R.id.btnPedido).setOnClickListener(v -> startActivity(new Intent(this, Pedido.class)));
        findViewById(R.id.btnCaja).setOnClickListener(v -> startActivity(new Intent(this, Caja.class)));
        findViewById(R.id.btnCierreCaja).setOnClickListener(v -> startActivity(new Intent(this, CierreCaja.class)));
        findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> leaveSession());
    }
}
