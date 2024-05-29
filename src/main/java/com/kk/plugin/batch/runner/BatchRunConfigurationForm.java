package com.kk.plugin.batch.runner;

import com.intellij.ide.macro.MacrosDialog;
import com.intellij.ide.util.BrowseFilesListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.JBUI;
import com.kk.plugin.batch.util.BatchBundle;

import javax.swing.*;
import java.awt.*;

/**
 * @author wibotwi
 */
public class BatchRunConfigurationForm implements BatchRunConfigurationParams {
    private TextFieldWithBrowseButton scriptNameField;
    private RawCommandLineEditor scriptParametersField;
    private JPanel commonOptionsPlaceholder;
    private JPanel rootPanel;
    private BatchCommonOptionsForm commonOptionsForm;

    public BatchRunConfigurationForm(BatchRunConfiguration runConfiguration) {
        commonOptionsForm = new BatchCommonOptionsForm(runConfiguration);
        commonOptionsPlaceholder.add(commonOptionsForm.getRootPanel(), BorderLayout.CENTER);

        scriptNameField.addBrowseFolderListener("Select Script", "", runConfiguration.getProject(), BrowseFilesListener.SINGLE_FILE_DESCRIPTOR);
        MacrosDialog.addTextFieldExtension((ExtendableTextField)scriptParametersField.getTextField());
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    public CommonBatchRunConfigurationParams getCommonParams() {
        return commonOptionsForm;
    }

    public String getScriptName() {
        return FileUtil.toSystemIndependentName(scriptNameField.getText().trim());
    }

    public void setScriptName(String scriptName) {
        scriptNameField.setText(scriptName);
    }

    public String getScriptParameters() {
        return scriptParametersField.getText().trim();
    }

    public void setScriptParameters(String scriptParameters) {
        scriptParametersField.setText(scriptParameters);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(4, 2, JBUI.emptyInsets(), -1, -1));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, BatchBundle.message("runcfg.labels.script"));
        rootPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, BatchBundle.message("runcfg.labels.script_parameters"));
        rootPanel.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        commonOptionsPlaceholder = new JPanel();
        commonOptionsPlaceholder.setLayout(new BorderLayout(0, 0));
        rootPanel.add(commonOptionsPlaceholder, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        scriptParametersField = new RawCommandLineEditor();
        scriptParametersField.setDialogCaption(BatchBundle.message("runcfg.captions.script_parameters_dialog"));
        rootPanel.add(scriptParametersField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), null, null, 0, false));
        scriptNameField = new TextFieldWithBrowseButton();
        rootPanel.add(scriptNameField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        rootPanel.add(spacer1, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }
}
