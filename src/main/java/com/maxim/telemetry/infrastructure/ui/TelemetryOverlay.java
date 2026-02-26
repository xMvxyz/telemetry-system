package com.maxim.telemetry.infrastructure.ui;

import com.maxim.telemetry.TelemetrySystemApplication;
import com.maxim.telemetry.application.service.RecordingService;
import com.maxim.telemetry.domain.model.MetricType;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TelemetryOverlay {

    private static final Map<MetricType, Label> labels = new EnumMap<>(MetricType.class);
    private static Label statusLabel;
    private static Label gameLabel;
    private static final Map<MetricType, String> pendingMetrics = new ConcurrentHashMap<>();
    private static String pendingStatus = "IDLE";
    private static String pendingGame = "Detectando...";
    private static boolean isReady = false;

    public static void launchOverlay() {
        new Thread(() -> {
            try {
                Application.launch(OverlayApp.class);
            } catch (Exception e) {}
        }).start();
    }

    public static void updateMetric(MetricType type, String value) {
        if (!isReady) {
            pendingMetrics.put(type, value);
            return;
        }
        Platform.runLater(() -> {
            Label label = labels.get(type);
            if (label != null) {
                label.setText(type.name().replace("_USAGE", "") + ": " + value);
            }
        });
    }

    public static void updateStatus(String status) {
        if (!isReady) {
            pendingStatus = status;
            return;
        }
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(status);
                statusLabel.setStyle(status.contains("REC") ? "-fx-text-fill: #ff4444;" : "-fx-text-fill: #aaaaaa;");
            }
        });
    }

    public static void updateGame(String gameName) {
        if (!isReady) {
            pendingGame = gameName;
            return;
        }
        Platform.runLater(() -> {
            if (gameLabel != null) gameLabel.setText(gameName);
        });
    }

    public static class OverlayApp extends Application {
        private double xOffset = 0;
        private double yOffset = 0;

        @Override
        public void start(Stage primaryStage) {
            primaryStage.initStyle(StageStyle.TRANSPARENT);
            primaryStage.setAlwaysOnTop(true);

            VBox root = new VBox(5);
            root.setPadding(new Insets(10));
            root.setPrefWidth(180); // Un poco mas ancho
            root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-background-radius: 10;");

            Label title = new Label("TELEMETRY");
            title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            
            gameLabel = new Label(pendingGame);
            gameLabel.setStyle("-fx-text-fill: #ffa500; -fx-font-size: 11px; -fx-font-weight: bold;");

            statusLabel = new Label(pendingStatus);
            statusLabel.setStyle("-fx-text-fill: #aaaaaa;");

            root.getChildren().addAll(title, gameLabel, statusLabel);

            setupLabel(MetricType.CPU_USAGE, "#00ff00", root);
            setupLabel(MetricType.RAM_USAGE, "#00ffff", root);
            setupLabel(MetricType.GPU_USAGE, "#ff8800", root);
            setupLabel(MetricType.VRAM_USAGE, "#ffff00", root);
            setupLabel(MetricType.FPS, "#ff00ff", root);

            HBox controls = new HBox(5);
            controls.setAlignment(Pos.CENTER);
            controls.setVisible(false); // Ocultar pero no borrar
            controls.setManaged(false); // No ocupa espacio
            Button btnStart = new Button("REC");
            Button btnStop = new Button("STOP");
            btnStart.setStyle("-fx-font-size: 9px;");
            btnStop.setStyle("-fx-font-size: 9px;");

            btnStart.setOnAction(e -> {
                RecordingService service = TelemetrySystemApplication.getContext().getBean(RecordingService.class);
                service.startRecording(null);
            });
            
            btnStop.setOnAction(e -> {
                RecordingService service = TelemetrySystemApplication.getContext().getBean(RecordingService.class);
                service.stopRecording();
            });

            controls.getChildren().addAll(btnStart, btnStop);
            root.getChildren().add(controls);

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
            primaryStage.show();

            isReady = true;
            pendingMetrics.forEach(TelemetryOverlay::updateMetric);
            updateStatus(pendingStatus);
            updateGame(pendingGame);
        }

        private void setupLabel(MetricType type, String color, VBox root) {
            Label label = new Label(type.name().replace("_USAGE", "") + ": --");
            label.setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'Consolas';");
            labels.put(type, label);
            root.getChildren().add(label);
        }
    }
}
