package dos.client;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ClientStarter {
	public static void main(String[] args) {
		try (RPCClient sendReq = new RPCClient()) {
			// String response = sendReq.makeGet("application/studenti+json", "{\"matricola\":\"20023869\"}");
			String response = sendReq.makePost("application/studenti+json", "{\n" + 
					"\"matricola\":\"20023867\",\n" + 
					"\"nome\":\"DavidHerzLolxG\",\n" + 
					"\"cognome\":\"Sovano\",\n" + 
					"\"nascita\":\"06-12-1999\",\n" + 
					"\"CDL\":\"Informatica\",\n" + 
					"\"anno\":3\n" + 
					"}");
			System.out.println("Response: " + response);

		} catch (IOException | TimeoutException | InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e1) {
			System.out.println("Error closing client");
		}
	}
}
