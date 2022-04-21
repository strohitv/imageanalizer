package tv.strohi.twitch.imageanalyzer.analyzer;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
public class Analyzer {
    private static List<BufferedImage> images = new ArrayList<>();

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

                    InputStream inputStream = finalProcess.getInputStream();
                    //do whatever has to be done with inputStream

                    readImages(inputStream, finalProcess);

                    if (finalProcess.getErrorStream().available() > 0) {
                        System.out.println(new String(finalProcess.getErrorStream().readNBytes(finalProcess.getErrorStream().available())));
                    }
                } catch (IOException x) {
                    x.printStackTrace();
                }
            }).start();

            new Thread(() -> {
                int number = 0;
                try {
                    while (true) {
                        if (images.size() > 0) {
                            BufferedImage img = images.remove(0);
                            ImageIO.write(img, "png", new File("images\\selfie_" + number + ".png"));
                            number++;
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

            BufferedImage image = reader.read(0);
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

    @Scheduled(initialDelay = 1000, fixedRate = 10000)
    public void analyseFrame() {
    }
}
