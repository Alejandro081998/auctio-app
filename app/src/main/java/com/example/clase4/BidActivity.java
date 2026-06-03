package com.example.clase4;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BidActivity extends AppCompatActivity {

    private TextView txtTituloPuja;
    private TextView txtDatosItem;
    private TextView txtRangoPuja;
    private TextView txtMensajePuja;
    private EditText edtImportePuja;
    private Button btnEnviarPuja;
    private Button btnVolverDetalle;

    private int userId;
    private int auctionId;
    private int itemId;

    private String descripcion;
    private String categoriaSubasta;
    private double precioBase;
    private double mejorOferta;
    private double pujaMinima;
    private Double pujaMaxima;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bid);

        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#F3F0E8"));
        getWindow().setNavigationBarColor(android.graphics.Color.parseColor("#F3F0E8"));

        BottomNavHelper.configurar(this);

        txtTituloPuja = findViewById(R.id.txtTituloPuja);
        txtDatosItem = findViewById(R.id.txtDatosItem);
        txtRangoPuja = findViewById(R.id.txtRangoPuja);
        txtMensajePuja = findViewById(R.id.txtMensajePuja);
        edtImportePuja = findViewById(R.id.edtImportePuja);
        btnEnviarPuja = findViewById(R.id.btnEnviarPuja);
        btnVolverDetalle = findViewById(R.id.btnVolverDetalle);

        SharedPreferences preferences = getSharedPreferences("sesion", MODE_PRIVATE);
        userId = preferences.getInt("userId", 0);

        auctionId = getIntent().getIntExtra("auctionId", 0);
        itemId = getIntent().getIntExtra("itemId", 0);
        descripcion = getIntent().getStringExtra("descripcion");
        categoriaSubasta = getIntent().getStringExtra("categoria");
        if (categoriaSubasta == null) {
            categoriaSubasta = "";
        }
        precioBase = getIntent().getDoubleExtra("precioBase", 0);
        mejorOferta = getIntent().getDoubleExtra("mejorOferta", 0);

        mostrarDatosItem();

        btnEnviarPuja.setOnClickListener(v -> validarYEnviarPuja());

        btnVolverDetalle.setOnClickListener(v -> finish());
    }

    private void mostrarDatosItem() {
        double valorReferencia = mejorOferta > 0 ? mejorOferta : precioBase;
        boolean categoriaPremium =
                "oro".equals(categoriaSubasta) || "platino".equals(categoriaSubasta);

        pujaMinima = categoriaPremium
                ? valorReferencia + 0.01
                : valorReferencia + (precioBase * 0.01);

        pujaMaxima = categoriaPremium
                ? null
                : valorReferencia + (precioBase * 0.20);

        txtTituloPuja.setText("Pujar por ítem #" + itemId);

        txtDatosItem.setText(
                "Artículo\n" + descripcion + "\n\n" +
                        "Precio base\n$" + precioBase + "\n\n" +
                        "Mejor oferta actual\n$" + mejorOferta + "\n\n" +
                        "Categoría de subasta\n" + categoriaSubasta
        );

        if (pujaMaxima == null) {
            txtRangoPuja.setText(
                    "Puja mínima: $" + String.format("%.2f", pujaMinima) + "\n" +
                            "Puja máxima: sin límite para categoría oro/platino"
            );
        } else {
            txtRangoPuja.setText(
                    "Puja mínima: $" + String.format("%.2f", pujaMinima) + "\n" +
                            "Puja máxima: $" + String.format("%.2f", pujaMaxima)
            );
        }

        edtImportePuja.setHint("Mínimo: $" + String.format("%.2f", pujaMinima));
    }

    private void validarYEnviarPuja() {
        String importeTexto = edtImportePuja.getText().toString().trim();

        if (importeTexto.isEmpty()) {
            txtMensajePuja.setText("Ingresá un importe para pujar.");
            return;
        }

        double importe;

        try {
            importe = Double.parseDouble(importeTexto);
        } catch (Exception e) {
            txtMensajePuja.setText("El importe ingresado no es válido.");
            return;
        }

        if (importe < pujaMinima) {
            txtMensajePuja.setText("La puja debe ser al menos $" + String.format("%.2f", pujaMinima));
            return;
        }

        if (pujaMaxima != null && importe > pujaMaxima) {
            txtMensajePuja.setText("La puja no puede superar $" + String.format("%.2f", pujaMaxima));
            return;
        }

        txtMensajePuja.setText("");
        btnEnviarPuja.setEnabled(false);
        btnEnviarPuja.setText("Enviando...");

        enviarPuja(importe);
    }

    private void enviarPuja(double importe) {
        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/bids");
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("clienteId", userId);
                body.put("subastaId", auctionId);
                body.put("itemId", itemId);
                body.put("importe", importe);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int statusCode = connection.getResponseCode();

                InputStream inputStream;

                if (statusCode >= 200 && statusCode < 300) {
                    inputStream = connection.getInputStream();
                } else {
                    inputStream = connection.getErrorStream();
                }

                String respuesta = leerRespuesta(inputStream);
                JSONObject json = new JSONObject(respuesta);

                if (statusCode == 201 || statusCode == 200) {
                    String mensaje = json.optString("mensaje", "Puja registrada correctamente");

                    mainHandler.post(() -> {
                        btnEnviarPuja.setEnabled(true);
                        btnEnviarPuja.setText("Enviar puja");
                        txtMensajePuja.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        txtMensajePuja.setText(mensaje);

                        edtImportePuja.setText("");

                        // Volvemos al detalle luego de un pequeño delay
                        new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), 1200);
                    });

                } else {
                    String error = json.optString("error", "No se pudo registrar la puja");

                    mainHandler.post(() -> {
                        btnEnviarPuja.setEnabled(true);
                        btnEnviarPuja.setText("Enviar puja");
                        txtMensajePuja.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        txtMensajePuja.setText(error);
                    });
                }

            } catch (Exception e) {
                mainHandler.post(() -> {
                    btnEnviarPuja.setEnabled(true);
                    btnEnviarPuja.setText("Enviar puja");
                    txtMensajePuja.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    txtMensajePuja.setText("No se pudo conectar con el servidor.");
                });
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
