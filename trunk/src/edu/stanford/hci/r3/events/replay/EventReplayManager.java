package edu.stanford.hci.r3.events.replay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.hci.r3.PaperToolkit;
import edu.stanford.hci.r3.events.EventEngine;
import edu.stanford.hci.r3.events.PenEvent;
import edu.stanford.hci.r3.events.PenEvent.PenEventModifier;
import edu.stanford.hci.r3.pen.PenSample;
import edu.stanford.hci.r3.util.DebugUtils;
import edu.stanford.hci.r3.util.files.FileUtils;

/**
 * <p>
 * This class interacts with the EventEngine to simulate real-time input events. The events can be
 * loaded from disk (XML files), and can be either batched or realtime events. Alternatively, events
 * generated by an actual pen can be saved out to a file, for future replay.
 * </p>
 * <p>
 * <span class="BSDLicense"> This software is distributed under the <a
 * href="http://hci.stanford.edu/research/copyright.txt">BSD License</a>. </span>
 * </p>
 * 
 * @author <a href="http://graphics.stanford.edu/~ronyeh">Ron B Yeh</a> (ronyeh(AT)cs.stanford.edu)
 */
public class EventReplayManager {

	/**
	 * 
	 */
	public static final String[] FILE_EXTENSION = new String[] { "eventData" };

	/**
	 * For tab-delimiting the fields in the eventData file.
	 */
	private static final String SEPARATOR = "\t";

	/**
	 * We will dispatch events to this event engine, simulating input from one or more pens...
	 */
	private EventEngine eventEngine;

	/**
	 * Events that we can replay...
	 */
	private ArrayList<PenEvent> loadedEvents = new ArrayList<PenEvent>();

	/**
	 * Allows us to write to our output file for serializing the event stream.
	 */
	private PrintWriter output;

	/**
	 * Write events to disk (autoflushed), so that we can replay sessions in the future.
	 */
	private File outputFile;

	private boolean playEventsInRealTime = true;

	/**
	 * @param engine
	 */
	public EventReplayManager(EventEngine engine) {
		eventEngine = engine;
	}

	/**
	 * 
	 */
	public void clearLoadedEvents() {
		loadedEvents = new ArrayList<PenEvent>();
	}

	/**
	 * The inverse of createStringFromEvent(...). This creates a PenEvent object from one line of
	 * the eventData file.
	 * 
	 * @param eventString
	 * @return
	 */
	public PenEvent createEventFromString(String eventString) {
		// DebugUtils.println(eventString);
		final String[] fields = eventString.split(SEPARATOR);
		final PenEventModifier modifier = PenEventModifier.valueOf(fields[0]);
		final String penName = fields[2];
		final int penID = Integer.parseInt(fields[1]);
		final long time = Long.parseLong(fields[3]);
		final double x = Double.parseDouble(fields[4]);
		final double y = Double.parseDouble(fields[5]);
		final long ts = Long.parseLong(fields[6]);
		final int f = Integer.parseInt(fields[7]);

		final PenEvent event = new PenEvent(penID, penName, time, new PenSample(x, y, f, ts));
		event.setModifier(modifier);
		return event;
	}

	/**
	 * @param event
	 * @return
	 */
	private String createStringFromEvent(PenEvent event) {
		final PenSample sample = event.getOriginalSample();
		return event.getModifier() + SEPARATOR + event.getPenID() + SEPARATOR + event.getPenName()
				+ SEPARATOR + event.getTimestamp() + SEPARATOR + sample.getX() + SEPARATOR
				+ sample.getY() + SEPARATOR + sample.getTimestamp() + SEPARATOR + sample.getForce();
	}

	/**
	 * @return
	 */
	private File getEventStoragePath() {
		return new File(PaperToolkit.getToolkitRootPath(), "eventData/");
	}

	/**
	 * @return the printWriter to the eventData file. This is initialized lazily, because we do not
	 *         want to create a file if saveEvent is never called.
	 */
	private PrintWriter getOutput() {
		if (output == null) {
			try {
				outputFile = new File(getEventStoragePath(), FileUtils
						.getCurrentTimeForUseInASortableFileName()
						+ ".eventData");
				output = new PrintWriter(new FileOutputStream(outputFile), true /* autoflush */);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		return output;
	}

	/**
	 * @param eventDataFile
	 */
	public void loadEventDataFrom(File eventDataFile) {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(eventDataFile));
			String inputLine = null;
			while ((inputLine = br.readLine()) != null) {
				PenEvent event = createEventFromString(inputLine);
				loadedEvents.add(event);
			}
			DebugUtils.println("Loaded " + loadedEvents.size() + " events.");
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Load the most recent event data file...
	 */
	public void loadMostRecentEventData() {
		final List<File> eventFiles = FileUtils.listVisibleFiles(getEventStoragePath(),
				FILE_EXTENSION);
		if (eventFiles.size() > 0) {
			final File mostRecentFile = eventFiles.get(eventFiles.size() - 1);
			DebugUtils.println(mostRecentFile);
			loadEventDataFrom(mostRecentFile);
		} else {
			DebugUtils.println("No Event Data Files Found in " + getEventStoragePath());
		}
	}

	/**
	 * Replays the list of events... Ideally, this should play it back at real time or some multiple
	 * of realtime...
	 * 
	 * Threaded, because we do not want any GUI to block when calling this. Alternatively, refactor
	 * this into blocking & nonblocking versions.
	 * 
	 * @param events
	 */
	public void replay(final List<PenEvent> events) {
		new Thread(new Runnable() {
			public void run() {
				long lastTimeStamp = 0;
				for (PenEvent event : events) {
					if (playEventsInRealTime && lastTimeStamp != 0) {
						try {
							// pause some amount, to replicate realtime...
							long diff = event.getTimestamp() - lastTimeStamp;
							if (diff > 0) {
								// DebugUtils.println("Sleeping for " + diff + " ms between
								// events.");
								Thread.sleep(diff);
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

					}

					// assume here that all PenEvent objects have their flags set correctly
					if (event.isPenUp()) {
						eventEngine.handlePenUpEvent(event);
					} else {
						eventEngine.handlePenEvent(event);
					}

					lastTimeStamp = event.getTimestamp();
				}
				DebugUtils.println("Done. Replayed " + events.size() + " Events");
			}
		}).start();
	}

	/**
	 * Replay the events that have been loaded, in the order that they appear in the list...
	 */
	public void replayLoadedEvents() {
		replay(loadedEvents);
	}

	/**
	 * Save this pen event. This is done automatically for events streamed through the Event Engine.
	 * In the future, we should probably log at the PenListener level too! This allows arbitrary
	 * event data save and replay.
	 * 
	 * @param event
	 */
	public void saveEvent(PenEvent event) {
		getOutput().println(createStringFromEvent(event));
	}
}
