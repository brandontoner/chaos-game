package com.brandontoner.chaos.game;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ObjIntConsumer;
import java.util.stream.IntStream;

import static java.lang.StrictMath.cos;
import static java.lang.StrictMath.sin;

class Program {
    private static final int[] COLORS =
            {0x00FF0000, 0x0000FF00, 0x000000FF, 0x0000FFFF, 0x00FF00FF, 0x00FFFF00, 0x00FFFFFF};
    private static final Random RANDOM = new Random();

    public static void main(String[] args) throws IOException, InterruptedException {
        int iterationCount = 100 * 1000 * 1000;
        int width = 3840;
        int height = 1600;
        int pointCount = 3;

        Path outputDirectory = Path.of("D:\\ChaosGame\\");
        Files.createDirectories(outputDirectory);

        int[] colors = IntStream.of(COLORS).limit(pointCount).toArray();

        createImage(outputDirectory, iterationCount, width, height, colors);
        createVideo(outputDirectory, iterationCount, width, height, colors, Path.of("D:\\output.mkv"));
    }

    private static void createImage(Path outputDirectory,
                                    int iterationCount,
                                    int width,
                                    int height,
                                    int[] colors) throws IOException {
        createImage(outputDirectory, iterationCount, width, height, colors, (image, value) -> {
        });
    }

    private static void createImage(Path outputDirectory,
                                    int iterationCount,
                                    int width,
                                    int height,
                                    int[] colors,
                                    ObjIntConsumer<? super RenderedImage> callback) throws IOException {

        Point[] points = generatePoints(width, height, colors.length);
        int currentX = width / 2;
        int currentY = height / 2;

        BufferedImage bitmap = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics graphics = bitmap.getGraphics();
        graphics.setColor(Color.BLACK);
        graphics.clearRect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        graphics.dispose();

        for (int iteration = 0; iteration < iterationCount; ++iteration) {
            int choice = RANDOM.nextInt(colors.length);
            int color = colors[choice];

            currentX = (currentX + points[choice].x()) / 2;
            currentY = (currentY + points[choice].y()) / 2;

            bitmap.setRGB(currentX, currentY, color);
            callback.accept(bitmap, iteration);
        }
        ImageIO.write(bitmap,
                      "PNG",
                      outputDirectory.resolve(String.format("%d_%d.png", colors.length, iterationCount)).toFile());
    }

    private static void createVideo(Path framesDirectory,
                                    int iterationCount,
                                    int width,
                                    int height,
                                    int[] colors,
                                    Path outputVideo) throws IOException, InterruptedException {

        int seconds = 60;
        int fps = 30;
        AtomicInteger frameIndex = new AtomicInteger(0);
        int framesCount = seconds * fps;
        int iterationsPerFrame = iterationCount / framesCount;


        createImage(Files.createTempDirectory("foo"), iterationCount, width, height, colors, ((image, iteration) -> {
            if (iteration % iterationsPerFrame == 0) {
                try {
                    int i = frameIndex.getAndIncrement();
                    ImageIO.write(image, "PNG", framesDirectory.resolve(String.format("%05d.png", i)).toFile());
                    System.err.format("Frame %d of %d%n", i, framesCount);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }));

        /*
        Process p = new ProcessBuilder("x264.exe",
                                       "--crf",
                                       "0",
                                       "--fps",
                                       String.valueOf(fps),
                                       "-q",
                                       "0",
                                       "-o",
                                       outputVideo.toAbsolutePath().toString(),
                                       framesDirectory.resolve("%5d.png").toAbsolutePath().toString()).start();
        p.waitFor();
         */
    }

    private static Point[] generatePoints(int width, int height, int pointsCount) {
        Point[] points = new Point[pointsCount];
        double angle = 2.0 * Math.PI / pointsCount;
        int radius = (height / 2) - 30;

        int ymin = Integer.MAX_VALUE;
        int ymax = Integer.MIN_VALUE;

        for (int i = 0; i < pointsCount; i++) {
            double theta = angle * i + Math.PI;
            int pointX = (int) (radius * sin(theta) + width / 2);
            int pointY = (int) (radius * cos(theta) + height / 2);
            points[i] = new Point(pointX, pointY);

            if (pointY < ymin) {
                ymin = pointY;
            }
            if (pointY > ymax) {
                ymax = pointY;
            }
        }
        // move the center to be in the middle of the image
        int increment = (height - ymax - ymin) / 2;
        return Arrays.stream(points).map(p -> new Point(p.x(), p.y() + increment)).toArray(Point[]::new);
    }

    private static void clearBufferedImage(BufferedImage bitmap) {
        Graphics graphics = bitmap.getGraphics();
        graphics.setColor(Color.BLACK);
        graphics.clearRect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        graphics.dispose();
    }

}

