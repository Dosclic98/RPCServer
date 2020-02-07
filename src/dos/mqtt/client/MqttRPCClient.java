package dos.mqtt.client;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.json.JSONObject;

import dos.keys.ReqKey;

public class MqttRPCClient {
	
	private static String BROKER_URL = "tcp://localhost:1883";
	private static String requestTopicName = "request";
	private String accept = "application/studenti+";
	public static int qos = 2;
	
	public String corrId = null;
	private String responseTopicName = null;
	private String clientId;
	private MqttClient mqttClient;
	public BlockingQueue<String> response = null;
	
	public MqttRPCClient() {
		try {
			response = new ArrayBlockingQueue<>(1);
			
			clientId = MqttClient.generateClientId();
			responseTopicName = clientId;
			mqttClient = new MqttClient(BROKER_URL, clientId);
			MqttConnectOptions option = new MqttConnectOptions();
			option.setCleanSession(false);
			option.setMaxInflight(1000);
			option.setWill(mqttClient.getTopic("dos/problem"), "I'm gone".getBytes(), 0 , false);

			mqttClient.setCallback(new ClientRespCallback(this));
			mqttClient.connect(option);
			mqttClient.subscribe(responseTopicName, qos);
			
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public String makeGet(String acceptField, String query) throws IOException, InterruptedException, MqttException {
		return genRequest("GET", acceptField, query);
	}

	public String makePost(String acceptField, String body) throws IOException, InterruptedException, MqttException {
		return genRequest("POST", acceptField, body);
	}

	public String makePut(String acceptField, String body) throws IOException, InterruptedException, MqttException {
		return genRequest("PUT", acceptField, body);
	}

	public String makeDelete(String acceptField, String body) throws IOException, InterruptedException, MqttException {
		return genRequest("DELETE", acceptField, body);
	}
	
	private String genRequest(String method, String acceptField, String content) throws IOException, InterruptedException, MqttException {
		acceptField = accept + acceptField;
		corrId = UUID.randomUUID().toString();
		JSONObject rqstMsgJson = new JSONObject();
		rqstMsgJson = rqstMsgJson.put(ReqKey.REPLYTO, responseTopicName); 
		rqstMsgJson = rqstMsgJson.put(ReqKey.METHOD, method);
		rqstMsgJson = rqstMsgJson.put(ReqKey.ACCEPT, acceptField);
		rqstMsgJson = rqstMsgJson.put(ReqKey.CORRID, corrId);
		if(method.equals("GET")) {
			rqstMsgJson = rqstMsgJson.put(ReqKey.QUERY, content);
		} else rqstMsgJson = rqstMsgJson.put(ReqKey.BODY, content);
		System.out.println("Requesting: " + rqstMsgJson.toString());
		
		sendReq(rqstMsgJson.toString());
		
		String result = response.take();
		return result;
		
	}
	
	private void sendReq(String message) throws MqttException {
		MqttTopic reqTopic = mqttClient.getTopic(requestTopicName);
		MqttMessage msgToSend = new MqttMessage(message.getBytes());
		msgToSend.setQos(2);
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
