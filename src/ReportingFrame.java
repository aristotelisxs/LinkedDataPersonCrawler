import java.awt.Desktop;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.JLabel;

/**
 * This screen will output a list of the resource URIs that map to the keywords and country
 * given from the user's previous inputs. The URIs links can be clicked to open their respective.
 * web pages in the machine's browser.
 * @author Aristotelis Charalampous
 *
 */
public class ReportingFrame extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4430147364032712876L;
	private JPanel contentPane;
	
	/**
	 * Resource URI holding list reflecting the query results.
	 */
	private JList<String> list;
	private JLabel lblNewLabel;
	
	/**
	 * Create the frame.
	 */
	public ReportingFrame(String title) {
				
		setResizable(false);
		setTitle(title);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 450, 244);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JButton btnOk = new JButton("Close");
		btnOk.setMnemonic('C');
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {

				ReportingFrame.this.setVisible(false);
				ReportingFrame.this.dispose();

			}
		});

		btnOk.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
		btnOk.setBounds(184, 183, 74, 23);
		contentPane.add(btnOk);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 42, 424, 130);
		contentPane.add(scrollPane);

		list = new JList<String>();
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
		list.setToolTipText("Click on a person's URI to navigate to his/her page.");
		scrollPane.setViewportView(list);
		list.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e){
				if(e.getValueIsAdjusting())
					return;

				String url = (String)list.getSelectedValue();

				if(url != null){

					url = url.replace("<", "");
					url = url.replace(">", "");
					
					//There is no need to change the resource to page because the URIs are dereferenceable					
					//url = url.replace("resource", "page");

					System.out.println("Visiting " + url);
					
					try {
						Desktop.getDesktop().browse(new URI(url));
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (URISyntaxException e1) {
						e1.printStackTrace();
					}
				}

			}
		});

		list.setVisibleRowCount(10);
		
		lblNewLabel = new JLabel("Search results (click to navigate to web page):");
		lblNewLabel.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
		lblNewLabel.setBounds(10, 11, 275, 23);
		contentPane.add(lblNewLabel);
	}

	/**
	 * Set the resource URI list contents
	 * @param data the string array list
	 */
	public void setListData(String[] data){

		if(data != null)
			this.list.setListData(data);

	}
}
