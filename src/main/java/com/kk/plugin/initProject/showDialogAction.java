package com.kk.plugin.initProject;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.kk.plugin.proto.ProtoConst;
import com.kk.plugin.util.ToolUtil;

import java.util.HashMap;
import java.util.Map;

public class showDialogAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        Map<String, Object> parseInfoMap = findTempConfig(project);
        if (parseInfoMap.get("cfg") != null && parseInfoMap.get("cfg") != null) {
            selSettingDialog dialog = new selSettingDialog(parseInfoMap);
            dialog.showAndGet();
        }
    }

    private Map<String, Object> findTempConfig(Project project) {
        VirtualFile selfCfg = null, exeFile = null, selfCfgJson = null;
        Map<String, Object> resultMap = new HashMap<>();
        PsiDirectory baseDir = null;
        VirtualFile projectDir = ToolUtil.findProjectPath(project, ProtoConst.SERVER_PROTO_FOLDER_NAME, 2);
        if (projectDir != null) {
            String selfCfgJsonName = "selfCfg.json";
            String tempConfigName = "selfCfg.temp";
            String exeName = "updateSetting.exe";

            VirtualFile toolsFolder = ToolUtil.findTargetFile(projectDir, "tools", 1);
            if (toolsFolder != null) {
                baseDir = PsiManager.getInstance(project).findDirectory(toolsFolder);
                for (VirtualFile file : toolsFolder.getChildren()) {
                    if (file.getName().equals(selfCfgJsonName)) {
                        selfCfgJson = file;
                    }
                    if (file.getName().equals(tempConfigName)) {
                        selfCfg = file;
                    }
                    if (file.getName().equals(exeName)) {
                        exeFile = file;
                    }
                }
                if (selfCfg == null || exeFile == null) {
                    Messages.showMessageDialog(project, "找不到配置文件", "错误", Messages.getInformationIcon());
                    return resultMap;
                }
            }
        }
        resultMap.put("json", selfCfgJson);
        resultMap.put("cfg", selfCfg);
        resultMap.put("exe", exeFile);
        resultMap.put("project", project);
        resultMap.put("baseDir", baseDir);
        return resultMap;
    }
}
