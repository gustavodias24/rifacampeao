package benicio.solucoes.rifacampeo.utils;

import java.util.List;

import benicio.solucoes.rifacampeo.RegioesActivity;
import benicio.solucoes.rifacampeo.objects.BilheteModel;
import benicio.solucoes.rifacampeo.objects.DateLimitModel;
import benicio.solucoes.rifacampeo.objects.GanhadorModel;
import benicio.solucoes.rifacampeo.objects.LancamentoModel;
import benicio.solucoes.rifacampeo.objects.NumerosPremiadosModel;
import benicio.solucoes.rifacampeo.objects.QueryModelEmpty;
import benicio.solucoes.rifacampeo.objects.QueryModelVendedorID;
import benicio.solucoes.rifacampeo.objects.RegiaoModel;
import benicio.solucoes.rifacampeo.objects.ResponseSimple;
import benicio.solucoes.rifacampeo.objects.ResultadoBilheteModel;
import benicio.solucoes.rifacampeo.objects.RetornoModel;
import benicio.solucoes.rifacampeo.objects.SaveBilheteResponse;
import benicio.solucoes.rifacampeo.objects.VendedorModel;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {

    @Multipart
    @POST("upload")
    Call<ResponseBody> uploadImagem(@Part MultipartBody.Part imagem);

    // Listar links das imagens
    @GET("listar-imagens")
    Call<List<String>> listarImagens();

    // Deletar imagem pelo nome
    @GET("deletar-imagem/{nome}")
    Call<ResponseBody> deletarImagem(@Path("nome") String nome);

    @POST("datalimite")
    Call<ResponseBody> setarDataLimite(@Body DateLimitModel datalimite);

    @GET("datalimite")
    Call<DateLimitModel> returnDataLimite();


    @POST("save/info")
    Call<RetornoModel> saveVendedores(@Body VendedorModel vendedorModel);


    @POST("return/info/{info}")
    Call<List<VendedorModel>> returnVendedores(@Path("info") int info, @Body QueryModelEmpty queryModelEmpty);

    @POST("save/info")
    Call<RetornoModel> saveRegiao(@Body RegiaoModel regiaoModel);

    @POST("return/info/{info}")
    Call<List<RegiaoModel>> returnRegioes(@Path("info") int info, @Body QueryModelEmpty queryModelEmpty);

    @POST("login")
    Call<ResponseSimple> login(@Body VendedorModel info);

    @GET("listar-imagens")
    Call<List<String>> returmImages();

    @POST("salvar-bilhete")
    Call<SaveBilheteResponse> saveBilhete(@Body BilheteModel bilheteModel);

    @POST("check-number")
    Call<SaveBilheteResponse> checkNumber(@Body BilheteModel bilheteModel);

    @POST("return/info/{info}")
    Call<List<BilheteModel>> returnBilhetes(@Path("info") int info, @Body QueryModelEmpty queryModelEmpty);

    @POST("return/info/{info}")
    Call<List<LancamentoModel>> returnLancamento(@Path("info") int info, @Body QueryModelVendedorID queryModelVendedorID);

    @POST("save/info")
    Call<RetornoModel> saveLancamento(@Body LancamentoModel lancamentoModel);

    @GET("get-bilhete/{id}")
    Call<String> getBilheteHtml(@Path("id") String id);

    @POST("bilhetes-ganharadores")
    Call<ResultadoBilheteModel> bilhetes_ganharadores(@Body NumerosPremiadosModel numerosPremiadosModel);

    @GET("ganhadores")
    Call<List<GanhadorModel>> ganhadores();

    @DELETE("/ganhadores/{id}")
    Call<RetornoModel> ganhador_delete(@Path("id") String _id);
    @POST("check-date")
    Call<RetornoModel> check_date(@Body BilheteModel dateLimitModel);

    @GET("delete/vendedor/{id}")
    Call<RetornoModel> vendedor_delete(@Path("id") String _id);



}
