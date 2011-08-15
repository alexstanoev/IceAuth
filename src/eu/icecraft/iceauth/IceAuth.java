package eu.icecraft.iceauth;

import java.io.File;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.alta189.sqlLibrary.MySQL.mysqlCore;
import com.alta189.sqlLibrary.SQLite.sqlCore;
//import com.nijikokun.bukkit.Permissions.Permissions;

public class IceAuth extends JavaPlugin {

	public String logPrefix = "[IceAuth] ";
	public Logger log = Logger.getLogger("Minecraft");
	public mysqlCore manageMySQL;
	public sqlCore manageSQLite;

	public Boolean MySQL = false;
	public String dbHost = null;
	public String dbUser = null;
	public String dbPass = null;
	public String dbDatabase = null;
	public String tableName;

	private ArrayList<Player> playersLoggedIn = new ArrayList<Player>();
	//private ArrayList<Player> notLoggedIn = new ArrayList<Player>();
	private Map<Player, Location> notLoggedIn = new HashMap<Player, Location>();
	private boolean useSpout;
	//private Permissions perm;
	//private boolean UseOP;
	private Thread thread;
	private String userField;
	private String passField;
	private MessageDigest md5;


	@Override
	public void onDisable() {

		try {
			thread.interrupt();
			thread.join();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}

		System.out.println(this.getDescription().getName() + " " + this.getDescription().getVersion() + " was disabled!");
	}

	@Override
	public void onEnable() {

		PluginManager pm = getServer().getPluginManager();

		/*
		Plugin perms = pm.getPlugin("Permissions");

		if (perms != null) {
			if (!pm.isPluginEnabled(perms)) {
				pm.enablePlugin(perms);
			}
			perm = (Permissions) perms;
		} else {
			UseOP  = true;
		}
		 */

		Plugin spLoaded = pm.getPlugin("Spout");

		if (spLoaded != null && pm.isPluginEnabled(spLoaded)) {
			System.out.println("[IceAuth] Found Spout, using inventory events.");
			useSpout = true;
		} else {
			System.out.println("[IceAuth] WARNING! Spout not found, inventories are unprotected!");
		}

		if(!this.getDataFolder().exists()) this.getDataFolder().mkdir();
		File confFile = new File(this.getDataFolder(), "config.yml");
		Configuration conf = new Configuration(confFile);
		if(!confFile.exists()) {

			conf.setProperty("mysql.use", false);
			conf.setProperty("mysql.dbHost", "localhost");
			conf.setProperty("mysql.dbUser", "root");
			conf.setProperty("mysql.dbPass", "");
			conf.setProperty("mysql.database", "minecraft");
			conf.setProperty("mysql.tableName", "auth");
			conf.setProperty("mysql.userField", "username");
			conf.setProperty("mysql.passField", "password");
			conf.save();

		}
		conf.load();

		// TODO: auto configuration

		this.MySQL = conf.getBoolean("mysql.use", false);
		this.dbHost = conf.getString("mysql.dbHost");
		this.dbUser = conf.getString("mysql.dbUser");
		this.dbPass = conf.getString("mysql.dbPass");
		this.dbDatabase = conf.getString("mysql.database");
		this.tableName = conf.getString("mysql.tableName");
		this.userField = conf.getString("mysql.userField");
		this.passField = conf.getString("mysql.passField");

		if (this.MySQL) {
			if (this.dbHost.equals(null)) { this.MySQL = false; this.log.severe(this.logPrefix + "MySQL is on, but host is not defined, defaulting to SQLite"); }
			if (this.dbUser.equals(null)) { this.MySQL = false; this.log.severe(this.logPrefix + "MySQL is on, but username is not defined, defaulting to SQLite"); }
			if (this.dbPass.equals(null)) { this.MySQL = false; this.log.severe(this.logPrefix + "MySQL is on, but password is not defined, defaulting to SQLite"); }
			if (this.dbDatabase.equals(null)) { this.MySQL = false; this.log.severe(this.logPrefix + "MySQL is on, but database is not defined, defaulting to SQLite"); }
		}

		if (this.MySQL) {

			this.manageMySQL = new mysqlCore(this.log, this.logPrefix, this.dbHost, this.dbDatabase, this.dbUser, this.dbPass);

			this.log.info(this.logPrefix + "MySQL Initializing");

			this.manageMySQL.initialize();

			try {
				if (this.manageMySQL.checkConnection()) {
					this.log.info(this.logPrefix + "MySQL connection successful");
					if (!this.manageMySQL.checkTable(tableName)) {
						this.MySQL = false;
					}
				} else {
					this.log.severe(this.logPrefix + "MySQL connection failed. Defaulting to SQLite");
					this.MySQL = false;
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			this.log.info(this.logPrefix + "SQLite Initializing");

			this.manageSQLite = new sqlCore(this.log, this.logPrefix, "IceAuth", this.getDataFolder().getPath());

			this.manageSQLite.initialize();

			if (!this.manageSQLite.checkTable(tableName)) {
				this.log.warning(this.logPrefix + "Table " + tableName + " does not exist! Disabling");
				pm.disablePlugin(this);
				// TODO: Fix
			}

		}

		try {
			this.md5 = MessageDigest.getInstance("MD5");
		} catch(NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		}

		IceAuthPlayerListener playerListener = new IceAuthPlayerListener(this);
		IceAuthBlockListener blockListener = new IceAuthBlockListener(this);
		IceAuthEntityListener entityListener = new IceAuthEntityListener(this);
		if(useSpout) {
			IceAuthSpoutListener spoutListener = new IceAuthSpoutListener(this);
			pm.registerEvent(Event.Type.CUSTOM_EVENT, spoutListener, Priority.Highest, this);
		}

		pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, playerListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.PLAYER_CHAT, playerListener, Priority.Highest, this);
		//pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Monitor, this); pm.registerEvent(Event.Type.PLAYER_LOGIN, playerListener, Priority.High, this);
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Low, this);
		pm.registerEvent(Event.Type.PLAYER_LOGIN, playerListener, Priority.High, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_KICK, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_PICKUP_ITEM, playerListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.PLAYER_DROP_ITEM, playerListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.ENTITY_TARGET, entityListener, Priority.Highest, this);

		thread = new Thread(new PlayerThread(this));
		thread.start();

		System.out.println("IceAuth v1.0 has been enabled. Forked thread: "+thread.getName());

	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		if(commandLabel.equalsIgnoreCase("register")) {
			if(!(sender instanceof Player)) {
				return false;
			}

			Player player = (Player) sender;
			if(isRegistered(player.getName())) {
				player.sendMessage(ChatColor.RED + "Already registered.");
				return false;
			}


			if(args.length != 1) {
				player.sendMessage("Usage: /register <password>");
				return false;
			}


			String password = args[0];

			if(!register(player.getName(), password)) {
				player.sendMessage(ChatColor.RED + "Something failed?");
				return false;
			}


			player.sendMessage(ChatColor.GREEN + "Registered successfully! Use /login <password>");

			return true;
		}

		if(commandLabel.equalsIgnoreCase("login") || commandLabel.equalsIgnoreCase("l")) {
			if(!(sender instanceof Player)) {
				return false;
			}
			Player player = (Player) sender;

			if(args.length != 1) {
				player.sendMessage("Usage: /login <password>");
				return false;
			}

			String playername = player.getName();
			String password = args[0];

			if(!isRegistered(player.getName())) {
				player.sendMessage(ChatColor.RED + "You need to register first!");
				return false;
			}

			if(checkAuth(player)) {
				player.sendMessage(ChatColor.RED + "Already logged in!");
				return false;
			}

			if(checkLogin(playername, password)) {
				player.sendMessage(ChatColor.GREEN + "Logged in successfully");
				delPlayerNotLoggedIn(player);
				addAuthPlayer(player);
				return true;
			} else {
				player.sendMessage(ChatColor.RED + "Wrong password!");
				return false;
			}

		}


		if(commandLabel.equalsIgnoreCase("changepassword")) {
			if(!(sender instanceof Player)) {
				return false;
			}
			//Player player = (Player) sender;
			/*
			if(!settings.ChangePasswordEnabled()) {
				player.sendMessage("Changing passwords is currently disabled!");
				return false;
			}
			if(!playercache.isPlayerRegistered(player)) {
				player.sendMessage(messages.getMessage("Error.NotRegistered"));
				return false;
			}
			if(!playercache.isPlayerAuthenticated(player)) {
				player.sendMessage(messages.getMessage("Error.NotLogged"));
				return false;
			}
			if(args.length != 2) {
				player.sendMessage(
						"Usage: /changepassword <oldpassword> <newpassword>");
				return false;
			}
			if(!comparePassword(args[0],
					data.loadHash(player.getName()))) {
				player.sendMessage(messages.getMessage("Error.WrongPassword"));
				return false;
			}

			String salt = Long.toHexString(
					Double.doubleToLongBits(Math.random()));
			boolean executed = data.updateAuth(player.getName(), pws.
					getSaltedHash(args[1], salt));

			if(!executed) {
				player.sendMessage(messages.getMessage("Error.DatasourceError"));
				MessageHandler.showError(
						"Failed to update an auth due to an error in the datasource!");
				return false;
			}

			player.sendMessage(messages.getMessage(
					"Command.ChangePasswordResponse"));
			MessageHandler.showInfo("Player " + player.getName()
					+ " changed his password!");
		}
			 */
			// TODO
		}
		return false;

	}

	public ResultSet query(String query) {

		ResultSet result = null;

		if (this.MySQL) {
			try {
				result = this.manageMySQL.sqlQuery(query);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			result = this.manageSQLite.sqlQuery(query);
		}

		return result;

	}

	public void addAuthPlayer(Player player) {
		playersLoggedIn.add(player);	
	}

	public boolean checkAuth(Player player) {
		return playersLoggedIn.contains(player);
	}

	public void removePlayerCache(Player player) {
		playersLoggedIn.remove(player);
	}

	public void addPlayerNotLoggedIn(Player player, Location loc) {
		notLoggedIn.put(player, loc);	
	}

	public void delPlayerNotLoggedIn(Player player) {
		notLoggedIn.remove(player);	
	}

	public String getMD5(String message) {
		byte[] digest;
		md5.reset();
		md5.update(message.getBytes());
		digest = md5.digest();

		return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1,
				digest));
	}

	public boolean isRegistered(String name) {

		ResultSet result = null;

		if (this.MySQL) {
			try {
				Connection connection = this.manageMySQL.getConnection();
				PreparedStatement regQ = connection.prepareStatement("SELECT COUNT(*) AS c FROM "+tableName+" WHERE " + userField + " = ?");
				regQ.setString(1, name);
				result = regQ.executeQuery();
				while(result.next()) {
					if(result.getInt("c") > 0) {
						return true;
					} else {
						return false;
					}
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				try {
					result.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

		} else {
			//result = this.manageSQLite.sqlQuery(query);
			// TODO: SQLite
		}

		return false;
	}

	public boolean checkLogin(String name, String password) {

		ResultSet result = null;

		if (this.MySQL) {
			try {
				Connection connection = this.manageMySQL.getConnection();
				PreparedStatement regQ = connection.prepareStatement("SELECT COUNT(*) AS c FROM "+tableName+" WHERE " + userField + " = ? && "+passField+" = ?");
				regQ.setString(1, name);
				regQ.setString(2, getMD5(password));
				result = regQ.executeQuery();
				while(result.next()) {
					if(result.getInt("c") > 0) {
						return true;
					} else {
						return false;
					}
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				try {
					result.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		} else {
			//result = this.manageSQLite.sqlQuery(query);
			// TODO: SQLite
		}

		return false;
	}

	public boolean register(String name, String password) {

		if (this.MySQL) {
			try {
				Connection connection = this.manageMySQL.getConnection();
				PreparedStatement regQ = connection.prepareStatement("INSERT INTO "+tableName+" ("+userField+", "+passField+") VALUES(?,?)");
				regQ.setString(1, name);
				regQ.setString(2, getMD5(password));
				regQ.executeUpdate();

				return true;

			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			//result = this.manageSQLite.sqlQuery(query);
			// TODO: SQLite
		}

		return false;
	}

	public boolean changePassword(String oldpass, String password) {
		// TODO: change password
		ResultSet result = null;

		if (this.MySQL) {
			try {
				Connection connection = this.manageMySQL.getConnection();
				PreparedStatement regQ = connection.prepareStatement("SELECT COUNT(*) FROM "+tableName+" WHERE " + userField + " = ? && "+passField+" = ?");
				regQ.setString(1, oldpass);
				//regQ.setString(2, getMD5(password));
				result = regQ.executeQuery();
				if(result.getInt(0) > 0) {
					return true;
				} else {
					return false;
				}

			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			//result = this.manageSQLite.sqlQuery(query);
			// TODO: SQLite
		}

		return false;
	}

	public boolean tpPlayers() {

		Set<Player> ks = notLoggedIn.keySet();
		for (Player player : ks) {

			Location pos = notLoggedIn.get(player);

			player.teleport(pos);
		}

		return true;
	}

}