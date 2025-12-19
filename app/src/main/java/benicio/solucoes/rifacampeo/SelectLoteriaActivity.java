package benicio.solucoes.rifacampeo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import benicio.solucoes.rifacampeo.databinding.ActivityMakeSorteioBinding;
import benicio.solucoes.rifacampeo.databinding.ActivitySelectLoteriaBinding;
import benicio.solucoes.rifacampeo.objects.BilheteModel;
import benicio.solucoes.rifacampeo.objects.DateLimitModel;
import benicio.solucoes.rifacampeo.objects.RecolheuModel;
import benicio.solucoes.rifacampeo.objects.RecolhimentoResponse;
import benicio.solucoes.rifacampeo.objects.ResponseSimple;
import benicio.solucoes.rifacampeo.objects.RetornoModel;
import benicio.solucoes.rifacampeo.objects.VendedorModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SelectLoteriaActivity extends AppCompatActivity {
    String TAG = "buceta";
    private ActivitySelectLoteriaBinding selectLoteriaBinding;
    float somaBilhetes = 0.0f;
    int limiteAposta = 0;

    float podefazer = 0.0f;

    float saldoVendedor = 0.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        selectLoteriaBinding = ActivitySelectLoteriaBinding.inflate(getLayoutInflater());
        setContentView(selectLoteriaBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);


        selectLoteriaBinding.button7.setOnClickListener(v -> {
            if (limiteAposta <= somaBilhetes && saldoVendedor > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SelectLoteriaActivity.this);
                builder.setTitle("Atenção");
                builder.setMessage("Você não pode apostar, pois já atingiu o seu limite de crédito.");
                builder.setPositiveButton("OK", null);
                AlertDialog dialog = builder.create();
                dialog.show();

                selectLoteriaBinding.limiteString.setVisibility(View.VISIBLE);
            } else {
                got_make("COR");
            }
        });

        selectLoteriaBinding.button8.setOnClickListener(v -> {
            if (limiteAposta <= somaBilhetes && saldoVendedor > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SelectLoteriaActivity.this);
                builder.setTitle("Atenção");
                builder.setMessage("Você não pode apostar, pois já atingiu o seu limite de crédito.");
                builder.setPositiveButton("OK", null);
                AlertDialog dialog = builder.create();
                dialog.show();

                selectLoteriaBinding.limiteString.setVisibility(View.VISIBLE);

            } else {
                got_make("FD");
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        VendedorModel vendedorModel = new VendedorModel();


        vendedorModel.setSenha(getIntent().getStringExtra("code"));
        vendedorModel.setSerial(getDeviceUniqueId());
        RetrofitUtils.getApiService().login(vendedorModel).enqueue(new Callback<ResponseSimple>() {
            @Override
            public void onResponse(Call<ResponseSimple> call, Response<ResponseSimple> response1) {
                if (response1.isSuccessful()) {
                    if (response1.body().isSuccess()) {

                        RetrofitUtils.getApiService().retornar_recolhimento(
                                        null, null, null, null, 999999999, 1)
                                .enqueue(new Callback<RecolhimentoResponse>() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void onResponse(Call<RecolhimentoResponse> call, Response<RecolhimentoResponse> response) {
                                        if (response.isSuccessful() && response.body() != null) {

                                            // Pega os extras da Intent
                                            float valorTotalGeradoCOR = response1.body().getValorTotalGeradoCOR();
                                            float valorTotalGeradoDF = response1.body().getValorTotalGeradoDF();
                                            float valorTotalGeradoDFLoteriaAtual = response1.body().getValorTotalGeradoDFLoteriaAtual();
                                            float valorTotalGeradoCORLoteriaAtual = response1.body().getValorTotalGeradoCORLoteriaAtual();

                                            float valorRecolhidoVendedor  = 0.0f;
                                            for (RecolheuModel recolheuModel : response.body().getItens()){
                                                if ( recolheuModel.getVendedor().trim().equals(response1.body().getVendedor().getNome().trim())){
                                                    valorRecolhidoVendedor += recolheuModel.getValor();
                                                }
                                            }

                                            limiteAposta = response1.body().getVendedor().getLimiteAposta();

                                            // Liga os TextViews
                                            TextView tvValorCoruja = findViewById(R.id.tvValorCoruja);
                                            TextView tvValorFederal = findViewById(R.id.tvValorFederal);


                                            float saldo_pode_fazer_loteria = (valorTotalGeradoCORLoteriaAtual + valorTotalGeradoDFLoteriaAtual);// - valorRecolhidoVendedor;

                                            saldoVendedor = ((valorTotalGeradoDF + valorTotalGeradoCOR) - ((valorTotalGeradoDF + valorTotalGeradoCOR) * ((float) response1.body().getVendedor().getComissao() / 100))) - valorRecolhidoVendedor;

                                            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
                                            nf.setMinimumFractionDigits(2);
                                            nf.setMaximumFractionDigits(2);

                                            Log.d("buceta", "valorTotalGeradoDF: " + valorTotalGeradoDF + " valorTotalGeradoCOR: " + valorTotalGeradoCOR + " valorRecolhidoVendedor: " + valorRecolhidoVendedor);
                                            // Monta os textos
                                            tvValorCoruja.setText(
                                                    "SALDO DE APOSTAS CORUJA: R$ " + nf.format(valorTotalGeradoCORLoteriaAtual) +
                                                            "\nSALDO DE APOSTAS FEDERAL: R$ " + nf.format(valorTotalGeradoDFLoteriaAtual) +
                                                    "\nTOTAL DE APOSTAS: R$ " + nf.format((valorTotalGeradoCORLoteriaAtual+valorTotalGeradoDFLoteriaAtual)));
                                            if ( saldoVendedor < 0){
                                                tvValorFederal.setText("SALDO DEVE: R$ 0");
                                            }else{
                                                tvValorFederal.setText("SALDO DEVE: R$ " + nf.format(saldoVendedor));
                                            }
                                            podefazer = limiteAposta - (saldo_pode_fazer_loteria);
                                            //tvLimiteAposta.setText("Saldo Loteria: R$ " + (valorTotalGeradoCOR+valorTotalGeradoDF) + "\nSaldo Recolhido: R$ " + valorRecolhidoVendedor);


                                            somaBilhetes = saldo_pode_fazer_loteria;

                                            Log.d("buceta", "somaBilhetes: " + somaBilhetes + " " + "limiteAposta: " + limiteAposta + "\n" + "saldoVendedor: " + saldoVendedor);

                                            if ( limiteAposta <= somaBilhetes && saldoVendedor > 0){
                                                selectLoteriaBinding.limiteString.setVisibility(View.VISIBLE);
                                            }


                                        } else {
                                            Toast.makeText(SelectLoteriaActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<RecolhimentoResponse> call, Throwable throwable) {
                                        Toast.makeText(SelectLoteriaActivity.this, "Resposta inválida da API", Toast.LENGTH_SHORT).show();
                                    }
                                });


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
                        i.putExtra("podefazer", podefazer);
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