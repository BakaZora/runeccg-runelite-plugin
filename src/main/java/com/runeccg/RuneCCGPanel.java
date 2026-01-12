package com.runeccg;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

@Slf4j
public class RuneCCGPanel extends PluginPanel
{
    private static final int XP_PER_SILVER_COIN = 1000;
    private final JLabel xpLabel = new JLabel("XP: 0 / 1000");
    private final JLabel coinsLabel = new JLabel();
    private final JProgressBar progressBar = new JProgressBar(0, XP_PER_SILVER_COIN);
    private final JTextField lastCodeField = new JTextField();
    private BufferedImage coinIcon;
    private Consumer<Integer> cashOutCallback;
    private JPanel headerPanel;
    private JPanel contentPanel;
    private JPanel loggedOutPanel;

    @Inject
    public RuneCCGPanel()
    {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Load coin icon
        try
        {
            coinIcon = ImageIO.read(getClass().getResourceAsStream("panel_icon.png"));
        }
        catch (IOException e)
        {
            log.error("Failed to load coin icon", e);
        }

        // Header panel
        headerPanel = new JPanel();
        headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        headerPanel.setLayout(new BorderLayout());

        JLabel title = new JLabel("RuneCCG");
        title.setForeground(ColorScheme.BRAND_ORANGE);
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(title, BorderLayout.NORTH);

        add(headerPanel, BorderLayout.NORTH);

        // Content panel
        contentPanel = new JPanel();
        contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        contentPanel.setBorder(new EmptyBorder(15, 10, 10, 10));
        contentPanel.setLayout(new BorderLayout(0, 10));

        // Coin display panel
        JPanel coinPanel = new JPanel();
        coinPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        coinPanel.setBorder(new EmptyBorder(15, 10, 15, 10));
        coinPanel.setLayout(new BorderLayout());

        if (coinIcon != null)
        {
            Image scaledIcon = coinIcon.getScaledInstance(48, 48, Image.SCALE_SMOOTH);
            JLabel iconLabel = new JLabel(new ImageIcon(scaledIcon));
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            coinPanel.add(iconLabel, BorderLayout.WEST);
        }

        coinsLabel.setText("0 Silver Coins");
        coinsLabel.setFont(FontManager.getRunescapeBoldFont());
        coinsLabel.setForeground(Color.YELLOW);
        coinsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        coinPanel.add(coinsLabel, BorderLayout.CENTER);

        contentPanel.add(coinPanel, BorderLayout.NORTH);

        // Progress panel
        JPanel progressPanel = new JPanel();
        progressPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        progressPanel.setLayout(new BorderLayout(0, 8));

        JLabel progressTitle = new JLabel("Progress to Next Silver Coin:");
        progressTitle.setHorizontalAlignment(SwingConstants.CENTER);
        progressTitle.setFont(FontManager.getRunescapeSmallFont());
        progressPanel.add(progressTitle, BorderLayout.NORTH);

        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 24));
        progressBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        progressBar.setForeground(new Color(255, 200, 0));
        progressPanel.add(progressBar, BorderLayout.CENTER);

        xpLabel.setHorizontalAlignment(SwingConstants.CENTER);
        xpLabel.setFont(FontManager.getRunescapeFont());
        xpLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
        progressPanel.add(xpLabel, BorderLayout.SOUTH);

        contentPanel.add(progressPanel, BorderLayout.CENTER);

        // Bottom panel for cash out and debug
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // Cash out panel
        JPanel cashOutPanel = new JPanel();
        cashOutPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        cashOutPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        cashOutPanel.setLayout(new BorderLayout(0, 8));

        JLabel cashOutTitle = new JLabel("Cash Out Silver Coins:");
        cashOutTitle.setFont(FontManager.getRunescapeSmallFont());
        cashOutPanel.add(cashOutTitle, BorderLayout.NORTH);

        JPanel inputContainer = new JPanel();
        inputContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        inputContainer.setLayout(new BorderLayout(0, 5));

        JTextField amountField = new JTextField();
        amountField.setPreferredSize(new Dimension(0, 30));
        inputContainer.add(amountField, BorderLayout.NORTH);

        JButton cashOutButton = new JButton("Generate Code");
        cashOutButton.setPreferredSize(new Dimension(0, 30));
        cashOutButton.addActionListener(e -> {
            try
            {
                int amount = Integer.parseInt(amountField.getText().trim());
                if (amount <= 0)
                {
                    JOptionPane.showMessageDialog(this, "Please enter a positive number", "Invalid Amount", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (cashOutCallback != null)
                {
                    cashOutCallback.accept(amount);
                    amountField.setText("");
                }
            }
            catch (NumberFormatException ex)
            {
                JOptionPane.showMessageDialog(this, "Please enter a valid number", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        });
        inputContainer.add(cashOutButton, BorderLayout.SOUTH);

        cashOutPanel.add(inputContainer, BorderLayout.CENTER);

        // Last generated code display
        JPanel codeDisplayPanel = new JPanel();
        codeDisplayPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        codeDisplayPanel.setLayout(new BorderLayout(0, 5));

        JLabel codeLabel = new JLabel("Last Generated Code:");
        codeLabel.setFont(FontManager.getRunescapeSmallFont());
        codeDisplayPanel.add(codeLabel, BorderLayout.NORTH);

        lastCodeField.setPreferredSize(new Dimension(0, 30));
        lastCodeField.setEditable(false);
        codeDisplayPanel.add(lastCodeField, BorderLayout.CENTER);

        cashOutPanel.add(codeDisplayPanel, BorderLayout.SOUTH);

        bottomPanel.add(cashOutPanel);

        bottomPanel.add(Box.createVerticalStrut(10));

        // Instructions panel
        JPanel instructionsPanel = new JPanel();
        instructionsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        instructionsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        instructionsPanel.setLayout(new BorderLayout());

        JEditorPane instructionsPane = new JEditorPane();
        instructionsPane.setContentType("text/html");
        instructionsPane.setEditable(false);
        instructionsPane.setOpaque(false);
        instructionsPane.setFocusable(false);

        String instructions = "<html><body style='font-family: sans-serif; font-size: 10px; color: #b8b8b8;'>"
                + "<p style='margin-top: 0; margin-bottom: 8px;'><b style='color: #ff981f;'>Where do I open packs?</b><br>"
                + "To give the best graphical experience, pack openings are done on a dedicated website - navigate to "
                + "<a href='https://www.runeccg.com'>RuneCCG.com</a> to buy and open packs!</p>"
                + "<p style='margin-bottom: 8px;'><b style='color: #ff981f;'>How do I move my coins from here to RuneCCG?</b><br>"
                + "1) Specify how many coins you want to cash out in the dialogue box<br>"
                + "2) Click the \"Generate Code\" button. This will deduct that amount from your balance<br>"
                + "3) Navigate to <a href='https://www.runeccg.com'>RuneCCG.com</a><br>"
                + "4) Click the currency button in the top right to open the Add Currency interface<br>"
                + "5) Paste the code in that you copied, you will be granted your coins</p>"
                + "<p style='color: #ff0000; font-weight: bold; margin-bottom: 0;'>DO NOT LOSE YOUR CODE OR YOU WILL NOT BE ABLE TO CLAIM YOUR COINS</p>"
                + "</body></html>";

        instructionsPane.setText(instructions);

        // Add hyperlink listener
        instructionsPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
            {
                try
                {
                    Desktop.getDesktop().browse(new URI(e.getURL().toString()));
                }
                catch (Exception ex)
                {
                    log.error("Failed to open URL", ex);
                }
            }
        });

        instructionsPanel.add(instructionsPane, BorderLayout.CENTER);
        bottomPanel.add(instructionsPanel);

        contentPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(contentPanel, BorderLayout.CENTER);

        // Start with panel disabled (user not logged in)
        setContentEnabled(false);
    }

    public void setCashOutCallback(Consumer<Integer> callback)
    {
        this.cashOutCallback = callback;
    }

    public void showCodeDialog(String code)
    {
        lastCodeField.setText(code);
        JOptionPane.showMessageDialog(this,
                "Code generated and copied to clipboard:\n\n" + code + "\n\nPaste this code on the website to redeem your Silver Coins!",
                "Cash Out Code",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void updateProgress(int currentXp, int totalCoins)
    {
        progressBar.setValue(currentXp);
        xpLabel.setText(String.format("XP: %d / %d", currentXp, XP_PER_SILVER_COIN));
        coinsLabel.setText(String.format("%d Silver Coins", totalCoins));
    }

    public void showEventWorldWarning(Runnable onConfirm)
    {
        // Clear content panel
        contentPanel.removeAll();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        contentPanel.add(Box.createVerticalStrut(20));

        // Warning message
        JEditorPane warningPane = new JEditorPane();
        warningPane.setContentType("text/html");
        warningPane.setEditable(false);
        warningPane.setOpaque(false);
        warningPane.setFocusable(false);
        warningPane.setBackground(ColorScheme.DARK_GRAY_COLOR);

        String warningText = "<html><body style='font-family: sans-serif; font-size: 12px; color: #ffffff; text-align: center;'>"
                + "<p style='margin: 10px; line-height: 1.5;'><b style='color: #ff0000; font-size: 14px;'>EVENT WORLD DETECTED</b></p>"
                + "<p style='margin: 10px; color: #ffcc00; line-height: 1.5;'>This is not a main game world. XP may be boosted here causing you to gain more coins than you normally would.</p>"
                + "<p style='margin: 10px; line-height: 1.5;'>Would you still like to track XP anyway?</p>"
                + "</body></html>";

        warningPane.setText(warningText);
        warningPane.setAlignmentX(CENTER_ALIGNMENT);
        contentPanel.add(warningPane);

        contentPanel.add(Box.createVerticalStrut(20));

        // Confirm button
        JButton confirmButton = new JButton("Yes Please!");
        confirmButton.setMaximumSize(new Dimension(200, 40));
        confirmButton.setAlignmentX(CENTER_ALIGNMENT);
        confirmButton.setBackground(new Color(70, 130, 70));
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setFont(FontManager.getRunescapeBoldFont());
        confirmButton.addActionListener(e -> onConfirm.run());

        contentPanel.add(confirmButton);
        contentPanel.add(Box.createVerticalGlue());

        revalidate();
        repaint();
    }

    public void setContentEnabled(boolean enabled)
    {
        if (!enabled)
        {
            // Hide content panel and show "logged out" message
            contentPanel.setVisible(false);

            loggedOutPanel = new JPanel();
            loggedOutPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
            loggedOutPanel.setLayout(new BoxLayout(loggedOutPanel, BoxLayout.Y_AXIS));

            JLabel messageLabel = new JLabel("Please log in or relog to see silver and track XP");
            messageLabel.setFont(FontManager.getRunescapeFont());
            messageLabel.setForeground(Color.GRAY);
            messageLabel.setAlignmentX(CENTER_ALIGNMENT);
            messageLabel.setBorder(new EmptyBorder(10, 0, 0, 0));

            loggedOutPanel.add(Box.createVerticalGlue());
            loggedOutPanel.add(messageLabel);
            loggedOutPanel.add(Box.createVerticalGlue());

            add(loggedOutPanel, BorderLayout.CENTER);
        }
        else
        {
            // Remove logged out panel and show content panel
            if (loggedOutPanel != null)
            {
                remove(loggedOutPanel);
                loggedOutPanel = null;
            }
            add(contentPanel, BorderLayout.CENTER);
            contentPanel.setVisible(true);
        }
        revalidate();
        repaint();
    }

    public void rebuildNormalUI()
    {
        // Clear content panel
        contentPanel.removeAll();
        contentPanel.setLayout(new BorderLayout(0, 10));

        // Coin display panel
        JPanel coinPanel = new JPanel();
        coinPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        coinPanel.setBorder(new EmptyBorder(15, 10, 15, 10));
        coinPanel.setLayout(new BorderLayout());

        if (coinIcon != null)
        {
            Image scaledIcon = coinIcon.getScaledInstance(48, 48, Image.SCALE_SMOOTH);
            JLabel iconLabel = new JLabel(new ImageIcon(scaledIcon));
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            coinPanel.add(iconLabel, BorderLayout.WEST);
        }

        coinsLabel.setFont(FontManager.getRunescapeBoldFont());
        coinsLabel.setForeground(Color.YELLOW);
        coinsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        coinPanel.add(coinsLabel, BorderLayout.CENTER);

        contentPanel.add(coinPanel, BorderLayout.NORTH);

        // Progress panel
        JPanel progressPanel = new JPanel();
        progressPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        progressPanel.setLayout(new BorderLayout(0, 8));

        JLabel progressTitle = new JLabel("Progress to Next Silver Coin:");
        progressTitle.setHorizontalAlignment(SwingConstants.CENTER);
        progressTitle.setFont(FontManager.getRunescapeSmallFont());
        progressPanel.add(progressTitle, BorderLayout.NORTH);

        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 24));
        progressBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        progressBar.setForeground(new Color(255, 200, 0));
        progressPanel.add(progressBar, BorderLayout.CENTER);

        xpLabel.setHorizontalAlignment(SwingConstants.CENTER);
        xpLabel.setFont(FontManager.getRunescapeFont());
        xpLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
        progressPanel.add(xpLabel, BorderLayout.SOUTH);

        contentPanel.add(progressPanel, BorderLayout.CENTER);

        // Bottom panel for cash out and debug
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // Cash out panel
        JPanel cashOutPanel = new JPanel();
        cashOutPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        cashOutPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        cashOutPanel.setLayout(new BorderLayout(0, 8));

        JLabel cashOutTitle = new JLabel("Cash Out Silver Coins:");
        cashOutTitle.setFont(FontManager.getRunescapeSmallFont());
        cashOutPanel.add(cashOutTitle, BorderLayout.NORTH);

        JPanel inputContainer = new JPanel();
        inputContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        inputContainer.setLayout(new BorderLayout(0, 5));

        JTextField amountField = new JTextField();
        amountField.setPreferredSize(new Dimension(0, 30));
        inputContainer.add(amountField, BorderLayout.NORTH);

        JButton cashOutButton = new JButton("Generate Code");
        cashOutButton.setPreferredSize(new Dimension(0, 30));
        cashOutButton.addActionListener(e -> {
            try
            {
                int amount = Integer.parseInt(amountField.getText().trim());
                if (amount <= 0)
                {
                    JOptionPane.showMessageDialog(this, "Please enter a positive number", "Invalid Amount", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (cashOutCallback != null)
                {
                    cashOutCallback.accept(amount);
                    amountField.setText("");
                }
            }
            catch (NumberFormatException ex)
            {
                JOptionPane.showMessageDialog(this, "Please enter a valid number", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        });
        inputContainer.add(cashOutButton, BorderLayout.SOUTH);

        cashOutPanel.add(inputContainer, BorderLayout.CENTER);

        // Last generated code display
        JPanel codeDisplayPanel = new JPanel();
        codeDisplayPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        codeDisplayPanel.setLayout(new BorderLayout(0, 5));

        JLabel codeLabel = new JLabel("Last Generated Code:");
        codeLabel.setFont(FontManager.getRunescapeSmallFont());
        codeDisplayPanel.add(codeLabel, BorderLayout.NORTH);

        lastCodeField.setPreferredSize(new Dimension(0, 30));
        lastCodeField.setEditable(false);
        codeDisplayPanel.add(lastCodeField, BorderLayout.CENTER);

        cashOutPanel.add(codeDisplayPanel, BorderLayout.SOUTH);

        bottomPanel.add(cashOutPanel);

        bottomPanel.add(Box.createVerticalStrut(10));

        // Instructions panel
        JPanel instructionsPanel = new JPanel();
        instructionsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        instructionsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        instructionsPanel.setLayout(new BorderLayout());

        JEditorPane instructionsPane = new JEditorPane();
        instructionsPane.setContentType("text/html");
        instructionsPane.setEditable(false);
        instructionsPane.setOpaque(false);
        instructionsPane.setFocusable(false);

        String instructions = "<html><body style='font-family: sans-serif; font-size: 10px; color: #b8b8b8;'>"
                + "<p style='margin-top: 0; margin-bottom: 8px;'><b style='color: #ff981f;'>Where do I open packs?</b><br>"
                + "To give the best graphical experience, pack openings are done on a dedicated website - navigate to "
                + "<a href='https://www.runeccg.com'>RuneCCG.com</a> to buy and open packs!</p>"
                + "<p style='margin-bottom: 8px;'><b style='color: #ff981f;'>How do I move my coins from here to RuneCCG?</b><br>"
                + "1) Specify how many coins you want to cash out in the dialogue box<br>"
                + "2) Click the \"Generate Code\" button. This will deduct that amount from your balance<br>"
                + "3) Navigate to <a href='https://www.runeccg.com'>RuneCCG.com</a><br>"
                + "4) Click the currency button in the top right to open the Add Currency interface<br>"
                + "5) Paste the code in that you copied, you will be granted your coins</p>"
                + "<p style='color: #ff0000; font-weight: bold; margin-bottom: 0;'>DO NOT LOSE YOUR CODE OR YOU WILL NOT BE ABLE TO CLAIM YOUR COINS</p>"
                + "</body></html>";

        instructionsPane.setText(instructions);

        // Add hyperlink listener
        instructionsPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
            {
                try
                {
                    Desktop.getDesktop().browse(new URI(e.getURL().toString()));
                }
                catch (Exception ex)
                {
                    log.error("Failed to open URL", ex);
                }
            }
        });

        instructionsPanel.add(instructionsPane, BorderLayout.CENTER);
        bottomPanel.add(instructionsPanel);

        contentPanel.add(bottomPanel, BorderLayout.SOUTH);

        revalidate();
        repaint();
    }
}
