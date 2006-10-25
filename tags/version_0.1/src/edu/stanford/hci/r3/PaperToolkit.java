package edu.stanford.hci.r3;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;

import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.ConditionalHighlighter;

import com.thoughtworks.xstream.XStream;

import edu.stanford.hci.r3.designer.acrobat.AcrobatDesignerLauncher;
import edu.stanford.hci.r3.designer.acrobat.RegionConfiguration;
import edu.stanford.hci.r3.events.EventEngine;
import edu.stanford.hci.r3.paper.Region;
import edu.stanford.hci.r3.paper.Sheet;
import edu.stanford.hci.r3.pattern.coordinates.TiledPatternCoordinateConverter;
import edu.stanford.hci.r3.pattern.coordinates.PatternLocationToSheetLocationMapping.RegionID;
import edu.stanford.hci.r3.pen.Pen;
import edu.stanford.hci.r3.units.Centimeters;
import edu.stanford.hci.r3.units.Inches;
import edu.stanford.hci.r3.units.Pixels;
import edu.stanford.hci.r3.units.Points;
import edu.stanford.hci.r3.util.DebugUtils;
import edu.stanford.hci.r3.util.StringUtils;
import edu.stanford.hci.r3.util.WindowUtils;
import edu.stanford.hci.r3.util.layout.StackedLayout;

/**
 * <p>
 * Every PaperToolit has one EventEngine that handles input from users, and schedules output for the
 * system. A PaperToolkit can run one or more Applications at the same time. You can also deactivate
 * applications (to pause them). Or, you can remove them altogether. (These features are not yet
 * fully implemented.)
 * </p>
 * <p>
 * <span class="BSDLicense"> This software is distributed under the <a
 * href="http://hci.stanford.edu/research/copyright.txt">BSD License</a>. </span>
 * </p>
 * 
 * @author <a href="http://graphics.stanford.edu/~ronyeh">Ron B Yeh</a> (ronyeh(AT)cs.stanford.edu)
 */
public class PaperToolkit {

	/**
	 * Font for the App Manager GUI.
	 */
	private static final Font APP_MANAGER_FONT = new Font("Trebuchet MS", Font.PLAIN, 18);

	/**
	 * Serializes/Unserializes toolkit objects to/from XML strings.
	 */
	private static XStream xmlEngine;

	/**
	 * @param xmlFile
	 * @return
	 */
	public static Object fromXML(File xmlFile) {
		Object o = null;
		try {
			o = getXMLEngine().fromXML(new FileInputStream(xmlFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return o;
	}

	/**
	 * @param resourcePath
	 * @return
	 */
	public static File getResourceFile(String resourcePath) {
		try {
			File f = new File(PaperToolkit.class.getResource(resourcePath).toURI());
			return f;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @return the XStream processor that parses and creates XML.
	 */
	private static synchronized XStream getXMLEngine() {
		if (xmlEngine == null) {
			xmlEngine = new XStream();

			// Add Aliases Here (for more concise XML)
			xmlEngine.alias("Sheet", Sheet.class);
			xmlEngine.alias("Inches", Inches.class);
			xmlEngine.alias("Centimeters", Centimeters.class);
			xmlEngine.alias("Pixels", Pixels.class);
			xmlEngine.alias("Points", Points.class);
			xmlEngine.alias("RegionConfiguration", RegionConfiguration.class);
			xmlEngine.alias("Region", Region.class);
			xmlEngine.alias("Rectangle2DDouble", Rectangle2D.Double.class);
			xmlEngine.alias("TiledPatternCoordinateConverter",
					TiledPatternCoordinateConverter.class);
			xmlEngine.alias("RegionID", RegionID.class);
		}
		return xmlEngine;
	}

	/**
	 * Sets up parameters for any Java Swing UI we need.
	 */
	public static void initializeLookAndFeel() {
		WindowUtils.setNativeLookAndFeel();
	}

	/**
	 * @param obj
	 * @return a string representing the object translated into XML
	 */
	public static String toXML(Object obj) {
		return getXMLEngine().toXML(obj);
	}

	/**
	 * @param object
	 * @param outputFile
	 */
	public static void toXML(Object object, File outputFile) {
		try {
			FileOutputStream fos = new FileOutputStream(outputFile);
			toXML(object, fos);
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param object
	 * @param stream
	 *            write the xml to disk or another output stream.
	 */
	public static void toXML(Object object, OutputStream stream) {
		getXMLEngine().toXML(object, stream);
	}

	/**
	 * Stop, run, pause applications.
	 */
	private JFrame appManager;

	private JPanel controls;

	private JButton designSheetsButton;

	/**
	 * The engine that processes all pen events, producing the correct outputs and calling the right
	 * event handlers.
	 */
	private EventEngine eventEngine;

	/**
	 * Exits the app manager.
	 */
	private JButton exitAppManagerButton;

	/**
	 * Visual list of loaded (and possibly running) apps.
	 */
	private JXList listOfApps;

	/**
	 * A list of all applications loaded (but not necessarily running) in this system.
	 */
	private List<Application> loadedApplications = new ArrayList<Application>();

	/**
	 * Description for the app manager.
	 */
	private JLabel mainMessage;

	private JButton printSheetsButton;

	/**
	 * The Run Queue.
	 */
	private List<Application> runningApplications = new ArrayList<Application>();

	/**
	 * Starts the selected application.
	 */
	private JButton startAppButton;

	/**
	 * Stops the selected application.
	 */
	private JButton stopAppButton;

	/**
	 * Whether to show the application manager whenever an app is loaded/started. Defaults to false.
	 * True is useful for debugging and stopping apps that don't have a GUI.
	 */
	private boolean useAppManager = false;

	/**
	 * The version of the PaperToolkit.
	 */
	private String versionString = "0.1";

	/**
	 * Start up a paper toolkit. A toolkit can load multiple applications, and dispatch events
	 * accordingly (and between applications, ideally). There will be one event engine in the paper
	 * toolkit, and all events that applications generate will be fed through this single event
	 * engine.
	 */
	public PaperToolkit() {
		printInitializationMessages();
		initializeLookAndFeel();
		eventEngine = new EventEngine();
	}

	/**
	 * Allows an end user to stop, start, and otherwise manage loaded applications.
	 * 
	 * @return
	 */
	public JFrame getApplicationManager() {
		if (appManager == null) {
			appManager = new JFrame("R3 Applications");

			appManager.setLayout(new BorderLayout());
			appManager.add(getMainMessage(), BorderLayout.NORTH);
			appManager.add(getListOfApps(), BorderLayout.CENTER);
			appManager.add(getExitAppManagerButton(), BorderLayout.SOUTH);
			appManager.add(getControls(), BorderLayout.EAST);

			appManager.setSize(640, 480);
			appManager.setLocation(WindowUtils.getWindowOrigin(appManager,
					WindowUtils.DESKTOP_CENTER));
			appManager.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			appManager.setVisible(true);
		}
		return appManager;
	}

	/**
	 * @return
	 */
	private Component getControls() {
		if (controls == null) {
			controls = new JPanel();
			controls.setLayout(new StackedLayout(StackedLayout.VERTICAL));

			controls.add(getDesignSheetsButton(), "TopWide");
			controls.add(getPrintSheetsButton(), "TopWide");
			controls.add(getStartApplicationButton(), "TopWide");
			controls.add(getStopApplicationButton(), "TopWide");
		}
		return controls;
	}

	/**
	 * @return
	 */
	private Component getDesignSheetsButton() {
		if (designSheetsButton == null) {
			designSheetsButton = new JButton("Design Sheets");
			designSheetsButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					JFrame frame = AcrobatDesignerLauncher.start();
					frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
				}
			});
		}
		return designSheetsButton;
	}


	/**
	 * @return
	 */
	private Component getExitAppManagerButton() {
		// stop all apps and then exit the application manager
		if (exitAppManagerButton == null) {
			exitAppManagerButton = new JButton("Exit App Manager");
			exitAppManagerButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					System.out.println("Stopping all Applications...");
					Object[] objects = runningApplications.toArray();
					for (Object o : objects) {
						stopApplication((Application) o);
					}
					System.out.println("Exiting the Paper Toolkit Application Manager...");
					System.exit(0);
				}
			});
		}
		return exitAppManagerButton;
	}
	
	/**
	 * @return a GUI list of loaded applications (running or not). Grey out the ones that are not
	 *         running.
	 */
	private Component getListOfApps() {
		if (listOfApps == null) {
			ListModel model = new AbstractListModel() {
				public Object getElementAt(int appIndex) {
					return loadedApplications.get(appIndex);
				}

				public int getSize() {
					return loadedApplications.size();
				}
			};
			listOfApps = new JXList(model);
			listOfApps.addHighlighter(new ConditionalHighlighter(Color.WHITE, Color.LIGHT_GRAY, 0,
					-1) {
				@Override
				protected boolean test(ComponentAdapter c) {
					if (c.getValue() instanceof Application) {
						Application app = (Application) c.getValue();
						if (!runningApplications.contains(app)) { // loaded, but not running
							return true;
						}
					}
					return false;
				}
			});
			listOfApps.setCellRenderer(new DefaultListCellRenderer() {
				public Component getListCellRendererComponent(JList list, Object value, int index,
						boolean isSelected, boolean cellHasFocus) {
					String appDescription = value.toString();
					if (value instanceof Application) {
						Application app = (Application) value;
						if (runningApplications.contains(app)) { // loaded, but not running
							appDescription = appDescription + " [running]";
						} else {
							appDescription = appDescription + " [stopped]";
						}
					}
					return super.getListCellRendererComponent(list, appDescription, index,
							isSelected, cellHasFocus);
				}
			});
			listOfApps.setBorder(BorderFactory.createEmptyBorder(20, 5, 20, 5));
			listOfApps.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			listOfApps.setFont(APP_MANAGER_FONT);
			if (model.getSize() > 0) {
				listOfApps.setSelectedIndex(0);
			}
		}
		return listOfApps;
	}

	/**
	 * @return
	 */
	private Component getMainMessage() {
		if (mainMessage == null) {
			mainMessage = new JLabel("<html>Manage your applications here.<br/>"
					+ "Closing this App Manager will stop <b>all</b> running applications.</html>");
			mainMessage.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
			mainMessage.setFont(APP_MANAGER_FONT);
		}
		return mainMessage;
	}

	/**
	 * @return
	 */
	private Component getPrintSheetsButton() {
		if (printSheetsButton == null) {
			printSheetsButton = new JButton("Make PDFs");
			printSheetsButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					Application selectedApp = (Application) listOfApps.getSelectedValue();
					if (selectedApp != null) {
						selectedApp.renderToPDF(new File("."), "FileName");
						listOfApps.repaint();
					}
				}
			});
		}
		return printSheetsButton;
	}

	/**
	 * @return
	 */
	private Component getStartApplicationButton() {
		if (startAppButton == null) {
			startAppButton = new JButton("Start Selected Application");
			startAppButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					Application selectedApp = (Application) listOfApps.getSelectedValue();
					if (selectedApp != null) {
						startApplication(selectedApp);
						listOfApps.repaint();
					}
				}
			});
		}
		return startAppButton;
	}

	/**
	 * @return
	 */
	private Component getStopApplicationButton() {
		if (stopAppButton == null) {
			stopAppButton = new JButton("Stop Selected Application");
			stopAppButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					Application selectedApp = (Application) listOfApps.getSelectedValue();
					if (selectedApp != null) {
						stopApplication(selectedApp);
						listOfApps.repaint();
					}
				}
			});
		}
		return stopAppButton;
	}

	/**
	 * @param app
	 */
	public void loadApplication(Application app) {
		DebugUtils.println("Loading " + app.getName());
		loadedApplications.add(app);
		// show the app manager
		if (useAppManager) {
			getApplicationManager();
		}
	}

	/**
	 * TODO: Figure out the easiest way to send a PDF (with or without regions) to the default
	 * printer.
	 * 
	 * @param sheet
	 */
	public void print(Sheet sheet) {
		// Implement this...
		DebugUtils.println("Unimplemented Method");
	}

	/**
	 * A Welcome message.
	 */
	private void printInitializationMessages() {
		final String dashes = StringUtils.repeat("-", versionString.length());
		System.out.println("-----------------------------------------------------------" + dashes);
		System.out.println("Reduce, Recycle, Reuse: A Paper Applications Toolkit ver. "
				+ versionString);
		System.out.println("-----------------------------------------------------------" + dashes);
	}

	/**
	 * 
	 */
	public static void startAcrobatDesigner() {
		AcrobatDesignerLauncher.start();
	}

	/**
	 * Start this application and register all live pens with the event engine. The event engine
	 * will then start dispatching events for this application until the application is stopped.
	 * 
	 * @param paperApp
	 */
	public void startApplication(Application paperApp) {
		if (!loadedApplications.contains(paperApp)) {
			loadApplication(paperApp);
		}

		// get all the pens and start them in live mode...
		// we assume we have decided where eac pen server will run
		// start live mode will connect to that pen server.
		final List<Pen> pens = paperApp.getPens();

		// add all the live pens to the eventEngine
		for (Pen pen : pens) {
			pen.startLiveMode(); // starts live mode at the pen's default place
			if (pen.isLive()) {
				eventEngine.register(pen);
			}
		}

		// keep track of the pattern assigned to different sheets and regions
		eventEngine.registerPatternMapsForEventHandling(paperApp.getPatternMaps());
		runningApplications.add(paperApp);
	}

	/**
	 * Remove the application and stop receiving events from its pens....
	 * 
	 * @param paperApp
	 */
	public void stopApplication(Application paperApp) {
		final List<Pen> pens = paperApp.getPens();
		for (Pen pen : pens) {
			if (pen.isLive()) {
				eventEngine.unregisterPen(pen);
				// stop the pen from listening!
				pen.stopLiveMode();
			}
		}
		eventEngine.unregisterPatternMapsForEventHandling(paperApp.getPatternMaps());
		runningApplications.remove(paperApp);
	}

	/**
	 * @param app
	 */
	public void unloadApplication(Application app) {
		loadedApplications.remove(app);
	}

	/**
	 * @param flag
	 */
	public void useApplicationManager(boolean flag) {
		useAppManager = flag;
	}
}