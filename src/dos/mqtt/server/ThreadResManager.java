package dos.mqtt.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.json.JSONObject;

import dos.keys.ReqKey;
import dos.keys.ResKey;

public class ThreadResManager extends Thread {
	
	Object lock = null;
	BlockingQueue<JSONObject> clientReq = null;
	JSONObject firstMessage = null;
	private long timeout = 5000;
	private String replyTopicName = null;
	private MqttClient mqttClient = null;
	String clientId = null;
	private static String BROKER_URL = "tcp://localhost:1883";
	private static final String connectionURL = "http://localhost:9000/data";
	private int qos = 2;
	
	public ThreadResManager(Object lock, JSONObject firstMessage) throws MqttException {
		clientReq = new ArrayBlockingQueue<>(2);
		this.lock = lock;
		this.firstMessage = firstMessage;
		replyTopicName = firstMessage.getString(ReqKey.REPLYTO);
		
		clientId = MqttClient.generateClientId();
		mqttClient = new MqttClient(BROKER_URL, clientId);
		MqttConnectOptions option = new MqttConnectOptions();
		option.setCleanSession(false);
		option.setMaxInflight(1000);
		option.setWill(mqttClient.getTopic("dos/problem"), "I'm gone".getBytes(), 0 , false);
		
		mqttClient.connect(option);

	}
	
	public void run() {
		while(true) {
			JSONObject message = null;
			if(firstMessage != null) {
				message = firstMessage;
				firstMessage = null;
				System.out.println("New thread generated: managing first message");
			} else {
				try {
					System.out.println("Waiting for requests");
					message = clientReq.poll(timeout, TimeUnit.MILLISECONDS);
					synchronized(lock) {
						if(message == null) {
							MqttRPCServer.activeThreads.remove(replyTopicName);
							System.out.println("Timeout expired, stopping thread managing: " + replyTopicName);
						} else {
							System.out.println("New request arrived: sending request");
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if(message == null) {
				try {
					this.terminate();
				} catch (MqttException e) {
					e.printStackTrace();
				}
				break;
			}
			
			try {
				String response = sendRequest(message);
				sendResp(response);
			} catch (IOException | MqttException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	private static String sendRequest(JSONObject msgJson) throws IOException {
		
		int responseCode = 0;
		String responseBody = "";
		JSONObject response = null;
		
		URL url = null;
		HttpURLConnection conn = null;
		if(msgJson.getString(ReqKey.METHOD).equals("GET")) {
			String query = URLEncoder.encode(msgJson.getString(ReqKey.QUERY), StandardCharsets.UTF_8.toString()).replace("\n", "+");
			String urlString = connectionURL + "?" +  query;
			url = new URL(urlString);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", msgJson.getString(ReqKey.ACCEPT));
			
			conn.setRequestProperty("Accept-Charset", "UTF-8");
			
			responseCode = conn.getResponseCode();
			responseBody = getResponseBody(conn);
			String responseMessage = conn.getResponseMessage();
			response = new JSONObject();
			response.put(ResKey.CODE, responseCode);
			response.put(ResKey.BODY, responseBody);
			response.put(ResKey.MSG, responseMessage);
			response.put(ReqKey.CORRID, msgJson.getString(ReqKey.CORRID));
			
		} else if(msgJson.getString(ReqKey.METHOD).equals("POST") || 
				  msgJson.getString(ReqKey.METHOD).equals("PUT") ||
				  msgJson.getString(ReqKey.METHOD).equals("DELETE")) {
			String urlString = connectionURL;
			url = new URL(urlString);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod(msgJson.getString(ReqKey.METHOD));
			conn.setRequestProperty("Accept", msgJson.getString(ReqKey.ACCEPT));
			
			conn.setRequestProperty("Accept-Charset", "UTF-8");
			conn.setDoOutput(true);
			
			try(OutputStream os = conn.getOutputStream()) {
				byte[] input = msgJson.getString(ReqKey.BODY).getBytes("utf-8");
				os.write(input, 0, input.length);           
			}
			
			responseCode = conn.getResponseCode();
			responseBody = getResponseBody(conn);
			String responseMessage = conn.getResponseMessage();
			response = new JSONObject();
			response.put(ResKey.CODE, responseCode);
			response.put(ResKey.BODY, responseBody);
			response.put(ResKey.MSG, responseMessage);
			response.put(ReqKey.CORRID, msgJson.getString(ReqKey.CORRID));
		} 
		
		return response.toString();
	}

	private static String getResponseBody(HttpURLConnection conn) {
		try {
			BufferedReader in = null;
			if(conn.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
				in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			} else in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();			
			
			return response.toString();
		} catch(IOException e) {
			return "Error retriving body";
		}
	}
	
	private void sendResp(String message) throws MqttException {
		MqttTopic reqTopic = mqttClient.getTopic(replyTopicName);
		MqttMessage msgToSend = new MqttMessage(message.getBytes());
		msgToSend.setQos(qos);
		reqTopic.publish(msgToSend);
	}
	
	public void terminate() throws MqttException {
		if(mqttClient.isConnected()) {
			mqttClient.disconnect();				
		}
		mqttClient.close();
		System.out.println(mqttClient.getClientId() + " disconnected");
	}
}
