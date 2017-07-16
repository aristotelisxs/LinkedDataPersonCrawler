import javax.swing.JOptionPane;

/**
 * This class will be used to avoid polling mechanisms when the Semantic Web is being queried by live crawlers
 * produced from this application.
 * @author Aristotelis Charalampous
 */
public class RunnableCrawl implements Runnable {

	
	/**
	 * See constructor description. 
	 */
	private PersonCrawler currentCrawler;
	
	/**
	 * See constructor description. 
	 */
	private String[] keyWords; 
	
	/**
	 * See constructor description. 
	 */
	private boolean firstRun;
	
	/**
	 * See constructor description. 
	 */
	private boolean precomputeSuccess;
	
	/**
	 * See constructor description. 
	 */
	private SearchInterface searchInterface;
	
    /**
     * Collect all required data to initiate the live querying process
     * @param crawler The live crawler
     * @param keyWords Keywords to be searched for
     * @param firstRun Is this the first live crawling of the provided crawler?
     * @param precomputeSuccess Were pre-computed information already provided to the user?
     * @param GUI The graphical user interface to report back to.
     */
    public RunnableCrawl(PersonCrawler crawler, String[] keyWords, boolean firstRun, boolean precomputeSuccess, SearchInterface GUI) {
    	
    	this.currentCrawler = crawler;
    	this.keyWords = keyWords;
    	this.firstRun = firstRun;
    	this.precomputeSuccess = precomputeSuccess;
    	this.searchInterface = GUI;
    	
    }

    public void run() {
    	
    	String[] reportDetails = this.currentCrawler.crawlIntoLinkedDataCloud(keyWords, firstRun, precomputeSuccess);
    	
    	// Re-enable the main interface.
    	this.searchInterface.enableComponents();
		
		if(reportDetails != null){

			// Matching resources were found.
			if(reportDetails.length != 0){

				/*
				 *  If results have been found after crawling, it means they have been found before as well.
				 *  Hide the previous screen.
				 */
				this.searchInterface.precomputeReportScreen.setVisible(false);
				this.searchInterface.reportScreen.setListData(reportDetails);
				this.searchInterface.reportScreen.setVisible(true);

			}

		} else {

			JOptionPane.showMessageDialog(this.searchInterface, "No resources were found for the selected keywords.");

		}
    }
}
