package com.untc;

import com.google.gson.*;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Varbits;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import static net.runelite.client.RuneLite.RUNELITE_DIR;

import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
	name = "Task Generator"
)
public class TaskGeneratorPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private TaskGeneratorConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	private TaskGeneratorPanel taskGeneratorPanel;

	private NavigationButton navigationButton;

	@Getter
	private JsonObject collectionLogData;

	@Getter
	private JsonObject loadedTask;

	@Getter
	@Inject
	private ClientThread clientThread;

	private JsonArray generatedTaskItems;

	private final String[] excludedItems = new String[]{
			"Gilded",
			"3rd age",
			"Ring of",
			"Victor's cape",
			"Icthlarin's shroud",
			"Xeric's",
			"Sinhaza shroud",
			"Godsword shard",
			"Evil chicken",
			"(dusk)",
			"Jar of",
			"Bucket helm (g)",
			"Lava dragon mask",
			"Iasor seed",
			"Attas seed",
			"Kronos seed",
			"Bottomless compost bucket",
			"Infernal cape",
			"Fire cape",
			"Draconic visage",
			"Skeletal visage",
			"Remnant of"
	};

	private String task;

	private boolean loaded;

	private static final Pattern COLLECTION_LOG_ITEM_REGEX = Pattern.compile("New item added to your collection log:.*");

	private static final String COLLECTION_LOG_TEXT = "New item added to your collection log: ";

	@Override
	protected void startUp() throws Exception {
		taskGeneratorPanel = new TaskGeneratorPanel(this);
		initPanel();
		log.info("Plugin started!");
	}

	@Override
	protected void shutDown() throws Exception {
		clientToolbar.removeNavigation(navigationButton);
		log.info("Plugin stopped!");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged) {
		if (!configChanged.getGroup().equals("taskgenerator")) {
			return;
		}
		if (configChanged.getKey().equals("show_panel")) {
			if (configChanged.getNewValue().equals("true")) {
				initPanel();
				return;
			}
			clientToolbar.removeNavigation(navigationButton);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) {
		if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN || gameStateChanged.getGameState() == GameState.HOPPING) {
			saveTask();
		}
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
			if (!loaded)
				clientThread.invokeLater(this::loadTask);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage) {
		if (chatMessage.getType() != ChatMessageType.GAMEMESSAGE && chatMessage.getType() != ChatMessageType.SPAM) {
			return;
		}
		if (config.autogenerate()) {
			String inputMessage = chatMessage.getMessage();
			String outputMessage = Text.removeTags(inputMessage);
			String item;
			if (COLLECTION_LOG_ITEM_REGEX.matcher(outputMessage).matches() && client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION) >= 1) {
				item = outputMessage.substring(COLLECTION_LOG_TEXT.length());
				completeTask(item);
			}
		}
	}

	@Provides
	TaskGeneratorConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TaskGeneratorConfig.class);
	}

	private void initPanel() {
		if (config.showPanel()) {
			final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "side_panel_icon.png");
			navigationButton = NavigationButton.builder()
					.tooltip("Task Generator")
					.icon(icon)
					.priority(6)
					.panel(taskGeneratorPanel)
					.build();
			clientToolbar.addNavigation(navigationButton);
		}
	}

	public void loadCollectionLog() {
		if (client.getLocalPlayer() == null) {
			return;
		}
		try {
			String fileName = "collectionlog-" + client.getLocalPlayer().getName() + ".json";
			FileReader fileReader = new FileReader(new File(RUNELITE_DIR, "collectionlog") + File.separator + fileName);
			collectionLogData = new JsonParser().parse(fileReader).getAsJsonObject();
			fileReader.close();
			log.info("Collection log data loaded for " + client.getLocalPlayer().getName());
		} catch (IOException | JsonParseException e) {
			log.error("Error loading collection log data");
		}
	}

	public void generateTask() {
		if (client.getLocalPlayer() == null) {
			return;
		}
		if (collectionLogData == null) {
			loadCollectionLog();
		}
		getTask(collectionLogData);
	}

	private void getTask(JsonObject jsonObject) {
		JsonObject tabs = jsonObject.getAsJsonObject("tabs");
		String tab = getKey(tabs, true);
		if (tab.equals("Raids") && !config.includeRaids()) {
			getTask(jsonObject);
			return;
		}
		JsonArray pets = tabs.getAsJsonObject("Other").getAsJsonObject("All Pets").getAsJsonArray("items");
		JsonArray randomEvents = tabs.getAsJsonObject("Other").getAsJsonObject("Random Events").getAsJsonArray("items");
		task = getKey(tabs.getAsJsonObject(tab), true);
		generatedTaskItems = tabs.getAsJsonObject(tab).getAsJsonObject(task).getAsJsonArray("items");
		String item = getTask(generatedTaskItems);
		String name = item.split("=")[0];
		if (Arrays.stream(excludedItems).anyMatch(name::contains) || isExcluded(randomEvents, name) || (!config.includePets() && isExcluded(pets, name))) {
			getTask(jsonObject);
			return;
		}
		if (isObtained(generatedTaskItems, name)) {
			log.info("Already obtained " + name + ", generating new task...");
			getTask(jsonObject);
			return;
		}
		int itemId = Integer.parseInt(item.substring(item.indexOf("=") + 1).trim());
		String formatted = name;
		if (config.generalizeCategories()) {
			formatted = "Get a unique item from<br>" + task;
		}
		taskGeneratorPanel.refreshTaskPanel(formatted, itemId);
	}

	public void completeTask(String item) {
		if (generatedTaskItems == null || collectionLogData == null) {
			return;
		}
		if (!config.autogenerate()) {
			generateTask();
			return;
		}
		for (int i = 0; i < generatedTaskItems.size(); i++) {
			JsonObject jsonObject = generatedTaskItems.get(i).getAsJsonObject();
			if(isObtained(generatedTaskItems, item)) {
				return;
			}
			if (jsonObject.get("name").getAsString().equals(item)) {
				log.info("Task completed! [item=" + item + "]");
				generateTask();
			}
		}
	}

	private String getKey(JsonObject jsonObject, boolean random) {
		val keys = new ArrayList<>(jsonObject.keySet());
		if (random) {
			int index = new Random().nextInt(keys.size());
			return keys.get(index);
		} else return keys.get(0);
	}

	private String getTask(JsonArray jsonArray) {
		int index = new Random().nextInt(jsonArray.size());
		JsonObject key = jsonArray.get(index).getAsJsonObject();
		return key.get("name").getAsString() + "=" + key.get("id").getAsString();
	}

	private boolean isExcluded(JsonArray jsonArray, String name) {
		for (int i = 0; i < jsonArray.size(); i++) {
			JsonObject item = jsonArray.get(i).getAsJsonObject();
			if (item.get("name").getAsString().equals(name)) {
				return true;
			}
		}
		return false;
	}

	private boolean isObtained(JsonArray jsonArray, String name) {
		for (int i = 0; i < jsonArray.size(); i++) {
			JsonObject item = jsonArray.get(i).getAsJsonObject();
			if (item.get("name").getAsString().equals(name)) {
				if (item.get("obtained").getAsString().equals("true") || Integer.parseInt(item.get("quantity").getAsString()) >= 1) {
					return true;
				}
			}
		}
		return false;
	}

	private void loadTask() {
		if (client.getLocalPlayer().getName() == null) {
			clientThread.invokeAtTickEnd(this::loadTask);
			return;
		}
		loadCollectionLog();
		try {
			String fileName = "current-task-" + client.getLocalPlayer().getName() + ".json";
			FileReader fileReader = new FileReader(new File(RUNELITE_DIR, "collectionlog") + File.separator + fileName);
			loadedTask = new JsonParser().parse(fileReader).getAsJsonObject();
			task = getKey(loadedTask, false);
			generatedTaskItems = loadedTask.getAsJsonObject(task).getAsJsonArray("items");

			String item = getTask(generatedTaskItems);
			String name = item.split("=")[0];
			int itemId = Integer.parseInt(item.substring(item.indexOf("=") + 1).trim());
			String formatted = name;
			if (config.generalizeCategories()) {
				formatted = "Get a unique item from<br>" + task;
			}
			taskGeneratorPanel.refreshTaskPanel(formatted, itemId);
			loaded = true;
			fileReader.close();
		} catch (IOException ioException) {
			log.error("Error loading current task");
		}
	}

	private  void saveTask() {
		if (generatedTaskItems == null || collectionLogData == null) {
			return;
		}
		File file = new File(RUNELITE_DIR, "collectionlog");
		file.mkdir();
		String path;
		String fileName = "current-task-" + client.getLocalPlayer().getName() + ".json";
		path = file + File.separator + fileName;
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(path));
			writer.write("{\"" + task + "\":{\"items\":");
			writer.write(generatedTaskItems.toString() +"}}");
			writer.close();
		} catch (IOException ioException) {
			log.error("Unable to export current task: " + ioException.getMessage());
		}
	}
}
