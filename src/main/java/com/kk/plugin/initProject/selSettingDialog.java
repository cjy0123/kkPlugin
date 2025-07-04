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
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Map;

import static com.kk.plugin.proto.ProtoActionManager.generateCommandLine;

public class selSettingDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextPane textArea;
    private JLabel ipLabel;
    private JTextField ipField;
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
        ipField.setText(getIPAddress());

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
//        try {
//            InetAddress localHost = InetAddress.getLocalHost();
//            return localHost.getHostAddress();
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//            return "Unknown";
//        }
        StringBuilder ipString = new StringBuilder();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                // 过滤掉回环接口和未启用的接口
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual())
                    continue;

                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    // 只考虑IPv4地址
                    if (!inetAddress.isSiteLocalAddress() || inetAddress.getHostAddress().indexOf(':') > -1)
                        continue;

                    ipString.append(inetAddress.getHostAddress());
                    ipString.append(",");
//                    System.out.println("Main network interface IP: " + inetAddress.getHostAddress());
//                    break; // 找到第一个IPv4地址后就退出
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ipString.toString();
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

                String[] ignoreKeys = {"project", "host", "openDate"};
                java.util.Set<String> ignoreSet = new java.util.HashSet<>();
                for (String k : ignoreKeys) ignoreSet.add(k);

                // 先将 jsonJsonObject 拷贝一份用于最终合成
                JsonObject mergedJson = jsonJsonObject.deepCopy();

                for (Map.Entry<String, JsonElement> entry : tempJsonObject.entrySet()) {
                    String key = entry.getKey();
                    if (ignoreSet.contains(key)) {
                        // 如果 jsonJsonObject 没有该 key，也要高亮显示并添加进去
                        if (!jsonJsonObject.has(key)) {
                            String line = "  \"" + key + "\": " + entry.getValue() + ",\n";
                            doc.insertString(doc.getLength(), line, diffStyle);
                            mergedJson.add(key, entry.getValue());
                        } else {
                            String line = "  \"" + key + "\": " + jsonJsonObject.get(key) + ",\n";
                            doc.insertString(doc.getLength(), line, defaultStyle);
                        }
                        continue;
                    }
                    if (!jsonJsonObject.has(key) || !entry.getValue().equals(jsonJsonObject.get(key))) {
                        // 差异项高亮，并合并到 mergedJson
                        String line = "  \"" + key + "\": " + entry.getValue() + ",\n";
                        doc.insertString(doc.getLength(), line, diffStyle);
                        mergedJson.add(key, entry.getValue());
                    } else {
                        String line = "  \"" + key + "\": " + jsonJsonObject.get(key) + ",\n";
                        doc.insertString(doc.getLength(), line, defaultStyle);
                    }
                }
                // 处理 jsonJsonObject 中有但 tempJsonObject 没有的 key
                for (Map.Entry<String, JsonElement> entry : jsonJsonObject.entrySet()) {
                    String key = entry.getKey();
                    if (!tempJsonObject.has(key)) {
                        String line = "  \"" + key + "\": " + entry.getValue() + ",\n";
                        doc.insertString(doc.getLength(), line, defaultStyle);
                    }
                }
                // 去掉最后一个逗号
                if (doc.getLength() > 2) {
                    doc.remove(doc.getLength()-2,1);
                }
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
