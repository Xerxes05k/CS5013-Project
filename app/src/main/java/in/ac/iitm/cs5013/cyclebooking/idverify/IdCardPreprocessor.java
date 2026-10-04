package in.ac.iitm.cs5013.cyclebooking.idverify;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

/**
 * OpenCV clean-up before OCR, one step per problem a phone photo of a card has:
 * small text (upscale), tilt (find the card's outline and rotate it level), sensor
 * noise (median blur), glare and uneven light (CLAHE, then a local threshold to black
 * text on white).
 */
final class IdCardPreprocessor {

    static {
        nu.pattern.OpenCV.loadLocally();
    }

    private IdCardPreprocessor() {
    }

    /** Returns a PNG of the cleaned-up card, ready for Tesseract. */
    static byte[] clean(byte[] photo) {
        Mat img = Imgcodecs.imdecode(new MatOfByte(photo), Imgcodecs.IMREAD_GRAYSCALE);
        if (img.empty()) {
            throw new IllegalArgumentException("Not an image");
        }

        // Tesseract reads best with capital letters ~30 px tall; phone crops are smaller.
        if (img.cols() < 1600) {
            double scale = 1600.0 / img.cols();
            Imgproc.resize(img, img, new Size(), scale, scale, Imgproc.INTER_CUBIC);
        }

        img = straighten(img);

        // Denoise before boosting contrast; CLAHE would otherwise amplify the speckle
        // of a dim photo along with the text.
        Imgproc.medianBlur(img, img, 5);

        // Local contrast equalisation: lifts text out of a glare patch or shadow without
        // blowing out the rest of the card the way a global brightness change would.
        CLAHE clahe = Imgproc.createCLAHE(2.0, new Size(8, 8));
        clahe.apply(img, img);

        // Threshold each pixel against its own neighbourhood rather than one value for
        // the whole card, so text under glare or in shadow still comes out black.
        Imgproc.adaptiveThreshold(img, img, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 41, 15);
        // A second median pass removes the isolated dots the threshold leaves behind.
        Imgproc.medianBlur(img, img, 3);

        MatOfByte png = new MatOfByte();
        Imgcodecs.imencode(".png", img, png);
        return png.toArray();
    }

    /**
     * Finds the card as the largest bright region in the photo and rotates the photo
     * so the card's edges are level. The card is lighter than whatever it lies on, so
     * an Otsu threshold separates the two. Only small angles are corrected; past 20
     * degrees the resident is better off retaking the photo.
     */
    private static Mat straighten(Mat gray) {
        Mat bright = new Mat();
        Imgproc.threshold(gray, bright, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(bright, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfPoint card = contours.stream().max(Comparator.comparingDouble(Imgproc::contourArea)).orElse(null);
        if (card == null) {
            return gray;
        }

        RotatedRect box = Imgproc.minAreaRect(new MatOfPoint2f(card.toArray()));
        double angle = box.angle;
        // OpenCV 4.5+ reports angles in [0, 90); fold into [-45, 45).
        if (angle >= 45) {
            angle -= 90;
        }
        if (Math.abs(angle) < 0.5 || Math.abs(angle) > 20) {
            return gray;
        }
        Mat rotation = Imgproc.getRotationMatrix2D(box.center, angle, 1.0);
        Mat out = new Mat();
        Imgproc.warpAffine(gray, out, rotation, gray.size(), Imgproc.INTER_CUBIC, Core.BORDER_REPLICATE);
        return out;
    }
}
