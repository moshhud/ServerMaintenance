package maintenance;

import java.net.Socket;
import org.apache.log4j.Logger;

public class ServiceQueue {
	private Socket[] socketArray;
	private static ServiceQueue queue = null;
	private static Logger logger = Logger.getLogger(ServiceQueue.class);
	
	int push =0;
	int pop = 0;
	final int QUEUE_SIZE = 400;
	private boolean isFull;
	
	
	
	public ServiceQueue(){
		
		socketArray = new Socket[QUEUE_SIZE];
		push =0;
		pop = 0;
		isFull = false;
		
	}//
	
	
	public static ServiceQueue getInstance()
	 {
	   if (queue == null)		   
		   startQueue();
	   return queue;
	}
	
	private static synchronized void startQueue(){
		if(queue == null){
			queue = new ServiceQueue();
		}			
		   
	}
	
	public boolean isEmpty(){
		return push == pop;
	}
	
	public boolean isFull(){
		return (push+1)%QUEUE_SIZE == pop;
	}
	
	public synchronized void push(Socket socket){
		if (isFull()){
			isFull = true;
			try{
				wait();
			}
			catch(Exception e){
				logger.fatal("Push: ", e);
			}
		}
		if (isEmpty()){
			notifyAll();			
		}
		push = (push+1)%QUEUE_SIZE;
		socketArray[push] = socket;
		//System.out.println("Push: "+push);
	}//end of method push
	
	public synchronized Socket pop(){
		if (isEmpty()) {
			try{
				wait();
			}
			catch(Exception e){
				logger.fatal("Pop: ", e);
			}
		}
		
		if (isFull){
			isFull = false;
			notifyAll();
		}
		pop = (pop+1)%QUEUE_SIZE;
		Socket soc = socketArray[pop];
		socketArray[pop] = null;
		//System.out.println("Pop: "+pop);
		return soc;
		
	}//end of method pop
	
	public int getSocketArraySize(){
		int size=0;
		if (push >= pop) {
			size = (push - pop);
		}
		
		return size;
	}
	

}//end of class
