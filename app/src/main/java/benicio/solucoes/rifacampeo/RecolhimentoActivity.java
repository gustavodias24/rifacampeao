package benicio.solucoes.rifacampeo;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import benicio.solucoes.rifacampeo.adapters.AdapterRecolhimento;
import benicio.solucoes.rifacampeo.databinding.ActivityRecolhimentoBinding;
import benicio.solucoes.rifacampeo.objects.QueryModelEmpty;
import benicio.solucoes.rifacampeo.objects.RecolheuModel;
import benicio.solucoes.rifacampeo.objects.RecolhimentoResponse;
import benicio.solucoes.rifacampeo.objects.VendedorModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecolhimentoActivity extends AppCompatActivity {

    private ActivityRecolhimentoBinding mainBinding;

    private final List<RecolheuModel> lista_recolhimento = new ArrayList<>();
    private AdapterRecolhimento adapterRecolhimento;

    private final List<VendedorModel> vendedores = new ArrayList<>();

    private String nomeRecolhedor = "";
    private SharedPreferences prefs;
    private SharedPreferences.Editor edt;

    // =======================
    // AUTOCOMPLETE RECOLHEDOR
    // =======================
    private ArrayAdapter<String> adapterRecolhedor;
    private final List<String> recolhedoresAll = new ArrayList<>();
    private ColorStateList recolhedorTintNormal;
    private boolean recolhedorBloqueado = false;

    // =====================
    // AUTOCOMPLETE VENDEDOR
    // =====================
    private ArrayAdapter<String> adapterNomes;
    private final List<String> vendedoresAll = new ArrayList<>();
    private ColorStateList vendedorTintNormal;
    private boolean vendedorBloqueado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityRecolhimentoBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // =======================
        // SETUP AUTOCOMPLETE RECOLHEDOR
        // =======================
        adapterRecolhedor = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<>()
        );
        mainBinding.edtRecolhedor.setAdapter(adapterRecolhedor);
        mainBinding.edtRecolhedor.setThreshold(0);
        configurarAutocompleteRecolhedor();

        // =====================
        // SETUP AUTOCOMPLETE VENDEDOR
        // =====================
        adapterNomes = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<>()
        );
        mainBinding.edtVendedor.setAdapter(adapterNomes);
        mainBinding.edtVendedor.setThreshold(0);
        configurarAutocompleteVendedor();

        configurarRV();

        prefs = getSharedPreferences("rprefs", MODE_PRIVATE);
        edt = prefs.edit();

        nomeRecolhedor = prefs.getString("recolhedor", "");
        mainBinding.edtRecolhedor.setText(nomeRecolhedor);

        Bundle b = getIntent().getExtras();
        boolean modoRecolhedor = b != null && b.getBoolean("recolhedor", false);

        if (!modoRecolhedor) {
            //mainBinding.textRecolhedor.setVisibility(View.GONE);
            mainBinding.edtRecolhedor.setVisibility(View.GONE);
            mainBinding.button9.setVisibility(View.GONE);


            // se escondeu o recolhedor, garante que ele não bloqueia nada
            //setBloqueioRecolhedor(false);
        } else {
            // ✅ Somente leitura (não permite digitar)
            mainBinding.edtRecolhedor.setKeyListener(null);
            mainBinding.edtRecolhedor.setCursorVisible(false);

            // (opcional) evita colar texto
            mainBinding.edtRecolhedor.setLongClickable(false);
            mainBinding.edtRecolhedor.setTextIsSelectable(false);
            mainBinding.btnGerarRelatorio.setVisibility(View.GONE);
        }

        // carrega vendedores via API (preenche adapter + valida)
        carregarVendedores();

        mainBinding.button9.setOnClickListener(v -> {
            if (!validarRecolhedor(true)) return;
            //if (!validarVendedor(true)) return;

            nomeRecolhedor = mainBinding.edtRecolhedor.getText().toString().trim();
            edt.putString("recolhedor", nomeRecolhedor).apply();
            Toast.makeText(this, "Recolhedor Salvo!", Toast.LENGTH_SHORT).show();
        });

        mainBinding.btnadd.setOnClickListener(v -> {
//            if (!validarRecolhedor(true)) return;
            //if (!validarVendedor(true)) return;

            nomeRecolhedor = mainBinding.edtRecolhedor.getText().toString().trim();
            if (nomeRecolhedor.isEmpty()) {
                Toast.makeText(this, "Insira um nome de recolhedor!", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent i = new Intent(this, MakeRecolhimentoActivity.class);
            i.putExtra("recolhedor", nomeRecolhedor);
            i.putExtra("isRecolhedor", modoRecolhedor);
            startActivity(i);
        });

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String dataAtual = sdf.format(new Date());
        mainBinding.etDataInicio.setText(dataAtual);
        mainBinding.etDataFim.setText(dataAtual);

        mainBinding.btnFiltrar.setOnClickListener(v -> {
            filtarAction();
        });

        mainBinding.etDataInicio.setOnClickListener(v -> mostrarDatePicker(mainBinding.etDataInicio));
        mainBinding.etDataFim.setOnClickListener(v -> mostrarDatePicker(mainBinding.etDataFim));

        mainBinding.btnGerarRelatorio.setOnClickListener(v -> gerarPdfRecolhimentos(lista_recolhimento));
    }

    private void filtarAction(){
        if (!validarRecolhedor(true)) return;
        //if (!validarVendedor(true)) return;

        String vendedor = mainBinding.edtVendedor.getText().toString().trim();
        if (vendedor.isEmpty() || vendedor.equalsIgnoreCase("Todos")) vendedor = null;

        String dataInicio = mainBinding.etDataInicio.getText().toString().trim();
        String dataFim = mainBinding.etDataFim.getText().toString().trim();
        if (dataInicio.isEmpty()) dataInicio = null;
        if (dataFim.isEmpty()) dataFim = null;

        String recolhedorFiltro = mainBinding.edtRecolhedor.getText().toString().trim();
        if (recolhedorFiltro.isEmpty()) recolhedorFiltro = null;

        Integer tipo = null;

        filtrarRecolhimentos(vendedor, dataInicio, dataFim, tipo, recolhedorFiltro);
    }

    // =======================
    // BLOQUEIO UNIFICADO
    // =======================
//    private void setBloqueioRecolhedor(boolean bloquear) {
//        if (recolhedorBloqueado == bloquear) return;
//        recolhedorBloqueado = bloquear;
////        atualizarBloqueioTela();
//    }

//    private void setBloqueioVendedor(boolean bloquear) {
//        if (vendedorBloqueado == bloquear) return;
//        vendedorBloqueado = bloquear;
//        atualizarBloqueioTela();
//    }

//    private void atualizarBloqueioTela() {
//        boolean bloquear = recolhedorBloqueado || vendedorBloqueado;
//
//        mainBinding.btnFiltrar.setEnabled(!bloquear);
//        mainBinding.btnadd.setEnabled(!bloquear);
//        mainBinding.button9.setEnabled(!bloquear);
//
//        // não desabilitar edtRecolhedor nem edtVendedor (pra conseguir corrigir)
//        mainBinding.etDataInicio.setEnabled(!bloquear);
//        mainBinding.etDataFim.setEnabled(!bloquear);
//        mainBinding.btnGerarRelatorio.setEnabled(!bloquear);
//
//        float alpha = bloquear ? 0.5f : 1f;
//
//        mainBinding.etDataInicio.setAlpha(alpha);
//        mainBinding.etDataFim.setAlpha(alpha);
//
//        mainBinding.btnFiltrar.setAlpha(alpha);
//        mainBinding.btnadd.setAlpha(alpha);
//        mainBinding.button9.setAlpha(alpha);
//        mainBinding.btnGerarRelatorio.setAlpha(alpha);
//    }

    // =======================
    // DATE PICKER
    // =======================
    private void mostrarDatePicker(EditText campoAlvo) {
        final Calendar cal = Calendar.getInstance();
        int ano = cal.get(Calendar.YEAR);
        int mes = cal.get(Calendar.MONTH);
        int dia = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String diaFmt = String.format(Locale.getDefault(), "%02d", dayOfMonth);
                    String mesFmt = String.format(Locale.getDefault(), "%02d", month + 1);
                    String anoFmt = String.format(Locale.getDefault(), "%04d", year);
                    campoAlvo.setText(diaFmt + "/" + mesFmt + "/" + anoFmt);
                },
                ano, mes, dia
        );

        dp.show();
    }

    // =======================
    // FILTRO
    // =======================
    private void filtrarRecolhimentos(String vendedor,
                                      String dataInicio,
                                      String dataFim,
                                      Integer tipo,
                                      String recolhedorFiltro) {

        lista_recolhimento.clear();
        showLoading(true);

        String vendedorParam = (vendedor != null && !vendedor.trim().isEmpty()) ? vendedor.trim() : null;
        String dataInicioParam = (dataInicio != null && !dataInicio.trim().isEmpty()) ? dataInicio.trim() : null;
        String dataFimParam = (dataFim != null && !dataFim.trim().isEmpty()) ? dataFim.trim() : null;

        RetrofitUtils.getApiService()
                .retornar_recolhimento(
                        vendedorParam,
                        dataInicioParam,
                        dataFimParam,
                        tipo,
                        999999999,
                        1
                )
                .enqueue(new Callback<RecolhimentoResponse>() {
                    @Override
                    public void onResponse(Call<RecolhimentoResponse> call,
                                           Response<RecolhimentoResponse> response) {
                        showLoading(false);

                        if (response.isSuccessful() && response.body() != null) {

                            lista_recolhimento.clear();
                            List<RecolheuModel> itensApi = response.body().getItens();

                            Bundle b = getIntent().getExtras();
                            boolean modoRecolhedor = b != null && b.getBoolean("recolhedor", false);

                            String recolhedorFiltroNorm = recolhedorFiltro != null ? recolhedorFiltro.trim() : "";
                            String nomeRecolhedorNorm = nomeRecolhedor != null ? nomeRecolhedor.trim() : "";

                            if (!modoRecolhedor && recolhedorFiltroNorm.isEmpty()) {
                                lista_recolhimento.addAll(itensApi);
                            } else {
                                for (RecolheuModel r : itensApi) {
                                    String rec = r.getRecolhedor() == null ? "" : r.getRecolhedor().trim();

                                    boolean okTela = true;
                                    if (modoRecolhedor && !nomeRecolhedorNorm.isEmpty()) {
                                        okTela = rec.equalsIgnoreCase(nomeRecolhedorNorm);
                                    }

                                    boolean okFiltro = true;
                                    if (!recolhedorFiltroNorm.isEmpty()) {
                                        okFiltro = rec.equalsIgnoreCase(recolhedorFiltroNorm);
                                    }

                                    if (okTela && okFiltro) {
                                        lista_recolhimento.add(r);
                                    }
                                }
                            }

                            adapterRecolhimento.notifyDataSetChanged();

                            Toast.makeText(RecolhimentoActivity.this,
                                    "Filtro aplicado (" + lista_recolhimento.size() + " itens)",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(RecolhimentoActivity.this,
                                    "Nenhum resultado encontrado",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<RecolhimentoResponse> call, Throwable throwable) {
                        showLoading(false);
                        Toast.makeText(RecolhimentoActivity.this,
                                "Erro ao aplicar filtro",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        listarRecolhimentos();
    }

    private void listarRecolhimentos() {
        showLoading(true);

        RetrofitUtils.getApiService().retornar_recolhimento(null, null, null, null, 999999999, 1)
                .enqueue(new Callback<RecolhimentoResponse>() {
                    @Override
                    public void onResponse(Call<RecolhimentoResponse> call, Response<RecolhimentoResponse> response) {
                        showLoading(false);

                        if (response.isSuccessful() && response.body() != null) {

                            lista_recolhimento.clear();
                            List<RecolheuModel> itensApi = response.body().getItens();

                            Bundle b = getIntent().getExtras();
                            boolean modoRecolhedor = b != null && b.getBoolean("recolhedor", false);

                            if (!modoRecolhedor) {
                                lista_recolhimento.addAll(itensApi);
                            } else {
                                for (RecolheuModel recolhe : itensApi) {
                                    if (recolhe.getRecolhedor() != null &&
                                            recolhe.getRecolhedor().trim().equalsIgnoreCase(nomeRecolhedor.trim())) {
                                        lista_recolhimento.add(recolhe);
                                    }
                                }
                            }

                            adapterRecolhimento.notifyDataSetChanged();

                            // atualiza lista de recolhedores
                            atualizarAutocompleteRecolhedor(itensApi);
                        }
                    }

                    @Override
                    public void onFailure(Call<RecolhimentoResponse> call, Throwable throwable) {
                        showLoading(false);
                    }
                });
    }

    // =======================
    // ATUALIZA AUTOCOMPLETE RECOLHEDOR
    // =======================
    private void atualizarAutocompleteRecolhedor(List<RecolheuModel> itens) {
        LinkedHashSet<String> set = new LinkedHashSet<>();

        for (RecolheuModel r : itens) {
            String rec = r.getRecolhedor();
            if (rec != null) {
                rec = rec.trim();
                if (!rec.isEmpty()) set.add(rec);
            }
        }

        recolhedoresAll.clear();
        recolhedoresAll.addAll(set);

        adapterRecolhedor.clear();
        adapterRecolhedor.addAll(recolhedoresAll);
        adapterRecolhedor.notifyDataSetChanged();

        CharSequence atual = mainBinding.edtRecolhedor.getText();
//        adapterRecolhedor.getFilter().filter(atual, count -> {
//            validarRecolhedor(true);
//            if (mainBinding.edtRecolhedor.hasFocus()) {
//                mainBinding.edtRecolhedor.post(() -> mainBinding.edtRecolhedor.showDropDown());
//            }
//        });
    }

    private void configurarRV() {
        mainBinding.recolhimentorv.setLayoutManager(new LinearLayoutManager(this));
        mainBinding.recolhimentorv.setHasFixedSize(true);
        adapterRecolhimento = new AdapterRecolhimento(lista_recolhimento, this);
        mainBinding.recolhimentorv.setAdapter(adapterRecolhimento);
    }

    // =======================
    // CARREGA VENDEDORES (CORRIGIDO PRA NAO CACHEAR VAZIO)
    // =======================
    private void carregarVendedores() {
        RetrofitUtils.getApiService().returnVendedores(1, new QueryModelEmpty())
                .enqueue(new Callback<List<VendedorModel>>() {
                    @Override
                    public void onResponse(Call<List<VendedorModel>> call, Response<List<VendedorModel>> response) {
                        if (response.isSuccessful() && response.body() != null) {

                            vendedores.clear();
                            vendedores.addAll(response.body());

                            // lista fonte
                            vendedoresAll.clear();
                            vendedoresAll.add("Todos");

                            for (VendedorModel v : vendedores) {
                                String nome = safe(v.getNome()).trim();
                                if (!nome.isEmpty()) vendedoresAll.add(nome);
                            }

                            // atualiza adapter do jeito certo
                            adapterNomes.clear();
                            adapterNomes.addAll(vendedoresAll);
                            adapterNomes.notifyDataSetChanged();

                            CharSequence atual = mainBinding.edtVendedor.getText();
                            adapterNomes.getFilter().filter(atual, count -> {
                                //validarVendedor(true);
                                if (mainBinding.edtVendedor.hasFocus()) {
                                    mainBinding.edtVendedor.post(() -> mainBinding.edtVendedor.showDropDown());
                                }
                            });

                        } else {
                            Toast.makeText(RecolhimentoActivity.this,
                                    "Erro de conexão ao carregar vendedores",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<VendedorModel>> call, Throwable t) {
                        Toast.makeText(RecolhimentoActivity.this,
                                "Falha na API de vendedores",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    private void showLoading(boolean show) {
        if (show) {
            mainBinding.progressRecolhimento.setVisibility(View.VISIBLE);
            mainBinding.recolhimentorv.setVisibility(View.GONE);
        } else {
            mainBinding.progressRecolhimento.setVisibility(View.GONE);
            mainBinding.recolhimentorv.setVisibility(View.VISIBLE);
        }
    }

    // ==========================================================
    // AUTOCOMPLETE RECOLHEDOR (VALIDACAO + BLOQUEIO)
    // ==========================================================
    private void configurarAutocompleteRecolhedor() {
        recolhedorTintNormal = ViewCompat.getBackgroundTintList(mainBinding.edtRecolhedor);

        mainBinding.edtRecolhedor.setOnClickListener(v -> mostrarDropDownRecolhedor());

        mainBinding.edtRecolhedor.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mostrarDropDownRecolhedor();
            } else {
                validarRecolhedor(true);
            }
        });

        mainBinding.edtRecolhedor.setOnItemClickListener((parent, view, position, id) -> {
            String selecionado = (String) parent.getItemAtPosition(position);
            if (selecionado != null) {
                selecionado = selecionado.trim();
                mainBinding.edtRecolhedor.setText(selecionado);
                mainBinding.edtRecolhedor.setSelection(selecionado.length());
                nomeRecolhedor = selecionado;

//                // aplicarErroRecolhedor(false);(false);
                //setBloqueioRecolhedor(false);
            }
        });

        mainBinding.edtRecolhedor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (mainBinding.edtRecolhedor.getVisibility() != View.VISIBLE) return;

                String txt = s.toString().trim();

                if (recolhedoresAll.isEmpty()) {
                    // aplicarErroRecolhedor(false);(false);
                    //setBloqueioRecolhedor(false);
                    return;
                }

                if (txt.isEmpty()) {
                    // aplicarErroRecolhedor(false);(false);
                    //setBloqueioRecolhedor(false);
                    mostrarDropDownRecolhedor();
                    return;
                }

                boolean temSugestao = temSugestaoRecolhedor(txt);

                if (temSugestao) {
                    // aplicarErroRecolhedor(false);(false);
                    //setBloqueioRecolhedor(false);
                } else {
                    // aplicarErroRecolhedor(false);(true);
                    //setBloqueioRecolhedor(true);
                }

                mostrarDropDownRecolhedor();
            }
        });
    }

    private void mostrarDropDownRecolhedor() {
        if (mainBinding.edtRecolhedor.getVisibility() != View.VISIBLE) return;
        if (adapterRecolhedor == null || adapterRecolhedor.getCount() == 0) return;

        mainBinding.edtRecolhedor.requestFocus();
        CharSequence atual = mainBinding.edtRecolhedor.getText();

        adapterRecolhedor.getFilter().filter(atual, count -> {
            mainBinding.edtRecolhedor.post(() -> {
                mainBinding.edtRecolhedor.dismissDropDown();
                mainBinding.edtRecolhedor.showDropDown();
            });
        });
    }

    private boolean validarRecolhedor(boolean exigirExato) {
        if (mainBinding.edtRecolhedor.getVisibility() != View.VISIBLE) {
            //setBloqueioRecolhedor(false);
            return true;
        }

        String txt = mainBinding.edtRecolhedor.getText().toString().trim();

        if (recolhedoresAll.isEmpty()) return true;

        if (txt.isEmpty()) {
            // aplicarErroRecolhedor(false);(false);
            //setBloqueioRecolhedor(false);
            return true;
        }

        boolean ok = exigirExato ? existeRecolhedor(txt) : temSugestaoRecolhedor(txt);

        // aplicarErroRecolhedor(false);(!ok);
        //setBloqueioRecolhedor(!ok);

        if (!ok) {
            mainBinding.edtRecolhedor.requestFocus();
            mostrarDropDownRecolhedor();
        } else {
            nomeRecolhedor = txt;
        }

        return ok;
    }

    private boolean existeRecolhedor(String txt) {
        String t = txt.trim().toLowerCase(Locale.getDefault());
        for (String n : recolhedoresAll) {
            if (n != null && n.trim().toLowerCase(Locale.getDefault()).equals(t)) return true;
        }
        return false;
    }

    private boolean temSugestaoRecolhedor(String txt) {
        String t = txt.trim().toLowerCase(Locale.getDefault());
        for (String n : recolhedoresAll) {
            if (n == null) continue;
            String nn = n.trim().toLowerCase(Locale.getDefault());
            if (nn.startsWith(t)) return true;
        }
        return false;
    }

//    private void // aplicarErroRecolhedor(false);(boolean erro) {
//        if (erro) {
//            mainBinding.edtRecolhedor.setError("Selecione um recolhedor da lista");
//            ViewCompat.setBackgroundTintList(
//                    mainBinding.edtRecolhedor,
//                    ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.holo_red_dark))
//            );
//        } else {
//            mainBinding.edtRecolhedor.setError(null);
//            ViewCompat.setBackgroundTintList(mainBinding.edtRecolhedor, recolhedorTintNormal);
//        }
//    }

    // ==========================================================
    // AUTOCOMPLETE VENDEDOR (VALIDACAO + BLOQUEIO) - NOVO
    // ==========================================================
    private void configurarAutocompleteVendedor() {
        vendedorTintNormal = ViewCompat.getBackgroundTintList(mainBinding.edtVendedor);

        mainBinding.edtVendedor.setOnClickListener(v -> mostrarDropDownVendedor());

        mainBinding.edtVendedor.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mostrarDropDownVendedor();
            } else {
                //validarVendedor(true);
            }
        });

        mainBinding.edtVendedor.setOnItemClickListener((parent, view, position, id) -> {
            String selecionado = (String) parent.getItemAtPosition(position);
            if (selecionado != null) {
                selecionado = selecionado.trim();
                mainBinding.edtVendedor.setText(selecionado);
                mainBinding.edtVendedor.setSelection(selecionado.length());

                aplicarErroVendedor(false);
                //setBloqueioVendedor(false);
            }
        });

        mainBinding.edtVendedor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (mainBinding.edtVendedor.getVisibility() != View.VISIBLE) return;

                String txt = s.toString().trim();

                if (vendedoresAll.isEmpty()) {
                    aplicarErroVendedor(false);
                    //setBloqueioVendedor(false);
                    return;
                }

                if (txt.isEmpty()) {
                    aplicarErroVendedor(false);
                    //setBloqueioVendedor(false);
                    mostrarDropDownVendedor();
                    return;
                }

                boolean temSugestao = temSugestaoVendedor(txt);

                if (temSugestao) {
                    aplicarErroVendedor(false);
                    //setBloqueioVendedor(false);
                } else {
                    aplicarErroVendedor(true);
                    //setBloqueioVendedor(true);
                }

                mostrarDropDownVendedor();
            }
        });
    }

    private void mostrarDropDownVendedor() {
        if (mainBinding.edtVendedor.getVisibility() != View.VISIBLE) return;
        if (adapterNomes == null || adapterNomes.getCount() == 0) return;

        mainBinding.edtVendedor.requestFocus();
        CharSequence atual = mainBinding.edtVendedor.getText();

        adapterNomes.getFilter().filter(atual, count -> {
            mainBinding.edtVendedor.post(() -> {
                mainBinding.edtVendedor.dismissDropDown();
                mainBinding.edtVendedor.showDropDown();
            });
        });
    }

//    private boolean validarVendedor(boolean exigirExato) {
//        if (mainBinding.edtVendedor.getVisibility() != View.VISIBLE) {
//            setBloqueioVendedor(false);
//            return true;
//        }
//
//        String txt = mainBinding.edtVendedor.getText().toString().trim();
//
//        if (vendedoresAll.isEmpty()) return true;
//
//        // vazio é permitido (sem filtro)
//        if (txt.isEmpty()) {
//            aplicarErroVendedor(false);
//            setBloqueioVendedor(false);
//            return true;
//        }
//
//        boolean ok = exigirExato ? existeVendedor(txt) : temSugestaoVendedor(txt);
//
//        aplicarErroVendedor(!ok);
//        setBloqueioVendedor(!ok);
//
//        if (!ok) {
//            mainBinding.edtVendedor.requestFocus();
//            mostrarDropDownVendedor();
//        }
//
//        return ok;
//    }

    private boolean existeVendedor(String txt) {
        String t = txt.trim().toLowerCase(Locale.getDefault());
        for (String n : vendedoresAll) {
            if (n != null && n.trim().toLowerCase(Locale.getDefault()).equals(t)) return true;
        }
        return false;
    }

    private boolean temSugestaoVendedor(String txt) {
        String t = txt.trim().toLowerCase(Locale.getDefault());
        for (String n : vendedoresAll) {
            if (n == null) continue;
            String nn = n.trim().toLowerCase(Locale.getDefault());
            if (nn.startsWith(t)) return true;
        }
        return false;
    }

    private void aplicarErroVendedor(boolean erro) {
        if (erro) {
            mainBinding.edtVendedor.setError("Selecione um vendedor da lista");
            ViewCompat.setBackgroundTintList(
                    mainBinding.edtVendedor,
                    ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            );
        } else {
            mainBinding.edtVendedor.setError(null);
            ViewCompat.setBackgroundTintList(mainBinding.edtVendedor, vendedorTintNormal);
        }
    }

    // ==========================================================
    // PDF (SEU CÓDIGO)
    // ==========================================================
    private void gerarPdfRecolhimentos(List<RecolheuModel> recolhimentos) {

        if (recolhimentos == null || recolhimentos.isEmpty()) {
            Toast.makeText(this, "Nenhum recolhimento encontrado para gerar o relatório.", Toast.LENGTH_SHORT).show();
            return;
        }

        final int PAGE_W = 595, PAGE_H = 842;
        final int MARGIN = 36;
        final int CONTENT_W = PAGE_W - (MARGIN * 2);

        Paint title = new Paint();
        title.setTextSize(18f);
        title.setFakeBoldText(true);

        Paint sub = new Paint();
        sub.setTextSize(12.5f);

        Paint label = new Paint();
        label.setTextSize(13f);
        label.setFakeBoldText(true);

        Paint value = new Paint();
        value.setTextSize(12.5f);

        Paint small = new Paint();
        small.setTextSize(10f);
        small.setAlpha(180);

        Paint box = new Paint();
        box.setStyle(Paint.Style.STROKE);
        box.setStrokeWidth(1.5f);
        box.setAntiAlias(true);

        Paint divider = new Paint();
        divider.setStrokeWidth(1f);
        divider.setAlpha(140);

        Locale ptBr = new Locale("pt", "BR");

        PdfDocument doc = new PdfDocument();
        int pageNum = 1;
        PdfDocument.Page page = doc.startPage(
                new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create()
        );
        Canvas canvas = page.getCanvas();
        int y = MARGIN;

        y = drawHeaderRecolhimentos(canvas, title, sub, divider, CONTENT_W, MARGIN, y);

        float totalRecolhido = 0f;
        float totalPago = 0f;

        for (RecolheuModel r : recolhimentos) {

            int tipo = r.getTipo();
            String tipoDesc;
            if (tipo == 0) {
                tipoDesc = "Recolhimento";
                totalRecolhido += r.getValor();
            } else if (tipo == 1) {
                tipoDesc = "Pagamento";
                totalPago += r.getValor();
            } else {
                tipoDesc = "Tipo " + tipo;
            }

            String vendedor = r.getVendedor() != null ? r.getVendedor() : "-";
            String dataStr = r.getDataHoraAtual() != null ? r.getDataHoraAtual() : "-";
            String valorFmt = String.format(ptBr, "R$ %.2f", r.getValor());
            String obs = r.getObservacoes();

            int cardPadding = 16;
            int lineHeight = (int) (value.getTextSize() + 10);
            int linhasBase = 4;
            int linhasObs = (obs != null && !obs.trim().isEmpty()) ? 1 : 0;
            int headerH = (int) (label.getTextSize() + 22);
            int cardH = cardPadding * 2 + headerH + ((linhasBase + linhasObs) * lineHeight);

            if (y + cardH + 30 > PAGE_H - MARGIN) {
                drawFooterRecolhimentos(canvas, small, MARGIN, PAGE_W, PAGE_H, pageNum);
                doc.finishPage(page);

                pageNum++;
                page = doc.startPage(
                        new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create()
                );
                canvas = page.getCanvas();
                y = MARGIN;
                y = drawHeaderRecolhimentos(canvas, title, sub, divider, CONTENT_W, MARGIN, y);
            }

            float left = MARGIN;
            float top = y;
            float right = MARGIN + CONTENT_W;
            float bottom = y + cardH;

            canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, box);

            int cx = MARGIN + cardPadding;
            int cy = y + cardPadding;

            canvas.drawText("Tipo: " + tipoDesc, cx, cy + label.getTextSize(), label);
            cy += (int) (label.getTextSize() + 8);

            canvas.drawLine(cx, cy, right - cardPadding, cy, divider);
            cy += 14;

            canvas.drawText("Vendedor: " + vendedor, cx, cy, value);
            cy += lineHeight;

            canvas.drawText("Valor: " + valorFmt, cx, cy, value);
            cy += lineHeight;

            canvas.drawText("Data: " + dataStr, cx, cy, value);
            cy += lineHeight;

            if (obs != null && !obs.trim().isEmpty()) {
                canvas.drawText("Observações: " + obs.trim(), cx, cy, value);
                cy += lineHeight;
            }

            y = (int) (bottom + 20);
        }

        String resumo1 = "Total Recolhido: " + String.format(ptBr, "R$ %.2f", totalRecolhido);
        String resumo2 = "Total Pago: " + String.format(ptBr, "R$ %.2f", totalPago);
        String resumo3 = "Saldo (Recolhido - Pago): " +
                String.format(ptBr, "R$ %.2f", (totalRecolhido - totalPago));

        int resumoY = PAGE_H - 70;
        canvas.drawText(resumo1, MARGIN, resumoY, label);
        canvas.drawText(resumo2, MARGIN, resumoY + 18, label);
        canvas.drawText(resumo3, MARGIN, resumoY + 36, label);

        drawFooterRecolhimentos(canvas, small, MARGIN, PAGE_W, PAGE_H, pageNum);
        doc.finishPage(page);

        File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = getExternalFilesDir(null);

        String dataArq = new SimpleDateFormat("dd_MM_yyyy_HH_mm", ptBr).format(new Date());
        File pdf = new File(dir, "relatorio_recolhimentos_" + dataArq + ".pdf");

        try (FileOutputStream fos = new FileOutputStream(pdf)) {
            doc.writeTo(fos);
            Toast.makeText(this, "PDF gerado em: " + pdf.getAbsolutePath(), Toast.LENGTH_LONG).show();
            compartilharPdfNoWhatsApp(pdf);
        } catch (IOException e) {
            Toast.makeText(this, "Erro ao salvar PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            doc.close();
        }
    }

    private int drawHeaderRecolhimentos(Canvas canvas,
                                        Paint title,
                                        Paint sub,
                                        Paint divider,
                                        int contentW,
                                        int margin,
                                        int y) {
        canvas.drawText("Relatório de Recolhimentos", margin, y + title.getTextSize(), title);
        y += (int) (title.getTextSize() + 6);

        String data = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR"))
                .format(new Date());
        canvas.drawText("Gerado em " + data, margin, y + sub.getTextSize(), sub);
        y += (int) (sub.getTextSize() + 8);

        canvas.drawLine(margin, y, margin + contentW, y, divider);
        y += 12;
        return y;
    }

    private void drawFooterRecolhimentos(Canvas canvas,
                                         Paint small,
                                         int margin,
                                         int pageW,
                                         int pageH,
                                         int pageNum) {
        String left = "© " + java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                + " • Sistema de Recolhimentos";
        String right = "Página " + pageNum;

        canvas.drawText(left, margin, pageH - 14, small);
        float rightW = small.measureText(right);
        canvas.drawText(right, pageW - margin - rightW, pageH - 14, small);
    }

    private void compartilharPdfNoWhatsApp(File pdfFile) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    pdfFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Segue o relatório em PDF 📄");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            shareIntent.setPackage("com.whatsapp");
            startActivity(shareIntent);
        } catch (Exception e) {
            Intent genericShare = new Intent(Intent.ACTION_SEND);
            genericShare.setType("application/pdf");
            genericShare.putExtra(
                    Intent.EXTRA_STREAM,
                    FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile)
            );
            genericShare.putExtra(Intent.EXTRA_TEXT, "Segue o relatório em PDF 📄");
            genericShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(genericShare, "Compartilhar PDF"));
        }
    }
}