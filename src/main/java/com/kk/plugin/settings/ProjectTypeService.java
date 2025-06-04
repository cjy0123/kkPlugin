package com.kk.plugin.settings;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.kk.plugin.proto.ProtoConst;
import com.kk.plugin.util.ToolUtil;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
public final class ProjectTypeService {
    private final Project project;
    private boolean isGoProject = false;

    public ProjectTypeService(Project project) {
        this.project = project;
        detectProjectType();
    }

    private void detectProjectType() {
        // 检查是否是Go项目
        VirtualFile projectDir = ToolUtil.findProjectPath(project, ProtoConst.SERVER_PROTO_FOLDER_NAME, 2);
        if (projectDir != null) {
            isGoProject = projectDir.findChild("idePlugins.json") != null;
        }
    }

    public static ProjectTypeService getInstance(@NotNull Project project) {
        return project.getService(ProjectTypeService.class);
    }

    public boolean isGoProject() {
        return isGoProject;
    }
}