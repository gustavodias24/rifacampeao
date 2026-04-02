package benicio.solucoes.rifacampeo.utils;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import benicio.solucoes.rifacampeo.objects.GanhadorModel;

public class PrinterTicketUtils2 {

    private static final UUID SPP_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final Charset PRINTER_CHARSET = Charset.forName("windows-1252");

    public static void printTicketFromHtml(Activity activity,
                                           BluetoothDevice printer,
                                           String html) {

        if (printer == null) {
            showToast(activity, "Impressora não encontrada.");
            return;
        }

        if (html == null || html.trim().isEmpty()) {
            showToast(activity, "Nada para imprimir.");
            return;
        }

        new Thread(() -> {
            BluetoothSocket socket = null;
            try {
                TicketData data = parseResultadoHtml(html);
                socket = printer.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                socket.connect();

                OutputStream out = socket.getOutputStream();
                printResultado(out, data);
                out.flush();

                showToast(activity, "Impressão enviada.");
            } catch (Exception e) {
                e.printStackTrace();
                showToast(activity, "Erro ao imprimir: " + e.getMessage());
            } finally {
                try {
                    if (socket != null) socket.close();
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    public static void printGanhador(Activity activity,
                                     BluetoothDevice printer,
                                     GanhadorModel item) {

        if (printer == null) {
            showToast(activity, "Impressora não encontrada.");
            return;
        }

        if (item == null) {
            showToast(activity, "Item inválido para imprimir.");
            return;
        }

        new Thread(() -> {
            BluetoothSocket socket = null;
            try {
                TicketData data = fromGanhador(item);
                socket = printer.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                socket.connect();

                OutputStream out = socket.getOutputStream();
                printResultado(out, data);
                out.flush();

                showToast(activity, "Impressão enviada.");
            } catch (Exception e) {
                e.printStackTrace();
                showToast(activity, "Erro ao imprimir: " + e.getMessage());
            } finally {
                try {
                    if (socket != null) socket.close();
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    private static void printResultado(OutputStream out, TicketData d) throws Exception {
        out.write(EscPosBase.init_printer());
        out.write(EscPosBase.alignLeft());

        // título
        out.write(new byte[]{0x1B, 0x45, 0x01}); // bold on
        writeLine(out, safe(d.titulo));
        out.write(new byte[]{0x1B, 0x45, 0x00}); // bold off

        out.write(new byte[]{0x1B, 0x45, 0x01});// bold on
        out.write(new byte[]{0x1D, 0x21, 0x11});
        writeLine(out, "Loteria: " + safe(d.loteria));
        out.write(new byte[]{0x1B, 0x45, 0x00}); // bold off
        out.write(new byte[]{0x1D, 0x21, 0x00});
        writeLine(out, "Data: " + safe(d.data));

        StringBuilder numerosBuild = new StringBuilder();
        for (String linhaNumero : d.numeros) {
            numerosBuild.append(linhaNumero).append("\n");
        }
//        writeLine(out, numerosBuild.toString());
        writeLine(out, "--------------------------------");
        out.write("\n".getBytes(PRINTER_CHARSET));

        out.write(new byte[]{0x1B, 0x45, 0x01}); // bold on
        writeLine(out, "Numeros sorteados");
        out.write(EscPosBase.alignCenter());
        out.write(new byte[]{0x1D, 0x21, 0x11});
        out.write(new byte[]{0x1B, 0x45, 0x01}); // bold on
        for (String number : numerosBuild.toString().replace("º  ", "").split("\n")){
            writeLine(out,number.substring(0, 1) +" - "+ number.substring(1));
        }
        out.write(new byte[]{0x1B, 0x45, 0x00}); // bold off
        out.write("\n".getBytes(PRINTER_CHARSET));





        out.write(EscPosBase.nextLine(4));
    }

    private static TicketData fromGanhador(GanhadorModel item) {
        TicketData d = new TicketData();
        d.titulo = "Resultado do Sorteio";
        d.loteria = safe(item.getLoteria());
        d.data = safe(item.getData_lancada());

        d.numeros.add("1º  " + formatarNumero4(item.getNumero1()));
        d.numeros.add("2º  " + formatarNumero4(item.getNumero2()));
        d.numeros.add("3º  " + formatarNumero4(item.getNumero3()));
        d.numeros.add("4º  " + formatarNumero4(item.getNumero4()));
        d.numeros.add("5º  " + formatarNumero4(item.getNumero5()));
        d.numeros.add("6º  " + formatarNumero4(item.getNumero6()));


        return d;
    }

    private static TicketData parseResultadoHtml(String html) {
        TicketData d = new TicketData();

        d.titulo = extract(html, "<div\\s+class=['\"]titulo['\"][^>]*>(.*?)</div>");
        d.loteria = extract(html, "Loteria:\\s*(.*?)</div>");
        d.data = extract(html, "Data:\\s*(.*?)</div>");

        List<String> linhasNumero = extractAll(html, "<div\\s+class=['\"]numero['\"][^>]*>(.*?)</div>");
        for (String s : linhasNumero) {
            String limpo = cleanHtmlText(s);
            if (!limpo.isEmpty()) {
                d.numeros.add(limpo);
            }
        }

        if (isEmpty(d.titulo)) d.titulo = "Resultado de Sorteio";

        return d;
    }

    private static String extract(String text, String regex) {
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(text);
        if (m.find()) {
            return cleanHtmlText(m.group(1));
        }
        return "";
    }

    private static List<String> extractAll(String text, String regex) {
        List<String> lista = new ArrayList<>();
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(text);
        while (m.find()) {
            lista.add(cleanHtmlText(m.group(1)));
        }
        return lista;
    }

    private static String cleanHtmlText(String s) {
        if (s == null) return "";

        String out = s;
        out = out.replace("&nbsp;", " ");
        out = out.replace("&amp;", "&");
        out = out.replace("&lt;", "<");
        out = out.replace("&gt;", ">");
        out = out.replace("&quot;", "\"");
        out = out.replace("&#39;", "'");
        out = out.replaceAll("<[^>]+>", "");
        out = out.replaceAll("\\s+", " ").trim();

        return out;
    }

    private static void writeLine(OutputStream out, String text) throws Exception {
        out.write((safe(text) + "\n").getBytes(PRINTER_CHARSET));
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String formatarNumero4(Object numero) {
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

    private static String removeAccents(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        return n.replaceAll("[^\\p{ASCII}]", "");
    }

    private static void showToast(Activity activity, String msg) {
        if (activity == null) return;
        activity.runOnUiThread(() ->
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
        );
    }

    private static class TicketData {
        String titulo;
        String loteria;
        String data;
        List<String> numeros = new ArrayList<>();
    }
}