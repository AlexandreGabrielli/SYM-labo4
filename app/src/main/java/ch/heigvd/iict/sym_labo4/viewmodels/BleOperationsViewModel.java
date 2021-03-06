package ch.heigvd.iict.sym_labo4.viewmodels;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Random;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.ble.data.Data;

public class BleOperationsViewModel extends AndroidViewModel {

    private static final String TAG = BleOperationsViewModel.class.getSimpleName();

    private MySymBleManager ble = null;
    private BluetoothGatt mConnection = null;

    //live data - observer
    private final MutableLiveData<Boolean> mIsConnected = new MutableLiveData<>();

    public LiveData<Boolean> isConnected() {
        return mIsConnected;
    }

    //live data - observer temperature
    private final MutableLiveData<Integer> mTemperatureIsConnected = new MutableLiveData<>();

    public LiveData<Integer> isTemperatureConnected() {
        return mTemperatureIsConnected;
    }

    //live data - observer clickCounter
    private final MutableLiveData<Integer> mclickIsConnected = new MutableLiveData<>();

    public LiveData<Integer> isclickConnected() {
        return mclickIsConnected;
    }

    //live data - observer clickCounter
    private final MutableLiveData<String> mTimeIsConnected = new MutableLiveData<>();

    public LiveData<String> isTimeConnected() {
        return mTimeIsConnected;
    }

    //references to the Services and Characteristics of the SYM Pixl
    private BluetoothGattService timeService = null, symService = null;
    private BluetoothGattCharacteristic currentTimeChar = null, integerChar = null, temperatureChar = null, buttonClickChar = null;

    public BleOperationsViewModel(Application application) {
        super(application);
        this.mIsConnected.setValue(false); //to be sure that it's never null
        this.ble = new MySymBleManager();
        this.ble.setGattCallbacks(this.bleManagerCallbacks);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "onCleared");
        this.ble.disconnect();
    }

    public void connect(BluetoothDevice device) {
        Log.d(TAG, "User request connection to: " + device);
        if (!mIsConnected.getValue()) {
            this.ble.connect(device)
                    .retry(1, 100)
                    .useAutoConnect(false)
                    .enqueue();
        }
    }

    public void disconnect() {
        Log.d(TAG, "User request disconnection");
        this.ble.disconnect();
        if (mConnection != null) {
            mConnection.disconnect();
        }
    }

    /* TODO
        vous pouvez placer ici les différentes méthodes permettant à l'utilisateur
        d'interagir avec le périphérique depuis l'activité
     */
    public boolean sendInt(int integer) {
        if (!isConnected().getValue() || integerChar == null) return false;
        return ble.sendInt(integer);
    }

    public boolean setTime() {
        if (!isConnected().getValue() || currentTimeChar == null) return false;
        return ble.setTime();

    }


    public boolean readTemperature() {
        if (!isConnected().getValue() || temperatureChar == null) return false;
        return ble.readTemperature();
    }

    private BleManagerCallbacks bleManagerCallbacks = new BleManagerCallbacks() {
        @Override
        public void onDeviceConnecting(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceConnecting");
            mIsConnected.setValue(false);
        }

        @Override
        public void onDeviceConnected(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceConnected");
            mIsConnected.setValue(true);
        }

        @Override
        public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceDisconnecting");
            mIsConnected.setValue(false);
        }

        @Override
        public void onDeviceDisconnected(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceDisconnected");
            mIsConnected.setValue(false);
        }

        @Override
        public void onLinkLossOccurred(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onLinkLossOccurred");
        }

        @Override
        public void onServicesDiscovered(@NonNull BluetoothDevice device, boolean optionalServicesFound) {
            Log.d(TAG, "onServicesDiscovered");
        }

        @Override
        public void onDeviceReady(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceReady");
        }

        @Override
        public void onBondingRequired(@NonNull BluetoothDevice device) {
            Log.w(TAG, "onBondingRequired");
        }

        @Override
        public void onBonded(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onBonded");
        }

        @Override
        public void onBondingFailed(@NonNull BluetoothDevice device) {
            Log.e(TAG, "onBondingFailed");
        }

        @Override
        public void onError(@NonNull BluetoothDevice device, @NonNull String message, int errorCode) {
            Log.e(TAG, "onError:" + errorCode);
        }

        @Override
        public void onDeviceNotSupported(@NonNull BluetoothDevice device) {
            Log.e(TAG, "onDeviceNotSupported");
            Toast.makeText(getApplication(), "Device not supported", Toast.LENGTH_SHORT).show();
        }
    };


    /*
     *  This class is used to implement the protocol to communicate with the BLE device
     */
    private class MySymBleManager extends BleManager<BleManagerCallbacks> {

        private MySymBleManager() {
            super(getApplication());
        }

        @Override
        public BleManagerGattCallback getGattCallback() {
            return mGattCallback;
        }

        /**
         * BluetoothGatt callbacks object.
         */
        private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {

            @Override
            public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
                mConnection = gatt; //trick to force disconnection
                Log.d(TAG, "isRequiredServiceSupported - discovered services:");
                timeService = gatt.getService(UUID.fromString("00001805-0000-1000-8000-00805f9b34fb"));
                symService = gatt.getService(UUID.fromString("3c0a1000-281d-4b48-b2a7-f15579a1c38f"));
                currentTimeChar = timeService.getCharacteristic(UUID.fromString("00002A2B-0000-1000-8000-00805f9b34fb"));
                gatt.setCharacteristicNotification(currentTimeChar, true); //enable or disable notifications/indications for a given characteristic.

                integerChar = symService.getCharacteristic(UUID.fromString("3c0a1001-281d-4b48-b2a7-f15579a1c38f"));
                temperatureChar = symService.getCharacteristic(UUID.fromString("3c0a1002-281d-4b48-b2a7-f15579a1c38f"));
                buttonClickChar = symService.getCharacteristic(UUID.fromString("3c0a1003-281d-4b48-b2a7-f15579a1c38f"));
                gatt.setCharacteristicNotification(buttonClickChar, true);

                return timeService != null && symService != null && currentTimeChar != null && integerChar != null
                        && temperatureChar != null && buttonClickChar != null;

            }

            @Override
            protected void initialize() {

                setNotificationCallback(currentTimeChar)
                        .with((device, data) -> {
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(data.getIntValue(Data.FORMAT_UINT16, 0), data.getIntValue(Data.FORMAT_UINT8, 2) - 1, data.getIntValue(Data.FORMAT_UINT8, 3),
                                    data.getIntValue(Data.FORMAT_UINT8, 4), data.getIntValue(Data.FORMAT_UINT8, 5), data.getIntValue(Data.FORMAT_UINT8, 6));

                            String result = calendar.get(Calendar.HOUR_OF_DAY) + "H" + calendar.get(Calendar.MINUTE) + "m" + calendar.get(Calendar.SECOND)
                                    + " the " + calendar.get(Calendar.DATE) + "/" + (calendar.get(Calendar.MONTH) + 1) + "/" + calendar.get(Calendar.YEAR);
                            mTimeIsConnected.setValue(result);
                        });
                enableNotifications(currentTimeChar)
                        .enqueue();

                setNotificationCallback(buttonClickChar)
                        .with((device, data) -> {
                            int clickCounter = data.getIntValue(Data.FORMAT_UINT8, 0);
                            System.out.println("-------------------------------" + clickCounter);
                            mclickIsConnected.setValue(clickCounter);
                        });
                enableNotifications(buttonClickChar)
                        .enqueue();

            }


            @Override
            protected void onDeviceDisconnected() {
                //we reset services and characteristics
                timeService = null;
                currentTimeChar = null;

                symService = null;
                integerChar = null;
                temperatureChar = null;
                buttonClickChar = null;
            }
        };

        public boolean readTemperature() {
            readCharacteristic(temperatureChar).with((device, data) -> {
                int temperature = data.getIntValue(Data.FORMAT_UINT16, 0);
                mTemperatureIsConnected.setValue(temperature);
            }).enqueue();
            return true;
        }

        public boolean sendInt(int integer) {
            Data data = Data.from(Integer.toString(integer));
            writeCharacteristic(integerChar, data).enqueue();

            return true;
        }

        public boolean setTime() {
            Calendar calendar = Calendar.getInstance();
            short year = (short) calendar.get(Calendar.YEAR);
            ByteBuffer bufferYear = ByteBuffer.allocate(2);
            bufferYear.putShort(year);
            byte[] bytesYears = bufferYear.array();
            byte[] bytes = {
                    bytesYears[1],
                    bytesYears[0],
                    (byte) (calendar.get(Calendar.MONTH) + 1),
                    (byte) calendar.get(Calendar.DAY_OF_MONTH),
                    (byte) calendar.get(Calendar.HOUR_OF_DAY),
                    (byte) calendar.get(Calendar.MINUTE),
                    (byte) calendar.get(Calendar.SECOND),
                    (byte) calendar.get(Calendar.DAY_OF_WEEK),
                    0x0,
                    0x0
            };

            writeCharacteristic(currentTimeChar, bytes).enqueue();
            return true;
        }
    }
}
