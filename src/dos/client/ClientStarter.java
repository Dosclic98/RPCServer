package dos.client;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ClientStarter {
	public static void main(String[] args) {
		try (RPCClient sendReq = new RPCClient()) {
			String response = sendReq.makeGet("application/studenti+json", "http://localhost:9000/data?{\"id\":3}");
			System.out.println("Response: " + response);
		} catch (IOException | TimeoutException | InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e1) {
			System.out.println("Error closing client");
		}
	}
}
