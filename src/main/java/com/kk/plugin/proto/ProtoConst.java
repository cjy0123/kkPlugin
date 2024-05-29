package com.kk.plugin.proto;

public class ProtoConst {
    /**
     * proto文件后缀，只有该后缀的才做相关处理
     */
    public static String PROTO_FILE_NAME_SUFFIX = ".proto";
    /**
     * 存放proto文件的目录
     */
    public static String PROTO_FOLDER_NAME = "proto";
    public static String DB_PROTO_FOLDER_NAME = "dbProto";
    public static String CLIENT_PROTO_FOLDER_NAME = "client-test";

    public static String SERVER_PROTO_FOLDER_NAME = "servers";

    /**
     * 默认
     */
    public static int EVENT_CASE_DEFAULT = 0;
    /**
     * 选中的目录
     */
    public static int EVENT_CASE_FOLDER = 1;
    /**
     * 选中了单个文件
     */
    public static int EVENT_CASE_SINGLE_FILE = 2;
    /**
     * 选中了多个文件
     */
    public static int EVENT_CASE_MULTI_FILE = 3;


}
