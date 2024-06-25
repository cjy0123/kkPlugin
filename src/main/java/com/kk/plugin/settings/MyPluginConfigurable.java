package com.kk.plugin.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class MyPluginConfigurable implements Configurable {
    private com.intellij.openapi.ui.TextFieldWithBrowseButton configTxt;
    private final Project project;
    private JPanel settingPanel;

    public MyPluginConfigurable(Project project) {
        this.project = project;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "My Plugin Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        String configManagerFile = MyPluginSettings.getInstance(project).getConfigManagerTxt();
        configTxt.setText(configManagerFile);
        configTxt.addActionListener(e -> {
            // 创建文件选择器描述符
            FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false);

            // 获取文件选择对话框工厂
            FileChooserDialog fileChooserDialog = FileChooserFactory.getInstance().createFileChooser(descriptor, project, null);

            // 显示文件选择对话框
            VirtualFile[] selectedFiles = fileChooserDialog.choose(null, project);
            if (selectedFiles.length > 0) {
                // 获取选中的文件路径，并填入 TextField
                String selectedFilePath = selectedFiles[0].getPath();
                configTxt.setText(selectedFilePath);
            }
        });
        return settingPanel;
    }

    @Override
    public boolean isModified() {
        return !configTxt.getText().equals(MyPluginSettings.getInstance(project).getConfigManagerTxt());
    }

    @Override
    public void apply() {
        MyPluginSettings.getInstance(project).setConfigManagerTxt(configTxt.getText());
    }

    @Override
    public void reset() {
        configTxt.setText(MyPluginSettings.getInstance(project).getConfigManagerTxt());
    }
}
