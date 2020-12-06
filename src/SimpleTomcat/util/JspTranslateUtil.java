package SimpleTomcat.util;

import SimpleTomcat.catalina.Context;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.jasper.JasperException;
import org.apache.jasper.JspC;

import java.io.File;

/**
 * JspTranslateUtil is to translate .jsp file to .java file and then compile .java file to .class file
 */
public class JspTranslateUtil {
    // Define java key words
    private static final String[] javaKeyWords = { "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long",
            "native", "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile",
            "while" };

    /**
     * compile Jsp file
     * @param context: context
     * @param file: file
     * @throws JasperException: JasperException
     */
    public static void compileJsp(Context context, File file) throws JasperException {
        String subDirectory;
        String path = context.getPath();
        if (path.equals("/")) {
            subDirectory = "_";
        } else {
            subDirectory = StrUtil.subAfter(path, "/", false);
        }

        String workPath = new File(Constant.workFolder, subDirectory).getAbsolutePath() + File.separator;

        String[] args = new String[]{ "-webapp", context.getDocBase().toLowerCase(), "-d", workPath.toLowerCase(), "-compile", };

        JspC jspC = new JspC();
        jspC.setArgs(args);
        jspC.execute(file);
    }

    public static final String makeJavaIdentifier(String identifier) {
        return makeJavaIdentifier(identifier, true);
    }

    public static final String makeJavaIdentifier(String identifier, boolean periodToUnderscore) {
        StringBuilder modifiedIdentifier = new StringBuilder(identifier.length());
        if (!Character.isJavaIdentifierPart(identifier.charAt(0))) {
            modifiedIdentifier.append('_');
        }
        for (int i = 0; i < identifier.length(); i++) {
            char ch = identifier.charAt(i);
            if (Character.isJavaIdentifierPart(ch) && (ch != '_' || !periodToUnderscore)) {
                modifiedIdentifier.append(ch);
            } else if (ch == '.' && periodToUnderscore) {
                modifiedIdentifier.append('_');
            } else {
                modifiedIdentifier.append(mangleChar(ch));
            }
        }

        if (isJavaKeyWord(modifiedIdentifier.toString())) {
            modifiedIdentifier.append('_');
        }

        return modifiedIdentifier.toString();
    }

    public static final String mangleChar(char ch) {
        char[] result = new char[5];
        result[0] = '_';
        result[1] = Character.forDigit((ch >> 12) & 0xf, 16);
        result[2] = Character.forDigit((ch >> 8) & 0xf, 16);
        result[3] = Character.forDigit((ch >> 4) & 0xf, 16);
        result[4] = Character.forDigit(ch & 0xf, 16);
        return new String(result);
    }

    public static boolean isJavaKeyWord(String key) {
        int i = 0;
        int j = javaKeyWords.length;
        while (i < j) {
            int k = (i + j) / 2;
            int result = javaKeyWords[k].compareTo(key);
            if (result == 0) {
                return true;
            }
            if (result < 0) {
                i = k + 1;
            } else {
                j = k;
            }
        }
        return false;
    }

    public static String getServletPath(String uri, String subDirectory) {
        String tempPath = "org/apache/jsp/" + uri;

        File tempFile = FileUtil.file(Constant.workFolder, subDirectory, tempPath);

        String fileNameOnly = tempFile.getName();
        String classFileName = JspTranslateUtil.makeJavaIdentifier(fileNameOnly);

        File servletFile = new File(tempFile.getParent(), classFileName);

        return servletFile.getAbsolutePath();
    }

    public static String getServletClassPath(String uri, String subDirectory) {
        return getServletPath(uri, subDirectory) + ".class";
    }

    public static String getServletJavaPath(String uri, String subDirectory) {
        return getServletPath(uri, subDirectory) + ".java";
    }

    public static String getJspServletClassName(String uri, String subDirectory) {
        File tempFile = FileUtil.file(Constant.workFolder, subDirectory);
        String tempPath = tempFile.getAbsolutePath() + File.separator;
        String servletPath = getServletPath(uri, subDirectory);

        String jsServletClassPath = StrUtil.subAfter(servletPath, tempPath, false);
        String jspServletClassName = StrUtil.replace(jsServletClassPath, File.separator, ".");
        return jspServletClassName;
    }

    public static void main(String[] args) {
        try {
            Context context = new Context("/javaweb", "/Users/zifwang/Desktop/javaweb/web", true, null);
            File file = new File("/Users/zifwang/Desktop/javaweb/web/index.jsp");
            compileJsp(context,file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

