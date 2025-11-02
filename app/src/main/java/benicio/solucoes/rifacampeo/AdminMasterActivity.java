package benicio.solucoes.rifacampeo;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    // Lista mostrada no adapter
    private final List<BilheteModel> bilhetes = new ArrayList<>();
    // Backup com todos os itens (não filtrados)
    private final List<BilheteModel> bilhetesAll = new ArrayList<>();

    private AdapterBilhetes adapterBilhetes;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityAdminMasterBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Navegação existente
        mainBinding.banner.setOnClickListener(v -> startActivity(new Intent(this, BannersActivity.class)));
        mainBinding.lancardatalimite.setOnClickListener(v -> startActivity(new Intent(this, LancarDataLimiteActivity.class)));
        mainBinding.cadastrarvendedores.setOnClickListener(v -> startActivity(new Intent(this, VendedoresActivity.class)));
        mainBinding.CadastrarRegiao.setOnClickListener(v -> startActivity(new Intent(this, RegioesActivity.class)));
        mainBinding.button3.setOnClickListener(v -> startActivity(new Intent(this, RelatoriosActivity.class)));
        mainBinding.button2.setOnClickListener(v -> startActivity(new Intent(this, ResultadoActivity.class)));

        // Lista
        rv = mainBinding.rvBilhetes;
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setHasFixedSize(true);
        rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        rv.setNestedScrollingEnabled(false); // melhora dentro de ScrollView
        adapterBilhetes = new AdapterBilhetes(this, bilhetes);
        rv.setAdapter(adapterBilhetes);

        // Date pickers para os campos de data
        setupDatePicker(mainBinding.edtDataInicio);
        setupDatePicker(mainBinding.edtDataFim);

        // Botões de filtro
        mainBinding.btnAplicarFiltro.setOnClickListener(v -> aplicarFiltros());
        mainBinding.btnLimparFiltro.setOnClickListener(v -> limparFiltros());

        atualizarLista();
    }

    private void setupDatePicker(EditText target) {
        target.setOnClickListener(v -> {
            final Calendar cal = Calendar.getInstance();
            Date pre = parseDateSafe(target.getText().toString());
            if (pre != null) cal.setTime(pre);

            int y = cal.get(Calendar.YEAR);
            int m = cal.get(Calendar.MONTH);
            int d = cal.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dp = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.YEAR, year);
                c.set(Calendar.MONTH, month);
                c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                target.setText(sdf.format(c.getTime()));
            }, y, m, d);
            dp.show();
        });
    }

    public void atualizarLista() {
        RetrofitUtils.getApiService().returnBilhetes(3, new QueryModelEmpty())
                .enqueue(new Callback<List<BilheteModel>>() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onResponse(Call<List<BilheteModel>> call, Response<List<BilheteModel>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            bilhetesAll.clear();
                            bilhetes.clear();

                            bilhetesAll.addAll(response.body());
                            Collections.reverse(bilhetesAll); // mais recentes primeiro

                            bilhetes.addAll(bilhetesAll);
                            adapterBilhetes.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<BilheteModel>> call, Throwable throwable) {
                        Toast.makeText(AdminMasterActivity.this, "Falha ao carregar bilhetes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void aplicarFiltros() {
        String dataInicioStr = safeStr(mainBinding.edtDataInicio.getText().toString());
        String dataFimStr    = safeStr(mainBinding.edtDataFim.getText().toString());
        String vendedorStr   = safeStr(mainBinding.edtVendedor.getText().toString());
        String idStr         = safeStr(mainBinding.edtId.getText().toString());

        Date dtInicio = parseDateSafe(dataInicioStr);
        Date dtFim    = parseDateSafe(dataFimStr);

        // Ajuste fim do dia para incluir o dia inteiro
        if (dtFim != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(dtFim);
            c.set(Calendar.HOUR_OF_DAY, 23);
            c.set(Calendar.MINUTE, 59);
            c.set(Calendar.SECOND, 59);
            dtFim = c.getTime();
        }

        List<BilheteModel> filtrados = new ArrayList<>();
        for (BilheteModel b : bilhetesAll) {
            // --- Filtro por datas (campo b.getData() em dd/MM/yyyy) ---
            boolean passaData = true;
            Date dataBilhete = parseDateSafe(b.getData()); // espera "dd/MM/yyyy"
            if (dtInicio != null && dataBilhete != null && dataBilhete.before(dtInicio)) {
                passaData = false;
            }
            if (dtFim != null && dataBilhete != null && dataBilhete.after(dtFim)) {
                passaData = false;
            }

            // --- Filtro por vendedor (nome OU documento) ---
            boolean passaVendedor = true;
            if (!TextUtils.isEmpty(vendedorStr)) {
                String vend = vendedorStr.toLowerCase(Locale.ROOT);
                String nome = safeStr(b.getNome_vendedor()).toLowerCase(Locale.ROOT);
                String doc  = safeStr(b.getDocumento_vendedor()).toLowerCase(Locale.ROOT);
                passaVendedor = (nome.contains(vend) || doc.contains(vend));
            }

            // --- Filtro por ID do bilhete (_id) ---
            boolean passaId = true;
            if (!TextUtils.isEmpty(idStr)) {
                String id = idStr.toLowerCase(Locale.ROOT);
                String cur = safeStr(b.get_id()).toLowerCase(Locale.ROOT);
                passaId = cur.contains(id);
            }

            if (passaData && passaVendedor && passaId) {
                filtrados.add(b);
            }
        }

        bilhetes.clear();
        bilhetes.addAll(filtrados);
        adapterBilhetes.notifyDataSetChanged();
    }

    private void limparFiltros() {
        mainBinding.edtDataInicio.setText("");
        mainBinding.edtDataFim.setText("");
        mainBinding.edtVendedor.setText("");
        mainBinding.edtId.setText("");

        bilhetes.clear();
        bilhetes.addAll(bilhetesAll);
        adapterBilhetes.notifyDataSetChanged();
    }

    private Date parseDateSafe(String s) {
        s = safeStr(s);
        if (s.isEmpty()) return null;
        try {
            sdf.setLenient(false);
            return sdf.parse(s);
        } catch (ParseException e) {
            // se vier em outro formato, tente aqui formatos alternativos:
            // return tryAltFormats(s);
            return null;
        }
    }

    private String safeStr(String s) {
        return s == null ? "" : s.trim();
    }
}
