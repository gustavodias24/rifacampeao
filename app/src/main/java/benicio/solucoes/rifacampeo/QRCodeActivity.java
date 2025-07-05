package benicio.solucoes.rifacampeo;

import android.graphics.Bitmap;
import android.os.Bundle;

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

public class QRCodeActivity extends AppCompatActivity {

    private ActivityQrcodeBinding mainBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityQrcodeBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            String linkQr = b.getString("linkqr", "");
            generateQRCode(linkQr);
        }

        mainBinding.button.setOnClickListener(v -> finish());

    }

    private void generateQRCode(String text) {
        BarcodeEncoder barcodeEncoder
                = new BarcodeEncoder();
        try {

            // This method returns a Bitmap image of the
            // encoded text with a height and width of 400
            // pixels.
            Bitmap bitmap = barcodeEncoder.encodeBitmap(text, BarcodeFormat.QR_CODE, 400, 400);

            // Sets the Bitmap to ImageView
            mainBinding.imageView4.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
}