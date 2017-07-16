import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 * This visual class uses Swing to give a compact GUI for the user of this application to select the keywords with which
 * the search will be initiated as well as the country that is associated with these people.
 * @author Aristotelis Charalampous
 *
 */
public class SearchInterface extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2804841429803943181L;
	private JPanel contentPane;

	/**
	 * Key words to drive queries will be given here. 
	 */
	private JTextField textField;

	/**
	 * A combo box used to allow the user to select the country within which
	 * queries will be firstly directed.
	 */
	private JComboBox<String> comboBox;

	/**
	 * We opt to storing a set of PersonCrawler objects for each individual country so that
	 * waiting times are extremely minimised for future queries about people from countries which
	 * there were past queries. The keys correspond to the country's name.
	 */
	private HashMap<String, PersonCrawler> crawlerMap = new HashMap<String, PersonCrawler>();

	/**
	 *  Reporting screen with URI list object.
	 */
	public final ReportingFrame reportScreen = new ReportingFrame("Complete Crawling Results");

	public final ReportingFrame precomputeReportScreen = new ReportingFrame("Precomputed Crawling Results");
	
	/**
	 * Reconstruction screen with index files objects. 
	 */
	private final ReconstructIndexFile reconstructScreen = new ReconstructIndexFile(this);

	private final int defaultPeopleToCrawl = 50;
	
	/**
	 * Stop words removing object.
	 */
	private final StopWords stopWords = new StopWords();
	
	/**
	 * Click and magic happens! 
	 */
	private JButton btnSubmit;
	
	private JButton btnReconstructFiles;
	
	/**
	 * Create the frame.
	 */
	public SearchInterface() {
		setResizable(false);
		setTitle("Linked Data Person Crawler");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 361, 133);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		textField = new JTextField();
		textField.setFont(new Font("Trebuchet MS", Font.PLAIN, 11));
		textField.setBounds(109, 11, 236, 20);
		contentPane.add(textField);
		textField.setColumns(10);

		JLabel lblNewLabel = new JLabel("Search terms:");
		lblNewLabel.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
		lblNewLabel.setBounds(10, 14, 89, 14);
		contentPane.add(lblNewLabel);

		comboBox = new JComboBox<String>();
		comboBox.setFont(new Font("Trebuchet MS", Font.PLAIN, 11));
		// A selection of all official countries in the world from: https://www.countries-ofthe-world.com/all-countries.html
		comboBox.setModel(new DefaultComboBoxModel<String>(new String[] {"No Country", "Afghanistan", "Albania", "Algeria", "Andorra", "Angola", 
				"Antigua_and_Barbuda", "Argentina", "Armenia", "Australia", "Austria", "Azerbaijan", "Bahamas", "Bahrain", 
				"Bangladesh", "Barbados", "Belarus", "Belgium", "Belize", "Benin", "Bhutan", "Bolivia", "Bosnia_and_Herzegovina", 
				"Botswana", "Brazil", "Brunei", "Bulgaria", "Burkina_Faso", "Burundi", "Cabo_Verde", "Cambodia", "Cameroon", "Canada", 
				"Central_African_Republic", "Chad", "Chile", "China", "Colombia", "Comoros", "Congo,_Republic_of_the", 
				"Congo,_Democratic_Republic_of_the", "Costa_Rica", "Cote_d'Ivoire", "Croatia", "Cuba", "Cyprus", "Czech_Republic", 
				"Denmark", "Djibouti", "Dominica", "Dominican_Republic", "Ecuador", "Egypt", "El_Salvador", "Equatorial_Guinea", "Eritrea", 
				"Estonia", "Ethiopia", "Fiji", "Finland", "France", "Gabon", "Gambia", "Georgia", "Germany", "Ghana", "Greece", "Grenada", 
				"Guatemala", "Guinea", "Guinea-Bissau", "Guyana", "Haiti", "Honduras", "Hungary", "Iceland", "India", "Indonesia", "Iran", 
				"Iraq", "Ireland", "Israel", "Italy", "Jamaica", "Japan", "Jordan", "Kazakhstan", "Kenya", "Kiribati", "Kosovo", "Kuwait", 
				"Kyrgyzstan", "Laos", "Latvia", "Lebanon", "Lesotho", "Liberia", "Libya", "Liechtenstein", "Lithuania", "Luxembourg", 
				"Macedonia", "Madagascar", "Malawi", "Malaysia", "Maldives", "Mali", "Malta", "Marshall_Islands", "Mauritania", "Mauritius", 
				"Mexico", "Micronesia", "Moldova", "Monaco", "Mongolia", "Montenegro", "Morocco", "Mozambique", "Myanmar", "Namibia", 
				"Nauru", "Nepal", "Netherlands", "New_Zealand", "Nicaragua", "Niger", "Nigeria", "North_Korea", "Norway", "Oman", "Pakistan", 
				"Palau", "Palestine", "Panama", "Papua_New_Guinea", "Paraguay", "Peru", "Philippines", "Poland", "Portugal", "Qatar", 
				"Romania", "Russia", "Rwanda", "St._Kitts_and_Nevis", "St._Lucia", "St._Vincent_and_The_Grenadines", "Samoa", "San_Marino", 
				"Sao_Tome_and_Principe", "Saudi_Arabia", "Senegal", "Serbia", "Seychelles", "Sierra_Leone", "Singapore", "Slovakia", 
				"Slovenia", "Solomon_Islands", "Somalia", "South_Africa", "South_Korea", "South_Sudan", "Spain", "Sri_Lanka", "Sudan", 
				"Suriname", "Swaziland", "Sweden", "Switzerland", "Syria", "Taiwan", "Tajikistan", "Tanzania", "Thailand", "Timor-Leste", 
				"Togo", "Tonga", "Trinidad_and_Tobago", "Tunisia", "Turkey", "Turkmenistan", "Tuvalu", "Uganda", "Ukraine", 
				"United_Arab_Emirates", "UK", "US", "Uruguay", "Uzbekistan", "Vanuatu", "Vatican_City", "Venezuela", "Vietnam", "Yemen", 
				"Zambia", "Zimbabwe"}));

		comboBox.setBounds(109, 42, 236, 20);
		contentPane.add(comboBox);

		JLabel lblSelectACountry = new JLabel("Select country:");
		lblSelectACountry.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
		lblSelectACountry.setBounds(10, 45, 89, 14);
		contentPane.add(lblSelectACountry);

		btnSubmit = new JButton("Submit");
		btnSubmit.setMnemonic('S');
		btnSubmit.setToolTipText("Search the Linked Data Cloud");
		btnSubmit.addActionListener(new ActionListener() {

			/*
			 * The following routines concern the selection inputs for people that we will need to crawl into.
			 */
			public void actionPerformed(ActionEvent arg0) {

				// Dispose of the report screen if it is still open when a new search is requested.
				if(SearchInterface.this.reportScreen.isVisible())
					SearchInterface.this.reportScreen.setVisible(false);
					
				
				if(SearchInterface.this.textField.getText().trim().length() != 0){

					if(!SearchInterface.this.comboBox.getSelectedItem().equals("No Country")) {

						int peopleToCrawl = -1;

						// Upper and lower bound within which the number of users selected for a new index file should lie.
						while(peopleToCrawl <= 0){

							try{

								File f = new File(SearchInterface.this.comboBox.getSelectedItem().toString() + "_index.tsv");								

								/*
								 *  If the object exists from a previous execution of this code, do not ask for user input as to how many people
								 *  need to be queried (this has already been assigned previously).
								 */
								if(!SearchInterface.this.crawlerMap.containsKey(SearchInterface.this.comboBox.getSelectedItem().toString()) && !f.exists()){

									/*
									 *  NOTE: The number of users to hold assigned to an object for inverse index file creation purposes is FINAL unless the file is deleted
									 *  and re-constructed using this application.
									 */
									Object returned = JOptionPane.showInputDialog(SearchInterface.this, "Index file unavailable. Please provide its desired size:", 
											"Info", JOptionPane.PLAIN_MESSAGE, null, null, "");

									if(returned == null){

										JOptionPane.showMessageDialog(SearchInterface.this, "Using default value of " + defaultPeopleToCrawl);
										peopleToCrawl = defaultPeopleToCrawl;

									} else {

										try{
											peopleToCrawl = Integer.parseInt(returned.toString());
										} catch (Exception ex) {
											JOptionPane.showMessageDialog(SearchInterface.this, "Invalid number. Using default value of " + defaultPeopleToCrawl);
											peopleToCrawl = defaultPeopleToCrawl;
										}

									}

									PersonCrawler crawler = new PersonCrawler(peopleToCrawl, true);
									SearchInterface.this.crawlerMap.put(SearchInterface.this.comboBox.getSelectedItem().toString(), crawler);

								} else if (!SearchInterface.this.crawlerMap.containsKey(SearchInterface.this.comboBox.getSelectedItem().toString()) && f.exists()){

									/*
									 *  File already exists but there is no object associated to it. Create a new object that will automatically pick
									 *  up indexes from the file. The value we assign to the person to crawl does not matter here and is only assigned
									 *  to exit the loop.
									 */
									peopleToCrawl = defaultPeopleToCrawl;

									PersonCrawler crawler = new PersonCrawler(peopleToCrawl, true);
									SearchInterface.this.crawlerMap.put(SearchInterface.this.comboBox.getSelectedItem().toString(), crawler);

								} else {

									/*
									 * The object already exists so need to create a new one.
									 *  Assign this to exit the loop. The variable will not be used afterwards so it is
									 *  okay to do so.
									 */
									peopleToCrawl = 1;

								}

							} catch (Exception ex){

								JOptionPane.showMessageDialog(SearchInterface.this, "Invalid number. Please retry.");

							}

						}

						// Disable interface interactions while crawling is in progress.
						SearchInterface.this.disableComponents();
						PersonCrawler currentCrawler = SearchInterface.this.crawlerMap.get(SearchInterface.this.comboBox.getSelectedItem().toString());
						
						String[] reportDetails = currentCrawler.crawlIntoCountry(SearchInterface.this.comboBox.getSelectedItem().toString(), 
								SearchInterface.this.stopWords.removeStopWords(SearchInterface.this.textField.getText().trim().replaceAll(" +", " ")).split(" "));

						SearchInterface.this.enableComponents();

						if(reportDetails != null){

							// Matching resources were found.
							if(reportDetails.length != 0){

								SearchInterface.this.reportScreen.setListData(reportDetails);
								SearchInterface.this.reportScreen.setVisible(true);

							}

						} else {

							JOptionPane.showMessageDialog(SearchInterface.this, "No resources were found for the combination of selected keywords.");

						}


					} else {
						
						int reply = JOptionPane.showConfirmDialog(SearchInterface.this, "Are you sure? The keyword(s) will be used as source URI(s) instead.");

						if(reply == JOptionPane.NO_OPTION)

							JOptionPane.showMessageDialog(SearchInterface.this, "Please select a country first then.");

						else {

							int peopleToCrawl = defaultPeopleToCrawl;

							Object returned = JOptionPane.showInputDialog(SearchInterface.this, "Please provide number of people to find:", 
									"Info", JOptionPane.PLAIN_MESSAGE, null, null, "");

							if(returned == null)

								JOptionPane.showMessageDialog(SearchInterface.this, "Using default value of " + defaultPeopleToCrawl);

							else {

								try{
									peopleToCrawl = Integer.parseInt(returned.toString());
								} catch (Exception ex) {
									JOptionPane.showMessageDialog(SearchInterface.this, "Invalid number. Using default value of " + defaultPeopleToCrawl);
								}

							}

							PersonCrawler currentCrawler = null;
							
							if(!SearchInterface.this.crawlerMap.containsKey(SearchInterface.this.comboBox.getSelectedItem().toString())){
								currentCrawler = new PersonCrawler(peopleToCrawl, false);
								SearchInterface.this.crawlerMap.put(SearchInterface.this.comboBox.getSelectedItem().toString(), currentCrawler);
								System.out.println("Creating new generic crawler");
								
								boolean precomputeSuccess = SearchInterface.this.showPrecomputedData(currentCrawler, SearchInterface.this.stopWords
										.removeStopWords(SearchInterface.this.textField.getText().trim()
										.replaceAll(" +", " ")).split(" "), true);
								
								SearchInterface.this.disableComponents();

								// Initiate a crawler to 
								RunnableCrawl threadCrawl = new RunnableCrawl(currentCrawler, SearchInterface.this.stopWords
										.removeStopWords(SearchInterface.this.textField.getText().trim()
										.replaceAll(" +", " ")).split(" "), true, precomputeSuccess, SearchInterface.this);
								
								Thread t = new Thread(threadCrawl);
						        t.start();								
								
							} else {
								currentCrawler =  SearchInterface.this.crawlerMap.get(SearchInterface.this.comboBox.getSelectedItem().toString());
								currentCrawler.setPeopleRequired(peopleToCrawl);
								System.out.println("Using existing crawler");
								
								boolean precomputeSuccess = SearchInterface.this.showPrecomputedData(currentCrawler, SearchInterface.this.stopWords
										.removeStopWords(SearchInterface.this.textField.getText().trim()
										.replaceAll(" +", " ")).split(" "), false);
								
								SearchInterface.this.disableComponents();

								RunnableCrawl threadCrawl = new RunnableCrawl(currentCrawler, SearchInterface.this.stopWords
										.removeStopWords(SearchInterface.this.textField.getText().trim()
										.replaceAll(" +", " ")).split(" "), false, precomputeSuccess, SearchInterface.this);
								
								Thread t = new Thread(threadCrawl);
						        t.start();
								
							}
									
						}

					}

				} else {

					JOptionPane.showMessageDialog(SearchInterface.this, "Please give a valid input.");

				}

			}
		});

		btnSubmit.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
		btnSubmit.setBounds(109, 73, 79, 23);
		contentPane.add(btnSubmit);

		btnReconstructFiles = new JButton("Reconstruct Files");
		btnReconstructFiles.setMnemonic('R');
		btnReconstructFiles.setToolTipText("Request index file reconstruction");
		btnReconstructFiles.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {

				SearchInterface.this.setVisible(false);
				SearchInterface.this.reconstructScreen.setVisible(true);
				SearchInterface.this.reconstructScreen.setListData();

			}
		});

		btnReconstructFiles.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
		btnReconstructFiles.setBounds(198, 73, 147, 23);
		contentPane.add(btnReconstructFiles);

	}

	/**
	 * Based on user request, we replace the object reference of a country, for which
	 * the index file was reconstructed, with the one it currently corresponds to.
	 * @param countryName The name of the country of which the index file was reconstructed.
	 * @param countryObject The crawler object which was used to reconstruct the index file.
	 */
	public void reconstructIndex(String countryName, PersonCrawler countryObject){

		this.crawlerMap.put(countryName, countryObject);

		/*
		 *  Disable further interactions with the currently visible interface (which we assume here would be the
		 *  reconstruction interface)
		 */
		this.reconstructScreen.setEnabled(false);

		/*
		 *  No need to report back to user as we crawl just for the sake of re-populating the re-constructed file
		 *  after user request (second method of re-constructing other than the automatic).
		 */
		countryObject.crawlIntoCountry(countryName, SearchInterface.this.stopWords.removeStopWords(SearchInterface.this.textField.getText().trim().replaceAll(" +", " ")).split(" "));

		this.reconstructScreen.setListData();

		// Resume interaction with reconstruction interface.
		this.reconstructScreen.setEnabled(true);

	}
	
	/**
	 * Disable main components of this object.
	 */
	public void disableComponents(){
		
		this.btnReconstructFiles.setEnabled(false);
		this.textField.setEnabled(false);
		this.btnSubmit.setEnabled(false);
		this.comboBox.setEnabled(false);
		
	}
	
	/**
	 * Enable main components of this object.
	 */
	public void enableComponents(){
		
		this.btnReconstructFiles.setEnabled(true);
		this.textField.setEnabled(true);
		this.btnSubmit.setEnabled(true);
		this.comboBox.setEnabled(true);
		
	}
	
	/**
	 * Display data that are collected from previous knowledge by extracting them from the pre-existing index file.
	 * @param liveCrawler The crawler that is going to perform a live crawling attempt
	 * @param keyWords The keywords used in the search engine.
	 * @param firstRun Is this the first time the crawler is running?
	 * @return
	 */
	public boolean showPrecomputedData(PersonCrawler liveCrawler, String[] keyWords, boolean firstRun){
		
		String[] precomputedResults = liveCrawler.prepopulateResults(keyWords, firstRun);
		
		if(precomputedResults != null && precomputedResults.length > 0){
			
			this.precomputeReportScreen.setVisible(true);
			this.precomputeReportScreen.setListData(precomputedResults);
			this.precomputeReportScreen.setEnabled(true);
			
			return true;
		}
		
		return false;
	}

}
