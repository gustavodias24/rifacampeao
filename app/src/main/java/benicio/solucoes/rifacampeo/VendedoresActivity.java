package benicio.solucoes.rifacampeo;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import benicio.solucoes.rifacampeo.adapters.AdapterVendedores;
import benicio.solucoes.rifacampeo.databinding.ActivityAdminMasterBinding;
import benicio.solucoes.rifacampeo.databinding.ActivityVendedoresBinding;
import benicio.solucoes.rifacampeo.databinding.LayoutInputVendedorBinding;
import benicio.solucoes.rifacampeo.objects.BilheteModel;
import benicio.solucoes.rifacampeo.objects.QueryModelEmpty;
import benicio.solucoes.rifacampeo.objects.RetornoModel;
import benicio.solucoes.rifacampeo.objects.VendedorModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VendedoresActivity extends AppCompatActivity {

    private AlertDialog loadingDialog;

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

        mainBinding.relatoriovendedor.setOnClickListener(v -> {

            showLoadingDialog();

            // Chama a API UMA vez só
            RetrofitUtils.getApiService().returnBilhetes(3, new QueryModelEmpty())
                    .enqueue(new Callback<List<BilheteModel>>() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void onResponse(Call<List<BilheteModel>> call, Response<List<BilheteModel>> response) {
                            if (response.isSuccessful() && response.body() != null) {

                                List<BilheteModel> bilhetes = response.body();

                                // Agora que temos TODOS os bilhetes, gera o PDF
                                gerarPdfVendedores(vendedores, bilhetes);

                            } else {
                                Toast.makeText(VendedoresActivity.this,
                                        "Resposta inválida da API", Toast.LENGTH_SHORT).show();
                                        hideLoadingDialog();
                            }
                        }

                        @Override
                        public void onFailure(Call<List<BilheteModel>> call, Throwable t) {
                            Toast.makeText(VendedoresActivity.this,
                                    "Falha ao carregar bilhetes", Toast.LENGTH_SHORT).show();
                                        hideLoadingDialog();
                        }
                    });

        });

        mainBinding.recolhimento.setOnClickListener( v2 -> startActivity(new Intent(this, RecolhimentoActivity.class)));

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


    // Dentro da sua Activity (ex: RelatoriosActivity)

    // Dentro da sua Activity (ex: RelatoriosActivity)
    private void gerarPdfVendedores(List<VendedorModel> vendedores,
                                    List<BilheteModel> bilhetes) {

        if (vendedores == null || vendedores.isEmpty()) {
            Toast.makeText(this, "Nenhum vendedor encontrado para gerar o relatório.", Toast.LENGTH_SHORT).show();
            hideLoadingDialog();
            return;
        }

        final int PAGE_W = 595, PAGE_H = 842; // A4 ~72dpi
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

        // Cabeçalho (pode usar o mesmo que você já tinha)
        y = drawHeaderVendedores(canvas, title, sub, divider, CONTENT_W, MARGIN, y);

        float saldoGeral = 0f;

        for (VendedorModel v : vendedores) {

            // ================= SOMA DOS BILHETES DESSE VENDEDOR =================
            int somaBilhetes = 0;
            if (bilhetes != null) {
                for (BilheteModel b : bilhetes) {
                    if (v.getNome() != null &&
                            v.getNome().equals(b.getNome_vendedor())) {

                        somaBilhetes += b.getValorBilheteTotal(); // int -> soma em reais
                    }
                }
            }

            // ================= CÁLCULO IGUAL AO toStringVendedor =================
            float saldoTotal = (v.getRecebimento() + somaBilhetes) - v.getPagamento();
            float comissaoGanha = (saldoTotal * v.getComissao()) / 100f;
            float saldo = saldoTotal - comissaoGanha;

            String totalFmt    = String.format(ptBr, "R$ %.2f", saldoTotal);
            String comissaoFmt = String.format(ptBr, "R$ %.2f", comissaoGanha);
            String saldoFmt    = String.format(ptBr, "R$ %.2f", saldo);

            saldoGeral += saldo;

            // ================= LAYOUT / ESPAÇAMENTO =================
            int cardPadding = 18;
            int lineHeight = (int) (value.getTextSize() + 12);
            int numLinhasBody = 5 + 1 + 1; // dados + (total/ comissão) + saldo
            int headerH = (int) (label.getTextSize() + 26);
            int cardH = cardPadding * 2 + headerH + (numLinhasBody * lineHeight);

            // quebra de página se não couber
            if (y + cardH + 30 > PAGE_H - MARGIN) {
                drawFooterVendedores(canvas, small, MARGIN, PAGE_W, PAGE_H, pageNum);
                doc.finishPage(page);

                pageNum++;
                page = doc.startPage(
                        new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create()
                );
                canvas = page.getCanvas();
                y = MARGIN;
                y = drawHeaderVendedores(canvas, title, sub, divider, CONTENT_W, MARGIN, y);
            }

            float left = MARGIN;
            float top = y;
            float right = MARGIN + CONTENT_W;
            float bottom = y + cardH;

            canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, box);

            int cx = MARGIN + cardPadding;
            int cy = y + cardPadding;

            // Título
            canvas.drawText("Vendedor: " + safe(v.getNome()), cx, cy + label.getTextSize(), label);
            cy += (int) (label.getTextSize() + 8);

            // divisor
            canvas.drawLine(cx, cy, right - cardPadding, cy, divider);
            cy += 14;

            // Dados principais
            canvas.drawText("Senha: " + safe(v.getSenha()), cx, cy, value);           cy += lineHeight;
            canvas.drawText("Despesas: " + safe(v.getDespesas()), cx, cy, value);     cy += lineHeight;
            canvas.drawText("Comissão: " + v.getComissao() + "%", cx, cy, value);     cy += lineHeight;
            canvas.drawText("Ativado: " + v.isAtivado(), cx, cy, value);              cy += lineHeight;
            canvas.drawText("Celular: " + safe(v.getNumeroCelular()), cx, cy, value); cy += lineHeight;

            // separador dos valores
            cy += 6;
            canvas.drawLine(cx, cy, right - cardPadding, cy, divider);
            cy += 16;

            // Total + Comissão
            canvas.drawText("Total: " + totalFmt + "   |   Comissão: " + comissaoFmt, cx, cy, value);
            cy += lineHeight;

            // Saldo maior e negrito
            Paint saldoPaint = new Paint(value);
            saldoPaint.setFakeBoldText(true);
            saldoPaint.setTextSize(15f);
            cy += 4;
            canvas.drawText("Saldo: " + saldoFmt, cx, cy, saldoPaint);

            y = (int) (bottom + 28);
        }

        // Resumo geral
        String resumoGeral = "Saldo total de todos os vendedores: " +
                String.format(ptBr, "R$ %.2f", saldoGeral);
        canvas.drawText(resumoGeral, MARGIN, PAGE_H - 40, label);

        drawFooterVendedores(canvas, small, MARGIN, PAGE_W, PAGE_H, pageNum);
        doc.finishPage(page);

        // salvar / compartilhar
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = getExternalFilesDir(null);
        String dataArq = new java.text.SimpleDateFormat("dd_MM_yyyy_HH_mm", ptBr)
                .format(new java.util.Date());
        File pdf = new File(dir, "relatorio_vendedores_" + dataArq + ".pdf");

        try (FileOutputStream fos = new FileOutputStream(pdf)) {
            doc.writeTo(fos);
             compartilharPdfNoWhatsApp(pdf); // se quiser
             hideLoadingDialog();
        } catch (IOException e) {
            hideLoadingDialog();
            Toast.makeText(this, "Erro ao salvar PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            hideLoadingDialog();
            doc.close();
        }
    }

    private int drawHeaderVendedores(Canvas canvas, Paint title, Paint sub, Paint divider, int contentW, int margin, int y) {
        canvas.drawText("Relatório de Vendedores", margin, y + title.getTextSize(), title);
        y += (int) (title.getTextSize() + 6);

        String data = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR")).format(new java.util.Date());
        canvas.drawText("Gerado em " + data, margin, y + sub.getTextSize(), sub);
        y += (int) (sub.getTextSize() + 8);

        canvas.drawLine(margin, y, margin + contentW, y, divider);
        y += 12;
        return y;
    }

    private void drawFooterVendedores(Canvas canvas, Paint small, int margin, int pageW, int pageH, int pageNum) {
        String left = "© " + java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + " • Sistema de Relatórios";
        String right = "Página " + pageNum;
        canvas.drawText(left, margin, pageH - 14, small);
        float rightW = small.measureText(right);
        canvas.drawText(right, pageW - margin - rightW, pageH - 14, small);
    }

    private String safe(Object v) {
        return (v == null) ? "" : v.toString();
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

    private void showLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false); // não pode fechar
        builder.setCancelable(false);

        // Layout simples com ProgressBar + texto
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 40);
        layout.setGravity(Gravity.CENTER);

        ProgressBar progressBar = new ProgressBar(this);
        TextView text = new TextView(this);
        text.setText("Gerando relatório...\nAguarde.");
        text.setGravity(Gravity.CENTER);
        text.setPadding(0, 24, 0, 0);

        layout.addView(progressBar);
        layout.addView(text);

        builder.setView(layout);

        loadingDialog = builder.create();
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }


}