package com.kk.plugin.batch.fileTypes;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.kk.plugin.batch.BatchLanguage;
import com.kk.plugin.batch.util.BatchBundle;
import com.kk.plugin.batch.util.BatchIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * Batch file type.
 *
 * @author Alexey Efimov
 */
public final class BatchFileType extends LanguageFileType {
    public static final BatchFileType BATCH_FILE_TYPE = new BatchFileType();

    @NonNls
    public static final List<String> DEFAULT_ASSOCIATED_EXTENSIONS = Arrays.asList("bat", "cmd", "exe");

    public BatchFileType() {
        super(BatchLanguage.INSTANCE);
    }

    @NotNull
    @NonNls
    public String getDefaultExtension() {
        return DEFAULT_ASSOCIATED_EXTENSIONS.get(0);
    }

    @NotNull
    public String getDescription() {
        return BatchBundle.message("batch.filetype.description");
    }

    @NotNull
    public Icon getIcon() {
        return BatchIcons.BATCH_FILE_ICON;
    }

    @NotNull
    @NonNls
    public String getName() {
        return "Batch";
    }
}
