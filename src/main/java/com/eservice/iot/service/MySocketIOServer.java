package com.eservice.iot.service;


import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Class Description:
 *
 *  netty-socketio 实现的 socke.io 服务端
 *
 * @author Wilson Hu
 * @date 8/12/2018
 */
@Component
public class MySocketIOServer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String SOCKET_SERVER = "192.168.0.111";

    private int SOCKET_SERVER_PORT = 4000;

    private static SocketIOServer server;

    private HashMap<UUID,SocketIOClient> mSocketClientMap;

    public MySocketIOServer() {
        server = initServer();
        mSocketClientMap = new HashMap<>();
    }
    /**
     * 初始化服务端
     * @return
     */
    private SocketIOServer initServer() {
        Configuration config = new Configuration();
        config.setHostname(SOCKET_SERVER);
        config.setPort(SOCKET_SERVER_PORT);
        server = new SocketIOServer(config);
        return server;
    }

    /**
     * 启动服务端
     */
    public void startServer() {

        // 添加连接监听
        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient socketIOClient) {
                logger.info("Connect Client UUID:  " + socketIOClient.getSessionId());
                mSocketClientMap.put(socketIOClient.getSessionId(), socketIOClient);
            }
        });

        // 添加断开连接监听
        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient socketIOClient) {
                logger.info("Disconnect Client UUID: " + socketIOClient.getSessionId());
                mSocketClientMap.remove(socketIOClient.getSessionId());
            }
        });

        // 添加事件监听
        server.addEventListener("join", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient socketIOClient, String str, AckRequest ackRequest) throws Exception {
                logger.info("收到客户端加入消息：" + str);
                server.getBroadcastOperations().sendEvent("joinSuccess", "join success");
                socketIOClient.sendEvent("");
            }
        });
        // 启动服务端
        server.start();
    }

    public void broadcastMsgToClients(String data) {

    }

    /**
     * 停止服务端
     */
    public void stopServer() {
        server.stop();
    }
}
