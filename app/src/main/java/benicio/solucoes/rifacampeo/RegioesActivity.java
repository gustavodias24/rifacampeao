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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import benicio.solucoes.rifacampeo.adapters.AdapterRegiao;
import benicio.solucoes.rifacampeo.databinding.ActivityAdminMasterBinding;
import benicio.solucoes.rifacampeo.databinding.ActivityRegioesBinding;
import benicio.solucoes.rifacampeo.databinding.LayoutInputRegiaoBinding;
import benicio.solucoes.rifacampeo.objects.QueryModelEmpty;
import benicio.solucoes.rifacampeo.objects.RegiaoModel;
import benicio.solucoes.rifacampeo.objects.RetornoModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegioesActivity extends AppCompatActivity {

    private ActivityRegioesBinding mainBinding;

    private List<RegiaoModel> regioes = new ArrayList<>();
    private AdapterRegiao adapterRegiao;

    private RecyclerView rv;

    private Dialog novaRegiaoDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityRegioesBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        rv = mainBinding.rvregioes;
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setHasFixedSize(true);
        adapterRegiao = new AdapterRegiao(regioes, this);
        rv.setAdapter(adapterRegiao);

        listarRegioes();

        mainBinding.novaregiao.setOnClickListener(v -> showDialogNovaRegiao());
    }

    private void showDialogNovaRegiao() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);

        LayoutInputRegiaoBinding inputRegiaoBinding = LayoutInputRegiaoBinding.inflate(getLayoutInflater());

        inputRegiaoBinding.cadatrar.setOnClickListener(v -> {
            String nome = inputRegiaoBinding.edtNome.getText().toString();
            if (nome.isEmpty()) {
                Toast.makeText(this, "Nome é obrigatório", Toast.LENGTH_SHORT).show();
            } else {
                RetrofitUtils.getApiService().saveRegiao(new RegiaoModel(UUID.randomUUID().toString(), nome)).enqueue(new Callback<RetornoModel>() {
                    @Override
                    public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(RegioesActivity.this, "Cadastro com sucesso!", Toast.LENGTH_SHORT).show();
                            novaRegiaoDialog.dismiss();
                            listarRegioes();
                        } else {
                            Toast.makeText(RegioesActivity.this, "Problema de conexão", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<RetornoModel> call, Throwable throwable) {

                    }
                });
            }
        });

        b.setView(inputRegiaoBinding.getRoot());
        novaRegiaoDialog = b.create();
        novaRegiaoDialog.show();
    }


    private void listarRegioes() {
        regioes.clear();
        RetrofitUtils.getApiService().returnRegioes(2, new QueryModelEmpty()).enqueue(new Callback<List<RegiaoModel>>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(Call<List<RegiaoModel>> call, Response<List<RegiaoModel>> response) {
                if (response.isSuccessful()) {
                    regioes.addAll(response.body());
                    adapterRegiao.notifyDataSetChanged();
                } else {
                    Toast.makeText(RegioesActivity.this, "Problema de conexão", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<RegiaoModel>> call, Throwable throwable) {

            }
        });
    }


}