package com.example.walok_app;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Exercise real Activities and SQLite on an emulator, without resetting the database. */
@RunWith(AndroidJUnit4.class)
public class WalokFlowTest {
    private Context context;
    private UserDatabaseHelper helper;
    private SQLiteDatabase db;
    private String originalUser;
    private Locale originalLocale;
    private final List<Long> createdOrders = new ArrayList<>();
    private long createdClose = -1;

    @Before public void prepare() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        helper = new UserDatabaseHelper(context);
        db = helper.getWritableDatabase();
        originalUser = context.getSharedPreferences("walok_prefs", Context.MODE_PRIVATE).getString("usuario", "");
        originalLocale = Locale.getDefault();
    }

    private void session(String user) {
        context.getSharedPreferences("walok_prefs", Context.MODE_PRIVATE).edit().putString("usuario", user).commit();
    }

    private long scalar(String sql, String... args) {
        try (Cursor cursor = db.rawQuery(sql, args)) {
            cursor.moveToFirst();
            return cursor.getLong(0);
        }
    }

    private Intent launch(Class<?> activity) {
        return new Intent(context, activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    }

    @Test public void openingOrdersPreservesExistingRecords() {
        ContentValues values = new ContentValues();
        values.put("mesa", "3");
        values.put("plato", "Arroz Chaufa");
        values.put("cantidad", 2);
        values.put("precio", 18);
        values.put("total", 36);
        long id = db.insertOrThrow("pedidos", null, values);
        createdOrders.add(id);
        long users = scalar("SELECT COUNT(*) FROM users");
        session("usuario@walok.com");
        try (ActivityScenario<Pedido> scenario = ActivityScenario.launch(launch(Pedido.class))) {
            onView(withId(R.id.spMesa)).check(matches(isDisplayed()));
            assertEquals(1, scalar("SELECT COUNT(*) FROM pedidos WHERE id = ?", String.valueOf(id)));
            assertEquals(users, scalar("SELECT COUNT(*) FROM users"));
            scenario.recreate();
            assertEquals(1, scalar("SELECT COUNT(*) FROM pedidos WHERE id = ?", String.valueOf(id)));
        }
    }

    @Test public void loginOrderPaymentAndConfirmedClosePersistCorrectly() {
        session("");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(launch(MainActivity.class))) {
            onView(withId(R.id.etUsername)).perform(scrollTo(), replaceText("usuario@walok.com"), closeSoftKeyboard());
            onView(withId(R.id.etPassword)).perform(scrollTo(), replaceText("incorrecta"), closeSoftKeyboard());
            onView(withId(R.id.btnLogIn)).perform(scrollTo(), click());
            onView(withId(R.id.tvFeedback)).check(matches(withText(R.string.login_invalid)));
            onView(withId(R.id.etPassword)).perform(scrollTo(), replaceText("12345678"), closeSoftKeyboard());
            onView(withId(R.id.btnLogIn)).perform(scrollTo(), click());
            onView(withId(R.id.btnPedido)).perform(scrollTo(), click());
            onView(withId(R.id.spMesa)).perform(scrollTo(), click());
            onData(hasToString("Mesa 3")).perform(click());
            onView(withId(R.id.spPlato)).perform(scrollTo(), click());
            onData(hasToString("Arroz Chaufa")).perform(click());
            onView(withId(R.id.etCantidad)).perform(scrollTo(), replaceText("0"), closeSoftKeyboard());
            onView(withId(R.id.btnGenerar)).perform(scrollTo(), click());
            onView(withId(R.id.tvFeedback)).check(matches(withText(R.string.quantity_invalid)));
            onView(withId(R.id.etCantidad)).perform(scrollTo(), replaceText("2"), closeSoftKeyboard());
            onView(withId(R.id.tvTotalPedido)).check(matches(withText("S/ 36.00")));
            onView(withId(R.id.btnGenerar)).perform(scrollTo(), click());
            long order = scalar("SELECT MAX(id) FROM pedidos");
            createdOrders.add(order);
            onView(withId(R.id.tvFeedback)).check(matches(withText(containsString("Pedido " + order + " registrado"))));
            onView(withId(R.id.btnCerrar)).perform(scrollTo(), click());
            onView(withId(R.id.btnPedido)).perform(scrollTo(), click());
            assertEquals(1, scalar("SELECT COUNT(*) FROM pedidos WHERE id = ?", String.valueOf(order)));
            onView(withId(R.id.btnCerrar)).perform(scrollTo(), click());
            onView(withId(R.id.btnCaja)).perform(scrollTo(), click());
            onView(withId(R.id.etPedidoId)).perform(scrollTo(), replaceText(String.valueOf(order)), closeSoftKeyboard());
            onView(withId(R.id.btnBuscar)).perform(scrollTo(), click());
            onView(withId(R.id.etTotal)).check(matches(withText("S/ 36.00")));
            onView(withId(R.id.etPedidoId)).perform(scrollTo(), replaceText("999999999"), closeSoftKeyboard());
            onView(withId(R.id.btnPagar)).check(matches(not(isEnabled())));
            onView(withId(R.id.btnBuscar)).perform(scrollTo(), click());
            onView(withId(R.id.tvEmpty)).check(matches(withText(R.string.cash_not_found)));
            onView(withId(R.id.etPedidoId)).perform(scrollTo(), replaceText(String.valueOf(order)), closeSoftKeyboard());
            onView(withId(R.id.btnBuscar)).perform(scrollTo(), click());
            onView(withId(R.id.btnPagar)).perform(scrollTo(), click());
            onView(withId(R.id.tvEstado)).check(matches(withText(R.string.paid)));
            onView(withId(R.id.btnPagar)).check(matches(not(isEnabled())));
            assertEquals(1, scalar("SELECT COUNT(*) FROM pagos WHERE pedidoId = ?", String.valueOf(order)));
            onView(withId(R.id.btnCierreCaja)).perform(scrollTo(), click());
            long previousCloses = scalar("SELECT COUNT(*) FROM cierres");
            onView(withId(R.id.btnCerrarCaja)).perform(scrollTo(), click());
            onView(withText(R.string.cancel_close)).perform(click());
            assertEquals(previousCloses, scalar("SELECT COUNT(*) FROM cierres"));
            Locale.setDefault(new Locale("es", "ES"));
            onView(withId(R.id.btnCerrarCaja)).perform(scrollTo(), click());
            onView(withText(R.string.confirm_close)).perform(click());
            onView(withId(R.id.btnLogIn)).check(matches(isDisplayed()));
            assertEquals(previousCloses + 1, scalar("SELECT COUNT(*) FROM cierres"));
            createdClose = scalar("SELECT MAX(id) FROM cierres");
            assertEquals(1, scalar("SELECT COUNT(*) FROM pedidos WHERE id = ?", String.valueOf(order)));
            assertEquals("", context.getSharedPreferences("walok_prefs", Context.MODE_PRIVATE).getString("usuario", ""));
        }
    }

    @After public void cleanOnlyTestRecords() {
        Locale.setDefault(originalLocale);
        for (long id : createdOrders) {
            db.delete("pagos", "pedidoId = ?", new String[]{String.valueOf(id)});
            db.delete("pedidos", "id = ?", new String[]{String.valueOf(id)});
        }
        if (createdClose > 0) db.delete("cierres", "id = ?", new String[]{String.valueOf(createdClose)});
        session(originalUser);
        helper.close();
    }
}
