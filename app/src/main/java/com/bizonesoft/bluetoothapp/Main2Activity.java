package com.bizonesoft.bluetoothapp;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Main2Activity extends AppCompatActivity
{
    TextView read;
    EditText write;
    Button submit;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        read=findViewById(R.id.read);
        write=findViewById(R.id.write);
        submit=findViewById(R.id.submit);

        handler=new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message)
            {
                if (message.what==MessageConstants.MESSAGE_READ)
                {
                    read.setText(message.getData().getString("msg"));
                }
                else if (message.what==MessageConstants.MESSAGE_WRITE)
                {
                    read.setText(read.getText().toString()+message.getData().getString("message"));
                }

                if (message.what==3)
                {
                    read.setText(message.obj.toString());
                }

                return true;
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                new DisplayThread(write.getText().toString(),handler).run();
                new DisplayThread("",handler).run();
            }
        });

    }



    public class DisplayThread extends Thread
    {
        int n=0;
        String w="";
        private Handler mHandler;
        DisplayThread(int n, Handler handler)
        {
            this.n=n;
            mHandler=handler;
        }
        DisplayThread(String w, Handler handler)
        {
            this.w=w;
            mHandler=handler;
        }


        public void run()
        {
            if (n>0) {
                Message readMsg = mHandler.obtainMessage();
                Bundle data=new Bundle();
                data.putString("msg","hi");
                readMsg.setData(data);
                mHandler.sendMessage(readMsg);
            }else if (w.length()>0)
            {
                Message readMsg = mHandler.obtainMessage();
                Bundle data=new Bundle();
                data.putString("message",",hmm\nhello");
                readMsg.setData(data);
                mHandler.sendMessage(readMsg);
            }
            else {
                Message readMsg = mHandler.obtainMessage(
                        3,1, -1,
                        "yes");
                readMsg.sendToTarget();
            }
        }
    }


}
