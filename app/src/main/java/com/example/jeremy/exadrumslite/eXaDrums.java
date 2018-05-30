package com.example.jeremy.exadrumslite;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class eXaDrums extends Activity implements View.OnClickListener
{

    //Declaration Button
    Button buttonStart;
    Button buttonQuit;
    ToggleButton toggleMetronome;
    Spinner kitsList;
    SeekBar seekBarTempo;
    TextView textViewBpm;
    String serverIP;

    Gson gson = new Gson();

    NetworkThread networkThread;
    boolean networkThreadRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_e_xa_drums);

        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        int addr = dhcp.serverAddress;
        serverIP = ((addr & 0xFF) + "." + ((addr >>>= 8) & 0xFF) + "." + ((addr >>>= 8) & 0xFF) + "." + ((addr >>>= 8) & 0xFF));


        //Intialization Button

        buttonStart = findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener(eXaDrums.this);

        buttonQuit = findViewById(R.id.buttonQuit);
        buttonQuit.setOnClickListener(eXaDrums.this);

        toggleMetronome = findViewById(R.id.toggleMetronome);
        toggleMetronome.setOnClickListener(eXaDrums.this);

        kitsList = findViewById(R.id.kitsList);

        List<String> arraySpinner = new ArrayList<>();
        arraySpinner.add("test");
        arraySpinner.add("test2");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, arraySpinner);
        kitsList.setAdapter(adapter);

        seekBarTempo = findViewById(R.id.seekBarTempo);
        textViewBpm =  findViewById(R.id.textViewBpm);

        seekBarTempo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @SuppressLint("DefaultLocale")
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b)
            {
                textViewBpm.setText(String.format("%d bpm", i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar)
            {
                //textViewBpm.setText(seekBar.getProgress() + " bpm");
                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        System.out.println("Update Progress " + seekBar.getProgress());
                        JsonQuery jsonQuery = new JsonQuery();
                        jsonQuery.id = 1;
                        jsonQuery.method = "changeTempo";
                        jsonQuery.params = new ArrayList<Integer>();
                        jsonQuery.params.add(seekBar.getProgress());

                        networkThread.SendAndReceive(gson.toJson(jsonQuery));
                    }
                }).start();
            }
        });

        networkThread = new NetworkThread(serverIP);
        //Here MainActivity.this is a Current Class Reference (context)
    }


    @Override
    public void onClick(View view)
    {
        if(!networkThreadRunning)
        {
            networkThread.start();
            networkThreadRunning = true;
        }

        // detect the view that was "clicked"
        switch(view.getId())
        {
            case R.id.buttonStart:
            {

                JsonQuery jsonQuery = new JsonQuery();
                jsonQuery.id = 1;
                jsonQuery.method = "isStarted";
                jsonQuery.params = new ArrayList<Integer>();

                String reply = networkThread.SendAndReceive(gson.toJson(jsonQuery));

                JsonResult<Boolean> jsonResult = gson.fromJson(reply, JsonResult.class);

                if(!jsonResult.result)
                {
                    jsonQuery.method = "start";
                    setText(buttonStart, "Stop");
                }
                else
                {
                    jsonQuery.method = "stop";
                    setText(buttonStart, "Start");
                }

                reply = networkThread.SendAndReceive(gson.toJson(jsonQuery));

                break;
            }
            case R.id.buttonQuit:
            {
                if(networkThreadRunning)
                {

                    JsonQuery jsonQuery = new JsonQuery();
                    jsonQuery.id = 1;
                    jsonQuery.method = "isStarted";
                    jsonQuery.params = new ArrayList<Integer>();

                    String reply = networkThread.SendAndReceive(gson.toJson(jsonQuery));
                    JsonResult<Boolean> jsonResult = gson.fromJson(reply, JsonResult.class);

                    if(jsonResult.result)
                    {
                        jsonQuery.method = "stop";
                        reply = networkThread.SendAndReceive(gson.toJson(jsonQuery));
                    }

                    networkThread.Stop();

                    try
                    {
                        networkThread.join();
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }

                finish();
            }
            case R.id.toggleMetronome:
            {

                JsonQuery jsonQuery = new JsonQuery();
                jsonQuery.id = 1;
                jsonQuery.method = "enableMetronome";
                jsonQuery.params = new ArrayList<Boolean>();
                jsonQuery.params.add(toggleMetronome.isChecked());

                String reply = networkThread.SendAndReceive(gson.toJson(jsonQuery));

                if(toggleMetronome.isChecked())
                {
                    System.out.println("Metronome checked");
                }
                else
                {
                    System.out.println("Metronome unchecked");
                }
            }
        }
    }

    private void setText(final Button b,final String value)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                b.setText(value);
            }
        });
    }


}