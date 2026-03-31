package benicio.solucoes.rifacampeo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import benicio.solucoes.rifacampeo.adapters.AdapterVendedores;
import benicio.solucoes.rifacampeo.databinding.ActivityVendedoresBinding;
import benicio.solucoes.rifacampeo.databinding.LayoutInputRecolhedorBinding;
import benicio.solucoes.rifacampeo.databinding.LayoutInputVendedorBinding;
import benicio.solucoes.rifacampeo.objects.BilheteModel;
import benicio.solucoes.rifacampeo.objects.QueryModelEmpty;
import benicio.solucoes.rifacampeo.objects.RecolheuModel;
import benicio.solucoes.rifacampeo.objects.RecolhimentoResponse;
import benicio.solucoes.rifacampeo.objects.RetornoModel;
import benicio.solucoes.rifacampeo.objects.VendedorModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VendedoresActivity extends AppCompatActivity {

    public static AlertDialog loadingDialog;

    public static Dialog dialogVendedor;
    public static ActivityVendedoresBinding mainBinding;
    public static List<VendedorModel> vendedores = new ArrayList<>();
    public static AdapterVendedores adapterVendedores;
    public static RecyclerView rvVendedores;

    Dialog drelatoriosaldo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityVendedoresBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        mainBinding.novovendedor.setOnClickListener(v -> configuarDialog());
        mainBinding.senharecolhe.setOnClickListener(v -> configuarDialogRecolhedor());

        drelatoriosaldo = new AlertDialog.Builder(this).setMessage("Gerando...").setCancelable(false).create();
        mainBinding.relatoriosaldo.setOnClickListener(v -> {


            drelatoriosaldo.show();

            RetrofitUtils.getApiService()
                    .retornar_recolhimento(null, null, null, null, 999999999, 1)
                    .enqueue(new Callback<RecolhimentoResponse>() {
                        @Override
                        public void onResponse(Call<RecolhimentoResponse> call, Response<RecolhimentoResponse> response) {
                            if (response.isSuccessful()) {
                                gerarPdfVendedores(vendedores, "", response.body().itens);
                                drelatoriosaldo.dismiss();
                            }
                        }

                        @Override
                        public void onFailure(Call<RecolhimentoResponse> call, Throwable throwable) {
                            Toast.makeText(VendedoresActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            drelatoriosaldo.dismiss();
                        }
                    });

        });

        rvVendedores = mainBinding.rvvendedores;
        configurarRV();

        mainBinding.edtFiltroVendedor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapterVendedores != null) {
                    adapterVendedores.filtrarPorNome(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mainBinding.relatoriovendedor.setOnClickListener(v -> {
            showLoadingDialog(this);

            RetrofitUtils.getApiService().returnBilhetes(3, new QueryModelEmpty())
                    .enqueue(new Callback<List<BilheteModel>>() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void onResponse(Call<List<BilheteModel>> call, Response<List<BilheteModel>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<BilheteModel> bilhetes = response.body();
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

        mainBinding.recolhimento.setOnClickListener(v2 -> {
            Intent i = new Intent(this, RecolheRelatorioActivity.class);
            i.putExtra("recolhedor", false);
            startActivity(i);
        });
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

//        canvas.drawText("Filtro documento: " + filtroTexto, margin, y + sub.getTextSize(), sub);
//        y += (int) (sub.getTextSize() + 10);

        canvas.drawLine(margin, y, margin + contentW, y, divider);
        y += 18;

        return y;
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
            String Recolhedor = vendedor.getDocumento();//!isBlank(vendedor.getDocumento()) ? vendedor.getNome().trim() : "-";
            Log.d("mayara", "gerarPdfVendedores: " + Recolhedor);
            float saldoAtual = vendedor.getSaldoAtual(recolhimentos != null ? recolhimentos : new ArrayList<>());
            String saldoFmt = String.format(ptBr, "R$ %.2f", saldoAtual);

            String linha = "Nome vendedor: " + nome + "    Saldo deve: " + saldoFmt + "    Recolhedor: " + Recolhedor;

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

    @Override
    protected void onStart() {
        super.onStart();
        listarVendedores(this);
    }

    public static void listarVendedores(Activity context) {
        showLoadingDialog(context);

        vendedores.clear();

        RetrofitUtils.getApiService()
                .returnVendedores(1, new QueryModelEmpty())
                .enqueue(new Callback<List<VendedorModel>>() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onResponse(Call<List<VendedorModel>> call,
                                           Response<List<VendedorModel>> response) {

                        hideLoadingDialog();

                        if (response.isSuccessful() && response.body() != null) {
                            vendedores.clear();
                            vendedores.addAll(response.body());

                            if (adapterVendedores == null) {
                                adapterVendedores = new AdapterVendedores(vendedores, context);
                                rvVendedores.setLayoutManager(new LinearLayoutManager(context));
                                rvVendedores.setHasFixedSize(true);
                                rvVendedores.setAdapter(adapterVendedores);
                            } else {
                                adapterVendedores.atualizarLista(vendedores);
                            }

                            String textoFiltro = mainBinding.edtFiltroVendedor.getText().toString();
                            adapterVendedores.filtrarPorNome(textoFiltro);

                        } else {
                            Toast.makeText(context,
                                    "Erro de conexão ao carregar vendedores",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<VendedorModel>> call, Throwable throwable) {
                        hideLoadingDialog();
                        Toast.makeText(context,
                                "Falha na requisição ao carregar vendedores",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void configurarRV() {
        rvVendedores.setLayoutManager(new LinearLayoutManager(this));
        rvVendedores.setHasFixedSize(true);
        adapterVendedores = new AdapterVendedores(vendedores, this);
        rvVendedores.setAdapter(adapterVendedores);
    }

    private void configuarDialogRecolhedor() {
        AlertDialog.Builder b = new AlertDialog.Builder(VendedoresActivity.this);

        LayoutInputRecolhedorBinding inputRecolhedorBinding =
                LayoutInputRecolhedorBinding.inflate(getLayoutInflater());

        inputRecolhedorBinding.cadatrar.setOnClickListener(v -> {

            String nome = inputRecolhedorBinding.edtNome.getText().toString().trim();
            String celular = inputRecolhedorBinding.edtCelular.getText().toString().trim();
            String senha = inputRecolhedorBinding.edtSenha.getText().toString().trim();

            if (nome.isEmpty()) {
                Toast.makeText(this, "Nome não pode ser vazio", Toast.LENGTH_SHORT).show();
                return;
            }

            if (celular.isEmpty()) {
                Toast.makeText(this, "Celular não pode ser vazio", Toast.LENGTH_SHORT).show();
                return;
            }

            if (senha.length() != 6) {
                Toast.makeText(this, "A senha precisa ter 6 dígitos numéricos!", Toast.LENGTH_SHORT).show();
                return;
            }

            int comissao = 0;
            int limiteAposta = 0;
            String despesas = "";
            String documento = "";
            boolean ativo = true;

            RetrofitUtils.getApiService().saveVendedores(new VendedorModel(
                    celular,
                    nome,
                    UUID.randomUUID().toString(),
                    senha,
                    despesas,
                    "",
                    comissao,
                    ativo,
                    documento,
                    limiteAposta
            )).enqueue(new Callback<RetornoModel>() {
                @Override
                public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(VendedoresActivity.this, "Cadastrado!", Toast.LENGTH_SHORT).show();
                        listarVendedores(VendedoresActivity.this);
                        dialogVendedor.dismiss();
                    } else {
                        Toast.makeText(VendedoresActivity.this, "Problema de Conexão!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<RetornoModel> call, Throwable throwable) {
                    Toast.makeText(VendedoresActivity.this, "Falha ao cadastrar!", Toast.LENGTH_SHORT).show();
                }
            });
        });

        b.setView(inputRecolhedorBinding.getRoot());
        dialogVendedor = b.create();
        dialogVendedor.show();
    }

    private void configuarDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(VendedoresActivity.this);
        LayoutInputVendedorBinding inputVendedorBinding =
                LayoutInputVendedorBinding.inflate(getLayoutInflater());

        configurarAutocompleteRecolhe(inputVendedorBinding);

        inputVendedorBinding.edtDocumento.setOnClickListener(v -> {
            if (inputVendedorBinding.edtDocumento.getText().toString().trim().length() >= 2) {
                inputVendedorBinding.edtDocumento.showDropDown();
            }
        });

        inputVendedorBinding.edtDocumento.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && inputVendedorBinding.edtDocumento.getText().toString().trim().length() >= 2) {
                inputVendedorBinding.edtDocumento.showDropDown();
            }
        });

        inputVendedorBinding.cadatrar.setOnClickListener(v -> {
            String nome = inputVendedorBinding.edtNome.getText().toString().trim();
            String recolhe = inputVendedorBinding.edtDocumento.getText().toString().trim();
            String celular = inputVendedorBinding.edtCelular.getText().toString().trim();
            String despesas = inputVendedorBinding.edtDespesas.getText().toString().trim();
            String senha = inputVendedorBinding.edtSenha.getText().toString().trim();
            String comissaoStr = inputVendedorBinding.edtComissao.getText().toString().trim();
            String limiteApostaStr = inputVendedorBinding.edtLimiteaposta.getText().toString().trim();

            if (nome.isEmpty()) {
                inputVendedorBinding.edtNome.setError("Nome é obrigatório");
                inputVendedorBinding.edtNome.requestFocus();
                return;
            }

            if (recolhe.isEmpty()) {
                inputVendedorBinding.edtDocumento.setError("Recolhe é obrigatório");
                inputVendedorBinding.edtDocumento.requestFocus();
                return;
            }

            if (!nomeExisteNaLista(recolhe)) {
                inputVendedorBinding.edtDocumento.setError("Selecione um nome válido da lista");
                inputVendedorBinding.edtDocumento.requestFocus();
                return;
            }

            if (celular.isEmpty()) {
                inputVendedorBinding.edtCelular.setError("Celular é obrigatório");
                inputVendedorBinding.edtCelular.requestFocus();
                return;
            }

            if (comissaoStr.isEmpty()) {
                inputVendedorBinding.edtComissao.setError("Comissão não pode ser vazia");
                inputVendedorBinding.edtComissao.requestFocus();
                return;
            }

            if (senha.length() != 6) {
                inputVendedorBinding.edtSenha.setError("A senha precisa ter 6 dígitos numéricos");
                inputVendedorBinding.edtSenha.requestFocus();
                return;
            }

            int limiteAposta = 0;
            try {
                if (!limiteApostaStr.isEmpty()) {
                    limiteAposta = (int) Double.parseDouble(limiteApostaStr);
                }
            } catch (Exception e) {
                inputVendedorBinding.edtLimiteaposta.setError("Limite de aposta inválido");
                inputVendedorBinding.edtLimiteaposta.requestFocus();
                return;
            }

            int comissao;
            try {
                comissao = (int) Double.parseDouble(comissaoStr);
            } catch (Exception e) {
                inputVendedorBinding.edtComissao.setError("Comissão inválida");
                inputVendedorBinding.edtComissao.requestFocus();
                return;
            }

            RetrofitUtils.getApiService().saveVendedores(new VendedorModel(
                    celular,
                    nome,
                    UUID.randomUUID().toString(),
                    senha,
                    despesas,
                    "",
                    comissao,
                    inputVendedorBinding.radioAtivo.isChecked(),
                    recolhe,
                    limiteAposta
            )).enqueue(new Callback<RetornoModel>() {
                @Override
                public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(VendedoresActivity.this, "Cadastrado!", Toast.LENGTH_SHORT).show();
                        listarVendedores(VendedoresActivity.this);
                        dialogVendedor.dismiss();
                    } else {
                        Toast.makeText(VendedoresActivity.this, "Problema de Conexão!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<RetornoModel> call, Throwable throwable) {
                    Toast.makeText(VendedoresActivity.this, "Falha ao cadastrar!", Toast.LENGTH_SHORT).show();
                }
            });
        });

        b.setView(inputVendedorBinding.getRoot());
        dialogVendedor = b.create();
        dialogVendedor.show();
    }

    private void configurarAutocompleteRecolhe(LayoutInputVendedorBinding inputVendedorBinding) {
        List<String> nomesVendedores = obterNomesVendedores();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                nomesVendedores
        );

        inputVendedorBinding.edtDocumento.setAdapter(adapter);
        inputVendedorBinding.edtDocumento.setThreshold(2);
    }

    private List<String> obterNomesVendedores() {
        Set<String> nomesUnicos = new LinkedHashSet<>();

        for (VendedorModel vendedor : vendedores) {
            if (vendedor != null && vendedor.getNome() != null) {
                String nome = vendedor.getNome().trim();
                if (!nome.isEmpty()) {
                    nomesUnicos.add(nome);
                }
            }
        }

        return new ArrayList<>(nomesUnicos);
    }

    private boolean nomeExisteNaLista(String nomeDigitado) {
        String nomeNormalizadoDigitado = normalizarTexto(nomeDigitado);

        for (VendedorModel vendedor : vendedores) {
            if (vendedor != null && vendedor.getNome() != null) {
                String nomeLista = normalizarTexto(vendedor.getNome().trim());
                if (nomeLista.equals(nomeNormalizadoDigitado)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String normalizarTexto(String texto) {
        if (texto == null) return "";
        String textoNormalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        textoNormalizado = textoNormalizado.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return textoNormalizado.trim().toLowerCase();
    }

    private void gerarPdfVendedores(List<VendedorModel> vendedores,
                                    List<BilheteModel> bilhetes) {

        if (vendedores == null || vendedores.isEmpty()) {
            Toast.makeText(this, "Nenhum vendedor encontrado para gerar o relatório.", Toast.LENGTH_SHORT).show();
            hideLoadingDialog();
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

        y = drawHeaderVendedores(canvas, title, sub, divider, CONTENT_W, MARGIN, y);

        float saldoGeral = 0f;

        for (VendedorModel v : vendedores) {

            int somaBilhetes = 0;
            if (bilhetes != null) {
                for (BilheteModel b : bilhetes) {
                    if (v.getNome() != null &&
                            v.getNome().equals(b.getNome_vendedor())) {

                        somaBilhetes += b.getValorBilheteTotal();
                    }
                }
            }

            float saldoTotal = (v.getRecebimento() + somaBilhetes) - v.getPagamento();
            float comissaoGanha = (saldoTotal * v.getComissao()) / 100f;
            float saldo = saldoTotal - comissaoGanha;

            String totalFmt = String.format(ptBr, "R$ %.2f", saldoTotal);
            String comissaoFmt = String.format(ptBr, "R$ %.2f", comissaoGanha);
            String saldoFmt = String.format(ptBr, "R$ %.2f", saldo);

            saldoGeral += saldo;

            int cardPadding = 18;
            int lineHeight = (int) (value.getTextSize() + 12);
            int numLinhasBody = 5 + 1 + 1;
            int headerH = (int) (label.getTextSize() + 26);
            int cardH = cardPadding * 2 + headerH + (numLinhasBody * lineHeight);

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

            canvas.drawText("Vendedor: " + safe(v.getNome()), cx, cy + label.getTextSize(), label);
            cy += (int) (label.getTextSize() + 8);

            canvas.drawLine(cx, cy, right - cardPadding, cy, divider);
            cy += 14;

            canvas.drawText("Senha: " + safe(v.getSenha()), cx, cy, value);
            cy += lineHeight;
            canvas.drawText("Despesas: " + safe(v.getDespesas()), cx, cy, value);
            cy += lineHeight;
            canvas.drawText("Comissão: " + v.getComissao() + "%", cx, cy, value);
            cy += lineHeight;
            canvas.drawText("Ativado: " + v.isAtivado(), cx, cy, value);
            cy += lineHeight;
            canvas.drawText("Celular: " + safe(v.getNumeroCelular()), cx, cy, value);
            cy += lineHeight;

            cy += 6;
            canvas.drawLine(cx, cy, right - cardPadding, cy, divider);
            cy += 16;

            canvas.drawText("Total: " + totalFmt + "   |   Comissão: " + comissaoFmt, cx, cy, value);
            cy += lineHeight;

            Paint saldoPaint = new Paint(value);
            saldoPaint.setFakeBoldText(true);
            saldoPaint.setTextSize(15f);
            cy += 4;
            canvas.drawText("Saldo: " + saldoFmt, cx, cy, saldoPaint);

            y = (int) (bottom + 28);
        }

        String resumoGeral = "Saldo total de todos os vendedores: " +
                String.format(ptBr, "R$ %.2f", saldoGeral);
        canvas.drawText(resumoGeral, MARGIN, PAGE_H - 40, label);

        drawFooterVendedores(canvas, small, MARGIN, PAGE_W, PAGE_H, pageNum);
        doc.finishPage(page);

        File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = getExternalFilesDir(null);
        String dataArq = new java.text.SimpleDateFormat("dd_MM_yyyy_HH_mm", ptBr)
                .format(new java.util.Date());
        File pdf = new File(dir, "relatorio_vendedores_" + dataArq + ".pdf");

        try (FileOutputStream fos = new FileOutputStream(pdf)) {
            doc.writeTo(fos);
            compartilharPdfNoWhatsApp(pdf);
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

        String data = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR"))
                .format(new java.util.Date());
        canvas.drawText("Gerado em " + data, margin, y + sub.getTextSize(), sub);
        y += (int) (sub.getTextSize() + 8);

        canvas.drawLine(margin, y, margin + contentW, y, divider);
        y += 12;
        return y;
    }

    private void drawFooterVendedores(Canvas canvas, Paint small, int margin, int pageW, int pageH, int pageNum) {
        String left = "© " + java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) +
                " • Sistema de Relatórios";
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

    public static void showLoadingDialog(Activity context) {
        if (loadingDialog != null && loadingDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 40);
        layout.setGravity(Gravity.CENTER);

        ProgressBar progressBar = new ProgressBar(context);
        TextView text = new TextView(context);
        text.setText("Gerando relatório...\nAguarde.");
        text.setGravity(Gravity.CENTER);
        text.setPadding(0, 24, 0, 0);

        layout.addView(progressBar);
        layout.addView(text);

        builder.setView(layout);

        loadingDialog = builder.create();
        loadingDialog.show();
    }

    public static void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}