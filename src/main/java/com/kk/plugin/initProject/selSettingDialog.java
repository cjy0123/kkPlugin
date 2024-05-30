package com.kk.plugin.initProject;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.kk.plugin.util.GoPluginUtil;
import com.kk.plugin.util.TextConsoleView;
import com.kk.plugin.util.ToolUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import static com.kk.plugin.proto.ProtoActionManager.generateCommandLine;

public class selSettingDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextPane textArea;
    private JLabel ipLabel;
    private final Map<String, Object> parseInfoMap;


    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }

    public selSettingDialog(Map<String, Object> parseInfoMap) {
        super(true);
        init();
        setTitle("初始化配置");
        setSize(800, 400);
        centerRelativeToParent();

        setOKButtonText("应用");
        setCancelButtonText("取消");
        ipLabel.setText("本机ip：" + getIPAddress());

        this.parseInfoMap = parseInfoMap;
        VirtualFile tempCfg, jsonCfg = null;
        tempCfg = (VirtualFile) parseInfoMap.get("cfg");
        if (parseInfoMap.get("json") != null) {
            jsonCfg = (VirtualFile) parseInfoMap.get("json");
        }
        loadFileContent(tempCfg, jsonCfg);
        // 遇到 ESCAPE 时调用 onCancel()
        contentPane.registerKeyboardAction(e -> doCancelAction(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    public void doOKAction() {
        PsiDirectory baseDir = (PsiDirectory) parseInfoMap.get("baseDir");
        String filePath = baseDir.getVirtualFile().getPath() + "/selfCfg.json";
        saveToFile(filePath, textArea.getText());
        Project project = (Project) parseInfoMap.get("project");
        TextConsoleView consoleView = GoPluginUtil.getConsoleView(project);
        VirtualFile exeFile = (VirtualFile) parseInfoMap.get("exe");
        GeneralCommandLine commandLine = generateCommandLine(parseInfoMap, exeFile.getName(), project, consoleView);
        ToolUtil.execCmd(project, consoleView, commandLine);
        super.doOKAction();
    }

    private void loadFileContent(VirtualFile tempFile, VirtualFile jsonFile) {
        try {
            byte[] tempBytes = tempFile.contentsToByteArray();
            String tempContent = new String(tempBytes);
            String jsonContent;
            if (jsonFile == null) {
                jsonContent = "{}";
            } else {
                byte[] jsonBytes = jsonFile.contentsToByteArray();
                jsonContent = new String(jsonBytes);
            }
            compareJsonKeys(tempContent, jsonContent);
        } catch (IOException e) {
            e.printStackTrace();
            textArea.setText("读取配置错误: " + e.getMessage());
        }
    }

    private void saveToFile(String filePath, String content) {
        try {
            File file = new File(filePath);
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "写文件错误: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getIPAddress() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "Unknown";
        }
    }

    private void compareJsonKeys(String tempContent, String jsonContent) {
        try {
            JsonElement tempJsonElement = JsonParser.parseString(tempContent);
            JsonElement jsonJsonElement = JsonParser.parseString(jsonContent);

            if (tempJsonElement.isJsonObject() && jsonJsonElement.isJsonObject()) {
                JsonObject tempJsonObject = tempJsonElement.getAsJsonObject();
                JsonObject jsonJsonObject = jsonJsonElement.getAsJsonObject();

                StyledDocument doc = textArea.getStyledDocument();
                StyleContext context = new StyleContext();
                Style defaultStyle = context.addStyle("defaultStyle", null);
                Style diffStyle = context.addStyle("diffStyle", null);
                StyleConstants.setBackground(diffStyle, Color.RED);

                textArea.setText("");

                for (Map.Entry<String, JsonElement> entry : tempJsonObject.entrySet()) {
                    String key = entry.getKey();
                    if (!jsonJsonObject.has(key)) {
                        String line = "  \"" + key + "\": " + entry.getValue() + ",\n";
                        doc.insertString(doc.getLength(), line, diffStyle);
                    } else {
                        String line = "  \"" + key + "\": " + jsonJsonObject.get(key) + ",\n";
                        doc.insertString(doc.getLength(), line, defaultStyle);
                    }
                }
                doc.remove(doc.getLength()-2,1);
                doc.insertString(0, "{\n", defaultStyle);
                doc.insertString(doc.getLength(), "}", defaultStyle);
            } else {
                textArea.setText("JSON格式错误或不是对象");
            }
        } catch (JsonSyntaxException | BadLocationException e) {
            e.printStackTrace();
            textArea.setText("解析JSON错误: " + e.getMessage());
        }
    }
}
