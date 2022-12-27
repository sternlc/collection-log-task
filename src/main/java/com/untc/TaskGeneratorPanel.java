package com.untc;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.UnaryOperator;

public class TaskGeneratorPanel extends PluginPanel {

    public final JPanel taskPanel;

    private String itemName;

    public TaskGeneratorPanel(final TaskGeneratorPlugin taskGeneratorPlugin) {
        super();

        taskPanel = new JPanel();
        taskPanel.setLayout(new BorderLayout());
        taskPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());

        JPanel titlePanel = new JPanel();
        titlePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        titlePanel.setLayout(new BorderLayout());

        JLabel title = new JLabel();
        title.setText("Task Generator");
        title.setForeground(Color.WHITE);
        titlePanel.add(title, BorderLayout.WEST);

        final JPanel buttons = new JPanel(new GridLayout(1, 1, 10, 0));
        buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JButton generateTaskButton = new JButton();
        SwingUtil.removeButtonDecorations(generateTaskButton);
        generateTaskButton.setIcon(getIcon(img -> ImageUtil.resizeImage(img, 16, 16), "task_generator_icon.png"));
        generateTaskButton.setToolTipText("Generate new task");
        generateTaskButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
        generateTaskButton.setUI(new BasicButtonUI());
        generateTaskButton.addActionListener(e -> taskGeneratorPlugin.generateTask());
        buttons.add(generateTaskButton);

        JButton completeTaskButton = new JButton();
        SwingUtil.removeButtonDecorations(completeTaskButton);
        completeTaskButton.setIcon(getIcon(img -> ImageUtil.resizeImage(img, 16, 16), "task_complete_icon.png"));
        completeTaskButton.setToolTipText("Complete task");
        completeTaskButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
        completeTaskButton.setUI(new BasicButtonUI());
        completeTaskButton.addActionListener(e -> {
            if (!itemName.isEmpty()) {
                taskGeneratorPlugin.completeTask(itemName);
            }
        });
        buttons.add(completeTaskButton);

        titlePanel.add(buttons, BorderLayout.EAST);

        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());
        container.add(titlePanel, BorderLayout.NORTH);

        add(container, BorderLayout.NORTH);
        add(taskPanel, BorderLayout.SOUTH);
    }

    public void refreshTaskPanel(String name, int itemId) {
        SwingUtilities.invokeLater(() -> {
            taskPanel.removeAll();
            JLabel label = new JLabel();
            Image image = null;
            URL url;
            try {
                url = new URL(itemImageUrl(itemId));
                image = ImageIO.read(url);
            } catch (MalformedURLException malformedURLException) {
                System.out.println("Malformed URL!");
            } catch (IOException ioException) {
                System.out.println("IO exception getting item image!");
            }
            if (image != null) {
                label.setIcon(new ImageIcon(image));
                label.setText("<html>" + name + "</html>");
                itemName = name;
                taskPanel.add(label, BorderLayout.WEST);
                taskPanel.repaint();
                taskPanel.revalidate();
            }
        });
    }

    private static String itemImageUrl(int itemId) {
        return "https://static.runelite.net/cache/item/icon/" + itemId + ".png";
    }

    private BufferedImage getImage(String path) {
        return ImageUtil.loadImageResource(TaskGeneratorPlugin.class, path);
    }

    private ImageIcon getIcon(@Nonnull UnaryOperator<BufferedImage> unaryOperator, String path) {
        BufferedImage bufferedImage = unaryOperator.apply(getImage(path));
        return new ImageIcon(bufferedImage);
    }
}
