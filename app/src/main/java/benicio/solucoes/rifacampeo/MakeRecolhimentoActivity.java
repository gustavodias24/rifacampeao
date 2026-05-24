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

import java.text.Normalizer;
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

        Bundle extras = getIntent() != null ? getIntent().getExtras() : null;
        if (extras != null) {
            nomeRecolhedor = extras.getString("recolhedor", "");
            isRecolhedor = extras.getBoolean("isRecolhedor", false);
        }

        if (isRecolhedor) {
            mainBinding.rbPagamento.setVisibility(View.GONE);
            mainBinding.rbRecolhimento.setChecked(true);
        }

        Log.d("MakeRecolhimento", "nomeRecolhedor: " + nomeRecolhedor);

        adapterNomes = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<>()
        );

        mainBinding.edtVendedor.setAdapter(adapterNomes);
        mainBinding.edtVendedor.setThreshold(1);

        carregarVendedores();

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

            if (nomes.isEmpty()) {
                new AlertDialog.Builder(MakeRecolhimentoActivity.this)
                        .setTitle("Atenção")
                        .setMessage("A lista de vendedores ainda não foi carregada. Tente novamente.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            String vendedorEncontrado = encontrarNomeVendedorNaLista(vendedorTexto);

            if (vendedorEncontrado == null) {
                new AlertDialog.Builder(MakeRecolhimentoActivity.this)
                        .setTitle("Vendedor não encontrado")
                        .setMessage("O nome do vendedor informado não existe. Selecione um vendedor da lista.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            mainBinding.edtVendedor.requestFocus();
                            mainBinding.edtVendedor.showDropDown();
                        })
                        .show();

                return;
            }

            vendedorTexto = vendedorEncontrado;

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

            if (isRecolhedor) {
                tipo = 0;
            }

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
                                Toast.makeText(
                                        MakeRecolhimentoActivity.this,
                                        "Recolhimento Registrado",
                                        Toast.LENGTH_SHORT
                                ).show();

                                finish();
                            } else {
                                Toast.makeText(
                                        MakeRecolhimentoActivity.this,
                                        "Erro ao registrar (código " + response.code() + ")",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable throwable) {
                            loadingDialog.dismiss();

                            Toast.makeText(
                                    MakeRecolhimentoActivity.this,
                                    "Falha na conexão",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
        });
    }

    private void carregarVendedores() {
        RetrofitUtils.getApiService().returnVendedores(1, new QueryModelEmpty())
                .enqueue(new Callback<List<VendedorModel>>() {
                    @Override
                    public void onResponse(
                            Call<List<VendedorModel>> call,
                            Response<List<VendedorModel>> response
                    ) {
                        if (response.isSuccessful() && response.body() != null) {

                            vendedores.clear();
                            vendedores.addAll(response.body());

                            nomes.clear();

                            for (VendedorModel vendedor : vendedores) {
                                String nome = safe(vendedor.getNome()).trim();

                                if (!nome.isEmpty()) {
                                    nomes.add(nome);
                                }
                            }

                            adapterNomes.clear();
                            adapterNomes.addAll(nomes);
                            adapterNomes.notifyDataSetChanged();

                        } else {
                            Toast.makeText(
                                    MakeRecolhimentoActivity.this,
                                    "Erro de conexão ao carregar vendedores",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<VendedorModel>> call, Throwable t) {
                        Toast.makeText(
                                MakeRecolhimentoActivity.this,
                                "Falha na API de vendedores",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private String encontrarNomeVendedorNaLista(String textoDigitado) {
        String textoDigitadoNormalizado = normalizarTexto(textoDigitado);

        for (String nome : nomes) {
            String nomeNormalizado = normalizarTexto(nome);

            if (nomeNormalizado.equals(textoDigitadoNormalizado)) {
                return nome;
            }
        }

        return null;
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }

        String textoLimpo = texto.trim().replaceAll("\\s+", " ");

        String textoSemAcentos = Normalizer.normalize(textoLimpo, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        return textoSemAcentos.toLowerCase(Locale.ROOT);
    }

    private String safe(String texto) {
        return texto == null ? "" : texto;
    }

    private boolean isValorMonetarioValido(String texto) {
        if (texto == null) {
            return false;
        }

        texto = texto.trim();

        if (texto.isEmpty()) {
            return false;
        }

        String regex = "^[0-9]+([.,][0-9]{1,2})?$";

        return texto.matches(regex);
    }
}