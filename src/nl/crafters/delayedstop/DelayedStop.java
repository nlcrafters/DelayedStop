package nl.crafters.delayedstop;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.logging.Logger;

import javax.swing.Timer;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin; 
import org.bukkit.util.config.Configuration;
import com.nijiko.permissions.*;
import com.nijikokun.bukkit.Permissions.*;
import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.dataholder.worlds.WorldsHolder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;

public class DelayedStop extends JavaPlugin{
	int repeatingTask = 0;
	private String CHATPREFIX = ChatColor.RED + "[NLC]" + ChatColor.AQUA;
	private  Configuration config;
	private  Calendar timeStop;
	private final static char DEG = '\u00A7';
	private static PermissionHandler Permissions=null;
	private WorldsHolder wh = null;
	private int pSystem = 0;
	private boolean shuttingDown = false;
	private String reason = "";
	private boolean inPause = false; 
	private Timer timer;
	@Override
	public void onEnable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		AddLog("Enabling Delayed Stop v" + pdfFile.getVersion());
		getCommand("dstop").setExecutor(new CommandExecutor() {
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            	execCommand(sender, command, label, args);
        	    return true;
            }
        });
		CheckConfig();
		loadConfig();
		setupPermissions();
		AddLog("version "  + pdfFile.getVersion() + " enabled");
	}
	@Override
	public void onDisable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		AddLog("version "  + pdfFile.getVersion() + " disabled");
		pdfFile = null;
	}
	public void execCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		String cmd = command.getName();
		String action = "";
		Player player = null;
		int delay = 0;
		if (!cmd.equalsIgnoreCase("dstop"))
			return;
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
		if ( action.equalsIgnoreCase("")&& (checkP(player,"delayedstop.cancel") || checkP(player,"delayedstop.start"))  ) {
			Log(player,CHATPREFIX + " Usage: /dstop <minutes> or /dstop cancel");
			return;
			
		}
		else if ( action.equalsIgnoreCase("debug")&& checkP(player,"delayedstop.start")  ) {
			try {
				Long timeLeft = (timeStop.getTimeInMillis()-Calendar.getInstance().getTimeInMillis()) / 1000;
				Log(player,"Time left :" + timeLeft);
			} catch (Exception e) {}
			Log(player,"Countdown enabled:" + repeatingTask );
			Log(player,"Time left:" + getTimeLeft() );
			Log(player,"Current notifications:" + config.getString("notification.notify-at","10m,5m,1m,30s,10s,5s"));
		}
		else if ( action.equalsIgnoreCase("pause")&& checkP(player,"delayedstop.start")  ) {
			try {
				inPause = true;
				this.getServer().broadcastMessage(CHATPREFIX + " " + getMessage("broadcasttext.restart-paused-message","Server restart is paused!"));
			} catch (Exception e) {}
			Log(null,"Server restart paused");
		}
		else if ( ( action.equalsIgnoreCase("go") || action.equalsIgnoreCase("resume")) && checkP(player,"delayedstop.start")  ) {
			try {
				inPause = false;
				this.getServer().broadcastMessage(CHATPREFIX + " " + getMessage("broadcasttext.restart-resumed-message","Server restart is resumed!"));
			} catch (Exception e) {}
			Log(null,"Server restart resumed");
		}
		else if ( ( action.equalsIgnoreCase("cancel") || action.equalsIgnoreCase("off")) && checkP(player,"delayedstop.cancel")  ) {
			if (repeatingTask==0) {
				Log(player,CHATPREFIX + " No delayed stop in progress");
				return;
			}
			AddLog("Cancelling restart " + repeatingTask);
			timer.stop();
			repeatingTask = 0;
			this.getServer().broadcastMessage(CHATPREFIX + " " + getMessage("broadcasttext.restart-cancelled-message","Server restart is cancelled!"));
			delay=0;
		}
		else if (action.equalsIgnoreCase("force") && checkP(player,"delayedstop.start")) {
			AddLog("Saving player data");
			this.getServer().savePlayers();
            for(org.bukkit.World w : this.getServer().getWorlds())
            {
            	AddLog("Saving world data" + w.getName());
                w.save();
            }
            AddLog("Kicking " + this.getServer().getOnlinePlayers().length + " players");
            for(org.bukkit.entity.Player p : this.getServer().getOnlinePlayers())
            {
                p.kickPlayer("Server is shutting down...");
            }
            
            AddLog("Disabling plugins");
            for (Plugin p : this.getServer().getPluginManager().getPlugins()) {
            	if (!p.equals(this)) {
            		p.onDisable();	
            	}
            }
            AddLog("Stopping server...");
            System.exit(0);				
		}
		else if (action.equalsIgnoreCase("save") && checkP(player,"delayedstop.start")) {
			
			Log(player,"Saving player data");
			this.getServer().savePlayers();
            for(org.bukkit.World w : this.getServer().getWorlds())
            {
            	Log(player,"Saving world data for world.." + w.getName());
                w.save();
                Log(player,"Done saving world  " + w.getName());
            }
            Log(player,"Done saving player data");
		}	
		else if (action.equalsIgnoreCase("start") && checkP(player,"delayedstop.start")) {
			if (delay==0) {
				return;
			}
			if (repeatingTask!=0) 
			{
				Log(player,CHATPREFIX + " Delayed stop already in progress!");
				return;
			}
			reason = "";
			if (args.length > 1) {
				for (int i=1;i<args.length;i++) {
					if (reason.equalsIgnoreCase("")) 
						reason = args[i];
					else 
						reason = reason + " " + args[i];
				}
				reason = " (" + reason + ")";
			}
			timeStop = Calendar.getInstance();
			timeStop.add(Calendar.SECOND, delay);
			this.getServer().broadcastMessage(CHATPREFIX + " " + getMessage("broadcasttext.time-left-message","") + reason);
			AddLog(getMessage("broadcasttext.time-left-message",""));
			
			// New timer test
			timer = new Timer(1000,taction);
			timer.start();
			repeatingTask = 1;

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
		
	}
	ActionListener taction = new ActionListener ()
    {
      public void actionPerformed (ActionEvent event)
      {
    	  TimeTick();
      }
    };	
    
	private String getTimeLeft() {
		Long timeLeft;
		try {
			timeLeft = (timeStop.getTimeInMillis()-Calendar.getInstance().getTimeInMillis()) / 1000;
		} catch (Exception e) {
			return "";
		}
		Long Minutes = (timeLeft / 60);
		Long Seconds = (timeLeft - (Minutes*60));
		String oneSecond = config.getString("broadcasttext.second-text");
		String oneMinute = config.getString("broadcasttext.minute-text");
		String moreSeconds = config.getString("broadcasttext.seconds-text",oneSecond);
		String moreMinutes = config.getString("broadcasttext.minutes-text",oneMinute);
		String andText  = config.getString("broadcasttext.and-text","and");
		if (Minutes==0) {
			if (Seconds>1) 
				return Seconds +  " " + moreSeconds;
			else
				return Seconds +  " " + oneSecond;
		}
		else {
			if (Seconds==0) {
				if (Minutes>1)
					return Minutes + " " + moreMinutes;
				else
					return Minutes + " " + oneMinute;
			}
			else {
				if (Minutes>1 && Seconds > 1) {
						return Minutes + " " + moreMinutes + " " + andText + " " + Seconds + "  " + moreSeconds;	
				}
				else if (Minutes==1 && Seconds>1) {
					return Minutes + " " + oneMinute + " " + andText + " " + Seconds + "  " + moreSeconds;	
				}
				else { 
					return Minutes + " " + oneMinute + " " + andText + " " + Seconds + "  " + oneSecond;	
				}
			}
		}
	}
	private void TimeTick() {
		if (inPause) {
			timeStop.add(Calendar.SECOND,1);
			return;
		}
		Long timeLeft = (timeStop.getTimeInMillis()-Calendar.getInstance().getTimeInMillis()) / 1000;
		Long Minutes = (timeLeft / 60);
		Long Seconds = (timeLeft - (Minutes*60));
		int numsecs = 0;
		String strNotifiers[] = config.getString("notification.notify-at","10m,5m,1m,30s,10s,5s").split(",");
		for (String s: strNotifiers) {
			if (isInteger(s)) { // Just seconds
				numsecs = Integer.parseInt(s);
			}
			else if (s.contains("s")) { // Just seconds
				numsecs = (Integer.parseInt(s.replaceAll("s","" )));
			}
			else if (s.contains("m")) { // just minutes
				numsecs = (Integer.parseInt(s.replaceAll("m","" )) * 60);
			}
			if (timeLeft==numsecs) {
				this.getServer().broadcastMessage(CHATPREFIX + " " + 
						  getMessage("broadcasttext.time-left-message","") + reason);
				AddLog(getMessage("broadcasttext.time-left-message","") + reason);
			}
		}
		if (Minutes<=0 && Seconds<=0)  {
			shutDown();
		}
		

	}
	private void shutDown() {
		if (!shuttingDown) {
			shuttingDown = true;
			String msg = getMessage("broadcasttext.server-down-message","") + reason;

			AddLog("Kicking " + this.getServer().getOnlinePlayers().length + " players");
			for(Player p : this.getServer().getOnlinePlayers())
			{
	            try 
	            {
	            	p.kickPlayer(msg);
	            }
	            catch (Exception e) {}
			}
            AddLog("Shutting down");
			timer.stop();
			repeatingTask=0;
			((CraftServer) this.getServer()).getServer().a();
		}
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
	private void AddLog(String message) {
    	Logger.getLogger("Minecraft").info("[DelayedStop] " + message);
    }
	public void Log(Player p, String message) {
		if (p==null) {
			AddLog(message);
		}
		else {
			p.sendMessage(message);
		}
	}

	private void CheckConfig() {
		getDataFolder().mkdirs();
		String name = "config.yml";
		File actual = new File(getDataFolder(),name);
		if (!actual.exists()) {
		    InputStream input =this.getClass().getResourceAsStream("/config.yml");
		    if (input != null) {
		        FileOutputStream output = null;
		        try {
		            output = new FileOutputStream(actual);
		            byte[] buf = new byte[8192];
		            int length = 0;
		            while ((length = input.read(buf)) > 0) {
		                output.write(buf, 0, length);
		            }
		            AddLog("Default configuration file written: " + name);
		        } catch (IOException e) {
		            e.printStackTrace();
		        } finally {
		            try {
		                if (input != null)
		                    input.close();
		            } catch (IOException e) {}
	
		            try {
		                if (output != null)
		                    output.close();
		            } catch (IOException e) {}
		        }
		    }            
		}
	}
	private void loadConfig() {
    	
    	config = this.getConfiguration();
    	config.load();
		CHATPREFIX = getColor("labelsandcolors.chat-prefix-color") + 
		             getMessage("labelsandcolors.chat-prefix","") +
		             getColor("labelsandcolors.chat-message-color");
		//JailTime = config.getInt("settings.jail-time",-1);
    }	
	private String getMessage(String configMessage, String defaultMessage) {
		config.load();
		String t = "";
		if (defaultMessage.equalsIgnoreCase(""))
			t = config.getString(configMessage);
		else
			t = config.getString(configMessage,defaultMessage);
			
		if (t.indexOf("@")==-1) {
			return t;
		}
		t = t.replaceAll("@time-left@", getColor("labelsandcolors.chat-highlighted-color") + getTimeLeft() + getColor("labelsandcolors.chat-message-color")  );
		return t;
	}
	private String getColor(String configColor) 
	{
		String col =config.getString(configColor); 
		String out = "";
		if (col.length()==2) {
			out = DEG + col.substring(1);
			return out;
		}
		return col;
	}	
	private void setupPermissions() {
		// first search for groupmanager
		Plugin p = this.getServer().getPluginManager().getPlugin("GroupManager");
		pSystem =0;
		if (p!=null) {
			if (!this.getServer().getPluginManager().isPluginEnabled("GroupManager")) {
				this.getServer().getPluginManager().enablePlugin(p);
			}
			GroupManager gm = (GroupManager) p;
			wh = gm.getWorldsHolder();
			pSystem = 1;
			AddLog("GroupManager system found");
		}
		else  // Then search for permissions
		{
			p = this.getServer().getPluginManager().getPlugin("Permissions");
			if(Permissions == null) {
			    if(p!= null) {
			    	Permissions = ((Permissions)p ).getHandler();
			    	pSystem = 2;
			    	AddLog("Permission system found");
			    } 
			}
		}
	}	
	// Check permissions simplified
	private boolean checkP(Player p, String perm) {
		if (p==null) {
			return true;
		}
		else {
			if (pSystem==0) {
				return (p.isOp());
			}
			else if (pSystem==1) {
				 return wh.getWorldPermissions(p).has(p,perm);
			}
			else if (pSystem==2) {
				if (Permissions.has(p,perm)) {
					return true;
				}
			}
		}
		return false;
	}

}
