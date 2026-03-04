package benicio.solucoes.rifacampeo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

    private SharedPreferences prefs;
    private SharedPreferences.Editor edt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMenuAcvityBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        prefs = getSharedPreferences("rprefs", MODE_PRIVATE);
        edt = prefs.edit();

        sharedPreferences = getSharedPreferences("info", MODE_PRIVATE);
        editor = sharedPreferences.edit();


        mainBinding.button11.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminMasterActivity.class));
        });


        mainBinding.button10.setOnClickListener(v -> {
            Intent i = new Intent(this, RecolhimentoActivity.class);
            i.putExtra("recolhedor", true);
            startActivity(i);
        });


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

    public String getDeviceUniqueId() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
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
        String code = et1.getText().toString() + et2.getText().toString() + et3.getText().toString() + et4.getText().toString() + et5.getText().toString() + et6.getText().toString();

//        } else if (code.equals("426759")) {
        if (code.equals("426759")) {
            pedirSegundaSenha("@4267#");
        } else if (code.equals("565656")) {
            pedirSegundaSenha("@5656#");
        } else {
            VendedorModel vendedorModel = new VendedorModel();
            vendedorModel.setSenha(code);
            vendedorModel.setSerial(getDeviceUniqueId());

            RetrofitUtils.getApiService().login(vendedorModel).enqueue(new Callback<ResponseSimple>() {
                @Override
                public void onResponse(Call<ResponseSimple> call, Response<ResponseSimple> response) {
                    if (response.isSuccessful()) {
                        if (response.body().isSuccess()) {
                            ResponseSimple body = response.body();

                            VendedorModel v = body.getVendedor();

                            editor.putString("id_vendedor", (v.get_id()));
                            editor.putString("nome_vendedor", (v.getNome()));
                            editor.putString("documento_vendedor", (v.getDocumento()));

                            // ===== resposta do login =====
                            editor.putBoolean("success", body.isSuccess());
                            editor.putString("msg", (body.getMsg()));

                            // ===== credenciais / código =====
                            editor.putString("senha", (code));     // se você REALMENTE quer salvar isso
                            editor.putString("codigo", (code));    // substitui aquele "vendedor" errado

                            // ===== totais do ResponseSimple =====
                            editor.putFloat("valorTotalGeradoCOR", body.getValorTotalGeradoCOR());
                            editor.putFloat("valorTotalGeradoDF", body.getValorTotalGeradoDF());
                            editor.putFloat("valorTotalGeradoDFLoteriaAtual", body.getValorTotalGeradoDFLoteriaAtual());
                            editor.putFloat("valorTotalGeradoCORLoteriaAtual", body.getValorTotalGeradoCORLoteriaAtual());

                            editor.apply();

                            Intent i = new Intent(MenuAcvity.this, PremioActivity.class);

                            i.putExtra("valorTotalGeradoCOR", response.body().getValorTotalGeradoCOR());
                            i.putExtra("valorTotalGeradoDF", response.body().getValorTotalGeradoDF());
                            i.putExtra("limiteAposta", response.body().getVendedor().getLimiteAposta());
                            i.putExtra("code", code);

                            startActivity(i);
                        } else {
                            //Toast.makeText(MenuAcvity.this, response.body().getMsg(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        //Toast.makeText(MenuAcvity.this, "Credenciais Inválidas", Toast.LENGTH_SHORT).show();
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
        et6.setText("");
    }

    private void pedirSegundaSenha(String senhaBater) {
        final EditText input = new EditText(this);
        input.setHint("Digite a segunda senha");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setSingleLine(true);

        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Confirmação de Admin").setMessage("Informe a segunda senha para continuar").setView(input).setCancelable(false).setPositiveButton("Entrar", null) // vou sobrescrever depois pra não fechar sozinho
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss()).create();

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Confirmação de Admin").setMessage("Informe a segunda senha para continuar").setView(input).setCancelable(false).setPositiveButton("Entrar", null).setNegativeButton("Cancelar", (d, w) -> d.dismiss()).create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String senha = input.getText().toString().trim();

                dialog.dismiss();

                if (senhaBater.equals("@4267#")) {
                    startActivity(new Intent(this, AdminMasterActivity.class));
                } else {
                    VendedorModel vendedorModel = new VendedorModel();
                    vendedorModel.setSenha(senha);
//                        vendedorModel.setSerial(getDeviceUniqueId());

                    RetrofitUtils.getApiService().login(vendedorModel).enqueue(new Callback<ResponseSimple>() {
                        @Override
                        public void onResponse(Call<ResponseSimple> call, Response<ResponseSimple> response) {
                            if (response.isSuccessful()) {
                                if (response.body().isSuccess()) {
                                    Intent i = new Intent(MenuAcvity.this, RecolhimentoActivity.class);
                                    edt.putString("recolhedor", response.body().getVendedor().getNome()).apply();
                                    i.putExtra("recolhedor", true);
                                    startActivity(i);
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseSimple> call, Throwable throwable) {

                        }
                    });
                }
            });
        });

        dialog.show();
    }


}