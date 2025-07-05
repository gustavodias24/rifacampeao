package benicio.solucoes.rifacampeo;

import static android.content.pm.PackageManager.*;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import benicio.solucoes.rifacampeo.adapters.AdapterBanners;
import benicio.solucoes.rifacampeo.databinding.ActivityAdminMasterBinding;
import benicio.solucoes.rifacampeo.databinding.ActivityBannersBinding;
import benicio.solucoes.rifacampeo.utils.ApiService;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BannersActivity extends AppCompatActivity {

    private static final int PERMISSAO_GALERIA_REQUEST_CODE = 101;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imagemSelecionadaUri;
    public static ApiService apiService;
    private ActivityBannersBinding mainBinding;

    public static AdapterBanners adapterBanners;
    public static List<String> banners = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityBannersBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        verificarPermissaoGaleria();

        apiService = RetrofitUtils.getApiService();
        configurarRecycler();
        listarImagens();

        mainBinding.btenviarbanner.setOnClickListener(v -> abrirGaleria());
    }


    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void configurarRecycler() {
        mainBinding.rvbanners.setLayoutManager(new LinearLayoutManager(this));
        mainBinding.rvbanners.setHasFixedSize(true);
        mainBinding.rvbanners.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapterBanners = new AdapterBanners(banners, this);
        mainBinding.rvbanners.setAdapter(adapterBanners);
    }

    private void listarImagens() {
        banners.clear();
        apiService.listarImagens().enqueue(new Callback<List<String>>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    banners.addAll(response.body());
                    adapterBanners.notifyDataSetChanged();
                } else {
                    Toast.makeText(BannersActivity.this, "Erro de Conexão!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable throwable) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imagemSelecionadaUri = data.getData();

            String pathImagem = getCaminhoRealDaImagem(imagemSelecionadaUri);
            if (pathImagem != null) {
                enviarImagemParaApi(pathImagem);
            }
        }
    }

    private String getCaminhoRealDaImagem(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor == null) return null;

        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(column_index);
        cursor.close();
        return path;
    }

    private void enviarImagemParaApi(String caminho) {
        File file = new File(caminho);
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("imagem", file.getName(), requestFile);

        ApiService api = RetrofitUtils.getApiService();
        Call<ResponseBody> call = api.uploadImagem(body);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(BannersActivity.this, "Imagem enviada com sucesso", Toast.LENGTH_SHORT).show();
                    listarImagens();
                } else {
                    Toast.makeText(BannersActivity.this, "Problema de Conexão!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(BannersActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verificarPermissaoGaleria() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSAO_GALERIA_REQUEST_CODE);
        }
    }

    // Callback da solicitação de permissão
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSAO_GALERIA_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            }
        }
    }
}