package benicio.solucoes.rifacampeo;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import benicio.solucoes.rifacampeo.databinding.ActivityRecolheRelatorioBinding;
import benicio.solucoes.rifacampeo.objects.QueryModelEmpty;
import benicio.solucoes.rifacampeo.objects.RecolheuModel;
import benicio.solucoes.rifacampeo.objects.RecolhimentoResponse;
import benicio.solucoes.rifacampeo.objects.VendedorModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecolheRelatorioActivity extends AppCompatActivity {

    private ActivityRecolheRelatorioBinding binding;

    private final List<RecolheuModel> listaCompleta = new ArrayList<>();
    private final List<RecolheuModel> listaFiltrada = new ArrayList<>();
    private AdapterRecolhimento adapterRecolhimento;

    private final List<String> vendedoresAutocomplete = new ArrayList<>();
    private final List<String> recolhedoresAutocomplete = new ArrayList<>();

    private ArrayAdapter<String> vendedorAdapter;
    private ArrayAdapter<String> recolhedorAdapter;

    // filtros atuais usados também no PDF
    private String filtroAtualVendedor = null;
    private String filtroAtualRecolhedor = null;
    private String filtroAtualDataInicio = null;
    private String filtroAtualDataFim = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityRecolheRelatorioBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootRelatorios, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );
            return insets;
        });

        configurarRecyclerView();
        configurarAutocomplete();
        configurarDatas();
        configurarAcoes();

        carregarVendedores();
        carregarRecolhimentos();
    }

    @Override
    protected void onStart() {
        super.onStart();
        carregarRecolhimentos();
    }

    private void configurarRecyclerView() {
        binding.rvBilhetes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvBilhetes.setHasFixedSize(true);
        adapterRecolhimento = new AdapterRecolhimento(listaFiltrada, this);
        binding.rvBilhetes.setAdapter(adapterRecolhimento);
    }

    private void configurarAutocomplete() {
        vendedoresAutocomplete.clear();
        recolhedoresAutocomplete.clear();

        vendedorAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                vendedoresAutocomplete
        );

        recolhedorAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                recolhedoresAutocomplete
        );

        binding.spNomeVendedor.setAdapter(vendedorAdapter);
        binding.spNomeVendedor.setThreshold(2);

        binding.spDocumentoVendedor.setAdapter(recolhedorAdapter);
        binding.spDocumentoVendedor.setThreshold(2);

        configurarRegraMinimo2Caracteres(binding.spNomeVendedor);
        configurarRegraMinimo2Caracteres(binding.spDocumentoVendedor);
    }

    private void configurarRegraMinimo2Caracteres(AutoCompleteTextView autoCompleteTextView) {
        autoCompleteTextView.setOnClickListener(v -> {
            String texto = safe(autoCompleteTextView.getText().toString()).trim();
            if (texto.length() >= 2) {
                autoCompleteTextView.showDropDown();
            }
        });

        autoCompleteTextView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                String texto = safe(autoCompleteTextView.getText().toString()).trim();
                if (texto.length() >= 2) {
                    autoCompleteTextView.showDropDown();
                }
            }
        });
    }

    private void configurarDatas() {
        String hoje = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        binding.edtDataInicio.setText(hoje);
        binding.edtDataFim.setText(hoje);

        filtroAtualDataInicio = hoje;
        filtroAtualDataFim = hoje;

        binding.edtDataInicio.setOnClickListener(v -> mostrarDatePicker(binding.edtDataInicio));
        binding.edtDataFim.setOnClickListener(v -> mostrarDatePicker(binding.edtDataFim));
    }

    private void configurarAcoes() {
        binding.btnBuscar.setOnClickListener(v -> aplicarFiltros());
        binding.btnGerarPdf.setOnClickListener(v -> gerarPdfRecolhimentos(listaFiltrada));
    }

    private void carregarVendedores() {
        RetrofitUtils.getApiService()
                .returnVendedores(1, new QueryModelEmpty())
                .enqueue(new Callback<List<VendedorModel>>() {
                    @Override
                    public void onResponse(Call<List<VendedorModel>> call, Response<List<VendedorModel>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            LinkedHashSet<String> set = new LinkedHashSet<>();

                            for (VendedorModel vendedor : response.body()) {
                                String nome = safe(vendedor.getNome()).trim();
                                if (!nome.isEmpty()) {
                                    set.add(nome);
                                }
                            }

                            vendedoresAutocomplete.clear();
                            vendedoresAutocomplete.addAll(set);
                            vendedorAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(RecolheRelatorioActivity.this,
                                    "Erro ao carregar vendedores",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<VendedorModel>> call, Throwable t) {
                        Toast.makeText(RecolheRelatorioActivity.this,
                                "Falha ao carregar vendedores",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void carregarRecolhimentos() {
        RetrofitUtils.getApiService()
                .retornar_recolhimento(null, null, null, null, 999999999, 1)
                .enqueue(new Callback<RecolhimentoResponse>() {
                    @Override
                    public void onResponse(Call<RecolhimentoResponse> call, Response<RecolhimentoResponse> response) {
                        listaCompleta.clear();

                        if (response.isSuccessful() && response.body() != null && response.body().getItens() != null) {
                            listaCompleta.addAll(response.body().getItens());
                        }

                        atualizarAutocompleteRecolhedores(listaCompleta);
                        aplicarFiltros();
                    }

                    @Override
                    public void onFailure(Call<RecolhimentoResponse> call, Throwable t) {
                        Toast.makeText(RecolheRelatorioActivity.this,
                                "Erro ao carregar recolhimentos",
                                Toast.LENGTH_SHORT).show();

                        listaCompleta.clear();
                        listaFiltrada.clear();
                        adapterRecolhimento.notifyDataSetChanged();
                    }
                });
    }

    private void atualizarAutocompleteRecolhedores(List<RecolheuModel> itens) {
        LinkedHashSet<String> set = new LinkedHashSet<>();

        for (RecolheuModel item : itens) {
            String recolhedor = safe(item.getRecolhedor()).trim();
            if (!recolhedor.isEmpty()) {
                set.add(recolhedor);
            }
        }

        recolhedoresAutocomplete.clear();
        recolhedoresAutocomplete.addAll(set);
        recolhedorAdapter.notifyDataSetChanged();
    }

    private void aplicarFiltros() {
        String vendedorSelecionado = getAutoCompleteValue(binding.spNomeVendedor);
        String recolhedorSelecionado = getAutoCompleteValue(binding.spDocumentoVendedor);
        String dataInicio = emptyToNull(binding.edtDataInicio.getText().toString());
        String dataFim = emptyToNull(binding.edtDataFim.getText().toString());

        filtroAtualVendedor = vendedorSelecionado;
        filtroAtualRecolhedor = recolhedorSelecionado;
        filtroAtualDataInicio = dataInicio;
        filtroAtualDataFim = dataFim;

        listaFiltrada.clear();

        for (RecolheuModel item : listaCompleta) {
            if (passaNosFiltros(item, vendedorSelecionado, recolhedorSelecionado, dataInicio, dataFim)) {
                listaFiltrada.add(item);
            }
        }

        adapterRecolhimento.notifyDataSetChanged();

        Toast.makeText(
                this,
                "Filtro aplicado: " + listaFiltrada.size() + " resultado(s)",
                Toast.LENGTH_SHORT
        ).show();
    }

    private boolean passaNosFiltros(RecolheuModel item,
                                    String vendedorFiltro,
                                    String recolhedorFiltro,
                                    String dataInicioFiltro,
                                    String dataFimFiltro) {

        if (item == null) return false;

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

    private void mostrarDatePicker(EditText campoAlvo) {
        Calendar cal = Calendar.getInstance();

        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String diaFmt = String.format(Locale.getDefault(), "%02d", dayOfMonth);
                    String mesFmt = String.format(Locale.getDefault(), "%02d", month + 1);
                    String anoFmt = String.format(Locale.getDefault(), "%04d", year);
                    campoAlvo.setText(diaFmt + "/" + mesFmt + "/" + anoFmt);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        dp.show();
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

    private String getAutoCompleteValue(AutoCompleteTextView autoCompleteTextView) {
        if (autoCompleteTextView == null || autoCompleteTextView.getText() == null) {
            return null;
        }
        String texto = autoCompleteTextView.getText().toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private String safe(String s) {
        return s == null ? "" : s;
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

            canvas.drawText("Recolhe: " + recolhedor, cx, cy, value);
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
            compartilharPdf(pdf);
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
        String recolhedorRelatorio = !isBlank(filtroAtualRecolhedor) ? filtroAtualRecolhedor : "Todos";

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
        y += (int) (sub.getTextSize() + 10);

        canvas.drawText("Filtros utilizados:", margin, y + sub.getTextSize(), title);
        y += (int) (sub.getTextSize() + 8);

        canvas.drawText("Vendedor: " + vendedorRelatorio, margin, y + sub.getTextSize(), sub);
        y += (int) (sub.getTextSize() + 6);

        canvas.drawText("Recolhe: " + recolhedorRelatorio, margin, y + sub.getTextSize(), sub);
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
}