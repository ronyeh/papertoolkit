package papertoolkit.printing;

import java.awt.Desktop;
import java.awt.print.PageFormat;
import java.io.File;
import java.io.IOException;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import papertoolkit.units.Points;
import papertoolkit.util.ArrayUtils;


/**
 * <p>
 * Represents a printer, and enables you to control it.
 * </p>
 * <p>
 * Also, includes utilities that allows you to query and work with the system's Printers.
 * </p>
 * <p>
 * <span class="BSDLicense"> This software is distributed under the <a
 * href="http://hci.stanford.edu/research/copyright.txt">BSD License</a>. </span>
 * </p>
 * 
 * @author <a href="http://graphics.stanford.edu/~ronyeh">Ron B Yeh</a> (ronyeh(AT)cs.stanford.edu)
 */
public class Printer {

	/**
	 * @param pageFormat
	 */
	public static void displayPageFormat(PageFormat pageFormat) {
		System.out.println("Page Format {");
		System.out.println("\tPage Width: " + new Points(pageFormat.getWidth()).toInches() + " Height: "
				+ new Points(pageFormat.getHeight()).toInches());
		System.out.println("\tImageable X: " + new Points(pageFormat.getImageableX()).toInches() + " Y: "
				+ new Points(pageFormat.getImageableY()).toInches());
		System.out.println("\tImageable W: " + new Points(pageFormat.getImageableWidth()).toInches() + " H: "
				+ new Points(pageFormat.getImageableHeight()).toInches());
		System.out.print("\tMatrix: ");
		ArrayUtils.printArray(pageFormat.getMatrix());
		System.out.println("} End Page Format");
		System.out.flush();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Printer: " + service.getName();
	}

	/**
	 * @param f
	 *            the file to print, using the system's default settings.
	 */
	public static void print(File f) {
		if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.PRINT)) {
			System.err.println("Printers: Cannot print the file, because "
					+ "Java 1.6 Desktop printing is not supported.");
			return;
		}

		// use the system's default printing mechanism
		try {
			Desktop.getDesktop().print(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private PrintService service;

	private boolean showPageSetupDialog = false;

	private boolean showPrintPreferencesDialog = false;

	/**
	 * Creates a Printer that points to the default print service.
	 * 
	 */
	public Printer() {
		this(PrintServiceLookup.lookupDefaultPrintService());
	}

	/**
	 * A printer initialized with some Java print service object.
	 * 
	 * @param serv
	 */
	public Printer(PrintService serv) {
		service = serv;
	}

	/**
	 * @return the dots per inch (resolution) of this printer.
	 */
	public double getDPI() {
		// TODO Get real DPI
		System.err.println("Printer :: getDPI currently hardcoded to 600 DPI");
		return 600.0;
	}

}
