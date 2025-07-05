package benicio.solucoes.rifacampeo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import benicio.solucoes.rifacampeo.databinding.ActivityMenuAcvityBinding;
import benicio.solucoes.rifacampeo.objects.ResponseSimple;
import benicio.solucoes.rifacampeo.objects.VendedorModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MenuAcvity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    EditText et1, et2, et3, et4, et5, et6;
    private ActivityMenuAcvityBinding mainBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMenuAcvityBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        sharedPreferences = getSharedPreferences("info", MODE_PRIVATE);
        editor = sharedPreferences.edit();


        et1 = findViewById(R.id.et1);
        et2 = findViewById(R.id.et2);
        et3 = findViewById(R.id.et3);
        et4 = findViewById(R.id.et4);
        et5 = findViewById(R.id.et5);
        et6 = findViewById(R.id.et6);

        setupEditTexts(et1, et2);
        setupEditTexts(et2, et3);
        setupEditTexts(et3, et4);
        setupEditTexts(et4, et5);
        setupEditTexts(et5, et6);

        // No último campo, verificamos a sequência
        et6.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    checkCode();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

    }

    private void setupEditTexts(EditText current, EditText next) {
        current.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    next.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void checkCode() {
        String code =
                et1.getText().toString()
                + et2.getText().toString()
                + et3.getText().toString()
                + et4.getText().toString()
                + et5.getText().toString()
                + et6.getText().toString();

//        } else if (code.equals("426759")) {
        if (code.equals("111111")) {
            startActivity(new Intent(this, AdminMasterActivity.class));
        } else {
            VendedorModel vendedorModel = new VendedorModel();
            vendedorModel.setSenha(code);
            RetrofitUtils.getApiService().login(vendedorModel).enqueue(new Callback<ResponseSimple>() {
                @Override
                public void onResponse(Call<ResponseSimple> call, Response<ResponseSimple> response) {
                    if (response.isSuccessful()) {
                        if (response.body().isSuccess()) {

                            editor.putString("id_vendedor", response.body().getMsg()).apply();

                            startActivity(new Intent(MenuAcvity.this, PremioActivity.class));
                        } else {
                            Toast.makeText(MenuAcvity.this, "Senha Incorreta ou Usuário Bloqueado", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MenuAcvity.this, "Problema de Conexão!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseSimple> call, Throwable throwable) {

                }
            });
        }

        et1.setText("");
        et2.setText("");
        et3.setText("");
        et4.setText("");
        et5.setText("");
        et6.setText("");;
    }
}