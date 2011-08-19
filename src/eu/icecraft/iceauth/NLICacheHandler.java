package eu.icecraft.iceauth;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.ArrayList;

import org.bukkit.inventory.ItemStack;

import eu.icecraft.iceauth.IceAuth.NLIData;

public class NLICacheHandler {

	public IceAuth plugin;

	public NLICacheHandler(IceAuth plugin) {
		this.plugin = plugin;
		final File folder = new File(plugin.getDataFolder() + "/cache");
		if (!folder.exists()) {
			folder.mkdirs();
		}
	}

	public void createCache(String playername, NLIData invarm) {
		final File file = new File(plugin.getDataFolder() + "/cache/" + playername + ".cache");

		if (file.exists()) {
			//return;
		} else {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		FileWriter writer = null;
		try {
			
			writer = new FileWriter(file, false);

			ItemStack[] invstack = invarm.getInventory();

			for (int i = 0; i < invstack.length; i++) {

				int itemid = 0;
				int amount = 0;
				int durability = 0;

				if (invstack[i] != null) {
					itemid = invstack[i].getTypeId();
					amount = invstack[i].getAmount();
					durability = invstack[i].getDurability();
				}

				writer.write("i" + ":" + itemid + ":" + amount + ":"
						+ durability + "\r\n");
				writer.flush();
			}

			ItemStack[] wearstack = invarm.getArmour();

			for (int i = 0; i < wearstack.length; i++) {
				int itemid = 0;
				int amount = 0;
				int durability = 0;

				if (wearstack[i] != null) {
					itemid = wearstack[i].getTypeId();
					amount = wearstack[i].getAmount();
					durability = wearstack[i].getDurability();
				}

				writer.write("w" + ":" + itemid + ":" + amount + ":"
						+ durability + "\r\n");
				writer.flush();
			}

			writer.close();
		} catch (final Exception e) {
            e.printStackTrace();
		}
	}

	public NLIData readCache(String playername) {
		final File file = new File(plugin.getDataFolder() + "/cache/" + playername + ".cache");

		ArrayList<ItemStack> stacki = new ArrayList<ItemStack>();
		ArrayList<ItemStack> stacka = new ArrayList<ItemStack>();

		if (!file.exists()) {
			NLIData nli = (this.plugin).new NLIData(stacki.toArray(new ItemStack[0]), stacka.toArray(new ItemStack[0]));
			return nli;
		}

		Scanner reader = null;
		try {
			reader = new Scanner(file);

			while (reader.hasNextLine()) {
				final String line = reader.nextLine();

				if (!line.contains(":")) {
					continue;
				}

				final String[] in = line.split(":");

				if (in.length != 4) {
					continue;
				}
				if (!in[0].equals("i") && !in[0].equals("w")) {
					continue;
				}

				if (in[0].equals("i")) {
					stacki.add(new ItemStack(Integer.parseInt(in[1]),
							Integer.parseInt(in[2]), Short.parseShort((in[3]))));
				} else {
					stacka.add(new ItemStack(Integer.parseInt(in[1]),
							Integer.parseInt(in[2]), Short.parseShort((in[3]))));
				}

			}
		} catch (final Exception e) {
            e.printStackTrace();
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		NLIData nli = (this.plugin).new NLIData(stacki.toArray(new ItemStack[0]), stacka.toArray(new ItemStack[0]));
		return nli;
	}

	public void removeCache(String playername) {
		final File file = new File(plugin.getDataFolder() + "/cache/" + playername + ".cache");

		if (file.exists()) {
			file.delete();
		}
	}

	public boolean doesCacheExist(String playername) {
		final File file = new File(plugin.getDataFolder() + "/cache/" + playername + ".cache");

		if (file.exists()) {
			return true;
		}
		return false;
	}
	
}
