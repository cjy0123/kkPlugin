package com.kk.plugin.anyCmd;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.kk.plugin.proto.ProtoActionManager;
import com.kk.plugin.proto.ProtoConst;
import com.kk.plugin.util.GoPluginUtil;
import com.kk.plugin.util.TextConsoleView;
import com.kk.plugin.util.ToolUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;

/**
 * 执行任意cmd
 */
public class AnyCmd extends AnAction {

    public static String EXE_FILE_NAME_SUFFIX = ".exe";
    public static String BAT_FILE_NAME_SUFFIX = ".bat";

    @Override
    public void actionPerformed(AnActionEvent e) {
        @Nullable Project project = e.getProject();
        if (project == null) {
            return;
        }
        TextConsoleView consoleView = GoPluginUtil.getConsoleView(project);
        // 获取当前上下文的 PsiElement
        PsiElement psiElement = PlatformDataKeys.PSI_ELEMENT.getData(e.getDataContext());
        PsiFile psiFile = (PsiFile) psiElement;
        PsiDirectory baseDir = psiFile.getContainingDirectory();
        StringBuilder protoCmd = new StringBuilder(((PsiFile) psiElement).getName());
        ToolUtil.runBackgroundTask(project, "执行"+psiFile.getName(), progressIndicator -> {
            //提交当前所有文件
            WriteCommandAction.runWriteCommandAction(project, (Computable<Object>) () -> {
                PsiDocumentManager.getInstance(project).commitAllDocuments();
                return null;
            });
            boolean execSuccess = ToolUtil.execCmd(project, consoleView, protoCmd.toString(), new File(baseDir.getVirtualFile().getPath()));
            if (execSuccess) {
                GoPluginUtil.syncPsiFile(project);//刷新文件缓存
                GoPluginUtil.printlnConsole(project, consoleView, ConsoleViewContentType.NORMAL_OUTPUT, ((PsiFile) psiElement).getName() + "执行完成!");
            }
        });
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        if (!ActionPlaces.PROJECT_VIEW_POPUP.equals(e.getPlace())) {//只有在点击右键时才做过滤
            return;
        }

        PsiElement psiElement = PlatformDataKeys.PSI_ELEMENT.getData(e.getDataContext());
        if (psiElement != null && psiElement instanceof PsiFile) {
            PsiFile psiFile = (PsiFile) psiElement;
            if (!psiFile.getName().endsWith(EXE_FILE_NAME_SUFFIX)) {
                e.getPresentation().setEnabledAndVisible(false);
            } else {
                e.getPresentation().setText("执行" + psiFile.getName());
                e.getPresentation().setDescription("执行" + psiFile.getName());
            }
        }
    }
}
