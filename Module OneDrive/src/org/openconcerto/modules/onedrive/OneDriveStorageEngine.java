package org.openconcerto.modules.onedrive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openconcerto.erp.storage.StorageEngine;

import de.tuberlin.onedrivesdk.OneDriveException;
import de.tuberlin.onedrivesdk.OneDriveFactory;
import de.tuberlin.onedrivesdk.OneDriveSDK;
import de.tuberlin.onedrivesdk.common.OneDriveScope;
import de.tuberlin.onedrivesdk.file.OneFile;
import de.tuberlin.onedrivesdk.folder.OneFolder;
import de.tuberlin.onedrivesdk.uploadFile.OneUploadFile;


public class OneDriveStorageEngine implements StorageEngine {

	private OneDriveSDK sdk;
	
	@Override
    public boolean isConfigured() {
		try {
            final Properties props = OneDrivePreferencePanel.getProperties();
            System.out.println("IsConfigured: " + !props.getProperty(OneDrivePreferencePanel.PROP_CLIENT_REFRESH_TOKEN, "").isEmpty());
            return !props.getProperty(OneDrivePreferencePanel.PROP_CLIENT_REFRESH_TOKEN, "").isEmpty() ;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
	
	@Override
    public void connect() throws IOException {
		final Properties props = OneDrivePreferencePanel.getProperties();
		sdk = OneDriveFactory.createOneDriveSDK(
			    props.getProperty(OneDrivePreferencePanel.PROP_CLIENT_ID, ""), 
				props.getProperty(OneDrivePreferencePanel.PROP_CLIENT_SECRET, ""),
			    "",                              
			    OneDriveScope.READWRITE, OneDriveScope.OFFLINE_ACCESS); 
        try {
            sdk.authenticateWithRefreshToken(props.getProperty(OneDrivePreferencePanel.PROP_CLIENT_REFRESH_TOKEN, "")); 
        } catch (OneDriveException e) {
        	e.printStackTrace();
            throw new IOException("Identifiant ou mot de passe incorrect");
        }
    }
	
	@Override
    public void disconnect() throws IOException {
        // Nothing to do
    }
	
	@Override
    public void store(InputStream inStream, String remotePath, String title, boolean synchronous) throws IOException {
		System.out.println("stream remote path:" + remotePath);
		
		File file = new File(title);
		final FileOutputStream fOut = new FileOutputStream(file);
		byte[] buffer = new byte[inStream.available()];
		inStream.read(buffer);
        fOut.write(buffer);
        fOut.flush();
        fOut.close();
		
		store(file, remotePath, title, synchronous);
    }
	
	public void store(final File file, String remotePath, final String title, boolean synchronous) throws IOException {
		System.out.println("file remote path:" + remotePath);
		if (sdk == null) {
            throw new IllegalStateException("L'authentification est nécessaire avant de pouvoir sauvegarder le document");
        }
		ExecutorService executor = Executors.newFixedThreadPool(5);
		
		//String path = remotePath.replace("\\","/") + "/" + title;
		//System.out.println("Uploading file with path: " + path);
/*        try {
        	remoteFolder = sdk.getFolderByPath(path);
        } catch (OneDriveException e) {
            try {
            	System.out.println("Folder not found, trying to create: OpenConcerto/" + path);
				remoteFolder = sdk.getRootFolder().createFolder(path + ":/");
			} catch (OneDriveException e1) {
				// TODO Auto-generated catch block
				System.out.println("Error whent trying to create path: " + path);
				System.out.println(e.getMessage());
				e1.printStackTrace();
			}
        }*/
		
/*		OneFolder oc = null;
        try {
        	oc = sdk.getFolderByPath("OpenConcerto");
        } catch (OneDriveException e) {
        	System.out.println(e.getMessage());
            try {
				oc = sdk.getRootFolder().createFolder("OpenConcerto");
			} catch (OneDriveException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }*/
        
        OneFolder oc = null;
        
        try {
			oc = sdk.createOrGetFolderByPath("OpenConcerto/" + remotePath.replace("\\","/"));
		} catch (OneDriveException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        
        
        OneUploadFile upload = null;
		try {
			upload = oc.uploadFile(file);
		} catch (OneDriveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (upload == null) {
			throw new IllegalStateException("Impossible de créer le fichier à uploader.");
		}
        Future<OneFile> futureUpload = executor.submit(upload);
        try {
			System.out.println(futureUpload.get().getCreatedDateTime());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	@Override
    public boolean allowAutoStorage() {
		try {
            final Properties props = OneDrivePreferencePanel.getProperties();
            System.out.println("AutoSave: " + props.getProperty(OneDrivePreferencePanel.PROP_AUTO_SAVE, "true").equals("true"));
            return props.getProperty(OneDrivePreferencePanel.PROP_AUTO_SAVE, "true").equals("true");
        } catch (IOException e) {
            return false;
        }
    }
}
