package com.example.clase4;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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

public class PurchasesActivity extends AppCompatActivity {

    private TextView txtMensajeCompras;
    private Button btnActualizarCompras;
    private Button btnVolverCompras;
    private LinearLayout contenedorCompras;
    private int userId;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchases);

        txtMensajeCompras = findViewById(R.id.txtMensajeCompras);
        btnActualizarCompras = findViewById(R.id.btnActualizarCompras);
        btnVolverCompras = findViewById(R.id.btnVolverCompras);
        contenedorCompras = findViewById(R.id.contenedorCompras);

        SharedPreferences preferences = getSharedPreferences("sesion", MODE_PRIVATE);
        userId = preferences.getInt("userId", 0);

        btnActualizarCompras.setOnClickListener(v -> cargarCompras());
        btnVolverCompras.setOnClickListener(v -> finish());

        cargarCompras();
    }

    private void cargarCompras() {
        txtMensajeCompras.setText("Cargando compras...");
        contenedorCompras.removeAllViews();

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/clients/" + userId + "/purchases");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");

                int statusCode = connection.getResponseCode();
                InputStream inputStream = statusCode >= 200 && statusCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                String respuesta = leerRespuesta(inputStream);

                if (statusCode == 200) {
                    JSONArray compras = new JSONArray(respuesta);
                    mainHandler.post(() -> mostrarCompras(compras));
                } else {
                    JSONObject errorJson = new JSONObject(respuesta);
                    mainHandler.post(() -> txtMensajeCompras.setText(
                            errorJson.optString("error", "Error al cargar compras")
                    ));
                }
            } catch (Exception e) {
                mainHandler.post(() -> txtMensajeCompras.setText("No se pudo conectar con el servidor."));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void mostrarCompras(JSONArray compras) {
        contenedorCompras.removeAllViews();

        if (compras.length() == 0) {
            txtMensajeCompras.setText("No tenés compras adjudicadas.");
            return;
        }

        txtMensajeCompras.setText("Compras encontradas: " + compras.length());

        try {
            for (int i = 0; i < compras.length(); i++) {
                contenedorCompras.addView(crearCardCompra(compras.getJSONObject(i)));
            }
        } catch (Exception e) {
            txtMensajeCompras.setText("Error mostrando compras.");
        }
    }

    private View crearCardCompra(JSONObject compra) {
        int ventaId = compra.optInt("ventaId", 0);
        String articulo = compra.optString("descripcionCatalogo", "-");
        String estadoPago = compra.optString("estadoPago", "-");
        double importe = compra.optDouble("importe", 0);
        double comision = compra.optDouble("comision", 0);
        double envio = compra.optDouble("costoEnvio", 0);
        double total = importe + comision + envio;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(28, 24, 28, 24);
        card.setBackgroundColor(Color.WHITE);
        card.setElevation(4);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 22);
        card.setLayoutParams(params);

        TextView titulo = new TextView(this);
        titulo.setText("Compra #" + ventaId);
        titulo.setTextSize(18);
        titulo.setTextColor(Color.parseColor("#0F172A"));
        titulo.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView detalle = new TextView(this);
        detalle.setText(
                "Artículo: " + articulo + "\n" +
                        "Puja: $" + importe + "\n" +
                        "Comisión: $" + comision + "\n" +
                        "Envío: $" + envio + "\n" +
                        "Total: $" + total + "\n" +
                        "Estado de pago: " + estadoPago
        );
        detalle.setTextSize(15);
        detalle.setTextColor(Color.parseColor("#475569"));
        detalle.setPadding(0, 12, 0, 12);

        Button btnPagar = new Button(this);
        btnPagar.setText(estadoPago.equals("pagado") ? "Compra pagada" : "Registrar pago");
        btnPagar.setEnabled(!estadoPago.equals("pagado"));
        btnPagar.setTextColor(Color.WHITE);
        btnPagar.setBackgroundColor(estadoPago.equals("pagado")
                ? Color.parseColor("#64748B")
                : Color.parseColor("#2563EB"));
        btnPagar.setOnClickListener(v -> pagarCompra(ventaId));

        card.addView(titulo);
        card.addView(detalle);
        card.addView(btnPagar);

        return card;
    }

    private void pagarCompra(int ventaId) {
        txtMensajeCompras.setText("Registrando pago...");

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/purchases/" + ventaId + "/pay");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                int statusCode = connection.getResponseCode();
                InputStream inputStream = statusCode >= 200 && statusCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                String respuesta = leerRespuesta(inputStream);
                JSONObject json = new JSONObject(respuesta);

                mainHandler.post(() -> {
                    txtMensajeCompras.setText(json.optString(
                            statusCode == 200 ? "mensaje" : "error",
                            statusCode == 200 ? "Pago registrado." : "No se pudo registrar el pago."
                    ));
                    cargarCompras();
                });
            } catch (Exception e) {
                mainHandler.post(() -> txtMensajeCompras.setText("No se pudo conectar con el servidor."));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
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
}
