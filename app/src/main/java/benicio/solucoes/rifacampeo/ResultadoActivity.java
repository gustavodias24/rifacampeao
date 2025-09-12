package benicio.solucoes.rifacampeo;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import benicio.solucoes.rifacampeo.databinding.ActivityResultadoBinding;
import benicio.solucoes.rifacampeo.objects.NumerosPremiadosModel;
import benicio.solucoes.rifacampeo.objects.ResultadoBilheteModel;
import benicio.solucoes.rifacampeo.objects.SorteioModel;
import benicio.solucoes.rifacampeo.utils.ApiService;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultadoActivity extends AppCompatActivity {

    Dialog dialogCarreando;

    int nu1, nu2, nu3, nu4, nu5, nu6; // Variáveis finais
    EditText e1, e2, e3, e4, e12, e22, e32, e42, e13, e23, e33, e43,
            e14, e24, e34, e44, e15, e25, e35, e45, e16, e26, e36, e46;

    ActivityResultadoBinding mainBinding;

    ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityResultadoBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        apiService = RetrofitUtils.getApiService();

        AlertDialog.Builder builder = new AlertDialog.Builder(ResultadoActivity.this);
        builder.setMessage("CARREGANDO...");
        builder.setCancelable(false);
        dialogCarreando = builder.create();

// Inicializar os EditTexts (exemplo, adicione todos que precisar)
        e1 = findViewById(R.id.e1);
        e2 = findViewById(R.id.e2);
        e3 = findViewById(R.id.e3);
        e4 = findViewById(R.id.e4);

        e12 = findViewById(R.id.e12);
        e22 = findViewById(R.id.e22);
        e32 = findViewById(R.id.e32);
        e42 = findViewById(R.id.e42);

        e13 = findViewById(R.id.e13);
        e23 = findViewById(R.id.e23);
        e33 = findViewById(R.id.e33);
        e43 = findViewById(R.id.e43);

        e14 = findViewById(R.id.e14);
        e24 = findViewById(R.id.e24);
        e34 = findViewById(R.id.e34);
        e44 = findViewById(R.id.e44);

        e15 = findViewById(R.id.e15);
        e25 = findViewById(R.id.e25);
        e35 = findViewById(R.id.e35);
        e45 = findViewById(R.id.e45);

        e16 = findViewById(R.id.e16);
        e26 = findViewById(R.id.e26);
        e36 = findViewById(R.id.e36);
        e46 = findViewById(R.id.e46);

        // Colocar os campos em um array para facilitar
        EditText[] campos = {e1, e2, e3, e4,
                e12, e22, e32, e42,
                e13, e23, e33, e43,
                e14, e24, e34, e44,
                e15, e25, e35, e45,
                e16, e26, e36, e46};

        // Aplicar o TextWatcher em cada campo
        for (int i = 0; i < campos.length; i++) {
            final int index = i;
            campos[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1) {
                        if (index < campos.length - 1) {
                            campos[index + 1].requestFocus(); // Vai para o próximo campo
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        mainBinding.button5.setOnClickListener(v -> {

            dialogCarreando.show();

            nu1 = getNumero(e1, e2, e3, e4);
            nu2 = getNumero(e12, e22, e32, e42);
            nu3 = getNumero(e13, e23, e33, e43);
            nu4 = getNumero(e14, e24, e34, e44);
            nu5 = getNumero(e15, e25, e35, e45);
            nu6 = getNumero(e16, e26, e36, e46);

            // Aqui você pode usar os valores nu1, nu2, nu3, etc.
            // Exemplo:
            Log.d("Mayara", "nu1: " + nu1);
            Log.d("Mayara", "nu2: " + nu2);
            Log.d("Mayara", "nu3: " + nu3);
            Log.d("Mayara", "nu4: " + nu4);
            Log.d("Mayara", "nu5: " + nu5);
            Log.d("Mayara", "nu6: " + nu6);

            apiService.bilhetes_ganharadores(new NumerosPremiadosModel(nu1, nu2, nu3, nu4, nu5, nu6)).enqueue(new Callback<ResultadoBilheteModel>() {
                @Override
                public void onResponse(Call<ResultadoBilheteModel> call, Response<ResultadoBilheteModel> response) {

                    dialogCarreando.dismiss();

                    if (response.isSuccessful() && response.body() != null) {
                        SorteioModel sorteioModel = response.body().getMsg();

                        // Referências dos botões
                        Button btn1 = findViewById(R.id.sorteio1);
                        Button btn2 = findViewById(R.id.sorteio2);
                        Button btn3 = findViewById(R.id.sorteio3);
                        Button btn4 = findViewById(R.id.sorteio4);
                        Button btn5 = findViewById(R.id.sorteio5);
                        Button btn6 = findViewById(R.id.sorteio6);

                        // Configura cada botão
                        configurarBotao(btn1, sorteioModel.getSorteio1());
                        configurarBotao(btn2, sorteioModel.getSorteio2());
                        configurarBotao(btn3, sorteioModel.getSorteio3());
                        configurarBotao(btn4, sorteioModel.getSorteio4());
                        configurarBotao(btn5, sorteioModel.getSorteio5());
                        configurarBotao(btn6, sorteioModel.getSorteio6());
                    } else {
                        Toast.makeText(ResultadoActivity.this, "Erro na resposta", Toast.LENGTH_SHORT).show();
                    }


                }

                @Override
                public void onFailure(Call<ResultadoBilheteModel> call, Throwable throwable) {
                    dialogCarreando.dismiss();
                    Toast.makeText(ResultadoActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });


        });


    }

    private void configurarBotao(Button button, String link) {
        if (link != null && !link.isEmpty()) {
            button.setText("GANHOU");
            button.setTextColor(Color.GREEN);

            button.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                startActivity(intent);
            });
        } else {
            button.setText("NADA");
            button.setTextColor(Color.RED);
            button.setOnClickListener(null); // sem ação
        }
    }

    // Função que lê 4 EditTexts e retorna um número inteiro
    private int getNumero(EditText a, EditText b, EditText c, EditText d) {
        String s1 = a.getText().toString().trim();
        String s2 = b.getText().toString().trim();
        String s3 = c.getText().toString().trim();
        String s4 = d.getText().toString().trim();

        // Se estiver vazio, substitui por "0"
        if (s1.isEmpty()) s1 = "0";
        if (s2.isEmpty()) s2 = "0";
        if (s3.isEmpty()) s3 = "0";
        if (s4.isEmpty()) s4 = "0";

        // Junta os 4 dígitos e converte em int
        String numeroStr = s1 + s2 + s3 + s4;
        return Integer.parseInt(numeroStr);
    }
}