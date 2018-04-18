package com.bizonesoft.bluetoothapp;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by sagar on 2/3/18.
 */

public class ManageConnectThread extends Thread
{
    private final String TAG="ManageConnectThread";
    private Handler mHandler;
    private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private byte[] mmBuffer; // mmBuffer store for the stream
    private int position_in_list;

    ManageConnectThread(){}

    ManageConnectThread(BluetoothSocket socket, Handler handler,int position_in_list)
    {
        mHandler=handler;
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        this.position_in_list=position_in_list;
        // Get the input and output streams; using temp objects because
        // member streams are final.
        try {
            tmpIn = socket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating input stream", e);
        }
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {
        mmBuffer = new byte[1024];
        int numBytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs.
        while (true) {
            try {
                // Read from the InputStream.
                numBytes = mmInStream.read(mmBuffer);
                final String string=new String(mmBuffer,"UTF-8");
                Log.d("bluetoothresponse", string);
                // Send the obtained bytes to the UI activity.
                mHandler.obtainMessage(MessageConstants.MESSAGE_READ, numBytes, position_in_list,string).sendToTarget();
            } catch (Exception e)
            {
                Log.d(TAG, "Input stream was disconnected", e);
                mHandler.obtainMessage(MessageConstants.MESSAGE_DEVICE_DISCONNECTED,0,position_in_list).sendToTarget();
                cancel();
                break;
            }
        }
    }

    void kill()
    {
        currentThread().interrupt();
    }

    // Call this from the main activity to send data to the remote device.
    void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);

            // Share the sent message with the UI activity.
            mHandler.obtainMessage(MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer).sendToTarget();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);

            // Send a failure message back to the activity.
            mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST).sendToTarget();
        }
    }
    // Call this method from the main activity to shut down the connection.
    private void cancel() {
        try {
            mmSocket.close();
            mHandler.obtainMessage(MessageConstants.MESSAGE_SOCKET_CLOSED,2,position_in_list).sendToTarget();
        } catch (IOException e)
        {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }
}
