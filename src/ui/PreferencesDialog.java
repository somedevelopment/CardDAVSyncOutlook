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

import com.alee.extended.panel.GroupPanel;
import com.alee.laf.button.WebButton;
import com.alee.laf.checkbox.WebCheckBox;
import com.alee.laf.rootpane.WebDialog;
import com.alee.managers.tooltip.TooltipManager;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import utilities.Config;

/**
 * Preferences dialog for various configuration options
 * @author Alexander Bikadorov <abiku@cs.tu-berlin.de>
 */
public class PreferencesDialog extends WebDialog {



    PreferencesDialog() {
        this.setTitle("Preferences");
        this.setSize(330, 200);
        this.setResizable(false);
        this.setModal(true);
        this.setLayout(new BorderLayout(10, 10));

        final Config config = Config.getInstance();
        GroupPanel prefPanel = new GroupPanel(10, false);

        // preferences
        final WebCheckBox outlookCheckBox = new WebCheckBox("Close Outlook?");
        outlookCheckBox.setText("Close Outlook?");
        outlookCheckBox.setFont(new Font("Calibri", Font.BOLD, 12));
        String tooltipText = "Close Outlook after synchronization is finished.";
        TooltipManager.addTooltip(outlookCheckBox, tooltipText);
        outlookCheckBox.setSelected(config.getBoolean(Config.GLOB_CLOSE, false));
        prefPanel.add(outlookCheckBox);

        this.add(prefPanel, BorderLayout.CENTER);

        // buttons
        WebButton cancelButton = new WebButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PreferencesDialog.this.dispose();
            }
        });
        WebButton saveButton = new WebButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setProperty(Config.GLOB_CLOSE, outlookCheckBox.isSelected());
                PreferencesDialog.this.dispose();
            }
        });

        GroupPanel buttonPanel = new GroupPanel(2, cancelButton, saveButton);
        buttonPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
        this.add(buttonPanel, BorderLayout.SOUTH);
    }

}

