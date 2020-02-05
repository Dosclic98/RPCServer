package dos.client;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import dos.keys.ReqKey;

public class RPCClient implements AutoCloseable {
	
	private Connection connection;
	private Channel channel;
	private String requestQueueName = "rqstQueue";
	
	public RPCClient() throws IOException, TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		
		connection = factory.newConnection();
		channel = connection.createChannel();
	}
	
	private String call(String message) throws IOException, InterruptedException {
		final String corrId = UUID.randomUUID().toString();
		String replyQueueName = channel.queueDeclare().getQueue();
		
		AMQP.BasicProperties props = new AMQP.BasicProperties
				.Builder()
				.correlationId(corrId)
				.replyTo(replyQueueName)
				.build();
		
		channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));
		
		final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);
		String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
			if(delivery.getProperties().getCorrelationId().equals(corrId)) {
				response.offer(new String(delivery.getBody(), "UTF-8"));
			}
		}, consumerTag -> {
		});
		
		String result = response.take();
		channel.basicCancel(ctag);
		return result;
	}
	
	public String makeGet(String acceptField, String query) throws IOException, InterruptedException {
		JSONObject rqstMsgJson = new JSONObject();
		rqstMsgJson = rqstMsgJson.put(ReqKey.METHOD, "GET");
		rqstMsgJson = rqstMsgJson.put(ReqKey.ACCEPT, acceptField);
		rqstMsgJson = rqstMsgJson.put(ReqKey.QUERY, query);
		System.out.println("Requesting: " + rqstMsgJson.toString());
		
		return this.call(rqstMsgJson.toString());
	}

	public String makePost(String acceptField, String body) throws IOException, InterruptedException {
		JSONObject rqstMsgJson = new JSONObject();
		rqstMsgJson = rqstMsgJson.put(ReqKey.METHOD, "POST");
		rqstMsgJson = rqstMsgJson.put(ReqKey.ACCEPT, acceptField);
		rqstMsgJson = rqstMsgJson.put(ReqKey.BODY, body);
		System.out.println("Requesting: " + rqstMsgJson.toString());
		
		return this.call(rqstMsgJson.toString());
	}
/*
	public String makePut() {
		
	}

	public String makeDelete() {
		
	}
*/
	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub
		
	}

}
