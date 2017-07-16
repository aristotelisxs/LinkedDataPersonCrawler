import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;


public class ReconstructIndexFile extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1862805560886605033L;
	private JPanel contentPane;

	/**
	 * List populated with filenames of countries' index files. 
	 */
	private JList<String> list;

	/**
	 * Establish linkage with the main search interface GUI by storing a local pointer to
	 * it for communication between the two needs. 
	 */
	private SearchInterface mainGUI;

	/**
	 * Default value for people to crawl in a re-constructed file.
	 */
	private final int defaultPeopleToCrawl = 50;

	private JButton btnReconstruct;

	private JButton btnBack;
	
	/**
	 * Create the frame.
	 */
	public ReconstructIndexFile(SearchInterface mainGUI) {
		this.mainGUI = mainGUI;

		setTitle("File Reconstruction");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 375, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 43, 339, 177);
		contentPane.add(scrollPane);

		list = new JList<String>();
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane.setViewportView(list);

		btnReconstruct = new JButton("Reconstruct");
		btnReconstruct.setMnemonic('R');
		btnReconstruct.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if(!ReconstructIndexFile.this.list.isSelectionEmpty()){

					int peopleToCrawl = -1;

					// Upper and lower bound within which the number of users selected for a new index file should lie.
					while(peopleToCrawl <= 0 || peopleToCrawl >= 300){

						Object returned = JOptionPane.showInputDialog(ReconstructIndexFile.this, "Please give us the number of users it should hold: ", 
								"Info", JOptionPane.PLAIN_MESSAGE, null, null, "");

						if(returned == null){

							JOptionPane.showMessageDialog(ReconstructIndexFile.this, "Using default value of " + defaultPeopleToCrawl);
							peopleToCrawl = defaultPeopleToCrawl;

						} else {

							try{

								peopleToCrawl = Integer.parseInt(returned.toString());

							} catch (Exception ex) {

								JOptionPane.showMessageDialog(ReconstructIndexFile.this, "Not a valid number.");

							}
						}

					}

					String countryName = ReconstructIndexFile.this.list.getSelectedValue();
					File file = new File(countryName);
					countryName = countryName.replace("_index.tsv", "");

					/*
					 *  Delete old file to make room for the new to be re-constructed by the new object
					 *  representing the country selected.
					 */

					if(file.delete()){

						System.out.println(file.getName() + " has been deleted!");

					} else {

						System.out.println("Delete operation has failed.");

					}

					System.out.println("Replacing " + countryName + " file and object.");
					PersonCrawler crawler = new PersonCrawler(peopleToCrawl, true);
					
					ReconstructIndexFile.this.disableComponents();
					ReconstructIndexFile.this.mainGUI.reconstructIndex(countryName, crawler);
					ReconstructIndexFile.this.enableComponents();

				} else {

					JOptionPane.showMessageDialog(ReconstructIndexFile.this, "Please select a value first.");

				}
			}
		});

		btnReconstruct.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
		btnReconstruct.setBounds(69, 231, 110, 23);
		contentPane.add(btnReconstruct);

		btnBack = new JButton("Back");
		btnBack.setMnemonic('B');
		btnBack.setToolTipText("Return to searching screen");
		btnBack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				ReconstructIndexFile.this.setVisible(false);
				ReconstructIndexFile.this.mainGUI.setVisible(true);

			}
		});
		btnBack.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
		btnBack.setBounds(189, 231, 89, 23);
		contentPane.add(btnBack);

		JLabel lblAvailableCountryIndex = new JLabel("Select from the available country index files:");
		lblAvailableCountryIndex.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
		lblAvailableCountryIndex.setBounds(10, 11, 268, 26);
		contentPane.add(lblAvailableCountryIndex);
	}

	/**
	 * This method will populate the list in the current JFrame with the file names
	 * of inverse index text files that exist in the project's directory.
	 */
	public void setListData(){

		// Search in current directory using the empty string.
		File[] fileArray = this.finder(System.getProperty("user.dir"));
		String[] names = new String[fileArray.length];

		// Iterate through the extracted file objects and retrieve their names.
		for (int i = 0; i < fileArray.length; i++) {

			names[i] = fileArray[i].getName();

		}

		this.list.setListData(names);

	}

	/**
	 * A method that will extract file objects based on the presence of country inverse index files in a given
	 * directory (within project to avoid hard-coding)
	 * @param dirPath The directory within which to search files for.
	 * @return A file array with inverse index files.
	 */
	public File[] finder(String dirPath){

		File dir = new File(dirPath);

		return dir.listFiles(new FilenameFilter() { 

			public boolean accept(File dir, String filename){

				return (filename.endsWith("_index.tsv") && !filename.startsWith("Generic_")); 

			}
		} );

	}
	
	/**
	 * Disable main components of this object. 
	 */
	public void disableComponents(){
		
		this.btnBack.setEnabled(false);
		this.btnReconstruct.setEnabled(false);
		
	}
	
	/**
	 * Enable main components of this object.
	 */
	public void enableComponents(){
		
		this.btnBack.setEnabled(true);
		this.btnReconstruct.setEnabled(true);
	
	}
	
}
