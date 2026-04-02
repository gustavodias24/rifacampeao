package benicio.solucoes.rifacampeo.adapters;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import benicio.solucoes.rifacampeo.R;
import benicio.solucoes.rifacampeo.objects.GanhadorModel;
import benicio.solucoes.rifacampeo.objects.RetornoModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GanhadorAdapter extends RecyclerView.Adapter<GanhadorAdapter.VH> implements Filterable {

    public interface PrinterBridge {
        void imprimirGanhadorDireto(GanhadorModel item);
    }

    private final Activity activity;
    private final List<GanhadorModel> items = new ArrayList<>();
    private final List<GanhadorModel> filtered = new ArrayList<>();
    private final PrinterBridge printerBridge;

    private boolean removerDelete;

    public GanhadorAdapter(Activity activity, boolean removerDelete) {
        this.activity = activity;
        this.removerDelete = removerDelete;

        if (activity instanceof PrinterBridge) {
            this.printerBridge = (PrinterBridge) activity;
        } else {
            this.printerBridge = null;
        }
    }

    public void setItems(List<GanhadorModel> novos) {
        items.clear();
        if (novos != null) items.addAll(novos);
        filtered.clear();
        filtered.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ganhador, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        GanhadorModel g = filtered.get(pos);

        if (removerDelete) {
            h.btnDeletar.setVisibility(View.GONE);
        }

        h.tvLoteria.setText(g.getLoteria() != null ? g.getLoteria() : "-");
        h.tvData.setText(g.getData_lancada() != null ? g.getData_lancada() : "-");
        h.tvNumeros.setText(formatarNumerosLinha(g));

        h.btnImprimir.setOnClickListener(v -> {
            int position = h.getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;

            GanhadorModel item = filtered.get(position);
            imprimirResultadoDireto(item);
        });

        h.btnPdf.setOnClickListener(v -> {
            int position = h.getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;

            GanhadorModel item = filtered.get(position);
            gerarPdfDoItem(item);
        });

        h.btnDeletar.setOnClickListener(v -> {
            int position = h.getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;

            GanhadorModel item = filtered.get(position);

            new androidx.appcompat.app.AlertDialog.Builder(activity)
                    .setTitle("Confirmar exclusão")
                    .setMessage("Deseja realmente deletar este resultado?")
                    .setPositiveButton("Sim", (dialog, which) -> {
                        RetrofitUtils.getApiService().ganhador_delete(item.get_id())
                                .enqueue(new Callback<RetornoModel>() {
                                    @Override
                                    public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
                                        if (response.isSuccessful()) {
                                            GanhadorModel removed = filtered.remove(position);
                                            items.remove(removed);
                                            notifyItemRemoved(position);
                                            Toast.makeText(activity, "Item removido com sucesso", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(activity, "Erro ao deletar item", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<RetornoModel> call, Throwable throwable) {
                                        Toast.makeText(activity, "Falha ao deletar item", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton("Não", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    private void imprimirResultadoDireto(GanhadorModel item) {
        if (item == null) {
            Toast.makeText(activity, "Item inválido para imprimir", Toast.LENGTH_SHORT).show();
            return;
        }

        if (printerBridge == null) {
            Toast.makeText(activity, "A Activity não implementa PrinterBridge", Toast.LENGTH_LONG).show();
            return;
        }

        printerBridge.imprimirGanhadorDireto(item);
    }

    private void gerarPdfDoItem(GanhadorModel item) {
        if (item == null) {
            Toast.makeText(activity, "Item inválido para gerar PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        final int PAGE_W = 595;
        final int PAGE_H = 842;
        final int MARGIN = 40;
        final int CONTENT_W = PAGE_W - (MARGIN * 2);

        Paint title = new Paint();
        title.setTextSize(18f);
        title.setFakeBoldText(true);

        Paint text = new Paint();
        text.setTextSize(13f);

        Paint bold = new Paint();
        bold.setTextSize(13f);
        bold.setFakeBoldText(true);

        Paint numeroPaint = new Paint();
        numeroPaint.setTextSize(26f);
        numeroPaint.setFakeBoldText(true);

        Paint divider = new Paint();
        divider.setStrokeWidth(1f);
        divider.setAlpha(140);

        Paint small = new Paint();
        small.setTextSize(10f);
        small.setAlpha(180);

        PdfDocument doc = new PdfDocument();
        PdfDocument.Page page = doc.startPage(
                new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        );

        Canvas canvas = page.getCanvas();
        int y = MARGIN;

        String loteria = safe(item.getLoteria());
        String data = safe(item.getData_lancada());

        canvas.drawText("Resultado de Sorteio", MARGIN, y, title);
        y += 28;

        canvas.drawText("Loteria: " + loteria, MARGIN, y, bold);
        y += 24;

        canvas.drawText("Data: " + data, MARGIN, y, text);
        y += 20;

        canvas.drawLine(MARGIN, y, MARGIN + CONTENT_W, y, divider);
        y += 24;

        canvas.drawText("Números sorteados", MARGIN, y, bold);
        y += 40;

        canvas.drawText("1º  " + formatarNumero4(item.getNumero1()), MARGIN, y, numeroPaint);
        y += 36;

        canvas.drawText("2º  " + formatarNumero4(item.getNumero2()), MARGIN, y, numeroPaint);
        y += 36;

        canvas.drawText("3º  " + formatarNumero4(item.getNumero3()), MARGIN, y, numeroPaint);
        y += 36;

        canvas.drawText("4º  " + formatarNumero4(item.getNumero4()), MARGIN, y, numeroPaint);
        y += 36;

        canvas.drawText("5º  " + formatarNumero4(item.getNumero5()), MARGIN, y, numeroPaint);
        y += 36;

        canvas.drawText("6º  " + formatarNumero4(item.getNumero6()), MARGIN, y, numeroPaint);
        y += 40;

        String dataGeracao = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawLine(MARGIN, y, MARGIN + CONTENT_W, y, divider);
        y += 22;
        canvas.drawText("Gerado em: " + dataGeracao, MARGIN, y, small);

        canvas.drawText("Página 1", PAGE_W - MARGIN - small.measureText("Página 1"), PAGE_H - 18, small);

        doc.finishPage(page);

        File dir = activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = activity.getExternalFilesDir(null);

        String nomeSeguro = safe(item.getLoteria()).replaceAll("[^a-zA-Z0-9_-]", "_");
        String dataArquivo = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss", Locale.getDefault()).format(new Date());
        File pdfFile = new File(dir, "resultado_" + nomeSeguro + "_" + dataArquivo + ".pdf");

        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            doc.writeTo(fos);
            Toast.makeText(activity, "PDF gerado: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            compartilharPdf(pdfFile);
        } catch (IOException e) {
            Toast.makeText(activity, "Erro ao salvar PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            doc.close();
        }
    }

    private void compartilharPdf(File pdfFile) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    activity,
                    activity.getPackageName() + ".fileprovider",
                    pdfFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Segue o resultado em PDF 📄");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            activity.startActivity(Intent.createChooser(shareIntent, "Compartilhar PDF"));
        } catch (Exception e) {
            Toast.makeText(activity, "Erro ao compartilhar PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatarNumerosLinha(GanhadorModel g) {
        return formatarNumero4(g.getNumero1()) + ", "
                + formatarNumero4(g.getNumero2()) + ", "
                + formatarNumero4(g.getNumero3()) + ", "
                + formatarNumero4(g.getNumero4()) + ", "
                + formatarNumero4(g.getNumero5()) + ", "
                + formatarNumero4(g.getNumero6());
    }

    private String formatarNumero4(Object numero) {
        if (numero == null) return "0000";

        String numeroStr = String.valueOf(numero).trim();

        try {
            int valor = Integer.parseInt(numeroStr);
            return String.format(Locale.getDefault(), "%04d", valor);
        } catch (Exception e) {
            numeroStr = numeroStr.replaceAll("\\D+", "");
            if (numeroStr.isEmpty()) return "0000";

            if (numeroStr.length() >= 4) {
                return numeroStr.substring(numeroStr.length() - 4);
            }

            while (numeroStr.length() < 4) {
                numeroStr = "0" + numeroStr;
            }
            return numeroStr;
        }
    }

    private String safe(String s) {
        return s == null ? "-" : s.trim();
    }

    @Override
    public int getItemCount() {
        return filtered.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvLoteria, tvData, tvNumeros;
        ImageButton btnDeletar, btnImprimir, btnPdf;

        VH(@NonNull View itemView) {
            super(itemView);
            tvLoteria = itemView.findViewById(R.id.tvLoteria);
            tvData = itemView.findViewById(R.id.tvData);
            tvNumeros = itemView.findViewById(R.id.tvNumeros);
            btnDeletar = itemView.findViewById(R.id.btnDelete);
            btnImprimir = itemView.findViewById(R.id.btnImprimir);
            btnPdf = itemView.findViewById(R.id.btnPdf);
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String q = constraint != null ? constraint.toString().trim().toLowerCase() : "";
                List<GanhadorModel> out = new ArrayList<>();

                if (TextUtils.isEmpty(q)) {
                    out.addAll(items);
                } else {
                    for (GanhadorModel g : items) {
                        String lot = g.getLoteria() != null ? g.getLoteria().toLowerCase() : "";
                        String dat = g.getData_lancada() != null ? g.getData_lancada().toLowerCase() : "";
                        String nums = (formatarNumero4(g.getNumero1()) + " "
                                + formatarNumero4(g.getNumero2()) + " "
                                + formatarNumero4(g.getNumero3()) + " "
                                + formatarNumero4(g.getNumero4()) + " "
                                + formatarNumero4(g.getNumero5()) + " "
                                + formatarNumero4(g.getNumero6())).toLowerCase();

                        if (lot.contains(q) || dat.contains(q) || nums.contains(q)) {
                            out.add(g);
                        }
                    }
                }

                FilterResults fr = new FilterResults();
                fr.values = out;
                return fr;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filtered.clear();
                filtered.addAll((List<GanhadorModel>) results.values);
                notifyDataSetChanged();
            }
        };
    }
}