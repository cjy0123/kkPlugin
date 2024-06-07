package com.kk.plugin;

import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorEventMulticaster;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.kk.plugin.lisener.GoFileDocumentListener;
import org.jetbrains.annotations.NotNull;

public class PluginStartupActivity implements StartupActivity, DumbAware {
    @Override
    public void runActivity(@NotNull Project project) {
        GoFileDocumentListener lis = new GoFileDocumentListener();
        EditorFactory editorFactory = EditorFactory.getInstance();
        EditorEventMulticaster eventMulticaster = editorFactory.getEventMulticaster();
        eventMulticaster.addDocumentListener(lis, project);
    }
}
