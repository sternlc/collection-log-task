package com.untc;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("taskgenerator")
public interface TaskGeneratorConfig extends Config
{
	@ConfigItem(
			keyName = "show_panel",
			name = "Show the task generator panel",
			description = "Show the task generator panel",
			position = 2
	)
	default boolean showPanel() {
		return true;
	}

	@ConfigItem(
			keyName = "autogenerate",
			name = "Auto-complete task",
			description = "Automatically generate a new task after completing your current task",
			position = 3
	)
	default boolean autogenerate() {
		return true;
	}

	@ConfigItem(
			keyName = "include_pets",
			name = "Include pets",
			description = "Allows pets to be generated as a task",
			position = 4
	)
	default boolean includePets() {
		return false;
	}

	@ConfigItem(
			keyName = "include_rares",
			name = "Include rare items",
			description = "Allows rare items like 3rd age to be generated as a task",
			position = 5
	)
	default boolean includeRaids() {
		return true;
	}

	@ConfigItem(
			keyName = "generalize",
			name = "Generalize categories",
			description = "Allow ANY unique from a category to count for completion (instead of only a specific item)",
			position = 6
	)
	default boolean includeRares() {
		return false;
	}
}
