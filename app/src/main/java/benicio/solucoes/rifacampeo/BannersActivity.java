package benicio.solucoes.rifacampeo;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import benicio.solucoes.rifacampeo.adapters.AdapterBanners;
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

    private static final int RC_PERMISSAO_GALERIA = 101;
    private static final int RC_PICK_IMAGE = 102;

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

        apiService = RetrofitUtils.getApiService();
        configurarRecycler();
        listarImagens();

        mainBinding.btenviarbanner.setOnClickListener(v -> escolherImagemComPermissao());
    }

    /** Passo 1: checa/solicita a permissão correta por versão **/
    private void escolherImagemComPermissao() {
        String permissao = getPermissaoLeituraImagens();
        if (permissao == null) {
            // Androids antigos que não exigem essa permissão (raro), já abre
            abrirSeletorDeImagem();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, permissao) == PERMISSION_GRANTED) {
            abrirSeletorDeImagem();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permissao}, RC_PERMISSAO_GALERIA);
        }
    }

    /** Para Android 13+ usa READ_MEDIA_IMAGES; <=12 usa READ_EXTERNAL_STORAGE */
    @Nullable
    private String getPermissaoLeituraImagens() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
    }

    /** Passo 2: abre seletor via SAF (não usa caminho absoluto) */
    private void abrirSeletorDeImagem() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Selecione uma imagem"), RC_PICK_IMAGE);
    }

    /** Recycler e lista **/
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
                if (response.isSuccessful() && response.body() != null) {
                    banners.addAll(response.body());
                    adapterBanners.notifyDataSetChanged();
                } else {
                    Toast.makeText(BannersActivity.this, "Erro de Conexão!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable throwable) {
                Toast.makeText(BannersActivity.this, "Falha ao listar: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Passo 3: trata o resultado com URI (sem tentar pegar caminho físico) */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                enviarImagemParaApi(uri);
            }
        }
    }

    /** Constrói o multipart copiando o conteúdo do Uri para um arquivo temporário */
    private void enviarImagemParaApi(Uri uri) {
        try {
            // Descobre nome e mime
            ContentResolver cr = getContentResolver();
            String mime = cr.getType(uri);
            if (mime == null) mime = "image/*";

            String nomeArquivo = getDisplayName(uri);
            if (nomeArquivo == null) nomeArquivo = "imagem.jpg";

            // copia para /cache para conseguir criar RequestBody de File
            File temp = copiarParaCache(uri, nomeArquivo);

            RequestBody requestFile = RequestBody.create(MediaType.parse(mime), temp);
            MultipartBody.Part body = MultipartBody.Part.createFormData("imagem", temp.getName(), requestFile);

            ApiService api = RetrofitUtils.getApiService();
            api.uploadImagem(body).enqueue(new Callback<ResponseBody>() {
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
        } catch (Exception e) {
            Toast.makeText(this, "Falha ao preparar upload: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Util: pega DISPLAY_NAME do Uri (nome do arquivo) */
    @Nullable
    private String getDisplayName(Uri uri) {
        String result = null;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIdx >= 0 && cursor.moveToFirst()) {
                result = cursor.getString(nameIdx);
            }
            cursor.close();
        }
        return result;
    }

    /** Util: copia conteúdo do Uri para um arquivo temporário no cache */
    private File copiarParaCache(Uri uri, String nomeDesejado) throws Exception {
        File outFile = new File(getCacheDir(), nomeDesejado);
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(outFile)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.flush();
        }
        return outFile;
    }

    /** Retorno da permissão: se concedida, abre a galeria */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_PERMISSAO_GALERIA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirSeletorDeImagem();
            } else {
                Toast.makeText(this, "Permissão negada. Não é possível selecionar imagens.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
