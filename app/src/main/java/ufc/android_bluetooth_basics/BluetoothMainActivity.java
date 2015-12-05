package ufc.android_bluetooth_basics;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class BluetoothMainActivity extends AppCompatActivity {

    private final String TAG = "BLUETOOTH_BASICS";

    private ListView listViewDevices;

    private BluetoothAdapter bluetoothAdapter;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private final int DISCOVERABLE_TIMEOUT = 0;

    private List<BluetoothDevice> bluetoothDevices;
    private List<String> bluetoothDevicesString;
    private ArrayAdapter<String> arrayAdapter;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (!bluetoothDevices.contains(device)) {
                    addBluetoothDevice(device);
                    arrayAdapter.notifyDataSetChanged();
                }
                snakeBar("Device found: " + device.getName());
                Log.d(TAG, "Device found: " + device.getName());
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                snakeBar(R.string.end_discovering);
                Log.d(TAG, "Ended discovering");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listViewDevices = (ListView) findViewById(R.id.listViewDevices);
        bluetoothDevices = new ArrayList<BluetoothDevice>();
        bluetoothDevicesString = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, bluetoothDevicesString);
        listViewDevices.setAdapter(arrayAdapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bluetoothAdapter.startDiscovery()) {
                    snakeBar(R.string.start_discovering);
                    Log.d(TAG, "Discovering start");
                } else {
                    snakeBar(R.string.start_discovering_error);
                    Log.d(TAG, "Error: can't start discovering");
                }
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver, filter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            snakeBar(R.string.not_support_bluetooth);
            Log.d(TAG, "Device does not support Bluetooth");
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
            } else {
                setDiscoverable();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                if (resultCode == Activity.RESULT_OK) {
                    setDiscoverable();
                } else {
                    snakeBar(R.string.request_enable_bluetooth_error);
                    Log.d(TAG, "Bluetooth not enabled");
                    this.finish();
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
            Log.d(TAG, "Disable bluetooth");
        }
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(broadcastReceiver);
    }

    private void setDiscoverable() {
        Log.d(TAG, "Bluetooth enabled");
        try {
            Method setScanModeMethod = BluetoothAdapter.class.getDeclaredMethod("setScanMode", int.class, int.class);
            setScanModeMethod.invoke(bluetoothAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, DISCOVERABLE_TIMEOUT);
        } catch (Exception e) {
            Log.d(TAG, "Error: this device is not discoverable");
            e.printStackTrace();
        }
        snakeBar(R.string.set_discoverable);
        Log.d(TAG, "This device is discoverable");
    }

    private void addBluetoothDevice(BluetoothDevice device) {
        bluetoothDevices.add(device);
        bluetoothDevicesString.add(device.getName() + "\n" + device.getAddress() + "\nFound");
    }

    private void removeBluetoothDevice(BluetoothDevice device) {
        int index = bluetoothDevices.indexOf(device);
        bluetoothDevices.remove(index);
        bluetoothDevicesString.remove(index);
    }

    private BluetoothDevice getBluetoothDevice(BluetoothDevice device) {
        return bluetoothDevices.get(bluetoothDevices.indexOf(device));
    }

    private void snakeBar(int resId) {
        Snackbar.make(findViewById(android.R.id.content), resId, Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    private void snakeBar(String text) {
        Snackbar.make(findViewById(android.R.id.content), text, Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }
}
