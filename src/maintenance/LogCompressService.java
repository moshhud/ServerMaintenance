package maintenance;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
import java.util.StringTokenizer;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;



public class LogCompressService extends Thread{
	
	private static Logger logger = Logger.getLogger(LogCompressService.class);
	public static LogCompressService obService;
	boolean running = false;
	static long currentTime = 0L;
	static long prevTime = 0L;
	static long rotationTime = 1;
	static long rotationTimeMilli = rotationTime*60*1000;	
	static String path ="";
	static int counter=0;
	
	static String ServiceName = "iTelSwitchPlusSignaling";
	String logFiles[] = new String[3000]; 
	static String services[] = new String[100];
	
	public static LogCompressService getInstance(){
		if(obService==null){			
			createInstance();
		}
		
		return obService;
		
	}//
	
	public static synchronized LogCompressService createInstance(){
		if(obService==null){
			obService = new LogCompressService();
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
		        
		        if(properties.get("ServiceName")!=null){
		        	ServiceName =  (String) properties.get("ServiceName");
		        }
		        
		        StringTokenizer ST = new StringTokenizer(ServiceName,",");
		        
		        while(ST.hasMoreTokens()){
		        	services[counter] = ST.nextToken();
		        	counter++;
		        }
		        
		        
		        String t="";
		      
		        if(properties.get("rotationTime")!=null){
		        	t =  (String) properties.get("rotationTime");
		        }
		        
		        rotationTime = Long.parseLong(t);
		        rotationTimeMilli = rotationTime*60*1000;
		        
		        if(properties.get("Path")!=null){
		        	path =  (String) properties.get("Path");
		        	
		        }
		        
		       
		        fileInputStream.close();

		      }
		    logger.debug("Services: "+ServiceName);
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
		
	}//

public void shutdown()
{
	System.out.println("switch log compressor stopped");
	logger.debug("switch log compressor stopped");
    running = false;
    //System.exit(0);
 }//


public void run(){
	logger.debug("iTel Switch Log Compress service started");
	running = true;
	String keyword = "2016-03";
	
	try {
		while(running){
			
			if(counter>0){
				for(int i=0;i<counter;i++){
					String p = path+services[i]+"/";
					logger.debug("Current path: "+p);
					//System.out.println("Current path: "+p);					
					keyword = getDateAndTime();
					compressLogs(p,keyword);
					String webLogPath = "/";
					logger.debug("Current path: "+webLogPath);
					compressLogs(webLogPath,keyword);
				}				
				
			}
			Thread.sleep(rotationTimeMilli);
			
			
		}
	}
	catch(Exception e){
		logger.fatal("Error: "+e.toString());
		//closeConnection();
		shutdown();
	}
}


public void  compressLogs(String filePath,String keyword){
	long st = System.currentTimeMillis();
	long et= 0;	
	
	try{
		
		String file ="";
		currentTime = System.currentTimeMillis();
		prevTime = currentTime - rotationTimeMilli;
		long difference = currentTime-prevTime;
		
		if(difference>=rotationTimeMilli){
			
			findFiles(filePath,keyword);		
			
			File dir = new File(filePath+"/LogBackUp/");
			
			if(dir.mkdir()){
				logger.debug("Back Up directory created");
			}
			else{
				logger.debug("Back Up directory already exists");
			}
			
			if(i>0){
				for(int j=0;j<i;j++){				
					file = logFiles[j];
					logger.debug("Compressing Log files: "+filePath+file);
					compressGzipFile(filePath+file, filePath+"LogBackUp/"+file+".gz");	
					
					File f = new File(filePath+file);
					if(f.delete()){
						logger.debug("Completed :  "+file);
					}
					
				}
				
				et = System.currentTimeMillis();
				logger.debug("Logs compressed done.");
				logger.debug("Total Time: "+(et-st)/1000);				
				
			}
			else{
				logger.debug("Log File not found");
			}
			
			Thread.sleep(1000);
			logger.debug("Deleting old logs...");
			deleteOldLogs(filePath);
			
		}
		else{
		  System.out.println("Else: Rotation time diff: "+difference);
		}
		
		
		
				
	  }
	  catch(Exception e){
		logger.fatal(e.toString());
		
	  }
	
}//

public void deleteOldLogs(String filePath){
	
	findFiles(filePath,".log.");
	String file = "myfile";
	if(i>0){
		for(int j=0;j<i;j++){	
			file = logFiles[j];
			File f = new File(filePath+file);
			if(f.delete()){
				logger.debug("Deleted :  "+file);
			}
		}
	}
	else{
		logger.debug("Old log files not available.  ");
	}
	
}



int i=0;
public void findFiles(String dir, String keyword){
	i=0;	
	try{
		File file = new File(dir);
		
        String[] names = file.list();

     for(String name : names)
     {      
      
      if (new File(dir + name).isDirectory())
      {
        //System.out.println("Directory: "+name);
        //logger.debug("Directory: "+name);
      }      
      if (new File(dir + name).isFile())
      {
    	  
        if(name.contains(keyword)){        	
        	logFiles[i] = name;
        	logger.debug("File: "+name);
        	i++;
        }
      }      
     }
     
	}
	catch(Exception e){
		logger.fatal("Directory error: "+e.toString());
		
	}
}

  public String getDateAndTime(){
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM");
    long now = System.currentTimeMillis();
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis((long) now);   
    return formatter.format(calendar.getTime());
   
  }//
  
  private static void compressGzipFile(String file, String gzipFile) {
      try {
          FileInputStream fis = new FileInputStream(file);
          FileOutputStream fos = new FileOutputStream(gzipFile);
          GZIPOutputStream gzipOS = new GZIPOutputStream(fos);
          byte[] buffer = new byte[1024];
          int len;
          while((len=fis.read(buffer)) != -1){
              gzipOS.write(buffer, 0, len);
          }
          //close resources
          gzipOS.close();
          fos.close();
          fis.close();
      } catch (IOException e) {
          e.printStackTrace();
      }
      
  }//

}//
