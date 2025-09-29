package benicio.solucoes.rifacampeo;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import benicio.solucoes.rifacampeo.adapters.GanhadorAdapter;
import benicio.solucoes.rifacampeo.databinding.ActivityDatasAnterioresSorteioBinding;
import benicio.solucoes.rifacampeo.databinding.ActivityResultadoBinding;
import benicio.solucoes.rifacampeo.objects.GanhadorModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DatasAnterioresSorteioActivity extends AppCompatActivity {


    ActivityDatasAnterioresSorteioBinding mainBinding;
    private GanhadorAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityDatasAnterioresSorteioBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        adapter = new GanhadorAdapter();
        mainBinding.recyclerDatas.setLayoutManager(new LinearLayoutManager(this));
        mainBinding.recyclerDatas.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mainBinding.recyclerDatas.setAdapter(adapter);

        // 🔎 busca
        mainBinding.etBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        carregarGanhadores();
    }

    private void carregarGanhadores() {
        RetrofitUtils.getApiService().ganhadores()
                .enqueue(new Callback<List<GanhadorModel>>() {
                    @Override
                    public void onResponse(Call<List<GanhadorModel>> call, Response<List<GanhadorModel>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            adapter.setItems(response.body());
                        } else {
                            Toast.makeText(DatasAnterioresSorteioActivity.this,
                                    "Falha ao carregar: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<List<GanhadorModel>> call, Throwable t) {
                        Toast.makeText(DatasAnterioresSorteioActivity.this,
                                "Erro de rede: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}