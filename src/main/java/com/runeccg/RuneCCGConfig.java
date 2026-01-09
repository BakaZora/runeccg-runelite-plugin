package com.runeccg;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;

@ConfigGroup("runeccg")
public interface RuneCCGConfig extends Config
{
	// Config is now stored per-character using ConfigManager directly
	// See RuneCCGPlugin for getCurrentXp(), setCurrentXp(), getTotalCoins(), setTotalCoins()
}
