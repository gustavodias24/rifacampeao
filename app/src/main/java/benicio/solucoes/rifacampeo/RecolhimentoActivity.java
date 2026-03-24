package benicio.solucoes.rifacampeo;

import android.app.DatePickerDialog;
import android.app.Dialog;
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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
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

    private ArrayAdapter<String> adapterRecolhedor;
    private final List<String> recolhedoresAll = new ArrayList<>();

    private ArrayAdapter<String> adapterNomes;
    private final List<String> vendedoresAll = new ArrayList<>();
    private ColorStateList vendedorTintNormal;

    // filtros atuais usados também no PDF
    private String filtroAtualVendedor;
    private String filtroAtualDataInicio;
    private String filtroAtualDataFim;
    private String filtroAtualRecolhedor;
    private Integer filtroAtualTipo;

    Dialog drelatoriosaldo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityRecolhimentoBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);




        adapterRecolhedor = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<>()
        );
        mainBinding.edtRecolhedor.setAdapter(adapterRecolhedor);
        mainBinding.edtRecolhedor.setThreshold(0);
        configurarAutocompleteRecolhedor();

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

        drelatoriosaldo = new AlertDialog.Builder(this).setMessage("Gerando...").setCancelable(false).create();
        mainBinding.relatoriosaldo.setOnClickListener(v -> {


            drelatoriosaldo.show();

            RetrofitUtils.getApiService()
                    .retornar_recolhimento(null, null, null, null, 999999999, 1)
                    .enqueue(new Callback<RecolhimentoResponse>() {
                        @Override
                        public void onResponse(Call<RecolhimentoResponse> call, Response<RecolhimentoResponse> response) {
                            if (response.isSuccessful()) {
                                gerarPdfVendedores(vendedores, nomeRecolhedor, response.body().itens);
                                drelatoriosaldo.dismiss();
                            }
                        }

                        @Override
                        public void onFailure(Call<RecolhimentoResponse> call, Throwable throwable) {
                            Toast.makeText(RecolhimentoActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            drelatoriosaldo.dismiss();
                        }
                    });

        });

        mainBinding.edtRecolhedor.setText(nomeRecolhedor);

        boolean modoRecolhedor = isModoRecolhedor();

        if (!modoRecolhedor) {
            mainBinding.edtRecolhedor.setVisibility(View.GONE);
            mainBinding.button9.setVisibility(View.GONE);
        } else {
            mainBinding.edtRecolhedor.setKeyListener(null);
            mainBinding.edtRecolhedor.setCursorVisible(false);
            mainBinding.edtRecolhedor.setLongClickable(false);
            mainBinding.edtRecolhedor.setTextIsSelectable(false);
            mainBinding.btnGerarRelatorio.setVisibility(View.GONE);
        }

        carregarVendedores();

        mainBinding.button9.setOnClickListener(v -> {
            if (!validarRecolhedor(true)) return;

            nomeRecolhedor = mainBinding.edtRecolhedor.getText().toString().trim();
            edt.putString("recolhedor", nomeRecolhedor).apply();
            Toast.makeText(this, "Recolhedor salvo!", Toast.LENGTH_SHORT).show();
        });

        mainBinding.btnadd.setOnClickListener(v -> {
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

        filtroAtualDataInicio = dataAtual;
        filtroAtualDataFim = dataAtual;

        mainBinding.btnFiltrar.setOnClickListener(v -> filtarAction());

        mainBinding.etDataInicio.setOnClickListener(v -> mostrarDatePicker(mainBinding.etDataInicio));
        mainBinding.etDataFim.setOnClickListener(v -> mostrarDatePicker(mainBinding.etDataFim));

        mainBinding.btnGerarRelatorio.setOnClickListener(v -> gerarPdfRecolhimentos(lista_recolhimento));

        filtarAction();
    }

    private int drawHeaderVendedores(Canvas canvas,
                                     Paint title,
                                     Paint sub,
                                     Paint divider,
                                     int contentW,
                                     int margin,
                                     int y,
                                     String documentoFiltro) {

        Locale ptBr = new Locale("pt", "BR");
        String dataGeracao = new SimpleDateFormat("dd/MM/yyyy HH:mm", ptBr).format(new Date());
        String filtroTexto = !isBlank(documentoFiltro) ? documentoFiltro : "Todos";

        canvas.drawText("Relatório de Vendedores", margin, y + title.getTextSize(), title);
        y += (int) (title.getTextSize() + 8);

        canvas.drawText("Gerado em: " + dataGeracao, margin, y + sub.getTextSize(), sub);
        y += (int) (sub.getTextSize() + 6);

        canvas.drawText("Recolhedor: " + filtroTexto, margin, y + sub.getTextSize(), sub);
        y += (int) (sub.getTextSize() + 10);

        canvas.drawLine(margin, y, margin + contentW, y, divider);
        y += 18;

        return y;
    }

    private void drawFooterPadrao(Canvas canvas,
                                  Paint small,
                                  int margin,
                                  int pageW,
                                  int pageH,
                                  int pageNum) {
        String left = "© " + Calendar.getInstance().get(Calendar.YEAR) + " • Sistema";
        String right = "Página " + pageNum;

        canvas.drawText(left, margin, pageH - 14, small);
        float rightW = small.measureText(right);
        canvas.drawText(right, pageW - margin - rightW, pageH - 14, small);
    }

    private void gerarPdfVendedores(List<VendedorModel> vendedores,
                                    String documentoFiltro,
                                    List<RecolheuModel> recolhimentos) {

        if (vendedores == null || vendedores.isEmpty()) {
            Toast.makeText(this, "Nenhum vendedor encontrado para gerar o relatório.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<VendedorModel> vendedoresFiltrados = new ArrayList<>();

        String filtroDoc = emptyToNull(documentoFiltro);

        for (VendedorModel vendedor : vendedores) {
            if (vendedor == null) continue;

            if (isBlank(filtroDoc)) {
                vendedoresFiltrados.add(vendedor);
            } else {
                String docVendedor = safe(vendedor.getDocumento()).trim();
                if (normalize(docVendedor).equals(normalize(filtroDoc))) {
                    vendedoresFiltrados.add(vendedor);
                }
            }
        }

        if (vendedoresFiltrados.isEmpty()) {
            Toast.makeText(this, "Nenhum vendedor encontrado com esse documento.", Toast.LENGTH_SHORT).show();
            return;
        }

        final int PAGE_W = 595;
        final int PAGE_H = 842;
        final int MARGIN = 36;
        final int CONTENT_W = PAGE_W - (MARGIN * 2);

        Paint title = new Paint();
        title.setTextSize(18f);
        title.setFakeBoldText(true);

        Paint sub = new Paint();
        sub.setTextSize(12f);

        Paint value = new Paint();
        value.setTextSize(12.5f);

        Paint small = new Paint();
        small.setTextSize(10f);
        small.setAlpha(180);

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

        y = drawHeaderVendedores(canvas, title, sub, divider, CONTENT_W, MARGIN, y, filtroDoc);

        int lineHeight = 24;

        for (VendedorModel vendedor : vendedoresFiltrados) {
            String nome = !isBlank(vendedor.getNome()) ? vendedor.getNome().trim() : "-";
            float saldoAtual = vendedor.getSaldoAtual(recolhimentos != null ? recolhimentos : new ArrayList<>());
            String saldoFmt = String.format(ptBr, "R$ %.2f", saldoAtual);

            String linha = "Nome vendedor: " + nome + "    Saldo deve: " + saldoFmt;

            if (y + 40 > PAGE_H - MARGIN) {
                drawFooterPadrao(canvas, small, MARGIN, PAGE_W, PAGE_H, pageNum);
                doc.finishPage(page);

                pageNum++;
                page = doc.startPage(
                        new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create()
                );
                canvas = page.getCanvas();
                y = MARGIN;
                y = drawHeaderVendedores(canvas, title, sub, divider, CONTENT_W, MARGIN, y, filtroDoc);
            }

            canvas.drawText(linha, MARGIN, y, value);
            y += lineHeight;

            canvas.drawLine(MARGIN, y, MARGIN + CONTENT_W, y, divider);
            y += 14;
        }

        drawFooterPadrao(canvas, small, MARGIN, PAGE_W, PAGE_H, pageNum);
        doc.finishPage(page);

        File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = getExternalFilesDir(null);

        String dataArq = new SimpleDateFormat("dd_MM_yyyy_HH_mm", ptBr).format(new Date());
        File pdf = new File(dir, "relatorio_vendedores_" + dataArq + ".pdf");

        try (FileOutputStream fos = new FileOutputStream(pdf)) {
            doc.writeTo(fos);
            Toast.makeText(this, "PDF gerado em: " + pdf.getAbsolutePath(), Toast.LENGTH_LONG).show();
            compartilharPdf(pdf);
        } catch (IOException e) {
            Toast.makeText(this, "Erro ao salvar PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            doc.close();
        }
    }

    private void compartilharPdf(File pdfFile) {
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

            startActivity(Intent.createChooser(shareIntent, "Compartilhar PDF"));
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao compartilhar PDF.", Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    protected void onStart() {
        super.onStart();
        listarRecolhimentos();
    }



    private boolean isModoRecolhedor() {
        Bundle b = getIntent().getExtras();
        return b != null && b.getBoolean("recolhedor", false);
    }

    private void filtarAction() {
        String vendedor = emptyToNull(mainBinding.edtVendedor.getText().toString());
        if (vendedor != null && vendedor.equalsIgnoreCase("Todos")) vendedor = null;

        String dataInicio = emptyToNull(mainBinding.etDataInicio.getText().toString());
        String dataFim = emptyToNull(mainBinding.etDataFim.getText().toString());

        String recolhedorFiltro = null;
        if (mainBinding.edtRecolhedor.getVisibility() == View.VISIBLE) {
            recolhedorFiltro = emptyToNull(mainBinding.edtRecolhedor.getText().toString());
        }

        Integer tipo = null; // mantém como estava, caso depois exista filtro de tipo na tela

        filtroAtualVendedor = vendedor;
        filtroAtualDataInicio = dataInicio;
        filtroAtualDataFim = dataFim;
        filtroAtualRecolhedor = recolhedorFiltro;
        filtroAtualTipo = tipo;

        filtrarRecolhimentos(vendedor, dataInicio, dataFim, tipo, recolhedorFiltro);
    }

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

    private void filtrarRecolhimentos(String vendedor,
                                      String dataInicio,
                                      String dataFim,
                                      Integer tipo,
                                      String recolhedorFiltro) {

        lista_recolhimento.clear();
        adapterRecolhimento.notifyDataSetChanged();
        showLoading(true);

        // Busca tudo e filtra localmente.
        // Isso corrige problemas quando a API não devolve corretamente pelos parâmetros.
        RetrofitUtils.getApiService()
                .retornar_recolhimento(null, null, null, null, 999999999, 1)
                .enqueue(new Callback<RecolhimentoResponse>() {
                    @Override
                    public void onResponse(Call<RecolhimentoResponse> call,
                                           Response<RecolhimentoResponse> response) {
                        showLoading(false);

                        List<RecolheuModel> itensApi = new ArrayList<>();
                        if (response.isSuccessful() && response.body() != null && response.body().getItens() != null) {
                            itensApi.addAll(response.body().getItens());
                        }

                        lista_recolhimento.clear();

                        boolean modoRecolhedor = isModoRecolhedor();
                        String nomeFixado = modoRecolhedor ? nomeRecolhedor : null;

                        for (RecolheuModel item : itensApi) {
                            if (passaNosFiltros(item, vendedor, dataInicio, dataFim, tipo, recolhedorFiltro, modoRecolhedor, nomeFixado)) {
                                lista_recolhimento.add(item);
                            }
                        }

                        adapterRecolhimento.notifyDataSetChanged();
                        atualizarAutocompleteRecolhedor(itensApi);

                        if (lista_recolhimento.isEmpty()) {
                            Toast.makeText(RecolhimentoActivity.this,
                                    "Nenhum resultado encontrado",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(RecolhimentoActivity.this,
                                    "Filtro aplicado (" + lista_recolhimento.size() + " itens)",
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

    private boolean passaNosFiltros(RecolheuModel item,
                                    String vendedorFiltro,
                                    String dataInicioFiltro,
                                    String dataFimFiltro,
                                    Integer tipoFiltro,
                                    String recolhedorFiltro,
                                    boolean modoRecolhedor,
                                    String recolhedorFixado) {

        if (item == null) return false;

        if (modoRecolhedor && !isBlank(recolhedorFixado)) {
            if (!normalize(item.getRecolhedor()).equals(normalize(recolhedorFixado))) {
                return false;
            }
        }

        if (!isBlank(vendedorFiltro)) {
            if (!normalize(item.getVendedor()).contains(normalize(vendedorFiltro))) {
                return false;
            }
        }

        if (!isBlank(recolhedorFiltro)) {
            if (!normalize(item.getRecolhedor()).contains(normalize(recolhedorFiltro))) {
                return false;
            }
        }

        if (tipoFiltro != null && item.getTipo() != tipoFiltro) {
            return false;
        }

        return estaDentroDoPeriodo(item.getDataHoraAtual(), dataInicioFiltro, dataFimFiltro);
    }

    private boolean estaDentroDoPeriodo(String dataItemTexto, String dataInicioTexto, String dataFimTexto) {
        if (isBlank(dataInicioTexto) && isBlank(dataFimTexto)) return true;

        Date dataItem = parseDateFlex(dataItemTexto);
        if (dataItem == null) return false;

        Date dataInicio = parseDateFlex(dataInicioTexto);
        Date dataFim = parseDateFlex(dataFimTexto);

        if (dataInicio != null && dataItem.before(inicioDoDia(dataInicio))) {
            return false;
        }

        if (dataFim != null && dataItem.after(fimDoDia(dataFim))) {
            return false;
        }

        return true;
    }

    private Date parseDateFlex(String value) {
        if (isBlank(value)) return null;

        String[] patterns = new String[]{
                "dd/MM/yyyy HH:mm:ss",
                "dd/MM/yyyy HH:mm",
                "dd/MM/yyyy",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
                sdf.setLenient(false);
                return sdf.parse(value.trim());
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private Date inicioDoDia(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private Date fimDoDia(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    private void listarRecolhimentos() {
        showLoading(true);

        RetrofitUtils.getApiService().retornar_recolhimento(null, null, null, null, 999999999, 1)
                .enqueue(new Callback<RecolhimentoResponse>() {
                    @Override
                    public void onResponse(Call<RecolhimentoResponse> call, Response<RecolhimentoResponse> response) {
                        showLoading(false);

                        lista_recolhimento.clear();

                        if (response.isSuccessful() && response.body() != null && response.body().getItens() != null) {
                            List<RecolheuModel> itensApi = response.body().getItens();

                            boolean modoRecolhedor = isModoRecolhedor();

                            if (!modoRecolhedor) {
                                lista_recolhimento.addAll(itensApi);
                            } else {
                                for (RecolheuModel recolhe : itensApi) {
                                    if (normalize(recolhe.getRecolhedor()).equals(normalize(nomeRecolhedor))) {
                                        lista_recolhimento.add(recolhe);
                                    }
                                }
                            }

                            atualizarAutocompleteRecolhedor(itensApi);
                        }

                        adapterRecolhimento.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailure(Call<RecolhimentoResponse> call, Throwable throwable) {
                        showLoading(false);
                    }
                });
    }

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
    }

    private void configurarRV() {
        mainBinding.recolhimentorv.setLayoutManager(new LinearLayoutManager(this));
        mainBinding.recolhimentorv.setHasFixedSize(true);
        adapterRecolhimento = new AdapterRecolhimento(lista_recolhimento, this);
        mainBinding.recolhimentorv.setAdapter(adapterRecolhimento);
    }

    private void carregarVendedores() {
        RetrofitUtils.getApiService().returnVendedores(1, new QueryModelEmpty())
                .enqueue(new Callback<List<VendedorModel>>() {
                    @Override
                    public void onResponse(Call<List<VendedorModel>> call, Response<List<VendedorModel>> response) {
                        if (response.isSuccessful() && response.body() != null) {

                            vendedores.clear();
                            vendedores.addAll(response.body());

                            LinkedHashSet<String> nomes = new LinkedHashSet<>();
                            nomes.add("Todos");

                            for (VendedorModel v : vendedores) {
                                String nome = safe(v.getNome()).trim();
                                if (!nome.isEmpty()) nomes.add(nome);
                            }

                            vendedoresAll.clear();
                            vendedoresAll.addAll(nomes);

                            adapterNomes.clear();
                            adapterNomes.addAll(vendedoresAll);
                            adapterNomes.notifyDataSetChanged();

                            CharSequence atual = mainBinding.edtVendedor.getText();
                            adapterNomes.getFilter().filter(atual, count -> {
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

    private String emptyToNull(String s) {
        if (s == null) return null;
        String txt = s.trim();
        return txt.isEmpty() ? null : txt;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.getDefault());
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

    private void configurarAutocompleteRecolhedor() {
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
            }
        });

        mainBinding.edtRecolhedor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (mainBinding.edtRecolhedor.getVisibility() != View.VISIBLE) return;

                String txt = s.toString().trim();

                if (recolhedoresAll.isEmpty()) return;

                if (txt.isEmpty()) {
                    mostrarDropDownRecolhedor();
                    return;
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
            return true;
        }

        String txt = mainBinding.edtRecolhedor.getText().toString().trim();

        if (recolhedoresAll.isEmpty()) return true;
        if (txt.isEmpty()) return true;

        boolean ok = exigirExato ? existeRecolhedor(txt) : temSugestaoRecolhedor(txt);

        if (!ok) {
            mainBinding.edtRecolhedor.requestFocus();
            mostrarDropDownRecolhedor();
        } else {
            nomeRecolhedor = txt;
        }

        return ok;
    }

    private boolean existeRecolhedor(String txt) {
        String t = normalize(txt);
        for (String n : recolhedoresAll) {
            if (normalize(n).equals(t)) return true;
        }
        return false;
    }

    private boolean temSugestaoRecolhedor(String txt) {
        String t = normalize(txt);
        for (String n : recolhedoresAll) {
            if (normalize(n).startsWith(t)) return true;
        }
        return false;
    }

    private void configurarAutocompleteVendedor() {
        vendedorTintNormal = ViewCompat.getBackgroundTintList(mainBinding.edtVendedor);

        mainBinding.edtVendedor.setOnClickListener(v -> mostrarDropDownVendedor());

        mainBinding.edtVendedor.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mostrarDropDownVendedor();
            }
        });

        mainBinding.edtVendedor.setOnItemClickListener((parent, view, position, id) -> {
            String selecionado = (String) parent.getItemAtPosition(position);
            if (selecionado != null) {
                selecionado = selecionado.trim();
                mainBinding.edtVendedor.setText(selecionado);
                mainBinding.edtVendedor.setSelection(selecionado.length());
                aplicarErroVendedor(false);
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
                    return;
                }

                if (txt.isEmpty()) {
                    aplicarErroVendedor(false);
                    mostrarDropDownVendedor();
                    return;
                }

                boolean temSugestao = temSugestaoVendedor(txt);

                if (temSugestao) {
                    aplicarErroVendedor(false);
                } else {
                    aplicarErroVendedor(true);
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

    private boolean temSugestaoVendedor(String txt) {
        String t = normalize(txt);
        for (String n : vendedoresAll) {
            if (normalize(n).startsWith(t)) return true;
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

    private void gerarPdfRecolhimentos(List<RecolheuModel> recolhimentos) {

        if (recolhimentos == null || recolhimentos.isEmpty()) {
            Toast.makeText(this, "Nenhum recolhimento encontrado para gerar o relatório.", Toast.LENGTH_SHORT).show();
            return;
        }

        final int PAGE_W = 595;
        final int PAGE_H = 842;
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

            String vendedor = !isBlank(r.getVendedor()) ? r.getVendedor().trim() : "-";
            String dataStr = !isBlank(r.getDataHoraAtual()) ? r.getDataHoraAtual().trim() : "-";
            String recolhedor = !isBlank(r.getRecolhedor()) ? r.getRecolhedor().trim() : "-";
            String valorFmt = String.format(ptBr, "R$ %.2f", r.getValor());
            String obs = !isBlank(r.getObservacoes()) ? r.getObservacoes().trim() : null;

            int cardPadding = 16;
            int lineHeight = (int) (value.getTextSize() + 10);
            int linhasBase = 5;
            int linhasObs = (obs != null) ? 1 : 0;
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

            canvas.drawText("Recolhedor: " + recolhedor, cx, cy, value);
            cy += lineHeight;

            canvas.drawText("Valor: " + valorFmt, cx, cy, value);
            cy += lineHeight;

            canvas.drawText("Data: " + dataStr, cx, cy, value);
            cy += lineHeight;

            if (obs != null) {
                canvas.drawText("Observações: " + obs, cx, cy, value);
                cy += lineHeight;
            }

            y = (int) (bottom + 20);
        }

        int resumoAltura = 70;
        if (y + resumoAltura > PAGE_H - MARGIN) {
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

        String resumo1 = "Total Recolhido: " + String.format(ptBr, "R$ %.2f", totalRecolhido);
        String resumo2 = "Total Pago: " + String.format(ptBr, "R$ %.2f", totalPago);
        String resumo3 = "Saldo (Recolhido - Pago): " +
                String.format(ptBr, "R$ %.2f", (totalRecolhido - totalPago));

        canvas.drawText(resumo1, MARGIN, y + 15, label);
        canvas.drawText(resumo2, MARGIN, y + 33, label);
        canvas.drawText(resumo3, MARGIN, y + 51, label);

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

        Locale ptBr = new Locale("pt", "BR");

        String vendedorRelatorio = !isBlank(filtroAtualVendedor) ? filtroAtualVendedor : "Todos";
        String recolhedorRelatorio;

        if (!isBlank(filtroAtualRecolhedor)) {
            recolhedorRelatorio = filtroAtualRecolhedor;
        } else if (isModoRecolhedor() && !isBlank(nomeRecolhedor)) {
            recolhedorRelatorio = nomeRecolhedor;
        } else {
            recolhedorRelatorio = "Todos";
        }

        String periodoRelatorio;
        if (!isBlank(filtroAtualDataInicio) && !isBlank(filtroAtualDataFim)) {
            periodoRelatorio = filtroAtualDataInicio + " até " + filtroAtualDataFim;
        } else if (!isBlank(filtroAtualDataInicio)) {
            periodoRelatorio = "A partir de " + filtroAtualDataInicio;
        } else if (!isBlank(filtroAtualDataFim)) {
            periodoRelatorio = "Até " + filtroAtualDataFim;
        } else {
            periodoRelatorio = "Todos";
        }

        String dataGeracao = new SimpleDateFormat("dd/MM/yyyy HH:mm", ptBr).format(new Date());

        canvas.drawText("Relatório de Recolhimentos", margin, y + title.getTextSize(), title);
        y += (int) (title.getTextSize() + 6);

        canvas.drawText("Gerado em " + dataGeracao, margin, y + sub.getTextSize(), sub);
        y += (int) (sub.getTextSize() + 6);

        canvas.drawText("Vendedor: " + vendedorRelatorio, margin, y + sub.getTextSize(), sub);
        y += (int) (sub.getTextSize() + 6);

        canvas.drawText("Recolhedor: " + recolhedorRelatorio, margin, y + sub.getTextSize(), sub);
        y += (int) (sub.getTextSize() + 6);

        canvas.drawText("Período: " + periodoRelatorio, margin, y + sub.getTextSize(), sub);
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
        String left = "© " + Calendar.getInstance().get(Calendar.YEAR) + " • Sistema de Recolhimentos";
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
            try {
                Uri uri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        pdfFile
                );

                Intent genericShare = new Intent(Intent.ACTION_SEND);
                genericShare.setType("application/pdf");
                genericShare.putExtra(Intent.EXTRA_STREAM, uri);
                genericShare.putExtra(Intent.EXTRA_TEXT, "Segue o relatório em PDF 📄");
                genericShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(Intent.createChooser(genericShare, "Compartilhar PDF"));
            } catch (Exception ex) {
                Toast.makeText(this, "Erro ao compartilhar PDF.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}