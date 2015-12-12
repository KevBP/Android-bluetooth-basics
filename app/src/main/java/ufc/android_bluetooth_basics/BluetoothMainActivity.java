package ufc.android_bluetooth_basics;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BluetoothMainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private final String TAG = "BLUETOOTH_BASICS";
    private final String NAME = "BluetoothBasics";
    private final UUID MY_UUID = UUID.fromString("46b70f01-7bd0-44f9-8dcb-22b3e0e00fb5");
    private final int DISCOVERABLE_TIMEOUT = 0;
    private ListView listViewDevices;
    private BluetoothAdapter bluetoothAdapter;
    private List<BluetoothDevice> bluetoothDevices;
    private List<String> bluetoothDevicesString;
    private ArrayAdapter<String> arrayAdapter;

    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private Handler handler = new Handler(); // TODO

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
        listViewDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                connectThread = new ConnectThread(getBluetoothDeviceFromIndex(position));
                connectThread.start();
                BluetoothDevice device = getBluetoothDeviceFromIndex(position);
                bluetoothDevicesString.set(position, device.getName() + "\n" + device.getAddress() + "\nConnected");
                arrayAdapter.notifyDataSetChanged();
            }
        });

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

        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
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

    private BluetoothDevice getBluetoothDeviceFromIndex(int index) {
        return bluetoothDevices.get(index);
    }

    private synchronized void connected(BluetoothSocket socket) {
        BluetoothDevice device = socket.getRemoteDevice();
        Log.d(TAG, "Connection accepted");
        snakeBar(R.string.new_connection);

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }

    private void snakeBar(int resId) {
        Snackbar.make(findViewById(android.R.id.content), resId, Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    private void snakeBar(String text) {
        Snackbar.make(findViewById(android.R.id.content), text, Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket bluetoothServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) { }
            bluetoothServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Start server");
            snakeBar(R.string.start_server);
            BluetoothSocket socket = null;
            while (true) {
                try {
                    socket = bluetoothServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                if (socket != null) {
                    connected(socket);
                    /*try {
                        bluetoothServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG, "Can't stop server");
                    }
                    break;*/
                }
            }
            Log.d(TAG, "Stop server");
            snakeBar(R.string.stop_server);
        }

        public void cancel() {
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            bluetoothDevice = device;

            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            bluetoothSocket = tmp;
            Log.d(TAG, "Socket created");
            snakeBar(R.string.new_socket);
        }

        public void run() {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
                snakeBar(R.string.end_discovering);
                Log.d(TAG, "Ended discovering");
            }

            try {
                bluetoothSocket.connect();
            } catch (IOException connectException) {
                try {
                    bluetoothSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            connected(bluetoothSocket);
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private ScheduledExecutorService messageLoopScheduler;
        private byte test = (byte)(Math.random() * 100);

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            device = socket.getRemoteDevice();
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            inputStream = tmpIn;
            outputStream = tmpOut;

            messageLoopScheduler = Executors.newScheduledThreadPool(7);
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            messageLoopScheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    byte[] tmp = new byte[1];
                    tmp[0] = test;
                    write(tmp);
                }
            }, 0, 10, TimeUnit.SECONDS);

            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    snakeBar("Receive " + String.valueOf(buffer[0]) + " : " + device.getName());
                    Log.d(TAG, "Receive " + String.valueOf(buffer[0]) + " : " + device.getName());
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
                snakeBar("Send " + String.valueOf(bytes[0])  + " : " + device.getName());
                Log.d(TAG, "Send " + String.valueOf(bytes[0])  + " : " + device.getName());
            } catch (IOException e) { }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) { }
        }
    }
}
