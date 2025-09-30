package benicio.solucoes.rifacampeo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;

import benicio.solucoes.rifacampeo.databinding.ActivityMakeSorteioBinding;
import benicio.solucoes.rifacampeo.databinding.ActivitySelectLoteriaBinding;
import benicio.solucoes.rifacampeo.objects.BilheteModel;
import benicio.solucoes.rifacampeo.objects.DateLimitModel;
import benicio.solucoes.rifacampeo.objects.RetornoModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SelectLoteriaActivity extends AppCompatActivity {

    private ActivitySelectLoteriaBinding selectLoteriaBinding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        selectLoteriaBinding = ActivitySelectLoteriaBinding.inflate(getLayoutInflater());
        setContentView(selectLoteriaBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);


        selectLoteriaBinding.button7.setOnClickListener(v -> {
            got_make( "COR");
        });

        selectLoteriaBinding.button8.setOnClickListener(v -> {
            got_make("FD");
        });
    }

    private void got_make(String loteria){

        BilheteModel b = new BilheteModel(); // uso bilhete model para passar a loteria
        b.setLoteria(loteria);
        RetrofitUtils.getApiService().check_date(b).enqueue(new Callback<RetornoModel>() {
            @Override
            public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
                if ( response.isSuccessful() ){
                    if ( response.body().getSuccess()){
                        Intent i = new Intent(SelectLoteriaActivity.this,MakeSorteioActivity.class);
                        i.putExtra("loteria", loteria);
                        startActivity(i);
                    }
                    Toast.makeText(SelectLoteriaActivity.this, response.body().getMsg(), Toast.LENGTH_SHORT).show();

                }
            }

            @Override
            public void onFailure(Call<RetornoModel> call, Throwable throwable) {

            }
        });


    }
}