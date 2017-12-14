package maintenance;

public class SettingsDTO {
    
    public static SettingsDTO obSettingsDTO;
 
    
    
    String serverlIP;
    int serverPort;    
    String userName;
    String password;
    String clientIP;
    int clientPort;
    String nonce;
    
    
    public static SettingsDTO getInstance(){
		if(obSettingsDTO==null){			
			createInstance();
		}
		
		return obSettingsDTO;
	}//
	
	public static synchronized SettingsDTO createInstance(){
		if(obSettingsDTO==null){
			obSettingsDTO = new SettingsDTO();
			
		}
		return obSettingsDTO;
	}//
	
	    

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }
        
    

    public String getServerlIP() {
        return serverlIP;
    }

    public void setServerlIP(String serverlIP) {
        this.serverlIP = serverlIP;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getClientIP() {
        return clientIP;
    }

    public void setClientIP(String clientIP) {
        this.clientIP = clientIP;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    
    
    
    
}//end of class
