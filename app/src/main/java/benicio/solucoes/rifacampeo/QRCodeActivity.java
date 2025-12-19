package benicio.solucoes.rifacampeo;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import benicio.solucoes.rifacampeo.databinding.ActivityQrcodeBinding;
import benicio.solucoes.rifacampeo.utils.ApiService;
import benicio.solucoes.rifacampeo.utils.PrinterTicketUtils;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QRCodeActivity extends AppCompatActivity {

    private static final int REQ_BLUETOOTH_PERMS = 1001;
    private static final int REQUEST_ENABLE_BT = 100;
    private ActivityQrcodeBinding mainBinding;
    ApiService apiService;

    private BluetoothDevice printerBluetooth;
    String html = "";

    @Override
    @SuppressLint({"MissingPermission", "SimpleDateFormat"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityQrcodeBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        mainBinding.compartilharZap.setEnabled(false);


        mainBinding.webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                mainBinding.compartilharZap.setEnabled(true);
            }
        });


        mainBinding.print.setOnClickListener(v -> {
            Log.d("amopussy", html);

            if (printerBluetooth == null) acharPrinterBluetooth();
            if (printerBluetooth == null) return;

            if (html == null || html.trim().isEmpty()) {
                Toast.makeText(this, "Carregando... Tente novamente!", Toast.LENGTH_LONG).show();
                return;
            }

            PrinterTicketUtils.printTicketFromHtml(this, printerBluetooth, html);
        });

        Bundle b = getIntent().getExtras();
        if (b != null) {
            //String linkQr = b.getString("linkqr", "");
            //generateQRCode(linkQr);

            String numero =  b.getString("numero", "");

            apiService = RetrofitUtils.getApiService();
            apiService.getBilheteHtml(numero).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    html = response.body();
                    renderHtmlOnScreen(html);

                    mainBinding.compartilharZap.setOnClickListener(v -> {
                        captureAndSharePng();
                    });
                }

                @Override
                public void onFailure(Call<String> call, Throwable throwable) {
                    Log.d("mayara", "onFailure: "+ call.toString());
                    Log.d("mayara", "onFailure: "+ throwable.getMessage());
                }
            });
        }

        mainBinding.button.setOnClickListener(v -> finish());

    }

    @SuppressLint("MissingPermission")
    private void acharPrinterBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth não foi encontrado ou não disponível neste equipamento.", Toast.LENGTH_SHORT).show();
            // Device doesn't support Bluetooth
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice bondedDevice : bondedDevices) {
            if (bondedDevice.getName().toLowerCase().contains("print")) {
                printerBluetooth = bondedDevice;
                break;
            }
        }
    }

    private void renderHtmlOnScreen(String html) {
        WebView webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true); // Caso precise JS
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }


    /** Captura o WebView em PNG e abre Intent de compartilhamento **/
    private void captureAndSharePng() {
        // Aguarda layout/scroll final para evitar cortes
        mainBinding.webView.post(() -> {
            // Altura total do conteúdo em pixels (contentHeight é em CSS px, multiplique por a escala)
            int contentHeightPx = (int) (mainBinding.webView.getContentHeight() * mainBinding.webView.getScale());
            int width = mainBinding.webView.getWidth();
            int height = contentHeightPx;

            // Evita altura zero
            if (width <= 0 || height <= 0) {
                // como fallback, force um layout observando o próximo pass
                mainBinding.webView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        mainBinding.webView.getViewTreeObserver().removeOnPreDrawListener(this);
                        captureAndSharePng(); // tenta de novo
                        return true;
                    }
                });
                return;
            }

            // CUIDADO: bitmaps gigantes podem gerar OOM.
            // Se o conteúdo for muito alto, você pode limitar ou fazer captura por partes.
            try {
                Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bmp);
                mainBinding.webView.draw(canvas);

                // Salva no cache interno (compatível com FileProvider)
                File imagesFolder = new File(getCacheDir(), "images");
                if (!imagesFolder.exists()) imagesFolder.mkdirs();
                File outFile = new File(imagesFolder, "bilhete.png");

                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                }

                // Compartilha usando FileProvider
                Uri contentUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        outFile
                );

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, "Compartilhar bilhete"));

            } catch (OutOfMemoryError oom) {
                Log.e("share", "Bitmap muito grande. Considere reduzir largura/altura.", oom);
                // Aqui você pode mostrar um Toast e/ou gerar captura em blocos
            } catch (IOException e) {
                Log.e("share", "Erro salvando/compartilhando PNG", e);
            }
        });
    }


    private void checarPermissoesBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] perms = new String[]{
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT
            };

            List<String> faltando = new ArrayList<>();
            for (String p : perms) {
                if (ActivityCompat.checkSelfPermission(this, p)
                        != PackageManager.PERMISSION_GRANTED) {
                    faltando.add(p);
                }
            }

            if (!faltando.isEmpty()) {
                ActivityCompat.requestPermissions(
                        this,
                        faltando.toArray(new String[0]),
                        REQ_BLUETOOTH_PERMS
                );
            } else {
                // Já tem permissão, pode chamar acharPrinterBluetooth()
                acharPrinterBluetooth();
            }
        } else {
            // Android 11 ou menor: só chamar direto (já que as permissões antigas
            // são tratadas na instalação)
            acharPrinterBluetooth();
        }
    }

//    private void generateQRCode(String text) {
//        BarcodeEncoder barcodeEncoder
//                = new BarcodeEncoder();
//        try {
//
//            // This method returns a Bitmap image of the
//            // encoded text with a height and width of 400
//            // pixels.
//            Bitmap bitmap = barcodeEncoder.encodeBitmap(text, BarcodeFormat.QR_CODE, 400, 400);
//
//            // Sets the Bitmap to ImageView
//            mainBinding.imageView4.setImageBitmap(bitmap);
//        } catch (WriterException e) {
//            e.printStackTrace();
//        }
//    }
}