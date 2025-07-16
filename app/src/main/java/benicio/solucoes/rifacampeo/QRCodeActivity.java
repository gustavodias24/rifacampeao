package benicio.solucoes.rifacampeo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import benicio.solucoes.rifacampeo.databinding.ActivityMakeSorteioBinding;
import benicio.solucoes.rifacampeo.databinding.ActivityQrcodeBinding;
import benicio.solucoes.rifacampeo.utils.ApiService;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QRCodeActivity extends AppCompatActivity {

    private ActivityQrcodeBinding mainBinding;
    ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityQrcodeBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);


        Bundle b = getIntent().getExtras();
        if (b != null) {
            //String linkQr = b.getString("linkqr", "");
            //generateQRCode(linkQr);

            String numero =  b.getString("numero", "");

            apiService = RetrofitUtils.getApiService();
            apiService.getBilheteHtml(numero).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    String html = response.body();
                    Log.d("mayara", "onResponse: " + html);
                    renderHtmlOnScreen(html);
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

    private void renderHtmlOnScreen(String html) {
        WebView webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true); // Caso precise JS
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
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