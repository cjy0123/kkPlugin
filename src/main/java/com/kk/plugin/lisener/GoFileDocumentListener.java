package com.kk.plugin.lisener;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.ui.Messages;

public class GoFileDocumentListener implements DocumentListener {
    private final String markerStart = "// BEGIN AUTOGENERATED CODE";
    private final String markerEnd = "// END AUTOGENERATED CODE";

    @Override
    public void beforeDocumentChange(DocumentEvent event) {
        Document document = event.getDocument();
        String documentText = document.getText();

        int startIdx = documentText.indexOf(markerStart);
        int endIdx = documentText.indexOf(markerEnd);

        if (startIdx != -1 && endIdx != -1 && startIdx < event.getOffset() && endIdx > event.getOffset()) {
            Messages.showWarningDialog("You are modifying autogenerated code. Be cautious!", "Warning");
        }
    }
}
