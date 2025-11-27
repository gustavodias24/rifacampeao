package benicio.solucoes.rifacampeo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;

import benicio.solucoes.rifacampeo.databinding.ActivityMakeSorteioBinding;
import benicio.solucoes.rifacampeo.databinding.ActivitySelectLoteriaBinding;
import benicio.solucoes.rifacampeo.objects.BilheteModel;
import benicio.solucoes.rifacampeo.objects.DateLimitModel;
import benicio.solucoes.rifacampeo.objects.ResponseSimple;
import benicio.solucoes.rifacampeo.objects.RetornoModel;
import benicio.solucoes.rifacampeo.objects.VendedorModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SelectLoteriaActivity extends AppCompatActivity {

    private ActivitySelectLoteriaBinding selectLoteriaBinding;
    int somaBilhetes = 0;
    int limiteAposta = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        selectLoteriaBinding = ActivitySelectLoteriaBinding.inflate(getLayoutInflater());
        setContentView(selectLoteriaBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);


        selectLoteriaBinding.button7.setOnClickListener(v -> {
            if (somaBilhetes >= limiteAposta) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SelectLoteriaActivity.this);
                builder.setTitle("Atenção");
                builder.setMessage("Você não pode apostar, pois já atingiu o seu limite de crédito.");
                builder.setPositiveButton("OK", null);
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                got_make("COR");
            }
        });

        selectLoteriaBinding.button8.setOnClickListener(v -> {
            if (somaBilhetes >= limiteAposta) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SelectLoteriaActivity.this);
                builder.setTitle("Atenção");
                builder.setMessage("Você não pode apostar, pois já atingiu o seu limite de crédito.");
                builder.setPositiveButton("OK", null);
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                got_make("FD");
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        VendedorModel vendedorModel = new VendedorModel();
        Log.d("buceta", "onStart: " + getIntent().getStringExtra("code"));
        vendedorModel.setSenha(getIntent().getStringExtra("code"));
        vendedorModel.setSerial(getDeviceUniqueId());
        RetrofitUtils.getApiService().login(vendedorModel).enqueue(new Callback<ResponseSimple>() {
            @Override
            public void onResponse(Call<ResponseSimple> call, Response<ResponseSimple> response) {
                if (response.isSuccessful()) {
                    if (response.body().isSuccess()) {

                        // Pega os extras da Intent
                        int valorTotalGeradoCOR = response.body().getValorTotalGeradoCOR();
                        int valorTotalGeradoDF = response.body().getValorTotalGeradoDF();
                        limiteAposta = response.body().getVendedor().getLimiteAposta();

                        // Liga os TextViews
                        TextView tvValorCoruja = findViewById(R.id.tvValorCoruja);
                        TextView tvValorFederal = findViewById(R.id.tvValorFederal);
                        TextView tvLimiteAposta = findViewById(R.id.tvLimiteAposta);
                        TextView tvStatusCredito = findViewById(R.id.tvStatusCredito);

                        // Monta os textos
                        tvValorCoruja.setText("Você gerou R$ " + valorTotalGeradoCOR + " em bilhetes Coruja");
                        tvValorFederal.setText("Você gerou R$ " + valorTotalGeradoDF + " em bilhetes Federal");
                        tvLimiteAposta.setText("Seu Crédito para gerar bilhetes é de R$ " + limiteAposta);

                        Log.d("buceta", "valorTotalGeradoCOR: " + valorTotalGeradoCOR + " " + "valorTotalGeradoDF: " + valorTotalGeradoDF);

                        int somaBilhetes = valorTotalGeradoCOR + valorTotalGeradoDF;

                        Log.d("buceta", "somaBilhetes: " + somaBilhetes + " " + "limiteAposta: " + limiteAposta);

                        if (somaBilhetes < limiteAposta) {
                            tvStatusCredito.setText("Você está dentro do seu limite de aposta.");
                            tvStatusCredito.setTextColor(Color.BLACK);
                        } else {
                            tvStatusCredito.setText("Você ultrapassou o seu limite de aposta!");
                            tvStatusCredito.setTextColor(Color.RED);
                        }

                    }
                } else {
                    Toast.makeText(SelectLoteriaActivity.this, "Problema de Conexão!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseSimple> call, Throwable throwable) {

            }
        });
    }

    private void got_make(String loteria) {

        BilheteModel b = new BilheteModel(); // uso bilhete model para passar a loteria
        b.setLoteria(loteria);
        RetrofitUtils.getApiService().check_date(b).enqueue(new Callback<RetornoModel>() {
            @Override
            public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
                if (response.isSuccessful()) {
                    if (response.body().getSuccess()) {
                        Intent i = new Intent(SelectLoteriaActivity.this, MakeSorteioActivity.class);
                        i.putExtra("loteria", loteria);
                        startActivity(i);
                    }
                    Toast.makeText(SelectLoteriaActivity.this, response.body().getMsg(), Toast.LENGTH_SHORT).show();

                }
            }

            @Override
            public void onFailure(Call<RetornoModel> call, Throwable throwable) {

            }
        });


    }

    public String getDeviceUniqueId() {
        return Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }
}