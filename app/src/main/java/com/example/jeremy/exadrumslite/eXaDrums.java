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
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        DhcpInfo dhcp = Objects.requireNonNull(wifi).getDhcpInfo();
        int addr = dhcp.serverAddress;

        if(dhcp.ipAddress != 0)
        {
            serverIP = ((addr & 0xFF) + "." + ((addr >> 8) & 0xFF) + "." + ((addr >> 16) & 0xFF) + "." + ((addr >> 24) & 0xFF));
        }
        else
        {
            serverIP = "0.0.0.0";
        }


        //Intialization Button

        buttonStart = findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener(eXaDrums.this);

        buttonQuit = findViewById(R.id.buttonQuit);
        buttonQuit.setOnClickListener(eXaDrums.this);

        toggleMetronome = findViewById(R.id.toggleMetronome);
        toggleMetronome.setOnClickListener(eXaDrums.this);

        kitsList = findViewById(R.id.kitsList);

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
                        JsonQuery<Integer> jsonQuery = new JsonQuery<>();
                        jsonQuery.method = "changeTempo";
                        jsonQuery.params = new ArrayList<>();
                        jsonQuery.params.add(seekBar.getProgress());

                        networkThread.SendAndReceive(gson.toJson(jsonQuery));
                    }
                }).start();
            }
        });

        networkThread = new NetworkThread(serverIP);
        networkThread.start();
        networkThreadRunning = true;

        JsonQuery jsonQuery = new JsonQuery();
        jsonQuery.method = "getKitsNames";

        String reply = networkThread.SendAndReceive(gson.toJson(jsonQuery));
        JsonResult<List<String>> jsonResult = gson.fromJson(reply, new TypeToken<JsonResult<List<String>>>(){}.getType());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, jsonResult.result);
        kitsList.setAdapter(adapter);

    }


    @Override
    public void onClick(View view)
    {

        // detect the view that was "clicked"
        switch(view.getId())
        {
            case R.id.buttonStart:
            {

                JsonQuery jsonQuery = new JsonQuery();
                jsonQuery.method = "isStarted";

                String reply = networkThread.SendAndReceive(gson.toJson(jsonQuery));

                JsonResult<Boolean> jsonResult = gson.fromJson(reply, new TypeToken<JsonResult<Boolean>>(){}.getType());

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

                networkThread.SendAndReceive(gson.toJson(jsonQuery));

                break;
            }
            case R.id.buttonQuit:
            {
                if(networkThreadRunning)
                {

                    JsonQuery jsonQuery = new JsonQuery();
                    jsonQuery.method = "isStarted";

                    String reply = networkThread.SendAndReceive(gson.toJson(jsonQuery));

                    JsonResult<Boolean> jsonResult = gson.fromJson(reply, new TypeToken<JsonResult<Boolean>>(){}.getType());

                    if(jsonResult.result)
                    {
                        jsonQuery.method = "stop";
                        networkThread.SendAndReceive(gson.toJson(jsonQuery));
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

                JsonQuery<Boolean> jsonQuery = new JsonQuery<>();
                jsonQuery.method = "enableMetronome";
                jsonQuery.params = new ArrayList<>();
                jsonQuery.params.add(toggleMetronome.isChecked());

                networkThread.SendAndReceive(gson.toJson(jsonQuery));

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