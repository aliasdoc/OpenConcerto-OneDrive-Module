package org.openconcerto.modules.onedrive;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Calendar;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureSQLElement;
import org.openconcerto.erp.core.sales.invoice.report.VenteFactureXmlSheet;
import org.openconcerto.erp.core.sales.quote.element.DevisSQLElement;
import org.openconcerto.erp.core.sales.quote.report.DevisXmlSheet;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.list.IListeAction;
import org.openconcerto.utils.ExceptionHandler;

public class OneDriveListAction implements IListeAction {
	@Override
    public ButtonsBuilder getHeaderButtons() {
        return ButtonsBuilder.emptyInstance();
    }

    @Override
    public Action getDefaultAction(IListeEvent evt) {
        return null;
    }

    @Override
    public PopupBuilder getPopupContent(PopupEvent evt) {
        final PopupBuilder actions = new PopupBuilder(this.getClass().getPackage().getName());
        final List<SQLRowValues> rows = evt.getSelectedRows();
        final JMenuItem createCallAction = createAction("Envoyer sur OneDrive", rows);
        ImageIcon icon = new ImageIcon(OneDrivePreferencePanel.class.getResource("onedrive-icon.png"));
        Image image = icon.getImage(); // transform it
        Image newimg = image.getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH); // scale it the smooth way 
        icon = new ImageIcon(newimg);  // transform it back
        createCallAction.setIcon(icon);
        actions.addItem(createCallAction);
        return actions;
    }
    
    @SuppressWarnings("serial")
	private JMenuItem createAction(final String label, final List<SQLRowValues> rows) {
        return new JMenuItem(new AbstractAction(label) {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    final Thread t = new Thread(new Runnable() {
                        @Override
                        final public void run() {
                            try {
                                final OneDriveStorageEngine engine = new OneDriveStorageEngine();
                                engine.connect();
                                final SQLElementDirectory directory = Configuration.getInstance().getDirectory();
                                final SQLTable invoiceTable = directory.getElement(SaisieVenteFactureSQLElement.class).getTable();
                                final SQLTable quoteTable = directory.getElement(DevisSQLElement.class).getTable();
                                for (SQLRowAccessor sqlRowAccessor : rows) {

                                    if (sqlRowAccessor.getTable().equals(invoiceTable)) {
                                        final SQLRow row = sqlRowAccessor.asRow();
                                        final Calendar c = row.getDate("DATE");
                                        final VenteFactureXmlSheet venteFactureXmlSheet = new VenteFactureXmlSheet(row);
                                        final File pdf = venteFactureXmlSheet.getGeneratedPDFFile();
                                        if (!pdf.exists()) {
                                            venteFactureXmlSheet.getOrCreateDocumentFile();
                                            venteFactureXmlSheet.showPrintAndExport(false, false, true);
                                        }
                                        engine.store(pdf, "Factures/" + c.get(Calendar.YEAR), pdf.getName(), true);
                                    } else if (sqlRowAccessor.getTable().equals(quoteTable)) {
                                        final SQLRow row = sqlRowAccessor.asRow();
                                        final Calendar c = row.getDate("DATE");
                                        final DevisXmlSheet devisXmlSheet = new DevisXmlSheet(row);
                                        final File pdf = devisXmlSheet.getGeneratedPDFFile();
                                        if (!pdf.exists()) {
                                            devisXmlSheet.getOrCreateDocumentFile();
                                            devisXmlSheet.showPrintAndExport(false, false, true);
                                        }
                                        engine.store(pdf, "Devis/" + c.get(Calendar.YEAR), pdf.getName(), true);
                                    }

                                }
                                engine.disconnect();
                            } catch (Exception e) {
                                JOptionPane.showMessageDialog(null, e.getMessage());
                            }

                        }
                    });
                    t.start();
                } catch (Exception ex) {
                    ExceptionHandler.handle("Echec de l'envoi", ex);
                }
            }
        });
    }
}
