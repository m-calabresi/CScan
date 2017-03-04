package com.cala.scanner.classes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URIChecker {
    private static final String URL_REGEX = "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$";
    private static final String HTTP_HEADER = "http://";
    private static final String HTTPS_HEADER = "http://";
    private static final String FTP_HEADER = "ftp://";

    public static boolean isURI(String str) {
        Pattern p = Pattern.compile(URL_REGEX);
        Matcher m = p.matcher(str);

        return m.find();
    }

    public static String toLink(String str) {
        if (!str.startsWith(HTTP_HEADER) &&
                !str.startsWith(HTTPS_HEADER) &&
                !str.startsWith(FTP_HEADER))
            str = HTTP_HEADER + str;

        return str;
    }
}
