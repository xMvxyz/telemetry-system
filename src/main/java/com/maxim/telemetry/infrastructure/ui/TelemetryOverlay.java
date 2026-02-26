package com.maxim.telemetry.infrastructure.ui;

import com.maxim.telemetry.domain.model.MetricType;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.Map;

@Component
public class TelemetryOverlay {

    private static final Map<MetricType, Label> labels = new EnumMap<>(MetricType.class);
    private static final Label statusLabel = new Label("IDLE");

    @PostConstruct
    public void init() {
        // Only launch if JavaFX hasn't started yet
        new Thread(() -> {
            try {
                Application.launch(OverlayApp.class);
            } catch (IllegalStateException e) {
                // JavaFX already started
            }
        }).start();
    }

    public static void updateMetric(MetricType type, String value) {
        Platform.runLater(() -> {
            Label label = labels.get(type);
            if (label != null) {
                label.setText(type.name().replace("_USAGE", "") + ": " + value);
            }
        });
    }

    public static void updateStatus(String status) {
        Platform.runLater(() -> {
            statusLabel.setText(status);
            if (status.contains("RECORDING")) {
                statusLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-weight: bold;");
            } else {
                statusLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-weight: normal;");
            }
        });
    }

    public static class OverlayApp extends Application {
        private double xOffset = 0;
        private double yOffset = 0;

        @Override
        public void start(Stage primaryStage) {
            primaryStage.initStyle(StageStyle.TRANSPARENT);
            primaryStage.setAlwaysOnTop(true);

            VBox root = new VBox(2);
            root.setPadding(new Insets(10));
            root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-background-radius: 12; -fx-border-color: #555; -fx-border-radius: 12; -fx-border-width: 1;");

            Label title = new Label("🚀 TELEMETRY");
            title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
            root.getChildren().add(title);

            setupLabel(MetricType.CPU_USAGE, "#00ff00", root);
            setupLabel(MetricType.RAM_USAGE, "#00ffff", root);
            setupLabel(MetricType.VRAM_USAGE, "#ffff00", root);
            setupLabel(MetricType.FPS, "#ff00ff", root);

            statusLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 10px;");
            root.getChildren().addAll(new Label(" "), statusLabel);

            // Draggable logic
            root.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            root.setOnMouseDragged(event -> {
                primaryStage.setX(event.getScreenX() - xOffset);
                primaryStage.setY(event.getScreenY() - yOffset);
            });

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            primaryStage.setScene(scene);
            primaryStage.setX(20);
            primaryStage.setY(20);
            primaryStage.show();
        }

        private void setupLabel(MetricType type, String color, VBox root) {
            Label label = new Label(type.name().replace("_USAGE", "") + ": --");
            label.setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'Consolas'; -fx-font-weight: bold; -fx-font-size: 12px;");
            labels.put(type, label);
            root.getChildren().add(label);
        }
    }
}
