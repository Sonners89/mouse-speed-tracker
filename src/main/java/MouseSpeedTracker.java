import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MouseSpeedTracker extends JFrame {
    private static final double DPI = 1000.0; // Настройте DPI вашей мыши
    private static final double INCH_TO_METER = 0.0254;

    private long lastTime = 0;
    private double lastX = 0;
    private double lastY = 0;
    private double currentSpeed = 0.0;
    private double maxSpeed = 0.0;
    private final Timer updateTimer;

    private final JLabel currentSpeedLabel;
    private final JLabel maxSpeedLabel;

    public MouseSpeedTracker() {
        setTitle("Mouse Speed Tracker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 250);
        setLayout(new GridBagLayout());

        // Установка FlatLaf для современного вида
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
        currentSpeedLabel.setFont(new Font("Arial", Font.BOLD, 16));
        currentSpeedLabel.setForeground(new Color(0, 100, 0));
        gbc.gridx = 1;
        add(currentSpeedLabel, gbc);

        // Максимальная скорость
        gbc.gridy = 2;
        gbc.gridx = 0;
        add(new JLabel("Максимальная скорость:"), gbc);

        maxSpeedLabel = new JLabel("0.000 m/s", SwingConstants.RIGHT);
        maxSpeedLabel.setFont(new Font("Arial", Font.BOLD, 16));
        maxSpeedLabel.setForeground(new Color(150, 0, 0));
        gbc.gridx = 1;
        add(maxSpeedLabel, gbc);

        // Кнопка сброса
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JButton resetButton = new JButton("Сбросить максимальную скорость");
        resetButton.addActionListener(e -> {
            maxSpeed = 0.0;
            updateLabels();
        });
        add(resetButton, gbc);

        // Информация
        gbc.gridy = 4;
        JLabel infoLabel = new JLabel("<html><div style='text-align: center;'>"
                + "DPI: " + DPI + "<br>"
                + "Перемещайте мышь для измерения скорости"
                + "</div></html>", SwingConstants.CENTER);
        infoLabel.setForeground(Color.GRAY);
        add(infoLabel, gbc);

        // Настройка отслеживания мыши
        setupMouseTracking();

        // Таймер для периодического обновления
        updateTimer = new Timer(10, e -> updateLabels());
        updateTimer.start();

        // Обработчик закрытия окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                updateTimer.stop();
            }
        });

        setLocationRelativeTo(null);
    }

    private void setupMouseTracking() {
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                calculateSpeed(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                calculateSpeed(e.getX(), e.getY());
            }
        });

        // Также отслеживаем движение мыши вне окна, если окно активно
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) event;
                if (me.getID() == MouseEvent.MOUSE_MOVED && isActive()) {
                    Point windowPoint = getLocationOnScreen();
                    Point mousePoint = me.getLocationOnScreen();
                    int relativeX = mousePoint.x - windowPoint.x;
                    int relativeY = mousePoint.y - windowPoint.y;

                    // Проверяем, находится ли мышь в пределах окна
                    if (relativeX >= 0 && relativeX <= getWidth() &&
                            relativeY >= 0 && relativeY <= getHeight()) {
                        calculateSpeed(relativeX, relativeY);
                    }
                }
            }
        }, AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    private void calculateSpeed(double x, double y) {
        long currentTime = System.currentTimeMillis();

        if (lastTime == 0) {
            lastTime = currentTime;
            lastX = x;
            lastY = y;
            return;
        }

        long timeDiff = currentTime - lastTime;

        if (timeDiff > 0) {
            // Вычисляем расстояние в пикселях
            double dx = x - lastX;
            double dy = y - lastY;
            double distancePixels = Math.sqrt(dx * dx + dy * dy);

            // Конвертируем в метры
            double distanceMeters = (distancePixels / DPI) * INCH_TO_METER;

            // Вычисляем скорость (м/с)
            double timeSeconds = timeDiff / 1000.0;
            currentSpeed = distanceMeters / timeSeconds;

            // Обновляем максимальную скорость
            if (currentSpeed > maxSpeed) {
                maxSpeed = currentSpeed;
            }

            lastTime = currentTime;
            lastX = x;
            lastY = y;

            updateLabels();
        }
    }

    private void updateLabels() {
        currentSpeedLabel.setText(String.format("%.3f m/s", currentSpeed));
        maxSpeedLabel.setText(String.format("%.3f m/s", maxSpeed));

        // Плавное уменьшение текущей скорости, если мышь не двигается
        if (System.currentTimeMillis() - lastTime > 100) {
            currentSpeed *= 0.9;
            if (currentSpeed < 0.001) {
                currentSpeed = 0.0;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MouseSpeedTracker tracker = new MouseSpeedTracker();
            tracker.setVisible(true);
        });
    }
}