/*
 * Copyright (c) 2014 Swen Walkowski.
 * All rights reserved. Originator: Swen Walkowski.
 * Get more information about CardDAVSyncOutlook at https://github.com/somedevelopment/CardDAVSyncOutlook/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package ui;

import com.alee.extended.label.WebLinkLabel;
import com.alee.laf.WebLookAndFeel;
import com.alee.laf.button.WebButton;
import com.alee.laf.checkbox.WebCheckBox;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebMenu;
import com.alee.laf.menu.WebMenuBar;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.rootpane.WebDialog;
import com.alee.laf.rootpane.WebFrame;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.separator.WebSeparator;
import com.alee.laf.text.WebPasswordField;
import com.alee.laf.text.WebTextField;
import com.alee.laf.text.WebTextPane;
import com.alee.managers.tooltip.TooltipManager;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyledDocument;
import main.FocusTraversalOnArray;
import main.Main;
import main.Status;
import utilities.Config;

//TODO split up user interface for Outlook, DAV and general configuration information
public class Userinterface {

    private final static String RES_PATH = "res";
    private final static String DEFAULT_CONTACT_FOLDER = "Default";

    private Main control;

    private WebFrame frame;
    private final WebPasswordField passwordField;
    private final WebTextField usernameField;
    private final WebTextField urlField;
    private final WebCheckBox insecureSSLBox;
    private final WebLabel lblContactNumbers;
    private final WebCheckBox savePasswordBox;
    private final WebCheckBox initModeBox;

    static private WebTextPane textPane;
    static private StyledDocument docTextPane;
    static private WebComboBox contactFolderBox;

    /**
     * Create the application.
     */
    public Userinterface(final Main main) {

        control = main;

        WebLookAndFeel.install();

        frame = new WebFrame();
        frame.setBounds(100, 100, 630, 430);
        frame.setResizable(false);
        frame.setTitle("CardDAVSyncOutlook");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Userinterface.this.shutdown();
            }
        });

        // menu
        WebMenuBar menubar = new WebMenuBar();

        WebMenu fileMenu = new WebMenu("File");
        WebMenuItem exitMenuItem = new WebMenuItem("Exit");
        exitMenuItem.setToolTipText("Exit application");
        exitMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
        });
        fileMenu.add(exitMenuItem);
        menubar.add(fileMenu);

        WebMenu extrasMenu = new WebMenu("Extras");
        WebMenuItem exportICALMenuItem = new WebMenuItem("Export calendar");
        exportICALMenuItem.setToolTipText("Export Outlook calendar to iCAL file (to date - one month)");
        exportICALMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                control.exportICAL();
            }
        });
        extrasMenu.add(exportICALMenuItem);
        WebMenuItem prefMenuItem = new WebMenuItem("Preferences");
        prefMenuItem.setToolTipText("Global Preferences");
        prefMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PreferencesDialog pref = new PreferencesDialog();
                pref.setVisible(true);
            }
        });
        extrasMenu.add(prefMenuItem);
        menubar.add(extrasMenu);

        WebMenu helpMenu = new WebMenu("Help");
        WebMenuItem aboutMenuItem = new WebMenuItem("About");
        aboutMenuItem.setToolTipText("About...");
        aboutMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                WebPanel aboutPanel = new WebPanel();
                aboutPanel.add(new WebLabel("CardDAVSyncOutlook "+Main.VERSION));
                WebLinkLabel linkLabel = new WebLinkLabel();
                linkLabel.setLink("https://github.com/somedevelopment/CardDAVSyncOutlook/");
                linkLabel.setText("Visit the developer site");
                aboutPanel.add(linkLabel, BorderLayout.SOUTH);
                WebOptionPane.showMessageDialog(frame,
                        aboutPanel,
                        "About",
                        WebOptionPane.INFORMATION_MESSAGE);
            }
        });
        helpMenu.add(aboutMenuItem);
        menubar.add(helpMenu);

        frame.setJMenuBar(menubar);

        // ui components and layout:...
        WebPanel northPanel = new WebPanel();
        northPanel.setBorderColor(Color.LIGHT_GRAY);
        northPanel.setMargin(new Insets(0, 5, 0, 5));
        northPanel.setLayout(new GridLayout(0, 1, 0, 0));

        // ...URL...
        WebLabel lblHost = new WebLabel("CardDAV addressbook URL: ");
        lblHost.setVerticalAlignment(SwingConstants.BOTTOM);
        lblHost.setMargin(new Insets(0, 3, 0, 0));
        lblHost.setFont(new Font("Calibri", Font.BOLD, 12));
        northPanel.add(lblHost);

        urlField = new WebTextField();
       // urlField.addMouseListener(new MouseAdapter() {
               //@Override
               //public void mouseExited(MouseEvent arg0) {
               //	if (urlField.getText().trim().startsWith("https://")) {
               //		insecureSSLBox.setSelected(true);
               //		urlField.setText("http" + urlField.getText().substring(5));
               //		Status.print("Activated insecure SSL");
               //	}
               //}
       // });
        urlField.setFont(new Font("Calibri", Font.PLAIN, 12));
        //textHostURL.setColumns(10)
        urlField.setInputPrompt("http://<server-name>/owncloud/remote.php/carddav/addressbooks/<user_name>/<addr_book_name>");
        urlField.setHideInputPromptOnFocus(false);
        northPanel.add(urlField);

        // ...account: credentials and login options...
        WebPanel accountPanel = new WebPanel();
        accountPanel.setLayout(new FlowLayout(FlowLayout.LEADING));

        WebLabel lblUsername = new WebLabel("Username:");
        lblUsername.setFont(new Font("Calibri", Font.BOLD, 12));
        accountPanel.add(lblUsername);

        usernameField = new WebTextField();
        usernameField.setFont(new Font("Calibri", Font.PLAIN, 12));
        usernameField.setColumns(10);
        accountPanel.add(usernameField);

        accountPanel.add(new WebSeparator());

        WebLabel lblPassword = new WebLabel("Password:");
        lblPassword.setFont(new Font("Calibri", Font.BOLD, 12));
        accountPanel.add(lblPassword);

        passwordField = new WebPasswordField();
        passwordField.setColumns(10);
        passwordField.setFont(new Font("Calibri", Font.PLAIN, 11));
        passwordField.setEchoChar('*');
        accountPanel.add(passwordField);

        accountPanel.add(new WebSeparator());

        savePasswordBox = new WebCheckBox("Save Password");
        savePasswordBox.setFont(new Font("Calibri", Font.BOLD, 12));
        String tooltipText = "Save the password in configuration file as plaintext(!)";
        TooltipManager.addTooltip(savePasswordBox, tooltipText);
        accountPanel.add(savePasswordBox);

        accountPanel.add(new WebSeparator());

        insecureSSLBox = new WebCheckBox("Allow insecure SSL");
        insecureSSLBox.setFont(new Font("Calibri", Font.BOLD, 12));
        tooltipText = "Do not check the SSL certificate. Needed when the server uses a self-signed certificate";
        TooltipManager.addTooltip(insecureSSLBox, tooltipText);
        accountPanel.add(insecureSSLBox);

        northPanel.add(accountPanel);

        // ...outlook contact folder...
        WebPanel contactFolderPanel = new WebPanel();
        contactFolderPanel.setLayout(new FlowLayout(FlowLayout.LEADING));

        WebLabel contactFolderLabel = new WebLabel("Outlook Folder: ");
        contactFolderLabel.setFont(new Font("Calibri", Font.BOLD, 12));
        contactFolderPanel.add(contactFolderLabel);

        contactFolderBox = new WebComboBox();
        tooltipText = "The Outlook Contact Folder to sync with";
        TooltipManager.addTooltip(contactFolderBox, tooltipText);
        contactFolderBox.addItem(DEFAULT_CONTACT_FOLDER);
        contactFolderPanel.add(contactFolderBox);

        contactFolderPanel.add(new WebSeparator());

        WebButton listContactFolderButton = new WebButton("Get list");
        listContactFolderButton.setFont(new Font("Calibri", Font.BOLD, 12));
        tooltipText = "Get a list of all Outlook Contact Folders";
        TooltipManager.addTooltip(listContactFolderButton, tooltipText);
        listContactFolderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                control.listContactFolders();
            }
        });
        contactFolderPanel.add(listContactFolderButton);

        northPanel.add(contactFolderPanel);

        // ...sync button and sync options...
        WebPanel runPanel = new WebPanel();

        WebButton btnSync = new WebButton("Start Synchronization");
        btnSync.setFont(new Font("Calibri", Font.BOLD, 12));
        btnSync.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Userinterface.this.callSync();
            }
        });
        runPanel.add(btnSync, BorderLayout.CENTER);

        initModeBox = new WebCheckBox("Initialization Mode");
        initModeBox.setFont(new Font("Calibri", Font.BOLD, 12));
        tooltipText = "Compare contacts by all fields. Useful on the first run";
        TooltipManager.addTooltip(initModeBox, tooltipText);
        runPanel.add(initModeBox, BorderLayout.EAST);

        northPanel.add(runPanel);

        frame.getContentPane().add(northPanel, BorderLayout.NORTH);

        // ... status text pane...
        textPane = new WebTextPane();
        textPane.setFont(new Font("Calibri", Font.PLAIN, 12));
        textPane.setEditable(false);
        docTextPane = textPane.getStyledDocument();

        WebScrollPane scrollPane = new WebScrollPane(textPane);
        scrollPane.setDarkBorder(Color.LIGHT_GRAY);
        scrollPane.setBorderColor(Color.LIGHT_GRAY);

        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        //textPane.getCaret().
        DefaultCaret caret = (DefaultCaret) textPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        // ...number of contacts.
        WebPanel southPanel = new WebPanel();
        frame.getContentPane().add(southPanel, BorderLayout.SOUTH);
        southPanel.setLayout(new FlowLayout(FlowLayout.LEADING));

        WebLabel lblNumbersOfContacts = new WebLabel("# of loaded Contacts:");
        southPanel.add(lblNumbersOfContacts);
        lblNumbersOfContacts.setFont(new Font("Calibri", Font.BOLD, 12));

        southPanel.add(new WebSeparator());

        lblContactNumbers = new WebLabel("");
        southPanel.add(lblContactNumbers);
        lblContactNumbers.setFont(new Font("Calibri", Font.PLAIN, 12));

        frame.getContentPane().setFocusTraversalPolicy(
                new FocusTraversalOnArray(
                        new Component[]{
                            usernameField,
                            passwordField,
                            urlField,
                            btnSync,
                            textPane,
                            scrollPane,
                            lblHost,
                            lblPassword,
                            lblUsername
                        }
                )
        );
        frame.setFocusTraversalPolicy(
                new FocusTraversalOnArray(
                        new Component[]{
                            usernameField,
                            passwordField,
                            urlField,
                            btnSync,
                            textPane,
                            lblHost,
                            lblPassword,
                            frame.getContentPane(),
                            scrollPane,
                            lblUsername
                        }
                )
        );

        //Load config
        Status.print("Load Config");
        Config config = Config.getInstance();
        usernameField.setText(config.getString(Config.ACC_USER, ""));
        passwordField.setText(config.getString(Config.ACC_PASS, ""));
        urlField.setText(config.getString(Config.ACC_URL, ""));
        insecureSSLBox.setSelected(config.getBoolean(Config.ACC_SSL, false));
        savePasswordBox.setSelected(config.getBoolean(Config.ACC_SAVE_PASS, false));
        String savedContactFolder = config.getString(Config.ACC_OUTLOOK_FOLDER, "");
        if (!savedContactFolder.isEmpty()) {
            contactFolderBox.addItem(savedContactFolder);
            contactFolderBox.setSelectedItem(savedContactFolder);
        }

        this.setTray();
    }

    public void setVisible() {
        frame.setVisible(true);
    }

    public void setContactNumbers(String text) {
        lblContactNumbers.setText(text);
    }

    public void runAndShutDown() {
        this.callSync();
        this.shutdown();
    }

    private void callSync() {
        // options from gui components
        String url = urlField.getText().trim();
        String username = usernameField.getText().trim();
        String password = String.valueOf(passwordField.getPassword()).trim();
        String outlookFolder = (String) contactFolderBox.getSelectedItem();
        if (outlookFolder.equals(DEFAULT_CONTACT_FOLDER))
            outlookFolder = "";
        boolean insecureSSL = insecureSSLBox.isSelected();
        boolean initMode = initModeBox.isSelected();

        // options from config
        Config config = Config.getInstance();
        boolean closeOutlook = config.getBoolean(Config.GLOB_CLOSE, false);
        boolean clearNumbers = config.getBoolean(Config.GLOB_CORRECT_NUMBERS, false);
        String region = config.getString(Config.GLOB_REGION_CODE, "");

        control.syncContacts(url,
                clearNumbers,
                region,
                username,
                password,
                outlookFolder,
                insecureSSL,
                closeOutlook,
                initMode);

        // TODO, for testing a appointments sync
        //control.syncAppointments();
    }

    private void saveConfig() {
        Config config = Config.getInstance();
        config.setProperty(Config.ACC_USER, usernameField.getText());
        if (savePasswordBox.isSelected())
            config.setProperty(Config.ACC_PASS, new String(passwordField.getPassword()));
        config.setProperty(Config.ACC_URL, urlField.getText());
        config.setProperty(Config.ACC_SSL, insecureSSLBox.isSelected());
        config.setProperty(Config.ACC_SAVE_PASS, savePasswordBox.isSelected());
        String outlookFolder = "";
        if (contactFolderBox.getSelectedIndex() != 0)
            outlookFolder = (String) contactFolderBox.getSelectedItem();
        config.setProperty(Config.ACC_OUTLOOK_FOLDER, outlookFolder);
        config.saveToFile();
        Status.print("Config Saved");
    }

    private void toggleState() {
        if (frame.getState() == Frame.NORMAL) {
            frame.setState(Frame.ICONIFIED);
            frame.setVisible(false);
        } else {
            frame.setState(Frame.NORMAL);
            frame.setVisible(true);
            frame.toFront();
        }
    }

    void shutdown() {
        this.saveConfig();
        frame.setVisible(false);
        frame.dispose();
        control.shutdown();
    }

    private void setTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("tray icon not supported");
            return;
        }

        // load image
        Image image = getImage("dav_sync_outlook.png");

        final WebPopupMenu popup = new WebPopupMenu();
        WebMenuItem syncItem = new WebMenuItem("Sync");
        syncItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Userinterface.this.callSync();
            }
        });
        popup.add(syncItem);
        WebMenuItem quitItem = new WebMenuItem("Quit");
        quitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Userinterface.this.shutdown();
            }
        });
        popup.add(quitItem);

        // workaround: hidden dialog, so that the popup menu disappears when
        // focus is lost
        final WebDialog hiddenDialog = new WebDialog ();
        hiddenDialog.setUndecorated(true);

        // create an action listener to listen for default action executed on the tray icon
        MouseListener listener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                check(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1)
                    Userinterface.this.toggleState();
                else
                    check(e);
            }
            private void check(MouseEvent e) {
                if (!e.isPopupTrigger())
                    return;

                hiddenDialog.setVisible(true);

                // TODO ugly
                // Weblaf 1.28 doesn't support popups outside of a frame, this
                // is a workaround
                popup.setLocation(e.getX() - 20, e.getY() - 40);
                // this is wrong, but a TrayIcon is not a component
                popup.setInvoker(hiddenDialog);
                popup.setCornerWidth(0);
                popup.setVisible(true);
            }
        };

        TrayIcon trayIcon = new TrayIcon(image, "CardDAVSyncOutlook" /*, popup*/);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(listener);

        SystemTray tray = SystemTray.getSystemTray();
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public static void resetTextPane() {
        textPane.setText("");
    }

    public static void setTextInTextPane(String strText) {
        try {
            if (docTextPane.getLength() > 0) {
                docTextPane.insertString(docTextPane.getLength(), strText + "\n", null);
            } else {
                docTextPane.insertString(0, strText + "\n", null);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public static void setContactFolderItems(List<String> contactFolders) {
        contactFolderBox.removeAllItems();
        contactFolderBox.addItem(DEFAULT_CONTACT_FOLDER);
        for (String folder : contactFolders)
            contactFolderBox.addItem(folder);
    }

    static Image getImage(String fileName) {
        URL imageUrl = ClassLoader.getSystemResource(RES_PATH + "/" + fileName);
        if (imageUrl == null) {
            System.out.println("can't find image resource");
            return new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        }
        return Toolkit.getDefaultToolkit().createImage(imageUrl);
    }
}
