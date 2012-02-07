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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.alta189.sqlLibrary.MySQL.mysqlCore;
import com.alta189.sqlLibrary.SQLite.sqlCore;

@SuppressWarnings("deprecation")
public class IceAuth extends JavaPlugin {
	public String logPrefix = "[IceAuth] ";
	public Logger log = Logger.getLogger("Minecraft");
	public mysqlCore manageMySQL;
	public sqlCore manageSQLite;

	public boolean giveKits;
	public List<String> kit;

	public Location firstSpawn;

	public Boolean MySQL = false;
	public String dbHost;
	public String dbUser;
	public String dbPass;
	public String dbDatabase;
	public String tableName;
	public String userField;
	public String passField;

	public ArrayList<String> playersLoggedIn = new ArrayList<String>();
	public ArrayList<String> notRegistered = new ArrayList<String>();
	public Map<String, NLIData> notLoggedIn = new HashMap<String, NLIData>();

	public int threadRuns;
	public int sqlQueries = 0;
	public long timingNanos;
	public long sqlQueryTime;
	public long syncTaskTime;

	public MessageDigest md5;
	public NLICacheHandler nch;

	@Override
	public void onDisable() {
		System.out.println(this.getDescription().getName() + " " + this.getDescription().getVersion() + " was disabled!");
	}

	@Override
	public void onEnable() {
		startTiming();

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

			List<String> defaultKit = new ArrayList<String>();
			defaultKit.add("358:1:1"); // 1 welcome map
			defaultKit.add("358:2:1"); // 1 rules map
			defaultKit.add("270:0:1"); // 1 wooden pickaxe
			defaultKit.add("17:0:4"); // 4 logs

			conf.setProperty("kits.enable", true);
			conf.setProperty("kits.items", defaultKit);

			conf.save();
		}

		conf.load();

		this.giveKits = conf.getBoolean("kits.enable", true);
		this.kit = conf.getStringList("kits.items", null);

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
					this.tableName = "auth";
					this.userField = "username";
					this.passField = "password";
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

			this.tableName = "auth";
			this.userField = "username";
			this.passField = "password";

			this.manageSQLite.initialize();

			if (!this.manageSQLite.checkTable(tableName)) {
				this.manageSQLite.createTable("CREATE TABLE auth (id INT AUTO_INCREMENT PRIMARY_KEY, username VARCHAR(30), password VARCHAR(50));");
			}
		}

		try {
			this.md5 = MessageDigest.getInstance("MD5");
		} catch(NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		}

		nch = new NLICacheHandler(this);

		IceAuthPlayerListener playerListener = new IceAuthPlayerListener(this);
		IceAuthBlockListener blockListener = new IceAuthBlockListener(this);
		IceAuthEntityListener entityListener = new IceAuthEntityListener(this);

		this.getServer().getPluginManager().registerEvents(playerListener, this);
		this.getServer().getPluginManager().registerEvents(blockListener, this);
		this.getServer().getPluginManager().registerEvents(entityListener, this);

		this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new PlayerSyncThread(), 20, 20);

		int timeToStart = Math.round(stopTiming() / 1000000);
		System.out.println(this.getDescription().getName() + " " + this.getDescription().getVersion() + " has been enabled in " + timeToStart + "ms.");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if(commandLabel.equalsIgnoreCase("register")) {
			if(!(sender instanceof Player)) {
				return false;
			}

			Player player = (Player) sender;
			if(!checkUnReg(player)) {
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

			if(checkUnReg(player)) {
				player.sendMessage(ChatColor.RED + "You need to register first!");
				return false;
			}

			if(checkAuth(player)) {
				player.sendMessage(ChatColor.RED + "Already logged in!");
				return false;
			}

			if(checkLogin(playername, password)) {
				player.sendMessage(ChatColor.GREEN + "Logged in successfully");

				restoreInv(player);

				NLIData nli = notLoggedIn.get(playername);
				player.setGameMode(nli.getGameMode());

				delPlayerNotLoggedIn(player);
				addAuthPlayer(player);

				return true;
			} else {
				player.sendMessage(ChatColor.RED + "Wrong password!");
				System.out.println("[IceAuth] Player "+player.getName()+" tried logging in with a wrong password!");
				return false;
			}
		}

		if(commandLabel.equalsIgnoreCase("changepassword")) {
			if(!(sender instanceof Player)) {
				return false;
			}

			Player player = (Player) sender;

			if(checkUnReg(player)) {
				player.sendMessage(ChatColor.RED + "You aren't registered!");
				return false;
			}
			if(!checkAuth(player)) {
				player.sendMessage(ChatColor.RED + "You aren't logged in!");
				return false;
			}
			if(args.length != 2) {
				player.sendMessage("Usage: /changepassword <oldpassword> <newpassword>");
				return false;
			}

			changePassword(args[0], args[1], player);
			return true;
		}

		if(commandLabel.equalsIgnoreCase("iceauth") && sender.isOp()) {
			sender.sendMessage(ChatColor.YELLOW + "=== IceAuth performance stats ===");
			sender.sendMessage(ChatColor.YELLOW + "Time taken for SQL queries: " + Math.round(this.sqlQueryTime / 1000000) + "ms. over " + this.sqlQueries + " queries.");
			sender.sendMessage(ChatColor.YELLOW + "Time taken for sync task: " + Math.round(this.syncTaskTime / 1000000) + "ms. over " + this.threadRuns + " runs.");
			sender.sendMessage(ChatColor.YELLOW + "playersLoggedIn size: " + this.playersLoggedIn.size());
			sender.sendMessage(ChatColor.YELLOW + "notRegistered size: " + this.notRegistered.size());
			sender.sendMessage(ChatColor.YELLOW + "notLoggedIn size: " + this.notLoggedIn.size());
			sender.sendMessage(ChatColor.YELLOW + "shouldBeCancelled size: " + IceAuthPlayerListener.shouldBeCancelled.size());
			return true;
		}

		return false;
	}

	public void addAuthPlayer(Player player) {
		playersLoggedIn.add(player.getName());	
	}

	public boolean checkAuth(Player player) {
		return playersLoggedIn.contains(player.getName());
	}

	public boolean checkUnReg(Player player) {
		return notRegistered.contains(player.getName());
	}

	public void removePlayerCache(Player player) {
		String pName = player.getName();

		if(!checkAuth(player)) restoreInv(player); else saveInventory(player);

		playersLoggedIn.remove(pName);
		notLoggedIn.remove(pName);	
		notRegistered.remove(pName);
	}

	public void addPlayerNotLoggedIn(Player player, Location loc, Boolean registered) {
		NLIData nli = new NLIData(loc, (int) (System.currentTimeMillis() / 1000L), player.getInventory().getContents(), player.getInventory().getArmorContents(), player.getGameMode());
		notLoggedIn.put(player.getName(), nli);
		if(!registered) notRegistered.add(player.getName());
	}

	public void saveInventory(Player player) {
		NLIData nli = new NLIData(player.getInventory().getContents(), player.getInventory().getArmorContents());
		nch.createCache(player.getName(), nli);
	}

	public void delPlayerNotLoggedIn(Player player) {
		notLoggedIn.remove(player.getName());	
		notRegistered.remove(player.getName());
	}

	public void msgPlayerLogin(Player player) {
		if(checkUnReg(player)) {
			player.sendMessage(ChatColor.RED + "Use /register <password> to register!");
		} else {
			player.sendMessage(ChatColor.RED + "Use /login <password> to log in!");
		}
	}

	public boolean checkInvEmpty(ItemStack[] invstack) {
		for (int i = 0; i < invstack.length; i++) {
			if (invstack[i] != null) {
				if(invstack[i].getTypeId() > 0) return false;
			}
		}
		return true;
	}

	public boolean isInvCacheEmpty(String pName) {
		NLIData nli = nch.readCache(pName);
		ItemStack[] inv = nli.getInventory();
		if(checkInvEmpty(inv)) return true; 
		return false;
	}

	public void restoreInv(Player player) {
		restoreInv(player, false);
	}

	public void restoreInv(Player player, boolean useCache) {
		NLIData nli = null;

		if(useCache) {
			nli = nch.readCache(player.getName());
		} else {
			nli = notLoggedIn.get(player.getName());
		}

		ItemStack[] invstackbackup = null;
		ItemStack[] armStackBackup = null;

		try {
			invstackbackup = nli.getInventory();
			armStackBackup = nli.getArmour();
		} catch(Exception e) {
			System.out.println("[IceAuth] Restoring inventory failed for player " + player.getName());
			e.printStackTrace();
			return;
		}

		if(invstackbackup != null) {
			player.getInventory().setContents(invstackbackup);
		}

		if(armStackBackup[3] != null) {
			if(armStackBackup[3].getAmount() != 0) {
				player.getInventory().setHelmet(armStackBackup[3]);
			}
		}
		if(armStackBackup[2] != null) {
			if(armStackBackup[2].getAmount() != 0) {
				player.getInventory().setChestplate(armStackBackup[2]);
			}
		}
		if(armStackBackup[1] != null) {
			if(armStackBackup[1].getAmount() != 0) {
				player.getInventory().setLeggings(armStackBackup[1]);
			}
		}
		if(armStackBackup[0] != null) {
			if(armStackBackup[0].getAmount() != 0) {
				player.getInventory().setBoots(armStackBackup[0]);
			}
		}
	}

	public String getMD5(String message) {
		byte[] digest;
		md5.reset();
		md5.update(message.getBytes());
		digest = md5.digest();

		return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest));
	}

	public boolean isRegistered(String name) {

		ResultSet result = null;

		Connection connection = null;

		if (this.MySQL) {
			try {
				connection = this.manageMySQL.getConnection();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			connection = this.manageSQLite.getConnection();
		}

		try {
			startTiming();
			PreparedStatement regQ = connection.prepareStatement("SELECT COUNT(*) AS c FROM "+tableName+" WHERE " + userField + " = ?");
			regQ.setString(1, name);
			result = regQ.executeQuery();
			this.sqlQueryTime += stopTiming();
			this.sqlQueries++;

			while(result.next()) {
				if(result.getInt("c") > 0) {
					return true;
				} else {
					return false;
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				result.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return false;
	}

	public boolean checkLogin(String name, String password) { // fails at sqlite (or register, not tested)

		ResultSet result = null;
		Connection connection = null;

		if (this.MySQL) {
			try {
				connection = this.manageMySQL.getConnection();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			connection = this.manageSQLite.getConnection();
		}
		try {
			startTiming();
			PreparedStatement regQ = connection.prepareStatement("SELECT COUNT(*) AS c FROM "+tableName+" WHERE " + userField + " = ? && "+passField+" = ?");
			regQ.setString(1, name);
			regQ.setString(2, getMD5(password));
			result = regQ.executeQuery();
			this.sqlQueryTime += stopTiming();
			this.sqlQueries++;

			while(result.next()) {
				if(result.getInt("c") > 0) {
					return true;
				} else {
					return false;
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				result.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}


		return false;
	}

	public boolean register(String name, String password) {
		Connection connection = null;

		if (this.MySQL) {
			try {
				connection = this.manageMySQL.getConnection();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			connection = this.manageSQLite.getConnection();
		}
		try {
			startTiming();
			PreparedStatement regQ = connection.prepareStatement("INSERT INTO "+tableName+" ("+userField+", "+passField+") VALUES(?,?)");
			regQ.setString(1, name);
			regQ.setString(2, getMD5(password));
			regQ.executeUpdate();
			this.sqlQueryTime += stopTiming();
			this.sqlQueries++;

			System.out.println("[IceAuth] Player "+name+" registered sucessfully.");

			notRegistered.remove(name);

			return true;

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}

	public boolean changePassword(String oldpass, String password, Player player) {

		if(checkLogin(player.getName(), oldpass)) {

			Connection connection = null;

			if (this.MySQL) {
				try {
					connection = this.manageMySQL.getConnection();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			} else {
				connection = this.manageSQLite.getConnection();
			}
			try {
				startTiming();
				PreparedStatement regQ = connection.prepareStatement("UPDATE "+tableName+" SET " + passField + " = ? WHERE " + userField + " = ?");
				regQ.setString(1, getMD5(password));
				regQ.setString(2, player.getName());
				regQ.executeUpdate();
				this.sqlQueryTime += stopTiming();
				this.sqlQueries++;

				player.sendMessage(ChatColor.GREEN + "Password updated sucessfully!");
				System.out.println("[IceAuth] Player "+player.getName()+" changed his password!");
				return true;

			} catch (SQLException e) {
				e.printStackTrace();
			}

		} else {
			player.sendMessage(ChatColor.RED + "Wrong password!");
			System.out.println("[IceAuth] Player "+player.getName()+" tried changepassword with a wrong password!");
			return true;
		}

		return false;
	}

	public void giveKits(Player player) {
		if(this.kit != null) {
			PlayerInventory inv = player.getInventory();
			for(String item : this.kit) {
				String[] parts = item.split(":");
				if(parts.length != 3) continue;

				int type = Integer.parseInt(parts[0]);
				short damage = Short.parseShort(parts[1]);
				int amount = Integer.parseInt(parts[2]);

				ItemStack is = new ItemStack(type, amount);
				if(damage != 0) is.setDurability(damage);

				inv.addItem(is);
			}
		}
	}

	public void startTiming() {
		this.timingNanos = System.nanoTime();
	}

	public long stopTiming() {
		return System.nanoTime() - this.timingNanos;
	}

	public void tpPlayers(boolean msgLogin) {
		startTiming();
		for (Player player : this.getServer().getOnlinePlayers()) {
			if(player != null && !checkAuth(player)) {
				String playerName = player.getName();
				NLIData nli = notLoggedIn.get(playerName);
				Location pos = nli.getLoc();

				if((int) (System.currentTimeMillis() / 1000L) - nli.getLoggedSecs() > 60) {
					player.kickPlayer("You took too long to log in!");
					System.out.println("[IceAuth] Player "+playerName+" took too long to log in");
					continue;
				}

				player.teleport(pos);

				if(msgLogin) {
					msgPlayerLogin(player);
				}
			}
		}
		this.syncTaskTime += stopTiming();
	}

	// Data structures

	public class NLIData {
		private int loggedSecs;
		private Location loc;
		private ItemStack[] inventory;
		private ItemStack[] armour;
		private GameMode gameMode;	

		public NLIData(ItemStack[] inventory, ItemStack[] armour) {
			this.inventory = inventory;
			this.armour = armour;
		}

		public NLIData(Location loc, int loggedSecs, ItemStack[] inventory, ItemStack[] armour, GameMode gameMode) {
			this.inventory = inventory;
			this.armour = armour;
			this.loc = loc;
			this.loggedSecs = loggedSecs;
			this.gameMode = gameMode;
		}

		public Location getLoc() {
			return this.loc;
		}

		public int getLoggedSecs() {
			return this.loggedSecs;
		}

		public ItemStack[] getInventory() {
			return inventory;
		}

		public ItemStack[] getArmour() {
			return armour;
		}

		public GameMode getGameMode() {
			return gameMode;
		}
	}

	public class PlayerSyncThread implements Runnable {
		@Override
		public void run() {
			threadRuns++;
			if(threadRuns % 11 == 0) {
				tpPlayers(true);
			} else {
				tpPlayers(false);
			}
		}

	}

}
