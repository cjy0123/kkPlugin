package com.kk.plugin.util;

import com.goide.cgo.CgoFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * psiFile 操作工具类
 */
public class PsiFileUtil {

    public static PsiFile write(PsiDirectory targetDir, String filename, String content, boolean overwrite) {
        return WriteCommandAction.runWriteCommandAction(targetDir.getProject(), (Computable<PsiFile>) () -> {
            PsiFile psiFile = PsiFileFactory.getInstance(targetDir.getProject()).
                    createFileFromText(filename, CgoFileType.INSTANCE, content);
            PsiFile oldFile = targetDir.findFile(filename);
            if (oldFile != null) {
                if (!overwrite) {
                    return null;
                }
                oldFile.delete(); // 删除旧文件
            }
            return (PsiFile) targetDir.add(psiFile);
        });
    }

    public static PsiFile write(PsiDirectory targetDir, String filename, String content) {
        return write(targetDir, filename, content, true);
    }

    public static @NotNull PsiDirectory findAndCreateIfNotExitDirs(@NotNull PsiDirectory baseDir, @NotNull String[] paths) {
        if (paths.length == 0) {
            return baseDir;
        }
        PsiDirectory parentDir = baseDir;
        PsiDirectory directory = null;
        for (String subPath : paths) {
            PsiDirectory finalParentDir = parentDir;
            directory = ApplicationManager.getApplication().runReadAction((Computable<PsiDirectory>) () -> finalParentDir.findSubdirectory(subPath));
            if (directory == null) {
                directory = WriteCommandAction.runWriteCommandAction(baseDir.getProject(), (Computable<PsiDirectory>) () -> finalParentDir.createSubdirectory(subPath));
            }
            parentDir = directory;
        }
        return directory;
    }

    public static @NotNull PsiDirectory findAndCreateIfNotExitDirs(@NotNull PsiDirectory baseDir, @NotNull Collection<String> paths) {
        String[] pathArr = new String[paths.size()];
        paths.toArray(pathArr);
        return findAndCreateIfNotExitDirs(baseDir, pathArr);
    }

    @Nullable
    public static PsiDirectory findSubDir(@NotNull PsiDirectory baseDir, @NotNull String subDir) {
        if (StringUtils.isBlank(subDir)) {
            return null;
        }
        return ApplicationManager.getApplication().runReadAction((Computable<PsiDirectory>) () -> baseDir.findSubdirectory(subDir));
    }

    @Nullable
    public static PsiFile findFile(@NotNull PsiDirectory baseDir, @NotNull String file) {
        if (StringUtils.isBlank(file)) {
            return null;
        }
        return ApplicationManager.getApplication().runReadAction((Computable<PsiFile>) () -> baseDir.findFile(file));
    }

    public static VirtualFile findFile(@NotNull VirtualFile directory, @NotNull String file) {
        for (VirtualFile childFile : directory.getChildren()) {
            if (childFile.isDirectory()) {
                findFile(childFile, file);
            } else if (childFile.getName().equals(file)) {
                return childFile;
            }
        }
        return null;
    }

    public static String readGoFileContent(PsiFile psiFile) throws IOException {
        VirtualFile virtualFile = psiFile.getVirtualFile();
        return readGoFileContent(virtualFile);
    }

    public static String readGoFileContent(VirtualFile virtualFile) throws IOException {
        byte[] fileBytes = virtualFile.contentsToByteArray();
        return new String(fileBytes, StandardCharsets.UTF_8);
    }

    public static void write(PsiFile psiFile, String content) {
        if (psiFile != null) {
            PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(psiFile.getProject());
            psiDocumentManager.commitAllDocuments();
            psiFile.getViewProvider().getDocument().setText(content);
            psiDocumentManager.commitAllDocuments();
        }
    }

    public static void write(VirtualFile file, String content) {
        try {
            file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
            file.refresh(false, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
