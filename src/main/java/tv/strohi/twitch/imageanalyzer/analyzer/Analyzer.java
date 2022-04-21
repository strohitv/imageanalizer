package tv.strohi.twitch.imageanalyzer.analyzer;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
public class Analyzer {
//    private static List<BufferedImage> images = new ArrayList<>();
    private static List<IIOImage> images = new ArrayList<>();

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
                Tesseract tesseract = new Tesseract();
                tesseract.setDatapath("tess4j");
                tesseract.setLanguage("eng");
//                tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SINGLE_WORD);
                tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SPARSE_TEXT);
//                tesseract.setOcrEngineMode(1);
                tesseract.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_TESSERACT_ONLY);
//                tesseract.setTessVariable("user_defined_dpi", "213");
                tesseract.setTessVariable("user_defined_dpi", "71");

                int number = 0;
                boolean loadColors = true;
                try {
                    while (true) {
                        if (finalProcess.getErrorStream().available() > 0) {
                            finalProcess.getErrorStream().readNBytes(finalProcess.getErrorStream().available());
                        }

                        if (images.size() > 0) {
                            IIOImage img = images.remove(0);
//                            ImageIO.write(img, "png", new File("images\\selfie_" + number + ".png"));
                            BufferedImage bufferedImage = (BufferedImage) img.getRenderedImage();
//                            ImageIO.write(bufferedImage.getSubimage(905, 53, 109, 48), "png", new File("images\\aaa_selfie.png"));
                            BufferedImage bufferedSubImage = bufferedImage.getSubimage(905, 53, 109, 48);
//                            bufferedSubImage = Scalr.resize(bufferedSubImage, bufferedSubImage.getWidth() * 8, bufferedSubImage.getHeight() * 8);

//                            ImageIO.write(bufferedSubImage, "png", new File("images\\aaa_selfie.png"));
//                            ImageIO.write(bufferedImage, "png", new File("images\\aaab_selfie.png"));
                            String result = tesseract.doOCR(bufferedSubImage)
                                    .trim()
                                    .replace("'!", "9")
                                    .replace("-", ":")
                                    .replaceAll("[^0-9:]", "");

//                            ImageIO.write(bufferedImage.getSubimage(894, 43, 129, 67), "png", new File("images\\aaa_selfie.png"));
//                            String result = tesseract.doOCR(bufferedImage.getSubimage(894, 43, 129, 67)).trim();
                            if (!result.isBlank()) {

                                if (loadColors && Arrays.asList(new String[] {"5:00", "4:59", "4:58", "3:00", "2:59", "2:58"}).contains(result)) {
                                    System.out.println(result);

                                    String ownColor = getPixelColor(bufferedImage, 575, 42);
                                    String enemyColor = getPixelColor(bufferedImage, 1085, 42);
                                    loadColors = false;
                                } else if (!loadColors && Arrays.asList(new String[] {"4:55", "4:54", "4:53", "4:52", "2:55", "2:54", "2:53", "2:52"}).contains(result)) {
                                    System.out.println(result);
                                    loadColors = true;
                                }

                                // pixel eigene Farbe: 575, 42
                                // pixel gegnerische Farbe: 1085, 42
                            }
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

//            BufferedImage image = reader.read(0);
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

    private String getPixelColor(BufferedImage image, int x, int y) {
        int clr = image.getRGB(x, y);
        int red =   (clr & 0x00ff0000) >> 16;
        int green = (clr & 0x0000ff00) >> 8;
        int blue =   clr & 0x000000ff;
        String hex = String.format("#%02X%02X%02X", red, green, blue);
        System.out.println(String.format("%d %d %d",red, green, blue));

        return hex;
//        System.out.println("Red Color value = " + red);
//        System.out.println("Green Color value = " + green);
//        System.out.println("Blue Color value = " + blue);
    }

    @Scheduled(initialDelay = 1000, fixedRate = 10000)
    public void analyseFrame() {
    }
}
