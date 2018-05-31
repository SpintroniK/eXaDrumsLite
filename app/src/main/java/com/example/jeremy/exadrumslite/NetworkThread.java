package com.example.jeremy.exadrumslite;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;



public class NetworkThread extends Thread
{
    private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    private Lock responseLock = new ReentrantLock();
    private Condition responseCondition = responseLock.newCondition();

    private String commandMsg;
    private String serverIP;

    private AtomicBoolean isRunning = new AtomicBoolean(true);
    private int isWait = 1;
    private boolean waitForResponse = true;
    private String reply = "";

    NetworkThread(String ipAddress)
    {
        serverIP = ipAddress;
    }

    String SendAndReceive(String s)
    {
        try
        {
            lock.lock();
            isWait = 0;
            commandMsg = s;
            condition.signalAll();
        }
        finally
        {
            lock.unlock();
        }

        return WaitForResponse();
    }


    void Stop()
    {
        try
        {
            lock.lock();
            isRunning.set(false);
            isWait = 0;
            condition.signalAll();
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public void run()
    {

        String serverName = (serverIP.equals("0.0.0.0")) ? "10.0.2.2" : serverIP; //192.168.0.42";
        int port = 8080;

        try
        {

            System.out.println("Connecting to " + serverName + " on port " + port);

            Socket client = new Socket(serverName, port);
            System.out.println("Just connected to " + client.getRemoteSocketAddress());


            while(isRunning.get())
            {

                try
                {
                    lock.lock();
                    while(isWait == 1) condition.await();
                    isWait = 1;
                }
                finally
                {
                    lock.unlock();
                }


                String message = commandMsg;
                if(!isRunning.get())
                {
                    break;
                }

                System.out.println("Send " + message);

                SocketSend(client, message);
                String response = SocketReceive(client);

                System.out.println("Received " + response);


                try
                {
                    responseLock.lock();
                    this.reply = response;
                    waitForResponse = false;
                    responseCondition.signalAll();
                }
                finally
                {
                    responseLock.unlock();
                }

                System.out.println(reply);

            }

            client.close();
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private String SocketReceive(Socket socket) throws IOException
    {
        InputStreamReader in = new InputStreamReader(socket.getInputStream());
        BufferedReader br = new BufferedReader(in);

        return br.readLine();
    }

    private void SocketSend(Socket socket, String message) throws IOException
    {
        BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
        bos.write(message.getBytes());
        bos.flush();
    }

    private String WaitForResponse()
    {

        String response = "";

        try
        {
            responseLock.lock();
            while(waitForResponse) responseCondition.await();
            response = this.reply;
            waitForResponse = true;
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        finally
        {
            responseLock.unlock();
        }

        return response;
    }
}
