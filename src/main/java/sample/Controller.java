package sample;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class Controller {
    @FXML
    Button playBtn;
    @FXML
    TextField filePathText;
    @FXML
    TextField frameDurationText;
    @FXML
    TextField currentStepText;
    @FXML
    TextField totalStepText;
    @FXML
    TextField mainText;
    @FXML
    ImageView imgView;

    private File classFile;

    private ExecutorService playService;

    private ArrayList<Image> images = new ArrayList<>();

    @FXML
    public void initialize() {
        currentStepText.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue,
                                String newValue) {
                if (!newValue.matches("\\d*")) {
                    currentStepText.setText(newValue.replaceAll("[^\\d]", ""));
                }
                if (totalStepText.getText().length() != 0 && currentStepText.getText().length() != 0) {
                    int total = Integer.parseInt(totalStepText.getText());
                    int current = Integer.parseInt(currentStepText.getText());
                    if (current > 0 && current <= total) {
                        imgView.setImage(images.get(current - 1));
                    }
                }
            }
        });

        frameDurationText.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue,
                                String newValue) {
                if (!newValue.matches("\\d*")) {
                    currentStepText.setText(newValue.replaceAll("[^\\d]", ""));
                }
                if (totalStepText.getText().length() != 0 && currentStepText.getText().length() != 0) {
                    int total = Integer.parseInt(totalStepText.getText());
                    int current = Integer.parseInt(currentStepText.getText());
                    if (current > 0 && current <= total) {
                        imgView.setImage(images.get(current - 1));
                    }
                }
            }
        });
    }

    @FXML
    public void browseBtnAction() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Root directory chooser");
        classFile = dc.showDialog(null);
        if (classFile != null) {
            filePathText.setText(classFile.toString());
        } else {
            filePathText.clear();
        }
    }

    @FXML
    public void runBtnAction() {
        if (classFile != null && !mainText.getText().isEmpty()) {
            String mainName = mainText.getText();
            String command = "java -cp " + classFile.toString() + " " + mainName;
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            try {
                ProcessBuilder builder = new ProcessBuilder();
                if (isWindows) {
                    builder.command("cmd.exe", "/c", command);
                } else {
                    builder.command("sh", "-c", command);
                }
                builder.directory(new File(classFile.getParent()));
                Process process = builder.start();

                StringBuffer sb = new StringBuffer();
                StringBuffer eb = new StringBuffer();

                StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), s -> sb.append(s + "\n"));
                StreamGobbler estreamGobbler = new StreamGobbler(process.getErrorStream(), s -> eb.append(s + "\n"));



                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.submit(streamGobbler);
                executorService.submit(estreamGobbler);
                int exitCode = process.waitFor();
                assert exitCode == 0;
                executorService.shutdownNow();

                System.out.println("output: " + sb.toString());
                System.out.println("errors: " + eb.toString());

                images.clear();
                generateSteps(sb.toString()).forEach(s -> images.add(generateImage(s)));
                loadImages();

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void stepBack() {
        if (!totalStepText.getText().isEmpty()) {
            int current = Integer.parseInt(currentStepText.getText());
            if (current > 1) {
                currentStepText.setText((current - 1) + "");
            }
        }
    }

    @FXML
    public void stepForward() {
        if (!totalStepText.getText().isEmpty()) {
            int current = Integer.parseInt(currentStepText.getText());
            int total = Integer.parseInt(totalStepText.getText());
            if (current < total) {
                currentStepText.setText((current + 1) + "");
            }
        }
    }

    @FXML
    public void resetImg() {
        if (!totalStepText.getText().isEmpty()) {
            currentStepText.setText("1");
            if (playService != null) {
                playService.shutdownNow();
                playBtn.setDisable(false);
            }
        }
    }

    @FXML
    public void playImg() {
        if (!totalStepText.getText().isEmpty()) {
            playService = Executors.newSingleThreadExecutor();
            playService.submit(new Thread(() -> {
                try {
                    Platform.runLater(() -> currentStepText.setText("1"));
                    Thread.sleep(200);
                    int current = Integer.parseInt(currentStepText.getText());
                    int total = Integer.parseInt(totalStepText.getText());
                    int delay = 500;
                    if (!frameDurationText.getText().isEmpty()) {
                        delay = Integer.parseInt(frameDurationText.getText());
                    }

                    while (current != total) {
                        Thread.sleep(delay);
                        Platform.runLater(() -> stepForward());
                        current = Integer.parseInt(currentStepText.getText());
                    }
                } catch (InterruptedException exc) {
                    // should not be able to get here...
                    throw new Error("Unexpected interruption");
                }
                playBtn.setDisable(false);
            }));
            playBtn.setDisable(true);
            playService.shutdown();
        }
    }

    private Image generateImage(int[] list) {
        int width = 800;
        int height = 510;
        int widthPad = 100;
        // Constructs a BufferedImage of one of the predefined image types.
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Create a graphics which can be used to draw into the buffered image
        Graphics2D g2d = bufferedImage.createGraphics();

        // fill all the image with white
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, width, height);

        // Drawing the bars
        g2d.setColor(Color.black);
        int rectWidth = (width - widthPad) / list.length;

        for (int i = 0; i < list.length; i++) {
            g2d.setColor(new Color(0, 0, 0, list[i]));
            g2d.fillRect(widthPad / 2 + i * rectWidth, height - (list[i] * 2), rectWidth, list[i] * 2);
        }

        // Disposes of this graphics context and releases any system resources that it is using.
        g2d.dispose();

        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    private ArrayList<int[]> generateSteps(String a) {
        String[] lines = a.split("\\R");
        ArrayList<int[]> steps = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            steps.add(Stream.of(lines[i].split(",")).mapToInt(Integer::parseInt).toArray());
        }

        return steps;
    }

    private void loadImages() {
        if (!images.isEmpty()) {
            totalStepText.setText(images.size() + "");
            currentStepText.setText("1");
            currentStepText.setDisable(false);

        } else {
            totalStepText.setText("");
            currentStepText.setText("");
            currentStepText.setDisable(true);
        }
    }

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }
}
