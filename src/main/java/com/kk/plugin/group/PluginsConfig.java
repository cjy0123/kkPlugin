package com.kk.plugin.group;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import java.util.Map;

import javax.swing.*;

public record PluginsConfig(Map<String, Object> config) {

    public String getName() {
        return (String) config.get("name");
    }

    public String getDes() {
        return (String) config.get("des");
    }

    public Icon getIcon() {
        return IconLoader.findIcon((String) config.get("icon"), AllIcons.class);
    }

    public String getWorkDir() {
        return (String) config.get("workDir");
    }

    public String getDoAction() {
        return (String) config.get("doAction");
    }


    public String getParams() {
        return (String) config.get("params");
    }

    public boolean getCheck() {
        return (boolean) config.getOrDefault("check", false);
    }

}
