package ch.heigvd.iict.sym_labo4;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;
import java.util.List;

import ch.heigvd.iict.sym_labo4.abstractactivies.BaseTemplateActivity;
import ch.heigvd.iict.sym_labo4.adapters.ResultsAdapter;
import ch.heigvd.iict.sym_labo4.viewmodels.BleOperationsViewModel;

/**
 * Project: Labo4
 * Created by fabien.dutoit on 09.08.2019
 * (C) 2019 - HEIG-VD, IICT
 */

public class BleActivity extends BaseTemplateActivity {

    private static final String TAG = BleActivity.class.getSimpleName();

    //system services
    private BluetoothAdapter bluetoothAdapter = null;

    //view model
    private BleOperationsViewModel bleViewModel = null;

    //gui elements
    private View operationPanel = null;
    private View scanPanel = null;

    private ListView scanResults = null;
    private TextView emptyScanResults = null;
    //TODO j'ai ajouter ces 3 lignes
    private TextView temperatureScanResults = null;
    private TextView clickResults = null;
    private Button temperatureButton = null;
    private Button sendIntButton = null;
    private Button setTimeButton = null;
    private TextView timeResults = null;

    //menu elements
    private MenuItem scanMenuBtn = null;
    private MenuItem disconnectMenuBtn = null;

    //adapters
    private ResultsAdapter scanResultsAdapter = null;

    //states
    private Handler handler = null;
    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);

        this.handler = new Handler();

        //enable and start bluetooth - initialize bluetooth adapter
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();

        //link GUI
        this.operationPanel = findViewById(R.id.ble_operation);
        this.scanPanel = findViewById(R.id.ble_scan);
        this.scanResults = findViewById(R.id.ble_scanresults);
        this.emptyScanResults = findViewById(R.id.ble_scanresults_empty);
        //TODO 3 lignes suivante
        this.temperatureButton = findViewById(R.id.ble_temperature_button);
        this.temperatureButton.setOnClickListener(v -> bleViewModel.readTemperature());
        this.temperatureScanResults = findViewById(R.id.ble_temperature_text);
        this.sendIntButton = findViewById(R.id.ble_send_int);
        this.sendIntButton.setOnClickListener(v -> bleViewModel.sendInt());
        this.setTimeButton = findViewById(R.id.ble_set_time);
        this.setTimeButton.setOnClickListener(v -> bleViewModel.setTime());
        this.clickResults = findViewById(R.id.ble_click_text);
        this.timeResults = findViewById(R.id.ble_time_text);

        //manage scanned item
        this.scanResultsAdapter = new ResultsAdapter(this);
        this.scanResults.setAdapter(this.scanResultsAdapter);
        this.scanResults.setEmptyView(this.emptyScanResults);

        //connect to view model
        this.bleViewModel = ViewModelProviders.of(this).get(BleOperationsViewModel.class);

        updateGui();

        //events
        this.scanResults.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            runOnUiThread(() -> {
                //we stop scanning
                scanLeDevice(false);
                //we connect to the clicked device
                bleViewModel.connect(((ScanResult) scanResultsAdapter.getItem(position)).getDevice());
            });
        });

        //ble events
        this.bleViewModel.isConnected().observe(this, (isConnected) -> {
            updateGui();
        });
        //temperature
        this.bleViewModel.isTemperatureConnected().observe(this, integer -> {
            //TODO division par 10 en float
            String temp = "temperature " + integer / 10.0;
            temperatureScanResults.setText(temp);
        });
        //clickCounter
        this.bleViewModel.isclickConnected().observe(this,integer -> {
            String click = "click counter " + integer;
            clickResults.setText(click);
        });
        //time
        this.bleViewModel.isTimeConnected().observe(this,s -> {
            timeResults.setText(s);
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ble_menu, menu);
        //we link the two menu items
        this.scanMenuBtn = menu.findItem(R.id.menu_ble_search);
        this.disconnectMenuBtn = menu.findItem(R.id.menu_ble_disconnect);
        //we update the gui
        updateGui();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_ble_search) {
            if (isScanning)
                scanLeDevice(false);
            else
                scanLeDevice(true);
            return true;
        } else if (id == R.id.menu_ble_disconnect) {
            bleViewModel.disconnect();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.isScanning)
            scanLeDevice(false);
        if (isFinishing())
            this.bleViewModel.disconnect();
    }

    /*
     * Method used to update the GUI according to BLE status:
     * - connected: display operation panel (BLE control panel)
     * - not connected: display scan result
     */
    private void updateGui() {
        Boolean isConnected = this.bleViewModel.isConnected().getValue();
        if (isConnected != null && isConnected) {
            this.scanPanel.setVisibility(View.GONE);
            this.operationPanel.setVisibility(View.VISIBLE);

            if (this.scanMenuBtn != null && this.disconnectMenuBtn != null) {
                this.scanMenuBtn.setVisible(false);
                this.disconnectMenuBtn.setVisible(true);
            }
        } else {
            this.operationPanel.setVisibility(View.GONE);
            this.scanPanel.setVisibility(View.VISIBLE);

            if (this.scanMenuBtn != null && this.disconnectMenuBtn != null) {
                this.disconnectMenuBtn.setVisible(false);
                this.scanMenuBtn.setVisible(true);
            }
        }
    }

    //this method need user granted localisation permission, our demo app is requesting it on MainActivity
    private void scanLeDevice(final boolean enable) {
        final BluetoothLeScanner bluetoothScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (enable) {

            //config
            ScanSettings.Builder builderScanSettings = new ScanSettings.Builder();
            builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
            builderScanSettings.setReportDelay(0);

            //we scan for any BLE device
            //we don't filter them based on advertised services...
            //TODO ajouter un filtre pour n'afficher que les devices proposant
            // le service "SYM" (UUID: "3c0a1000-281d-4b48-b2a7-f15579a1c38f")
            ScanFilter scanFilter = new ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid.fromString("3c0a1000-281d-4b48-b2a7-f15579a1c38f")).build();
            List<ScanFilter> filters = new ArrayList<ScanFilter>();
            filters.add(scanFilter);
            //reset display
            scanResultsAdapter.clear();

            bluetoothScanner.startScan(filters, builderScanSettings.build(), leScanCallback);
            Log.d(TAG, "Start scanning...");
            isScanning = true;

            //we scan only for 15 seconds
            handler.postDelayed(() -> {
                scanLeDevice(false);
            }, 15 * 1000L);

        } else {
            bluetoothScanner.stopScan(leScanCallback);
            isScanning = false;
            Log.d(TAG, "Stop scanning (manual)");
        }
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);
            runOnUiThread(() -> {
                scanResultsAdapter.addDevice(result);
            });
        }
    };

}
