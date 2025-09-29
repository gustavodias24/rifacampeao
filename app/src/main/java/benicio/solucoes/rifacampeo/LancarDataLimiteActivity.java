package benicio.solucoes.rifacampeo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import benicio.solucoes.rifacampeo.databinding.ActivityLancarDataLimiteBinding;
import benicio.solucoes.rifacampeo.objects.DateLimitModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LancarDataLimiteActivity extends AppCompatActivity {

    private ActivityLancarDataLimiteBinding mainBinding;
    private EditText editTextDateFD, editTextTimeFD, editTextDateCOR, editTextTimeCOR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityLancarDataLimiteBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        editTextDateFD = findViewById(R.id.editTextDateFD);
        editTextTimeFD = findViewById(R.id.editTextTimeFD);
        editTextDateCOR = findViewById(R.id.editTextDateCOR);
        editTextTimeCOR = findViewById(R.id.editTextTimeCOR);

        // Máscaras
        applyMask(editTextDateFD, "##/##/####");
        applyMask(editTextDateCOR, "##/##/####");
        applyMask(editTextTimeFD, "##:##");
        applyMask(editTextTimeCOR, "##:##");


        RetrofitUtils.getApiService().returnDataLimite().enqueue(new Callback<DateLimitModel>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(Call<DateLimitModel> call, Response<DateLimitModel> response) {
                if (response.isSuccessful()) {


                    editTextDateFD.setText(response.body().getDataHoraFD().split(" ")[0]);
                    editTextDateCOR.setText(response.body().getDataHoraCOR().split(" ")[0]);

                    editTextTimeFD.setText(response.body().getDataHoraFD().split(" ")[1]);
                    editTextTimeCOR.setText(response.body().getDataHoraCOR().split(" ")[1]);
                } else {
                    Toast.makeText(LancarDataLimiteActivity.this, "Problema de Conexão", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DateLimitModel> call, Throwable throwable) {
                Log.d("mayara", "onFailure: " + throwable.getMessage());
            }
        });



        mainBinding.button6.setOnClickListener(v -> {

            DateLimitModel dateLimitModel = new DateLimitModel();
            dateLimitModel.setDataHoraCOR(editTextDateCOR.getText().toString() + " " + editTextTimeCOR.getText().toString());
            dateLimitModel.setDataHoraFD(editTextDateFD.getText().toString() + " " + editTextTimeFD.getText().toString());


            if (    editTextDateFD.getText().toString().isEmpty() ||
                    editTextTimeFD.getText().toString().isEmpty() ||
                    editTextDateCOR.getText().toString().isEmpty() ||
                    editTextTimeCOR.getText().toString().isEmpty()

            ){
                Toast.makeText(this, "Preencha TODAS as informações", Toast.LENGTH_SHORT).show();
                return;
            }

            RetrofitUtils.getApiService().setarDataLimite(dateLimitModel).enqueue(new Callback<>() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
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

    private void applyMask(EditText editText, final String mask) {
        editText.addTextChangedListener(new TextWatcher() {
            boolean isUpdating;
            String old = "";

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String digits = s.toString().replaceAll("[^\\d]", "");
                StringBuilder out = new StringBuilder();

                if (isUpdating) {
                    old = digits;
                    isUpdating = false;
                    return;
                }

                int i = 0;
                for (char m : mask.toCharArray()) {
                    if (m != '#') {
                        out.append(m);
                        continue;
                    }
                    if (i < digits.length()) {
                        out.append(digits.charAt(i));
                        i++;
                    } else {
                        break;
                    }
                }

                isUpdating = true;
                editText.setText(out.toString());
                editText.setSelection(out.length());
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }
}