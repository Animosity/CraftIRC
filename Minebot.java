import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.Properties;
import org.jibble.pircbot.*;


public class Minebot extends PircBot 
{
    private static Minebot instance = null;
	protected static final Logger log = Logger.getLogger("Minecraft");
	Properties ircSettings = new Properties();
	String ircSettingsFilename = "CraftIRC.settings";
	private static final Map<String,String> colorMap = new HashMap<String,String>();
	
	String cmd_prefix;
	String irc_relayed_user_color;
	public String irc_handle;
	
	String irc_server, irc_server_port, irc_server_pass;
	String irc_auth_method, irc_auth_username, irc_auth_pass;
	String irc_channel, irc_channel_pass, irc_admin_channel, irc_admin_channel_pass;
	Boolean irc_server_ssl = false;
	
	ArrayList<String> optn_main_req_prefixes = new ArrayList<String>(); // require IRC user have +/%/@/&/~ -- NOT IMPLEMENTED
	ArrayList<String> optn_admin_req_prefixes = new ArrayList<String>(); // require IRC user (admin) have +/%/@/&/~

	ArrayList<String> optn_main_send_events = new ArrayList<String>();  // which MC events to send to main IRC channel
	ArrayList<String> optn_admin_send_events = new ArrayList<String>(); // which MC events to send to admin IRC channel
	
	ArrayList<String> optn_send_all_MC_chat = new ArrayList<String>();  // where to send MC chat
	
	ArrayList<String> optn_send_all_IRC_chat; // send IRC chat to MC? - now channel sources are selectable
	String optn_notify_admins_cmd;
	int bot_timeout = 5000; // how long to wait after joining channels to wait for the bot to check itself
	
	User[] irc_users_main;
	User[] irc_users_admin;
	
	protected Minebot() 
	{
	    
	}
	
	public static Minebot getInstance()
	{
	   if (instance == null)
	   {
	       instance = new Minebot();
	   }
	   return instance;
	}
	
	public void init() 
	{
	       this.initColorMap();
	        try { ircSettings.load(new FileInputStream(ircSettingsFilename)); }
	        catch (IOException e) {
	            log.info(CraftIRC.NAME + " - Error while READING settings file " + ircSettingsFilename);
	            e.printStackTrace();
	        }
	        
	        try 
	        {
	            cmd_prefix = ircSettings.getProperty("command-prefix");
	            
	            if (colorMap.containsKey(ircSettings.getProperty("irc-relayed-user-color").toLowerCase()))
	            {
	                irc_relayed_user_color = colorMap.get(ircSettings.getProperty("irc-relayed-user-color").toLowerCase());
	            }
	            else { irc_relayed_user_color = colorMap.get("white");  }
	                        
	            irc_handle = ircSettings.getProperty("irc-handle");
	            
	            irc_server = ircSettings.getProperty("irc-server");
	            irc_server_port = ircSettings.getProperty("irc-server-port");
	            irc_server_pass = ircSettings.getProperty("irc-server-password");
	            irc_server_ssl = Boolean.parseBoolean(ircSettings.getProperty("irc-server-ssl"));
	            
	            irc_auth_method = ircSettings.getProperty("irc-auth-method");
	            irc_auth_username = ircSettings.getProperty("irc-auth-username");
	            irc_auth_pass = ircSettings.getProperty("irc-auth-password");
	            
	            irc_channel = ircSettings.getProperty("irc-channel");
	            irc_channel_pass = ircSettings.getProperty("irc-channel-password");
	            irc_admin_channel = ircSettings.getProperty("irc-admin-channel");
	            irc_admin_channel_pass = ircSettings.getProperty("irc-admin-channel-password");
	            optn_send_all_MC_chat = this.getChatRelayChannels(ircSettings.getProperty("send-all-chat".toLowerCase()), "send-all-chat");
	            optn_send_all_IRC_chat = this.getChatRelayChannels(ircSettings.getProperty("send-all-IRC".toLowerCase()), "send-all-IRC");
	            optn_main_send_events = this.getEventVerbosity(ircSettings.getProperty("send-events"));
	            optn_admin_send_events = this.getEventVerbosity(ircSettings.getProperty("admin-send-events"));
	            
	            if (ircSettings.containsKey("notify-admins-cmd"))
	            {
	                optn_notify_admins_cmd  = ircSettings.getProperty("notify-admins-cmd");
	                if (optn_notify_admins_cmd.length() < 1) 
	                {
	                    log.info(CraftIRC.NAME + " - no notify-admins-cmd set, disabling admin notification command.");
	                    optn_notify_admins_cmd = null;
	                }
	                else 
	                {
	                    if (!optn_notify_admins_cmd.startsWith("/")) { optn_notify_admins_cmd = "/" + optn_notify_admins_cmd; }
	                }
	            }
	            
	            // get the 'check' delay from properties
	            if (ircSettings.containsKey("bot-timeout"))
	            {
	                try { this.bot_timeout = 1000*Integer.parseInt(ircSettings.getProperty("bot-timeout")); } // get input in seconds, convert to ms
	                catch (Exception e) { this.bot_timeout = 5000; }
	                
	            }
	            
	            if (ircSettings.containsKey("irc-admin-prefixes"))
	            {
	                this.optn_admin_req_prefixes = getRequiredPrefixes(ircSettings.getProperty("irc-admin-prefixes"));
	                
	            }
	        }
	            
	        catch (Exception e)
	        {
	            log.info(CraftIRC.NAME + " - Error while LOADING settings from " + this.ircSettingsFilename);
	            e.printStackTrace();
	        }
	        if (irc_handle.isEmpty()) { this.irc_handle = "minecraftbot"; }
	        this.setName(this.irc_handle);
	        this.setFinger(CraftIRC.NAME + " v" + CraftIRC.VERSION);
	        this.setLogin(CraftIRC.NAME);
	        this.setVersion(CraftIRC.NAME + " v" + CraftIRC.VERSION);
	        
	        /*
	        this.handle = botname;
	        this.setName(botname);
	        this.setFinger("CraftIRC - Minecraft bot");
	        this.setLogin("minecraftbot");
	        this.server_info = irc_server_info;
	        this.auth_info = irc_auth_info;
	        this.main_channel = irc_channel;
	        this.cmd_prefix = cmd_prefix;
	         */
	        

            try {
                start();
            } 
            catch (Exception e) {
                e.printStackTrace();
            }
            

   }

	private void initColorMap() {
		colorMap.put("black", Colors.Black);
		colorMap.put("navy", Colors.Navy);
		colorMap.put("green", Colors.Green);
		colorMap.put("blue", Colors.Blue);
		colorMap.put("red", Colors.Red);
		colorMap.put("purple", Colors.Purple);
		colorMap.put("gold", Colors.Gold);
		colorMap.put("lightgray", Colors.LightGray);
		colorMap.put("gray", Colors.Gray);
		colorMap.put("darkpurple", Colors.DarkPurple);
		colorMap.put("lightgreen", Colors.LightGreen);
		colorMap.put("lightblue", Colors.LightBlue);
		colorMap.put("rose", Colors.Rose);
		colorMap.put("lightpurple", Colors.LightPurple);
		colorMap.put("yellow", Colors.Yellow);
		colorMap.put("white", Colors.White);
	}


	
	private ArrayList<String> getRequiredPrefixes(String csv_prefixes)
	{
		return new ArrayList<String>(Arrays.asList(csv_prefixes.split(",")));
	}
	
	
	// Sets the directionality for MC->IRC chat (channels are targets)
	// And also sets the channel sources for IRC->MC chat
	private ArrayList<String> getChatRelayChannels(String csv_relay_channels, String propertyName)
	// Modified to deal w/ send-all-IRC, in addition to the existing send-all-chat support
	{
		ArrayList<String> relayChannels = new ArrayList<String>(Arrays.asList(csv_relay_channels.toLowerCase().split(",")));
		
		// backward compatibility w/ boolean argument of past
		if (relayChannels.contains("true"))
		{
			relayChannels.remove("true");
			relayChannels.add("main");
			relayChannels.add("admin");
		}
		
		if ((!relayChannels.contains("main") && !relayChannels.contains("admin")))
		{ 
			log.info(CraftIRC.NAME + " - No valid Minecraft chat relay channels set, disabling feature \"" + propertyName + "\"");
			return new ArrayList<String>(Arrays.asList(""));
		}
		return new ArrayList<String>(Arrays.asList(csv_relay_channels.toLowerCase().split(",")));
	}
	
	
	private ArrayList<String> getEventVerbosity(String csv_send_events)
	{
		return new ArrayList<String>(Arrays.asList(csv_send_events.toLowerCase().split(",")));
	}
	
	
	public void start()
	{
		
			log.info(CraftIRC.NAME + " v" + CraftIRC.VERSION + " loading.");

     		if (this.irc_server_port == null || this.irc_server_port.equals("")) 
     		{
				if (this.irc_server_ssl) 
				{
					this.irc_server_port = "6697";
				} 
				else 
				{
                    this.irc_server_port = "6667";
                }
        	}

			try {
				this.setAutoNickChange(true);

				if (this.irc_server_ssl) 
				{
	                		log.info(CraftIRC.NAME + " - Connecting to " 
	                        		+ this.irc_server + ":"
	                        		+ this.irc_server_port + " [SSL]");
	                		this.connect(this.irc_server,
	                        		Integer.parseInt(this.irc_server_port),
	                        		this.irc_server_pass, new TrustingSSLSocketFactory());
	            } 
				else 
           		{
            		log.info(CraftIRC.NAME + " - Connecting to " 
               			+ this.irc_server + ":"
                    	+ this.irc_server_port);
            		this.connect(this.irc_server,
                		Integer.parseInt(this.irc_server_port),
                		this.irc_server_pass);
            	}

        		if (this.isConnected()) 
        		{
            		log.info(CraftIRC.NAME + " - Connected");
        		} 
        		else 
        		{
            		log.info(CraftIRC.NAME + " - Connection failed!");
        		}

				this.authenticateBot(); // will always GHOST own registered nick if auth method is nickserv
				
				this.joinChannel(irc_channel, irc_channel_pass);
				this.joinAdminChannel();
				Thread.sleep(this.bot_timeout); // known to get ahead of the bot actually joining the channels
				this.checkChannels(); 
				
				
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (NickAlreadyInUseException e) {
				System.out.println("my handle is taken!");
				this.authenticateBot();
				this.joinChannel(irc_channel, irc_channel_pass);
				this.joinAdminChannel();
				
				try { 
					Thread.sleep(2000); // known to get ahead of the bot actually joining the channels
				} catch (InterruptedException e1) {	e1.printStackTrace(); } 
				
				this.checkChannels();
				
			} catch (IOException e) {
				e.printStackTrace();
				
			} catch (IrcException e) {
				e.printStackTrace();
				
			} catch (Exception e) {
				e.printStackTrace();			
			}

	}
	
	void authenticateBot()
	{
		if (this.irc_auth_method.equalsIgnoreCase("nickserv") && !irc_auth_pass.isEmpty())
		{
			log.info(CraftIRC.NAME + " - Using Nickserv authentication.");
			this.sendMessage("nickserv", "GHOST " + irc_handle + " " + irc_auth_pass);
			//this.setAutoNickChange(false);
			
			// Some IRC servers have quite a delay when ghosting...
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			this.changeNick(irc_handle);
			this.identify(irc_auth_pass);

			
		}
		if (this.irc_auth_method.equalsIgnoreCase("gamesurge"))
		{
			log.info(CraftIRC.NAME + " - Using GameSurge authentication.");
			this.changeNick(irc_handle);
			this.sendMessage("AuthServ@Services.GameSurge.net", "AUTH " + irc_auth_username + " " + irc_auth_pass);
		}
		
	}
	
	public void joinAdminChannel() 
	{
		if (irc_admin_channel == null || irc_admin_channel.equals("")) 
		{ 
			optn_admin_send_events.clear(); // clear any event option because we don't have anywhere to send them
			return; 
		}
		
		else 
		{ 
			this.joinChannel(irc_admin_channel, irc_admin_channel_pass); 
		}
		
	}
	
	
	// Determine which of the selected channels the bot is actually present in - disable features if not in the required channels.
	void checkChannels()
	{
		ArrayList<String> botChannels = new ArrayList<String>(Arrays.asList(this.getChannels()));
		
		if (!botChannels.contains(this.irc_channel)) 
		{ 
			log.info(CraftIRC.NAME + " - " + this.getNick() + " not in main channel: " + this.irc_channel + ", disabling all events for channel");
			this.optn_main_send_events.clear(); 
			this.optn_send_all_IRC_chat.remove("main");
			this.optn_send_all_MC_chat.remove("main");
			this.optn_main_req_prefixes.clear();
			
		} else { log.info(CraftIRC.NAME + " - Joined main channel: " + this.irc_channel); }
		
		if (!botChannels.contains(this.irc_admin_channel)) 
		{ 

			log.info(CraftIRC.NAME + " - " + this.getNick() + " not in admin channel: " + this.irc_admin_channel + ", disabling all events for channel");
			this.optn_admin_send_events.clear();
			this.optn_send_all_IRC_chat.remove("admin");
			this.optn_notify_admins_cmd = null;
			this.optn_admin_req_prefixes.clear();

		} else { log.info(CraftIRC.NAME + " - Joined admin channel: " + this.irc_admin_channel); }
		
	}
	
	
	// Update users
	public void onJoin(String channel, String sender, String login, String hostname) 
	{
		if (channel.equalsIgnoreCase(this.irc_channel)) { this.irc_users_main = this.getUsers(channel); }
		if (channel.equalsIgnoreCase(this.irc_admin_channel)) { this.irc_users_admin = this.getUsers(channel); }
	}
		
	// Update users
	public void onPart(String channel, String sender, String login, String hostname) 
	{
		if (channel.equalsIgnoreCase(this.irc_channel)) { this.irc_users_main = this.getUsers(channel); }
		if (channel.equalsIgnoreCase(this.irc_admin_channel)) { this.irc_users_admin = this.getUsers(channel); }
	}
	
	
	public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) 
	{
		if (recipientNick.equalsIgnoreCase(this.getNick())) 
		{ 
			if (channel.equalsIgnoreCase(this.irc_channel)) { this.joinChannel(this.irc_channel, this.irc_channel_pass); }
			if (channel.equalsIgnoreCase(this.irc_admin_channel)) { this.joinChannel(this.irc_admin_channel, this.irc_admin_channel_pass); }
		}
	}
	
	
	// IRC commands parsed here
	public void onMessage(String channel, String sender, String login, String hostname, String message) 
	{
		// Parse admin commands here
		if (channel.equalsIgnoreCase(this.irc_admin_channel) && userAuthorized(channel, sender))
		{

			String[] splitMessage = message.split(" ");
			if (message.startsWith(cmd_prefix + "kick"))
			{

				log.info(CraftIRC.NAME + " - " + channel + " - " + sender + " used: " + this.combineSplit(0, splitMessage, " "));
				
				etc.getServer().useConsoleCommand("kick " + this.combineSplit(1, splitMessage, " "));
			}
			
			if (message.startsWith(cmd_prefix + "ban"))
			{

				log.info(CraftIRC.NAME + " - " + channel + " - " + sender + " used: " + this.combineSplit(0, splitMessage, " "));
				
				etc.getServer().useConsoleCommand("ban " + this.combineSplit(1, splitMessage, " "));
			}
			
			if (message.startsWith(cmd_prefix + "broadcast"))
			{

				log.info(CraftIRC.NAME + " - " +  channel + " - " + sender + " used: " + this.combineSplit(0, splitMessage, " "));
				
				etc.getServer().useConsoleCommand("say " + this.combineSplit(1, splitMessage, " "));
			}

			
		}

		
		
		if (message.startsWith(cmd_prefix + "players")) 
		{
			String playerlist = this.getPlayerList();
			this.sendMessage(channel, playerlist); //set this to reply to the channel it was requested from
			return;
		}
		
		
		if (channel.equalsIgnoreCase(this.irc_channel) && this.optn_send_all_IRC_chat.contains("main"))
		{
			if (message.startsWith(cmd_prefix)) { return; } // don't send command messages to MC 
			msgToGame(sender, message);
		}
		
		else if (channel.equalsIgnoreCase(this.irc_admin_channel) && this.optn_send_all_IRC_chat.contains("admin"))
		{
			if (message.startsWith(cmd_prefix)) { return; } // don't send command messages to MC 
			msgToGame(sender, message);
		}
	
		else 
		{
			if (message.startsWith(cmd_prefix + "say")) 
			{
				message = message.substring(message.indexOf(" ")).trim();
				msgToGame(sender, message);
			}
		}

				
	}
	
	
	// IRC user authorization check against prefixes
	// Currently just for admin channel as first-order level of security
	
	public boolean userAuthorized(String channel, String user)
	{
		if (channel.equalsIgnoreCase(this.irc_admin_channel))
		{
			User[] adminUsers = (User[])super.getUsers(channel).clone(); // I just want a copy of it god damnit.
			
			for(int i = 0; i < adminUsers.length; i++)
			{
				User iterUser = adminUsers[i];
				if (iterUser.getNick().equalsIgnoreCase(user) && this.optn_admin_req_prefixes.contains(iterUser.getPrefix()))
				{
					return true;
				}
			}
			
		}
		
		return false;
	}
	
	// Form and broadcast messages to Minecraft
	public void msgToGame(String sender, String message)
	{
       String msg_to_broadcast = (new StringBuilder())
							       .append("[IRC]")
							       .append(" <").append(irc_relayed_user_color).append(sender).append(Colors.White).append("> ")
							       .append(message).toString();

       for (Player p : etc.getServer().getPlayerList())
       {
		    if(p != null)
		    {
		        p.sendMessage(msg_to_broadcast);
		    }
       }
       
	}
	
	
	// Return the # of players and player names on the Minecraft server
	private String getPlayerList() 
	{
		Iterator i$ = etc.getServer().getPlayerList().iterator();
		Integer playercount = 0;
		Integer maxplayers = etc.getInstance().getPlayerLimit();
		StringBuilder sb = new StringBuilder();
	
	    do
        {
            if(!i$.hasNext())
                break;
            Player p = (Player)i$.next();
            if(p != null)
            {
            	playercount++;
                sb.append(" ").append(p.getName());
            }
        } while(true);
	  
	   
	  if (playercount > 0) { return "Online (" + playercount + "/" + maxplayers + "): " + sb.toString(); }
	  else { return "nobody is minecrafting right now"; }
	}


	
	public String combineSplit(int initialPos, String[] parts, String delimiter) throws ArrayIndexOutOfBoundsException 
	{
		   String result = "";
		   for(int i = initialPos; i < parts.length; i++)
		   {
		      result = result + parts[i];
		      if (i != parts.length - 1) { result = result + delimiter; }
		   }
		   return result;
	}
	
	public void onDisconnect()
	{
		
	    // Maybe check if disabled, and if not, start(); depending on a flag set in the settings?
	}
	
} // EO Minebot
