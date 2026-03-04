package benicio.solucoes.rifacampeo.adapters;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import benicio.solucoes.rifacampeo.R;
import benicio.solucoes.rifacampeo.objects.RecolheuModel;

public class AdapterRecolhimento extends RecyclerView.Adapter<AdapterRecolhimento.MyViewHolder> {

    private final List<RecolheuModel> lista;
    private final Activity a;

    public AdapterRecolhimento(List<RecolheuModel> lista, Activity a) {
        this.lista = lista;
        this.a = a;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_recolhimento, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        RecolheuModel item = lista.get(position);

        holder.infos_recolhimento.setText(
                HtmlCompat.fromHtml(item.toStringHtml(), HtmlCompat.FROM_HTML_MODE_LEGACY)
        );

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            RecolheuModel clicked = lista.get(pos);
            compartilharRecolhimento(clicked);
        });
    }

    @Override
    public int getItemCount() {
        return (lista == null) ? 0 : lista.size();
    }

    private void compartilharRecolhimento(RecolheuModel item) {
        String mensagem = montarMensagemWhatsApp(item);

        // 1) Business > 2) Normal
        String pkg = getWhatsAppPreferido();

        if (pkg != null) {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, mensagem);
            send.setPackage(pkg);
            a.startActivity(send);
            return;
        }

        // 3) Sem WhatsApp => abre no navegador
        abrirNoNavegadorComMensagem(mensagem);
    }

    private String getWhatsAppPreferido() {
        if (isPackageInstalled("com.whatsapp.w4b")) return "com.whatsapp.w4b"; // Business primeiro
        if (isPackageInstalled("com.whatsapp")) return "com.whatsapp";         // Normal depois
        return null;
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            a.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void abrirNoNavegadorComMensagem(String mensagem) {
        String url = "https://wa.me/?text=" + Uri.encode(mensagem);
        Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

        if (browser.resolveActivity(a.getPackageManager()) != null) {
            a.startActivity(browser);
        } else {
            Toast.makeText(a, "Nenhum navegador disponível para abrir o link.", Toast.LENGTH_SHORT).show();
        }
    }

    private String montarMensagemWhatsApp(RecolheuModel r) {
        Locale ptBr = new Locale("pt", "BR");
        String valorFmt = String.format(ptBr, "R$ %.2f", r.getValor());

        String tipoDesc = (r.getTipo() == 0) ? "Recolhimento"
                : (r.getTipo() == 1) ? "Pagamento"
                : "Tipo " + r.getTipo();

        String vendedor = (r.getVendedor() == null || r.getVendedor().trim().isEmpty()) ? "-" : r.getVendedor().trim();
        String data = (r.getDataHoraAtual() == null || r.getDataHoraAtual().trim().isEmpty()) ? "-" : r.getDataHoraAtual().trim();
        String recolhedor = (r.getRecolhedor() == null || r.getRecolhedor().trim().isEmpty()) ? "-" : r.getRecolhedor().trim();

        StringBuilder sb = new StringBuilder();
        sb.append("📌 *").append(tipoDesc).append("*\n\n");
        sb.append("*Vendedor:* ").append(vendedor).append("\n");
        sb.append("*Valor:* ").append(valorFmt).append("\n");
        sb.append("*Data:* ").append(data).append("\n");
        sb.append("*Recolhedor:* ").append(recolhedor).append("\n");

        if (r.getObservacoes() != null && !r.getObservacoes().trim().isEmpty()) {
            sb.append("\n*Observações:* ").append(r.getObservacoes().trim()).append("\n");
        }

        return sb.toString();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView infos_recolhimento;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            infos_recolhimento = itemView.findViewById(R.id.infos_recolhimento);
        }
    }
}