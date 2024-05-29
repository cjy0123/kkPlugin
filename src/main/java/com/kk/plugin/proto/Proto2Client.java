package com.kk.plugin.proto;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.kk.plugin.util.GoPluginUtil;
import com.kk.plugin.util.TextConsoleView;
import com.kk.plugin.util.ToolUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * proto文件生成go文件
 * 1 单个或者多个proto文件
 * 2 proto目录
 */
public class Proto2Client extends ProtoActionManager {
    /**
     * 1  选择proto目录  psiElement、psiFiles 都有值
     * 2  选择单个proto文件  psiElement、psiFiles 都有值
     * 3  选择多个文件psiElement无， psiFiles有值
     */
    @Override
    public void actionPerformed(AnActionEvent e) {
        @Nullable Project project = e.getProject();
        if (project == null) {
            return;
        }
        Map<String, Object> parseInfoMap = parseActionInfo(e, project, ProtoConst.CLIENT_PROTO_FOLDER_NAME);
        int eventCase = (int) parseInfoMap.get(KEY_EVENT_CASE);
        TextConsoleView consoleView = GoPluginUtil.getConsoleView(project);
        if (eventCase == ProtoConst.EVENT_CASE_DEFAULT) {
            GoPluginUtil.printlnConsoleAndShowMessage(project, consoleView,
                    ConsoleViewContentType.LOG_ERROR_OUTPUT,
                    "生成失败", "未找到相关目录!");
            return;
        }

        // 生成命令行对象
        String pb2goBatName = "runProtocClient.bat";
        GeneralCommandLine commandLine = generateCommandLine(parseInfoMap, pb2goBatName,project,consoleView);
        ToolUtil.execCmd(project, consoleView, commandLine);
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        if (!ActionPlaces.EDITOR_POPUP.equals(e.getPlace())
                && !ActionPlaces.PROJECT_VIEW_POPUP.equals(e.getPlace())) {//只有在点击右键时才做过滤
            return;
        }

        Map<String, Object> parseInfoMap = parseActionInfo(e, project, ProtoConst.CLIENT_PROTO_FOLDER_NAME);
        int eventCase = (int) parseInfoMap.get(KEY_EVENT_CASE);
        if (eventCase == ProtoConst.EVENT_CASE_DEFAULT) {
            e.getPresentation().setEnabledAndVisible(false);
        }

    }

    public Map<String, Object> parseActionInfo(AnActionEvent e, Project project, String folderName) {
        if (!ActionPlaces.EDITOR_POPUP.equals(e.getPlace())
                && !ActionPlaces.PROJECT_VIEW_POPUP.equals(e.getPlace())) {//只有在点击右键时才做过滤
            return parseToolActionInfo(project, folderName);
        }

        String pb2goBatName = "runProtocClient.bat";
        //在Action显示之前,根据选中文件扩展名判定是否显示此Action
        PsiElement psiElement = PlatformDataKeys.PSI_ELEMENT.getData(e.getDataContext());
        int eventCase = ProtoConst.EVENT_CASE_DEFAULT;
        PsiDirectory baseDir = null;

        if (psiElement != null) {
            if (psiElement instanceof PsiDirectory psiDirectory) {
                String name = psiDirectory.getName();
                if (name.equals(folderName)) {
                    boolean hasFile = false;
                    for (PsiFile file : psiDirectory.getFiles()) {
                        if (file.getName().equals(pb2goBatName)) {
                            hasFile = true;
                            break;
                        }
                    }
                    if (hasFile) {
                        eventCase = ProtoConst.EVENT_CASE_FOLDER;
                        baseDir = psiDirectory;
                    }
                }
            } else if (psiElement instanceof PsiFile psiFile && ((PsiFile) psiElement).getName().equals(pb2goBatName)) {
                if (psiFile.getParent() != null && psiFile.getParent().getName().equals(folderName)) {
                    eventCase = ProtoConst.EVENT_CASE_SINGLE_FILE;
                    baseDir = psiFile.getParent();
                }
            }
        }

        // 支持右键选择文件
        if (eventCase == ProtoConst.EVENT_CASE_DEFAULT) {
            PsiFile psiFile = PlatformDataKeys.PSI_FILE.getData(e.getDataContext());
            if (psiFile != null && psiFile.getParent() != null && psiFile.getName().equals(pb2goBatName)) {
                if (psiFile.getParent().getName().equals(folderName)) {
                    eventCase = ProtoConst.EVENT_CASE_SINGLE_FILE;
                    baseDir = psiFile.getParent();
                }
            }
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(KEY_EVENT_CASE, eventCase);
        resultMap.put(KEY_BASE_DIR, baseDir);

        return resultMap;
    }

    public Map<String, Object> parseToolActionInfo(Project project, String folderName) {
        int eventCase = ProtoConst.EVENT_CASE_DEFAULT;
        PsiDirectory baseDir = null;

        VirtualFile projectDir = ToolUtil.findProjectPath(project, ProtoConst.CLIENT_PROTO_FOLDER_NAME, 2);
        if (projectDir != null) {
            String pb2goBatName = "runProtocClient.bat";
            VirtualFile virtualFile = ToolUtil.findTargetFile(projectDir, folderName, 3);
            if (virtualFile != null) {
                for (VirtualFile file : virtualFile.getChildren()) {
                    if (file.getName().equals(pb2goBatName)) {
                        eventCase = ProtoConst.EVENT_CASE_FOLDER;
                        baseDir = PsiManager.getInstance(project).findDirectory(virtualFile);
                        break;
                    }
                }
            }
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(KEY_EVENT_CASE, eventCase);
        resultMap.put(KEY_BASE_DIR, baseDir);

        return resultMap;
    }
}
