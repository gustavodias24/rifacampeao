package benicio.solucoes.rifacampeo;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

    private ArrayAdapter<String> adapterNomes;

    private List<RecolheuModel> lista_recolhimento = new ArrayList<>();
    private AdapterRecolhimento adapterRecolhimento;

    private final List<VendedorModel> vendedores = new ArrayList<>();
    private final List<String> nomes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityRecolhimentoBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        configurarRV();

        // Adapter para o AutoCompleteTextView de vendedor
        adapterNomes = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                nomes
        );
        carregarVendedores();   // Preenche Nome via API

        // Liga o adapter ao campo edtVendedor
        mainBinding.edtVendedor.setAdapter(adapterNomes);
        mainBinding.edtVendedor.setThreshold(1); // começa a sugerir a partir de 1 caractere

        mainBinding.btnadd.setOnClickListener(v ->
                startActivity(new Intent(this, MakeRecolhimentoActivity.class))
        );

        mainBinding.btnFiltrar.setOnClickListener(v -> {
            String vendedor = mainBinding.edtVendedor.getText().toString().trim();
            if (vendedor.isEmpty() || vendedor.equalsIgnoreCase("Todos")) {
                vendedor = null;
            }

            String dataInicio = mainBinding.etDataInicio.getText().toString().trim();
            String dataFim = mainBinding.etDataFim.getText().toString().trim();

            if (dataInicio.isEmpty()) dataInicio = null;
            if (dataFim.isEmpty()) dataFim = null;

            // tipo não tem no layout, então deixamos como null
            Integer tipo = null;

            filtrarRecolhimentos(vendedor, dataInicio, dataFim, tipo);
        });

        mainBinding.etDataInicio.setOnClickListener(v -> {
            mostrarDatePicker(mainBinding.etDataInicio);
        });

        mainBinding.etDataFim.setOnClickListener(v -> {
            mostrarDatePicker(mainBinding.etDataFim);
        });

        mainBinding.btnGerarRelatorio.setOnClickListener(v -> {
            gerarPdfRecolhimentos(lista_recolhimento);
        });
    }

    private void mostrarDatePicker(EditText campoAlvo) {
        // Data atual como padrão
        final Calendar cal = Calendar.getInstance();
        int ano = cal.get(Calendar.YEAR);
        int mes = cal.get(Calendar.MONTH);          // 0-11
        int dia = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    // month vem 0-11, então soma 1
                    String diaFmt = String.format(Locale.getDefault(), "%02d", dayOfMonth);
                    String mesFmt = String.format(Locale.getDefault(), "%02d", month + 1);
                    String anoFmt = String.format(Locale.getDefault(), "%04d", year);

                    String dataFinal = diaFmt + "/" + mesFmt + "/" + anoFmt;
                    campoAlvo.setText(dataFinal);
                },
                ano, mes, dia
        );

        dp.show();
    }

    private void filtrarRecolhimentos(String vendedor, String dataInicio, String dataFim, Integer tipo) {

        // mostra loading
        showLoading(true);

        // Normaliza vendedor: se vazio, manda null
        String vendedorParam = (vendedor != null && !vendedor.trim().isEmpty())
                ? vendedor.trim()
                : null;

        // Normaliza datas: se vazio, manda null
        String dataInicioParam = (dataInicio != null && !dataInicio.trim().isEmpty())
                ? dataInicio.trim()
                : null;

        String dataFimParam = (dataFim != null && !dataFim.trim().isEmpty())
                ? dataFim.trim()
                : null;

        RetrofitUtils.getApiService()
                .retornar_recolhimento(
                        vendedorParam,
                        dataInicioParam,  // vira data_inicio na rota
                        dataFimParam,     // vira data_fim na rota
                        tipo,
                        999999999,        // limit
                        1                 // page
                )
                .enqueue(new Callback<RecolhimentoResponse>() {
                    @Override
                    public void onResponse(Call<RecolhimentoResponse> call, Response<RecolhimentoResponse> response) {
                        showLoading(false);

                        if (response.isSuccessful() && response.body() != null) {

                            lista_recolhimento.clear();
                            lista_recolhimento.addAll(response.body().getItens());
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
                            lista_recolhimento.addAll(response.body().getItens());
                            adapterRecolhimento.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onFailure(Call<RecolhimentoResponse> call, Throwable throwable) {
                        showLoading(false);
                    }
                });
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

                            // Recria a lista mantendo "Todos" como primeira opção
                            nomes.clear();
                            nomes.add("Todos");

                            for (VendedorModel v : vendedores) {
                                nomes.add(safe(v.getNome()));
                            }

                            adapterNomes.notifyDataSetChanged();
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

    /** Mostra o loading no lugar da lista */
    private void showLoading(boolean show) {
        if (show) {
            mainBinding.progressRecolhimento.setVisibility(View.VISIBLE);
            mainBinding.recolhimentorv.setVisibility(View.GONE);
        } else {
            mainBinding.progressRecolhimento.setVisibility(View.GONE);
            mainBinding.recolhimentorv.setVisibility(View.VISIBLE);
        }
    }

    private void gerarPdfRecolhimentos(List<RecolheuModel> recolhimentos) {

        if (recolhimentos == null || recolhimentos.isEmpty()) {
            Toast.makeText(this, "Nenhum recolhimento encontrado para gerar o relatório.", Toast.LENGTH_SHORT).show();
            return;
        }

        final int PAGE_W = 595, PAGE_H = 842; // A4 ~72dpi
        final int MARGIN = 36;
        final int CONTENT_W = PAGE_W - (MARGIN * 2);

        // Paints
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

        // Cabeçalho
        y = drawHeaderRecolhimentos(canvas, title, sub, divider, CONTENT_W, MARGIN, y);

        float totalRecolhido = 0f;
        float totalPago = 0f;

        for (RecolheuModel r : recolhimentos) {

            // Tipo / valores
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

            // Layout / altura do card
            int cardPadding = 16;
            int lineHeight = (int) (value.getTextSize() + 10);
            int linhasBase = 4; // Tipo, Vendedor, Valor, Data
            int linhasObs = (obs != null && !obs.trim().isEmpty()) ? 1 : 0;
            int headerH = (int) (label.getTextSize() + 22);
            int cardH = cardPadding * 2 + headerH + ((linhasBase + linhasObs) * lineHeight);

            // Quebra de página se não couber
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

            // Título do card
            canvas.drawText("Tipo: " + tipoDesc, cx, cy + label.getTextSize(), label);
            cy += (int) (label.getTextSize() + 8);

            // divisor
            canvas.drawLine(cx, cy, right - cardPadding, cy, divider);
            cy += 14;

            // Campos
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

        // Resumo geral no rodapé da página atual
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

        // salvar / compartilhar
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = getExternalFilesDir(null);

        String dataArq = new SimpleDateFormat("dd_MM_yyyy_HH_mm", ptBr)
                .format(new Date());
        File pdf = new File(dir, "relatorio_recolhimentos_" + dataArq + ".pdf");

        try (FileOutputStream fos = new FileOutputStream(pdf)) {
            doc.writeTo(fos);
            Toast.makeText(this, "PDF gerado em: " + pdf.getAbsolutePath(), Toast.LENGTH_LONG).show();

            // Se quiser, pode chamar aqui:
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

            // tenta mandar direto pro WhatsApp
            shareIntent.setPackage("com.whatsapp");
            startActivity(shareIntent);
        } catch (Exception e) {
            // fallback: qualquer app
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
