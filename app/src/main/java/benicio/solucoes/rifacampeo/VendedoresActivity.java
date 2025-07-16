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

import benicio.solucoes.rifacampeo.adapters.AdapterVendedores;
import benicio.solucoes.rifacampeo.databinding.ActivityAdminMasterBinding;
import benicio.solucoes.rifacampeo.databinding.ActivityVendedoresBinding;
import benicio.solucoes.rifacampeo.databinding.LayoutInputVendedorBinding;
import benicio.solucoes.rifacampeo.objects.QueryModelEmpty;
import benicio.solucoes.rifacampeo.objects.RetornoModel;
import benicio.solucoes.rifacampeo.objects.VendedorModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VendedoresActivity extends AppCompatActivity {

    private Dialog dialogVendedor;
    private ActivityVendedoresBinding mainBinding;
    private List<VendedorModel> vendedores = new ArrayList<>();
    private AdapterVendedores adapterVendedores;
    private RecyclerView rvVendedores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityVendedoresBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        mainBinding.novovendedor.setOnClickListener(v -> configuarDialog());

        rvVendedores = mainBinding.rvvendedores;
        configurarRV();
        listarVendedores();

    }

    private void listarVendedores() {
        vendedores.clear();

        RetrofitUtils.getApiService().returnVendedores(1, new QueryModelEmpty()).enqueue(new Callback<List<VendedorModel>>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(Call<List<VendedorModel>> call, Response<List<VendedorModel>> response) {
                if (response.isSuccessful()) {
                    vendedores.addAll(response.body());
                    adapterVendedores.notifyDataSetChanged();
                } else {
                    Toast.makeText(VendedoresActivity.this, "Erro de conexão", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<VendedorModel>> call, Throwable throwable) {

            }
        });
    }

    private void configurarRV() {
        rvVendedores.setLayoutManager(new LinearLayoutManager(this));
        rvVendedores.setHasFixedSize(true);
        adapterVendedores = new AdapterVendedores(vendedores, this);
        rvVendedores.setAdapter(adapterVendedores);
    }

    private void configuarDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(VendedoresActivity.this);

        LayoutInputVendedorBinding inputVendedorBinding = LayoutInputVendedorBinding.inflate(getLayoutInflater());

        inputVendedorBinding.cadatrar.setOnClickListener(v -> {
            if (inputVendedorBinding.edtComissao.getText().toString().isEmpty()) {
                Toast.makeText(this, "Comissão não pode ser vazio", Toast.LENGTH_SHORT).show();
            } else {
                if (inputVendedorBinding.edtSenha.getText().toString().length() != 6) {
                    Toast.makeText(this, "A senha precisa ter 6 dígitos numéricos!", Toast.LENGTH_SHORT).show();
                } else {
                    RetrofitUtils.getApiService().saveVendedores(new VendedorModel(
                            inputVendedorBinding.edtCelular.getText().toString(),
                            inputVendedorBinding.edtNome.getText().toString(),
                            UUID.randomUUID().toString(),
                            inputVendedorBinding.edtSenha.getText().toString(),
                            inputVendedorBinding.edtDespesas.getText().toString(),
                            "",
                            Integer.parseInt(!inputVendedorBinding.edtComissao.getText().toString().isEmpty() ? inputVendedorBinding.edtComissao.getText().toString() : "0"),
                            inputVendedorBinding.radioAtivo.isChecked(),
                            inputVendedorBinding.edtComissao.getText().toString()
                    )).enqueue(new Callback<RetornoModel>() {
                        @Override
                        public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(VendedoresActivity.this, "Cadastrado!", Toast.LENGTH_SHORT).show();
                                listarVendedores();
                                dialogVendedor.dismiss();
                            } else {
                                Toast.makeText(VendedoresActivity.this, "Problema de Conexão!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<RetornoModel> call, Throwable throwable) {

                        }
                    });
                }
            }


        });

        b.setView(inputVendedorBinding.getRoot());
        dialogVendedor = b.create();
        dialogVendedor.show();
    }
}