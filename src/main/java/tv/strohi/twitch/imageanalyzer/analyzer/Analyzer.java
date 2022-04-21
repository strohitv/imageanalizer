package tv.strohi.twitch.imageanalyzer.analyzer;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tv.strohi.twitch.imageanalyzer.analyzer.model.ColorsBody;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
public class Analyzer {
    private static final List<IIOImage> images = new ArrayList<>();

    private final ObjectMapper mapper = new ObjectMapper();

    public Analyzer() {
        Process process;
        try {
            process = Runtime.getRuntime().exec(
                    "R:\\roh\\ffmpeg\\ffmpeg.exe -f dshow -rtbufsize 702000k -i video=\"OBS Virtual Camera\" -filter:v fps=2 -qscale:v 1 -f image2pipe -"
            );
            for (String format : ImageIO.getReaderFormatNames())
                System.out.println(format);

            Process finalProcess = process;
            new Thread(() -> {
                try {
                    if (finalProcess.getErrorStream().available() > 0) {
                        System.out.println(new String(finalProcess.getErrorStream().readNBytes(finalProcess.getErrorStream().available())));
                    }

                    readImages(finalProcess.getInputStream(), finalProcess);

                    if (finalProcess.getErrorStream().available() > 0) {
                        System.out.println(new String(finalProcess.getErrorStream().readNBytes(finalProcess.getErrorStream().available())));
                    }
                } catch (IOException x) {
                    x.printStackTrace();
                }
            }).start();

            new Thread(() -> {
                Tesseract tesseract = new Tesseract();
                tesseract.setDatapath("tess4j");
                tesseract.setLanguage("eng");
                tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SPARSE_TEXT);
                tesseract.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_TESSERACT_ONLY);
                tesseract.setTessVariable("user_defined_dpi", "71");

                try {
                    while (true) {
                        if (finalProcess.getErrorStream().available() > 0) {
                            finalProcess.getErrorStream().readNBytes(finalProcess.getErrorStream().available());
                        }

                        if (images.size() > 0) {
                            IIOImage img = images.remove(0);
                            BufferedImage bufferedImage = (BufferedImage) img.getRenderedImage();
                            BufferedImage bufferedSubImage = bufferedImage.getSubimage(905, 53, 109, 48);

                            String result = tesseract.doOCR(bufferedSubImage)
                                    .trim()
                                    .replace("'!", "9")
                                    .replace("-", ":")
                                    .replaceAll("[^0-9:]", "");

                            if (!result.isBlank()) {
                                if (result.matches("[0-9]:[0-9][0-9]")) {
                                    System.out.println(result);

                                    int[] ownColor = getPixelColor(bufferedImage, 575, 42);
                                    int[] enemyColor = getPixelColor(bufferedImage, 1085, 42);

                                    ColorsBody body = new ColorsBody(ownColor, enemyColor);

                                    sendToBot(body);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException ignored) {
            System.out.println("nix");
        }
    }

    private void sendToBot(ColorsBody body) throws IOException {
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
        System.out.println(new String(http.getInputStream().readAllBytes()));
    }

    private static final int MAX_IMAGE_SIZE = Integer.MAX_VALUE;

    static void readImages(InputStream stream, Process finalProcess) throws IOException {
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
                if (images.size() < 10) {
                    images.add(image);
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
        System.out.printf("%d %d %d%n", red, green, blue);

        return new int[]{red, green, blue};
    }

    @Scheduled(initialDelay = 1000, fixedRate = 10000)
    public void analyseFrame() {
    }
}
