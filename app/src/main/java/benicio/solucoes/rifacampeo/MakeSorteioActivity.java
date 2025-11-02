package benicio.solucoes.rifacampeo;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import benicio.solucoes.rifacampeo.adapters.AdapterNumero;
import benicio.solucoes.rifacampeo.databinding.ActivityMakeSorteioBinding;
import benicio.solucoes.rifacampeo.objects.BilheteModel;
import benicio.solucoes.rifacampeo.objects.DateLimitModel;
import benicio.solucoes.rifacampeo.objects.SaveBilheteResponse;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MakeSorteioActivity extends AppCompatActivity {

    public static int valorDoBilhete = 0 ;
    private boolean inFlight = false;
    public static int valorTotalBilhete = 0;
    private SharedPreferences sharedPreferences;
    EditText et1, et2, et3, et4;//, et5;
    public static ActivityMakeSorteioBinding makeSorteioBinding;
    private AdapterNumero adapterNumero;
    private List<String> numeros = new ArrayList<>();

    private Dialog dialogCarregando;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        makeSorteioBinding = ActivityMakeSorteioBinding.inflate(getLayoutInflater());
        setContentView(makeSorteioBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        AlertDialog.Builder bDialog = new AlertDialog.Builder(this);
        bDialog.setTitle("Carregando...");
        bDialog.setMessage("Espere!");
        bDialog.setCancelable(false);
        dialogCarregando = bDialog.create();
        dialogCarregando.show();

        RetrofitUtils.getApiService().returnDataLimite().enqueue(new Callback<DateLimitModel>() {
            @Override
            public void onResponse(Call<DateLimitModel> call, Response<DateLimitModel> response) {
                valorDoBilhete = Integer.parseInt(response.body().getValorRifa());
                dialogCarregando.dismiss();
            }

            @Override
            public void onFailure(Call<DateLimitModel> call, Throwable throwable) {

            }
        });

        sharedPreferences = getSharedPreferences("info", MODE_PRIVATE);

        makeSorteioBinding.finalizarBilhete.setOnClickListener(v -> {

            Dialog loading_d;
            AlertDialog.Builder loading_b = new AlertDialog.Builder(this);
            loading_b.setTitle("Aguarde!");
            loading_b.setMessage("Carregando...");
            loading_b.setCancelable(false);
            loading_d = loading_b.create();
            loading_d.show();

            // Data atual
            LocalDate dataAtual = LocalDate.now();
            String dataFormatada = dataAtual.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            // Hora atual
            LocalTime horaAtual = LocalTime.now();
            String horaFormatada = horaAtual.format(DateTimeFormatter.ofPattern("HH:mm"));

            if (numeros.isEmpty()) {
                Toast.makeText(this, "Você não escolheu nenhum número!", Toast.LENGTH_SHORT).show();
                loading_d.dismiss();
            } else {
                BilheteModel novoBilhete = new BilheteModel();
                novoBilhete.setData(dataFormatada);

                novoBilhete.setDocumento_vendedor(sharedPreferences.getString("documento", ""));
                novoBilhete.setNome_vendedor(sharedPreferences.getString("nome", ""));

                novoBilhete.setHora(horaFormatada);
                novoBilhete.setId_usuario(sharedPreferences.getString("id_vendedor", ""));
                for (String numeroString : numeros) {
                    novoBilhete.getNumeros().add(Integer.parseInt(numeroString));
                }

                novoBilhete.setLoteria(getIntent().getExtras().getString("loteria", "FD"));

                RetrofitUtils.getApiService().saveBilhete(novoBilhete).enqueue(new Callback<SaveBilheteResponse>() {
                    @Override
                    public void onResponse(Call<SaveBilheteResponse> call, Response<SaveBilheteResponse> response) {
                        if (response.isSuccessful()) {
                            if (response.body().isSuccess()) {
                                Toast.makeText(MakeSorteioActivity.this, "Bilhete Salvo", Toast.LENGTH_SHORT).show();
                                Intent i = new Intent(MakeSorteioActivity.this, QRCodeActivity.class);
                                i.putExtra("linkqr", "http://147.79.83.218:5002/download-bilhete/" + response.body().getMsg());
                                i.putExtra("numero", response.body().getMsg());
                                startActivity(i);
                                loading_d.dismiss();
                                finish();
                            } else {
                                loading_d.dismiss();
                                Toast.makeText(MakeSorteioActivity.this, response.body().getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            loading_d.dismiss();
                            Toast.makeText(MakeSorteioActivity.this, "Problema de conexão!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<SaveBilheteResponse> call, Throwable throwable) {
                        loading_d.dismiss();
                    }
                });
            }

            valorTotalBilhete = 0;
        });

        et1 = findViewById(R.id.et11);
        et2 = findViewById(R.id.et22);
        et3 = findViewById(R.id.et33);
        et4 = findViewById(R.id.et44);
//        et5 = findViewById(R.id.et55);

        setupEditTexts(et1, et2);
        setupEditTexts(et2, et3);
        setupEditTexts(et3, et4);
        //setupEditTexts(et4, et5);

        // No último campo, verificamos a sequência
        et4.addTextChangedListener(new TextWatcher() {
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

        makeSorteioBinding.imageView7.setOnClickListener(v -> {
            et1.setText("");
            et2.setText("");
            et3.setText("");
            et4.setText("");
            //et5.setText("");
        });


        makeSorteioBinding.imageView8.setOnClickListener(v -> {
            int n1 = new Random().nextInt(10) + 1;
            int n2 = new Random().nextInt(10);
            int n3 = new Random().nextInt(10);
            int n4 = new Random().nextInt(10);
            int n5 = new Random().nextInt(10);

            et1.setText(n1 + "");
            et2.setText(n2 + "");
            et3.setText(n3 + "");
            et4.setText(n4 + "");
            addNumero();
            //et5.setText(n5 + "");
        });

        RecyclerView recyclerView = makeSorteioBinding.rvNumeros;

        // Cria o GridLayoutManager com 5 colunas
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);

        // Defina seu adapter aqui
        adapterNumero = new AdapterNumero(numeros, this);
        recyclerView.setAdapter(adapterNumero);
        recyclerView.setHasFixedSize(true);


        makeSorteioBinding.et44.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (
                        !makeSorteioBinding.et11.getText().toString().isEmpty() &&
                                !makeSorteioBinding.et22.getText().toString().isEmpty() &&
                                !makeSorteioBinding.et33.getText().toString().isEmpty() &&
                                !makeSorteioBinding.et44.getText().toString().isEmpty()
                ) {
                    addNumero();
                    makeSorteioBinding.et11.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    public void addNumero() {

        if (inFlight) return;

        inFlight = true;
        String code = et1.getText().toString()
                + et2.getText().toString()
                + et3.getText().toString()
                + et4.getText().toString();
        //+ et5.getText().toString();

        if (code.length() == 4) {
            if (numeros.size() < 6) {

                Dialog loading_d;
                AlertDialog.Builder loading_b = new AlertDialog.Builder(this);
                loading_b.setTitle("Aguarde!");
                loading_b.setMessage("Carregando...");
                loading_b.setCancelable(false);
                loading_d = loading_b.create();
                loading_d.show();

                RetrofitUtils.getApiService().checkNumber(new BilheteModel(code, getIntent().getExtras().getString("loteria", "FD"))).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<SaveBilheteResponse> call, Response<SaveBilheteResponse> response) {

                        et1.setText("");
                        et2.setText("");
                        et3.setText("");
                        et4.setText("");
                        //et5.setText("");

                        if (response.isSuccessful()) {
                            if (!response.body().isSuccess()) {
                                Toast.makeText(MakeSorteioActivity.this, response.body().getMsg(), Toast.LENGTH_SHORT).show();
                            } else {
                                if ( !numeros.contains(code)){
                                    numeros.add(code);
                                    adapterNumero.notifyDataSetChanged();
                                    atualizarPrecoBilhete(1);
                                }else{
                                    Toast.makeText(MakeSorteioActivity.this, "Você já inseriu esse número!", Toast.LENGTH_SHORT).show();
                                }
                                Toast.makeText(MakeSorteioActivity.this, "Número adicionado!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        loading_d.dismiss();
                        inFlight = false;
                    }

                    @Override
                    public void onFailure(Call<SaveBilheteResponse> call, Throwable throwable) {
                        Toast.makeText(MakeSorteioActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        loading_d.dismiss();
                        inFlight = false;
                    }
                });

            } else {
                Toast.makeText(this, "No máximo 6 números ", Toast.LENGTH_SHORT).show();
                inFlight = false;
            }
        } else {
            inFlight = false;
        }

    }

    public static void atualizarPrecoBilhete(int addOrRemove) {
        if (addOrRemove == 1) {
            valorTotalBilhete += valorDoBilhete;
        } else {
            valorTotalBilhete -= valorDoBilhete;
        }

        makeSorteioBinding.valorTotal.setText("R$ " + valorTotalBilhete + ",00");
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
        String code = et1.getText().toString()
                + et2.getText().toString()
                + et3.getText().toString()
                + et4.getText().toString();
        //+ et5.getText().toString();

    }
}