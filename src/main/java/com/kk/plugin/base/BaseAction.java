package com.kk.plugin.base;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.kk.plugin.settings.ProjectTypeService;
import org.jetbrains.annotations.NotNull;

public abstract class BaseAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        Project project = e.getProject();
        if (project != null) {
            // 只在Go项目中显示
            boolean isVisible = ProjectTypeService.getInstance(project).isGoProject();
            e.getPresentation().setVisible(isVisible);
        } else {
            e.getPresentation().setVisible(false);
        }
    }
}