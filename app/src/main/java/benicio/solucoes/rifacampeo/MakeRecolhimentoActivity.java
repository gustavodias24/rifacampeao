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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import benicio.solucoes.rifacampeo.databinding.ActivityMakeRecolhimentoBinding;
import benicio.solucoes.rifacampeo.objects.QueryModelEmpty;
import benicio.solucoes.rifacampeo.objects.RecolheuModel;
import benicio.solucoes.rifacampeo.objects.VendedorModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MakeRecolhimentoActivity extends AppCompatActivity {

    private ActivityMakeRecolhimentoBinding mainBinding;

    private final List<VendedorModel> vendedores = new ArrayList<>();
    private final List<String> nomes = new ArrayList<>();
    private ArrayAdapter<String> adapterNomes;

    private String nomeRecolhedor = "";
    private boolean isRecolhedor = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityMakeRecolhimentoBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // extras safe (evita NullPointer)
        Bundle extras = getIntent() != null ? getIntent().getExtras() : null;
        if (extras != null) {
            nomeRecolhedor = extras.getString("recolhedor", "");
            isRecolhedor = extras.getBoolean("isRecolhedor", false);
        }

        if (isRecolhedor) {
            mainBinding.rbPagamento.setVisibility(View.GONE);
            // garante que o tipo seja recolhimento se você quiser:
            mainBinding.rbRecolhimento.setChecked(true);
        }

        Log.d("MakeRecolhimento", "nomeRecolhedor: " + nomeRecolhedor);

        // Adapter do AutoCompleteTextView (use lista interna do adapter, evita bug de cache)
        adapterNomes = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<>()
        );

        mainBinding.edtVendedor.setAdapter(adapterNomes);
        mainBinding.edtVendedor.setThreshold(1);

        // Carrega vendedores
        carregarVendedores();

        // Preenche data/hora atual (compatível com Android 7)
        String agora = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR"))
                .format(new Date());
        mainBinding.edtDataHora.setText(agora);

        mainBinding.btnConfirmar.setOnClickListener(v -> {
            String valorString = mainBinding.edtValor.getText().toString();
            String vendedorTexto = mainBinding.edtVendedor.getText().toString().trim();

            if (vendedorTexto.isEmpty()) {
                Toast.makeText(this, "Digite ou selecione um Vendedor!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValorMonetarioValido(valorString)) {
                Toast.makeText(this, "Valor inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            String normalizado = valorString.replace(",", ".");
            float valor;
            try {
                valor = Float.parseFloat(normalizado);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Valor inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            int tipo = mainBinding.rbRecolhimento.isChecked() ? 0 : 1;
            if (isRecolhedor) tipo = 0; // se quiser forçar recolhedor sempre recolhimento

            RecolheuModel recolheuModelNovo = new RecolheuModel(
                    mainBinding.edtDataHora.getText().toString(),
                    vendedorTexto,
                    valor,
                    mainBinding.edtObservacoes.getText().toString(),
                    tipo,
                    nomeRecolhedor
            );

            AlertDialog loadingDialog = new AlertDialog.Builder(MakeRecolhimentoActivity.this)
                    .setView(new ProgressBar(MakeRecolhimentoActivity.this))
                    .setCancelable(false)
                    .create();
            loadingDialog.show();

            RetrofitUtils.getApiService().salvar_recolhimento(recolheuModelNovo)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            loadingDialog.dismiss();

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
                            loadingDialog.dismiss();
                            Toast.makeText(MakeRecolhimentoActivity.this,
                                    "Falha na conexão", Toast.LENGTH_SHORT).show();
                        }
                    });
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
                            for (VendedorModel v : vendedores) {
                                String n = safe(v.getNome()).trim();
                                if (!n.isEmpty()) nomes.add(n);
                            }

                            // Atualiza o adapter do jeito certo (resolve “cache vazio”)
                            adapterNomes.clear();
                            adapterNomes.addAll(nomes);
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
        if (texto.isEmpty()) return false;

        // 1+ dígitos, opcionalmente separador + 1-2 dígitos
        String regex = "^[0-9]+([.,][0-9]{1,2})?$";
        return texto.matches(regex);
    }
}