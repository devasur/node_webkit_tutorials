package org.entelligentsia.image;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;


public class Server {
	ServerSocket serverSocket = null;
	private static final ExecutorService clientThreadExecutors = Executors.newCachedThreadPool();
	private static final ExecutorService serverThreadExecutor = Executors.newSingleThreadExecutor();
	
	public Server(final int port) throws Exception{
		serverThreadExecutor.submit(new Runnable(){
			public void run() {
				try {
					serverSocket = new ServerSocket(port);		
					System.out.println("Server started and listening on PORT:" + port);
					while(!serverSocket.isClosed()){
						try {
							Socket clientSocket = serverSocket.accept();
							System.out.println("New Client connection");
							clientThreadExecutors.submit(new ImageSpitter(clientSocket));
						} catch (java.net.SocketException e) {
							System.out.println("Server Socket is no more accepting connections :" + e.getMessage());
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void shutdown() throws Exception{
		System.out.println("Initiating orderly shutdown");
		clientThreadExecutors.shutdown();
		while(!clientThreadExecutors.isShutdown()){
			System.out.println("Waiting for pending tasks to finish");
		}
		System.out.println("No pending requests in queue.  Closing socket");
		serverSocket.close();
		System.out.println("Server socket is closed.  Good Bye!");
		serverThreadExecutor.shutdown();
		
	}
	
	class ImageSpitter implements Runnable{
		Socket clientSocket = null;
		public ImageSpitter(Socket socket){
			this.clientSocket = socket;
		}
		public void run() {
			try {
				OutputStream os = clientSocket.getOutputStream();
				BufferedImage bi = getRandomImage();
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ImageIO.write(bi, "PNG", bos);
				byte[] image = bos.toByteArray();
				System.out.println("Writing " + image.length + " bytes to output");
				os.write(image);
				os.flush();
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		private BufferedImage getRandomImage(){
			BufferedImage off_Image = new BufferedImage(250, 20,BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = off_Image.createGraphics();
			g2.drawString(UUID.randomUUID().toString(), 10, 15);
			return off_Image;
		}
	}
	
	public static void main(String[] args) throws Exception{
		final Server s = new Server(4444);
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				try {
					s.shutdown();
				} catch (Exception e) {
					System.out.println("Unable to shutdown server:" + e.getMessage());
				}
			}
		});
		while(true){			
		}
	}
}
