package maintenance;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import shutdown.ShutDownListener;
import shutdown.ShutDownService;


public class ServerMaintenanceMain implements ShutDownListener{
	
	
	static ServerMaintenanceMain obMain;
	static Logger logger = Logger.getLogger(ServerMaintenanceMain.class);
	static boolean running;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		try{
			PropertyConfigurator.configure("log4j.properties");
			obMain = new ServerMaintenanceMain();
			ShutDownService.getInstance().addShutDownListener(obMain);
			
			LogCompressService.getInstance().start();
			MysqlLogCompressor.getInstance().start();
			Service.getInstance().start();
			
			
			logger.debug("started successfully");
			//System.out.println("started successfully");
			
		}
		catch (Exception e)
	    {
			logger.fatal(e.toString());
	    }

	}//
	
	
	@Override
	public void shutDown() {
		// TODO Auto-generated method stub
		try{
			running = false;
			LogCompressService.getInstance().shutdown();
			MysqlLogCompressor.getInstance().shutdown();
		}
		catch (Exception ex)
	    {
	    }
		logger.debug("Shut down server successfully");
	    //System.out.println("Shut down server successfully");	    
	    System.exit(0);
	}//end of shutdown	

}
