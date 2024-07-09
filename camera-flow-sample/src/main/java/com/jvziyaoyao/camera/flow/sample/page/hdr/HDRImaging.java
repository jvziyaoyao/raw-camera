package com.jvziyaoyao.camera.flow.sample.page.hdr;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.photo.CalibrateDebevec;
import org.opencv.photo.MergeDebevec;
import org.opencv.photo.MergeMertens;
import org.opencv.photo.Photo;
import org.opencv.photo.Tonemap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)
public class HDRImaging {

    public void loadExposureSeq(String path, List<Mat> images, List<Float> times) {
        path += "/";

        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(path + "list.txt"));

            for (String line : lines) {
                String[] splitStr = line.split("\\s+");
                if (splitStr.length == 2) {
                    String name = splitStr[0];
                    Mat img = Imgcodecs.imread(path + name);
                    images.add(img);
                    float val = Float.parseFloat(splitStr[1]);
                    times.add(1 / val);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run(String[] args) {
        String path = args.length > 0 ? args[0] : "";
        if (path.isEmpty()) {
            System.out.println("Path is empty. Use the directory that contains images and exposure times.");
            System.exit(0);
        }

        List<Mat> images = new ArrayList<>();
        List<Float> times = new ArrayList<>();
        loadExposureSeq(path, images, times);

        Mat response = new Mat();
        CalibrateDebevec calibrate = Photo.createCalibrateDebevec();
        Mat matTimes = new Mat(times.size(), 1, CvType.CV_32F);
        float[] arrayTimes = new float[(int) (matTimes.total() * matTimes.channels())];
        for (int i = 0; i < times.size(); i++) {
            arrayTimes[i] = times.get(i);
        }
        matTimes.put(0, 0, arrayTimes);
        calibrate.process(images, response, matTimes);

        Mat hdr = new Mat();
        MergeDebevec mergeDebevec = Photo.createMergeDebevec();
        mergeDebevec.process(images, hdr, matTimes);

        Mat ldr = new Mat();
        Tonemap tonemap = Photo.createTonemap(2.2f);
        tonemap.process(hdr, ldr);

        Mat fusion = new Mat();
        MergeMertens mergeMertens = Photo.createMergeMertens();
        mergeMertens.process(images, fusion);

        Core.multiply(fusion, new Scalar(255, 255, 255), fusion);
        Core.multiply(ldr, new Scalar(255, 255, 255), ldr);
        Imgcodecs.imwrite("fusion.png", fusion);
        Imgcodecs.imwrite("ldr.png", ldr);
        Imgcodecs.imwrite("hdr.hdr", hdr);

        System.exit(0);
    }
}

//public class HDRImagingDemo {
//    public static void main(String[] args) {
//        // Load the native OpenCV library
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//
//        new HDRImaging().run(args);
//    }
//}
