package org.transport.service;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import lombok.AllArgsConstructor;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;
import org.transport.dto.DisplayDTO;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
@Service
public final class DisplayService {

	private final PersistenceService persistenceService;

	private static final int MAX_SMOOTH_AMOUNT = 5;

	/**
	 * Using the raw image bytes of an image of an LED dot matrix, convert it to a black and white byte array representing which LEDs are on.
	 *
	 * @param imageBytes the raw image bytes
	 * @return a {@link RawDisplayDTO} object with width, height, and image byte data (1 bit per pixel)
	 */
	public RawDisplayDTO process(byte[] imageBytes) {
		final Mat image = getImage(imageBytes);
		try {
			return getResult(image, estimatePitch(image, false), estimatePitch(image, true));
		} finally {
			image.release();
		}
	}

	/**
	 * Gets a PNG image as raw bytes, filtering by category, search terms, or dimensions.
	 *
	 * @param category        the category to filter by, or empty string for no filtering
	 * @param exactSearchTerm the search term to match exactly
	 * @param fuzzySearchTerm the search term to partially match
	 * @param width           the width to match, or 0 for no matching
	 * @param height          the height to match, or 0 for no matching
	 * @param scale           optional upscaling to the result image
	 * @param index           the zero-based index of the image, if there is more than one
	 * @return the image as a byte array
	 */
	public byte[] getDisplayPNG(String category, String exactSearchTerm, List<String> fuzzySearchTerm, int width, int height, int scale, int index) {
		final List<DisplayDTO> displays = persistenceService.getDisplays(category, exactSearchTerm, fuzzySearchTerm, width, height);
		final Mat image;

		if (displays.isEmpty()) {
			image = new Mat(1, 1, CvType.CV_8U, Scalar.all(0));
		} else {
			image = decodeImage(displays.get(Math.clamp(index, 0, displays.size() - 1)), scale);
		}

		final MatOfByte matOfByte = new MatOfByte();

		try {
			Imgcodecs.imencode(".png", image, matOfByte);
			return matOfByte.toArray();
		} finally {
			image.release();
			matOfByte.release();
		}
	}

	/**
	 * @param imageBytes the raw image bytes
	 * @return a grayscale {@link Mat} image
	 */
	private static Mat getImage(byte[] imageBytes) {
		final MatOfByte matOfByte = new MatOfByte(imageBytes);
		Mat imageBGR = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR);
		matOfByte.release();

		final Mat imageHSV = new Mat();
		Imgproc.cvtColor(imageBGR, imageHSV, Imgproc.COLOR_BGR2HSV);
		imageBGR.release();

		final Mat grayscaleImage = new Mat();
		Core.extractChannel(imageHSV, grayscaleImage, 2);
		final Mat croppedImage = new Mat(grayscaleImage, getCropRange(grayscaleImage, false), getCropRange(grayscaleImage, true)).clone();
		imageHSV.release();
		grayscaleImage.release();
		return croppedImage;
	}

	/**
	 * @param image the input image
	 * @param axis  {@code false} for X-axis, {@code true} for Y-axis
	 * @return a range of pixels with the black border removed
	 */
	private static Range getCropRange(Mat image, boolean axis) {
		final int width = image.width();
		final int height = image.height();
		final Mat croppedImage = new Mat(
				image,
				axis ? new Range((int) Math.floor(height * 0.4), (int) Math.ceil(height * 0.6) + 1) : Range.all(),
				axis ? Range.all() : new Range((int) Math.floor(width * 0.4), (int) Math.ceil(width * 0.6) + 1)
		);
		final double[] projection = getProjection(croppedImage, !axis);
		croppedImage.release();

		int start = -1;
		int end = -1;

		for (int i = 0; i < projection.length; i++) {
			if (start < 0 && projection[i] > 0) {
				start = i;
			}

			if (end < 0 && projection[projection.length - i - 1] > 0) {
				end = projection.length - i;
			}
		}

		return end > start ? new Range(start, end) : Range.all();
	}

	/**
	 * @param input the input image
	 * @param axis  {@code false} for X-axis, {@code true} for Y-axis
	 * @return an array of summed brightness values across an axis of the image
	 */
	private static double[] getProjection(Mat input, boolean axis) {
		final Mat sum = new Mat();
		Core.reduce(input, sum, axis ? 1 : 0, Core.REDUCE_SUM, CvType.CV_64F);
		final double[] output = new double[sum.rows() * sum.cols()];
		sum.get(0, 0, output);
		sum.release();
		return output;
	}

	/**
	 * @param image the input image
	 * @param axis  {@code false} for X-axis, {@code true} for Y-axis
	 * @return the estimated pixels between LEDs on the image
	 */
	private static int estimatePitch(Mat image, boolean axis) {
		final double[] projection = getProjection(image, axis);
		final IntArrayList pitches = new IntArrayList();

		for (int smoothAmount = 0; smoothAmount < MAX_SMOOTH_AMOUNT; smoothAmount++) {
			double previousValue = getSmoothedValue(projection, 0, smoothAmount);
			boolean previousIncreasing = false;
			boolean previousDecreasing = false;
			int previousMaxIndex = -1;
			int previousMinIndex = -1;

			for (int i = 1; i < projection.length; i++) {
				final double currentValue = getSmoothedValue(projection, i, smoothAmount);
				final boolean currentIncreasing = currentValue > previousValue;
				final boolean currentDecreasing = currentValue < previousValue;

				if (previousIncreasing && !currentIncreasing) {
					if (previousMaxIndex >= 0) {
						pitches.add(i - previousMaxIndex);
					}
					previousMaxIndex = i;
				}

				if (previousDecreasing && !currentDecreasing) {
					if (previousMinIndex >= 0) {
						pitches.add(i - previousMinIndex);
					}
					previousMinIndex = i;
				}

				previousValue = currentValue;
				previousIncreasing = currentIncreasing;
				previousDecreasing = currentDecreasing;
			}
		}

		return getMode(pitches);
	}

	/**
	 * @param image  the input image
	 * @param pitchX the estimated distance in pixels between LEDs on the X-axis
	 * @param pitchY the estimated distance in pixels between LEDs on the Y-axis
	 * @return a {@link RawDisplayDTO} object with width, height, and image byte data (1 bit per pixel)
	 */
	private static RawDisplayDTO getResult(Mat image, int pitchX, int pitchY) {
		final Mat binary = new Mat();
		Imgproc.threshold(image, binary, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

		final Mat integral = new Mat();
		Imgproc.integral(binary, integral, CvType.CV_32S);

		final int rawWidth = image.width();
		final int rawHeight = image.height();
		final int newPitchX = Math.max(1, pitchX);
		final int newPitchY = Math.max(1, pitchY);
		final int width = Math.ceilDiv(rawWidth, newPitchX);
		final int height = Math.ceilDiv(rawHeight, newPitchY);

		final int[] pixels = new int[width * height];
		final IntOpenHashSet values = new IntOpenHashSet();

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				final int x1 = x * newPitchX;
				final int x2 = Math.min((x + 1) * newPitchX, rawWidth);
				final int y1 = y * newPitchY;
				final int y2 = Math.min((y + 1) * newPitchY, rawHeight);
				final int value = getRectangleSum(integral, x1 + 1, y1 + 1, x2 + 1, y2 + 1);
				values.add(value);
				pixels[x + y * width] = value;
			}
		}

		binary.release();
		integral.release();
		final int threshold = getMedian(new IntArrayList(values));
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		for (int i = 0; i < pixels.length; i += 8) {
			int data = 0;
			for (int j = 0; j < 8; j++) {
				if (i + j < pixels.length) {
					data |= (pixels[i + j] >= threshold ? 0x80 : 0) >> j;
				}
			}
			byteArrayOutputStream.write(data);
		}

		return new RawDisplayDTO(width, height, byteArrayOutputStream.toByteArray());
	}

	/**
	 * @param values       an input array of values
	 * @param index        the index to retrieve
	 * @param smoothAmount how many adjacent values to smooth from
	 * @return a smoothed value created by the mean of adjacent values
	 */
	private static double getSmoothedValue(double[] values, int index, int smoothAmount) {
		double sum = 0;

		for (int i = -smoothAmount; i <= smoothAmount; i++) {
			sum += values[Math.clamp(index + i, 0, values.length - 1)];
		}

		return sum / (smoothAmount * 2 + 1);
	}

	private static Mat decodeImage(DisplayDTO display, int scale) {
		final int newScale = Math.max(1, scale);
		final Mat image = new Mat(display.height() * newScale, display.width() * newScale, CvType.CV_8U);
		int scaledX = 0;
		int scaledY = 0;

		for (int i = 0; i < display.imageBytes().length; i++) {
			final byte data = display.imageBytes()[i];
			for (int bit = 0; bit < 8; bit++) {
				for (int rawX = 0; rawX < newScale; rawX++) {
					for (int rawY = 0; rawY < newScale; rawY++) {
						image.put(scaledY + rawY, scaledX + rawX, (data & (0x80 >> bit)) > 0 ? 0xFF : 0);
					}
				}

				scaledX += newScale;

				if (scaledX == display.width() * newScale) {
					scaledX = 0;
					scaledY += newScale;
				}
			}
		}

		return image;
	}

	private static int getRectangleSum(Mat integral, int x1, int y1, int x2, int y2) {
		final int a = (int) integral.get(y1, x1)[0];
		final int b = (int) integral.get(y1, x2)[0];
		final int c = (int) integral.get(y2, x1)[0];
		final int d = (int) integral.get(y2, x2)[0];
		return d - b - c + a;
	}

	private static int getMedian(IntArrayList values) {
		Collections.sort(values);
		return values.isEmpty() ? 0 : values.getInt(values.size() / 2);
	}

	private static int getMode(IntArrayList values) {
		if (values.isEmpty()) {
			return 0;
		}

		final Int2LongOpenHashMap frequencyMap = new Int2LongOpenHashMap();
		for (int i = 0; i < values.size(); i++) {
			frequencyMap.addTo(values.getInt(i), 1);
		}

		final long maxFrequency = Collections.max(frequencyMap.values());
		return frequencyMap.int2LongEntrySet()
				.stream()
				.filter(entry -> entry.getLongValue() == maxFrequency)
				.mapToInt(Int2LongMap.Entry::getIntKey)
				.findFirst()
				.orElse(0);
	}

	public record RawDisplayDTO(int width, int height, byte[] imageBytes) {
	}
}
