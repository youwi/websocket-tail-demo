package com.xxg.websocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ResourceBundle;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/log")
public class LogWebSocketHandle {
	
	private Process process;
	private InputStream inputStream;
	private static ResourceBundle rb = ResourceBundle.getBundle("config");
	/**
	 * 新的WebSocket请求开启
	 */
	@OnOpen
	public void onOpen(final Session session) {
		try {
			// 执行tail -f命令

			process = Runtime.getRuntime().exec("tail -f  "+rb.getString("logfile"));
			inputStream = process.getInputStream();
			
			// 一定要启动新的线程，防止InputStream阻塞处理WebSocket的线程
			new  Thread(){

					BufferedReader reader= new BufferedReader(new InputStreamReader(inputStream));
					@Override
					public void run() {
					String line;
					try {
						while((line = reader.readLine()) != null) {
							// 将实时日志通过WebSocket发送给客户端，给每一行添加一个HTML换行
							if(!line.contains(rb.getString("ignore")))
							    session.getBasicRemote().sendText(line + "<br>");
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}.start();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * WebSocket请求关闭
	 */
	@OnClose
	public void onClose() {
		try {
			if(inputStream != null)
				inputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(process != null)
			process.destroy();
	}
	
	@OnError
	public void onError(Throwable thr) {
		thr.printStackTrace();
	}
}