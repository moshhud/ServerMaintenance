package shutdown;

import java.io.File;

public class ShutDown
{
  public static void main(String[] args)
  {
    for (int i = 0; i < 3; i++)
    {
      try
      {
        File f = new File("ShutDown.sd");
        if (f.createNewFile())
        {
          return;
        }

        try
        {
          Thread.sleep((int)(Math.random() * 1000.0D));
        }
        catch (InterruptedException ex)
        {
        }
      }
      catch (Exception ex)
      {
      }
    }

    System.out.println("Failed to create Shutdown File");
  }
}