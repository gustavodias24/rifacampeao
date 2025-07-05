package benicio.solucoes.rifacampeo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import benicio.solucoes.rifacampeo.databinding.ActivityAdminMasterBinding;
import benicio.solucoes.rifacampeo.databinding.ActivityLancarDataLimiteBinding;
import benicio.solucoes.rifacampeo.objects.DateLimitModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LancarDataLimiteActivity extends AppCompatActivity {

    private ActivityLancarDataLimiteBinding mainBinding;

    private DatePicker datePicker;
    private TimePicker timePicker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityLancarDataLimiteBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);


        RetrofitUtils.getApiService().returnDataLimite().enqueue(new Callback<DateLimitModel>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(Call<DateLimitModel> call, Response<DateLimitModel> response) {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    mainBinding.datahoraatual.setText("Data e Hora atual:  " + response.body().getDatalimite());
                } else {
                    Toast.makeText(LancarDataLimiteActivity.this, "Problema de Conexão", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DateLimitModel> call, Throwable throwable) {
                Log.d("mayara", "onFailure: " + throwable.getMessage());
            }
        });

        datePicker = findViewById(R.id.datePicker);
        timePicker = findViewById(R.id.timePicker);

        timePicker.setIs24HourView(false); // Usa formato AM/PM

        mainBinding.novadatahora.setOnClickListener(v -> {
            int day = datePicker.getDayOfMonth();
            int month = datePicker.getMonth(); // começa em 0!
            int year = datePicker.getYear();

            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();

            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day, hour, minute);

            // Formatando: dd-MM-yyyy hh:mm
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm", Locale.getDefault());
            String dataHoraFormatada = sdf.format(calendar.getTime());

            RetrofitUtils.getApiService().setarDataLimite(new DateLimitModel(dataHoraFormatada)).enqueue(new Callback<ResponseBody>() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        mainBinding.datahoraatual.setText("Data e Hora atual:  " + dataHoraFormatada);
                        Toast.makeText(LancarDataLimiteActivity.this, "Data e Hora Salvos", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LancarDataLimiteActivity.this, "Problema de Conexão", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable throwable) {

                }
            });
        });
    }
}