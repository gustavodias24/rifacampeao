package benicio.solucoes.rifacampeo;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import benicio.solucoes.rifacampeo.adapters.GanhadorAdapter;
import benicio.solucoes.rifacampeo.databinding.ActivityDatasAnterioresSorteioBinding;
import benicio.solucoes.rifacampeo.objects.GanhadorModel;
import benicio.solucoes.rifacampeo.utils.PrinterTicketUtils2;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DatasAnterioresSorteioActivity extends AppCompatActivity
        implements GanhadorAdapter.PrinterBridge {

    private static final int REQ_BLUETOOTH_PERMS = 1001;
    private static final int REQUEST_ENABLE_BT = 100;

    private ActivityDatasAnterioresSorteioBinding mainBinding;
    private GanhadorAdapter adapter;
    private BluetoothDevice printerBluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mainBinding = ActivityDatasAnterioresSorteioBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        boolean removeDelete = false;
        Bundle b = getIntent().getExtras();
        if (b != null) {
            removeDelete = b.getBoolean("remove", false);
        }

        checarPermissoesBluetooth();

        adapter = new GanhadorAdapter(this, removeDelete);

        mainBinding.recyclerDatas.setLayoutManager(new LinearLayoutManager(this));
        mainBinding.recyclerDatas.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        );
        mainBinding.recyclerDatas.setAdapter(adapter);

        mainBinding.etBusca.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        carregarGanhadores();
    }

    @Override
    public void imprimirGanhadorDireto(GanhadorModel item) {
        if (printerBluetooth == null) {
            acharPrinterBluetooth();
        }

        if (printerBluetooth == null) {
            Toast.makeText(this, "Impressora Bluetooth não encontrada.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (item == null) {
            Toast.makeText(this, "Item inválido para imprimir.", Toast.LENGTH_SHORT).show();
            return;
        }

        PrinterTicketUtils2.printGanhador(this, printerBluetooth, item);
    }

    private void carregarGanhadores() {
        RetrofitUtils.getApiService().ganhadores()
                .enqueue(new Callback<List<GanhadorModel>>() {
                    @Override
                    public void onResponse(Call<List<GanhadorModel>> call, Response<List<GanhadorModel>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<GanhadorModel> lista = response.body();
                            Collections.reverse(lista);
                            adapter.setItems(lista);
                        } else {
                            Toast.makeText(
                                    DatasAnterioresSorteioActivity.this,
                                    "Falha ao carregar: " + response.code(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<GanhadorModel>> call, Throwable t) {
                        Toast.makeText(
                                DatasAnterioresSorteioActivity.this,
                                "Erro de rede: " + t.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    @SuppressLint("MissingPermission")
    private void acharPrinterBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth não foi encontrado neste equipamento.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices == null || bondedDevices.isEmpty()) {
            Toast.makeText(this, "Nenhuma impressora Bluetooth pareada encontrada.", Toast.LENGTH_SHORT).show();
            return;
        }

        for (BluetoothDevice bondedDevice : bondedDevices) {
            String nome = bondedDevice.getName();
            if (nome != null && nome.toLowerCase(Locale.ROOT).contains("print")) {
                printerBluetooth = bondedDevice;
                break;
            }
        }

        if (printerBluetooth == null) {
            for (BluetoothDevice bondedDevice : bondedDevices) {
                String nome = bondedDevice.getName();
                if (nome != null && !nome.trim().isEmpty()) {
                    printerBluetooth = bondedDevice;
                    break;
                }
            }
        }
    }

    private void checarPermissoesBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] perms = new String[]{
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT
            };

            List<String> faltando = new ArrayList<>();
            for (String p : perms) {
                if (ActivityCompat.checkSelfPermission(this, p)
                        != PackageManager.PERMISSION_GRANTED) {
                    faltando.add(p);
                }
            }

            if (!faltando.isEmpty()) {
                ActivityCompat.requestPermissions(
                        this,
                        faltando.toArray(new String[0]),
                        REQ_BLUETOOTH_PERMS
                );
            } else {
                acharPrinterBluetooth();
            }
        } else {
            acharPrinterBluetooth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_BLUETOOTH_PERMS) {
            boolean tudoOk = true;

            if (grantResults.length == 0) {
                tudoOk = false;
            } else {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        tudoOk = false;
                        break;
                    }
                }
            }

            if (tudoOk) {
                acharPrinterBluetooth();
            } else {
                Toast.makeText(this, "Permissões de Bluetooth negadas.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    @SuppressLint("MissingPermission")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            acharPrinterBluetooth();
        }
    }
}