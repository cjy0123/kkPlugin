package com.kk.plugin.group;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.kk.plugin.proto.ProtoActionManager;
import com.kk.plugin.proto.ProtoConst;
import com.kk.plugin.util.GoPluginUtil;
import com.kk.plugin.util.TextConsoleView;
import com.kk.plugin.util.ToolUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kk.plugin.proto.ProtoActionManager.generateCommandLine;

public class DynamicActionsGroup extends CustomDefaultActionGroup {
    @Override
    @NotNull
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        AnAction[] child = new AnAction[0];
        Project project;
        if (e == null || (project = e.getProject()) == null) {
            return child;
        }

        List<PluginsConfig> configs = readActionNamesFromConfigFile(project);
        child = new AnAction[configs.size()];
        for (int i = 0; i < configs.size(); i++) {
            DynamicAction c = new DynamicAction(configs.get(i));
            child[i] = c;
        }

        return child;
    }

    private static class DynamicAction extends AnAction {
        private final PluginsConfig pluginsConfig;

        public DynamicAction(PluginsConfig actionConfig) {
            super(actionConfig.getName(), actionConfig.getDes(), actionConfig.getIcon());
            pluginsConfig = actionConfig;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            Project project = e.getProject();
            if (project == null) {
                return;
            }
            // 在这里动态生成代码
            generateCodeForAction(project);
        }

        private void generateCodeForAction(Project project) {
            boolean check = pluginsConfig.getCheck();
            if (check) {
                int result = Messages.showOkCancelDialog(project, "确认执行" + pluginsConfig.getName() + "吗？", "危险危险危险!", "确定", "取消", null);
                if (result == Messages.CANCEL) {
                    return;
                }
            }

            Map<String, Object> parseInfoMap = new HashMap<>();
            String workDirStr = project.getBasePath() + "/" + pluginsConfig.getWorkDir();
            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(workDirStr);
            if (virtualFile != null && virtualFile.isDirectory()) {
                PsiDirectory workDir = PsiManager.getInstance(project).findDirectory(virtualFile);
                parseInfoMap.put(ProtoActionManager.KEY_BASE_DIR, workDir);
            } else {
                Messages.showMessageDialog("找不到workDir,请检查配置:" + workDirStr, "出错啦", null);
                return;
            }
            parseInfoMap.put(ProtoActionManager.KEY_PARAMS, pluginsConfig.getParams());
            TextConsoleView consoleView = GoPluginUtil.getConsoleView(project);
            GeneralCommandLine commandLine = generateCommandLine(parseInfoMap, pluginsConfig.getDoAction(), project, consoleView);
            if (commandLine == null) {
                return;
            }
            ToolUtil.execCmd(project, consoleView, commandLine);
        }
    }


    public static List<PluginsConfig> readActionNamesFromConfigFile(Project project) {
        List<PluginsConfig> actionConfigs = new ArrayList<>();
        VirtualFile configFile = findConfigFile(project);
        if (configFile != null) {
            try {
                String content = new String(configFile.contentsToByteArray(), StandardCharsets.UTF_8);
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> configs = mapper.readValue(content, new TypeReference<>() {
                });
                for (Map<String, Object> config : configs) {
                    actionConfigs.add(new PluginsConfig(config));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return actionConfigs;
    }

    private static VirtualFile findConfigFile(Project project) {
        // 假设配置文件在项目根目录下
        VirtualFile projectDir = ToolUtil.findProjectPath(project, ProtoConst.SERVER_PROTO_FOLDER_NAME, 2);
        if (projectDir != null) {
            return projectDir.findChild("idePlugins.json");
        }
        return null;
    }
}
