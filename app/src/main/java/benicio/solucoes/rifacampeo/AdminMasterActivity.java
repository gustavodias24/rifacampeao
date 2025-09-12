package benicio.solucoes.rifacampeo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import benicio.solucoes.rifacampeo.adapters.AdapterBilhetes;
import benicio.solucoes.rifacampeo.databinding.ActivityAdminMasterBinding;
import benicio.solucoes.rifacampeo.objects.BilheteModel;
import benicio.solucoes.rifacampeo.objects.QueryModelEmpty;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminMasterActivity extends AppCompatActivity {

    private ActivityAdminMasterBinding mainBinding;
    private RecyclerView rv;
    private List<BilheteModel> bilhetes = new ArrayList<>();
    private AdapterBilhetes adapterBilhetes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityAdminMasterBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        mainBinding.banner.setOnClickListener(v -> startActivity(new Intent(this, BannersActivity.class)));
        mainBinding.lancardatalimite.setOnClickListener(v -> startActivity(new Intent(this, LancarDataLimiteActivity.class)));
        mainBinding.cadastrarvendedores.setOnClickListener(v -> startActivity(new Intent(this, VendedoresActivity.class)));
        mainBinding.CadastrarRegiao.setOnClickListener(v -> startActivity(new Intent(this, RegioesActivity.class)));
        mainBinding.button3.setOnClickListener(v -> startActivity(new Intent(this, RelatoriosActivity.class)));
        mainBinding.button2.setOnClickListener(v -> startActivity(new Intent(this, ResultadoActivity.class)));

        rv = mainBinding.rvBilhetes;
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setHasFixedSize(true);
        rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapterBilhetes = new AdapterBilhetes(this, bilhetes);
        rv.setAdapter(adapterBilhetes);

        atualizarLista();
    }

    public void atualizarLista() {
        RetrofitUtils.getApiService().returnBilhetes(3, new QueryModelEmpty()).enqueue(new Callback<List<BilheteModel>>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(Call<List<BilheteModel>> call, Response<List<BilheteModel>> response) {
                if (response.isSuccessful()) {
                    bilhetes.clear();
                    bilhetes.addAll(response.body());
                    Collections.reverse(bilhetes);
                    adapterBilhetes.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<BilheteModel>> call, Throwable throwable) {

            }
        });
    }

}