package shutdown;

import java.io.File;
import java.util.Date;
import java.util.Vector;

import org.apache.log4j.Logger;

public class ShutDownService extends Thread
{
  public static final String SHUT_DOWN_FILE_NAME = "ShutDown.sd";
  static Logger logger = Logger.getLogger(ShutDownService.class.getClass());
  boolean running;
  public Vector shutDownList;
  private static ShutDownService shutDownService = null;

  private ShutDownService()
  {
    File file = new File("ShutDown.sd");
    file.delete();
    this.shutDownList = new Vector();
    this.running = true;
    start();
  }

  public void addShutDownListener(ShutDownListener sdl)
  {
    this.shutDownList.add(sdl);
  }

  private static synchronized void createShutDownThread()
  {
    if (shutDownService == null)
      shutDownService = new ShutDownService();
  }

  public static ShutDownService getInstance()
  {
    if (shutDownService == null)
    {
      createShutDownThread();
    }
    return shutDownService;
  }

  public void removeAllShutDownListener()
  {
    this.shutDownList.removeAllElements();
  }

  public void removeShutDownListener(ShutDownListener sdl)
  {
    this.shutDownList.remove(sdl);
  }

  public void run()
  {
    while (this.running)
    {
      File file = new File("ShutDown.sd");
      if (file.exists())
      {
        logger.debug("Going To ShutDown At:" + new Date());
        if (!file.delete())
        {
          file.deleteOnExit();
        }
        stopService();
        for (int i = 0; i < this.shutDownList.size(); i++)
        {
          ShutDownListener sdl = (ShutDownListener)this.shutDownList.elementAt(i);
          sdl.shutDown();
        }
      }

      try
      {
        Thread.sleep(1000L);
      }
      catch (Exception ex)
      {
      }
    }
  }

  public void stopService() {
    this.running = false;
  }
}