package dos.mqtt.client;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClientMqttTester {
	
	MqttRPCClient sendReq = null;
	
	@Before
	public void init() throws IOException, TimeoutException {
		sendReq = new MqttRPCClient();
	}
	@Test
	public void test() throws IOException, InterruptedException, MqttException {
		String response = sendReq.makeGet("json", "{\"matricola\":\"20023867\"}");
		System.out.println("Response: " + response);
		
		response = sendReq.makeGet("json", "\"matricola\":\"20023867\"}");
		System.out.println("Response: " + response);
		
		response = sendReq.makePost("json", "{\n" + 
				"\"matricola\":\"20023867\",\n" + 
				"\"nome\":\"DavidHerzLolxG\",\n" + 
				"\"cognome\":\"Sovano\",\n" + 
				"\"nascita\":\"06-12-1999\",\n" + 
				"\"CDL\":\"Informatica\",\n" + 
				"\"anno\":3\n" + 
				"}");
		System.out.println("Response: " + response);

		response = sendReq.makePost("xml", "<studente>\n" + 
				"	<matricola>200w23866</matricola>\n" + 
				"	<nome>DavideHerl</nome>\n" + 
				"	<cognome>Sovanox</cognome>\n" + 
				"	<anno>1</anno>\n" + 
				"	<nascita>16-12-1997</nascita>\n" + 
				"	<CDL>Informatica</CDL>\n" + 
				"</studente>\n" + 
				"");
		System.out.println("Response: " + response);

		response = sendReq.makePost("xml", "<studente>\n" + 
				"	<matricola>20023866</matricola>\n" + 
				"	<nome>DavideHerl</nome>\n" + 
				"	<cognome>Sovanox</cognome>\n" + 
				"	<anno>1</anno>\n" + 
				"	<nascita>16-12-1997</nascita>\n" + 
				"	<CDL>Informatica</CDL>\n" + 
				"</studente>\n" + 
				"");
		System.out.println("Response: " + response);

		response = sendReq.makePut("json", "{\"id\":2, \"matricola\": \"20023866\", \"nome\": \"Doclang\", \"nascita\":\"01-04-2000\"}");
		System.out.println(response);

		response = sendReq.makePut("xml", "<studente><id>2</id><matricola>20023868</matricola><CDL>Biologia</CDL></studente>");
		System.out.println(response);
		
		response = sendReq.makeDelete("json", "{\"id\":5}");
		System.out.println(response);
		
		response = sendReq.makeDelete("xml", "<studente><id>4</id></studente>");
		System.out.println(response);
	}
	
	@After
	public void disc() throws MqttException {
		sendReq.terminate();
	}

}
