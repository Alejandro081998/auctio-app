package com.example.clase4;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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

public class AdminActivity extends AppCompatActivity {

    private TextView txtMensajeAdmin;
    private EditText edtAdminUsuarioId;
    private EditText edtAdminMedioPagoId;
    private EditText edtAdminProductoId;
    private EditText edtAdminMotivoRechazo;
    private EditText edtAdminSubastaId;
    private EditText edtAdminPrecioBase;
    private EditText edtAdminComision;
    private EditText edtAdminItemId;
    private EditText edtAdminClienteMulta;
    private EditText edtAdminMontoMulta;
    private Spinner spAdminCategoria;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        txtMensajeAdmin = findViewById(R.id.txtMensajeAdmin);
        edtAdminUsuarioId = findViewById(R.id.edtAdminUsuarioId);
        edtAdminMedioPagoId = findViewById(R.id.edtAdminMedioPagoId);
        edtAdminProductoId = findViewById(R.id.edtAdminProductoId);
        edtAdminMotivoRechazo = findViewById(R.id.edtAdminMotivoRechazo);
        edtAdminSubastaId = findViewById(R.id.edtAdminSubastaId);
        edtAdminPrecioBase = findViewById(R.id.edtAdminPrecioBase);
        edtAdminComision = findViewById(R.id.edtAdminComision);
        edtAdminItemId = findViewById(R.id.edtAdminItemId);
        edtAdminClienteMulta = findViewById(R.id.edtAdminClienteMulta);
        edtAdminMontoMulta = findViewById(R.id.edtAdminMontoMulta);
        spAdminCategoria = findViewById(R.id.spAdminCategoria);

        spAdminCategoria.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"comun", "especial", "plata", "oro", "platino"}
        ));

        findViewById(R.id.btnAdminAprobarUsuario).setOnClickListener(v -> verificarUsuario("si"));
        findViewById(R.id.btnAdminRechazarUsuario).setOnClickListener(v -> verificarUsuario("no"));
        findViewById(R.id.btnAdminVerificarMedio).setOnClickListener(v -> verificarMedioPago());
        findViewById(R.id.btnAdminAceptarProducto).setOnClickListener(v -> revisarProducto("aceptado"));
        findViewById(R.id.btnAdminRechazarProducto).setOnClickListener(v -> revisarProducto("rechazado"));
        findViewById(R.id.btnAdminAsignarProducto).setOnClickListener(v -> asignarProducto());
        findViewById(R.id.btnAdminCerrarItem).setOnClickListener(v -> cerrarItem());
        findViewById(R.id.btnAdminCrearMulta).setOnClickListener(v -> crearMulta());
        findViewById(R.id.btnVolverAdmin).setOnClickListener(v -> finish());
    }

    private void verificarUsuario(String admitido) {
        String userId = edtAdminUsuarioId.getText().toString().trim();
        if (userId.isEmpty()) {
            mostrarMensaje("Ingresá ID de usuario.");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("admitido", admitido);
            body.put("categoria", spAdminCategoria.getSelectedItem().toString());
        } catch (Exception ignored) {
        }

        enviarJson("/api/admin/users/" + userId + "/verification", "PATCH", body);
    }

    private void verificarMedioPago() {
        String medioPagoId = edtAdminMedioPagoId.getText().toString().trim();
        if (medioPagoId.isEmpty()) {
            mostrarMensaje("Ingresá ID de medio de pago.");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("verificado", "si");
        } catch (Exception ignored) {
        }

        enviarJson("/api/admin/payment-methods/" + medioPagoId + "/verification", "PATCH", body);
    }

    private void revisarProducto(String estado) {
        String productId = edtAdminProductoId.getText().toString().trim();
        if (productId.isEmpty()) {
            mostrarMensaje("Ingresá ID de producto.");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("estadoAprobacion", estado);
            body.put("motivoRechazo", edtAdminMotivoRechazo.getText().toString().trim());
            body.put("ubicacionDeposito", "Depósito asignado desde panel interno");
        } catch (Exception ignored) {
        }

        enviarJson("/api/admin/products/" + productId + "/review", "PATCH", body);
    }

    private void asignarProducto() {
        String auctionId = edtAdminSubastaId.getText().toString().trim();
        String productId = edtAdminProductoId.getText().toString().trim();
        String precioBase = edtAdminPrecioBase.getText().toString().trim();
        String comision = edtAdminComision.getText().toString().trim();

        if (auctionId.isEmpty() || productId.isEmpty() || precioBase.isEmpty() || comision.isEmpty()) {
            mostrarMensaje("Ingresá subasta, producto, precio base y comisión.");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("productId", Integer.parseInt(productId));
            body.put("precioBase", Double.parseDouble(precioBase));
            body.put("comision", Double.parseDouble(comision));
        } catch (Exception e) {
            mostrarMensaje("Precio base y comisión deben ser numéricos.");
            return;
        }

        enviarJson("/api/admin/auctions/" + auctionId + "/items", "POST", body);
    }

    private void cerrarItem() {
        String auctionId = edtAdminSubastaId.getText().toString().trim();
        String itemId = edtAdminItemId.getText().toString().trim();

        if (auctionId.isEmpty() || itemId.isEmpty()) {
            mostrarMensaje("Ingresá subasta e item.");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("costoEnvio", 0);
            body.put("retiroPersonal", "no");
        } catch (Exception ignored) {
        }

        enviarJson("/api/admin/auctions/" + auctionId + "/items/" + itemId + "/close", "POST", body);
    }

    private void crearMulta() {
        String clienteId = edtAdminClienteMulta.getText().toString().trim();
        String subastaId = edtAdminSubastaId.getText().toString().trim();
        String monto = edtAdminMontoMulta.getText().toString().trim();

        if (clienteId.isEmpty() || subastaId.isEmpty() || monto.isEmpty()) {
            mostrarMensaje("Ingresá cliente, subasta y monto de multa.");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("clienteId", Integer.parseInt(clienteId));
            body.put("subastaId", Integer.parseInt(subastaId));
            body.put("monto", Double.parseDouble(monto));
        } catch (Exception e) {
            mostrarMensaje("Cliente, subasta y monto deben ser numéricos.");
            return;
        }

        enviarJson("/api/admin/fines", "POST", body);
    }

    private void enviarJson(String path, String method, JSONObject body) {
        mostrarMensaje("Enviando operación interna...");

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + path);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(method.equals("PATCH") ? "POST" : method);
                if (method.equals("PATCH")) {
                    connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
                }
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int statusCode = connection.getResponseCode();
                InputStream inputStream = statusCode >= 200 && statusCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                String respuesta = leerRespuesta(inputStream);
                JSONObject json = new JSONObject(respuesta);

                mainHandler.post(() -> mostrarMensaje(json.optString(
                        statusCode >= 200 && statusCode < 300 ? "mensaje" : "error",
                        statusCode >= 200 && statusCode < 300
                                ? "Operación realizada."
                                : "No se pudo completar la operación."
                )));
            } catch (Exception e) {
                mainHandler.post(() -> mostrarMensaje("No se pudo conectar con el servidor."));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void mostrarMensaje(String mensaje) {
        txtMensajeAdmin.setText(mensaje);
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
