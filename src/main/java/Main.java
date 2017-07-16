import org.apache.commons.logging.Log;
import org.apache.log4j.Logger;

/**
 * A simple method to instantiate the application.
 * @author Aristotelis Charalampous
 *
 */
public class Main {

	public static void main(String[] args) {

		SearchInterface interactiveUI = new SearchInterface();
		interactiveUI.setVisible(true);
		
	}

}
