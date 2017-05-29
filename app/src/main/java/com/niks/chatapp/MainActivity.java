package com.niks.chatapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.niks.baseutils.CustomLogger;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    static final int SocketServerPORT = 8080;
//    TextView infoIp;


    List<ConnectThread> userList;

    ServerSocket serverSocket;
    private String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onStart() {
        super.onStart();
        CustomLogger.enable_logs = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        infoIp = (TextView) findViewById(R.id.infoip);

//        infoIp.setText(getIpAddress());

        userList = new ArrayList<>();

        ChatServerThread chatServerThread = new ChatServerThread();
        chatServerThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        closeServerSocket();

    }

    private void closeServerSocket() {
        CustomLogger.Log(TAG, "Closed server socket");
        try {
            if (serverSocket != null)
                serverSocket.close();
            if (userList != null)
                userList.clear();
        } catch (Exception e) {
        }
    }

    private class ChatServerThread extends Thread {

        @Override
        public void run() {
            Socket socket = null;

            try {
                serverSocket = new ServerSocket(SocketServerPORT);
                CustomLogger.Log(TAG, "Server Listening At : " + getIpAddress() + ":" + serverSocket.getLocalPort());

                while (true) {
                    socket = serverSocket.accept();
                    closeAllPreviousConnectionsBySocket(socket);
                    ConnectThread connectThread = new ConnectThread(socket);
                    connectThread.start();
                    userList.add(connectThread);
                }

            } catch (Exception e) {
            } finally {
                try {
                    closeServerSocket();
                } catch (Exception e) {
                }
            }

        }

    }

    private void closeAllPreviousConnectionsBySocket(Socket socket) {
        if (userList != null) {
            for (int i = 0; i < userList.size(); i++) {
                ConnectThread connectThread = userList.get(i);
                if (connectThread.socket.getInetAddress().equals(socket.getInetAddress())) {
                    connectThread.disconnect();
                    break;
                }
            }
        }
    }

    private class ConnectThread extends Thread {
        private DataInputStream dataInputStream = null;
        private DataOutputStream dataOutputStream = null;
        private Socket socket;

        ConnectThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                {
                    String message = "User Joined" + userList.size() + socket.getInetAddress() + ":" + socket.getPort() + " IP joined";
                    CustomLogger.Log(TAG, message);
                }

                while (true) {
                    if (dataInputStream.available() > 0) {
                        String user_command = dataInputStream.readUTF();
                        CustomLogger.Log(TAG, "Command Received From" + socket.getInetAddress() + " : " + user_command);
                        String reply_command = checkForCommand(user_command);

                        if (!TextUtils.isEmpty(reply_command)) {
                            dataOutputStream.writeUTF(reply_command);
                            dataOutputStream.flush();
                        }
                    }
                }
            } catch (Exception e) {
            } finally {
                disconnect();
            }
        }

        private void disconnect() {
            try {
                //If session is closed then
                if (dataInputStream != null)
                    dataInputStream.close();
                if (dataOutputStream != null)
                    dataOutputStream.close();
                userList.remove(this);
                CustomLogger.Log(TAG, "User Leaved : " + socket.getInetAddress());
            } catch (Exception e) {
            }
        }


    }

    private String checkForCommand(String user_command) {

        try {
            JSONObject jsonObject = new JSONObject(user_command);

            if (jsonObject.has("command")) {
                String command = jsonObject.getString("command");
                switch (command) {
                    case "login":
                        return handLoginRequest(jsonObject);

                    default:
                        return sendCommandNotFoundResponse();
                }
            } else {
                return sendCommandNotFoundResponse();
            }

        } catch (Exception e) {
            return sendFailedToProcessResponse();
        }
    }

    private String handLoginRequest(JSONObject jsonObject) {
        try {
            JSONObject user_detailsJsonObject = jsonObject.getJSONObject("user_details");
            String user_name = user_detailsJsonObject.getString("user_name");
            String password = user_detailsJsonObject.getString("password");
            if (user_name.equals("nik") && password.equals("nik")) {
                return "{\n" +
                        "  \"error\": 0,\n" +
                        "  \"command\": \"login\",\n" +
                        "  \"message\": \"successful\",\n" +
                        "  \"user_details\": {\n" +
                        "    \"user_name\": \"nik\",\n" +
                        "    \"password\": \"nik\"\n" +
                        "  }\n" +
                        "}";
            }
        } catch (Exception e) {

        }
        return sendFailedToProcessResponse();
    }

    private String sendCommandNotFoundResponse() {
        return "{\n" +
                "  \"error\": 1,\n" +
                "  \"message\": \"Command Not Found\"\n" +
                "}";
    }

    private String sendFailedToProcessResponse() {
        return "{\n" +
                "  \"error\": 1,\n" +
                "  \"message\": \"Failed to process request\"\n" +
                "}";
    }

//    private void broadcastMsg(String msg) {
//        for (int i = 0; i < userList.size(); i++) {
//            //Sending message to appropriate user
//            userList.get(i).chatThread.sendMsg(msg);
//            msgLog += "- send to " + userList.get(i).name + "\n";
//        }
//
//        MainActivity.this.runOnUiThread(new Runnable() {
//
//            @Override
//            public void run() {
//                chatMsg.setText(msgLog);
//            }
//        });
//    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                    }
                }
            }
        } catch (Exception e) {
            ip += "Something Wrong! " + e.toString() + "\n";
        }
        return ip;
    }


}
