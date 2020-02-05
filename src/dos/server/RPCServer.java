package dos.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;

import com.rabbitmq.client.*;

import dos.keys.ReqKey;

public class RPCServer {
	
	private static final String connectionURL = "http://localhost:9000/data";
	private static final String RPC_QUEUE_NAME = "rqstQueue";
	
	public static void main(String args[]) {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		
		try (Connection connection = factory.newConnection();
				Channel channel = connection.createChannel()) {
			channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
			channel.queuePurge(RPC_QUEUE_NAME);
			
			channel.basicQos(1);
			System.out.println("Awaiting RPC requests...");
			
			Object monitor = new Object();
			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				AMQP.BasicProperties replyProps = new AMQP.BasicProperties
						.Builder()
						.correlationId(delivery.getProperties().getCorrelationId())
						.build();
				
				String response = "";
				try {
					String message = new String(delivery.getBody(), "UTF-8");
					System.out.println("Message received: " + message);
					
					JSONObject msgJson = new JSONObject(message);
					
					response = sendRequest(msgJson);
				} catch(RuntimeException e) {
					e.printStackTrace();
				} finally {
					channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes("UTF-8"));
					channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
					
					synchronized (monitor) {
						monitor.notify();
					}
				}
			};
			
			channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> {}));
			
			while(true) {
				synchronized (monitor) {
					try {
						monitor.wait();
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}

	private static String sendRequest(JSONObject msgJson) throws IOException {
		
		int responseCode = 0;
		String responseBody = "";
		JSONObject response = null;
		
		URL url = null;
		HttpURLConnection conn = null;
		if(msgJson.getString(ReqKey.METHOD).equals("GET")) {
			System.out.println("GET Received");
			String query = msgJson.getString(ReqKey.QUERY);
			String urlString = connectionURL + "?" +  query;
			url = new URL(urlString);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", msgJson.getString(ReqKey.ACCEPT));
			//conn.setDoInput(true);
			//conn.setDoOutput(false);
			// conn.setRequestProperty("Accept-Charset", "UTF-8");
			// conn.connect();
			
			responseCode = conn.getResponseCode();
			responseBody = getResponseBody(conn);
			String responseMessage = conn.getResponseMessage();
			response = new JSONObject();
			response.put("resCode", responseCode);
			response.put("resBody", responseBody);
			response.put("resMsg", responseMessage);
			
		} else if(msgJson.getString(ReqKey.METHOD).equals("POST")) {
			
		} else if(msgJson.getString(ReqKey.METHOD).equals("PUT")) {
			
		} else if(msgJson.getString(ReqKey.METHOD).equals("DELETE")) {
			
		}
		
		return response.toString();
	}

	private static String getResponseBody(HttpURLConnection conn) {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();			
			
			return response.toString();
		} catch(IOException e) {
			return "No body";
		}
	}
	
}
