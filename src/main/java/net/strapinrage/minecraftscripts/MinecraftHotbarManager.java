package net.strapinrage.minecraftscripts;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MinecraftHotbarManager extends JFrame implements NativeKeyListener {

    private Map<String, Integer> itemBinds = new LinkedHashMap<>();
    private Map<String, String> bindKeys = new HashMap<>();
    private int lastSlot = 1;
    private boolean isRunning = false;

    private JPanel mainPanel;
    private JLabel statusLabel;
    private JLabel slotLabel;
    private JButton toggleButton;
    private List<ItemBindPanel> bindPanels;

    private JTextField windowFilterField;

    private Robot robot;

    private final Set<String> activeActions = ConcurrentHashMap.newKeySet();

    public MinecraftHotbarManager() throws AWTException {
        initializeBinds();
        setupUI();

        ImageIcon icon = new ImageIcon(getClass().getResource("/icon.png"));
        setIconImage(icon.getImage());

        robot = new Robot();

        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);
        logger.setUseParentHandlers(false);

        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
        } catch (NativeHookException e) {
            JOptionPane.showMessageDialog(this, "Failed to initialize global keyboard listener:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void initializeBinds() {
        itemBinds.put("Sword", 1);
        itemBinds.put("Fishing Rod", 2);
        itemBinds.put("Snowball", 3);
        itemBinds.put("Bow", 4);
        itemBinds.put("Food", 5);
        itemBinds.put("Blocks", 6);
        itemBinds.put("Tools", 7);
        itemBinds.put("Potions", 8);
        itemBinds.put("Pearl", 9);

        bindKeys.put("Fishing Rod", "F");
        bindKeys.put("Snowball", "H");
        bindKeys.put("Bow", "R");
        bindKeys.put("Food", "C");
        bindKeys.put("Blocks", "V");
        bindKeys.put("Tools", "B");
        bindKeys.put("Potions", "N");
        bindKeys.put("Pearl", "M");
    }

    private void setupUI() {
        setTitle("RAGESCRIPTS - PVP SCRIPTS");
        setSize(1100, 500);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);

        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(new Color(0x1E1E1E));
        createCustomTitleBar(rootPanel);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(0, 40, 0, 40));
        mainPanel.setBackground(new Color(0x1E1E1E));

        createHeader();
        createWindowFilterPanel();
        createStatusPanel();
        createBindsPanel();
        createControlPanel();

        rootPanel.add(mainPanel, BorderLayout.CENTER);
        add(rootPanel);
    }

    private void createCustomTitleBar(JPanel rootPanel) {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setPreferredSize(new Dimension(0, 40));
        titleBar.setBorder(new EmptyBorder(0, 15, 0, 15));
        titleBar.setBackground(new Color(0x2e2e2e));

        JLabel titleLabel = new JLabel("RAGESCRIPTS");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(0xcccccc));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        leftPanel.setOpaque(false);
        leftPanel.add(titleLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        rightPanel.setOpaque(false);

        JButton minimizeBtn = createCircleButton(new Color(0x6BEAFF));
        minimizeBtn.addActionListener(e -> setState(JFrame.ICONIFIED));

        JButton closeBtn = createCircleButton(new Color(0xFF7C77));
        closeBtn.addActionListener(e -> {
            try {
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException ex) {}
            System.exit(0);
        });

        rightPanel.add(minimizeBtn);
        rightPanel.add(closeBtn);

        final Point[] dragPoint = {null};
        MouseAdapter dragListener = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragPoint[0] = e.getPoint();
            }

            public void mouseDragged(MouseEvent e) {
                if (dragPoint[0] != null) {
                    Point currentLocation = getLocation();
                    setLocation(currentLocation.x + e.getX() - dragPoint[0].x,
                            currentLocation.y + e.getY() - dragPoint[0].y);
                }
            }
        };

        titleBar.addMouseListener(dragListener);
        titleBar.addMouseMotionListener(dragListener);
        titleBar.add(leftPanel, BorderLayout.WEST);
        titleBar.add(rightPanel, BorderLayout.EAST);

        rootPanel.add(titleBar, BorderLayout.NORTH);
    }

    private JButton createCircleButton(Color color) {
        JButton btn = new JButton() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(16, 16);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillOval(0, 0, getWidth(), getHeight());

                super.paintComponent(g2);
                g2.dispose();
            }

            @Override
            public boolean contains(int x, int y) {
                int radius = Math.min(getWidth(), getHeight()) / 2;
                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;
                int dx = x - centerX;
                int dy = y - centerY;
                return dx * dx + dy * dy <= radius * radius;
            }
        };

        btn.setBackground(color);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        return btn;
    }
    private void createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(15, 25, 15, 25));
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        headerPanel.setBackground(new Color(0x131313));

        JLabel titleLabel = new JLabel("PVP SCRIPTS MINECRAFT");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 26));
        titleLabel.setForeground(new Color(0xcccccc));

        JLabel subtitleLabel = new JLabel("Scripts designed for Minecraft PvP that simplify gameplay.");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        subtitleLabel.setForeground(new Color(0xaaaaaa));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);

        mainPanel.add(headerPanel);
    }

    private void createWindowFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        panel.setOpaque(false);

        JLabel label = new JLabel("Active window:");
        label.setForeground(new Color(0xcccccc));
        label.setFont(new Font("Arial", Font.PLAIN, 16));

        windowFilterField = new JTextField("Minecraft", 20);
        windowFilterField.setFont(new Font("Arial", Font.PLAIN, 16));
        windowFilterField.setBackground(new Color(0x3a3a3a));
        windowFilterField.setForeground(new Color(0xcccccc));
        windowFilterField.setCaretColor(new Color(0xcccccc));

        panel.add(label);
        panel.add(windowFilterField);

        mainPanel.add(panel);
    }

    private void createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        panel.setOpaque(false);

        statusLabel = new JLabel("Status: Disable");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusLabel.setForeground(new Color(0x5EFF00));

        slotLabel = new JLabel("Current slot: 1");
        slotLabel.setFont(new Font("Arial", Font.BOLD, 16));
        slotLabel.setForeground(new Color(0xcccccc));

        panel.add(statusLabel);
        panel.add(Box.createHorizontalStrut(50));
        panel.add(slotLabel);

        mainPanel.add(panel);
    }

    private void createBindsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 4, 15, 10));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
        panel.setBorder(new EmptyBorder(15, 0, 15, 0));
        panel.setOpaque(false);

        bindPanels = new ArrayList<>();
        for (String item : itemBinds.keySet()) {
            ItemBindPanel ibp = new ItemBindPanel(item);
            bindPanels.add(ibp);
            panel.add(ibp);
        }

        mainPanel.add(panel);
    }

    private void createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        panel.setOpaque(false);

        toggleButton = new JButton("Enable");
        toggleButton.setFont(new Font("Arial", Font.BOLD, 18));
        toggleButton.setPreferredSize(new Dimension(160, 40));
        toggleButton.setBackground(new Color(0x171515));
        toggleButton.setForeground(new Color(0xcccccc));
        toggleButton.setFocusPainted(false);
        toggleButton.addActionListener(e -> toggleRunning());
        panel.add(toggleButton);

        mainPanel.add(panel);
    }

    private void toggleRunning() {
        isRunning = !isRunning;
        statusLabel.setText("Status: " + (isRunning ? "Enable" : "Disable"));
        statusLabel.setForeground(isRunning ? new Color(0x5cb85c) : new Color(0x66FF00));
        toggleButton.setText(isRunning ? "Disable" : "Enable");
    }

    private boolean isMinecraftActiveWindow() {
        String filter = windowFilterField.getText().trim().toLowerCase();
        WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        char[] buffer = new char[512];
        User32.INSTANCE.GetWindowText(hwnd, buffer, 512);
        String current = Native.toString(buffer).toLowerCase();
        return current.contains(filter);
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (!isRunning || !isMinecraftActiveWindow()) return;

        int code = e.getKeyCode();
        if (code >= NativeKeyEvent.VC_1 && code <= NativeKeyEvent.VC_9) {
            lastSlot = code - NativeKeyEvent.VC_1 + 1;
            SwingUtilities.invokeLater(() -> slotLabel.setText("Current slot: " + lastSlot));
        }

        String keyChar = NativeKeyEvent.getKeyText(code).toUpperCase();
        for (String item : bindKeys.keySet()) {
            if (bindKeys.get(item).equalsIgnoreCase(keyChar)) {
                performBindAction(item);
                break;
            }
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
    }

    private void performBindAction(String item) {
        if (!activeActions.add(item)) return;

        Integer slot = itemBinds.get(item);
        if (slot == null) {
            activeActions.remove(item);
            return;
        }

        new Thread(() -> {
            try {
                robot.keyPress(KeyEvent.VK_1 + slot - 1);
                Thread.sleep(50);
                robot.keyRelease(KeyEvent.VK_1 + slot - 1);
                Thread.sleep(50);

                robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                Thread.sleep(50);
                robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                Thread.sleep(100);

                robot.keyPress(KeyEvent.VK_1);
                Thread.sleep(50);
                robot.keyRelease(KeyEvent.VK_1);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                activeActions.remove(item);
            }
        }).start();
    }

    private class ItemBindPanel extends JPanel {
        private String itemName;
        private JTextField keyField;
        private JComboBox<Integer> slotComboBox;

        public ItemBindPanel(String item) {
            this.itemName = item;
            setLayout(new BorderLayout(10, 0));
            setBackground(new Color(0x272727));
            setBorder(BorderFactory.createLineBorder(new Color(0x3a3a3a), 2));
            setPreferredSize(new Dimension(250, 60));

            JLabel nameLabel = new JLabel(itemName);
            nameLabel.setFont(new Font("Arial", Font.BOLD, 18));
            nameLabel.setForeground(new Color(0xcccccc));
            nameLabel.setBorder(new EmptyBorder(5, 10, 5, 10));

            keyField = new JTextField(bindKeys.getOrDefault(itemName, ""));
            keyField.setFont(new Font("Arial", Font.BOLD, 18));
            keyField.setHorizontalAlignment(JTextField.CENTER);
            keyField.setBackground(new Color(0x1e1e1e));
            keyField.setForeground(new Color(0x5EFF00));
            keyField.setBorder(BorderFactory.createLineBorder(new Color(0x3a3a3a), 2));
            keyField.setPreferredSize(new Dimension(50, 40));

            keyField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    String text = keyField.getText();
                    if (text.length() > 0) {
                        e.consume();
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    String text = keyField.getText().toUpperCase();
                    if (!text.isEmpty() && text.length() == 1) {
                        bindKeys.put(itemName, text);
                    }
                }
            });

            slotComboBox = new JComboBox<>();
            for (int i = 1; i <= 9; i++) {
                slotComboBox.addItem(i);
            }
            slotComboBox.setSelectedItem(itemBinds.getOrDefault(itemName, 1));
            slotComboBox.setPreferredSize(new Dimension(60, 40));
            slotComboBox.setFont(new Font("Arial", Font.BOLD, 18));
            slotComboBox.setBackground(new Color(0x1e1e1e));
            slotComboBox.setForeground(new Color(0x5EFF00));
            slotComboBox.setBorder(BorderFactory.createLineBorder(new Color(0x3a3a3a), 2));

            slotComboBox.addActionListener(e -> {
                Integer selectedSlot = (Integer) slotComboBox.getSelectedItem();
                if (selectedSlot != null) {
                    itemBinds.put(itemName, selectedSlot);
                }
            });

            JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 10));
            rightPanel.setOpaque(false);
            rightPanel.add(keyField);
            rightPanel.add(slotComboBox);

            add(nameLabel, BorderLayout.WEST);
            add(rightPanel, BorderLayout.EAST);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            try {
                new MinecraftHotbarManager();
            } catch (AWTException e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
    }
}
