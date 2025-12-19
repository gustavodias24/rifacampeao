package benicio.solucoes.rifacampeo.utils;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrinterTicketUtils {

    public static void printTicketFromHtml(Activity activity,
                                           BluetoothDevice printer,
                                           String html) {

        new Thread(() -> {
            try {
                TicketData data = parseHtml(html);
                printTicket(printer, data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static String removeAccents(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        return n.replaceAll("[^\\p{ASCII}]", "");
    }

    private static TicketData parseHtml(String html) {
        TicketData d = new TicketData();

        d.titulo    = extract(html, "<div\\s+class=\"ticket-title\"[^>]*>(.*?)</div>");
        d.nome      = extract(html, "Nome:\\s*(.*?)<br>");
        d.documento = extract(html, "Documento:\\s*(.*?)<br>");
        d.numero    = extract(html, "N[uú]mero:\\s*(.*?)<br>");
        d.data      = extract(html, "Data:\\s*(.*?)<br>");
        d.hora      = extract(html, "Hora:\\s*(.*?)<br>");
        d.loteria   = extract(html, "Loteria:\\s*(.*?)<br>");
        d.validade  = extract(html, "Validade:\\s*(.*?)<br>");
        d.valor     = extract(html, "<h1>\\s*Valor:\\s*(.*?)\\s*</h1>");

        // pega bilhetes
        d.bilhetes = extractAll(html, "<div\\s+class=\"number-item\"[^>]*>(.*?)</div>");

        // ✅ pega especificamente o IMG do QR (tem "qrcodes" ou "qr_")
        d.qrUrl = extract(html,
                "<img[^>]+src\\s*=\\s*\"([^\"]*(?:qrcodes|qr_)[^\"]*)\""
        );

        return d;
    }

    private static String extract(String text, String regex) {
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(text);
        if (m.find()) return removeAccents(m.group(1).trim());
        return "";
    }

    private static List<String> extractAll(String text, String regex) {
        List<String> lista = new ArrayList<>();
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(text);
        while (m.find()) {
            lista.add(removeAccents(m.group(1).trim()));
        }
        return lista;
    }

    private static void printTicket(BluetoothDevice printer, TicketData d) throws Exception {

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

        for (String bilhete : d.bilhetes) {
            out.write((bilhete + "\n").getBytes());
        }

        out.write("\n".getBytes());

        // ✅ imprimir QR
        if (d.qrUrl != null && !d.qrUrl.trim().isEmpty()) {
            Bitmap qr = downloadImage(d.qrUrl.trim());
            if (qr != null) {
                // ✅ garante tamanho bom pra POS
                Bitmap resized = Bitmap.createScaledBitmap(qr, 240, 240, false);
                byte[] qrBytes = PrinterConverter.bitmapToBytes(resized);
                out.write(qrBytes);
                out.write("\n".getBytes());
            }
        }

        out.write("\nBoa sorte!\n".getBytes());
        out.write("--------------------------------\n".getBytes());
        out.write(EscPosBase.nextLine(4));
        out.flush();

        socket.close();
    }

    // ✅ download mais robusto (timeout + http/https)
    private static Bitmap downloadImage(String urlStr) {
        HttpURLConnection conn = null;
        InputStream in = null;

        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Android");

            int code = conn.getResponseCode();
            if (code != 200) return null;

            in = conn.getInputStream();

            // lê tudo primeiro (evita stream cortado)
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) bos.write(buf, 0, r);

            byte[] bytes = bos.toByteArray();
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        } catch (Exception e) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            if (conn != null) conn.disconnect();
        }
    }

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
