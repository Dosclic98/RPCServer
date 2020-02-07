package dos.mqtt.server;

import java.util.HashMap;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MqttRPCServer {
	
	private static String BROKER_URL = "tcp://localhost:1883";
	private static String requestTopicName = "request";
	public static int qos = 2;
	public static String serverId = null; 
	private static MqttClient mqttClient;
	public static Object lock = null;
	public static HashMap<String, ThreadResManager> activeThreads = null;

	
	public static void main(String args[]) throws MqttException {
		activeThreads = new HashMap<String, ThreadResManager>();
		
		lock = new Object();
		serverId = MqttClient.generateClientId();
		mqttClient = new MqttClient(BROKER_URL, serverId);
		mqttClient.setCallback(new ServerReqCallback(lock));
		mqttClient.connect();
		mqttClient.subscribe(requestTopicName, qos);
		System.out.println("Waiting for requests...");
		
	}
}
