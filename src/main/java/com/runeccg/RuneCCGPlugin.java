package com.runeccg;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.GameState;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
		name = "RuneCCG"
)
public class RuneCCGPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	private RuneCCGPanel panel;
	private NavigationButton navButton;
	private final Map<Skill, Integer> previousXpMap = new HashMap<>();
	private boolean userConfirmedEventWorld = false;

	// Secret key for HMAC signing - KEEP THIS SECRET
	// In production, this should match the key on your backend
	private static final String SECRET_KEY = "RuneCCG_Secret_Key_2026_Change_This_In_Production";
	private static final String CONFIG_GROUP = "runeccg";
	private static final String CONFIG_KEY_CURRENT_XP = "currentXp";
	private static final String CONFIG_KEY_TOTAL_COINS = "totalCoins";

	// Helper methods for per-character config storage
	private int getCurrentXp()
	{
		Integer xp = configManager.getRSProfileConfiguration(CONFIG_GROUP, CONFIG_KEY_CURRENT_XP, int.class);
		return xp != null ? xp : 0;
	}

	private void setCurrentXp(int xp)
	{
		configManager.setRSProfileConfiguration(CONFIG_GROUP, CONFIG_KEY_CURRENT_XP, xp);
	}

	private int getTotalCoins()
	{
		Integer coins = configManager.getRSProfileConfiguration(CONFIG_GROUP, CONFIG_KEY_TOTAL_COINS, int.class);
		return coins != null ? coins : 0;
	}

	private void setTotalCoins(int coins)
	{
		configManager.setRSProfileConfiguration(CONFIG_GROUP, CONFIG_KEY_TOTAL_COINS, coins);
	}

	@Override
	protected void startUp() throws Exception
	{
		log.info("RuneCCG plugin started!");

		panel = injector.getInstance(RuneCCGPanel.class);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "panel_icon.png");

		navButton = NavigationButton.builder()
				.tooltip("RuneCCG")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);

		// Initialize the panel with current values
		panel.updateProgress(getCurrentXp(), getTotalCoins());

		// Set up callbacks
		panel.setCashOutCallback(this::cashOutSilverCoins);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("RuneCCG plugin stopped!");
		clientToolbar.removeNavigation(navButton);
		userConfirmedEventWorld = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			// Enable panel and refresh data for current character
			SwingUtilities.invokeLater(() -> {
				panel.setContentEnabled(true);
				panel.updateProgress(getCurrentXp(), getTotalCoins());
			});
			checkEventWorld();
		}
		else if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN ||
				 gameStateChanged.getGameState() == GameState.HOPPING)
		{
			userConfirmedEventWorld = false;
			previousXpMap.clear(); // Clear XP tracking when changing worlds

			// Disable panel when logged out
			if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN)
			{
				SwingUtilities.invokeLater(() -> panel.setContentEnabled(false));
			}
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		if (isEventWorld() && !userConfirmedEventWorld)
		{
			return;
		}

		Skill skill = statChanged.getSkill();
		int currentXp = statChanged.getXp();

		// Get previous XP for this skill
		Integer previousXp = previousXpMap.get(skill);

		if (previousXp == null)
		{
			// First time seeing this skill, initialize it
			previousXpMap.put(skill, currentXp);
			return;
		}

		// Calculate actual XP gained
		int xpGained = currentXp - previousXp;

		if (xpGained <= 0)
		{
			return;
		}

		// Update the previous XP tracker
		previousXpMap.put(skill, currentXp);

		// Add XP to current progress
		int progressXp = getCurrentXp() + xpGained;
		int coinsEarned = progressXp / 1000;

		if (coinsEarned > 0)
		{
			// Award coins
			setTotalCoins(getTotalCoins() + coinsEarned);
			// Keep remaining XP
			progressXp = progressXp % 1000;
		}

		setCurrentXp(progressXp);

		// Update panel
		SwingUtilities.invokeLater(() ->
				panel.updateProgress(getCurrentXp(), getTotalCoins())
		);
	}

	private void cashOutSilverCoins(int amount)
	{
		int currentCoins = getTotalCoins();

		if (amount > currentCoins)
		{
			SwingUtilities.invokeLater(() ->
					JOptionPane.showMessageDialog(panel,
							"Insufficient Silver Coins!\nYou have: " + currentCoins + "\nRequested: " + amount,
							"Insufficient Funds",
							JOptionPane.ERROR_MESSAGE)
			);
			return;
		}

		// Generate code
		String code = encodeCoins(amount);

		// Deduct coins
		setTotalCoins(currentCoins - amount);
		panel.updateProgress(getCurrentXp(), getTotalCoins());

		// Copy to clipboard
		StringSelection selection = new StringSelection(code);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);

		// Show dialog
		SwingUtilities.invokeLater(() -> panel.showCodeDialog(code));

		log.info("Cashed out {} Silver Coins. Code: {}", amount, code);
	}

	private String encodeCoins(int amount)
	{
		try
		{
			// Generate a random nonce (8 bytes)
			SecureRandom random = new SecureRandom();
			byte[] nonce = new byte[8];
			random.nextBytes(nonce);

			// Get current timestamp (seconds since epoch)
			long timestamp = System.currentTimeMillis() / 1000;

			// Create payload: nonce (8 bytes) + timestamp (8 bytes) + amount (4 bytes)
			ByteBuffer buffer = ByteBuffer.allocate(20);
			buffer.put(nonce);
			buffer.putLong(timestamp);
			buffer.putInt(amount);
			byte[] payload = buffer.array();

			// Calculate HMAC-SHA256 signature
			Mac hmac = Mac.getInstance("HmacSHA256");
			SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
			hmac.init(secretKeySpec);
			byte[] signature = hmac.doFinal(payload);

			// Take first 16 bytes of signature for compactness
			byte[] signatureTruncated = new byte[16];
			System.arraycopy(signature, 0, signatureTruncated, 0, 16);

			// Final code: payload (20 bytes) + signature (16 bytes) = 36 bytes
			ByteBuffer finalBuffer = ByteBuffer.allocate(36);
			finalBuffer.put(payload);
			finalBuffer.put(signatureTruncated);

			// Base64 encode (36 bytes -> 48 characters)
			return Base64.getEncoder().encodeToString(finalBuffer.array());
		}
		catch (Exception e)
		{
			log.error("Error encoding coins", e);
			return null;
		}
	}

	private boolean isEventWorld()
	{
		EnumSet<WorldType> worldType = client.getWorldType();
		return worldType.stream()
				.anyMatch(type -> type == WorldType.SEASONAL ||
						type == WorldType.DEADMAN ||
						type == WorldType.QUEST_SPEEDRUNNING ||
						type == WorldType.TOURNAMENT_WORLD ||
						type == WorldType.FRESH_START_WORLD ||
						type == WorldType.BETA_WORLD ||
						type == WorldType.NOSAVE_MODE);
	}

	private void checkEventWorld()
	{
		if (isEventWorld())
		{
			SwingUtilities.invokeLater(() ->
					panel.showEventWorldWarning(() -> {
						userConfirmedEventWorld = true;
						previousXpMap.clear(); // Clear the map so XP tracking starts fresh
						panel.rebuildNormalUI();
						panel.updateProgress(getCurrentXp(), getTotalCoins());
					})
			);
		}
		else
		{
			userConfirmedEventWorld = true;
		}
	}
}