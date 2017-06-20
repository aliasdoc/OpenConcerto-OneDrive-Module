package org.openconcerto.modules.onedrive;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.openconcerto.erp.preferences.DefaultLocalPreferencePanel;
import org.openconcerto.modules.onedrive.exceptions.OneDriveInternalException;
import org.openconcerto.modules.onedrive.networking.OneDriveAuthServer;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.component.TextPrompt;
import org.openconcerto.utils.JImage;

import de.tuberlin.onedrivesdk.OneDriveException;
import de.tuberlin.onedrivesdk.OneDriveFactory;
import de.tuberlin.onedrivesdk.OneDriveSDK;
import de.tuberlin.onedrivesdk.common.OneDriveScope;
import de.tuberlin.onedrivesdk.file.OneFile;
import de.tuberlin.onedrivesdk.folder.OneFolder;
import de.tuberlin.onedrivesdk.uploadFile.OneUploadFile;

@SuppressWarnings("serial")
public class OneDrivePreferencePanel extends DefaultLocalPreferencePanel {
	public static final String PROP_CLIENT_ID = "clientID";
    public static final String PROP_CLIENT_SECRET = "clientSecret";
    public static final String PROP_CLIENT_REFRESH_TOKEN = "refreshToken";
	public static final String PROP_LIST_SHOW_MENU = "listShowMenu";
    public static final String PROP_AUTO_SAVE = "auto";
    public static final String PROP_SHAREPOINT_URL = "sharepointURL";
    public static final String PROP_ACCOUNT_CLASSIC = "accountClassic";
    public static final String PROP_ACCOUNT_BUSINESS = "accountBusiness";
	public static final String ONEDRIVE_PROPERTIES = "onedrive.properties";
	
	final JTextField textClientID = new JTextField();
    final JTextField textClientSECRET = new JTextField();
    final JTextField textClientOAuthCode = new JTextField();
    final JTextField textClientRefreshToken = new JTextField();
    final JCheckBox checkAuto = new JCheckBox("Sauvegarder automatiquement les documents générés");
    final JCheckBox checkShowMenu = new JCheckBox("Activer le menu contextuel (clic droit sur les listes)");
    final JRadioButton radioOneDriveClassic = new JRadioButton("OneDrive Classique");
    final JRadioButton radioOneDriveBusiness = new JRadioButton("OneDrive Business");
    final JTextField textSharepointResourceURL = new JTextField();

	public OneDrivePreferencePanel() {
        super("OneDrive", ONEDRIVE_PROPERTIES);
        
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        
        // ONEDRIVE BANNER
        
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        
        c.gridwidth = 3;
        c.gridheight = 1;
        JImage imgOneDrive = new JImage(OneDrivePreferencePanel.class.getResource("OneDrive.png"));
        imgOneDrive.setBackground(Color.WHITE);
        imgOneDrive.check();
        this.add(imgOneDrive, c);
        
        c.weighty = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.BOTH;
        this.add(new TitledSeparator("Paramètres oAuth2 du client"), c);
        
        // CLIENT CREDENTIALS
        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;
        this.add(new JLabel("Client ID", SwingConstants.RIGHT), c);
        c.gridx++;
        c.gridwidth = 2;
        @SuppressWarnings("unused")
		TextPrompt textPromptClientID = new TextPrompt("CLIENT ID", textClientID);
        this.add(textClientID, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        this.add(new JLabel("Client SECRET", SwingConstants.RIGHT), c);
        
        c.gridx++;
        c.gridwidth = 2;
        @SuppressWarnings("unused")
		TextPrompt textPromptClientSecret = new TextPrompt("CLIENT SECRET", textClientSECRET);
        this.add(textClientSECRET, c);
        
        
        
        
        final JButton btnToken = new JButton("Obtenir un token");
        btnToken.setOpaque(false);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        this.add(btnToken, c);
        btnToken.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	if (textClientID.getText().equals("") | textClientSECRET.getText().equals("")){
            		JOptionPane.showMessageDialog(OneDrivePreferencePanel.this, "Veuillez renseigner les champes Client ID et Client SECRET", "Erreur",
            		        JOptionPane.ERROR_MESSAGE);  
            		return;
            	}

            	Semaphore answerLock = new Semaphore(1);
            	OneDriveAuthServer server = new OneDriveAuthServer(answerLock);
            	server.start();
            	
            	OneDriveSDK sdk = OneDriveFactory.createOneDriveSDK(
            		    textClientID.getText(), 
            		    textClientSECRET.getText(),             
            		    server.getCallbackURL(),                              
            		    OneDriveScope.READWRITE, OneDriveScope.OFFLINE_ACCESS);        
            	
            	try {
        			Desktop.getDesktop().browse(new URI(sdk.getAuthenticationURL()));
        		}
        		catch (URISyntaxException ex) {
        			throw new OneDriveInternalException(
        					"URL incorrecte, vérifier l'url retournée par le SDK OneDriver", ex);
        		}
        		catch (IOException ex) {
        			throw new UnsupportedOperationException("Impossible de lancer le navigateur du système pour l'authentification.", ex);
        		}

        		try {
        			answerLock.acquire();
        		}
        		catch (InterruptedException ex) {
        			throw new OneDriveInternalException(" Interruption du Semaphore dans " + this.getClass().getName());
        		}

        		String code = server.close();
        		answerLock.release();

        		if (code == null) {
        			throw new OneDriveInternalException("Le code n'as pu être récupéré");
        		} else {
        			try {
						sdk.authenticate(code);
						textClientOAuthCode.setText(code);
						textClientRefreshToken.setText(sdk.getRefreshToken());
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (OneDriveException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
        		}
        		
        		
            }
        });
        
        c.gridx++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        @SuppressWarnings("unused")
		TextPrompt textPromptAccessToken = new TextPrompt("CODE D'AUTHORISATION", textClientOAuthCode);
        this.add(textClientRefreshToken, c);
        textClientRefreshToken.setEnabled(false);
        
        
        
/*        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        this.add(new JLabel("Refresh token:", SwingConstants.RIGHT), c);
        
        c.gridx++;
        c.gridwidth = 2;
        @SuppressWarnings("unused")
		TextPrompt textPromptRefreshToken = new TextPrompt("TOKEN DE RAFRAICHISSEMENT", textClientRefreshToken);
        this.add(textClientRefreshToken, c);*/
      /*  
        // TYPE DE COMPTE
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.BOTH;
        this.add(new TitledSeparator("Type de compte"), c);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridy++;
        c.gridx = 0;
        

        ButtonGroup group = new ButtonGroup();
        group.add(radioOneDriveClassic);
        group.add(radioOneDriveBusiness);
        
        c.gridy++;
        c.gridwidth = 3;
        radioOneDriveClassic.setSelected(true);
        radioOneDriveClassic.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	textSharepointResourceURL.setEnabled(false);
            }
        });
        this.add(radioOneDriveClassic, c);
        
        c.gridy++;
        c.gridwidth = 1;
        radioOneDriveBusiness.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	textSharepointResourceURL.setEnabled(true);
            }
        });
        this.add(radioOneDriveBusiness, c);
        
        c.gridx++;
        this.add(new JLabel("Url de ressource", SwingConstants.RIGHT), c);
        
        c.gridx++;
        c.gridwidth = 2;
        textSharepointResourceURL.setEnabled(false);
		TextPrompt textPromptResourceURL = new TextPrompt("REQUIS", textSharepointResourceURL);
        textPromptResourceURL.setIcon(ImageIconWarning.getInstance());
        this.add(textSharepointResourceURL, c);
        
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        this.add(new JLabelWarning("L'URL de ressource correspond à celle fournie par microsoft, ex.: https://masociete-my.sharepoint.com"), c);		
        */
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.BOTH;
        this.add(new TitledSeparator("Paramètres"), c);
        
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 3;
        this.add(checkAuto, c);
        
        c.gridx = 0;
        c.gridy++;
        this.add(checkShowMenu, c);
        
        final JButton bTest = new JButton("Tester les paramètres");
        bTest.setOpaque(false);
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHEAST;
        this.add(bTest, c);
        bTest.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	if (textClientOAuthCode.getText().equals("")){
            		JOptionPane.showMessageDialog(OneDrivePreferencePanel.this, "Aucun token disponible,  veuillez d'abord en récupérer un en cliquant sur le bouton \"Obtenir un token\"", "Erreur",
            		        JOptionPane.ERROR_MESSAGE);  
            		return;
            	}

            	final Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                        	OneDriveSDK sdk = OneDriveFactory.createOneDriveSDK(
                        			textClientID.getText(), 
                        			textClientSECRET.getText(), 
                    			    "",                              
                    			    OneDriveScope.READWRITE, OneDriveScope.OFFLINE_ACCESS); 
                            try {
                                sdk.authenticateWithRefreshToken(textClientRefreshToken.getText()); 
                            
                            } catch (OneDriveException e) {
                            	e.printStackTrace();
                                throw new IOException("Identifiant ou mot de passe incorrect");
                            }

                            final File f = File.createTempFile("test_onedrive", "openconcerto.txt");
                            final FileOutputStream fOut = new FileOutputStream(f);
                            fOut.write("OpenConcerto".getBytes());
                            fOut.flush();
                            fOut.close();
                            
                            ExecutorService executor = Executors.newFixedThreadPool(5);
                            
                            OneFolder oc = null;
                            try {
                            	oc = sdk.getFolderByPath("OpenConcerto");
                            } catch (OneDriveException e) {
                            	System.out.println(e.getMessage());
                                oc = sdk.getRootFolder().createFolder("OpenConcerto");
                            }
                            
                            OneUploadFile upload = oc.uploadFile(f);
                            Future<OneFile> futureUpload = executor.submit(upload);
                            System.out.println(futureUpload.get().getCreatedDateTime());
                    
                            JOptionPane.showMessageDialog(OneDrivePreferencePanel.this, "Connexion réussie");
                        } catch (Throwable e1) {
                        	e1.printStackTrace();
                            JOptionPane.showMessageDialog(OneDrivePreferencePanel.this, "Connexion échouée");
                        }
                    }
                });
                t.setDaemon(true);
                t.setName("Test OneDrive");
                t.start();
            }
        });
        
        
        //this.debugLayout(this);

        textClientID.setText(properties.getProperty(PROP_CLIENT_ID, ""));
        textClientSECRET.setText(properties.getProperty(PROP_CLIENT_SECRET, ""));
        textClientRefreshToken.setText(properties.getProperty(PROP_CLIENT_REFRESH_TOKEN, ""));
        checkAuto.setSelected(properties.getProperty(PROP_AUTO_SAVE, "false").equals("true"));
        checkShowMenu.setSelected(properties.getProperty(PROP_LIST_SHOW_MENU, "false").equals("true"));
        radioOneDriveClassic.setSelected(properties.getProperty(PROP_ACCOUNT_CLASSIC, "false").equals("true"));
        radioOneDriveBusiness.setSelected(properties.getProperty(PROP_ACCOUNT_BUSINESS, "false").equals("true"));
        textSharepointResourceURL.setText(properties.getProperty(PROP_SHAREPOINT_URL, ""));
	}
	
	
	@Override
    public void storeValues() {
		properties.setProperty(PROP_CLIENT_ID, textClientID.getText());
        properties.setProperty(PROP_CLIENT_SECRET, textClientSECRET.getText());
        properties.setProperty(PROP_CLIENT_REFRESH_TOKEN, textClientRefreshToken.getText());
        properties.setProperty(PROP_AUTO_SAVE, String.valueOf(this.checkAuto.isSelected()));
        properties.setProperty(PROP_LIST_SHOW_MENU, String.valueOf(this.checkShowMenu.isSelected()));
        properties.setProperty(PROP_ACCOUNT_CLASSIC, String.valueOf(this.radioOneDriveClassic.isSelected()));
        properties.setProperty(PROP_ACCOUNT_BUSINESS, String.valueOf(this.radioOneDriveBusiness.isSelected()));
        properties.setProperty(PROP_SHAREPOINT_URL, textSharepointResourceURL.getText());
        super.storeValues();
    }

    @Override
    public void restoreToDefaults() {
    	textClientID.setText("");
        textClientSECRET.setText("");
        textClientOAuthCode.setText("");
        textClientRefreshToken.setText("");
        checkAuto.setSelected(false);
        checkShowMenu.setSelected(false);
        radioOneDriveClassic.setSelected(true);
        radioOneDriveBusiness.setSelected(false);
        textSharepointResourceURL.setText("");
    }

    public static Properties getProperties() throws IOException {
        return DefaultLocalPreferencePanel.getPropertiesFromFile(ONEDRIVE_PROPERTIES);
    }
    
    public void debugLayout(JPanel parent)
    {
    	final Random r=new Random();
    	for (Component c : parent.getComponents())
        {
    		Color color = new Color(r.nextInt(256),r.nextInt(256),r.nextInt(256),r.nextInt(256));
    		c.setBackground(color);
        }
    }
}
