package com.kk.plugin.proto;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.kk.plugin.util.GoPluginUtil;
import com.kk.plugin.util.TextConsoleView;
import com.kk.plugin.util.ToolUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kk.plugin.proto.ProtoConst.EVENT_CASE_DEFAULT;

public class ProtoActionManager extends AnAction {
    public static final String KEY_EVENT_CASE = "eventCase";
    public static final String KEY_PROTO_FILES = "psiFiles";
    public static final String KEY_BASE_DIR = "baseDir";


    public static GeneralCommandLine generateCommandLine(Map<String, Object> parseInfoMap, String pb2goBatName, Project project, TextConsoleView consoleView) {
        // 创建 GeneralCommandLine 实例
        GeneralCommandLine commandLine = new GeneralCommandLine();

        // 设置执行的命令
        commandLine.setExePath("cmd.exe");
        commandLine.getParametersList().addParametersString("/c");

        commandLine.addParameter(pb2goBatName);

        int eventCase = (int) parseInfoMap.getOrDefault(KEY_EVENT_CASE, EVENT_CASE_DEFAULT);
        PsiDirectory baseDir = (PsiDirectory) parseInfoMap.get(KEY_BASE_DIR);

        String pb2goPath = baseDir.getVirtualFile().getPath() + "/" + pb2goBatName;
        File pb2goFile = new File(pb2goPath);
        if (!pb2goFile.exists()) {
            GoPluginUtil.printlnConsole(project, consoleView, ConsoleViewContentType.LOG_ERROR_OUTPUT, "文件不存在: ", pb2goPath);
            return null;
        }

        if (parseInfoMap.containsKey(KEY_PROTO_FILES)) {
            // 添加命令行参数
            if (eventCase == ProtoConst.EVENT_CASE_FOLDER) {
                commandLine.addParameter("all");
            } else if (eventCase == ProtoConst.EVENT_CASE_SINGLE_FILE || eventCase == ProtoConst.EVENT_CASE_MULTI_FILE) {
                List<String> fileNames = (List<String>) parseInfoMap.get(KEY_PROTO_FILES);
                for (String fileName : fileNames) {
                    commandLine.addParameter(fileName);
                }
            }
        }
//        commandLine.addParameter("arg1");
//        commandLine.addParameter("arg2");
        // 可以继续添加其他参数

        // 设置工作目录

        commandLine.setWorkDirectory(new File(baseDir.getVirtualFile().getPath()));

        // 如果需要设置环境变量，可以使用下面的方法
        // commandLine.withEnvironment("VAR_NAME", "value");

        return commandLine;
    }

    public void GenProto2Go(Project project, TextConsoleView consoleView, Map<String, Object> parseInfoMap, String pb2goBatName) {
        int eventCase = (int) parseInfoMap.get(KEY_EVENT_CASE);
        List<String> fileNames = (List<String>) parseInfoMap.get(KEY_PROTO_FILES);
        PsiDirectory baseDir = (PsiDirectory) parseInfoMap.get(KEY_BASE_DIR);
        if (eventCase == EVENT_CASE_DEFAULT) {
            GoPluginUtil.printlnConsoleAndShowMessage(project, consoleView,
                    ConsoleViewContentType.LOG_ERROR_OUTPUT,
                    "生成proto失败", "未找到proto 相关目录!");
            return;
        }

        String pb2goPath = baseDir.getVirtualFile().getPath() + "/" + pb2goBatName;
        File pb2goFile = new File(pb2goPath);
        if (!pb2goFile.exists()) {
            GoPluginUtil.printlnConsole(project, consoleView, ConsoleViewContentType.LOG_ERROR_OUTPUT, "文件不存在: ", pb2goPath);
            return;
        }

        StringBuilder protoCmd = new StringBuilder(pb2goBatName);
        if (eventCase == ProtoConst.EVENT_CASE_FOLDER) {
            protoCmd.append(" all");
        } else if (eventCase == ProtoConst.EVENT_CASE_SINGLE_FILE || eventCase == ProtoConst.EVENT_CASE_MULTI_FILE) {
            for (String fileName : fileNames) {
                protoCmd.append(" ").append(fileName);
            }
        }

        ToolUtil.runBackgroundTask(project, "执行protoc转化go", progressIndicator -> {
            //提交当前所有文件
            WriteCommandAction.runWriteCommandAction(project, (Computable<Object>) () -> {
                PsiDocumentManager.getInstance(project).commitAllDocuments();
                return null;
            });
            boolean execSuccess = ToolUtil.execCmd(project, consoleView, protoCmd.toString(), new File(baseDir.getVirtualFile().getPath()));
            if (execSuccess) {
                GoPluginUtil.syncPsiFile(project);//刷新文件缓存
                GoPluginUtil.printlnConsole(project, consoleView, ConsoleViewContentType.NORMAL_OUTPUT, "protoc执行完成!");
            }
        });
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
    }

    /**
     * 解析本次的操作事件，并返回操作类型、与相关的文件
     */
    public Map<String, Object> parseActionInfo(AnActionEvent e, Project project, String folderName) {
        if (!ActionPlaces.EDITOR_POPUP.equals(e.getPlace())
                && !ActionPlaces.PROJECT_VIEW_POPUP.equals(e.getPlace())) {//只有在点击右键时才做过滤
            return parseToolActionInfo(project, folderName);
        }

        //在Action显示之前,根据选中文件扩展名判定是否显示此Action
        PsiElement psiElement = PlatformDataKeys.PSI_ELEMENT.getData(e.getDataContext());
        int eventCase = EVENT_CASE_DEFAULT;
        List<String> protoFiles = new ArrayList<>();
        PsiDirectory baseDir = null;

        if (psiElement != null) {
            if (psiElement instanceof PsiDirectory psiDirectory) {
                String name = psiDirectory.getName();
                if (name.equals(folderName)) {
                    boolean hasProtoFile = false;
                    for (PsiFile file : psiDirectory.getFiles()) {
                        if (file.getName().endsWith(ProtoConst.PROTO_FILE_NAME_SUFFIX)) {
                            hasProtoFile = true;
                            break;
                        }
                    }
                    if (hasProtoFile) {
                        eventCase = ProtoConst.EVENT_CASE_FOLDER;
                        baseDir = psiDirectory.getParentDirectory();
                    }
                }
            } else if (psiElement instanceof PsiFile psiFile && ((PsiFile) psiElement).getName().endsWith(ProtoConst.PROTO_FILE_NAME_SUFFIX)) {
                if (psiFile.getParent() != null && psiFile.getParent().getName().equals(folderName)) {
                    eventCase = ProtoConst.EVENT_CASE_SINGLE_FILE;
                    protoFiles.add(psiFile.getName());
                    baseDir = psiFile.getParent().getParentDirectory();
                }
            }
        } else {
            VirtualFile[] psiFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
            if (psiFiles != null && psiFiles.length > 0) {
                VirtualFile curDirVirtualFile = psiFiles[0].getParent();
                if (curDirVirtualFile != null
                        && curDirVirtualFile.isDirectory()
                        && curDirVirtualFile.getName().equals(folderName)) {
                    boolean allMatch = true;
                    for (VirtualFile file : psiFiles) {
                        if (file.isDirectory()
                                || !curDirVirtualFile.equals(file.getParent()) //所有文件在同一目录
                                || !file.getName().endsWith(ProtoConst.PROTO_FILE_NAME_SUFFIX)) {
                            allMatch = false;
                            break;
                        }
                    }
                    if (allMatch) {
                        eventCase = ProtoConst.EVENT_CASE_MULTI_FILE;
                        for (VirtualFile file : psiFiles) {
                            protoFiles.add(file.getName());
                        }
                        baseDir = PsiManager.getInstance(project).findDirectory(psiFiles[0].getParent().getParent());
                    }
                }
            }
        }
        // 支持右键选择文件
        if (eventCase == EVENT_CASE_DEFAULT) {
            PsiFile psiFile = PlatformDataKeys.PSI_FILE.getData(e.getDataContext());
            if (psiFile != null && psiFile.getParent() != null && psiFile.getName().endsWith(ProtoConst.PROTO_FILE_NAME_SUFFIX)) {
                if (psiFile.getParent().getName().equals(folderName)) {
                    eventCase = ProtoConst.EVENT_CASE_SINGLE_FILE;
                    protoFiles.add(psiFile.getName());
                    baseDir = psiFile.getParent().getParentDirectory();
                }
            } else {
//                System.out.println(e.getActionManager().getId(this));
            }
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(KEY_EVENT_CASE, eventCase);
        resultMap.put(KEY_PROTO_FILES, protoFiles);
        resultMap.put(KEY_BASE_DIR, baseDir);

        return resultMap;
    }

    /**
     * 支持toolbar选择
     */
    public Map<String, Object> parseToolActionInfo(Project project, String folderName) {
        int eventCase = EVENT_CASE_DEFAULT;
        List<String> protoFiles = new ArrayList<>();
        PsiDirectory baseDir = null;

        VirtualFile projectDir = ToolUtil.findProjectPath(project, ProtoConst.SERVER_PROTO_FOLDER_NAME, 2);
        if (projectDir != null) {
            VirtualFile virtualFile = ToolUtil.findTargetFile(projectDir, folderName, 3);
            if (virtualFile != null) {
                boolean hasProtoFile = false;
                for (VirtualFile file : virtualFile.getChildren()) {
                    if (file.getName().endsWith(ProtoConst.PROTO_FILE_NAME_SUFFIX)) {
                        hasProtoFile = true;
                        break;
                    }
                }
                if (hasProtoFile) {
                    eventCase = ProtoConst.EVENT_CASE_FOLDER;
                    baseDir = PsiManager.getInstance(project).findDirectory(virtualFile.getParent());
                }
            }
        }


        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(KEY_EVENT_CASE, eventCase);
        resultMap.put(KEY_PROTO_FILES, protoFiles);
        resultMap.put(KEY_BASE_DIR, baseDir);
        return resultMap;
    }


}
