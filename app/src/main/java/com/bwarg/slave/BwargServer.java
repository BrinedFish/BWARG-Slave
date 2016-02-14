package com.bwarg.slave;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by LM on 13.02.2016.
 */
public class BwargServer extends AsyncTask<Context, String, String>{
    private final static String TAG = "BwargServer";
    private DatagramSocket socket;
    private String device_name = "";

    public BwargServer(String device_name){
        this.device_name = device_name;
    }
    @Override
    protected String doInBackground(Context... contexts) {
        try {
            Context context = contexts[0];


            //Keep a socket open to listen to all the UDP trafic that is destined for this port
            socket = new DatagramSocket(context.getResources().getInteger(R.integer.port), InetAddress.getByName("0.0.0.0"));
            try {
                socket.setBroadcast(true);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            while (socket != null && !socket.isClosed()) {
                Log.d(TAG, "Ready to receive broadcast packets on : " + InetAddress.getByName("0.0.0.0")+":"+ context.getResources().getInteger(R.integer.port));

                //Receive a packet
                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);

                //Packet received
                Log.d(TAG, "Discovery packet received from: " + packet.getAddress().getHostAddress());
                Log.d(TAG, "Packet received; data: " + new String(packet.getData()));

                //See if the packet holds the right command (message)
                String message = new String(packet.getData()).trim();
                if(message != null) {
                    boolean message_sent = false;
                    String response = "null";
                    String dest_ip = "null";
                    if (message.equals(context.getResources().getString(R.string.discover_request))) {
                        response = context.getResources().getString(R.string.discover_response);
                        byte[] sendData = response.getBytes();

                        //Send a response
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                        socket.send(sendPacket);
                        message_sent = true;
                        dest_ip =  sendPacket.getAddress().getHostAddress();

                    }else if(message.equals(context.getResources().getString(R.string.name_request))){
                        response = context.getResources().getString(R.string.name_response)+device_name;
                        byte[] sendData = response.getBytes();

                        //Send a response
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                        socket.send(sendPacket);
                        message_sent = true;
                        dest_ip =  sendPacket.getAddress().getHostAddress();
                    }else if(message.equals(context.getResources().getString(R.string.port_request))){
                        response = context.getResources().getString(R.string.port_response)+StreamCameraActivity.mPort;
                        byte[] sendData = response.getBytes();

                        //Send a response
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                        socket.send(sendPacket);
                        message_sent = true;
                        dest_ip =  sendPacket.getAddress().getHostAddress();
                    }
                    if(message_sent){
                        Log.d(TAG, "Sent packet \""+response+"\" to " + dest_ip);
                        message_sent = false;
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (NullPointerException nex){
            nex.printStackTrace();
        }
        return "ended";
    }
    @Override
    protected void onPostExecute(String result){
        socket.close();
    }
}
