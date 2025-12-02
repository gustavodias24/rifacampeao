package benicio.solucoes.rifacampeo;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import benicio.solucoes.rifacampeo.databinding.ActivityMakeRecolhimentoBinding;
import benicio.solucoes.rifacampeo.objects.QueryModelEmpty;
import benicio.solucoes.rifacampeo.objects.RecolheuModel;
import benicio.solucoes.rifacampeo.objects.VendedorModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MakeRecolhimentoActivity extends AppCompatActivity {

    ActivityMakeRecolhimentoBinding mainBinding;

    private final List<VendedorModel> vendedores = new ArrayList<>();
    private final List<String> nomes = new ArrayList<>();
    private ArrayAdapter<String> adapterNomes;

    private String nomeRecolhedor = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityMakeRecolhimentoBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Adapter para o AutoCompleteTextView
        adapterNomes = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                nomes
        );

        carregarVendedores();

        nomeRecolhedor = getIntent().getExtras().getString("recolhedor", "");
        Log.d("buceta", "nomeRecolhedor: " + nomeRecolhedor);

        // seta o adapter no campo de vendedor
        mainBinding.edtVendedor.setAdapter(adapterNomes);
        mainBinding.edtVendedor.setThreshold(1); // começa a sugerir a partir de 1 caractere

        // Preenche data/hora atual
        String agora = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        mainBinding.edtDataHora.setText(agora);

        mainBinding.btnConfirmar.setOnClickListener(v -> {
            String valorString = mainBinding.edtValor.getText().toString();
            String vendedorTexto = mainBinding.edtVendedor.getText().toString().trim();

            if (vendedorTexto.isEmpty()) {
                Toast.makeText(this, "Digite ou selecione um Vendedor!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isValorMonetarioValido(valorString)) {

                String normalizado = valorString.replace(",", ".");
                float valor;
                try {
                    valor = Float.parseFloat(normalizado);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Valor inválido", Toast.LENGTH_SHORT).show();
                    return;
                }

                RecolheuModel recolheuModelNovo = new RecolheuModel(
                        mainBinding.edtDataHora.getText().toString(),
                        vendedorTexto, // aqui vai o que foi digitado / escolhido
                        valor,
                        mainBinding.edtObservacoes.getText().toString(),
                        mainBinding.rbRecolhimento.isChecked() ? 0 : 1,
                        nomeRecolhedor
                );

                // ---------- AlertDialog de carregando ----------
                AlertDialog loadingDialog = new AlertDialog.Builder(MakeRecolhimentoActivity.this)
                        .setView(new ProgressBar(MakeRecolhimentoActivity.this))
                        .setCancelable(false)
                        .create();
                loadingDialog.show();
                // ------------------------------------------------

                RetrofitUtils.getApiService().salvar_recolhimento(recolheuModelNovo)
                        .enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                loadingDialog.dismiss(); // fecha o loading

                                if (response.isSuccessful()) {
                                    Toast.makeText(MakeRecolhimentoActivity.this,
                                            "Recolhimento Registrado", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(MakeRecolhimentoActivity.this,
                                            "Erro ao registrar (código " + response.code() + ")",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable throwable) {
                                loadingDialog.dismiss(); // fecha o loading
                                Toast.makeText(MakeRecolhimentoActivity.this,
                                        "Falha na conexão", Toast.LENGTH_SHORT).show();
                            }
                        });

            } else {
                Toast.makeText(this, "Valor inválido", Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void carregarVendedores() {
        RetrofitUtils.getApiService().returnVendedores(1, new QueryModelEmpty())
                .enqueue(new Callback<List<VendedorModel>>() {
                    @Override
                    public void onResponse(Call<List<VendedorModel>> call,
                                           Response<List<VendedorModel>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            vendedores.clear();
                            vendedores.addAll(response.body());

                            nomes.clear();
                            // Se quiser manter um "Selecione" no autocomplete, descomenta:
                            // nomes.add("Selecione");

                            for (VendedorModel v : vendedores) {
                                nomes.add(safe(v.getNome()));
                            }

                            adapterNomes.notifyDataSetChanged();
                        } else {
                            Toast.makeText(MakeRecolhimentoActivity.this,
                                    "Erro de conexão ao carregar vendedores",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<VendedorModel>> call, Throwable t) {
                        Toast.makeText(MakeRecolhimentoActivity.this,
                                "Falha na API de vendedores", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    private boolean isValorMonetarioValido(String texto) {
        if (texto == null) return false;

        texto = texto.trim();

        // não aceita vazio
        if (texto.isEmpty()) return false;

        // Regex:
        // - 1 ou mais dígitos
        // - opcionalmente: vírgula OU ponto + 1 ou 2 dígitos
        String regex = "^[0-9]+([.,][0-9]{1,2})?$";

        return texto.matches(regex);
    }
}
