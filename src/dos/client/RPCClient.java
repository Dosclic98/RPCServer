package dos.client;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

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
	
	public String call(String message) throws IOException, InterruptedException {
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
	
	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub
		
	}

}
