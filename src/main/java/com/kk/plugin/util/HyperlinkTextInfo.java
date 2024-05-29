package com.kk.plugin.util;

import com.intellij.execution.filters.HyperlinkInfo;
import org.jetbrains.annotations.NotNull;

public class HyperlinkTextInfo {
    public String text;
    public HyperlinkInfo hyperlinkInfo;

    public HyperlinkTextInfo(@NotNull String text, @NotNull HyperlinkInfo hyperlinkInfo) {
        this.text = text;
        this.hyperlinkInfo = hyperlinkInfo;
    }
}
