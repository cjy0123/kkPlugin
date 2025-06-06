package com.kk.plugin.group;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.kk.plugin.settings.ProjectTypeService;

public class CustomDefaultActionGroup extends DefaultActionGroup {
    @Override
    public void update(AnActionEvent e) {
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
