package com.example.clase4;

import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

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

public class AuctionDetailActivity extends AppCompatActivity {

    private TextView txtTituloDetalle;
    private TextView txtDatosSubasta;
    private TextView txtEstadoVivo;
    private TextView txtMensajeDetalle;
    private Button btnActualizarCatalogo;
    private Button btnVolverHome;
    private LinearLayout contenedorCatalogo;

    private int auctionId;
    private boolean puedePujar;
    private String categoriaSubasta;
    private volatile boolean escuchandoEventos;
    private HttpURLConnection conexionEventos;

    /*
     IMPORTANTE:
     Usá la misma IP que pusiste en LoginActivity.java y HomeActivity.java.
    */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ExecutorService eventExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auction_detail);

        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#F3F0E8"));
        getWindow().setNavigationBarColor(android.graphics.Color.parseColor("#F3F0E8"));

        BottomNavHelper.configurar(this);

        txtTituloDetalle = findViewById(R.id.txtTituloDetalle);
        txtDatosSubasta = findViewById(R.id.txtDatosSubasta);
        txtEstadoVivo = findViewById(R.id.txtEstadoVivo);
        txtMensajeDetalle = findViewById(R.id.txtMensajeDetalle);
        btnActualizarCatalogo = findViewById(R.id.btnActualizarCatalogo);
        btnVolverHome = findViewById(R.id.btnVolverHome);
        contenedorCatalogo = findViewById(R.id.contenedorCatalogo);

        auctionId = getIntent().getIntExtra("auctionId", 0);
        puedePujar = getIntent().getBooleanExtra("puedePujar", false);
        categoriaSubasta = getIntent().getStringExtra("categoria");
        if (categoriaSubasta == null) {
            categoriaSubasta = "";
        }

        txtTituloDetalle.setText("Detalle de subasta #" + auctionId);

        btnActualizarCatalogo.setOnClickListener(v -> {
            cargarDetalleSubasta();
            cargarCatalogo();
        });

        btnVolverHome.setOnClickListener(v -> finish());

        cargarDetalleSubasta();
        cargarCatalogo();
        escucharEventosEnVivo();
    }

    @Override
    protected void onDestroy() {
        escuchandoEventos = false;
        if (conexionEventos != null) {
            conexionEventos.disconnect();
        }
        super.onDestroy();
    }

    private void escucharEventosEnVivo() {
        escuchandoEventos = true;

        eventExecutor.execute(() -> {
            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/auctions/" + auctionId + "/events");
                conexionEventos = (HttpURLConnection) url.openConnection();
                conexionEventos.setRequestMethod("GET");
                conexionEventos.setRequestProperty("Accept", "text/event-stream");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conexionEventos.getInputStream())
                );

                String linea;
                while (escuchandoEventos && (linea = reader.readLine()) != null) {
                    if (linea.startsWith("data: ")) {
                        JSONObject estado = new JSONObject(linea.substring(6));
                        mainHandler.post(() -> mostrarEstadoVivo(estado));
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() ->
                        txtEstadoVivo.setText("No se pudo conectar al estado en vivo.")
                );
            } finally {
                if (conexionEventos != null) {
                    conexionEventos.disconnect();
                }
            }
        });
    }

    private void mostrarEstadoVivo(JSONObject estado) {
        try {
            if (estado.has("error")) {
                txtEstadoVivo.setText(estado.optString("error", "Sin estado en vivo"));
                return;
            }

            JSONObject itemActual = estado.getJSONObject("itemActual");
            double mejorOferta = estado.optDouble("mejorOferta", 0);
            double pujaMinima = estado.optDouble("pujaMinima", 0);
            String pujaMaxima = estado.isNull("pujaMaxima")
                    ? "sin límite"
                    : "$" + estado.optDouble("pujaMaxima", 0);
            int segundosRestantes = estado.optInt("segundosRestantes", 0);

            txtEstadoVivo.setText(
                    "Estado en vivo\n" +
                            "Ítem actual: " + itemActual.optString("descripcionCatalogo", "-") + "\n" +
                            "Mejor oferta: $" + mejorOferta + "\n" +
                            "Puja mínima: $" + pujaMinima + "\n" +
                            "Puja máxima: " + pujaMaxima + "\n" +
                            "Tiempo restante: " + segundosRestantes + " segundos"
            );
        } catch (Exception e) {
            txtEstadoVivo.setText("Error mostrando estado en vivo.");
        }
    }

    private void cargarDetalleSubasta() {
        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/auctions/" + auctionId);
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
                    JSONObject subasta = new JSONObject(respuesta);

                    mainHandler.post(() -> mostrarDetalleSubasta(subasta));
                } else {
                    JSONObject errorJson = new JSONObject(respuesta);
                    String error = errorJson.optString("error", "Error al cargar subasta");

                    mainHandler.post(() -> txtDatosSubasta.setText(error));
                }

            } catch (Exception e) {
                mainHandler.post(() -> txtDatosSubasta.setText("No se pudo conectar con el servidor."));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void mostrarDetalleSubasta(JSONObject subasta) {
        try {
            String fecha = subasta.optString("fecha", "-");
            String hora = subasta.optString("hora", "-");
            String estado = subasta.optString("estado", "-");
            String ubicacion = subasta.optString("ubicacion", "-");
            String categoria = subasta.optString("categoria", "-");
            String moneda = subasta.optString("moneda", "-");
            String subastador = subasta.optString("subastador", "-");
            categoriaSubasta = categoria;

            String permiso = puedePujar
                    ? "Estado del usuario: habilitado para pujar"
                    : "Estado del usuario: solo visualización";

            txtDatosSubasta.setText(
                    "Ubicación: " + ubicacion + "\n" +
                            "Fecha: " + fecha + "\n" +
                            "Hora: " + hora + "\n" +
                            "Estado: " + estado + "\n" +
                            "Categoría: " + categoria + "\n" +
                            "Moneda: " + moneda + "\n" +
                            "Subastador: " + subastador + "\n\n" +
                            permiso
            );

        } catch (Exception e) {
            txtDatosSubasta.setText("Error mostrando datos de la subasta.");
        }
    }

    private void cargarCatalogo() {
        txtMensajeDetalle.setText("Cargando catálogo...");
        contenedorCatalogo.removeAllViews();

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/auctions/" + auctionId + "/catalog");
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
                    JSONArray catalogo = new JSONArray(respuesta);

                    mainHandler.post(() -> mostrarCatalogo(catalogo));
                } else {
                    JSONObject errorJson = new JSONObject(respuesta);
                    String error = errorJson.optString("error", "Error al cargar catálogo");

                    mainHandler.post(() -> txtMensajeDetalle.setText(error));
                }

            } catch (Exception e) {
                mainHandler.post(() -> txtMensajeDetalle.setText("No se pudo conectar con el servidor."));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void mostrarCatalogo(JSONArray catalogo) {
        contenedorCatalogo.removeAllViews();

        if (catalogo.length() == 0) {
            txtMensajeDetalle.setText("No hay ítems cargados para esta subasta.");
            return;
        }

        txtMensajeDetalle.setText("Ítems encontrados: " + catalogo.length());

        try {
            for (int i = 0; i < catalogo.length(); i++) {
                JSONObject item = catalogo.getJSONObject(i);

                int itemId = item.getInt("itemId");
                int productId = item.optInt("productoId", 0);
                String descripcionCatalogo = item.optString("descripcionCatalogo", "-");
                String descripcionCompleta = item.optString("descripcionCompleta", "-");
                String historia = item.optString("historia", "");
                String artistaDiseniador = item.optString("artistaDiseniador", "");
                double precioBase = item.optDouble("precioBase", 0);
                double comision = item.optDouble("comision", 0);
                double mejorOferta = item.optDouble("mejorOferta", precioBase);
                String vendido = item.optString("vendido", "no");

                View card = crearCardCatalogo(
                        itemId,
                        productId,
                        descripcionCatalogo,
                        descripcionCompleta,
                        historia,
                        artistaDiseniador,
                        precioBase,
                        comision,
                        mejorOferta,
                        vendido
                );

                contenedorCatalogo.addView(card);
            }

        } catch (Exception e) {
            txtMensajeDetalle.setText("Error mostrando catálogo.");
        }
    }

    private View crearCardCatalogo(
            int itemId,
            int productId,
            String descripcionCatalogo,
            String descripcionCompleta,
            String historia,
            String artistaDiseniador,
            double precioBase,
            double comision,
            double mejorOferta,
            String vendido
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(18));
        card.setBackgroundResource(R.drawable.bg_card_premium);
        card.setElevation(dp(8));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(22));
        card.setLayoutParams(cardParams);

        // VISUAL HERO DEL LOTE
        LinearLayout visual = new LinearLayout(this);
        visual.setOrientation(LinearLayout.VERTICAL);
        visual.setPadding(dp(18), dp(18), dp(18), dp(18));
        visual.setBackgroundResource(R.drawable.bg_visual_lot);

        LinearLayout.LayoutParams visualParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(190)
        );
        visual.setLayoutParams(visualParams);

        TextView status = new TextView(this);

        if (vendido.equals("si")) {
            status.setText("FINALIZADO · LOTE #" + itemId);
            status.setTextColor(Color.parseColor("#FECACA"));
        } else {
            status.setText("PUJA ABIERTA · LOTE #" + itemId);
            status.setTextColor(Color.WHITE);
        }

        status.setTextSize(11);
        status.setTypeface(null, android.graphics.Typeface.BOLD);
        status.setLetterSpacing(0.08f);

        TextView title = new TextView(this);
        title.setText(descripcionCatalogo);
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setMaxLines(2);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, dp(58), 0, 0);
        title.setLayoutParams(titleParams);

        TextView subtitle = new TextView(this);
        subtitle.setText("Pieza verificada · Autenticidad garantizada");
        subtitle.setTextColor(Color.parseColor("#E8EEF5"));
        subtitle.setTextSize(13);

        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.setMargins(0, dp(6), 0, 0);
        subtitle.setLayoutParams(subtitleParams);

        visual.addView(status);
        visual.addView(title);
        visual.addView(subtitle);

        ImageView fotoProducto = new ImageView(this);
        fotoProducto.setVisibility(View.VISIBLE);
        fotoProducto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        fotoProducto.setBackgroundColor(Color.parseColor("#E2E8F0"));
        fotoProducto.setImageResource(R.drawable.logo);

        LinearLayout.LayoutParams fotoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(180)
        );
        fotoParams.setMargins(0, dp(14), 0, 0);
        fotoProducto.setLayoutParams(fotoParams);

        if (productId > 0) {
            cargarFotoProducto(productId, fotoProducto);
        }

        // DESCRIPCIÓN
        TextView descripcion = new TextView(this);
        descripcion.setText(descripcionCompleta);
        descripcion.setTextColor(Color.parseColor("#475569"));
        descripcion.setTextSize(14);
        descripcion.setLineSpacing(dp(3), 1.0f);

        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.setMargins(0, dp(16), 0, 0);
        descripcion.setLayoutParams(descParams);

        // MÉTRICAS PREMIUM
        LinearLayout metricsRow = new LinearLayout(this);
        metricsRow.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams metricsRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        metricsRowParams.setMargins(0, dp(16), 0, 0);
        metricsRow.setLayoutParams(metricsRowParams);

        TextView bidBox = new TextView(this);
        bidBox.setText("MEJOR OFERTA\n$" + mejorOferta);
        bidBox.setTextColor(Color.parseColor("#071827"));
        bidBox.setTextSize(13);
        bidBox.setTypeface(null, android.graphics.Typeface.BOLD);
        bidBox.setPadding(dp(14), dp(14), dp(14), dp(14));
        bidBox.setBackgroundResource(R.drawable.bg_metric_box);

        LinearLayout.LayoutParams bidParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        bidParams.setMargins(0, 0, dp(6), 0);
        bidBox.setLayoutParams(bidParams);

        TextView baseBox = new TextView(this);
        baseBox.setText("PRECIO BASE\n$" + precioBase);
        baseBox.setTextColor(Color.parseColor("#071827"));
        baseBox.setTextSize(13);
        baseBox.setTypeface(null, android.graphics.Typeface.BOLD);
        baseBox.setPadding(dp(14), dp(14), dp(14), dp(14));
        baseBox.setBackgroundResource(R.drawable.bg_metric_box);

        LinearLayout.LayoutParams baseParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        baseParams.setMargins(dp(6), 0, 0, 0);
        baseBox.setLayoutParams(baseParams);

        metricsRow.addView(bidBox);
        metricsRow.addView(baseBox);

        // PROVENANCE / COMISIÓN
        LinearLayout infoPanel = new LinearLayout(this);
        infoPanel.setOrientation(LinearLayout.VERTICAL);
        infoPanel.setPadding(dp(16), dp(14), dp(16), dp(14));
        infoPanel.setBackgroundResource(R.drawable.bg_metric_box);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.setMargins(0, dp(14), 0, 0);
        infoPanel.setLayoutParams(infoParams);

        TextView infoTitle = new TextView(this);
        infoTitle.setText("INFORMACIÓN DEL LOTE");
        infoTitle.setTextColor(Color.parseColor("#A8872F"));
        infoTitle.setTextSize(11);
        infoTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        infoTitle.setLetterSpacing(0.08f);

        String detalleInfo = "Comisión: $" + comision;

        if (artistaDiseniador != null && !artistaDiseniador.equals("") && !artistaDiseniador.equals("null")) {
            detalleInfo += "\nArtista o diseñador: " + artistaDiseniador;
        }

        if (historia != null && !historia.equals("") && !historia.equals("null")) {
            detalleInfo += "\nHistoria: " + historia;
        }

        TextView infoText = new TextView(this);
        infoText.setText(detalleInfo);
        infoText.setTextColor(Color.parseColor("#475569"));
        infoText.setTextSize(13);
        infoText.setLineSpacing(dp(3), 1.0f);

        LinearLayout.LayoutParams infoTextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoTextParams.setMargins(0, dp(8), 0, 0);
        infoText.setLayoutParams(infoTextParams);

        infoPanel.addView(infoTitle);
        infoPanel.addView(infoText);

        // SEGURIDAD
        TextView security = new TextView(this);
        security.setText("AUTENTICIDAD VERIFICADA · OPERACIÓN SEGURA");
        security.setTextColor(Color.parseColor("#166534"));
        security.setTextSize(11);
        security.setTypeface(null, android.graphics.Typeface.BOLD);
        security.setGravity(android.view.Gravity.CENTER);
        security.setPadding(dp(12), dp(8), dp(12), dp(8));
        security.setBackgroundResource(R.drawable.bg_success_chip);

        LinearLayout.LayoutParams securityParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        securityParams.setMargins(0, dp(16), 0, 0);
        security.setLayoutParams(securityParams);

        Button btnPujar = new Button(this);

        if (puedePujar && !vendido.equals("si")) {
            btnPujar.setText("PUJAR");
            btnPujar.setBackgroundResource(R.drawable.bg_button_gold);
            btnPujar.setTextColor(Color.parseColor("#071827"));
            btnPujar.setEnabled(true);

            btnPujar.setOnClickListener(v -> {
                Intent intent = new Intent(AuctionDetailActivity.this, BidActivity.class);
                intent.putExtra("auctionId", auctionId);
                intent.putExtra("itemId", itemId);
                intent.putExtra("descripcion", descripcionCatalogo);
                intent.putExtra("productId", productId);
                intent.putExtra("precioBase", precioBase);
                intent.putExtra("mejorOferta", mejorOferta);
                intent.putExtra("categoria", categoriaSubasta);
                startActivity(intent);
            });

        } else {
            btnPujar.setText("SOLO VER");
            btnPujar.setBackgroundResource(R.drawable.bg_button_outline);
            btnPujar.setTextColor(Color.parseColor("#071827"));
            btnPujar.setEnabled(false);
        }

        btnPujar.setTextSize(12);
        btnPujar.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        btnParams.setMargins(0, dp(16), 0, 0);
        btnPujar.setLayoutParams(btnParams);

        card.addView(visual);
        card.addView(fotoProducto);
        card.addView(descripcion);
        card.addView(metricsRow);
        card.addView(infoPanel);
        card.addView(security);
        card.addView(btnPujar);

        return card;
    }

    private void cargarFotoProducto(int productId, ImageView imageView) {
        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/products/" + productId + "/photos");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");

                int statusCode = connection.getResponseCode();
                if (statusCode != 200) {
                    return;
                }

                String respuesta = leerRespuesta(connection.getInputStream());
                JSONArray fotos = new JSONArray(respuesta);
                if (fotos.length() == 0) {
                    return;
                }

                String base64 = fotos.getJSONObject(0).optString("fotoBase64", "");
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap == null) {
                    return;
                }

                mainHandler.post(() -> {
                    imageView.setImageBitmap(bitmap);
                    imageView.setVisibility(View.VISIBLE);
                });
            } catch (Exception ignored) {
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
