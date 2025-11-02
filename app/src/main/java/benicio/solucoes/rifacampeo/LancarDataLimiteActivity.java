package benicio.solucoes.rifacampeo;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ScrollView;
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
    private ScrollView scrollView;
    private ActivityLancarDataLimiteBinding mainBinding;
    private EditText editTextDateFD, editTextTimeFD, editTextDateCOR, editTextTimeCOR, editValor;
    Calendar calendar;


    private EditText premio1, premio2, premio3, premio4, premio5, premio6;
    private static final String PREF_NAME = "rifa_premios_prefs";
    private static final String KEY_PREMIO_1 = "premio_1";
    private static final String KEY_PREMIO_2 = "premio_2";
    private static final String KEY_PREMIO_3 = "premio_3";
    private static final String KEY_PREMIO_4 = "premio_4";
    private static final String KEY_PREMIO_5 = "premio_5";
    private static final String KEY_PREMIO_6 = "premio_6";

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
        editValor = findViewById(R.id.valorRifa);

        calendar = Calendar.getInstance();

        // Clique no campo de Data FD
        editTextDateFD.setOnClickListener(v -> showDatePicker(editTextDateFD));
        // Clique no campo de Hora FD
        editTextTimeFD.setOnClickListener(v -> showTimePicker(editTextTimeFD));

        // Clique no campo de Data COR
        editTextDateCOR.setOnClickListener(v -> showDatePicker(editTextDateCOR));
        // Clique no campo de Hora COR
        editTextTimeCOR.setOnClickListener(v -> showTimePicker(editTextTimeCOR));

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

                    editValor.setText(response.body().getValorRifa());
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

            salvarPremiosNosPrefs();

            DateLimitModel dateLimitModel = new DateLimitModel();
            dateLimitModel.setDataHoraCOR(editTextDateCOR.getText().toString() + " " + editTextTimeCOR.getText().toString());
            dateLimitModel.setDataHoraFD(editTextDateFD.getText().toString() + " " + editTextTimeFD.getText().toString());
            dateLimitModel.setValorRifa(editValor.getText().toString());


            if (    editTextDateFD.getText().toString().isEmpty() ||
                    editTextTimeFD.getText().toString().isEmpty() ||
                    editTextDateCOR.getText().toString().isEmpty() ||
                    editTextTimeCOR.getText().toString().isEmpty() ||
                    editValor.getText().toString().isEmpty()

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
        scrollView = findViewById(R.id.scrollViewRoot); // vamos dar um id no ScrollView já já
        // lista de EditTexts que precisam scroll automático
        // pega os campos
        premio1 = findViewById(R.id.premio1);
        premio2 = findViewById(R.id.premio2);
        premio3 = findViewById(R.id.premio3);
        premio4 = findViewById(R.id.premio4);
        premio5 = findViewById(R.id.premio5);
        premio6 = findViewById(R.id.premio6);
        carregarPremiosDosPrefs();
        View[] campos = {premio1, premio2, premio3, premio4, premio5, premio6};

        for (View campo : campos) {
            campo.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    // espera o teclado abrir e depois dá scroll
                    v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            scrollView.post(() -> scrollView.smoothScrollTo(0, v.getBottom()));
                            v.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    });
                }
            });
    }
    }

    private void carregarPremiosDosPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        String p1 = prefs.getString(KEY_PREMIO_1, "");
        String p2 = prefs.getString(KEY_PREMIO_2, "");
        String p3 = prefs.getString(KEY_PREMIO_3, "");
        String p4 = prefs.getString(KEY_PREMIO_4, "");
        String p5 = prefs.getString(KEY_PREMIO_5, "");
        String p6 = prefs.getString(KEY_PREMIO_6, "");

        premio1.setText(p1);
        premio2.setText(p2);
        premio3.setText(p3);
        premio4.setText(p4);
        premio5.setText(p5);
        premio6.setText(p6);
    }

    private void salvarPremiosNosPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(KEY_PREMIO_1, premio1.getText().toString().trim());
        editor.putString(KEY_PREMIO_2, premio2.getText().toString().trim());
        editor.putString(KEY_PREMIO_3, premio3.getText().toString().trim());
        editor.putString(KEY_PREMIO_4, premio4.getText().toString().trim());
        editor.putString(KEY_PREMIO_5, premio5.getText().toString().trim());
        editor.putString(KEY_PREMIO_6, premio6.getText().toString().trim());

        editor.apply(); // salva async

        Toast.makeText(this, "Prêmios salvos", Toast.LENGTH_SHORT).show();
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

    private void showDatePicker(EditText target) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year1, month1, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year1, month1, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    target.setText(sdf.format(selected.getTime()));
                }, year, month, day);
        datePicker.show();
    }

    private void showTimePicker(EditText target) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePicker = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selected.set(Calendar.MINUTE, minute1);
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    target.setText(sdf.format(selected.getTime()));
                }, hour, minute, true); // true = 24h
        timePicker.show();
    }
}