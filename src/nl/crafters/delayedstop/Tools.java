package nl.crafters.delayedstop;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

public class Tools {
	public static boolean isInteger( String input ) {  
		try {  
			Integer.parseInt( input );  
			return true;  
		}  
		catch(Exception e) {  
			return false;  
		}  
	}  	

	public static void AddLog(String message) {
    	Logger.getLogger("Minecraft").info("[DelayedStop] " + message);
    }
	public static void Log(Player p, String message) {
		if (p==null) {
			Tools.AddLog(message);
		}
		else {
			p.sendMessage(message);
		}
	}


}
