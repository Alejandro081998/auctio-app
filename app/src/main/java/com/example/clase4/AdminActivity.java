package com.example.clase4;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
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
    private TextView txtAdminPendientes;
    private TextView txtAdminUsuarioSeleccionado;
    private TextView txtAdminMedioSeleccionado;
    private LinearLayout contenedorUsuariosPendientes;
    private LinearLayout contenedorMediosPendientes;
    private ImageView imgAdminDniFrente;
    private ImageView imgAdminDniDorso;
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
    private String token;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        txtMensajeAdmin = findViewById(R.id.txtMensajeAdmin);
        txtAdminPendientes = findViewById(R.id.txtAdminPendientes);
        txtAdminUsuarioSeleccionado = findViewById(R.id.txtAdminUsuarioSeleccionado);
        txtAdminMedioSeleccionado = findViewById(R.id.txtAdminMedioSeleccionado);
        contenedorUsuariosPendientes = findViewById(R.id.contenedorUsuariosPendientes);
        contenedorMediosPendientes = findViewById(R.id.contenedorMediosPendientes);
        imgAdminDniFrente = findViewById(R.id.imgAdminDniFrente);
        imgAdminDniDorso = findViewById(R.id.imgAdminDniDorso);
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
        SharedPreferences preferences = getSharedPreferences("sesion", MODE_PRIVATE);
        token = preferences.getString("token", "");

        if (token == null || token.trim().isEmpty()) {
            mostrarError("Falta token Bearer de empleado.");
            txtAdminPendientes.setText("No se pueden cargar pendientes. Ingresa nuevamente con el admin 20000111 / 1234.");
        }

        spAdminCategoria.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"comun", "especial", "plata", "oro", "platino"}
        ));

        findViewById(R.id.btnAdminAprobarUsuario).setOnClickListener(v -> verificarUsuario("si"));
        findViewById(R.id.btnAdminRechazarUsuario).setOnClickListener(v -> verificarUsuario("no"));
        findViewById(R.id.btnAdminActualizarPendientes).setOnClickListener(v -> cargarPendientes());
        findViewById(R.id.btnAdminVerificarMedio).setOnClickListener(v -> verificarMedioPago());
        findViewById(R.id.btnAdminRechazarMedio).setOnClickListener(v -> rechazarMedioPago());
        findViewById(R.id.btnAdminAceptarProducto).setOnClickListener(v -> revisarProducto("aceptado"));
        findViewById(R.id.btnAdminRechazarProducto).setOnClickListener(v -> revisarProducto("rechazado"));
        findViewById(R.id.btnAdminAsignarProducto).setOnClickListener(v -> asignarProducto());
        findViewById(R.id.btnAdminCerrarItem).setOnClickListener(v -> cerrarItem());
        findViewById(R.id.btnAdminCrearMulta).setOnClickListener(v -> crearMulta());
        findViewById(R.id.btnVolverAdmin).setOnClickListener(v -> cerrarSesionAdmin());

        cargarPendientes();
    }

    private void cargarPendientes() {
        if (token == null || token.trim().isEmpty()) {
            txtAdminPendientes.setText("No se pueden cargar pendientes: falta token de admin. Ingresa nuevamente con 20000111 / 1234.");
            return;
        }

        txtAdminPendientes.setText("Cargando pendientes internos...");

        executor.execute(() -> {
            try {
                JSONArray usuarios = leerArrayAutorizado("/api/admin/users/pending");
                JSONArray medios = leerArrayAutorizado("/api/admin/payment-methods/pending");
                JSONArray productos = leerArrayAutorizado("/api/admin/products/pending");

                StringBuilder builder = new StringBuilder();
                builder.append("Usuarios pendientes\n");
                agregarResumen(builder, usuarios, "id", "documento", "categoria");
                builder.append("\nMedios de pago pendientes\n");
                agregarResumen(builder, medios, "id", "clienteNombre", "tipo");
                builder.append("\nConsignaciones pendientes\n");
                agregarResumen(builder, productos, "id", "duenioNombre", "descripcionCatalogo");

                mainHandler.post(() -> {
                    txtAdminPendientes.setText(builder.toString());
                    mostrarUsuariosPendientes(usuarios);
                    mostrarMediosPendientes(medios);
                });
            } catch (Exception e) {
                mainHandler.post(() -> txtAdminPendientes.setText(
                        "No se pudieron cargar los pendientes internos.\n" + e.getMessage()
                ));
            }
        });
    }

    private void agregarResumen(StringBuilder builder, JSONArray array, String idKey, String primaryKey, String secondaryKey) throws Exception {
        if (array.length() == 0) {
            builder.append("Sin pendientes.\n");
            return;
        }

        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            builder
                    .append("#")
                    .append(item.optInt(idKey, 0))
                    .append(" - ")
                    .append(item.optString(primaryKey, "-"))
                    .append(" - ")
                    .append(item.optString(secondaryKey, "-"))
                    .append("\n");
        }
    }

    private void mostrarUsuariosPendientes(JSONArray usuarios) {
        contenedorUsuariosPendientes.removeAllViews();

        if (usuarios.length() == 0) {
            TextView empty = crearTexto("No hay usuarios pendientes para revisar.", "#64748B", 14, false);
            contenedorUsuariosPendientes.addView(empty);
            return;
        }

        for (int i = 0; i < usuarios.length(); i++) {
            try {
                JSONObject usuario = usuarios.getJSONObject(i);
                contenedorUsuariosPendientes.addView(crearCardUsuarioPendiente(usuario));
            } catch (Exception ignored) {
            }
        }
    }

    private View crearCardUsuarioPendiente(JSONObject usuario) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundResource(R.drawable.bg_metric_box);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);

        String documento = usuario.optString("documento", "-");
        String nombre = usuario.optString("nombre", "-") + " " + usuario.optString("apellido", "-");
        String categoria = usuario.optString("categoria", "-");
        String admitido = usuario.optString("admitido", "-");

        TextView titulo = crearTexto("DNI " + documento, "#071827", 17, true);
        TextView detalle = crearTexto(nombre + "\nCategoria: " + categoria + " · Admitido: " + admitido, "#475569", 13, false);

        card.addView(titulo);
        card.addView(detalle);
        card.setOnClickListener(v -> seleccionarUsuarioPendiente(usuario));

        return card;
    }

    private void seleccionarUsuarioPendiente(JSONObject usuario) {
        int id = usuario.optInt("id", 0);
        edtAdminUsuarioId.setText(String.valueOf(id));

        txtAdminUsuarioSeleccionado.setText(
                "Usuario seleccionado\n" +
                        "ID: " + id + "\n" +
                        "Documento: " + usuario.optString("documento", "-") + "\n" +
                        "Nombre: " + usuario.optString("nombre", "-") + " " + usuario.optString("apellido", "-") + "\n" +
                        "Email: " + usuario.optString("email", "-") + "\n" +
                        "Direccion: " + usuario.optString("direccion", "-") + "\n" +
                        "Categoria actual: " + usuario.optString("categoria", "-") + "\n" +
                        "Admitido: " + usuario.optString("admitido", "-")
        );

        cargarImagenBase64(usuario.optString("fotoDniFrenteBase64", ""), imgAdminDniFrente);
        cargarImagenBase64(usuario.optString("fotoDniDorsoBase64", ""), imgAdminDniDorso);
        mostrarInfo("Revisando DNI " + usuario.optString("documento", "-"));
    }

    private void mostrarMediosPendientes(JSONArray medios) {
        contenedorMediosPendientes.removeAllViews();

        if (medios.length() == 0) {
            contenedorMediosPendientes.addView(crearTexto("No hay medios de pago pendientes.", "#64748B", 14, false));
            return;
        }

        for (int i = 0; i < medios.length(); i++) {
            try {
                JSONObject medio = medios.getJSONObject(i);
                contenedorMediosPendientes.addView(crearCardMedioPendiente(medio));
            } catch (Exception ignored) {
            }
        }
    }

    private View crearCardMedioPendiente(JSONObject medio) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundResource(R.drawable.bg_metric_box);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);

        String id = String.valueOf(medio.optInt("id", 0));
        String cliente = medio.optString("clienteNombre", "-");
        String tipo = medio.optString("tipo", "-");
        String entidad = medio.optString("entidad", "-");

        card.addView(crearTexto("Medio #" + id + " · " + formatearTipo(tipo), "#071827", 17, true));
        card.addView(crearTexto(cliente + "\n" + entidad, "#475569", 13, false));
        card.setOnClickListener(v -> seleccionarMedioPendiente(medio));

        return card;
    }

    private void seleccionarMedioPendiente(JSONObject medio) {
        int id = medio.optInt("id", 0);
        edtAdminMedioPagoId.setText(String.valueOf(id));

        String detalle =
                "Medio seleccionado\n" +
                        "ID: " + id + "\n" +
                        "Cliente: " + medio.optString("clienteNombre", "-") + "\n" +
                        "Tipo: " + formatearTipo(medio.optString("tipo", "-")) + "\n" +
                        "Entidad: " + medio.optString("entidad", "-") + "\n" +
                        "Datos: " + medio.optString("numeroReferencia", "-") + "\n" +
                        "Moneda: " + medio.optString("moneda", "-") + "\n" +
                        "Extranjera: " + medio.optString("esExtranjera", "-");

        if ("cheque_certificado".equals(medio.optString("tipo", ""))) {
            detalle += "\nMonto cheque: " + medio.optDouble("montoCheque", 0);
            detalle += "\nMonto disponible: " + medio.optDouble("montoDisponible", 0);
        }

        txtAdminMedioSeleccionado.setText(detalle);
        mostrarInfo("Revisando medio de pago #" + id);
    }

    private String formatearTipo(String tipo) {
        if ("tarjeta_credito".equals(tipo)) return "Tarjeta de credito";
        if ("cuenta_bancaria".equals(tipo)) return "Cuenta bancaria";
        if ("cheque_certificado".equals(tipo)) return "Cheque certificado";
        return tipo;
    }

    private void cargarImagenBase64(String base64, ImageView imageView) {
        try {
            if (base64 == null || base64.trim().isEmpty() || "null".equals(base64)) {
                imageView.setImageResource(R.drawable.logo);
                return;
            }

            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap == null) {
                imageView.setImageResource(R.drawable.logo);
                return;
            }

            imageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            imageView.setImageResource(R.drawable.logo);
        }
    }

    private TextView crearTexto(String text, String color, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.parseColor(color));
        view.setTextSize(size);
        view.setLineSpacing(dp(3), 1.0f);
        if (bold) {
            view.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        return view;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private JSONArray leerArrayAutorizado(String path) throws Exception {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(ApiConfig.BASE_URL + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + token);

            int statusCode = connection.getResponseCode();
            InputStream inputStream = statusCode >= 200 && statusCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String respuesta = leerRespuesta(inputStream);
            if (statusCode >= 200 && statusCode < 300) {
                return new JSONArray(respuesta);
            }

            String detalle;
            try {
                JSONObject error = new JSONObject(respuesta);
                detalle = error.optString("error", respuesta);
            } catch (Exception e) {
                detalle = respuesta;
            }
            throw new IllegalStateException(path + " -> HTTP " + statusCode + ": " + detalle);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void verificarUsuario(String admitido) {
        String userId = edtAdminUsuarioId.getText().toString().trim();
        if (userId.isEmpty()) {
            mostrarError("Ingresá ID de usuario.");
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
            mostrarError("Ingresá ID de medio de pago.");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("verificado", "si");
        } catch (Exception ignored) {
        }

        enviarJson("/api/admin/payment-methods/" + medioPagoId + "/verification", "PATCH", body);
    }

    private void rechazarMedioPago() {
        String medioPagoId = edtAdminMedioPagoId.getText().toString().trim();
        if (medioPagoId.isEmpty()) {
            mostrarError("Selecciona o ingresa un ID de medio de pago.");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("verificado", "no");
            body.put("rechazado", "si");
            body.put("motivoRechazo", "Rechazado por validacion administrativa");
        } catch (Exception ignored) {
        }

        enviarJson("/api/admin/payment-methods/" + medioPagoId + "/verification", "PATCH", body);
    }

    private void revisarProducto(String estado) {
        String productId = edtAdminProductoId.getText().toString().trim();
        if (productId.isEmpty()) {
            mostrarError("Ingresá ID de producto.");
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
            mostrarError("Ingresá subasta, producto, precio base y comisión.");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("productId", Integer.parseInt(productId));
            body.put("precioBase", Double.parseDouble(precioBase));
            body.put("comision", Double.parseDouble(comision));
        } catch (Exception e) {
            mostrarError("Precio base y comisión deben ser numéricos.");
            return;
        }

        enviarJson("/api/admin/auctions/" + auctionId + "/items", "POST", body);
    }

    private void cerrarItem() {
        String auctionId = edtAdminSubastaId.getText().toString().trim();
        String itemId = edtAdminItemId.getText().toString().trim();

        if (auctionId.isEmpty() || itemId.isEmpty()) {
            mostrarError("Ingresá subasta e item.");
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
            mostrarError("Ingresá cliente, subasta y monto de multa.");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("clienteId", Integer.parseInt(clienteId));
            body.put("subastaId", Integer.parseInt(subastaId));
            body.put("monto", Double.parseDouble(monto));
        } catch (Exception e) {
            mostrarError("Cliente, subasta y monto deben ser numéricos.");
            return;
        }

        enviarJson("/api/admin/fines", "POST", body);
    }

    private void enviarJson(String path, String method, JSONObject body) {
        if (token == null || token.trim().isEmpty()) {
            mostrarError("Falta token de admin. Ingresa nuevamente con 20000111 / 1234.");
            return;
        }

        mostrarInfo("Enviando operacion interna...");

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
                connection.setRequestProperty("Authorization", "Bearer " + token);
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
                boolean ok = statusCode >= 200 && statusCode < 300;
                String mensaje = json.optString(
                        ok ? "mensaje" : "error",
                        ok ? "Operacion realizada." : "No se pudo completar la operacion."
                );

                mainHandler.post(() -> {
                    if (ok) {
                        mostrarOk(mensaje);
                        FeedbackDialog.ok(AdminActivity.this, mensaje);
                        cargarPendientes();
                    } else {
                        mostrarError(mensaje);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    String mensaje = "No se pudo conectar con el servidor: " + e.getMessage();
                    mostrarError(mensaje);
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void cerrarSesionAdmin() {
        SharedPreferences preferences = getSharedPreferences("sesion", MODE_PRIVATE);
        preferences.edit().clear().apply();

        Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void mostrarMensaje(String mensaje) {
        mostrarInfo(mensaje);
    }

    private void mostrarInfo(String mensaje) {
        txtMensajeAdmin.setTextColor(Color.parseColor("#D7E3EF"));
        txtMensajeAdmin.setText(mensaje);
    }

    private void mostrarOk(String mensaje) {
        txtMensajeAdmin.setTextColor(Color.parseColor("#BBF7D0"));
        txtMensajeAdmin.setText(mensaje);
    }

    private void mostrarError(String mensaje) {
        txtMensajeAdmin.setTextColor(Color.parseColor("#FECACA"));
        txtMensajeAdmin.setText(mensaje);
        FeedbackDialog.error(this, mensaje);
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
