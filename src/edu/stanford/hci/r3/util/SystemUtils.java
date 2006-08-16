package edu.stanford.hci.r3.util;

/**
 * <p>
 * This software is distributed under the <a href="http://hci.stanford.edu/research/copyright.txt">
 * BSD License</a>.
 * </p>
 * 
 * @author <a href="http://graphics.stanford.edu/~ronyeh">Ron B Yeh</a> (ronyeh(AT)cs.stanford.edu)
 */
public class SystemUtils {

	/**
	 * System character(s) for separating lines. Different for Unix/DOS/Mac.
	 */
	public static final String LINE_SEPARATOR = System.getProperties()
			.getProperty("line.separator");

}
