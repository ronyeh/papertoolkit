package edu.stanford.hci.r3.demos.gigaprints2006.blog;

import java.io.File;

import edu.stanford.hci.r3.PaperToolkit;
import edu.stanford.hci.r3.application.Application;
import edu.stanford.hci.r3.paper.Region;
import edu.stanford.hci.r3.paper.Sheet;

public class CommentTester extends Application {

	public static void main(String[] args) {
		CommentTester print = new CommentTester();
		// print.generateCode();

		PaperToolkit p = new PaperToolkit(true /* use app manager */);
		p.loadApplication(print);
	}

	public CommentTester() {
		super("Comment Tester");

		Sheet s = new Sheet(8.5, 11);

		s.addConfigurationPath(new File("."));

		Region r = new Region("CommentTest", 0.5, 0.5, 7.5, 7.5);
		CommentCollector cc = new CommentCollector(null, 0) {
			@Override
			public void contentArrived() {

			}
		};
		r.addEventHandler(cc);
		s.addRegion(r);

		addSheet(s);

	}

}