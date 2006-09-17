package edu.stanford.hci.r3.render.regions;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;

import edu.stanford.hci.r3.paper.regions.TextRegion;
import edu.stanford.hci.r3.render.RegionRenderer;
import edu.stanford.hci.r3.units.Points;

/**
 * <p>
 * Renders a Text Region.
 * </p>
 * <p>
 * <span class="BSDLicense"> This software is distributed under the <a
 * href="http://hci.stanford.edu/research/copyright.txt">BSD License</a>.</span>
 * </p>
 * 
 * @author <a href="http://graphics.stanford.edu/~ronyeh">Ron B Yeh</a> (ronyeh(AT)cs.stanford.edu)
 */
public class TextRenderer extends RegionRenderer {

	private Font font;

	/**
	 * Calculate how big the text is.
	 */
	private LineMetrics lineMetrics;

	private String text;

	/**
	 * Render the Text with this Color.
	 */
	private Color textColor;

	/**
	 * @param tr
	 */
	public TextRenderer(TextRegion tr) {
		super(tr);
		region = tr;
		text = tr.getText();
		font = tr.getFont();
		textColor = tr.getColor();
		lineMetrics = font.getLineMetrics(text, new FontRenderContext(null, true, true));
	}

	/**
	 * @return
	 */
	public Points getAscentInPoints() {
		return new Points(lineMetrics.getAscent());
	}

	/**
	 * @return
	 */
	public Points getLineHeightInPoints() {
		return new Points(lineMetrics.getHeight());
	}

	/**
	 * @param g2d
	 */
	public void renderToG2D(Graphics2D g2d) {
		if (RegionRenderer.DEBUG_REGIONS) {
			super.renderToG2D(g2d);
		}

		final TextRegion tr = (TextRegion) region;

		// so that we can reset it later
		final Font oldFont = g2d.getFont();
		g2d.setFont(tr.getFont());
		// System.out.println(tr.getFont());

		g2d.setColor(textColor);

		final double offset = getAscentInPoints().getValue();
		final double textLineHeight = getLineHeightInPoints().getValue();

		// handle multiple lines
		final String[] linesOfText = tr.getLinesOfText();
		final int xOffset = (int) Math.round(tr.getX().getValueInPoints());
		double yOffset = tr.getY().getValueInPoints() + offset;
		for (String line : linesOfText) {
			g2d.drawString(line, xOffset, (int) Math.round(yOffset));
			yOffset += textLineHeight;
		}

		// rest the font
		g2d.setFont(oldFont);
	}

}