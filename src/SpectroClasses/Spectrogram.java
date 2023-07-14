package SpectroClasses;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

// Class extends JPanel for ease of displaying progress
public class Spectrogram extends JPanel {

	// File to be read, MUST BE WAV
	static File audioFile = null;

	// Stream object to read file
	static AudioInputStream sIn = null;

	// Number of bytes to process per cycle (must be a power of 2)
	static int numBytes = 0;

	// Frame rate of audio input stream, used to place an upper bound on numBytes
	static double frameRate = 0;

	// Number of bytes in each sample frame of original audio
	static int bytesPerFrame = 0;

	// Number of frequencies in each box
	static double bandwidth = 0.0;

	// Number of bandwidth-sized boxes contained in each cycle sample
	static int numBands = 0;

	// Used to determine percentage of the width of the output image to move
	static double chunksInFile = 0;

	// BufferedImage object to store the output for display and saving
	static BufferedImage bufIm = null;

	// Size of file in bytes, used to calculate chunksInFile as well as the upper
	// bound of the magnitude values of frequency map
	static int fileSize;

	static Graphics2D graphics;

	static double normFactor = Short.MAX_VALUE;

	static CountDownLatch cdl;

	public Spectrogram(File audio) {

		// Set audioFile to the chosen file from MainClass
		audioFile = audio;

		// Set fileSize
		fileSize = (int) audioFile.length();

		// Create a JFrame as wide as the screen and half as tall to hold the JPanel
		JFrame imgFrame = new JFrame("Spectrogram");
		imgFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		imgFrame.setExtendedState(imgFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		imgFrame.add(this);
		imgFrame.setVisible(true);

		// Initialize the bufIm object at twice as wide as the JPanel's width and the
		// same height (better horizontal resolution)
		bufIm = new BufferedImage((int) imgFrame.getWidth(), (int) imgFrame.getHeight(), BufferedImage.TYPE_INT_ARGB);

		graphics = bufIm.createGraphics();

		graphics.setColor(Color.BLACK);
		graphics.fillRect(0, 0, bufIm.getWidth(), bufIm.getHeight());

		try {

			// Init audio stream from file
			sIn = AudioSystem.getAudioInputStream(audioFile);

			// Get format
			AudioFormat baseFormat = sIn.getFormat();

			// Decode the file format
			AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), // sample
																														// rate
					16, // sample size (bits)
					baseFormat.getChannels(), // channels
					baseFormat.getChannels() * 2, // frame size
					baseFormat.getSampleRate(), // frame rate
					false); // little-endian

			// Get audio stream from decoded format and file stream
			AudioInputStream dIn = AudioSystem.getAudioInputStream(decodedFormat, sIn);

			// Get the bytesPerFrame
			bytesPerFrame = dIn.getFormat().getFrameSize();

			// If it's unspecified set it to 2
			if (bytesPerFrame == AudioSystem.NOT_SPECIFIED) {
				bytesPerFrame = 2;
			}

			System.out.println(bytesPerFrame);

			// Get frame rate
			frameRate = dIn.getFormat().getFrameRate();

			double bound = frameRate * bytesPerFrame / 8;

			int n = 0;

			while (Math.pow(2, n + 1) <= bound) {
				n++;
			}

			numBytes = (int) Math.pow(2, n);

			chunksInFile = fileSize / numBytes;

			bandwidth = bytesPerFrame * frameRate / numBytes;

			numBands = numBytes / bytesPerFrame;

			try {

				int numBytesRead = 0;

				int nThreads = Runtime.getRuntime().availableProcessors();

				cdl = new CountDownLatch(nThreads);

				Thread waitForWorkers = new Thread() {
					public void run() {
						try {
							cdl.await();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {

								File output = new File(System.getProperty("user.home")
										+ "/Documents/Audio/Spectrograms/"
										+ audioFile.getName().substring(0, audioFile.getName().indexOf(".")) + ".png");

								int suffix = 1;

								while (output.exists()) {
									output = new File(System.getProperty("user.home") + "/Documents/Audio/Spectrograms/"
											+ audioFile.getName().substring(0, audioFile.getName().indexOf(".")) + " "
											+ suffix + ".png");
									suffix++;
								}

								try {
									ImageIO.write(bufIm, "png", output);
								} catch (IOException e) {
									e.printStackTrace();
								}

								System.exit(0);
							}

						});
					}
				};

				waitForWorkers.start();

				ExecutorService es = Executors.newFixedThreadPool(nThreads);

				Vector<Batch> batches = new Vector<Batch>(nThreads);

				byte[] temp = new byte[numBytes];

				ArrayList<byte[]> readList = new ArrayList<byte[]>();

				int index = 0;

				int batchSize = (int) chunksInFile / nThreads;

				while ((numBytesRead = dIn.read(temp)) > -1) {

					if (numBytesRead < numBytes) {
						for (int nb = numBytesRead - 1; nb < numBytes; nb++) {
							temp[nb] = 0;
						}
					}

					byte[] read = temp.clone();

					readList.add(read);

					if (readList.size() == batchSize) {
						ArrayList<byte[]> tempList = new ArrayList<byte[]>(readList);

						Batch batch = new Batch(index, tempList, graphics, this);

						batches.add(batch);

						readList.clear();

						index++;
					}
				}

				for (Batch b : batches) {
					es.submit(b);
				}

				es.shutdown();

			} catch (Exception e1) {
				e1.printStackTrace();
				System.exit(1);
			}
		} catch (

		Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static class Batch implements Runnable {

		ArrayList<byte[]> readList;

		Spectrogram specOriginal;

		ArrayList<Double> freqList = new ArrayList<Double>();

		Graphics2D bufGraphics;

		int index;

		int count = 0;

		public Batch(int ind, ArrayList<byte[]> array, Graphics2D bufG, Spectrogram spec) {

			index = ind;

			readList = array;

			specOriginal = spec;

			bufGraphics = bufG;

		}

		@Override
		public void run() {

			while (count < readList.size()) {

				byte[] readBytes = readList.get(count);

				Double[] frames = new Double[numBytes / bytesPerFrame];

				for (int i = 0; i < readBytes.length; i += bytesPerFrame) {
					double sample = 0;

					for (int j = 0; j < bytesPerFrame; j++) {
						sample += (readBytes[i + j]);
					}

					sample = sample / (255 * bytesPerFrame);

					frames[i / bytesPerFrame] = sample;
				}

				Complex[] cVals = new Complex[frames.length];

				for (int i = 0; i < frames.length; i++) {
					cVals[i] = new Complex(frames[i], 0.0);
				}

				Complex[] transform = Fourier.fft(cVals);

				Double[] mags = new Double[transform.length];

				for (int i = 0; i < transform.length; i++) {
					mags[i] = transform[i].mag();
				}

				int lim = (int) (1760 / bandwidth);

				for (int i = 1; i <= lim; i++) {
					freqList.add(mags[i]);
				}

				paintImage(bufGraphics);

				specOriginal.getGraphics().drawImage(bufIm, 0, 0, specOriginal.getWidth(), specOriginal.getHeight(),
						specOriginal);

				freqList.clear();

				count++;
			}

			cdl.countDown();
		}

		void paintImage(Graphics2D graphics) {

			if (!freqList.isEmpty()) {

				ArrayList<Double> colorVals = new ArrayList<Double>();

				for (int i = 0; i < freqList.size(); i++) {

					double scaledMag = freqList.get(i) / 64;

					colorVals.add(scaledMag);
				}

				ArrayList<Rectangle2D> outShapes = new ArrayList<Rectangle2D>();

				for (int i = 0; i < colorVals.size(); i++) {

					Rectangle2D rect = new Rectangle2D.Double(
							(count + index * readList.size()) * (double) bufIm.getWidth() / chunksInFile,
							(double) bufIm.getHeight() - (i + 1) * (double) bufIm.getHeight() / colorVals.size(),
							(double) bufIm.getWidth() / chunksInFile, (double) bufIm.getHeight() / colorVals.size());

					outShapes.add(rect);
				}
				
				ArrayList<Double> sortCol = new ArrayList<Double>(colorVals);

				Collections.sort(sortCol);

				double median = sortCol.get(sortCol.size() / 2);

				for (int i = 0; i < colorVals.size(); i++) {

					Color col = Color.black;

					if (colorVals.get(i) >= median) {
						col = Color.getHSBColor(1 - colorVals.get(i).floatValue(), 1, colorVals.get(i).floatValue());
					}
					
					graphics.setColor(col);

					graphics.fill(outShapes.get(i));
				}
			}
		}
	}
}
