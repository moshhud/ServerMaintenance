package maintenance;



import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;



import org.apache.log4j.Logger;

public class ProcessService extends Thread{
	boolean running = false;
	static Logger logger = Logger.getLogger(ProcessService.class);
	InputStream in = null;
	OutputStream out = null;
	
	public ProcessService(){
		try {
			running = true;
			start();
					
		}
		catch (Exception e){
			logger.fatal(e);
		}
	}//
	
	public void run() {
		Socket clientSocket = null;		
		while (running){
			try{
				clientSocket = ServiceQueue.getInstance().pop();
				Processor ob = new Processor(clientSocket);
				ob.start();	
								
			}
			catch (Exception e){
				logger.fatal(e);
			}			
		}		
	}
	
	public void shutdown(){
		running = false;
	}

	
}//end of class
