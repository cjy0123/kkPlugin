package com.kk.plugin.util;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.AnsiEscapeDecoder;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

/**
 * 自定义 ConsoleView 支持 shell 脚本命令输出 带颜色文本
 */
public class TextConsoleView extends ConsoleViewImpl implements AnsiEscapeDecoder.ColoredTextAcceptor {
    private final AnsiEscapeDecoder myAnsiEscapeDecoder = new AnsiEscapeDecoder();

    public TextConsoleView(@NotNull Project project) {
        super(project, true);
    }

    /**
     * 支持带颜色 的 shell 脚本执行输出
     */
    public void printShellOut(@NotNull String text, boolean isStdOut) {
        Key<?> outputType = !isStdOut ? ProcessOutputTypes.STDERR : ProcessOutputTypes.STDOUT;
        myAnsiEscapeDecoder.escapeText(text, outputType, this);
    }

    @Override
    public void coloredTextAvailable(@NotNull String text, @NotNull Key attributes) {
        print(text, ConsoleViewContentType.getConsoleViewType(attributes));
    }
}
