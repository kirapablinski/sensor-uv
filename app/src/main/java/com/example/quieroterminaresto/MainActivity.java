package com.example.quieroterminaresto;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private TextView tvNivelUV, tvEstadoVentilador;
    private Button btnEncenderApagar;
    private OkHttpClient client;
    private Handler handler;

    private String apiUrlDatosUV = "https://api.thingspeak.com/channels/2788737/fields/1/last.json?api_key=ZKCWFVK4S2OJAPOC";
    private String apiUrlEnviarComando = "https://api.thingspeak.com/update?api_key=RZDE2VU25W07N7NM&field6=";

    private boolean ventiladorEncendido = false; // Estado inicial del ventilador

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar vistas
        tvNivelUV = findViewById(R.id.tvNivelUV);
        tvEstadoVentilador = findViewById(R.id.tvEstadoLed);
        btnEncenderApagar = findViewById(R.id.btnEncenderApagar);

        client = new OkHttpClient();
        handler = new Handler();

        // Iniciar la actualización periódica de los datos UV
        iniciarActualizacionDatos();

        // Configurar botón de encendido/apagado
        btnEncenderApagar.setOnClickListener(v -> toggleVentilador());
    }

    // Método para alternar el estado del ventilador
    private void toggleVentilador() {
        ventiladorEncendido = !ventiladorEncendido; // Cambia el estado
        int comando = ventiladorEncendido ? 1 : 0; // 1 = encender, 0 = apagar
        enviarComandoVentilador(comando);
    }

    // Método para enviar el comando al servidor
    private void enviarComandoVentilador(int comando) {
        String url = apiUrlEnviarComando + comando;

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvEstadoVentilador.setText("Error al enviar comando"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        String estado = ventiladorEncendido ? "Encendido" : "Apagado";
                        tvEstadoVentilador.setText("LED: " + estado);
                        btnEncenderApagar.setText(ventiladorEncendido ? "Apagar LED" : "Encender LED");
                    });
                }
            }
        });
    }

    // Método para actualizar los datos UV periódicamente
    private void iniciarActualizacionDatos() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                obtenerDatosUV();
                handler.postDelayed(this, 30000); // Actualiza cada 30 segundos
            }
        }, 0);
    }

    // Método para obtener los datos UV
    private void obtenerDatosUV() {
        Request request = new Request.Builder().url(apiUrlDatosUV).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvNivelUV.setText("Error al obtener datos"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    String nivelUV = extraerNivelUV(responseData);
                    runOnUiThread(() -> tvNivelUV.setText("Nivel UV: " + nivelUV));
                }
            }
        });
    }

    // Extraer el valor del campo UV del JSON
    private String extraerNivelUV(String responseData) {
        try {
            org.json.JSONObject jsonObject = new org.json.JSONObject(responseData);
            return jsonObject.getString("field1");
        } catch (Exception e) {
            e.printStackTrace();
            return "Error";
        }
    }
}
