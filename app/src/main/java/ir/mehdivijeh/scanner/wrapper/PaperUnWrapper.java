package ir.mehdivijeh.scanner.wrapper;
//author : mehdi vijeh

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.opencv.core.CvType.CV_32FC2;

public class PaperUnWrapper {

    private static final String TAG = "PaperUnWrapper";
    private static final int COL_COUNT = 30;
    private static final int ROW_COUNT = 20;
    private Mat srcImage;
    private Mat desImage;
    private List<double[]> pointPercent;
    private List<double[]> points;
    private double[] pointA; // top left
    private double[] pointB; // top center
    private double[] pointC; // top right
    private double[] pointD; // bottom right
    private double[] pointE; // bottom center
    private double[] pointF; // bottom left
    private double[] centerTop;
    private double[] centerBottom;
    private Line centerLine;

    public PaperUnWrapper(Mat srcImage, List<double[]> pointPercent) {
        this.srcImage = srcImage;
        this.pointPercent = pointPercent;
        desImage = srcImage.clone();

        points = loadPoints();
        mapPointsToMember(points);
        calculateCenters();
        centerLine = new Line(centerBottom, centerTop);
    }


    public Mat unwrap(boolean interpolate) {
        List<List<double[]>> sourceMap = calculateSourceMap();

        if (interpolate) {
            //TODO : NOT COMPLETE
            //unwrapPaperInterpolation();
        } else {
            unwrapPaperPerspective(sourceMap);
        }
        return this.desImage;
    }

    private List<double[]> loadPoints() {
        List<double[]> points = new ArrayList<>();
        for (double[] point : pointPercent) {
            double x = point[0] * srcImage.width();
            double y = point[1] * srcImage.height();
            double[] pointN = new double[]{x, y};
            points.add(pointN);
        }
        return points;
    }

    private void mapPointsToMember(List<double[]> points) {
        pointA = points.get(0);
        pointB = points.get(1);
        pointC = points.get(2);
        pointD = points.get(3);
        pointE = points.get(4);
        pointF = points.get(5);
    }

    private void calculateCenters() {
        double centerTopX = (pointA[0] + pointC[0]) / 2;
        double centerTopY = (pointA[1] + pointC[1]) / 2;
        centerTop = new double[]{centerTopX, centerTopY};

        double centerBottomX = (pointD[0] + pointF[0]) / 2;
        double centerBottomY = (pointD[1] + pointF[1]) / 2;
        centerBottom = new double[]{centerBottomX, centerBottomY};
    }

    private List<List<double[]>> calculateSourceMap() {
        List<double[]> topPoints = calculateEllipsePoint(pointA, pointB, pointC, COL_COUNT);
        List<double[]> bottomPoints = calculateEllipsePoint(pointF, pointE, pointD, COL_COUNT);

        List<List<double[]>> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < ROW_COUNT; rowIndex++) {
            List<double[]> row = new ArrayList<>();
            for (int colIndex = 0; colIndex < COL_COUNT; colIndex++) {
                double[] top_point = topPoints.get(colIndex);
                double[] bottom_point = bottomPoints.get(colIndex);

                double deltaX = (top_point[0] - bottom_point[0]) / ((float) (ROW_COUNT - 1));
                double deltaY = (top_point[1] - bottom_point[1]) / ((float) (ROW_COUNT - 1));
                double[] delta = new double[]{deltaX, deltaY};

                double pointX = top_point[0] - delta[0] * rowIndex;
                double pointY = top_point[1] - delta[1] * rowIndex;
                double[] point = new double[]{pointX, pointY};

                row.add(point);
            }
            rows.add(row);
        }
        return rows;
    }


    private List<double[]> calculateEllipsePoint(double[] left, double[] top, double[] right, int pointsCount) {
        double centerX = ((double) left[0] + right[0]) / 2;
        double centerY = ((double) left[1] + right[1]) / 2;
        double[] center = new double[]{centerX, centerY};

        double[] aVector = new double[]{left[0] - right[0], left[1] - right[1]};
        double aNorm = norm(aVector) / 2;

        double[] bVector = new double[]{center[0] - top[0], center[1] - top[1]};
        double bNorm = norm(bVector);

        double delta;
        if (top[1] - center[1] > 0) {
            delta = Math.PI / (pointsCount - 1);
        } else {
            delta = -Math.PI / (pointsCount - 1);
        }

        double cosRot = (right[0] - center[0]) / aNorm;
        double sinRot = (right[1] - center[1]) / aNorm;

        List<double[]> points = new ArrayList<>();
        for (int i = 0; i < pointsCount; i++) {
            double phi = delta * i;
            double[] dx_dy = getEllipsePoint(aNorm, bNorm, phi);
            double dx = dx_dy[0];
            double dy = dx_dy[1];

            double x = Math.round(center[0] + dx * cosRot - dy * sinRot);
            double y = Math.round(center[1] + dx * sinRot + dy * cosRot);

            points.add(new double[]{x, y});
        }

        Collections.reverse(points);
        return points;
    }

    private double norm(double[] vector) {
        return Math.sqrt(vector[0] * vector[0] + vector[1] + vector[1]);
    }

    /*
     * Get ellipse radius in polar coordinates
     * */
    private double[] getEllipsePoint(double a, double b, double phi) {
        return new double[]{a * Math.cos(phi), b * Math.sin(phi)};
    }

    /*
     * Unwrap label using transform
     * */
    private void unwrapPaperPerspective(List<List<double[]>> sourceMap) {
        int width = srcImage.width();
        int height = srcImage.height();

        float dx = ((float) width) / (COL_COUNT - 1);
        float dy = ((float) height) / (ROW_COUNT - 1);

        int dx_int = (int) Math.ceil(dx);
        int dy_int = (int) Math.ceil(dy);

        for (int rowIndex = 0; rowIndex < ROW_COUNT - 1; rowIndex++) {
            for (int colIndex = 0; colIndex < COL_COUNT - 1; colIndex++) {

                Mat src_mat = new Mat(4, 1, CV_32FC2);
                Mat dst_mat = new Mat(4, 1, CV_32FC2);

                src_mat.put(0, 0
                        , sourceMap.get(rowIndex).get(colIndex)[0]
                        , sourceMap.get(rowIndex).get(colIndex)[1]
                        , sourceMap.get(rowIndex).get(colIndex + 1)[0]
                        , sourceMap.get(rowIndex).get(colIndex + 1)[1]
                        , sourceMap.get(rowIndex + 1).get(colIndex)[0]
                        , sourceMap.get(rowIndex + 1).get(colIndex)[1]
                        , sourceMap.get(rowIndex + 1).get(colIndex + 1)[0]
                        , sourceMap.get(rowIndex + 1).get(colIndex + 1)[1]);


                dst_mat.put(0, 0, 0, 0, dx, 0, 0, dy, dx, dy);


                Mat destination = new Mat();
                Mat m = Imgproc.getPerspectiveTransform(src_mat, dst_mat);
                Imgproc.warpPerspective(srcImage, destination, m, new Size(dx_int, dy_int));
                int x_offset = (int) (dx * colIndex);
                int y_offset = (int) (dy * rowIndex);


                //Log.d(TAG, "unwrapPaperPerspective: " + desImage.cols() + " " + desImage.rows());
                Log.d(TAG, "unwrapPaperPerspective: " + this.desImage.colRange(x_offset, x_offset + dx_int).cols() + " " + this.desImage.rowRange(y_offset, y_offset + dy_int).rows());
                //Log.d(TAG, "unwrapPaperPerspective: " + destination.cols() + " " + destination.rows());
                //Log.d(TAG, "unwrapPaperPerspective: " + x_offset + " " + (x_offset + dx_int));
                // Log.d(TAG, "unwrapPaperPerspective: " + y_offset + " " + (y_offset + dy_int));
                destination.copyTo(this.desImage.rowRange(y_offset, y_offset + dy_int).colRange(x_offset, x_offset + dx_int));
            }
        }
    }

    /*
     * Unwrap label using interpolation - more accurate method in terms of quality
     * */
    private void unwrapPaperInterpolation(List<List<double[]>> sourceMap) {
        int width = srcImage.width();
        int height = srcImage.height();

        List<List<int[]>> destinationMap = calculateDestinationMap();
    }

    private List<List<int[]>> calculateDestinationMap() {
        int width = srcImage.width();
        int height = srcImage.height();

        float dx = ((float) width) / (COL_COUNT - 1);
        float dy = ((float) height) / (ROW_COUNT - 1);

        List<List<int[]>> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < ROW_COUNT; rowIndex++) {
            List<int[]> row = new ArrayList<>();
            for (int colIndex = 0; colIndex < COL_COUNT; colIndex++) {
                row.add(new int[]{((int) dx * colIndex), ((int) dy * rowIndex)});
            }
            rows.add(row);
        }
        return rows;
    }

    public List<double[]> getPoints() {
        return points;
    }
}
