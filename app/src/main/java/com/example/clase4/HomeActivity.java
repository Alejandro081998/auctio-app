package com.example.clase4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.Intent;

public class HomeActivity extends AppCompatActivity {

    private TextView txtBienvenida;
    private TextView txtCategoria;
    private TextView txtMensajeHome;
    private Button btnActualizarSubastas;
    private LinearLayout contenedorSubastas;

    private Button btnMediosPago;
    private Button btnSolicitarSubasta;
    private Button btnHistorial;
    private Button btnPerfil;
    private Button btnNotificaciones;
    private Button btnMultas;
    private Button btnCompras;
    private Button btnAdmin;

    /*
     IMPORTANTE:
     Usá la misma IP que pusiste en LoginActivity.java.
    */

    private int userId;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#F3F0E8"));
        getWindow().setNavigationBarColor(android.graphics.Color.parseColor("#F3F0E8"));

        BottomNavHelper.configurar(this);

        txtBienvenida = findViewById(R.id.txtBienvenida);
        txtCategoria = findViewById(R.id.txtCategoria);
        txtMensajeHome = findViewById(R.id.txtMensajeHome);
        btnActualizarSubastas = findViewById(R.id.btnActualizarSubastas);
        btnMediosPago = findViewById(R.id.btnMediosPago);
        btnSolicitarSubasta = findViewById(R.id.btnSolicitarSubasta);
        btnHistorial = findViewById(R.id.btnHistorial);
        btnPerfil = findViewById(R.id.btnPerfil);
        btnNotificaciones = findViewById(R.id.btnNotificaciones);
        btnMultas = findViewById(R.id.btnMultas);
        btnCompras = findViewById(R.id.btnCompras);
        btnAdmin = findViewById(R.id.btnAdmin);
        contenedorSubastas = findViewById(R.id.contenedorSubastas);

        SharedPreferences preferences = getSharedPreferences("sesion", MODE_PRIVATE);

        userId = preferences.getInt("userId", 0);
        String nombre = preferences.getString("nombre", "");
        String apellido = preferences.getString("apellido", "");
        String categoria = preferences.getString("categoria", "");
        boolean esAdmin = preferences.getBoolean("esAdmin", false);

        txtBienvenida.setText("Bienvenido, " + nombre + " " + apellido);
        txtCategoria.setText("Categoría: " + categoria);

        btnAdmin.setVisibility(esAdmin ? View.VISIBLE : View.GONE);

        btnActualizarSubastas.setOnClickListener(v -> cargarSubastas());

        btnMediosPago.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, PaymentMethodsActivity.class);
            startActivity(intent);
        });

        btnSolicitarSubasta.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProductRequestActivity.class);
            startActivity(intent);
        });

        btnHistorial.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        btnPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        btnNotificaciones.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, NotificationsActivity.class);
            startActivity(intent);
        });

        btnMultas.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, FinesActivity.class);
            startActivity(intent);
        });

        btnCompras.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, PurchasesActivity.class);
            startActivity(intent);
        });

        btnAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AdminActivity.class);
            startActivity(intent);
        });

        cargarSubastas();
        if (getIntent().getBooleanExtra("goToAuctions", false)) {
            View tituloSubastas = findViewById(R.id.txtTituloSubastas);

            if (tituloSubastas != null) {
                tituloSubastas.postDelayed(() -> irASeccionSubastas(), 500);
            }
        }
    }

    private void cargarSubastas() {
        txtMensajeHome.setText("Cargando subastas...");
        contenedorSubastas.removeAllViews();

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/clients/" + userId + "/auctions");
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");

                int statusCode = connection.getResponseCode();

                InputStream inputStream;

                if (statusCode >= 200 && statusCode < 300) {
                    inputStream = connection.getInputStream();
                } else {
                    inputStream = connection.getErrorStream();
                }

                String respuesta = leerRespuesta(inputStream);

                if (statusCode == 200) {
                    JSONArray subastas = new JSONArray(respuesta);

                    mainHandler.post(() -> mostrarSubastas(subastas));
                } else {
                    JSONObject errorJson = new JSONObject(respuesta);
                    String error = errorJson.optString("error", "Error al cargar subastas");

                    mainHandler.post(() -> txtMensajeHome.setText(error));
                }

            } catch (Exception e) {
                mainHandler.post(() -> txtMensajeHome.setText("No se pudo conectar con el servidor."));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void mostrarSubastas(JSONArray subastas) {
        contenedorSubastas.removeAllViews();

        if (subastas.length() == 0) {
            txtMensajeHome.setText("No hay subastas disponibles.");
            return;
        }

        txtMensajeHome.setText("Subastas encontradas: " + subastas.length());

        try {
            for (int i = 0; i < subastas.length(); i++) {
                JSONObject subasta = subastas.getJSONObject(i);

                int id = subasta.getInt("id");
                String fecha = subasta.optString("fecha", "-");
                String hora = subasta.optString("hora", "-");
                String estado = subasta.optString("estado", "-");
                String ubicacion = subasta.optString("ubicacion", "-");
                String categoria = subasta.optString("categoria", "-");
                String moneda = subasta.optString("moneda", "-");
                boolean puedePujar = subasta.optBoolean("puedePujar", false);
                String motivoBloqueo = subasta.optString("motivoBloqueo", "");

                View card = crearCardSubasta(
                        id,
                        fecha,
                        hora,
                        estado,
                        ubicacion,
                        categoria,
                        moneda,
                        puedePujar,
                        motivoBloqueo
                );

                contenedorSubastas.addView(card);
            }
        } catch (Exception e) {
            txtMensajeHome.setText("Error mostrando subastas.");
        }
    }

    private View crearCardSubasta(
            int id,
            String fecha,
            String hora,
            String estado,
            String ubicacion,
            String categoria,
            String moneda,
            boolean puedePujar,
            String motivoBloqueo
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(18));
        card.setBackgroundResource(R.drawable.bg_card_premium);
        card.setElevation(dp(2));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(22));
        card.setLayoutParams(cardParams);

        // BLOQUE VISUAL SUPERIOR
        LinearLayout visual = new LinearLayout(this);
        visual.setOrientation(LinearLayout.VERTICAL);
        visual.setPadding(dp(18), dp(18), dp(18), dp(18));
        visual.setBackgroundResource(R.drawable.bg_visual_lot);

        LinearLayout.LayoutParams visualParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(170)
        );
        visual.setLayoutParams(visualParams);

        TextView chipLive = new TextView(this);
        chipLive.setText(estado.toUpperCase() + "  ·  SUBASTA #" + id);
        chipLive.setTextColor(Color.WHITE);
        chipLive.setTextSize(11);
        chipLive.setTypeface(null, android.graphics.Typeface.BOLD);
        chipLive.setLetterSpacing(0.08f);

        TextView titleVisual = new TextView(this);
        titleVisual.setText("Evento de subasta verificado");
        titleVisual.setTextColor(Color.WHITE);
        titleVisual.setTextSize(23);
        titleVisual.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, dp(52), 0, 0);
        titleVisual.setLayoutParams(titleParams);

        TextView subVisual = new TextView(this);
        subVisual.setText(ubicacion);
        subVisual.setTextColor(Color.parseColor("#E8EEF5"));
        subVisual.setTextSize(14);

        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subParams.setMargins(0, dp(6), 0, 0);
        subVisual.setLayoutParams(subParams);

        visual.addView(chipLive);
        visual.addView(titleVisual);
        visual.addView(subVisual);

        // CHIP CATEGORÍA
        TextView chipCategoria = new TextView(this);
        chipCategoria.setText(categoria.toUpperCase() + "  ·  " + moneda.toUpperCase());
        chipCategoria.setTextColor(Color.parseColor("#071827"));
        chipCategoria.setTextSize(11);
        chipCategoria.setTypeface(null, android.graphics.Typeface.BOLD);
        chipCategoria.setGravity(android.view.Gravity.CENTER);
        chipCategoria.setPadding(dp(14), dp(8), dp(14), dp(8));
        chipCategoria.setBackgroundResource(R.drawable.bg_gold_chip);

        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        chipParams.setMargins(0, dp(16), 0, 0);
        chipCategoria.setLayoutParams(chipParams);

        // DESCRIPCIÓN EDITORIAL
        TextView descripcion = new TextView(this);
        descripcion.setText("Evento de subasta verificado con activos seleccionados por especialistas. Accedé al catálogo para revisar lotes, precios base y disponibilidad de puja.");
        descripcion.setTextColor(Color.parseColor("#475569"));
        descripcion.setTextSize(14);
        descripcion.setLineSpacing(dp(3), 1.0f);

        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.setMargins(0, dp(14), 0, 0);
        descripcion.setLayoutParams(descParams);

        // MÉTRICAS
        LinearLayout metricsRow = new LinearLayout(this);
        metricsRow.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams metricsRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        metricsRowParams.setMargins(0, dp(16), 0, 0);
        metricsRow.setLayoutParams(metricsRowParams);

        TextView dateBox = new TextView(this);
        dateBox.setText("DATE\n" + fecha);
        dateBox.setTextColor(Color.parseColor("#071827"));
        dateBox.setTextSize(12);
        dateBox.setTypeface(null, android.graphics.Typeface.BOLD);
        dateBox.setPadding(dp(14), dp(12), dp(14), dp(12));
        dateBox.setBackgroundResource(R.drawable.bg_metric_box);

        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        dateParams.setMargins(0, 0, dp(6), 0);
        dateBox.setLayoutParams(dateParams);

        TextView timeBox = new TextView(this);
        timeBox.setText("START\n" + hora);
        timeBox.setTextColor(Color.parseColor("#071827"));
        timeBox.setTextSize(12);
        timeBox.setTypeface(null, android.graphics.Typeface.BOLD);
        timeBox.setPadding(dp(14), dp(12), dp(14), dp(12));
        timeBox.setBackgroundResource(R.drawable.bg_metric_box);

        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        timeParams.setMargins(dp(6), 0, 0, 0);
        timeBox.setLayoutParams(timeParams);

        metricsRow.addView(dateBox);
        metricsRow.addView(timeBox);

        // ESTADO DE ACCESO
        TextView acceso = new TextView(this);

        if (puedePujar) {
            acceso.setText("USUARIO HABILITADO PARA PUJAR");
            acceso.setTextColor(Color.parseColor("#166534"));
            acceso.setBackgroundResource(R.drawable.bg_success_chip);
        } else {
            acceso.setText("SOLO VISUALIZACIÓN · " + motivoBloqueo);
            acceso.setTextColor(Color.parseColor("#991B1B"));
            acceso.setBackgroundResource(R.drawable.bg_danger_chip);
        }

        acceso.setTextSize(11);
        acceso.setTypeface(null, android.graphics.Typeface.BOLD);
        acceso.setGravity(android.view.Gravity.CENTER);
        acceso.setPadding(dp(12), dp(8), dp(12), dp(8));

        LinearLayout.LayoutParams accesoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        accesoParams.setMargins(0, dp(16), 0, 0);
        acceso.setLayoutParams(accesoParams);

        Button btnVerDetalle = new Button(this);
        btnVerDetalle.setText(puedePujar ? "ENTRAR AL CATÁLOGO" : "VER CATÁLOGO");
        btnVerDetalle.setTextColor(Color.parseColor("#071827"));
        btnVerDetalle.setTextSize(12);
        btnVerDetalle.setTypeface(null, android.graphics.Typeface.BOLD);
        btnVerDetalle.setBackgroundResource(
                puedePujar ? R.drawable.bg_button_gold : R.drawable.bg_button_outline
        );

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        btnParams.setMargins(0, dp(16), 0, 0);
        btnVerDetalle.setLayoutParams(btnParams);

        btnVerDetalle.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AuctionDetailActivity.class);
            intent.putExtra("auctionId", id);
            intent.putExtra("puedePujar", puedePujar);
            intent.putExtra("categoria", categoria);
            startActivity(intent);
        });

        card.addView(visual);
        card.addView(chipCategoria);
        card.addView(descripcion);
        card.addView(metricsRow);
        card.addView(acceso);
        card.addView(btnVerDetalle);

        return card;
    }

    private String leerRespuesta(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder respuesta = new StringBuilder();
        String linea;

        while ((linea = reader.readLine()) != null) {
            respuesta.append(linea);
        }

        return respuesta.toString();
    }
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    public void irASeccionSubastas() {
        ScrollView scrollHome = findViewById(R.id.scrollHome);
        View tituloSubastas = findViewById(R.id.txtTituloSubastas);

        if (scrollHome != null && tituloSubastas != null) {
            scrollHome.post(() -> scrollHome.smoothScrollTo(0, tituloSubastas.getTop()));
        }
    }
}
