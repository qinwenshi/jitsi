/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;
import java.security.cert.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.util.*;
import org.jitsi.service.resources.*;

/**
 * Dialog that is shown to the user when a certificate verification failed.
 * @author Damian Minkov
 */
class VerifyCertificateDialogImpl
    extends SIPCommDialog
    implements VerifyCertificateDialogService.VerifyCertificateDialog
{
    /**
     * Our logger.
     */
    private static final Logger logger =
        Logger.getLogger(VerifyCertificateDialogImpl.class);

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The resource service.
     */
    private ResourceManagementService R = DesktopUtilActivator.getResources();

    /**
     * The maximum width that we allow message dialogs to have.
     */
    private static final int MAX_MSG_PANE_WIDTH = 600;

    /**
     * The maximum height that we allow message dialogs to have.
     */
    private static final int MAX_MSG_PANE_HEIGHT = 800;

    /**
     * The certificate to show.
     */
    Certificate cert;
    java.util.List<X509Certificate> certs;

    /**
     * A text that describes why the verification failed.
     */
    String message;

    /**
     * The certificate panel.
     */
    TransparentPanel certPanel;

    /**
     * This dialog content pane.
     */
    TransparentPanel contentPane;

    /**
     * Whether certificate description is shown.
     */
    boolean certOpened = false;

    /**
     * The button to show certificate description.
     */
    JButton certButton;

    /**
     * The check box if checked permanently stored the certificate
     * which will be always trusted.
     */
    SIPCommCheckBox alwaysTrustCheckBox = new SIPCommCheckBox(
        R.getI18NString("service.gui.ALWAYS_TRUST"),
        false);

    /**
     * Whether the user trusts this certificate.
     */
    boolean isTrusted = false;

    /**
     * Creates the dialog.
     *
     * @param certs the certificates list
     * @param title The title of the dialog; when null the resource
     * <tt>service.gui.CERT_DIALOG_TITLE</tt> is loaded.
     * @param message A text that describes why the verification failed.
     */
    public VerifyCertificateDialogImpl(Certificate[] certs,
                                       String title, String message)
    {
        super(false);

        setTitle(title != null ? title :
            R.getI18NString("service.gui.CERT_DIALOG_TITLE"));
        setModal(true);

        this.certs = new ArrayList<X509Certificate>();
        for (Certificate certificate : certs)
        {
            if (certificate instanceof X509Certificate) {
                this.certs.add((X509Certificate) certificate);
            }
        }
        // for now shows only the first certificate from the chain for
        // non X.509 certificates
        this.cert = certs[0];
        this.message = message;

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        init();

        setLocationRelativeTo(getParent());
    }

    /**
     * Inits the dialog initial display.
     */
    private void init()
    {
        this.getContentPane().setLayout(new BorderLayout());

        contentPane =
            new TransparentPanel(new BorderLayout(5, 5));

        TransparentPanel northPanel =
            new TransparentPanel(new BorderLayout(5, 5));
        northPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));

        JLabel imgLabel = new JLabel(
            R.getImage("service.gui.icons.CERTIFICATE_WARNING"));
        imgLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        northPanel.add(imgLabel, BorderLayout.WEST);

        StyledHTMLEditorPane descriptionPane = new StyledHTMLEditorPane();
        descriptionPane.setOpaque(false);
        descriptionPane.setEditable(false);
        descriptionPane.setContentType("text/html");
        descriptionPane.setText(message);
        descriptionPane.setSize(
                    new Dimension(MAX_MSG_PANE_WIDTH, MAX_MSG_PANE_HEIGHT));
        int height = descriptionPane.getPreferredSize().height;
        descriptionPane.setPreferredSize(
                    new Dimension(MAX_MSG_PANE_WIDTH, height));

        northPanel.add(descriptionPane, BorderLayout.CENTER);
        contentPane.add(northPanel, BorderLayout.NORTH);

        certPanel = new TransparentPanel();
        contentPane.add(certPanel, BorderLayout.CENTER);

        TransparentPanel southPanel =
            new TransparentPanel(new BorderLayout());
        contentPane.add(southPanel, BorderLayout.SOUTH);

        certButton = new JButton();
        certButton.setText(R.getI18NString("service.gui.SHOW_CERT"));
        certButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                actionShowCertificate();
            }
        });
        TransparentPanel firstButonPanel =
            new TransparentPanel(new FlowLayout(FlowLayout.LEFT));
        firstButonPanel.add(certButton);
        southPanel.add(firstButonPanel, BorderLayout.WEST);

        TransparentPanel secondButonPanel =
            new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton(
            R.getI18NString("service.gui.CANCEL"));

        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                actionCancel();
            }
        });
        JButton continueButton = new JButton(
            R.getI18NString("service.gui.CONTINUE_ANYWAY"));

        continueButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                actionContinue();
            }
        });
        secondButonPanel.add(continueButton);
        secondButonPanel.add(cancelButton);
        southPanel.add(secondButonPanel, BorderLayout.EAST);

        this.getContentPane().add(contentPane, BorderLayout.CENTER);

        pack();
    }

    /**
     * Action when shoe certificate button is clicked.
     */
    private void actionShowCertificate()
    {
        if(certOpened)
        {
            certPanel.removeAll();
            certButton.setText(R.getI18NString("service.gui.SHOW_CERT"));

            certPanel.revalidate();
            certPanel.repaint();
            pack();
            certOpened = false;
            setLocationRelativeTo(getParent());
            return;
        }

        certPanel.setLayout(new BorderLayout(5, 5));
        certPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        certPanel.add(alwaysTrustCheckBox, BorderLayout.NORTH);

        Component certInfoPane = null;
        if (!certs.isEmpty())
        {
            certInfoPane = new X509CertificatePanel(certs);
        }
        else
        {
            JTextArea textArea = new JTextArea();
            textArea.setOpaque(false);
            textArea.setEditable(false);
            textArea.setText(cert.toString());

            final JScrollPane certScroll = new JScrollPane(certInfoPane);
            certScroll.setPreferredSize(new Dimension(300, 300));

            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    certScroll.getVerticalScrollBar().setValue(0);
                }
            });

            certInfoPane = certScroll;
        }
        certPanel.add(certInfoPane, BorderLayout.CENTER);

        certButton.setText(R.getI18NString("service.gui.HIDE_CERT"));

        certPanel.revalidate();
        certPanel.repaint();
        // restore default values for prefered size,
        // as we have resized its components let it calculate
        // that size
        setPreferredSize(null);
        pack();
        certOpened = true;
        setLocationRelativeTo(getParent());
    }

    /**
     * Action when cancel button is clicked.
     */
    private void actionCancel()
    {
        isTrusted = false;
        dispose();
    }

    /**
     * Action when continue is clicked.
     */
    private void actionContinue()
    {
        isTrusted = true;
        dispose();
    }

    /**
     * Called when dialog closed or escape pressed.
     * @param isEscaped is escape button pressed.
     */
    @Override
    protected void close(boolean isEscaped)
    {
        actionCancel();
    }

    /**
     * Whether the user has accepted the certificate or not.
     * @return whether the user has accepted the certificate or not.
     */
    public boolean isTrusted()
    {
        return this.isTrusted;
    }

    /**
     * Whether the user has selected to note the certificate so we always
     * trust it.
     * @return whether the user has selected to note the certificate so
     * we always trust it.
     */
    public boolean isAlwaysTrustSelected()
    {
        return this.alwaysTrustCheckBox.isSelected();
    }

    /**
     * Shows or hides the dialog and waits for user response.
     * @param isVisible whether we should show or hide the dialog.
     */
    @Override
    public void setVisible(boolean isVisible)
    {
        try
        {
            // show the dialog in the swing thread and wait for the user
            // choice
            SwingUtilities.invokeAndWait(new Runnable()
            {
                public void run()
                {
                    VerifyCertificateDialogImpl.super.setVisible(true);
                }
            });
        }
        catch (Exception e)
        {
            logger.error("Cannot show certificate verification dialog", e);
        }
    }
}
