package com.kk.plugin.batch.lexer;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.LayeredLexer;
import com.kk.plugin.batch.BatchTokenTypes;

/**
 * Lexer adapter.
 *
 * @author Alexey Efimov
 */
public class BatchHighlighterLexer extends LayeredLexer {
    public BatchHighlighterLexer() {
        super(new FlexAdapter(new _BatchLexer(null)));

        _ExpressionLexer stringLexer = new _ExpressionLexer(null);
        stringLexer.setDefaultToken(BatchTokenTypes.STRING_LITERAL);
        registerLayer(
                new FlexAdapter(stringLexer),
                BatchTokenTypes.STRING_LITERAL);
        _ExpressionLexer expressionLexer = new _ExpressionLexer(null);
        expressionLexer.setDefaultToken(BatchTokenTypes.EXPRESSION);
        registerLayer(
                new FlexAdapter(expressionLexer),
                BatchTokenTypes.EXPRESSION);
        _ExpressionLexer labelLexer = new _ExpressionLexer(null);
        labelLexer.setDefaultToken(BatchTokenTypes.LABEL_REFERENCE);
        registerLayer(
                new FlexAdapter(labelLexer),
                BatchTokenTypes.LABEL_REFERENCE);
    }
}
