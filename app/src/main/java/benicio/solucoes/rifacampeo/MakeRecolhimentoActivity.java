package benicio.solucoes.rifacampeo;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import benicio.solucoes.rifacampeo.databinding.ActivityMakeRecolhimentoBinding;
import benicio.solucoes.rifacampeo.databinding.ActivityVendedoresBinding;
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
    String vendedorSelecionado = "Selecione";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityMakeRecolhimentoBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        adapterNomes = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nomes);
        carregarVendedores();
        mainBinding.spVendedor.setAdapter(adapterNomes);

        String agora = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        mainBinding.edtDataHora.setText(agora);


        mainBinding.btnConfirmar.setOnClickListener(v -> {
            String valorString = mainBinding.edtValor.getText().toString();

            if (isValorMonetarioValido(valorString)) {
                if (!vendedorSelecionado.equals("Selecione")) {
                    String normalizado = valorString.replace(",", ".");
                    float valor = Float.parseFloat(normalizado);

                    RecolheuModel recolheuModelNovo = new RecolheuModel(
                            mainBinding.edtDataHora.getText().toString(),
                            vendedorSelecionado,
                            valor,
                            mainBinding.edtObservacoes.getText().toString(),
                            mainBinding.rbRecolhimento.isChecked() ? 0 : 1
                    );

                    // ---------- AlertDialog de carregando ----------
                    AlertDialog loadingDialog = new AlertDialog.Builder(MakeRecolhimentoActivity.this)
                            .setView(new ProgressBar(MakeRecolhimentoActivity.this))
                            .setCancelable(false)
                            .create();
                    loadingDialog.show();
                    // ------------------------------------------------

                    RetrofitUtils.getApiService().salvar_recolhimento(recolheuModelNovo).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            loadingDialog.dismiss(); // fecha o loading

                            if (response.isSuccessful()) {
                                Toast.makeText(MakeRecolhimentoActivity.this, "Recolhimento Registrado", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(MakeRecolhimentoActivity.this, "Erro ao registrar (código " + response.code() + ")", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable throwable) {
                            loadingDialog.dismiss(); // fecha o loading
                            Toast.makeText(MakeRecolhimentoActivity.this, "Falha na conexão", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(this, "Selecione um Vendedor!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Valor inválido", Toast.LENGTH_SHORT).show();
            }

        });

        mainBinding.spVendedor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                vendedorSelecionado = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }

    private void carregarVendedores() {
        RetrofitUtils.getApiService().returnVendedores(1, new QueryModelEmpty())
                .enqueue(new Callback<List<VendedorModel>>() {
                    @Override
                    public void onResponse(Call<List<VendedorModel>> call, Response<List<VendedorModel>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            vendedores.clear();
                            vendedores.addAll(response.body());

                            // Recria as listas mantendo "Todos" como primeiro item
                            nomes.clear();
                            nomes.add("Selecione");

                            for (VendedorModel v : vendedores) {
                                nomes.add(safe(v.getNome()));
                            }

                            adapterNomes.notifyDataSetChanged();
                        } else {
                            Toast.makeText(MakeRecolhimentoActivity.this, "Erro de conexão ao carregar vendedores", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<VendedorModel>> call, Throwable t) {
                        Toast.makeText(MakeRecolhimentoActivity.this, "Falha na API de vendedores", Toast.LENGTH_SHORT).show();
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