package org.hanxingfeng.blog.Entity;

import java.time.LocalDate;
import java.util.List;

public final class SystemConstants {
    public static final List<String> mdFilePath = List.of(
            "C:\\Users\\35560\\Desktop\\文件夹\\mysql\\java\\",
            "C:\\Users\\35560\\Desktop\\文件夹\\mysql\\mysql\\"
            // "E:\\obsdian\\blog\\src\\main\\resources\\zip"
    );
    public static final LocalDate START_TIME = LocalDate.of(2026,5,8);
    // TODO：密钥放入配置文件
    public static final String SECRET = "DemoNCloudClayHanXingFengDemoNCloudClayHanXingFeng";

    public static final String UPLOAD_DIR = "E:\\obsdian\\blog\\src\\main\\resources\\images\\";

    public static final String POST_DIR = "E:\\obsdian\\blog\\src\\main\\resources\\blog\\";

}
