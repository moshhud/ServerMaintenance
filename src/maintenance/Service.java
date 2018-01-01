package maintenance;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;


public class Service extends Thread{
	
	private static Logger logger = Logger.getLogger(Service.class);
	public static boolean running = false;
	public static Service obService;
	ProcessService obProcessService;
	private ServerSocket serverSocket;
	private Socket socketConnection;
	static String AllowedIP = "127.0.0.1";
	
	InputStream in = null;
	OutputStream out = null;
	

	
	public static Service getInstance(){
		if(obService==null){			
			createInstance();
		}
		
		return obService;
	}//
	
	public static synchronized Service createInstance(){
		if(obService==null){
			obService = new Service();
			LoadConfiguration();
		}
		return obService;
	}//
	
    public static void LoadConfiguration(){
		
		FileInputStream fileInputStream = null;
		String strConfigFileName = "properties.cfg";
		try
	    {
			Properties properties = new Properties();
		    File configFile = new File(strConfigFileName);
		    if (configFile.exists())
		      {
		    	fileInputStream = new FileInputStream(strConfigFileName);
		        properties.load(fileInputStream);
		        
		        if(properties.get("AllowedIP")!=null){
		        	AllowedIP = (String) properties.get("AllowedIP");
		        }
		        
		        String Port = "220";
		        String serverIP="";
		        if(properties.get("serverIP")!=null){
		        	serverIP = (String) properties.get("serverIP");
		        }
		        int serverPort = 0;
		        if(properties.get("serverPort")!=null){
		        	Port =  (String) properties.get("serverPort");
		        }
		        serverPort = Integer.parseInt(Port);	
		        
		        String user = "";
		        if(properties.get("userName")!=null){
		        	user =  (String) properties.get("userName");
		        }
		        String password = "";
		        if(properties.get("password")!=null){
		        	password =  (String) properties.get("password");
		        }
		        
		        
		        SettingsDTO.getInstance().setServerlIP(serverIP);
		        SettingsDTO.getInstance().setServerPort(serverPort);		        
		        SettingsDTO.getInstance().setUserName(user);
		        SettingsDTO.getInstance().setPassword(password);
		        
		        
		        fileInputStream.close();

		      }
	    }
		catch (Exception ex)
	    {
	      logger.fatal("Error while loading configuration file :" + ex.toString(), ex);
	      //System.out.println("Error: "+ex);
	      System.exit(0);
	    }
	    finally
	    {
	      if (fileInputStream != null)
	      {
	        try
	        {
	        	fileInputStream.close();
	        }
	        catch (Exception ex)
	        {
	        	logger.fatal(ex.toString());
	        }
	      }
	    }
		
	}
	
	
	public void shutdown()
	 {
		System.out.println("Shutting down");
	     running = false;
	     
	     if(serverSocket!=null){
	    	 try{
	    		 obProcessService.shutdown();
	    		 serverSocket.close();
	    	 }
	    	 catch (Exception e){
	    		 logger.fatal("Shutdown: "+e);
	    		 System.exit(0);
		     }
	     }
	     System.exit(0);
	  }//
	
	public void run(){
		running = true;
		try {
			serverSocket = new ServerSocket(SettingsDTO.getInstance().getServerPort(),0,InetAddress.getByName(SettingsDTO.getInstance().getServerlIP()));
			System.out.println("Service started with IP: "+SettingsDTO.getInstance().getServerlIP() +" Port: "+SettingsDTO.getInstance().getServerPort());
    		logger.debug("Service started with IP: "+SettingsDTO.getInstance().getServerlIP() +" Port: "+SettingsDTO.getInstance().getServerPort());
    		obProcessService = new ProcessService();
    		 
	    while(running){
	    	try{
	    		
	    		waitForConnection();	    		
	    		
		    }catch (Exception e) {
				// TODO Auto-generated catch block
	    		//System.out.println("Error:1 "+e.toString());
	    		logger.fatal("Error:1 "+e.toString());
	    		closeConnection();
			}
	    	finally{
	    		
	    	}
	      }
		}		
		catch(Exception e){
			System.out.println("socket: "+e.toString());
			//closeConnection();
			shutdown();
		}
	    
	}// End of run method  
	
	private void waitForConnection(){
		try{
			//System.out.println("waiting for client request...\n");
			logger.debug("waiting for client request...\n");
			socketConnection = serverSocket.accept();
			String IP = socketConnection.getInetAddress().toString();			
	        
			String ipaddress = IP.replace("/", "");
			
			
			logger.debug("Connected to "+ipaddress+":"+socketConnection.getPort());
			
			if(AllowedIP.contains(ipaddress)){
				logger.debug("Got Request from a valid IP: " +ipaddress);
				ServiceQueue.getInstance().push(socketConnection);
			}
			else{
				logger.debug("Got Request from a Invalid IP: "+ipaddress);
				sendResponse(socketConnection,"Unauthorized access request.");
			}
				
			int queueSize = ServiceQueue.getInstance().getSocketArraySize();			
			logger.debug("Queue size: "+queueSize);
			
		}
	    catch (Exception e) {				  
		  logger.fatal("Error: "+e.toString());
	    }
		
	}//
	
	private synchronized void sendResponse(Socket socketConnection,String message){
		try{
			
			out = socketConnection.getOutputStream();
			out.flush();
			message = getStatusMsg(-1,"-1",message,0);
			out.write(message.getBytes());
			out.flush();		
			out.close();
			socketConnection.close();
		}
	    catch (Exception e) {		
		  
		  logger.fatal("Error sending response: "+e.toString());
	    }
		
	}//	
	
	public String getStatusMsg(int command,String service, String status,int statusCode){
		String msg = "";
		JSONObject obj = new JSONObject();
		obj.put("command", command);
		obj.put("services", service);
		obj.put("status", status);		
		obj.put("statusCode", statusCode);	//statusCode=0 finish, 1=still going on
		msg = obj.toString();
		return msg;
	}
	
	private void closeConnection(){
		logger.debug("Closing connection ");
		running = false;
	     
	     
	     if(serverSocket!=null){
	    	 try{
	    		 obProcessService.shutdown();
	    		 serverSocket.close();
	    	 }
	    	 catch (Exception e){
	    		 logger.fatal("Closing connection: "+e);
	    		 System.exit(0);
		     }
	     }
	}
	

}
