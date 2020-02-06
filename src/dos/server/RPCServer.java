package dos.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;

import com.rabbitmq.client.*;

import dos.keys.ReqKey;
import dos.keys.ResKey;

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
			
		} else if(msgJson.getString(ReqKey.METHOD).equals("POST") || 
				  msgJson.getString(ReqKey.METHOD).equals("PUT") ||
				  msgJson.getString(ReqKey.METHOD).equals("DELETE")) {
			System.out.println(msgJson.getString(ReqKey.METHOD) + " Received");
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
	
}
