package org.transport.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@AllArgsConstructor
@Service
public final class DisplayService {

	private static final int MAX_SMOOTH_AMOUNT = 5;

	@EventListener(ApplicationReadyEvent.class)
	public void runOnStartup() {
		OpenCV.loadLocally();
	}

	public byte[] process(byte[] imageBytes) {
		final Mat image = getImage(imageBytes);
		final byte[] result = getResult(image, estimatePitch(image, false), estimatePitch(image, true));
		image.release();
		return result;
	}

	private static Mat getImage(byte[] imageBytes) {
		final MatOfByte matOfByte = new MatOfByte(imageBytes);
		Mat imageBGR = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR);
		matOfByte.release();

		final Mat imageHSV = new Mat();
		Imgproc.cvtColor(imageBGR, imageHSV, Imgproc.COLOR_BGR2HSV);
		imageBGR.release();

		final List<Mat> imageChannels = new ArrayList<>(3);
		Core.split(imageHSV, imageChannels);
		final Mat grayscaleImage = imageChannels.get(2);
		final Mat croppedImage = new Mat(grayscaleImage, getCropRange(grayscaleImage, false), getCropRange(grayscaleImage, true)).clone();
		imageHSV.release();
		imageChannels.forEach(Mat::release);
		return croppedImage;
	}

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

	private static double[] getProjection(Mat input, boolean axis) {
		final Mat sum = new Mat();
		Core.reduce(input, sum, axis ? 1 : 0, Core.REDUCE_SUM, CvType.CV_64F);
		final double[] output = new double[sum.rows() * sum.cols()];
		sum.get(0, 0, output);
		sum.release();
		return output;
	}

	private static int estimatePitch(Mat image, boolean axis) {
		final double[] projection = getProjection(image, axis);
		final List<Integer> pitches = new ArrayList<>();

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

	private static byte[] getResult(Mat image, int pitchX, int pitchY) {
		final Mat binary = new Mat();
		Imgproc.threshold(image, binary, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

		final int rawWidth = image.width();
		final int rawHeight = image.height();
		final int newPitchX = Math.max(1, pitchX);
		final int newPitchY = Math.max(1, pitchY);
		final int width = Math.ceilDiv(rawWidth, newPitchX);
		final int height = Math.ceilDiv(rawHeight, newPitchY);

		final int[] pixels = new int[width * height];
		final Set<Integer> values = new HashSet<>();

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				final Mat area = binary.submat(
						Math.min(y * newPitchY, rawHeight), Math.min((y + 1) * newPitchY, rawHeight),
						Math.min(x * newPitchX, rawWidth), Math.min((x + 1) * newPitchX, rawWidth)
				);
				final int value = Core.countNonZero(area);
				area.release();
				values.add(value);
				pixels[x + y * width] = value;
			}
		}

		binary.release();
		final int threshold = getMedian(new ArrayList<>(values));
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byteArrayOutputStream.write((byte) (width >> 8));
		byteArrayOutputStream.write((byte) (width & 0xFF));
		byte count = 0;
		boolean isWhite = false;

		for (final int pixel : pixels) {
			if ((pixel >= threshold) != isWhite || (count & 0xFF) == 0xFF) {
				byteArrayOutputStream.write(count);
				count = 0;
				isWhite = !isWhite;
			}
			count++;
		}

		byteArrayOutputStream.write(count);
		return byteArrayOutputStream.toByteArray();
	}

	private static double getSmoothedValue(double[] values, int index, int smoothAmount) {
		double sum = 0;

		for (int i = -smoothAmount; i <= smoothAmount; i++) {
			sum += values[Math.clamp(index + i, 0, values.length - 1)];
		}

		return sum / (smoothAmount * 2 + 1);
	}

	private static int getMedian(List<Integer> values) {
		Collections.sort(values);
		return values.isEmpty() ? 0 : values.get(values.size() / 2);
	}

	private static int getMode(List<Integer> values) {
		if (values.isEmpty()) {
			return 0;
		}

		final Map<Integer, Long> frequencyMap = values.stream().collect(Collectors.groupingBy(value -> value, Collectors.counting()));
		final long maxFrequency = Collections.max(frequencyMap.values());
		return frequencyMap.entrySet()
				.stream()
				.filter(entry -> entry.getValue() == maxFrequency)
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(0);
	}
}
