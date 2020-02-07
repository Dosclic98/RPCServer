package dos.mqtt.server;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import dos.keys.ReqKey;

public class ServerReqCallback implements MqttCallback{
	
	private Object lock = null;
	
	public ServerReqCallback(Object lock) {
		this.lock = lock;
	}

	@Override
	public void connectionLost(Throwable arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
		String strMsg = mqttMessage.toString(); 
		System.out.println("Request recieved: " + strMsg);
		JSONObject jsonMsg = new JSONObject(strMsg);
		String replyTo = jsonMsg.getString(ReqKey.REPLYTO);
		synchronized(lock) {
			ThreadResManager thManager;
			if(MqttRPCServer.activeThreads.containsKey(replyTo)) {
				System.out.println("Passing request to already present thread to manage: " + replyTo);
				thManager = MqttRPCServer.activeThreads.get(replyTo);
				thManager.clientReq.offer(jsonMsg);
			} else {
				System.out.println("Generating new thread to manage: " + replyTo);
				thManager = new ThreadResManager(lock, jsonMsg);
				MqttRPCServer.activeThreads.put(replyTo, thManager);
				thManager.start();
			}
		}
		
	}

}
