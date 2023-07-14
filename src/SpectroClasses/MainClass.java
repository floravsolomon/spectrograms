package SpectroClasses;

import java.awt.Window;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MainClass {

	public static void main(String[] args) {

		// Set UI to look like Windows
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		// Set directory for the saved output
		File outFolder = new File(System.getProperty("user.home") + "/Documents/Audio/Spectrograms/");

		// Make any requisite directories to allow outFolder to exist
		outFolder.mkdirs();

		// Choose the input audio file
		File audioFile = pickFile("Choose audio file:");

		// Start processing
		new Spectrogram(audioFile);
	}

	public static File pickFile(String title) {

		File f = null;

		// Create a dialog box to choose exports directory
		JFileChooser chooser = new JFileChooser();

		// Origin directory is user's desktop
		chooser.setCurrentDirectory(new File(System.getProperty("user.home") + "/Downloads/"));

		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		// Show the file chooser until the user picks a file
		do {

			// Create a wrapper dialog to give the file chooser a taskbar icon
			JDialog wrapper = new JDialog((Window) null);
			wrapper.setLocationRelativeTo(null);

			// Set title of dialog box
			chooser.setDialogTitle(title);

			// Show files and directories
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

			// Filter out anything but WAV files
			chooser.setFileFilter(new FileNameExtensionFilter("WAV File", "wav"));

			int returnVal = chooser.showOpenDialog(wrapper);

			// If the user selects a file, set f to that file
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				f = chooser.getSelectedFile();
			}
			// Otherwise the user quit the dialog, kill the program
			else {
				System.out.println("Quit");
				System.exit(0);
			}

			// Close the wrapper so it doesn't linger around
			wrapper.dispose();
		} while (!f.exists());

		return f;
	}
}
