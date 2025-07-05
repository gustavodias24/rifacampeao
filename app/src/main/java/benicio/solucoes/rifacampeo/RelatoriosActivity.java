package benicio.solucoes.rifacampeo;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import benicio.solucoes.rifacampeo.adapters.AdapterRelatorio;
import benicio.solucoes.rifacampeo.databinding.ActivityRelatoriosBinding;
import benicio.solucoes.rifacampeo.databinding.ActivityVendedoresBinding;
import benicio.solucoes.rifacampeo.databinding.LayourtDialogEntradaSaidaBinding;
import benicio.solucoes.rifacampeo.objects.LancamentoModel;
import benicio.solucoes.rifacampeo.objects.QueryModelEmpty;
import benicio.solucoes.rifacampeo.objects.RetornoModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RelatoriosActivity extends AppCompatActivity {
    Dialog dialogEntradaSaida;
    private ActivityRelatoriosBinding mainBinding;

    private RecyclerView rv;
    private AdapterRelatorio adapterRelatorio;
    private List<LancamentoModel> lancamentos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityRelatoriosBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

//        mainBinding.saidaEntrada.setOnClickListener(v -> {
//            AlertDialog.Builder b = new AlertDialog.Builder(RelatoriosActivity.this);
//            LayourtDialogEntradaSaidaBinding dialogEntradaSaidaBinding = LayourtDialogEntradaSaidaBinding.inflate(getLayoutInflater());
//
//            dialogEntradaSaidaBinding.cadastrarEntradaSaida.setOnClickListener(v2 -> {
//                //String valor = dialogEntradaSaidaBinding.editValorEntradaSaida.getText().toString();
//                String descricao = dialogEntradaSaidaBinding.editTextDescricaoEntradaSaida.getText().toString();
//
//                if (valor.isEmpty() || descricao.isEmpty()) {
//                    Toast.makeText(this, "Preencha todas as informações.", Toast.LENGTH_SHORT).show();
//                } else {
//
//                    // Data atual
//                    LocalDate dataAtual = LocalDate.now();
//                    String dataFormatada = dataAtual.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
//
//                    // Hora atual
//                    LocalTime horaAtual = LocalTime.now();
//                    String horaFormatada = horaAtual.format(DateTimeFormatter.ofPattern("HH:mm"));
//
//                    RetrofitUtils.getApiService().saveLancamento(new LancamentoModel(UUID.randomUUID().toString(), valor, descricao, dataFormatada, horaFormatada)).enqueue(new Callback<RetornoModel>() {
//                        @Override
//                        public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
//                            if (response.isSuccessful()) {
//                                Toast.makeText(RelatoriosActivity.this, "Salvo com Sucesso!", Toast.LENGTH_SHORT).show();
//                                dialogEntradaSaida.dismiss();
//                                puxarRelatoiro();
//                            } else {
//                                Toast.makeText(RelatoriosActivity.this, "Problema de conexão", Toast.LENGTH_SHORT).show();
//                            }
//                        }
//
//                        @Override
//                        public void onFailure(Call<RetornoModel> call, Throwable throwable) {
//
//                        }
//                    });
//                }
//
//
//            });
//
//            b.setView(dialogEntradaSaidaBinding.getRoot());
//            dialogEntradaSaida = b.create();
//            dialogEntradaSaida.show();
//
//        });

        rv = mainBinding.rvLancamento;
        rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setHasFixedSize(true);
        adapterRelatorio = new AdapterRelatorio(lancamentos, this);
        rv.setAdapter(adapterRelatorio);

        puxarRelatoiro();
    }


    private void puxarRelatoiro() {
//        RetrofitUtils.getApiService().returnLancamento(4, new QueryModelEmpty()).enqueue(new Callback<List<LancamentoModel>>() {
//            @SuppressLint("NotifyDataSetChanged")
//            @Override
//            public void onResponse(Call<List<LancamentoModel>> call, Response<List<LancamentoModel>> response) {
//                if (response.isSuccessful()) {
//                    lancamentos.clear();
//                    lancamentos.addAll(response.body());
//                    Collections.reverse(lancamentos);
//                    adapterRelatorio.notifyDataSetChanged();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<List<LancamentoModel>> call, Throwable throwable) {
//
//            }
//        });
    }
}