package com.kk.plugin.util;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class ConsoleViewJPanel extends JPanel {

    @NotNull
    public TextConsoleView consoleView;

    public ConsoleViewJPanel(LayoutManager layout, @NotNull TextConsoleView consoleView) {
        super(layout);
        this.consoleView = consoleView;
    }
}
