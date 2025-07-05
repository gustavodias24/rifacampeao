package benicio.solucoes.rifacampeo.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

import benicio.solucoes.rifacampeo.R;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdapterBanners extends RecyclerView.Adapter<AdapterBanners.MyViewHolder> {

    List<String> banners;
    Activity a;

    public AdapterBanners(List<String> banners, Activity a) {
        this.banners = banners;
        this.a = a;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_exibir_banner, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int p) {
        Picasso.get().load(banners.get(p)).into(
                holder.bannerImage
        );


        holder.removerBanner.setOnClickListener(v -> {
            RetrofitUtils.getApiService().deletarImagem(
                    banners.get(p).split("/image/")[1]
            ).enqueue(new Callback<ResponseBody>() {
                @SuppressLint("NotifyDataSetChanged")
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        banners.remove(p);
                        Toast.makeText(a, "Imagem Removida!", Toast.LENGTH_SHORT).show();
                        notifyDataSetChanged();
                    } else {
                        Toast.makeText(a, "Problema de Conexão!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable throwable) {

                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView bannerImage;
        Button removerBanner;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            bannerImage = itemView.findViewById(R.id.banner_exibir);
            removerBanner = itemView.findViewById(R.id.remover_banner);
        }
    }
}
