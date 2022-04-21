package tv.strohi.twitch.imageanalyzer.analyzer;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
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
                    "R:\\roh\\ffmpeg\\ffmpeg.exe -f dshow -rtbufsize 702000k -i video=\"OBS Virtual Camera\" -f image2pipe -blocksize 100000k -"//, // input file
//                    "-" // this tells ffmpeg to outout the result to stdin
            );

//            builder.redirectOutput(new File("selfie.txt"));

//            process.start();

//            while (process.isAlive()) {
//                System.out.println("nix");
//            }

//            System.out.println(new String(process.getErrorStream().readAllBytes()));

            for (String format : ImageIO.getReaderFormatNames())
                System.out.println(format);

            Process finalProcess = process;
            new Thread(() -> {
                try {
                    if (!finalProcess.isAlive()) {
                        System.out.println(new String(finalProcess.getErrorStream().readAllBytes()));
                    }

                    InputStream inputStream = finalProcess.getInputStream();
                    //do whatever has to be done with inputStream

                    readImages(inputStream, finalProcess);

//                    BufferedImage bi;
//                    int count = 0;
//                    while ((bi = ImageIO.read(inputStream)) != null) {
//                        System.out.println(count);
//                        count++;
//
//                        while(inputStream.available() > 8) {
//                            inputStream.mark(8);
//                            int byte1 = inputStream.read();
//                            int byte2 = inputStream.read();
//                            int byte3 = inputStream.read();
//                            int byte4 = inputStream.read();
//                            int byte5 = inputStream.read();
//                            int byte6 = inputStream.read();
//                            int byte7 = inputStream.read();
//                            int byte8 = inputStream.read();
//                            inputStream.reset();
//
//                            if (byte1 == 0x89 && byte2 == 0x50
//                                    && byte3 == 0x4E && byte4 == 0x47
//                                    && byte5 == 0x0D && byte6 == 0x0A
//                                    && byte7 == 0x1A && byte8 == 0x0A
//                            ) {
//                                break;
//                            } else {
//                                inputStream.read();
//                            }
//                        }
//                    }

                    if (!finalProcess.isAlive()) {
                        System.out.println(new String(finalProcess.getErrorStream().readAllBytes()));
                    }

//                    byte[] buffer;
//                    while ((buffer = inputStream.readAllBytes()) != null) {
//                        System.out.println(Arrays.toString(buffer));
//                    }
                } catch (IOException x) {
                    x.printStackTrace();
                }
            }).start();

//            new Thread(() -> {
//                try {
//                    while (true) {
//                        if (images.size() > 0) {
//                            System.out.println(images.size());
////                            while (images.size() > 20) {
////                                images.remove(0);
////                            }
//////                            images.remove(0);
////                            ImageIO.write(images.remove(0), "png", new File("selfie.png"));
//                        }
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }).start();
        } catch (IOException ignored) {
            System.out.println("nix");
        }
    }

    private static final int MAX_IMAGE_SIZE = 1000 * 1920 * 1080;

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

            if (reader.canReadRaster()) {
                if (finalProcess.getErrorStream().available() > 0) {
                    finalProcess.getErrorStream().readNBytes(finalProcess.getErrorStream().available());
//                    System.out.println(new String(finalProcess.getErrorStream().readNBytes(finalProcess.getErrorStream().available())));
                }

                BufferedImage image = reader.read(0);
                if (image == null) {
                    System.out.println("No more images to read, exiting.");
                    break;
                } else {
                    images.add(image);
                    if (images.size() > 40) {
                        images.set(images.size() - 40, null);
                    }
                    System.out.println(images.size());
//                    System.out.println(imgStream.getStreamPosition());
                }
            } else {
                System.out.println("No more images to read, exiting. 3");
                return;
            }

            long bytesRead = imgStream.getStreamPosition();

            stream.reset();
            stream.skip(bytesRead);
        }
    }

//        try {
////            webcam = Webcam.getDefault();
//            webcam = Webcam.getWebcams().stream().filter(wc -> wc.getName().contains("OBS")).findFirst().orElse(null);
//
//            if (webcam != null) {
//                webcam.setCustomViewSizes(WebcamResolution.HD.getSize()); // register custom size
//                webcam.setViewSize(WebcamResolution.HD.getSize());
//
//                webcam.open(true);
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }

    @Scheduled(initialDelay = 1000, fixedRate = 10000)
    public void analyseFrame() {
//        if (webcam != null && webcam.isOpen()) {
//            BufferedImage image = webcam.getImage();
//
//            try {
//                ImageIO.write(image, ImageUtils.FORMAT_JPG, new File("selfie.jpeg"));
//            } catch (IOException ignored) {
//            }
//        }
    }
}
