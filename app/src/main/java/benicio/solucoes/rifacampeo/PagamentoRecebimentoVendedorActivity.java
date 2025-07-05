package benicio.solucoes.rifacampeo;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import benicio.solucoes.rifacampeo.adapters.AdapterRelatorio;
import benicio.solucoes.rifacampeo.databinding.ActivityPagamentoRecebimentoVendedorBinding;
import benicio.solucoes.rifacampeo.databinding.ActivityRelatoriosBinding;
import benicio.solucoes.rifacampeo.databinding.LayourtDialogEntradaSaidaBinding;
import benicio.solucoes.rifacampeo.objects.LancamentoModel;
import benicio.solucoes.rifacampeo.objects.QueryModelEmpty;
import benicio.solucoes.rifacampeo.objects.QueryModelVendedorID;
import benicio.solucoes.rifacampeo.objects.RetornoModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PagamentoRecebimentoVendedorActivity extends AppCompatActivity {
    private boolean isUpdating = false;
    Dialog dialogEntradaSaida;
    Bundle bundle;
    private RecyclerView rv;

    private AdapterRelatorio adapterRelatorio;

    private ActivityPagamentoRecebimentoVendedorBinding mainBinding;

    private List<LancamentoModel> lancamentos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityPagamentoRecebimentoVendedorBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        bundle = getIntent().getExtras();

        mainBinding.nameVendedorLancamento.setText(
                "Lançamentos do Vendedor " + bundle.getString("NomeVendedor", "")
        );

        mainBinding.saidaEntrada.setOnClickListener(v -> {
            AlertDialog.Builder b = new AlertDialog.Builder(PagamentoRecebimentoVendedorActivity.this);
            LayourtDialogEntradaSaidaBinding dialogEntradaSaidaBinding = LayourtDialogEntradaSaidaBinding.inflate(getLayoutInflater());

            dialogEntradaSaidaBinding.editTextMoeda.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Não precisa usar
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Não precisa usar
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (isUpdating) return;

                    isUpdating = true;

                    // Remove qualquer caractere não numérico
                    String cleanString = s.toString().replaceAll("[^\\d]", "");

                    try {
                        double parsed = Double.parseDouble(cleanString) / 100.0;

                        // Formata para moeda BRL
                        String formatted = NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(parsed);

                        dialogEntradaSaidaBinding.editTextMoeda.setText(formatted);
                        dialogEntradaSaidaBinding.editTextMoeda.setSelection(formatted.length());

                    } catch (NumberFormatException e) {
                        s.clear();
                    }

                    isUpdating = false;
                }
            });

            dialogEntradaSaidaBinding.cadastrarEntradaSaida.setOnClickListener(v2 -> {

                String valor = dialogEntradaSaidaBinding.editTextMoeda.getText().toString();
                String descricao = dialogEntradaSaidaBinding.editTextDescricaoEntradaSaida.getText().toString();
                Boolean isReceita = dialogEntradaSaidaBinding.radioButtonReceita.isChecked();

                if (valor.isEmpty() || descricao.isEmpty()) {
                    Toast.makeText(this, "Preencha todas as informações.", Toast.LENGTH_SHORT).show();
                } else {

                    // Data atual
                    LocalDate dataAtual = LocalDate.now();
                    String dataFormatada = dataAtual.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                    // Hora atual
                    LocalTime horaAtual = LocalTime.now();
                    String horaFormatada = horaAtual.format(DateTimeFormatter.ofPattern("HH:mm"));

                    RetrofitUtils.getApiService().saveLancamento(new LancamentoModel(
                            valor,
                            descricao,
                            dataFormatada,
                            horaFormatada,
                            UUID.randomUUID().toString(),
                            bundle.getString("VendedorId", ""),
                            isReceita
                    )).enqueue(new Callback<>() {
                        @Override
                        public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(PagamentoRecebimentoVendedorActivity.this, "Salvo com Sucesso!", Toast.LENGTH_SHORT).show();
                                dialogEntradaSaida.dismiss();
                                puxarRelatoiro();
                            } else {
                                Toast.makeText(PagamentoRecebimentoVendedorActivity.this, "Problema de conexão", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<RetornoModel> call, Throwable throwable) {

                        }
                    });
                }


            });

            b.setView(dialogEntradaSaidaBinding.getRoot());
            dialogEntradaSaida = b.create();
            dialogEntradaSaida.show();
        });


        rv = mainBinding.rvLancamento;
        rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setHasFixedSize(true);
        adapterRelatorio = new AdapterRelatorio(lancamentos, this);
        rv.setAdapter(adapterRelatorio);

        puxarRelatoiro();
    }

    private void puxarRelatoiro() {
        RetrofitUtils.getApiService().returnLancamento(4, new QueryModelVendedorID(bundle.getString("VendedorId", ""))).enqueue(new Callback<List<LancamentoModel>>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(Call<List<LancamentoModel>> call, Response<List<LancamentoModel>> response) {
                if (response.isSuccessful()) {
                    lancamentos.clear();
                    lancamentos.addAll(response.body());
                    Collections.reverse(lancamentos);
                    adapterRelatorio.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<LancamentoModel>> call, Throwable throwable) {

            }
        });
    }
}