package benicio.solucoes.rifacampeo.utils;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PrinterTicketUtils {

    // ===============================
    //   FUNÇÃO PRINCIPAL
    // ===============================
    public static void printTicketFromHtml(Activity activity,
                                           BluetoothDevice printer,
                                           String html) {

        new Thread(() -> {
            try {
                TicketData data = parseHtml(html);
                printTicket(activity, printer, data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ===============================
    //   Remove acentos
    // ===============================
    private static String removeAccents(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        return n.replaceAll("[^\\p{ASCII}]", "");
    }

    // ===============================
    //   PARSE DO HTML
    // ===============================
    private static TicketData parseHtml(String html) {

        TicketData d = new TicketData();

        d.titulo = extract(html, "<div class=\"ticket-title\">(.*?)</div>");
        d.nome = extract(html, "Nome: (.*?)<br>");
        d.documento = extract(html, "Documento: (.*?)<br>");
        d.numero = extract(html, "Número: (.*?)<br>");
        d.data = extract(html, "Data: (.*?)<br>");
        d.hora = extract(html, "Hora: (.*?)<br>");
        d.loteria = extract(html, "Loteria: (.*?)<br>");
        d.validade = extract(html, "Validade: (.*?)<br>");
        d.valor = extract(html, "<h1>Valor: (.*?)</h1>");
        d.qrUrl = extract(html, "<img src=\"(.*?)\"");

        // PEGAR TODOS OS BILHETES
        d.bilhetes = extractAll(html, "<div class=\"number-item\">(.*?)</div>");

        return d;
    }

    // captura única
    private static String extract(String text, String regex) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(text);
        if (m.find()) return removeAccents(m.group(1).trim());
        return "";
    }

    // captura múltipla
    private static List<String> extractAll(String text, String regex) {
        List<String> lista = new ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(text);
        while (m.find()) {
            lista.add(removeAccents(m.group(1).trim()));
        }
        return lista;
    }

    // ===============================
    //   IMPRESSÃO ESC/POS
    // ===============================
    private static void printTicket(Activity activity,
                                    BluetoothDevice printer,
                                    TicketData d) throws Exception {

        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        BluetoothSocket socket = printer.createInsecureRfcommSocketToServiceRecord(uuid);
        socket.connect();
        OutputStream out = socket.getOutputStream();

        out.write(EscPosBase.init_printer());
        out.write(EscPosBase.alignCenter());

        out.write((d.titulo + "\n").getBytes());
        out.write("--------------------------------\n".getBytes());
        out.write(EscPosBase.alignLeft());

        out.write(("Nome: " + d.nome + "\n").getBytes());
        out.write(("Documento: " + d.documento + "\n").getBytes());
        out.write(("Numero: " + d.numero + "\n").getBytes());
        out.write(("Data: " + d.data + "\n").getBytes());
        out.write(("Hora: " + d.hora + "\n").getBytes());
        out.write(("Loteria: " + d.loteria + "\n").getBytes());
        out.write(("Validade: " + d.validade + "\n").getBytes());
        out.write(("Valor: " + d.valor + "\n\n").getBytes());

        out.write("--------------------------------\n".getBytes());
        out.write(EscPosBase.alignCenter());
        out.write("NUMEROS\n\n".getBytes());

        // 🌟 AGORA IMPRIME TODOS OS BILHETES 🌟
        for (String bilhete : d.bilhetes) {
            out.write((bilhete + "\n").getBytes());
        }

        out.write("\n".getBytes());

        // imprimir QR
        if (!d.qrUrl.isEmpty()) {
            Bitmap qr = downloadImage(d.qrUrl);
            if (qr != null) {
                byte[] qrBytes = PrinterConverter.bitmapToBytes(qr);
                out.write(qrBytes);
            }
        }

        out.write("\nBoa sorte!\n".getBytes());
        out.write("--------------------------------\n".getBytes());

        out.write(EscPosBase.nextLine(4));
        out.flush();

        socket.close();
    }


    // ===============================
    //  DOWNLOAD DO QR
    // ===============================
    private static Bitmap downloadImage(String url) {
        try {
            InputStream in = new URL(url).openStream();
            return BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            return null;
        }
    }

    // ===============================
    //  OBJETO DE DADOS
    // ===============================
    private static class TicketData {
        String titulo;
        String nome;
        String documento;
        String numero;
        String data;
        String hora;
        String loteria;
        String validade;
        String valor;
        String qrUrl;
        List<String> bilhetes = new ArrayList<>();
    }
}
