package nl.crafters.delayedstop;

import java.util.Calendar;

import javax.swing.Timer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class DelayedStopCommand implements CommandExecutor {
	private final DelayedStop plugin;
	public DelayedStopCommand(DelayedStop plugin) {
        this.plugin = plugin;
    }
	
	@Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		String cmd = command.getName();
		String action = "";
		Player player = null;
		int delay = 0;
		if (!cmd.equalsIgnoreCase("dstop"))
			return false;
		
		if (sender instanceof Player){
	     	player = (Player)sender;
	    }
	    else {
	    	player = null;
	    }
		try {
			action = args[0].toLowerCase();
		} catch (Exception e) {
			action = "";
		}
		if (isInteger(action)) {
			delay = Integer.parseInt(action);
			action = "start";
		}
		if ( action.equalsIgnoreCase("")&& (plugin.checkP(player,"delayedstop.cancel") || plugin.checkP(player,"delayedstop.start"))  ) {
			plugin.Log(player,plugin.CHATPREFIX + " Usage: /dstop <minutes> or /dstop cancel");
			return false;
			
		}
		else if ( action.equalsIgnoreCase("debug")&& plugin.checkP(player,"delayedstop.start")  ) {
			try {
				Long timeLeft = (plugin.timeStop.getTimeInMillis()-Calendar.getInstance().getTimeInMillis()) / 1000;
				plugin.Log(player,"Time left :" + timeLeft);
			} catch (Exception e) {}
			plugin.Log(player,"Countdown enabled:" + plugin.repeatingTask );
			plugin.Log(player,"Time left:" + plugin.getTimeLeft() );
			plugin.Log(player,"Current notifications:" + plugin.config.getString("notification.notify-at","10m,5m,1m,30s,10s,5s"));
		}
		else if ( action.equalsIgnoreCase("pause")&& plugin.checkP(player,"delayedstop.start")  ) {
			try {
				plugin.inPause = true;
				plugin.getServer().broadcastMessage(plugin.CHATPREFIX + " " + plugin.getMessage("broadcasttext.restart-paused-message","Server restart is paused!"));
			} catch (Exception e) {}
			plugin.Log(null,"Server restart paused");
		}
		else if ( ( action.equalsIgnoreCase("go") || action.equalsIgnoreCase("resume")) && plugin.checkP(player,"delayedstop.start")  ) {
			try {
				plugin.inPause = false;
				plugin.getServer().broadcastMessage(plugin.CHATPREFIX + " " + plugin.getMessage("broadcasttext.restart-resumed-message","Server restart is resumed!"));
			} catch (Exception e) {}
			plugin.Log(null,"Server restart resumed");
		}
		else if ( ( action.equalsIgnoreCase("cancel") || action.equalsIgnoreCase("off")) && plugin.checkP(player,"delayedstop.cancel")  ) {
			if (plugin.repeatingTask==0) {
				plugin.Log(player,plugin.CHATPREFIX + " No delayed stop in progress");
				return true;
			}
			plugin.AddLog("Cancelling restart " + plugin.repeatingTask);
			plugin.timer.stop();
			plugin.repeatingTask = 0;
			plugin.getServer().broadcastMessage(plugin.CHATPREFIX + " " + plugin.getMessage("broadcasttext.restart-cancelled-message","Server restart is cancelled!"));
			delay=0;
		}
		else if (action.equalsIgnoreCase("force") && plugin.checkP(player,"delayedstop.start")) {
			plugin.AddLog("Saving player data");
			plugin.getServer().savePlayers();
            for(org.bukkit.World w : plugin.getServer().getWorlds())
            {
            	plugin.AddLog("Saving world data" + w.getName());
                w.save();
            }
            plugin.AddLog("Kicking " + plugin.getServer().getOnlinePlayers().length + " players");
            for(org.bukkit.entity.Player p : plugin.getServer().getOnlinePlayers())
            {
                p.kickPlayer("Server is shutting down...");
            }
            
            plugin.AddLog("Disabling plugins");
            for (Plugin p : plugin.getServer().getPluginManager().getPlugins()) {
            	if (!p.equals(this)) {
            		p.onDisable();	
            	}
            }
            plugin.AddLog("Stopping server...");
            System.exit(0);				
		}
		else if (action.equalsIgnoreCase("save") && plugin.checkP(player,"delayedstop.start")) {
			
			plugin.Log(player,"Saving player data");
			plugin.getServer().savePlayers();
            for(org.bukkit.World w : plugin.getServer().getWorlds())
            {
            	plugin.Log(player,"Saving world data for world.." + w.getName());
                w.save();
                plugin.Log(player,"Done saving world  " + w.getName());
            }
            plugin.Log(player,"Done saving player data");
		}	
		else if (action.equalsIgnoreCase("start") && plugin.checkP(player,"delayedstop.start")) {
			if (delay==0) {
				return true;
			}
			if (plugin.repeatingTask!=0) 
			{
				plugin.Log(player,plugin.CHATPREFIX + " Delayed stop already in progress!");
				return true;
			}
			plugin.reason = "";
			if (args.length > 1) {
				for (int i=1;i<args.length;i++) {
					if (plugin.reason.equalsIgnoreCase("")) 
						plugin.reason = args[i];
					else 
						plugin.reason = plugin.reason + " " + args[i];
				}
				plugin.reason = " (" + plugin.reason + ")";
			}
			plugin.timeStop = Calendar.getInstance();
			plugin.timeStop.add(Calendar.SECOND, delay);
			plugin.getServer().broadcastMessage(plugin.CHATPREFIX + " " + plugin.getMessage("broadcasttext.time-left-message","") + plugin.reason);
			plugin.AddLog(plugin.getMessage("broadcasttext.time-left-message",""));
			
			// New timer test
			plugin.timer = new Timer(1000,plugin.taction);
			plugin.timer.start();
			plugin.repeatingTask = 1;
			/* OLD TIMER
			repeatingTask = this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() 
			{
				@Override
				public void run() {
					TimeTick();
				}
			}, 0, 1*20);
			*/
		}
		return true;        
	}
	private boolean isInteger( String input ) {  
		try {  
			Integer.parseInt( input );  
			return true;  
		}  
		catch(Exception e) {  
			return false;  
		}  
	}  	
	
}
