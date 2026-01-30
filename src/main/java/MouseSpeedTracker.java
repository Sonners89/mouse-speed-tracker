import com.formdev.flatlaf.FlatLightLaf;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MouseSpeedTracker extends JFrame {
    private static final double DPI = 1000.0;
    private static final double INCH_TO_METER = 0.0254;

    private double currentSpeed = 0.0;
    private double maxSpeed = 0.0;
    private long lastTime = 0;
    private int lastX = 0, lastY = 0;

    private final JLabel currentSpeedLabel;
    private final JLabel maxSpeedLabel;
    private final JLabel latencyLabel;

    private volatile boolean running = true;
    private Thread pollThread;

    public MouseSpeedTracker() {
        setTitle("Mouse Speed Tracker - Ultra Low Latency");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 300);
        setLayout(new GridBagLayout());

        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Заголовок
        JLabel titleLabel = new JLabel("Mouse Speed Tracker", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(titleLabel, gbc);

        // Текущая скорость
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        add(new JLabel("Текущая скорость:"), gbc);

        currentSpeedLabel = new JLabel("0.000 m/s", SwingConstants.RIGHT);
        currentSpeedLabel.setFont(new Font("Arial", Font.BOLD, 20));
        currentSpeedLabel.setForeground(new Color(0, 100, 0));
        gbc.gridx = 1;
        add(currentSpeedLabel, gbc);

        // Максимальная скорость
        gbc.gridy = 2;
        gbc.gridx = 0;
        add(new JLabel("Максимальная скорость:"), gbc);

        maxSpeedLabel = new JLabel("0.000 m/s", SwingConstants.RIGHT);
        maxSpeedLabel.setFont(new Font("Arial", Font.BOLD, 20));
        maxSpeedLabel.setForeground(new Color(150, 0, 0));
        gbc.gridx = 1;
        add(maxSpeedLabel, gbc);

        // Задержка
        gbc.gridy = 3;
        gbc.gridx = 0;
        add(new JLabel("Задержка:"), gbc);

        latencyLabel = new JLabel("< 1 ms", SwingConstants.RIGHT);
        latencyLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        latencyLabel.setForeground(Color.BLUE);
        gbc.gridx = 1;
        add(latencyLabel, gbc);

        // Кнопка сброса
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JButton resetButton = new JButton("Сбросить максимальную скорость");
        resetButton.addActionListener(e -> {
            maxSpeed = 0.0;
            updateLabels();
        });
        add(resetButton, gbc);

        // Запуск потока опроса
        startPollingThread();

        // Обработчик закрытия окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                running = false;
                if (pollThread != null) {
                    try {
                        pollThread.join(1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        setLocationRelativeTo(null);
    }

    private void startPollingThread() {
        pollThread = new Thread(() -> {
            try {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            } catch (SecurityException e) {
                // Игнорируем
            }

            while (running) {
                long startTime = System.nanoTime();

                // Получаем позицию мыши напрямую через WinAPI
                WinDef.POINT point = new WinDef.POINT();
                User32.INSTANCE.GetCursorPos(point);

                long currentTime = System.currentTimeMillis();

                if (lastTime == 0) {
                    lastTime = currentTime;
                    lastX = point.x;
                    lastY = point.y;
                } else {
                    long timeDiff = currentTime - lastTime;

                    if (timeDiff > 0) {
                        double dx = point.x - lastX;
                        double dy = point.y - lastY;
                        double distancePixels = Math.sqrt(dx * dx + dy * dy);

                        double distanceMeters = (distancePixels / DPI) * INCH_TO_METER;
                        double timeSeconds = timeDiff / 1000.0;

                        double instantSpeed = distanceMeters / timeSeconds;

                        // Экспоненциальное сглаживание
                        currentSpeed = 0.3 * currentSpeed + 0.7 * instantSpeed;

                        if (currentSpeed > maxSpeed) {
                            maxSpeed = currentSpeed;
                        }

                        updateLabels();

                        lastX = point.x;
                        lastY = point.y;
                        lastTime = currentTime;
                    }
                }

                // Вычисление задержки
                long endTime = System.nanoTime();
                long latencyNanos = endTime - startTime;
                updateLatency(latencyNanos);

                // Короткая пауза для уменьшения нагрузки на CPU
                try {
                    Thread.sleep(1); // 1 мс
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        pollThread.setDaemon(true);
        pollThread.start();
    }

    private void updateLabels() {
        SwingUtilities.invokeLater(() -> {
            currentSpeedLabel.setText(String.format("%.3f m/s", currentSpeed));
            maxSpeedLabel.setText(String.format("%.3f m/s", maxSpeed));
        });
    }

    private void updateLatency(long nanos) {
        SwingUtilities.invokeLater(() -> {
            double ms = nanos / 1_000_000.0;
            latencyLabel.setText(String.format("%.2f ms", ms));
        });
    }

    public static void main(String[] args) {
        // Проверяем ОС
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            JOptionPane.showMessageDialog(null,
                    "Эта версия использует WinAPI и работает только на Windows",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            MouseSpeedTracker tracker = new MouseSpeedTracker();
            tracker.setVisible(true);
        });
    }
}