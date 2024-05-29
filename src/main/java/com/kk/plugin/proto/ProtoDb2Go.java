package com.kk.plugin.proto;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.kk.plugin.util.GoPluginUtil;
import com.kk.plugin.util.TextConsoleView;
import com.kk.plugin.util.ToolUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * proto文件生成go文件
 * 1 单个或者多个proto文件
 * 2 proto目录
 */
public class ProtoDb2Go extends ProtoActionManager {
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
        TextConsoleView consoleView = GoPluginUtil.getConsoleView(project);

        Map<String, Object> parseInfoMap = parseActionInfo(e, project, ProtoConst.DB_PROTO_FOLDER_NAME);
        String pb2goBatName = "generateDbProto.bat";
//        ProtoActionManager protoManager = new ProtoActionManager();
//        protoManager.GenProto2Go(project, consoleView, parseInfoMap, pb2goBatName);
        // 生成命令行对象
        GeneralCommandLine commandLine = generateCommandLine(parseInfoMap, pb2goBatName, project, consoleView);
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
        Map<String, Object> parseInfoMap = parseActionInfo(e, project, ProtoConst.DB_PROTO_FOLDER_NAME);
        int eventCase = (int) parseInfoMap.get(KEY_EVENT_CASE);
        if (eventCase == ProtoConst.EVENT_CASE_DEFAULT) {
            e.getPresentation().setEnabledAndVisible(false);
        }
    }
}
