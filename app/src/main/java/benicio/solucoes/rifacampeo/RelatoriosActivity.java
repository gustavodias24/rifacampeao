package benicio.solucoes.rifacampeo;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import benicio.solucoes.rifacampeo.adapters.AdapterBilhetes;
import benicio.solucoes.rifacampeo.objects.BilheteModel;
import benicio.solucoes.rifacampeo.objects.QueryModelEmpty;
import benicio.solucoes.rifacampeo.objects.VendedorModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RelatoriosActivity extends AppCompatActivity {

    // Filtros (spinners e datas)
    private Spinner spNomeVendedor, spDocumentoVendedor, spLoteria;
    private EditText edtDataInicio, edtDataFim, edtIdBilhete;

    // Lista
    private RecyclerView rvBilhetes;
    private AdapterBilhetes adapterBilhetes;

    // Botões
    private Button btnBuscar, btnGerarPdf;

    // Dados
    private final List<BilheteModel> bilhetesAll = new ArrayList<>();
    private final List<BilheteModel> bilhetesFiltrados = new ArrayList<>();

    // Vendedores (para preencher os selects)
    private final List<VendedorModel> vendedores = new ArrayList<>();
    private final List<String> nomes = new ArrayList<>();
    private final List<String> documentos = new ArrayList<>();
    private ArrayAdapter<String> adapterNomes;
    private ArrayAdapter<String> adapterDocumentos;

    private final SimpleDateFormat sdfData = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Layout da tela
        setContentView(R.layout.activity_relatorios);

        bindViews();
        setupRecycler();
        setupPickers();

        setupSpinnerBase();     // Nome/Documento começa com "Todos"
        setupSpinnerLoteria();  // Loteria: Todos / FD / COR
        carregarVendedores();   // Preenche Nome/Documento via API

        btnBuscar.setOnClickListener(v -> carregarBilhetes());
        btnGerarPdf.setOnClickListener(v -> gerarPdf(bilhetesFiltrados));

        // Carrega lista inicial
        carregarBilhetes();
    }

    private void bindViews() {
        spNomeVendedor = findViewById(R.id.spNomeVendedor);
        spDocumentoVendedor = findViewById(R.id.spDocumentoVendedor);
        spLoteria = findViewById(R.id.spLoteria);
        edtDataInicio = findViewById(R.id.edtDataInicio);
        edtDataFim = findViewById(R.id.edtDataFim);
        edtIdBilhete = findViewById(R.id.spIdBilhete); // no XML o id é spIdBilhete mas é EditText
        rvBilhetes = findViewById(R.id.rvBilhetes);
        btnBuscar = findViewById(R.id.btnBuscar);
        btnGerarPdf = findViewById(R.id.btnGerarPdf);
    }

    private void setupRecycler() {
        rvBilhetes.setLayoutManager(new LinearLayoutManager(this));
        adapterBilhetes = new AdapterBilhetes(this, bilhetesFiltrados);
        rvBilhetes.setAdapter(adapterBilhetes);
    }

    private void setupPickers() {
        edtDataInicio.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog dp = new DatePickerDialog(
                    this,
                    (view, y, m, d) -> {
                        Calendar c = Calendar.getInstance();
                        c.set(y, m, d, 0, 0, 0);
                        edtDataInicio.setText(sdfData.format(c.getTime()));
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            dp.show();
        });

        edtDataFim.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog dp = new DatePickerDialog(
                    this,
                    (view, y, m, d) -> {
                        Calendar c = Calendar.getInstance();
                        c.set(y, m, d, 23, 59, 59);
                        edtDataFim.setText(sdfData.format(c.getTime()));
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            dp.show();
        });
    }

    /**
     * Inicializa os spinners de Nome/Documento com "Todos".
     * IMPORTANTE: agora NÃO sincroniza mais nome <-> documento.
     */
    private void setupSpinnerBase() {
        nomes.clear();
        documentos.clear();

        // item inicial padrão em cada spinner
        nomes.add("Todos");
        documentos.add("Todos");

        adapterNomes = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nomes);
        adapterNomes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spNomeVendedor.setAdapter(adapterNomes);

        adapterDocumentos = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, documentos);
        adapterDocumentos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDocumentoVendedor.setAdapter(adapterDocumentos);

        // ❌ Removido: nenhum setOnItemSelectedListener que force um spinner mudar o outro
    }

    /** Spinner Loteria: Todos / FD / COR */
    private void setupSpinnerLoteria() {
        String[] LOTERIAS = new String[]{"Todos", "FD", "COR"};
        ArrayAdapter<String> loteriaAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                LOTERIAS
        );
        loteriaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLoteria.setAdapter(loteriaAdapter);
    }

    /** Carrega Nome/Documento via API e preenche os spinners */
    private void carregarVendedores() {
        RetrofitUtils.getApiService().returnVendedores(1, new QueryModelEmpty())
                .enqueue(new Callback<List<VendedorModel>>() {
                    @Override
                    public void onResponse(Call<List<VendedorModel>> call, Response<List<VendedorModel>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            vendedores.clear();
                            vendedores.addAll(response.body());

                            // Recria as listas mantendo "Todos" como primeiro item
                            nomes.clear();
                            documentos.clear();
                            nomes.add("Todos");
                            documentos.add("Todos");

                            for (VendedorModel v : vendedores) {
                                nomes.add(safe(v.getNome()));
                                documentos.add(safe(v.getDocumento()));
                            }

                            adapterNomes.notifyDataSetChanged();
                            adapterDocumentos.notifyDataSetChanged();
                        } else {
                            Toast.makeText(RelatoriosActivity.this, "Erro de conexão ao carregar vendedores", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<VendedorModel>> call, Throwable t) {
                        Toast.makeText(RelatoriosActivity.this, "Falha na API de vendedores", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void carregarBilhetes() {
        RetrofitUtils.getApiService().returnBilhetes(3, new QueryModelEmpty())
                .enqueue(new Callback<List<BilheteModel>>() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onResponse(Call<List<BilheteModel>> call, Response<List<BilheteModel>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            bilhetesAll.clear();
                            bilhetesFiltrados.clear();

                            bilhetesAll.addAll(response.body());
                            Collections.reverse(bilhetesAll); // mais recentes primeiro, se essa for a ideia

                            aplicarFiltros();
                        } else {
                            Toast.makeText(RelatoriosActivity.this, "Resposta inválida da API", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<BilheteModel>> call, Throwable t) {
                        Toast.makeText(RelatoriosActivity.this, "Falha ao carregar bilhetes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void aplicarFiltros() {
        // pega o texto atual de cada filtro
        String nomeSel = spNomeVendedor.getSelectedItem() == null
                ? "" : spNomeVendedor.getSelectedItem().toString().trim();
        String docSel = spDocumentoVendedor.getSelectedItem() == null
                ? "" : spDocumentoVendedor.getSelectedItem().toString().trim();
        String lotSel = spLoteria.getSelectedItem() == null
                ? "" : spLoteria.getSelectedItem().toString().trim();

        // "Todos" = sem filtro
        if ("Todos".equalsIgnoreCase(nomeSel)) nomeSel = "";
        if ("Todos".equalsIgnoreCase(docSel)) docSel = "";
        if ("Todos".equalsIgnoreCase(lotSel)) lotSel = "";

        String dataIni = edtDataInicio.getText().toString().trim();
        String dataFim = edtDataFim.getText().toString().trim();
        String idBilheteFiltro = edtIdBilhete.getText().toString().trim();

        Date dIni = parseDateOrNull(dataIni);
        Date dFim = endOfDay(parseDateOrNull(dataFim));

        bilhetesFiltrados.clear();
        for (BilheteModel b : bilhetesAll) {
            if (matches(b, nomeSel, docSel, lotSel, dIni, dFim, idBilheteFiltro)) {
                bilhetesFiltrados.add(b);
            }
        }

        adapterBilhetes.notifyDataSetChanged();
    }

    /**
     * Checa se um bilhete atende aos filtros atuais.
     * (todos os filtros são aplicados em AND)
     */
    private boolean matches(
            BilheteModel b,
            String nome,
            String doc,
            String loteria,
            Date dIni,
            Date dFim,
            String idBilheteFiltro
    ) {

        boolean okNome      = isEmpty(nome)            || contains(b.getNome_vendedor(), nome);
        boolean okDoc       = isEmpty(doc)             || contains(b.getDocumento_vendedor(), doc);
        boolean okLot       = isEmpty(loteria)         || contains(b.getLoteria(), loteria);
        boolean okIdBilhete = isEmpty(idBilheteFiltro) || contains(b.get_id(), idBilheteFiltro);

        // data do bilhete -> assume que b.getData() já está "dd/MM/yyyy"
        Date dataBilhete = parseDateOrNull(formatDate(b.getData()));
        boolean okData = inRange(dataBilhete, dIni, dFim);

        return okNome && okDoc && okLot && okIdBilhete && okData;
    }

    private boolean inRange(Date value, Date start, Date end) {
        if (value == null) return start == null && end == null;
        if (start != null && value.before(start)) return false;
        if (end != null && value.after(end)) return false;
        return true; // inclusivo
    }

    private Date parseDateOrNull(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return sdfData.parse(s);
        } catch (ParseException e) {
            return null;
        }
    }

    private Date endOfDay(Date d) {
        if (d == null) return null;
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private boolean contains(String hay, String needle) {
        if (hay == null) return false;
        return normalize(hay).contains(normalize(needle));
    }

    private String normalize(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD);
        return n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase(Locale.ROOT);
    }

    private String formatDate(String raw) {
        // se já vier "dd/MM/yyyy", só retorna
        return raw;
    }

    // ====== PDF formatado + compartilhamento WhatsApp ======
    private void gerarPdf(List<BilheteModel> itens) {
        if (itens == null || itens.isEmpty()) {
            Toast.makeText(this, "Nada para gerar no PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        final int PAGE_W = 595, PAGE_H = 842;  // A4 em ~72dpi
        final int MARGIN = 36;
        final int CONTENT_W = PAGE_W - (MARGIN * 2);

        Paint title = new Paint();
        title.setTextSize(18f);
        title.setFakeBoldText(true);

        Paint sub = new Paint();
        sub.setTextSize(12.5f);

        Paint label = new Paint();
        label.setTextSize(12.5f);
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

        final int VALOR_UNITARIO = 10;
        final Locale PTBR = new Locale("pt", "BR");
        java.text.NumberFormat money = java.text.NumberFormat.getCurrencyInstance(PTBR);

        PdfDocument doc = new PdfDocument();
        int pageNum = 1;
        PdfDocument.Page page = doc.startPage(
                new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create()
        );
        Canvas canvas = page.getCanvas();

        int y = MARGIN;

        // Cabeçalho
        y = drawHeader(canvas, title, sub, divider, CONTENT_W, MARGIN, y);

        // Filtros atuais
        String filtros = resumoFiltros();
        y += drawWrappedText(canvas, "Filtros: " + filtros, sub, MARGIN, y, CONTENT_W) + 6;

        int totalNumeros = 0;
        double totalValor = 0;

        for (BilheteModel b : itens) {

            // linhas de conteúdo pra esse bilhete
            List<String> linhasBilhete = buildBilheteLines(b, VALOR_UNITARIO, money);

            int cardPadding = 10;
            int lineHeight = (int) (value.getTextSize() + 8); // mais espaço entre linhas

            final int TITLE_EXTRA_GAP = 8;
            int headerH = (int) (label.getTextSize() + 10 + TITLE_EXTRA_GAP);

            // quebra de texto por largura
            List<String> wrapped = new ArrayList<>();
            for (String ln : linhasBilhete) {
                wrapped.addAll(
                        wrapText(ln, value, CONTENT_W - (cardPadding * 2))
                );
            }

            int bodyH = wrapped.size() * lineHeight;
            int cardH = headerH + bodyH + (cardPadding * 2);

            // quebra de página se não couber
            if (y + cardH + 10 > PAGE_H - MARGIN) {
                drawFooter(canvas, small, MARGIN, PAGE_W, PAGE_H, pageNum);
                doc.finishPage(page);

                pageNum++;
                page = doc.startPage(
                        new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create()
                );
                canvas = page.getCanvas();
                y = MARGIN;
                y = drawHeader(canvas, title, sub, divider, CONTENT_W, MARGIN, y);
            }

            float left = MARGIN;
            float top = y;
            float right = MARGIN + CONTENT_W;
            float bottom = y + cardH;

            canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, box);

            int cx = MARGIN + cardPadding;
            int cy = y + cardPadding;

            String titulo = "Bilhete " + safe(b.getNumero()) + "  •  " + safe(b.getLoteria());
            canvas.drawText(titulo, cx, cy + label.getTextSize(), label);

            // divisor logo abaixo do título
            float dividerY = cy + label.getTextSize() + 4;
            canvas.drawLine(cx, dividerY, right - cardPadding, dividerY, divider);

            // desce pro corpo
            cy += headerH;

            for (String ln : wrapped) {
                canvas.drawText(ln, cx, cy, value);
                cy += lineHeight;
            }

            y = (int) bottom + 10;

            int qtd = (b.getNumeros() == null) ? 0 : b.getNumeros().size();
            totalNumeros += qtd;
            totalValor += qtd * VALOR_UNITARIO;
        }

        // Totais
        y += 6;
        canvas.drawLine(MARGIN, y, MARGIN + CONTENT_W, y, divider);
        y += 12;

        String totalTxt = "Total de números: " + totalNumeros +
                "    |    Valor total: " + money.format(totalValor);

        drawWrappedText(canvas, totalTxt, label, MARGIN, y, CONTENT_W);
        y += (int) (label.getTextSize()) + 10;

        drawFooter(canvas, small, MARGIN, PAGE_W, PAGE_H, pageNum);
        doc.finishPage(page);

        // salvar e compartilhar
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = getExternalFilesDir(null);
        File pdf = new File(dir, "relatorio_bilhetes_" + System.currentTimeMillis() + ".pdf");

        try (FileOutputStream fos = new FileOutputStream(pdf)) {
            doc.writeTo(fos);
            compartilharPdfNoWhatsApp(pdf);
        } catch (IOException e) {
            Toast.makeText(this, "Erro ao salvar PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            doc.close();
        }
    }

    private int drawHeader(Canvas canvas,
                           Paint title,
                           Paint sub,
                           Paint divider,
                           int contentW,
                           int margin,
                           int y) {

        canvas.drawText("Relatório de Bilhetes", margin, y + title.getTextSize(), title);
        y += (int) (title.getTextSize() + 6);

        String data = new java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                new java.util.Locale("pt", "BR")
        ).format(new java.util.Date());

        canvas.drawText("Gerado em " + data, margin, y + sub.getTextSize(), sub);
        y += (int) (sub.getTextSize() + 8);

        canvas.drawLine(margin, y, margin + contentW, y, divider);
        y += 12;

        return y;
    }

    private void drawFooter(Canvas canvas,
                            Paint small,
                            int margin,
                            int pageW,
                            int pageH,
                            int pageNum) {

        String left = "© " +
                java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) +
                " • Sistema de Relatórios";

        String right = "Página " + pageNum;

        canvas.drawText(left, margin, pageH - 14, small);

        float rightW = small.measureText(right);
        canvas.drawText(right, pageW - margin - rightW, pageH - 14, small);
    }

    @SuppressLint("DefaultLocale")
    private List<String> buildBilheteLines(
            BilheteModel b,
            int valorUnitario,
            java.text.NumberFormat money
    ) {
        List<String> linhas = new ArrayList<>();

        String id = safe(b.get_id());
        String data = safe(b.getData());
        String hora = safe(b.getHora());
        String usuario = safe(b.getId_usuario());
        String docVend = safe(b.getDocumento_vendedor());
        String nomeVend = safe(b.getNome_vendedor());
        String numeroBilhete = safe(b.getNumero());
        String loteria = safe(b.getLoteria());

        // ordena números
        List<Integer> nums = (b.getNumeros() == null)
                ? new ArrayList<>()
                : new ArrayList<>(b.getNumeros());
        Collections.sort(nums);
        String numsStr = nums.isEmpty() ? "-" : joinInts(nums, " ");

        int qtd = nums.size();
        String valorTotal = money.format(qtd * valorUnitario);
        String valorUnit = money.format(valorUnitario);

        linhas.add("\n\nID: " + id);
        linhas.add("Data: " + data + "    Hora: " + hora);
        linhas.add("Loteria: " + loteria + "    Nº Bilhete: " + numeroBilhete);
        linhas.add("Vendedor: " + nomeVend);
        linhas.add("Recolhe: " + docVend);
        linhas.add("ID do usuário: " + usuario);
        linhas.add("Números (" + qtd + "): " + numsStr);
        linhas.add("Valor unitário: " + valorUnit + "    Valor total: " + valorTotal);

        return linhas;
    }

    private int drawWrappedText(Canvas canvas,
                                String text,
                                Paint paint,
                                int x,
                                int y,
                                int maxW) {

        List<String> lines = wrapText(text, paint, maxW);
        int lineH = (int) (paint.getTextSize() + 6);

        for (String l : lines) {
            canvas.drawText(l, x, y + lineH, paint);
            y += lineH;
        }

        return lines.size() * lineH;
    }

    private List<String> wrapText(String text, Paint paint, int maxW) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            out.add("");
            return out;
        }

        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String w : words) {
            String test = (line.length() == 0) ? w : line + " " + w;
            if (paint.measureText(test) <= maxW) {
                line = new StringBuilder(test);
            } else {
                if (line.length() > 0) out.add(line.toString());

                // se a palavra sozinha já estoura o maxW, quebra ela
                if (paint.measureText(w) > maxW) {
                    out.addAll(breakLongWord(w, paint, maxW));
                    line = new StringBuilder();
                } else {
                    line = new StringBuilder(w);
                }
            }
        }

        if (line.length() > 0) {
            out.add(line.toString());
        }

        return out;
    }

    private List<String> breakLongWord(String w, Paint paint, int maxW) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();

        for (int i = 0; i < w.length(); i++) {
            String test = cur.toString() + w.charAt(i);
            if (paint.measureText(test) <= maxW) {
                cur.append(w.charAt(i));
            } else {
                if (cur.length() > 0) parts.add(cur.toString());
                cur = new StringBuilder().append(w.charAt(i));
            }
        }

        if (cur.length() > 0) {
            parts.add(cur.toString());
        }

        return parts;
    }

    private String joinInts(List<Integer> list, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) sb.append(sep);
        }
        return sb.toString();
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

    // Abrir visualizador de PDF (caso você queira inspecionar antes)
    private void abrirPdf(File pdf) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    pdf
            );
            Intent it = new Intent(Intent.ACTION_VIEW);
            it.setDataAndType(uri, "application/pdf");
            it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(it, "Abrir PDF com..."));
        } catch (Exception e) {
            Toast.makeText(this, "Instale um leitor de PDF para abrir o arquivo.", Toast.LENGTH_SHORT).show();
        }
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    @SuppressLint("DefaultLocale")
    private String resumoFiltros() {
        String nomeSel = (spNomeVendedor.getSelectedItem() == null)
                ? "" : spNomeVendedor.getSelectedItem().toString();
        String docSel = (spDocumentoVendedor.getSelectedItem() == null)
                ? "" : spDocumentoVendedor.getSelectedItem().toString();
        String loteriaSel = (spLoteria.getSelectedItem() == null)
                ? "" : spLoteria.getSelectedItem().toString();

        return String.format(
                Locale.getDefault(),
                "Nome=%s, Doc=%s, DataIni=%s, DataFim=%s, Loteria=%s, ID=%s",
                safe(nomeSel),
                safe(docSel),
                safe(edtDataInicio.getText().toString()),
                safe(edtDataFim.getText().toString()),
                safe(loteriaSel),
                safe(edtIdBilhete.getText().toString())
        );
    }
}
