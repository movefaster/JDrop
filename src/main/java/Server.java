import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * The {@code Server} class is both the server and the client for JDrop. By default, it listens to incoming connections
 * from other JDrop clients and sends text or file to other JDrop clients through port 10001.
 * The protocol for transmitting over a socket is as follows:
 * [6-bit code] [type (either "FILE" or "TEXT")]
 * If "FILE" type: [filename]\n[file size]\n[payload]
 * If "TEXT" type: [payload]\n
 */
public class Server {
    public static final int DEFAULT_PORT = 10001;
    public static final int DEFAULT_CHUNK = 8192;
    public static final Logger LOG = Logger.getGlobal();

    private ServerSocket serverSocket;
    private Socket socket;
    private Random random;
    private StringProperty code;
    private Consumer<Throwable> onErrorListener;

    public Server(final Consumer<Throwable> onErrorListener) {
        this.onErrorListener = onErrorListener;
        random = new Random();
        code = new SimpleStringProperty();
        renewCode();

        new Thread(() -> {
            while (true) {
                try {
                    serverSocket = new ServerSocket(DEFAULT_PORT);
                    LOG.log(Level.INFO, "Server listening on port " + DEFAULT_PORT);
                    Server.this.accept(serverSocket.accept());
                    serverSocket.close();
                } catch (IOException e) {
                    onErrorListener.accept(e);
                }
            }
        }).start();
    }

    private void accept(Socket socket) {
        LOG.log(Level.INFO, String.format("Received incoming connection from %s:%d through local port %d",
                socket.getInetAddress(), socket.getPort(), socket.getLocalPort()));
        try {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            Scanner in = new Scanner(inputStream);
            String code = in.next();
            if (!code.equals(this.code.get())) {
                LOG.log(Level.INFO, "Code mismatch. Disconnecting.");
                disconnect(socket);
                in.close();
                return;
            }
            String type = in.next();
            switch (type) {
                case "FILE":
                    acceptFile(in, inputStream);
                    break;
                case "TEXT":
                    readText(in);
                    break;
                default:
                    LOG.log(Level.WARNING, "Unrecognized type: " + type + ". Disconnecting.");
                    disconnect(socket);
                    in.close();
                    break;
            }
        } catch (IOException e) {
            onErrorListener.accept(e);
        }
    }

    private void acceptFile(Scanner in, InputStream stream) {
        String filename = in.nextLine().trim();
        long size = in.nextLong();
        final File[] temp = new File[1];
//        try {
//            temp = File.createTempFile("JDrop", ".tmp");
//            long counter = 0;
//            byte[] buffer = new byte[DEFAULT_CHUNK];
//            FileOutputStream out = new FileOutputStream(temp);
//            while (stream.available() > 0) {
//                int numBytes = stream.read(buffer);
//                counter += numBytes;
//                out.write(buffer, 0, numBytes);
//            }
//            LOG.log(Level.INFO, "Written " + counter + " bytes to " + temp.getAbsolutePath());
//        } catch (IOException e) {
//            onErrorListener.accept(e);
//            return;
//        }

        Platform.runLater(() -> {
            new AlertBuilder(Alert.AlertType.CONFIRMATION)
                    .setTitle("Incoming")
                    .setMessage(String.format("Incoming file: %s (%s). Do you want to accept?",
                            filename, humanReadableByteCount(size, false)))
                    .setPositive(r -> {
                        File file = new FileChooserBuilder()
                                .setTitle("Save File")
                                .setPath(new File(System.getProperty("user.home")))
                                .setFilename(filename)
                                .showSaveDialog(null);
                        if (file != null) {
//                            try {
//                                Files.move(temp[0].toPath(), file.toPath(),
//                                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
//                            } catch (AtomicMoveNotSupportedException e) {
//                                try {
//                                    Files.move(temp[0].toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
//                                } catch (IOException e1) {
//                                    onErrorListener.accept(e1);
//                                }
//                            } catch (IOException e) {
//                                onErrorListener.accept(e);
//                            }
                            readFile(file, stream, size);
                        }
                    }).showAndWait();
        });
    }

    private void readFile(final File file, InputStream stream, long size) {
        Task<Long> writeFileTask = new Task<Long>() {
            @Override
            protected Long call() throws Exception {
                LOG.log(Level.INFO, "Starting to write file...");
                long counter = 0;
                try {
                    byte[] buffer = new byte[DEFAULT_CHUNK];
                    FileOutputStream out = new FileOutputStream(file);
                    while (stream.available() > 0) {
                        int numBytes = stream.read(buffer);
                        out.write(buffer, 0, numBytes);
                        counter += numBytes;
                        updateProgress(counter, size);
                    }
                    LOG.log(Level.INFO, "Written " + counter + " bytes to " + file.getAbsolutePath());
                    out.close();
                } catch (IOException e) {
                    onErrorListener.accept(e);
                    return 0L;
                }
                return counter;
            }
        };

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setMaxWidth(Double.MAX_VALUE);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefWidth(grid.getWidth());
        grid.add(progressBar, 0, 0);

        Alert progressAlert = new AlertBuilder(Alert.AlertType.INFORMATION)
                .setTitle("Receiving file")
                .setHeaderText("Saving file to " + file.getAbsolutePath())
                .addCustomPane(grid)
                .setPositive("Cancel", r -> {
                    writeFileTask.cancel();
                }).get();
        progressAlert.show();

        long time = System.nanoTime();
        new Thread(writeFileTask).start();

        writeFileTask.setOnSucceeded(v -> {
//            progressAlert.close();
            new AlertBuilder(Alert.AlertType.INFORMATION)
                    .setTitle("Complete")
                    .setMessage(String.format("File has been saved to %s.\nTime: %6.3f seconds",
                            file.getAbsolutePath(), (System.nanoTime() - time) / 1e9))
                    .showAndWait();
            try {

                stream.close();
            } catch (IOException e) {
                onErrorListener.accept(e);
            }
            renewCode();
        });
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private void readText(Scanner in) {
        Pattern delimiter = in.delimiter();
        in.useDelimiter("\\z");
        String text = in.next();
        displayText(text);
        in.useDelimiter(delimiter);
    }

    private void displayText(String text) {
        Platform.runLater(() -> {
            new AlertBuilder(Alert.AlertType.INFORMATION)
                    .setTitle("Incoming text")
                    .addTextArea(text)
                    .showAndWait();
        });
    }

    private void disconnect(Socket socket) {
        try {
            LOG.log(Level.WARNING, "Disconnecting from " + socket.getInetAddress());
            socket.close();
        } catch (IOException e) {
            onErrorListener.accept(e);
        }
    }

    void sendText(String host, String code, String text) {
        try {
            LOG.log(Level.INFO, "Connecting to " + host + ":" + DEFAULT_PORT);
            socket = new Socket(host, DEFAULT_PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            String msg = String.format("%s TEXT %s", code, text);
            out.println(msg);
            out.close();
            LOG.log(Level.INFO, "Written " + msg + " to socket OutputStream");
            socket.close();
        } catch (IOException e) {
            onErrorListener.accept(e);
        }
    }

    void sendFile(String host, String code, File file) {
        try {
            FileInputStream in = new FileInputStream(file);
            LOG.log(Level.INFO, "Connecting to " + host + ":" + DEFAULT_PORT);
            socket = new Socket(host, DEFAULT_PORT);
            OutputStream out = socket.getOutputStream();
            byte[] msg = String.format("%s FILE %s\n%d\n", code, file.getName(), file.length()).getBytes();
            out.write(msg);
            long numBytes = writeFile(in, out);
            LOG.log(Level.INFO, "Written " + numBytes + " bytes to socket OutputStream");
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            onErrorListener.accept(e);
        }
    }

    private long writeFile(InputStream in, OutputStream out) {
        long counter = 0;
        byte[] buffer = new byte[DEFAULT_CHUNK];
        try {
            out.write(buffer, 0, DEFAULT_CHUNK);
            while (in.available() > 0) {
                int numBytes = in.read(buffer);
                counter += numBytes;
                out.write(buffer, 0, numBytes);
            }
        } catch (IOException e) {
            onErrorListener.accept(e);
        }
        return counter;
    }

    private void renewCode() {
        int code = random.nextInt(1000000);
        Platform.runLater(() -> Server.this.code.setValue(String.format("%06d", code)));
        LOG.log(Level.INFO, "New Code=" + this.code.get());
    }

    public String getCode() {
        return code.get();
    }
    public StringProperty codeProperty() {
        return code;
    }
}