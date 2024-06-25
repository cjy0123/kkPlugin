package com.kk.plugin.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.project.Project;

@State(
        name = "MyPluginSettings",
        storages = {@Storage("MyPluginSettings.xml")}
)
public class MyPluginSettings implements PersistentStateComponent<MyPluginSettings.State> {

    public static class State {
        public String configManagerTxt = "";
    }

    private State myState = new State();

    @Nullable
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.myState = state;
    }

    public static MyPluginSettings getInstance(@NotNull Project project) {
        return project.getService(MyPluginSettings.class);
    }

    public String getConfigManagerTxt() {
        return myState.configManagerTxt;
    }

    public void setConfigManagerTxt(String value) {
        myState.configManagerTxt = value;
    }


}
