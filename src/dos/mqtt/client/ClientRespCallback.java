package dos.mqtt.client;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import dos.keys.ReqKey;

public class ClientRespCallback implements MqttCallback {
	
	MqttRPCClient callerClient = null;
	
	public ClientRespCallback(MqttRPCClient caller) {
		callerClient = caller;
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
		JSONObject jsonMsg = new JSONObject(strMsg);
		
		if(jsonMsg.getString(ReqKey.CORRID).equals(callerClient.corrId)) {
			jsonMsg.remove(ReqKey.CORRID);
			callerClient.response.offer(jsonMsg.toString());
		}
		
	}

}
