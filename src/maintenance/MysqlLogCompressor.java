package maintenance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;



import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

public class MysqlLogCompressor extends Thread{
	
	private static Logger logger = Logger.getLogger(MysqlLogCompressor.class);	
	public static MysqlLogCompressor ob;
	boolean running = false;
	static int counter=0;
	static String mysql_log_Path = "/var/lib/mysql/";
	static String index_file_ext = ".index";
	static String index_file_name = "";
	String logFiles[] = new String[3000]; 
	static long rotationTime = 1;
	static long currentTime = 0L;
	static long prevTime = 0L;
	static long rotationTimeMilli = rotationTime*60*1000;	
	
	
	public static MysqlLogCompressor getInstance(){
		if(ob==null){			
			createInstance();
		}
		
		return ob;
		
	}//
	
	public static synchronized MysqlLogCompressor createInstance(){
		if(ob==null){
			ob = new MysqlLogCompressor();
			LoadConfiguration();
		}
		return ob;
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
		        
		        if(properties.get("MySqlIndex")!=null){
		        	index_file_name =  (String) properties.get("MySqlIndex");
		        }
		        
		        if(properties.get("MySqlPath")!=null){
		        	mysql_log_Path =  (String) properties.get("MySqlPath");
		        }
		        
		        String t="";
			      
		        if(properties.get("rotationTime")!=null){
		        	t =  (String) properties.get("rotationTime");
		        }
		        
		        rotationTime = Long.parseLong(t);
		        rotationTimeMilli = rotationTime*60*1000;
		       		        
		        fileInputStream.close();

		      }
		    logger.debug("Index File: "+index_file_name);
		    //System.out.println("Index File: "+index_file_name);
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
	
	public void run(){
		logger.debug("MySql Log Compress service started");
		//System.out.println("MySql Log Compress service started");
		running = true;
		
		try {
			while(running){	
				logger.debug("Current path: "+mysql_log_Path);			    
				compressLogs(mysql_log_Path);					
				Thread.sleep(rotationTimeMilli);
			}
		}
		catch(Exception e){
			logger.fatal("Error: "+e.toString());
			//closeConnection();
			shutdown();
		}
	}//
	
	
	public void  compressLogs(String filePath){
		long st = System.currentTimeMillis();
		long et= 0;
		
		
		
		try{
			
			String file ="";
			currentTime = System.currentTimeMillis();
			prevTime = currentTime - rotationTimeMilli;
			long difference = currentTime-prevTime;
			
			if(difference>=rotationTimeMilli){
				
				findFiles(filePath);		
				
				File dir = new File(filePath+"/LogBackUp/");
				
				if(dir.mkdir()){
					logger.debug("Back Up directory created");
				}
				else{
					logger.debug("Back Up directory already exists");
				}
				
				if(i>0){
					for(int j=0;j<i-1;j++){				
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
				
				
			}
			else{
			  System.out.println("Else: Rotation time diff: "+difference);
			}
			
			
			
					
		  }
		  catch(Exception e){
			logger.fatal(e.toString());
			
		  }
		
	}//



	int i=0;
	public void findFiles(String dir){
		i=0;	
		try{
			BufferedReader reader = new BufferedReader(new FileReader(dir+index_file_name));	
			StringBuffer sb = new StringBuffer();
			String str;
		      while ((str = reader.readLine()) != null)
		      {        
		    	  logFiles[i] = str.substring("./".length(),str.length());
		    	  //System.out.println("File: " + logFiles[i]);
		          i++;
		      }
		      reader.close();	    
	     
		}
		catch(Exception e){
			logger.fatal("Directory error: "+e.toString());
			
		}
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
	          
	          gzipOS.close();
	          fos.close();	          	          	         
	          fis.close();
	          
	      } catch (IOException e) {
	    	  //System.out.println("Compress error: "+e.toString());
	    	  logger.fatal("Compress error: "+e.toString());
	      }
	      
	  }//
	
	public void shutdown()
	{
		System.out.println("MySql log compressor stopped");
		logger.debug("MySql log compressor stopped");
	    running = false;
	    //System.exit(0);
	 }//

}//end of classs
