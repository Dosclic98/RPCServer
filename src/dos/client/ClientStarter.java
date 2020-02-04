package dos.client;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ClientStarter {
	public static void main(String[] args) {
		try (RPCClient sendReq = new RPCClient()) {
			// TODO generate and send request
		} catch (IOException | TimeoutException | InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e1) {
			System.out.println("Error closing client");
		}
	}
}
