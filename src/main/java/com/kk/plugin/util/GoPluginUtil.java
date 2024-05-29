package com.kk.plugin.util;

import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.HyperlinkInfoFactory;
import com.intellij.execution.filters.LazyFileHyperlinkInfo;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.execution.ui.actions.CloseAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.util.Collections;

public class GoPluginUtil {

    static String EventLogTabName = "服务端插件";
    /**
     * 将异常堆栈转换为字符串
     */
    public static String stackTraceToString(String prefix, Throwable e, int stackLineLimit) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(prefix)) {
            sb.append(prefix);
        }
        try (CharArrayWriter result = new CharArrayWriter();
             PrintWriter printWriter = new PrintWriter(result)) {
            e.printStackTrace(printWriter);
            BufferedReader reader = new BufferedReader(new CharArrayReader(result.toCharArray()));
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
                lineCount ++;
                if (stackLineLimit > 0 && lineCount >= stackLineLimit) {
                    if (reader.readLine() != null) {//还有数据
                        sb.append("(has more info...)");
                    }
                    break;
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return sb.toString();
    }
    public static void printConsole(Project project, TextConsoleView consoleView, ConsoleViewContentType type, Object... msgs) {
        if (consoleView == null) return;
        if (msgs == null || msgs.length == 0) return;
        for (Object msg : msgs) {
            if (msg instanceof Throwable) {
                consoleView.print(stackTraceToString(null, (Throwable) msg, 0), type);
            } else if (msg instanceof PsiFile || msg instanceof PsiDirectory) {
                PsiFileSystemItem psiFile = (PsiFileSystemItem) msg;
//                ApplicationManager.getApplication().runReadAction(()-> consoleView.printHyperlink(psiFile.getVirtualFile().getPath(), HyperlinkInfoFactory.getInstance().createMultiplePsiElementHyperlinkInfo(Collections.singletonList(psiFile))));
                //兼容老版本ide
                ApplicationManager.getApplication().runReadAction(()-> consoleView.printHyperlink(psiFile.getVirtualFile().getPath(), HyperlinkInfoFactory.getInstance().createMultipleFilesHyperlinkInfo(Collections.singletonList(psiFile.getVirtualFile()), 0, psiFile.getProject())));
            } else if (msg instanceof VirtualFile virtualFile) {
                ApplicationManager.getApplication().runReadAction(()-> consoleView.printHyperlink(virtualFile.getPath(), HyperlinkInfoFactory.getInstance().createMultipleFilesHyperlinkInfo(Collections.singletonList(virtualFile), 0, project)));
            } else if (msg instanceof HyperlinkTextInfo hyperlinkTextInfo) {
                consoleView.printHyperlink(hyperlinkTextInfo.text, hyperlinkTextInfo.hyperlinkInfo);
            } else if (msg instanceof File || msg instanceof Path) {
                consoleView.printHyperlink(msg.toString(), new LazyFileHyperlinkInfo(project, msg.toString(), 0, 0));
            } else {
                consoleView.print(String.valueOf(msg), type);
            }
        }
    }

    public static void printlnConsole(Project project, TextConsoleView consoleView, ConsoleViewContentType type, Object... msgs) {
        printConsole(project, consoleView, type, msgs);
        consoleView.print("\n", type);
    }

    public static void printlnConsoleAndShowMessage(Project project, TextConsoleView consoleView, ConsoleViewContentType type, String title, Object... msgs) {
        if (consoleView != null) printlnConsole(project, consoleView, type, msgs);
        StringBuilder sb = new StringBuilder();
        for (Object msg : msgs) {
            if (msg instanceof Throwable) {
                sb.append(stackTraceToString(null, (Throwable) msg, 15));
            } else if (msg instanceof PsiFile) {
                PsiFile psiFile = (PsiFile) msg;
                sb.append(psiFile.getVirtualFile().getPath());
            } else if (msg instanceof VirtualFile) {
                VirtualFile virtualFile = (VirtualFile) msg;
                sb.append(virtualFile.getPath());
            } else if (msg instanceof HyperlinkTextInfo) {
                HyperlinkTextInfo hyperlinkTextInfo = (HyperlinkTextInfo) msg;
                sb.append(hyperlinkTextInfo.text);
            } else {
                sb.append(msg.toString());
            }
        }
        Messages.showMessageDialog(project, sb.toString(), title, Messages.getInformationIcon());
    }

    public static HyperlinkTextInfo getHyperlinkTextInfo(String text, PsiElement psiElement) {
//        return new HyperlinkTextInfo(text, HyperlinkInfoFactory.getInstance().createMultiplePsiElementHyperlinkInfo(Collections.singleton(psiElement)));
        //兼容老版本ide
        PsiFile psiFile = psiElement.getContainingFile();
        Document document = PsiDocumentManager.getInstance(psiElement.getProject()).getDocument(psiElement.getContainingFile());
        int lineNumber = 0;
        if (document != null) {
            lineNumber = document.getLineNumber(psiElement.getTextOffset());
        }
        return new HyperlinkTextInfo(text, HyperlinkInfoFactory.getInstance().createMultipleFilesHyperlinkInfo(Collections.singletonList(psiFile.getVirtualFile()), lineNumber, psiElement.getProject()));
    }

    public static TextConsoleView getConsoleView(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.RUN);
        TextConsoleView consoleView = null;
        if (toolWindow != null) {//尝试释放已存在的 控制台输出窗口
            ContentManager contentManager = toolWindow.getContentManager();
            if (contentManager.getContentCount() > 0) {//找到是否有匹配的 控制台输出窗口
                for (Content content : contentManager.getContents()) {
                    if (!(content.getComponent() instanceof ConsoleViewJPanel)) {
                        continue;
                    }
                    ConsoleViewJPanel consoleViewJPanel = (ConsoleViewJPanel) content.getComponent();
                    if (consoleView == null) {
                        consoleView = consoleViewJPanel.consoleView;
                        contentManager.setSelectedContent(content);
                    } else {//移除重复发 tab
                        contentManager.removeContent(content, true);
                    }
                }
            }
        }
        if (consoleView == null) {
            //创建新的控制台输出窗口
            consoleView = new TextConsoleView(project);
            DefaultActionGroup toolbarActions = new DefaultActionGroup();
            JComponent panel = new ConsoleViewJPanel(new BorderLayout(), consoleView);
            panel.add(consoleView.getComponent(), BorderLayout.CENTER);
            ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("RunIdeConsole", toolbarActions, false);
            toolbar.setTargetComponent(consoleView.getComponent());
            panel.add(toolbar.getComponent(), BorderLayout.WEST);
            panel.setVisible(true);
            toolbar.getComponent().setVisible(true);

            RunContentDescriptor descriptor = new RunContentDescriptor(consoleView, null, panel, EventLogTabName) {
                @Override
                public boolean isContentReuseProhibited() {
                    return true;
                }
            };
            Executor executor = DefaultRunExecutor.getRunExecutorInstance();
            toolbarActions.addAll(consoleView.createConsoleActions());
            toolbarActions.add(new CloseAction(executor, descriptor, project));
            RunContentManager.getInstance(project).showRunContent(executor, descriptor);
            toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.RUN);
            if (toolWindow == null) {
                return null;
            }
        }
        if (!toolWindow.isAvailable()) {
            toolWindow.setAvailable(true);
        }
        toolWindow.show();
        return consoleView;
    }

    /**
     * 刷新psi文件缓存
     */
    public static void syncPsiFile(Project project) {
        WriteCommandAction.runWriteCommandAction(project, (Computable<Object>) ()-> {
            VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
            PsiDocumentManager.getInstance(project).commitAllDocuments();
            return null;
        });
    }
}
