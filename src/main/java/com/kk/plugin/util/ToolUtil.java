package com.kk.plugin.util;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.*;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.history.core.Paths;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ToolUtil {


    /**
     * 执行命令，并输出结果
     */
    public static boolean execCmd(Project project, TextConsoleView consoleView, String cmd, @Nullable File dir, StringBuilder execMessage, StringBuilder errorMessage) {
        if (consoleView != null) {
            consoleView.clear();
        }
        if (dir != null) {
            GoPluginUtil.printlnConsole(project, consoleView, ConsoleViewContentType.NORMAL_OUTPUT, "工作目录: ", dir.getAbsolutePath());
        }
        GoPluginUtil.printlnConsole(project, consoleView, ConsoleViewContentType.NORMAL_OUTPUT, "执行命令: ", cmd);
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", cmd}, null, dir);
            boolean exited = process.waitFor(20, TimeUnit.SECONDS);//120s超时
            if (!exited) {
                //超时强制关闭 命令进程
                process.destroyForcibly();
                GoPluginUtil.printlnConsole(project, consoleView, ConsoleViewContentType.LOG_ERROR_OUTPUT, "执行命令超时!");
                return true;
            }
            if (processOutput(consoleView, errorMessage, process)) return false;
        } catch (Exception e) {
            GoPluginUtil.printlnConsole(project, consoleView, ConsoleViewContentType.LOG_ERROR_OUTPUT, "执行命令异常!", e);
            return false;
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
        if (consoleView != null) {
            GoPluginUtil.printlnConsole(project, consoleView, ConsoleViewContentType.NORMAL_OUTPUT, "执行完成!\nstd 输出:");
//            consoleView.printShellOut(execMessage + "\n", true);
        }
        if (!errorMessage.isEmpty()) {
            if (consoleView != null) {
                GoPluginUtil.printlnConsole(project, consoleView, ConsoleViewContentType.LOG_ERROR_OUTPUT, "err 输出:");
                consoleView.printShellOut(errorMessage + "\n", false);
            }
            return false;
        }
        return true;
    }

    public static boolean execCmd(Project project, TextConsoleView consoleView, GeneralCommandLine commandLine) {
        if (consoleView != null) {
            consoleView.clear();
        }
        File dir = commandLine.getWorkDirectory();
        if (commandLine.getWorkDirectory() != null) {
            GoPluginUtil.printlnConsole(project, consoleView, ConsoleViewContentType.NORMAL_OUTPUT, "工作目录: ", dir.getAbsolutePath());
        }
        GoPluginUtil.printlnConsole(project, consoleView, ConsoleViewContentType.NORMAL_OUTPUT, "执行命令: ", commandLine.getCommandLineString());
        // 创建进程处理器
        OSProcessHandler osProcessHandler;
        StringBuilder errorMessage = new StringBuilder();

        try {
            osProcessHandler = new OSProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString(), StandardCharsets.UTF_8);
            // 附加进程终止监听器
            ProcessTerminatedListener.attach(osProcessHandler);

            // 添加监听器以捕获进程输出
            osProcessHandler.addProcessListener(new ProcessAdapter() {
                @Override
                public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                    ConsoleViewContentType contentType = outputType == ProcessOutputTypes.STDOUT ?
                            ConsoleViewContentType.NORMAL_OUTPUT :
                            ConsoleViewContentType.ERROR_OUTPUT;
                    GoPluginUtil.printConsole(project, consoleView, contentType, event.getText());
                }

                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                    GoPluginUtil.printlnConsole(project, consoleView,
                            ConsoleViewContentType.SYSTEM_OUTPUT,
                            "进程已终止，退出码：" + event.getExitCode());
                }
            });

            // 开始执行并返回处理器
            osProcessHandler.startNotify();
        } catch (Exception e) {
            GoPluginUtil.printlnConsole(project, consoleView, ConsoleViewContentType.LOG_ERROR_OUTPUT, "执行命令异常!", e);
            return false;
        }

        if (!errorMessage.isEmpty()) {
            if (consoleView != null) {
                GoPluginUtil.printlnConsole(project, consoleView, ConsoleViewContentType.LOG_ERROR_OUTPUT, "err 输出:");
                consoleView.printShellOut(errorMessage + "\n", false);
            }
            return false;
        }
        return true;
    }

    private static boolean processOutput(TextConsoleView consoleView, StringBuilder errorMessage, Process process) throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        BufferedReader buffError = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
        String line;
        while ((line = input.readLine()) != null) {
//                execMessage.append(line).append("\n");
            if (consoleView != null) {
                consoleView.printShellOut(line + "\n", true);
            }
        }
        while ((line = buffError.readLine()) != null) {
            errorMessage.append(line).append("\n");
        }
        input.close();
        buffError.close();
        return false;
    }

    /**
     * 执行命令，并输出结果
     */
    public static boolean execCmd(Project project, TextConsoleView consoleView, String cmd, @Nullable File dir) {
        return execCmd(project, consoleView, cmd, dir, new StringBuilder(), new StringBuilder());
    }

    /**
     * 从项目起始位置向下寻找目标文件或目录
     */
    public static VirtualFile findProjectPath(Project project, String fileName, int depth) {

        VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentRoots();
        for (VirtualFile root : roots) {
            VirtualFile file = findTargetFile(root, fileName, depth);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    /**
     * 向下寻找目标文件或目录
     *
     * @param startFile 查找的起始位置
     * @param fileName  目标文件或目录
     * @param depth     向下查找的深度
     * @return VirtualFile
     */
    public static VirtualFile findTargetFile(VirtualFile startFile, String fileName, int depth) {
        if (startFile == null || !startFile.exists()) {
            return null;
        }
        // 根目录就是要找的文件
        if (startFile.getName().equals(fileName)) {
            return startFile;
        }
        // 找不到
        if (depth < 1 || !startFile.isDirectory()) {
            return null;
        }
        List<VirtualFile[]> scanDirsList = new ArrayList<>();
        scanDirsList.add(startFile.getChildren());
        List<VirtualFile[]> tempList = new ArrayList<>();
        for (int i = 0; i < depth; i++) {
            tempList.clear();
            for (VirtualFile[] scanFiles : scanDirsList) {
                for (VirtualFile file : scanFiles) {
                    if (file.getName().equals(fileName)) {
                        return file;
                    }
                    // 跳过隐藏文件
                    if (file.getName().startsWith(".")) {
                        continue;
                    }
                    if (file.isDirectory()) {
                        tempList.add(file.getChildren());
                    }
                }
            }
            scanDirsList.clear();
            scanDirsList.addAll(tempList);
        }
        return null;
    }

    /**
     * phpstorm建项目，常规是2种方式
     * 1 是api、notice建2个项目
     * 2 api、notice在一个项目下
     * <p>
     * 找出api-lumen项目的路径
     */
    public static String findApiProjectPath(Project project) {

        String projectName = project.getName();
        String apiTagString = "api-lumen";

        String apiProjectPath = "";
        String projectBaseDir = project.getBasePath();
        if (projectBaseDir == null) {
            throw new RuntimeException("找不到项目根目录！");
        }
        if (projectName.endsWith(apiTagString)) { // 表明api是单独的项目
            apiProjectPath = projectBaseDir;
        } else {
            // 遍历项目当前目录,找出api项目目录
            File file = new File(projectBaseDir);
            File[] fs = file.listFiles();
            if (fs != null) {
                for (File f : fs) {
                    if (!f.isDirectory()) {
                        continue;
                    }
                    if (f.getAbsolutePath().endsWith(apiTagString)) {
                        apiProjectPath = f.getAbsolutePath();
                        break;
                    }
                }
            }
        }

        return apiProjectPath;
    }

    /**
     * 运行background 任务
     *
     * @param project  项目
     * @param title    task
     * @param runnable 要运行的任务内容
     */
    public static void runBackgroundTask(Project project, String title, Consumer<ProgressIndicator> runnable) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, title) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setFraction(0);
                runnable.accept(indicator);
                indicator.setFraction(1.0);
                indicator.setText("Finished");
            }
        });
    }

    /**
     * 获取当前代码分支
     *
     * @param project 项目
     * @param dir     目标目录
     */
    public static String getCodeBranch(Project project, TextConsoleView consoleView, @NotNull PsiDirectory dir) {
        StringBuilder execMessage = new StringBuilder();
        boolean getGitBranchRet = execCmd(project, consoleView, "git symbolic-ref --short -q HEAD", dir.getVirtualFile().toNioPath().toFile(), execMessage, new StringBuilder());
        if (!getGitBranchRet) {
            return null;
        }
        return execMessage.toString().trim();
    }

    public static void renameSnake(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            String name = humpToSnake(f.getName());
            if (!name.equals(f.getName())) {
                FileUtils.moveFile(f, new File(f.getParent() + "/" + name));
            }
        }
    }

    public static String humpToSnake(String str) {
        return (str.charAt(0) + str.substring(1).replaceAll("[A-Z]", "_$0")).toLowerCase();
    }

    /**
     * 判断指定目录列表下是否存在 指定路径文件
     */
    public static boolean fileExistInDirs(File[] dirs, String relativePath) {
        if (dirs == null) {
            return false;
        }
        for (File moduleDir : dirs) {
            if (new File(Paths.appended(moduleDir.getPath(), relativePath)).exists()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据 目录列表 获取文件路径字符串
     */
    public static String getPaths(String... dirs) {
        StringBuilder sb = new StringBuilder();
        for (String dir : dirs) {
            if (sb.length() > 0) {
                sb.append(File.separatorChar);
            }
            sb.append(dir);
        }
        return sb.toString();
    }

    /**
     * 根据 目录列表 获取文件路径字符串
     */
    public static String getPaths(List<String> dirs) {
        StringBuilder sb = new StringBuilder();
        for (String dir : dirs) {
            if (sb.length() > 0) {
                sb.append(File.separatorChar);
            }
            sb.append(dir);
        }
        return sb.toString();
    }
}
