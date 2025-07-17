package org.ace.util;

import java.io.File;

public class FolderUtil {

    public static String getUserDir() {
        return System.getProperty("user.dir");
    }

    public static boolean createFolder(String folderName) {
        File file = new File(FolderUtil.getUserDir() + "/" + folderName);
        if(!file.exists()) {
            return file.mkdir();
        }
        // the folder exists
        return true;
    }
}
