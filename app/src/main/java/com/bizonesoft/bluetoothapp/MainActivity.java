package com.bizonesoft.bluetoothapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter BA;
    private ArrayList<DeviceInfo> list = new ArrayList<>();
    private ArrayList<String> messagelist = new ArrayList<>();
    private ArrayList<Integer> button_ids = new ArrayList<>();
    ArrayAdapter adapter, message_adapter;
    ListView listView, response_list;
    TextView enabled, read, deviceinfo, deviceaddr, text_connection_count;
    Switch enabling_switch, type_switch;
    Button discoverable_Button, scan_device, submit, clear_chat;
    ProgressBar progressBar, progressbar_paired;
    EditText write;
    boolean isdiscoverable_btn = false, bluetooth_enabled = false, isdiscoverable = false, isServer = false;
    ArrayList<BluetoothSocket> communicating_sockets = new ArrayList<>();
    public final int REQUEST_COARSE_LOCATION = 0;
    Button parking1, parking2, parking3, parking4,parking5,parking6;

    final int BLUETOOTH_ON_VALUE = 1;
    final int DISCOVERABLE_VALUE = 2;

    ArrayList<ManageConnectThread> manageConnectThread;
    private BluetoothServerSocket mBTServerSocket = null;
    private BluetoothSocket mBTSocket = null;
    Handler mHandler;
    InitiateConnection InitiateConnection;
    String connectedTo = "";
    LinearLayout buttongroup;
    ArrayList<String> pin_nos=new ArrayList<>();
    boolean message_list=false;
    boolean clientConnected=false;
    String cust_UUID="00001101-0000-1000-8000-00805F9B34FB";

    LinearLayout main_chat_ll;
    RelativeLayout main_paired_device_rl,main_conn_tpe_rl,main_rl_bluetoothinfo;


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu); //your file name
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.pinchange:
                changePinAlert();
                return true;
            case R.id.list_visible:
                if (!message_list)
                {
                    message_list=true;
                    main_chat_ll.setVisibility(View.VISIBLE);
                }else {
                    message_list=false;
                    main_chat_ll.setVisibility(View.GONE);
                }
                return true;
            case R.id.menu_uuid:
                setUUID(cust_UUID);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void changePinAlert()
    {
        final EditText taskEditText = new EditText(this);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Change Pin Settings ?")
                .setMessage("Current Pin Settings : "+pin_nos.toString()+" \n\nPlease add new Pin numbers in following format.\nEg : 1,2,5")
                .setView(taskEditText)
                .setPositiveButton("Submit", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        if (taskEditText.getText().toString().length()>0)
                        {
                            String edt=taskEditText.getText().toString().trim();
                            String[] temp=edt.split(",");
                            if (temp.length>0)
                            {
                                pin_nos.clear();
                                Collections.addAll(pin_nos, temp);
                                showSnackbar("Pins setting changed successfully");
                                setPin_textChange();
                            }else {
                                showSnackbar("Pin number entered in invalid format");
                            }
                        }else {
                            showSnackbar("Pin number Cannot be Empty");
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    void setUUID(String uuid)
    {
        final EditText taskEditText = new EditText(this);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Set UUID")
                .setMessage("Current UUID : "+cust_UUID)
                .setView(taskEditText)
                .setPositiveButton("Submit", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        String edt=taskEditText.getText().toString().trim();
                        if (edt.length()==36)
                        {
                                showSnackbar("UUID changed successfully");
                                setUUID(edt);
                        }else {
                            showSnackbar("Invalid UUID");
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }


    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    @Override
    public void onBackPressed()
    {
        if (clientConnected)
        {
            changeUi(false);
            setDefaultButtonColor();
            closeSocket(isServer);
        }
        else
        {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.listView);
        response_list = findViewById(R.id.response);
        enabled = findViewById(R.id.enabled);
        enabling_switch = findViewById(R.id.enable_switch);
        type_switch = findViewById(R.id.type_switch);
        discoverable_Button = findViewById(R.id.discoverable);
        scan_device = findViewById(R.id.scan_device);
        progressBar = findViewById(R.id.progressbar);
        progressbar_paired = findViewById(R.id.progressbar_paired);
        read = findViewById(R.id.read);
        write = findViewById(R.id.write);
        submit = findViewById(R.id.submit);
        clear_chat = findViewById(R.id.clearchat);
        deviceinfo = findViewById(R.id.deviceinfo);
        deviceaddr = findViewById(R.id.deviceaddr);
        text_connection_count = findViewById(R.id.conn_count);
        parking1 = findViewById(R.id.parking1);
        parking2 = findViewById(R.id.parking2);
        parking3 = findViewById(R.id.parking3);
        parking4 = findViewById(R.id.parking4);
        parking5 = findViewById(R.id.parking5);
        parking6 = findViewById(R.id.parking6);
        buttongroup = findViewById(R.id.buttongroup);

        main_chat_ll=findViewById(R.id.chat_ll);
        main_paired_device_rl=findViewById(R.id.paired_device_rl);
        main_rl_bluetoothinfo=findViewById(R.id.rl_bluetoothinfo);
        main_conn_tpe_rl=findViewById(R.id.conn_tpe_rl);

        button_ids.add(parking1.getId());
        button_ids.add(parking2.getId());
        button_ids.add(parking3.getId());
        button_ids.add(parking4.getId());
        button_ids.add(parking5.getId());
        button_ids.add(parking6.getId());

        pin_nos.add("1");
        pin_nos.add("2");
        pin_nos.add("3");
        pin_nos.add("4");
        pin_nos.add("5");
        pin_nos.add("6");

//        buttongroup.setVisibility(View.GONE);

        text_connection_count.setText("0");

        setDefaultButtonColor();

        manageConnectThread = new ArrayList<>();

        try
        {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }catch (Exception ignored)
        {}

        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == MessageConstants.MESSAGE_READ)
                {
                    String readMessage="";
                    try {
//                        String base64encoded = new String((byte[]) msg.obj);
                        String base64encoded = (String) msg.obj;
//                        byte[] arr = Base64.decode(base64encoded, Base64.DEFAULT);
                        readMessage = new String(base64encoded);
                        String msg_frm = "unknown";
                        try {
                            msg_frm = communicating_sockets.get(msg.arg2).getRemoteDevice().getName();
                            assignButton(msg.arg2, readMessage);
                        } catch (Exception e) {
                        }
                        addMessagetoList(msg_frm + " : " + readMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                        addMessagetoList("Error processing message");
                        if (!message_list) {
                            showSnackbar("Error processing message");
                        }
                    }
                } else if (msg.what == MessageConstants.MESSAGE_CONNECTED) {
                    if (msg.arg1 == 1) {
                        connectedTo = msg.obj.toString();
                        addMessagetoList("Connected to " + msg.obj.toString());
                    } else
                        addMessagetoList("Failed to establish Connection");
                } else if (msg.what == MessageConstants.MESSAGE_TOAST) {
                    showToast((String) msg.obj);
                } else if (msg.what == MessageConstants.MESSAGE_SOCKET_CLOSED) {
                    //String txt= msg.arg1 ==0?"Server Socket Closed":msg.arg1==1?"Client Socket Closed":msg.arg1>1?"Socket Closed":"Socket Closed";
                    if (msg.arg1 > 1) {
                        removeSocket_frm_List(msg.arg2);
                    } else if (msg.arg1 == 0) {
                        setDefault_values();
                        type_switch.setChecked(false);
                        isServer = false;
                        showSnackbar("Server Shutdown");
                    } else {
                        setDefault_values();
                    }
                    //showToast(txt);
                } else if (msg.what == MessageConstants.MESSAGE_SCAN_STARTED) {
                    scan_device.setEnabled(false);
                    progressbar_paired.setVisibility(View.VISIBLE);
                } else if (msg.what == MessageConstants.MESSAGE_SCAN_FINISHED) {
                    progressbar_paired.setVisibility(View.INVISIBLE);
                    showToast("Scan Complete");
                    scan_device.setEnabled(true);
                } else if (msg.what == MessageConstants.MESSAGE_WRITE) {
                    //Nothing
                } else if (msg.what == MessageConstants.MESSAGE_DEVICE_DISCONNECTED) {
                    String device_disconnected = "";
                    try {
                        device_disconnected = communicating_sockets.get(msg.arg2).getRemoteDevice().getName();
                    } catch (Exception e) {
                    }
                    showToast("Device " + device_disconnected + " Disconnected");
                } else if (msg.what == MessageConstants.MESSAGE_DEVICE_DISCOVERABLE) {
                    isdiscoverable = false;
                    discoverable_Button.setText("Make Device Discoverable");
                    discoverable_Button.setTextColor(getBaseContext().getResources().getColor(R.color.colorPrimary));
                    discoverable_Button.setEnabled(true);

                } else if (msg.what == MessageConstants.MESSAGE_FAILEDTO_CONNECT_SERVER) {
                    setDefault_values();
                    showToast("Failed to connect to server");
                } else if (msg.what == MessageConstants.MESSAGE_TIMEOUT) {
                    setDefault_values();
                    showToast("Connection Timeout");
                }
                else if (msg.what == MessageConstants.MESSAGE_CONNECTION_COUNT)
                {
                   text_connection_count.setText(""+msg.obj);
                }
            }
        };

        type_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if (BA.isEnabled()) {
                    isServer = b;
                    type_switch.setChecked(isServer);
//                    if (isServer) {
//                        buttongroup.setVisibility(View.VISIBLE);
//                    } else {
//                        buttongroup.setVisibility(View.GONE);
//                    }

                    if (!isServer && mBTServerSocket != null) {
                        showSnackbar("Server ShutDown");
                        closeSocket(true);
                    } else if (isServer && mBTSocket != null) {
                        closeSocket(false);
                        InitiateConnection = new InitiateConnection();
                        InitiateConnection.start();
                    } else if (isServer && mBTServerSocket == null) {
                        InitiateConnection = new InitiateConnection();
                        InitiateConnection.start();
                    }
                }else
                {
                    type_switch.setChecked(false);
                    TurnOnBluetooth();
                }
            }
        });

        enabling_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton cb, boolean on) {
                if (on) {
                    TurnOnBluetooth();
                    showSnackbar("BLuetooth Turned On");

                    discoverable_Button.setEnabled(true);
                } else {
                    TurnOffBluetooth();
                    showSnackbar("BLuetooth Turned Off");
                }
            }
        });

        discoverable_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isdiscoverable_btn) {
                    MakeDiscoverable();
                } else {
                    showSnackbar("Please TurnOn Bluetooth First");
                }
            }
        });

        scan_device.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    checkLocationPermission();
                } else {
                    Scan_fr_Devices();
                }
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (write.getText().toString().length() > 0 && mBTServerSocket != null || mBTSocket != null) {
                        byte[] base64encode = Base64.encode(write.getText().toString().getBytes(), Base64.DEFAULT);
                        Log.d("thread count ", String.valueOf(manageConnectThread.size()));
                        for (ManageConnectThread m : manageConnectThread) {
                            try {
                                m.write(base64encode);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        String base64encoded = new String(base64encode, "UTF-8");
                        byte[] arr = Base64.decode(base64encoded, Base64.DEFAULT);
                        String writeMessage = new String(arr);
                        addMessagetoList("Me : " + writeMessage);
                        write.setText("");
                        write.clearFocus();

                    } else if (write.getText().toString().length() <= 0) {
                        write.setError("Message cannot be empty");
                    } else {
                        write.setError("Please setup a Connection first");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                if (BA.isEnabled()) {
                    if (mBTServerSocket == null) {
                        if (mBTSocket != null) {
                            closeSocket(false);
                        } else {
                            final BluetoothDevice device = BA.getRemoteDevice(list.get(i).deviceaddr);
                            InitiateConnection = new InitiateConnection(device);
                            InitiateConnection.start();
                        }
                    } else {
                        showToast("Please Unselect Server option to Connect as Client");
                    }
                }else {
                    TurnOnBluetooth();
                }
            }
        });

        clear_chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (message_adapter != null) {
                    if (messagelist.size() > 0) {
                        messagelist.clear();
                        message_adapter.notifyDataSetChanged();
                        showSnackbar("Message history cleared");
                    } else {
                        showSnackbar("Message history empty");
                    }
                } else {
                    showSnackbar("Message history empty");
                }
            }
        });

        BluetoothInit();
        getPairedBluetoothDevices();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Scan_fr_Devices();
                } else {
                    //TODO re-request
                    showToast("Unable to proceed without Coarse Location Permission");
                }
                break;
            }

        }
    }

    protected void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_LOCATION);
        } else {
            Scan_fr_Devices();
        }
    }

    public void Scan_fr_Devices() {
        try
        {
            if (BA.isEnabled()) {
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                registerReceiver(ScanDeviceReceiver, filter);
                if (BA.isDiscovering()) {
                    // Bluetooth is already in modo discovery mode, we cancel to restart it again
                    BA.cancelDiscovery();
                }
                BA.startDiscovery();
            }else {
                TurnOnBluetooth();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("HardwareIds")
    public void BluetoothInit() {
        BA = BluetoothAdapter.getDefaultAdapter();
        String mydevice = " " + BA.getName();
        String mydevice_addr = " " + BA.getAddress();

        deviceinfo.setText(String.format("%s%s", "Device Name : ", mydevice));
        deviceaddr.setText(String.format("%s%s", "Mac Address : ", mydevice_addr));

        if (BA == null) {
            showSnackbar("No BLuetooth Detected");
        } else {
            if (BA.isEnabled()) {
                enabling_switch.setChecked(true);
            }else {
                TurnOnBluetooth();
            }
        }
    }

    public void TurnOnBluetooth() {
        //SENDING REQUEST TO TURN ON BLUETOOTH
        Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(turnOn, BLUETOOTH_ON_VALUE);
    }

    public void TurnOffBluetooth() {
        BA.disable();
        isdiscoverable_btn = false;
        bluetooth_enabled = false;
        enabling_switch.setChecked(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if(requestCode == BLUETOOTH_ON_VALUE)
        {
            if (resultCode==RESULT_OK) {
                isdiscoverable_btn = true;
                enabling_switch.setChecked(true);
                bluetooth_enabled = true;
            }
        }

    }

    public void MakeDiscoverable() {
        Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        getVisible.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
        startActivityForResult(getVisible, DISCOVERABLE_VALUE);
        isdiscoverable = true;
        showSnackbar("Device is Discoverable for 2 Mins");
        discoverable_Button.setText(R.string.device_is_Discoverable);
        discoverable_Button.setTextColor(getBaseContext().getResources().getColor(R.color.colorPrimaryDark));
        discoverable_Button.setEnabled(false);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mHandler.obtainMessage(MessageConstants.MESSAGE_DEVICE_DISCOVERABLE).sendToTarget();
            }
        }, 120000);
    }

    public void getPairedBluetoothDevices() {
        Set<BluetoothDevice> pairedDevices = BA.getBondedDevices();
        for (BluetoothDevice bt : pairedDevices) {
            list.add(new DeviceInfo(bt.getName(), bt.getAddress()));
        }

        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        listView.setAdapter(adapter);
    }

    public void addMessagetoList(String message)
    {
        try {
            if (isServer)
            {
                if (mBTServerSocket==null)
                {
                    return;
                }
            }
            else if (mBTSocket==null)
            {
                return;
            }
            messagelist.add(message);
            if (message_adapter == null) {
                message_adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, messagelist);
                response_list.setAdapter(message_adapter);
            } else
            {
                message_adapter.notifyDataSetChanged();
//                response_list.setAdapter(message_adapter);
            }

            try
            {
                if (!message.startsWith("Me")) {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver ScanDeviceReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                for (int i = 0; i < list.size(); i++) {
                    if (!list.get(i).devicename.equals(deviceName)) {
                        list.add(new DeviceInfo(deviceName, deviceHardwareAddress));
                        adapter.notifyDataSetChanged();
                        break;
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                mHandler.obtainMessage(MessageConstants.MESSAGE_SCAN_STARTED).sendToTarget();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mHandler.obtainMessage(MessageConstants.MESSAGE_SCAN_FINISHED).sendToTarget();
            }
        }
    };

    public class DeviceInfo {
        String devicename, deviceaddr;

        DeviceInfo(String devicename, String deviceaddr) {
            this.devicename = devicename;
            this.deviceaddr = deviceaddr;
        }

        @Override
        public String toString() {
            return "DeviceInfo{" +
                    "devicename='" + devicename + '\'' +
                    ", deviceaddr='" + deviceaddr + '\'' +
                    '}';
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        try {
            unregisterReceiver(ScanDeviceReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeSocket(isServer);
    }

    public class InitiateConnection extends Thread {
        BluetoothDevice device;

        InitiateConnection(BluetoothDevice device) {
            this.device = device;
        }

        InitiateConnection() {
        }

        @Override
        public void run() {
            if (isServer) {
                //IF CONNECTION IS SUCCESSFUL YOU GET SOCKET IN RETURN
                mBTServerSocket = getServercommSocket(BA);
                server_open_fr_new_connection(mBTServerSocket);
            } else {
                //IF CONNECTION IS SUCCESSFUL YOU GET SOCKET IN RETURN
                mBTSocket = getRfcommSocket(device);
                if (mBTSocket != null) {
                    Client_connect client_connect = new Client_connect(mBTSocket);
                    client_connect.start();
                }
            }
        }
    }

    //CALLED BY CLIENT'S DEVICE TO GET CLIENT SOCKET, UUID SHOULD BE SAME AS SERVER'S UUID TO CONNECT
    public BluetoothSocket getRfcommSocket(BluetoothDevice device) {
        BluetoothSocket tmp = null;
        try {
            UUID UUID = java.util.UUID.fromString(cust_UUID);
            Log.d("UUID", cust_UUID);
            tmp = device.createRfcommSocketToServiceRecord(UUID);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("getRfcommSocket", "Client Socket's connect() method failed", e);
            showSnackbar("Failed to get Socket");
        }
        return tmp;
    }

    //CALLED BY CLIENT'S DEVICE TO GET SERVER SOCKET, IF DEVICE IS CONNECTED AS SERVER,IT CANNOT CALL CLIENTS CODE
    public BluetoothServerSocket getServercommSocket(BluetoothAdapter bluetoothAdapter) {
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code.
            UUID UUID = java.util.UUID.fromString(cust_UUID);
            Log.d("UUID", cust_UUID);
            tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("MADDFANN", UUID);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("getServercommSocket", "Server Socket's getServerconn method failed", e);
            showSnackbar("Failed to get Socket");
        }
        return tmp;
    }


    public class Client_connect extends Thread {
        BluetoothSocket socket;
        boolean fail = false;

        Client_connect(BluetoothSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BA.cancelDiscovery();
                if (socket != null) {
                    socket.connect();
                }
            } catch (IOException e) {
                fail = true;
                e.printStackTrace();
                Log.d("Client_connect", "failed to connect to Server");
                assignButton(manageConnectThread.size() - 1, "no");
                mHandler.obtainMessage(MessageConstants.MESSAGE_FAILEDTO_CONNECT_SERVER).sendToTarget();
                closeSocket(isServer);
            }

            String devicename = "";
            try {
                if (socket != null) {
                    //ADDING CLIENT SOCKET IN LIST,POSITION WILL ALWAYS BE ZERO, AS THERE WILL BE ONLY 1 SOCKET
                    communicating_sockets.add(socket);
                    devicename = !fail ? socket.getRemoteDevice().getName() : "";
                    manageConnectThread.add(new ManageConnectThread(socket, mHandler, 0));
                    manageConnectThread.get(manageConnectThread.size() - 1).start();
                    showSnackbar("Connected to Server");
                    changeUi(true);
                }
            } catch (Exception ignored) {
            }
            mHandler.obtainMessage(MessageConstants.MESSAGE_CONNECTED, fail ? 0 : 1, 0, devicename).sendToTarget();
        }
    }

    public void setPin_textChange()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                for (int i=0;i<pin_nos.size();i++)
                {
                    try
                    {
                        Button button = findViewById(button_ids.get(i));
                        button.setText("Parking Area "+pin_nos.get(i));

                    }catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                }
            }
        });
    }

    private void changeUi(final boolean connected)
    {
        try
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run()
                {
                    if (connected) {
                        clientConnected = true;
                        main_rl_bluetoothinfo.setVisibility(View.GONE);
                        main_conn_tpe_rl.setVisibility(View.GONE);
                        main_paired_device_rl.setVisibility(View.GONE);
                        main_chat_ll.setVisibility(View.GONE);
                    } else {
                        clientConnected = false;
                        main_rl_bluetoothinfo.setVisibility(View.VISIBLE);
                        main_conn_tpe_rl.setVisibility(View.VISIBLE);
                        main_paired_device_rl.setVisibility(View.VISIBLE);
                        main_chat_ll.setVisibility(View.VISIBLE);
                    }
                }
            });
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public class Server_accept extends Thread {
        BluetoothServerSocket socket;
        boolean fail = false;

        Server_accept(BluetoothServerSocket socket) {
            this.socket = socket;
            if (communicating_sockets.size() < 1)
                showSnackbar("Server is Up, waiting for Clients to connect");
            else
                showSnackbar("Server : waiting for more Clients to connect");

        }

        @Override
        public void run()
        {
            BluetoothSocket bluetoothSocket = null;
            try {
                BA.cancelDiscovery();
                if (socket != null)
                {
                    bluetoothSocket = socket.accept();
                }
            } catch (IOException e) {
                fail = true;
                e.printStackTrace();
                Log.d("Server_accept", "server wait timeout");
                mHandler.obtainMessage(MessageConstants.MESSAGE_TIMEOUT).sendToTarget();
                closeSocket(isServer);
            }

            String devicename = "";
            try {
                if (bluetoothSocket != null)
                {
                    // Adding Socket to socketlist,As this is SERVER,MANY CLIENTS CAN CONNECT SO,1 SOCKET ADDDED for each new connection
                    communicating_sockets.add(bluetoothSocket);
                    manageConnectThread.add(new ManageConnectThread(bluetoothSocket, mHandler, communicating_sockets.size() - 1));
                    manageConnectThread.get(manageConnectThread.size() - 1).start();

                    devicename = !fail ? bluetoothSocket.getRemoteDevice().getName() : "";
                    assignButton(manageConnectThread.size() - 1, "no");
                    //Current server thread will die, so call open connection again so that server can listen to new connections.
                    server_open_fr_new_connection(socket);
                    mHandler.obtainMessage(MessageConstants.MESSAGE_CONNECTION_COUNT,manageConnectThread.size()).sendToTarget();
                }
            } catch (Exception ignored) {
            }
            mHandler.obtainMessage(MessageConstants.MESSAGE_CONNECTED, fail ? 0 : 1, 0, devicename).sendToTarget();
        }
    }

    //Creates a new Thread which listens untill a client is connected
    public void server_open_fr_new_connection(BluetoothServerSocket mBTServerSocket) {
        if (mBTServerSocket != null) {
            Server_accept server_accept = new Server_accept(mBTServerSocket);
            server_accept.start();
        }
    }

    public void closeSocket(boolean isServer) {
        try {
            if (isServer) {
                mBTServerSocket.close();
                mBTServerSocket = null;
            } else {
                mBTSocket.close();
                mBTSocket = null;
            }
            mHandler.obtainMessage(MessageConstants.MESSAGE_SOCKET_CLOSED, isServer ? 0 : 1, 0).sendToTarget();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDefault_values() {
        try {
            mBTServerSocket = null;
            mBTSocket = null;
            connectedTo = "";
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeSocket_frm_List(int position) {
        Log.d("in removeSocket", "position : " + position);
        try {
            communicating_sockets.remove(position);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            manageConnectThread.get(position).kill();
            manageConnectThread.remove(position);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mHandler.obtainMessage(MessageConstants.MESSAGE_CONNECTION_COUNT,manageConnectThread.size()).sendToTarget();

    }

    public void assignButton(int position, String message)
    {
        for (int i=0;i<pin_nos.size();i++)
        {
//            String curr_pin=pin_nos.get(i).trim();
//            Log.d("condition1", String.valueOf(message.length()==curr_pin.length()+2));
//            Log.d("condition2", String.valueOf(message.length()==curr_pin.length()+2 && message.contains(curr_pin+",0")));
//            Log.d("condition3", String.valueOf(message.length()==curr_pin.length()+2 && message.contains(curr_pin+",0") || message.contains(curr_pin+",1")));
//            if (message.length()==curr_pin.length()+2 && (message.contains(curr_pin+",0") || message.contains(curr_pin+",1")))
//            {
//                Log.d("msglength", String.valueOf(message.length()));
//                Log.d("pinlength", String.valueOf(curr_pin.length()));
//                try
//                {
//                    Button button = findViewById(button_ids.get(i));
//                    int status = message.contains(curr_pin + ",0") ? 0 : message.contains(curr_pin + ",1") ? 1 : 2;
//                    Log.d("status", String.valueOf(status));
//                    Log.d("pos", String.valueOf(i));
//                    if (status == 0) {
//                        button.setBackgroundColor(getResources().getColor(R.color.colorGreen));
//                        button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    }
//                    if (status == 1) {
//                        button.setBackgroundColor(getResources().getColor(R.color.colorRed));
//                        button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    }
//                }catch (Exception e)
//                {
//                    e.printStackTrace();
//                }
//                break;
//            }

            if (message.contains("2"))
            {
                try
                {
                    Button button = findViewById(button_ids.get(0));
                    String bt=button.getText().toString();
//                    int status = message.contains("2") ? 0 : 1;
//                    Log.d("status", String.valueOf(status));
                    Log.d("pos", String.valueOf(0));
                    button.setBackgroundColor(getResources().getColor(R.color.colorRed));
                    button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    if (status == 0) {
//                        button.setBackgroundColor(getResources().getColor(R.color.colorGreen));
//                        button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    }
//                    if (status == 1) {
//                        button.setBackgroundColor(getResources().getColor(R.color.colorRed));
//                        button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
                break;
            }
            else if (message.contains("3"))
            {
                try
                {
                    Button button = findViewById(button_ids.get(0));
                    String bt=button.getText().toString();
                    button.setBackgroundColor(getResources().getColor(R.color.colorGreen));
                    button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    int status = message.contains("3") ? 0 : 1;
//                    Log.d("status", String.valueOf(status));
//                    Log.d("pos", String.valueOf(1));
//                    if (status == 0) {
//                        button.setBackgroundColor(getResources().getColor(R.color.colorGreen));
//                        button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    }
//                    if (status == 1) {
//                        button.setBackgroundColor(getResources().getColor(R.color.colorRed));
//                        button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
                break;
            }
            if (message.contains("4"))
            {
                try
                {
                    Button button = findViewById(button_ids.get(1));
                    String bt=button.getText().toString();
//                    int status = message.contains("2") ? 0 : 1;
//                    Log.d("status", String.valueOf(status));
                    Log.d("pos", String.valueOf(1));
                    button.setBackgroundColor(getResources().getColor(R.color.colorRed));
                    button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    if (status == 0) {
//                        button.setBackgroundColor(getResources().getColor(R.color.colorGreen));
//                        button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    }
//                    if (status == 1) {
//                        button.setBackgroundColor(getResources().getColor(R.color.colorRed));
//                        button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
                break;
            }
            else if (message.contains("5"))
            {
                try
                {
                    Button button = findViewById(button_ids.get(1));
                    String bt=button.getText().toString();
                    button.setBackgroundColor(getResources().getColor(R.color.colorGreen));
                    button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    int status = message.contains("3") ? 0 : 1;
//                    Log.d("status", String.valueOf(status));
//                    Log.d("pos", String.valueOf(1));
//                    if (status == 0) {
//                        button.setBackgroundColor(getResources().getColor(R.color.colorGreen));
//                        button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    }
//                    if (status == 1) {
//                        button.setBackgroundColor(getResources().getColor(R.color.colorRed));
//                        button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
                break;
            }
            if (message.contains("6"))
            {
                try
                {
                    Button button = findViewById(button_ids.get(2));
                    String bt=button.getText().toString();
//                    int status = message.contains("2") ? 0 : 1;
//                    Log.d("status", String.valueOf(status));
                    Log.d("pos", String.valueOf(2));
                    button.setBackgroundColor(getResources().getColor(R.color.colorRed));
                    button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    if (status == 0) {
//                        button.setBackgroundColor(getResources().getColor(R.color.colorGreen));
//                        button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    }
//                    if (status == 1) {
//                        button.setBackgroundColor(getResources().getColor(R.color.colorRed));
//                        button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
                break;
            }
            else if (message.contains("7"))
            {
                try
                {
                    Button button = findViewById(button_ids.get(2));
                    String bt=button.getText().toString();
                    button.setBackgroundColor(getResources().getColor(R.color.colorGreen));
                    button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    int status = message.contains("3") ? 0 : 1;
//                    Log.d("status", String.valueOf(status));
//                    Log.d("pos", String.valueOf(1));
//                    if (status == 0) {
//                        button.setBackgroundColor(getResources().getColor(R.color.colorGreen));
//                        button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    }
//                    if (status == 1) {
//                        button.setBackgroundColor(getResources().getColor(R.color.colorRed));
//                        button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
                break;
            }
            if (message.contains("8"))
            {
                try
                {
                    Button button = findViewById(button_ids.get(3));
                    String bt=button.getText().toString();
//                    int status = message.contains("2") ? 0 : 1;
//                    Log.d("status", String.valueOf(status));
                    Log.d("pos", String.valueOf(0));
                    button.setBackgroundColor(getResources().getColor(R.color.colorRed));
                    button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    if (status == 0) {
//                        button.setBackgroundColor(getResources().getColor(R.color.colorGreen));
//                        button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    }
//                    if (status == 1) {
//                        button.setBackgroundColor(getResources().getColor(R.color.colorRed));
//                        button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
                break;
            }
            else if (message.contains("9"))
            {
                try
                {
                    Button button = findViewById(button_ids.get(3));
                    String bt=button.getText().toString();
                    button.setBackgroundColor(getResources().getColor(R.color.colorGreen));
                    button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    int status = message.contains("3") ? 0 : 1;
//                    Log.d("status", String.valueOf(status));
//                    Log.d("pos", String.valueOf(1));
//                    if (status == 0) {
//                        button.setBackgroundColor(getResources().getColor(R.color.colorGreen));
//                        button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    }
//                    if (status == 1) {
//                        button.setBackgroundColor(getResources().getColor(R.color.colorRed));
//                        button.setTextColor(getResources().getColor(R.color.colorWhite));
//                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    public void setDefaultButtonColor()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                try {
                    for (int i = 0; i < pin_nos.size(); i++) {
                        Button button = findViewById(button_ids.get(i));
                        button.setBackgroundColor(getResources().getColor(R.color.colorGreen));
                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    public void showSnackbar(String message) {
        Snackbar snackbar = Snackbar
                .make(listView, message, Snackbar.LENGTH_LONG);

        snackbar.show();
    }

    public void showToast(String message) {
        Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
    }

}
