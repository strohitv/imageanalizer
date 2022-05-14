package tv.strohi.twitch.imageanalyzer.analyzer;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import tv.strohi.twitch.imageanalyzer.analyzer.model.ColorsBody;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Analyzer {
    private static final List<IIOImage> images = new ArrayList<>();

    private final ObjectMapper mapper = new ObjectMapper();

    public Analyzer() {
        try {
//            process = Runtime.getRuntime().exec(
//                    "R:\\roh\\ffmpeg\\ffmpeg.exe -f dshow -rtbufsize 702000k -i video=\"OBS Virtual Camera\" -filter:v fps=2 -qscale:v 1 -vf scale=1920:1080 -f image2pipe -"
//            );


            new Thread(() -> {
                try {
                    while (true) {
                        boolean isAvailable = false;
                        try {
                            isAvailable = Boolean.parseBoolean(isAvailable());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        if (isAvailable) {
                            Process process = Runtime.getRuntime().exec(
                                    "R:\\roh\\ffmpeg\\ffmpeg.exe -f dshow -i video=\"OBS Virtual Camera\" -qscale:v 1 -vf scale=1920:1080 -f image2pipe -"
                            );

                            if (process.getErrorStream().available() > 0) {
                                System.out.println(new String(process.getErrorStream().readNBytes(process.getErrorStream().available())));
                            }

                            readImages(process.getInputStream(), process);

                            if (process.getErrorStream().available() > 0) {
                                System.out.println(new String(process.getErrorStream().readNBytes(process.getErrorStream().available())));
                            }
                        }
                    }
                } catch (IOException x) {
                    x.printStackTrace();
                }
            }).start();

            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath("tess4j");
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SPARSE_TEXT);
            tesseract.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_TESSERACT_ONLY);
            tesseract.setTessVariable("user_defined_dpi", "71");

            Instant time = Instant.now();
            int fps = 0;
            int imageNr = 0;
            String lastMessage = "";

            try {
                while (true) {
                    if (Instant.now().isAfter(time.plus(10, ChronoUnit.SECONDS))) {
                        // new second
                        System.out.printf("I'm still alive - list size: %d - response (none = good): %s%n", images.size(), lastMessage.trim());
                        fps = 0;
                        time = Instant.now();
                    }

                    if (images.size() > 0) {
                        IIOImage img = images.remove(0);
                        BufferedImage bufferedImage = (BufferedImage) img.getRenderedImage();
                        BufferedImage bufferedSubImage = bufferedImage.getSubimage(905, 53, 109, 48);

                        imageNr++;
                        if (imageNr % 100 == 0) {
                            System.out.printf("saving frame # %d to hard drive...%n", imageNr);
                            writeImage(bufferedImage);
                        }

                        String result = tesseract.doOCR(bufferedSubImage)
                                .trim()
                                .replace("'!", "9")
                                .replace("-", ":")
                                .replaceAll("[^0-9:]", "");

                        if (!result.isBlank()) {
                            if (result.matches("[0-9]:[0-9][0-9]")) {
                                fps++;
                                // System.out.println(result);


                                int[] ownColor = getPixelColor(bufferedImage, 576, 42);
                                int[] enemyColor = getPixelColor(bufferedImage, 1346, 42);

                                ColorsBody body = new ColorsBody(ownColor, enemyColor);

                                lastMessage = sendToBot(body);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void writeImage(BufferedImage image) {
        try {
            ImageIO.write(image, "png", new File("images\\currentImage.png"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private String sendToBot(ColorsBody body) throws IOException {
        System.out.println("sending image to bot...");

        URL url = new URL("http://192.168.178.32:8080/colors");
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection) con;
        http.setRequestMethod("POST"); // PUT is another valid option
        http.setDoOutput(true);
        http.setRequestProperty("Content-Type", "application/json");
        http.connect();
        try (OutputStream os = http.getOutputStream()) {
            os.write(mapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8));
        }

        return new String(http.getInputStream().readAllBytes());
    }

    private static String isAvailable() throws IOException {
        URL url = new URL("http://192.168.178.32:8080/colors/active");
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection) con;
        http.setRequestMethod("GET"); // PUT is another valid option
        http.connect();

        return new String(http.getInputStream().readAllBytes());
    }

    private static final int MAX_IMAGE_SIZE = Integer.MAX_VALUE;

    private static void readImages(InputStream stream, Process finalProcess) throws IOException {
        Instant lastAddedImage = Instant.now();
        Instant lastMilliChangedNotification = Instant.now();
        int waitMillis = 500;

        Instant nextRestart = Instant.now().plus(1, ChronoUnit.MINUTES);

        stream = new BufferedInputStream(stream);
        while (true) {
            stream.mark(MAX_IMAGE_SIZE);

            ImageInputStream imgStream = ImageIO.createImageInputStream(stream);

            Iterator<ImageReader> i = ImageIO.getImageReaders(imgStream);
            if (!i.hasNext()) {
                System.out.println("No ImageReaders found, exiting.");
                break;
            }

            ImageReader reader = i.next();
            reader.setInput(imgStream);

            if (finalProcess.getErrorStream().available() > 0) {
                finalProcess.getErrorStream().readNBytes(finalProcess.getErrorStream().available());
            }

            IIOImage image = reader.readAll(0, null);
            if (image == null) {
                System.out.println("No more images to read, exiting.");
                break;
            } else {
                Instant now = Instant.now();
                boolean isAvailable = false;

                try {
                    isAvailable = Boolean.parseBoolean(isAvailable());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                if (isAvailable && now.isBefore(nextRestart)) {
                    if (now.isAfter(lastAddedImage.plus(waitMillis, ChronoUnit.MILLIS))) {
                        // try to balance the image load speed so that there are around 2 to 3 images in queue at all times
                        if (images.size() != 2 && images.size() != 3 && waitMillis <= 995 && waitMillis >= 15) {
                            if (images.size() >= 5) {
                                waitMillis += 5;
                            } else if (images.size() == 4) {
                                waitMillis += 1;
                            } else if (images.size() == 1) {
                                waitMillis -= 1;
                            } else { // images.size() == 0
                                waitMillis -= 5;
                            }

                            if (now.isAfter(lastMilliChangedNotification.plus(10, ChronoUnit.SECONDS))) {
                                System.out.printf("wait millis = %d ms%n", waitMillis);
                                lastMilliChangedNotification = now;
                            }
                        }

                        while (images.size() >= 5) {
//                            images.remove(images.size() - 1);
                            images.remove(0); // idea: remove old images instead of the newer ones, especially since the oldest one will be ca. 2.5 to 3 seconds old in that case
                        }

                        images.add(image);
                        lastAddedImage = now;
                    }
                } else {
                    images.clear();
                    finalProcess.destroy();
                    return;
                }
            }

            long bytesRead = imgStream.getStreamPosition();

            stream.reset();
            stream.skip(bytesRead);
        }
    }

    private int[] getPixelColor(BufferedImage image, int x, int y) {
        int clr = image.getRGB(x, y);
        int red = (clr & 0x00ff0000) >> 16;
        int green = (clr & 0x0000ff00) >> 8;
        int blue = clr & 0x000000ff;
        // System.out.printf("%d %d %d%n", red, green, blue);

        return new int[]{red, green, blue};
    }
}
