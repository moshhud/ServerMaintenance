package maintenance;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class Processor extends Thread{
	static Logger logger = Logger.getLogger(Processor.class);
	InputStream in = null;
	OutputStream out = null;
	Socket clientSocket = null;
	String fileLocation = "H:/ReceivedFile/";
	boolean readMSG = true;
	String bsscPropertiesFile = "server.cfg";
	String bsscPath= "/usr/local/";
	String IP="";
	int PORT=0;
	public Processor(Socket clientSocket){
		this.clientSocket = clientSocket;
		IP = clientSocket.getRemoteSocketAddress().toString();
		IP = IP.replace("/", "");
		PORT  = clientSocket.getPort();
		//System.out.println("Processing IP: "+IP.replace("/", "")+":"+clientSocket.getPort());
		logger.debug("Processing IP: "+IP);
	}
	public void run(){
		getStreams(clientSocket);		
		processConnection();
		
		
	}
	
	private synchronized void getStreams(Socket socketConnection){
		try{
			
			out = socketConnection.getOutputStream();
			out.flush();
			in = socketConnection.getInputStream();				
			//logger.debug("Got I/O");
		}
	    catch (Exception e) {		
		  System.out.println("Error in Processor: "+e.toString());
		  logger.fatal("Error in Processor: "+e.toString());
	    }
		
	}//	
	
	private synchronized void processConnection(){
		try{
			byte[] buffer = new byte[1024];
			String msg = "";			
			do{
				try{					
					int isAvailble = in.available();
					if (isAvailble > 0){	
						int buffSize = in.read(buffer);
						String recievedMsg = new String(buffer, 0, buffSize);							
						msg = recievedMsg;	
						processData(recievedMsg);							
						break;
					}	
				}
			    catch (Exception e) {		
				  logger.fatal("Error: "+e.toString());
			    }
			}while(!msg.equals("terminate"));
			
		}
	    catch (Exception e) {		
		  logger.fatal("Error: "+e.toString());
	    }
		
	}//

	
	private synchronized void  processData(String message){
		try{		
				
			int command = getCommand("command",message);
			String service = "";
			String switchIP = "";
			String switchPort = "";
			
			String response = "";
			switch(command){
			  case 1://start
				  service = getAttributeValue("services",message);				
				  logger.debug("Command:["+command+"] is executing for servic: "+service);
				  restart(service,1);
				  response = getStatusMsg(service,command,"Process completed.",0);
				  sendData(response);
				  break;
			    
			  case 2://stop
					service = getAttributeValue("services",message);				
					logger.debug("Command:["+command+"] is executing for servic: "+service);
					restart(service,2);
					response = getStatusMsg(service,command,"Process completed.",0);
					sendData(response);
				    break;
			  case 3://restart
					service = getAttributeValue("services",message);						
					logger.debug("Command:["+command+"] Restart is executing for servic: "+service);
					restart(service,3);
					response = getStatusMsg(service,command,"Process completed.",0);
					sendData(response);
				    break;
			  case 100://switch ip change from byte saver signal converter
					service = getAttributeValue("services",message);				
					switchIP = getAttributeValue("switch-ip",message);
					logger.debug("Command:["+command+"] is executing for service: "+service+",sw: "+switchIP);
					switchIPPortChange(service,100,switchIP,switchPort);
					response = getStatusMsg(service,command,"Process completed.",0);
					sendData(response);
				    break;
			  case 101://switch port change from byte saver signal converter
					service = getAttributeValue("services",message);		
					switchPort = getAttributeValue("switch-port",message);
					logger.debug("Command:["+command+"] is executing for service: "+service+", Port: "+switchPort);
					switchIPPortChange(service,101,switchIP,switchPort);
					response = getStatusMsg(service,command,"Process completed.",0);
					sendData(response);
				    break;
			  case 102://switch ip and port change from byte saver signal converter
					service = getAttributeValue("services",message);		
					switchIP = getAttributeValue("switch-ip",message);
					switchPort = getAttributeValue("switch-port",message);
					logger.debug("Command:["+command+"] is executing for service: "+service+",sw: "+switchIP+", port: "+switchPort);
					switchIPPortChange(service,102,switchIP,switchPort);
					response = getStatusMsg(service,command,"Process completed.",0);
					sendData(response);	
				    break;
			  case 103://Show switch ip and port from byte saver
				  
				    String key="";
					service = getAttributeValue("services",message);							
					logger.debug("Command:["+command+"] is executing for service: "+service);
					key = "terminatingPeerIP";
					switchIP = readPropValue(bsscPath+service+"/server.cfg",key);
					key = "terminatingPeerPort";					
					switchPort = readPropValue(bsscPath+service+"/server.cfg",key);
					String msg="Switch ip and Port,"+switchIP+","+switchPort;
					
					
					if(switchIP.contains("No such file or directory")){
						msg="No such file or directory,0,0";
					}
					response = getStatusMsg(service,103,msg,0);
					sendData(response);	
				    break; 
			   case 200:
				   String billing = getAttributeValue("billingName",message);
				   logger.debug("Got switch installation request for "+billing);
				   break;
			   case 800:
				   	service = getAttributeValue("services",message);
				   	logger.debug("Command:["+command+"] is executing for service: "+service);
				   	linuxCommand(service,800);
				   	msg="process completed";
				   	response = getStatusMsg(service,800,msg,0);
				   	sendData(response);
				   	break;
			   default:
				  service = getAttributeValue("services",message);
				  logger.debug("[D] Command not matched");
				  sendData(getStatusMsg(service,command,"UnRecognized Command",0));	
				  break;
			}			
		}
	    catch (Exception e) {		
		  System.out.println("Error: "+e.toString());
		  logger.fatal("Error: "+e.toString());
	    }		
	}//
	
	public void linuxCommand(String service,int cmd){
		String Status = "";
		String response="";
		try{
			Process process = null;
			logger.debug("Executing: "+service);
			process = Runtime.getRuntime().exec(service);
			Status = showProcessStatus(process);
			response = getStatusMsg(service,cmd,Status,1);
			sendData(response);		
		}
		catch (Exception e){
			logger.fatal("linuxCommand: "+e.toString());
		}
		
	}
	
	
	public void restart(String serviceName,int cmd){
		String Status = "";
		
		//add all exceptions/successful  logs pattern to reply accordingly. 
		String keyword[] = {"started successfully","Address already in use","Cannot assign requested address","could not open socket","Failed to open DatagramSocket"};
		
		try{
			Process process = null;
			String command = "service  "+serviceName+"  stop";
			String log  = "Mylog.log";
			
			if(serviceName.startsWith("ByteSaverMediaProxy")){
				log  = "MediaProxy.log";
			}
			else if(serviceName.startsWith("ByteSaverSignalConverter")){
				log  = "SignalingProxy.log";
			}
			else if(serviceName.startsWith("iTelSwitchPlusMediaProxy")){
				log  = "iTelSwitchPlusMediaProxy.log";
			}
			else if(serviceName.startsWith("iTelSwitchPlusSignaling")){
				log  = "iTelSwitchPlusSignaling.log";
			}
			else if(serviceName.startsWith("BalanceServer")){
				log  = "balance.log";
			}
			else if(serviceName.startsWith("TopUpServer")){
				log  = "server.log";
			}
			else if(serviceName.startsWith("iTelAutoSignUp")){
				log  = "iTelAutoSignUp.log";
			}
			else if(serviceName.startsWith("MobileBilling")){
				log  = "mobilebiling.log";
			}
			else if(serviceName.startsWith("MoneyTransfer")){
				log  = "MoneyTransfer.log";
			}
			String m="";
			if(cmd==1){
				m="Starting the service...";
			}
			else if(cmd==2){
				m="Stoping the service...";
			}
			else{
				m="Restarting the service...";
			}
			
			String response = getStatusMsg(serviceName,cmd,m,1);
			sendData(response);	
			
			logger.debug("Executing: "+command);
			process = Runtime.getRuntime().exec(command);
			Status = showProcessStatus(process);
			Thread.sleep(3000);			
			
			File logFile  = new File("/usr/local/"+serviceName+"/"+log);
			deleteFile(logFile);
			
			logger.debug("S: "+Status);
			int next=0;
			if(Status.contains("unrecognized service") || Status.contains("Permission denied")){
				logger.debug("Got some error: "+Status);
				
			}
			else{
				next=1;
				Status = "Shutdown completed";
			}
			
			if(next==1&&(cmd==3||cmd==1)){
				command = "service  "+serviceName+"  start";
				logger.debug("Executing: "+command);
				process = Runtime.getRuntime().exec(command);
				Thread.sleep(5000);				
				Status = readLogStatus(logFile,keyword);
				
			}//		
			response = getStatusMsg(serviceName,cmd,Status,1);
			sendData(response);		
			
		}//
		catch (Exception e){
			logger.fatal("Rest: "+e.toString());
		}
		
		
	}//
	
	public String showProcessStatus(Process process){
		String msg="No error detected.";
		try{
			
			BufferedReader stdInput  = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader stdError  = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				 			 			 			
			String strReadLine = "";
			StringBuffer sbOutput = new StringBuffer();
	        StringBuffer errorInfo = new StringBuffer();
				
				//error logs
			while ((strReadLine = stdError.readLine()) != null){
	              errorInfo.append(strReadLine);
	           }
	        if (errorInfo.length() > 0)
	           {
	             //System.out.println(errorInfo.toString());  
	             logger.debug(errorInfo.toString());
	             msg=errorInfo.toString();
	        }//
	        
	        
	           //reading successful command logs
	        while ((strReadLine = stdInput.readLine()) != null) {
	        	sbOutput.append(strReadLine + ",");
	        	logger.debug("IStream: " + strReadLine + "\n");
	        	
	        }
	        if (sbOutput.length() > 0){
	        	msg=sbOutput.toString();
	        }
	        
	       
		}
        catch (Exception e){
			logger.fatal("Process status: "+e.toString());
		}
		//logger.debug("return: "+msg);
		return msg;
	}
	
	public void switchIPPortChange(String serviceName,int cmd,String switchIP,String switchPort){
		String content = "";
		String value = "";
		String value2 = "";
		String key = "";
		String key2 = "";
		String msg = "Error";
		try{
			switch(cmd){
			  case 100:
				  logger.debug("Setting switch IP: "+switchIP+" into the byte saver: "+serviceName);
				  key = "terminatingPeerIP";
				  content = readFile(bsscPath+serviceName+"/server.cfg");
				  value = readPropValue(bsscPath+serviceName+"/server.cfg",key);				  
				  content = content.replace(key+"="+value,key+"="+switchIP);				  
				  writeFile(bsscPath+serviceName+"/server.cfg",content);
				  msg="Switch IP changed Successfully";
				  break;
			  case 101:
				  logger.debug("Setting switch Port:"+switchPort+" into the byte saver: "+serviceName);
				  key = "terminatingPeerPort";
				  content = readFile(bsscPath+serviceName+"/server.cfg");
				  value = readPropValue(bsscPath+serviceName+"/server.cfg",key);				  
				  content = content.replace(key+"="+value,key+"="+switchPort);				  
				  writeFile(bsscPath+serviceName+"/server.cfg",content);
				  msg="Switch Port changed Successfully";
				  break;
				  
			  case 102:
				  logger.debug("Setting switch IP: "+switchIP+" & Port: "+switchPort+" into the byte saver: "+serviceName);
				  key = "terminatingPeerIP";
				  key2 = "terminatingPeerPort";
				  content = readFile(bsscPath+serviceName+"/server.cfg");
				  value = readPropValue(bsscPath+serviceName+"/server.cfg",key);
				  value2 = readPropValue(bsscPath+serviceName+"/server.cfg",key2);
				  content = content.replace(key+"="+value,key+"="+switchIP);	
				  content = content.replace(key2+"="+value2,key2+"="+switchPort);	
				  writeFile(bsscPath+serviceName+"/server.cfg",content);
				  msg="Switch IP & Port changed Successfully";
				  break;
			}
			
			String response = getStatusMsg(serviceName,cmd,msg,1);
			sendData(response);	
			
			Thread.sleep(2000);
			logger.debug("Restarting the services");
			restart(serviceName,3);
		}
		catch (Exception e){
			logger.fatal(e.toString());
		}
	}//
	

	public String readFile(String fileName){
		String data = "";
		
		try{
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			StringBuffer sb = new StringBuffer();
			while ((data = reader.readLine()) != null){
				sb.append(data);
				sb.append("\n");
			}
			reader.close();
			
			data = sb.toString();
		}
		catch (Exception e){
			logger.fatal(e.toString());
		}
		return data;
	}//
	
	
	public void writeFile(String fileName, String content){
		
		try{
			File file = new File(fileName);
			
			FileOutputStream FOS = null;
			FOS = new FileOutputStream(file);
			FOS.write(content.getBytes());
			FOS.flush();
			
			if (FOS != null) {
				FOS.close();
			}
			
		}catch (Exception e){
			logger.fatal(e.toString());
		}
		
	}
	
    public String readPropValue(String filename, String key){
		
		String data = "";		
		try{
   			Properties prop =new Properties();
   			prop.load(new FileInputStream(filename));
   		
   			data = prop.getProperty(key);   			
   			logger.debug("Current Value : " + data);
   			
   		}
   		catch (Exception e){   
   			logger.fatal(e.toString());
   			data = e.toString();
   			if(data.contains("No such file or directory")){
   				data="No such file or directory";
   			}
        }//
		
		return data;		
    }//

	public String readLogStatus(File logFile, String keyword[]){
		String status = "Time out";
		long currentTime = System.currentTimeMillis();
		int a=0;
        boolean running = true;
        
        while (running){        	
        	try{
        		if (logFile.exists()){
        			List <String> lines = FileUtils.readLines(logFile);
        			for (String line : lines){
        				for(int i=0;i<keyword.length;i++){        					
        					if (line.toLowerCase().contains(keyword[i].toLowerCase())){
            					a=1;
            				}        				
        			      }        				
        			    if(a!=1)
            			  continue;	  
        			
        			    logger.debug(line);
        			    status = line;
        			    running=false;
        			    break;        			
        			}
        			
        		}
        		
        	}
        	catch (Exception e){
        	 	logger.fatal(e.toString());
        	}//
        	
        	if (System.currentTimeMillis() > currentTime + 30000){
        		logger.debug("Time out");
        		status = "Time out";
        		break;
        	}
        	
        }//end of while
        
        return status;
	}
	
	public int getCommand(String at,String msg){
		int value=-1;
        try{
            JSONParser parser=new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(msg);
            value = (int)(Long.parseLong((String)jsonObject.get(at)));           
            
        }
        catch (Exception e) 
        {           
           logger.fatal(e.toString());
        }        
        return value;
    }//
	
	public String getAttributeValue(String at,String msg){
		String value="";
        try{
            JSONParser parser=new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(msg);
            value = (String) jsonObject.get(at);            
            
        }
        catch (Exception e) 
        {            
            e.printStackTrace();
        }        
        return value;
    }//
	
	
	public String getStatusMsg(String service, int command,String status,int statusCode){
		String msg = "";
		String ip="";
		String port="";
		JSONObject obj = new JSONObject();
		obj.put("command", Integer.toString(command));
		obj.put("services", service);
		obj.put("status", status);
		if(command==103){
			StringTokenizer ST = new StringTokenizer(status,",");
			status = ST.nextToken();
			ip = ST.nextToken();
			port = ST.nextToken();
			obj.put("IP", ip);	
			obj.put("PORT", port);	
		}			
		obj.put("statusCode", Integer.toString(statusCode));	//statusCode=0 finish, 1=going on
		
		msg = obj.toString();
		return msg;
	}
	
	
	private synchronized void sendData(String message){
		try{
			logger.debug("Sending Response to "+IP);
			out.write(message.getBytes());
			out.flush();			
			//logger.debug(message);
		}
	    catch (Exception e) {		
		  //System.out.println("Error: "+e.toString());
		  logger.fatal("Error: "+e.toString());
	    }
		
	}//
	
	private synchronized void deleteFile(File logFile){
		if (logFile.exists())
	      {
	        try
	        {
	          logFile.delete();
	        }
	        catch (Exception e)
	        {
	        	logger.fatal(e.toString());
	        }
	      }
	}
	
	private synchronized String  uploadFile(String fileName){
		String status = "Failure";
		long timeStart = 0;
		long timeEnd = 0;
		byte [] bytes  = new byte [1024];
		//long fileSize = 1024;
		
		try{
			 
			 timeStart = System.currentTimeMillis();
			 ServerSocket serverSocket = new ServerSocket(15123);
             Socket clientSocket = serverSocket.accept();
			 InputStream is = clientSocket.getInputStream();
			 FileOutputStream fos = new FileOutputStream(fileLocation+fileName);			 
			 BufferedOutputStream bos = new BufferedOutputStream(fos);
			 
			 //double p = 0.0;
			 //long count = 0;
			 
			 int i=0;
             while ((i = is.read(bytes)) != -1){             	
             	bos.write(bytes, 0, i);
             	//p = (count*100)/fileSize;
             	//System.out.println("Percent: "+p);
             }		
			
			 bos.flush();
			 is.close();
			 bos.close();
			 clientSocket.close();
			 serverSocket.close();
			 timeEnd = System.currentTimeMillis();
			 long time = timeEnd - timeStart;
	         status = "Successful: file "+fileName+" received. Time: "+time;
	         	
	        }
	        catch (Exception e)
	        {
	        	status  = "Failure"+e.toString();
	        	System.out.println(e.toString());
	        	logger.fatal(e.toString());
	        }
	      logger.debug(status);
	      return status;
	}
	

}// End of class
