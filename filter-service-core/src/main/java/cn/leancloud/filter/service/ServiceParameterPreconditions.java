package cn.leancloud.filter.service;

import javax.annotation.Nullable;

import static com.google.common.base.Strings.lenientFormat;

public final class ServiceParameterPreconditions {
    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param paramName  parameter name
     * @param expression a boolean expression
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkParameter(String paramName, boolean expression) {
        if (!expression) {
            throw BadParameterException.invalidParameter(paramName);
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param paramName    parameter name
     * @param expression   a boolean expression
     * @param errorMessage the exception message to use if the check fails; will be converted to a
     *                     string using {@link String#valueOf(Object)}
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkParameter(String paramName, boolean expression, @Nullable Object errorMessage) {
        if (!expression) {
            throw BadParameterException.invalidParameter(paramName, String.valueOf(errorMessage));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param paramName            parameter name
     * @param expression           a boolean expression
     * @param errorMessageTemplate a template for the exception message should the check fail. The
     *                             message is formed by replacing each {@code %s} placeholder in the template with an
     *                             argument. These are matched by position - the first {@code %s} gets {@code
     *                             errorMessageArgs[0]}, etc. Unmatched arguments will be appended to the formatted message in
     *                             square braces. Unmatched placeholders will be left as-is.
     * @param errorMessageArgs     the arguments to be substituted into the message template. Arguments
     *                             are converted to strings using {@link String#valueOf(Object)}.
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkParameter(
            String paramName,
            boolean expression,
            @Nullable String errorMessageTemplate,
            @Nullable Object... errorMessageArgs) {
        if (!expression) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, errorMessageArgs));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(String paramName, boolean b, @Nullable String errorMessageTemplate, char p1) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(String paramName, boolean b, @Nullable String errorMessageTemplate, int p1) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(String paramName, boolean b, @Nullable String errorMessageTemplate, long p1) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b, @Nullable String errorMessageTemplate, @Nullable Object p1) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b, @Nullable String errorMessageTemplate, char p1, char p2) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b, @Nullable String errorMessageTemplate, char p1, int p2) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b, @Nullable String errorMessageTemplate, char p1, long p2) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b, @Nullable String errorMessageTemplate, char p1, @Nullable Object p2) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b, @Nullable String errorMessageTemplate, int p1, char p2) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b, @Nullable String errorMessageTemplate, int p1, int p2) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b, @Nullable String errorMessageTemplate, int p1, long p2) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b, @Nullable String errorMessageTemplate, int p1, @Nullable Object p2) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b, @Nullable String errorMessageTemplate, long p1, char p2) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b, @Nullable String errorMessageTemplate, long p1, int p2) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b, @Nullable String errorMessageTemplate, long p1, long p2) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b, @Nullable String errorMessageTemplate, long p1, @Nullable Object p2) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b, @Nullable String errorMessageTemplate, @Nullable Object p1, char p2) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b, @Nullable String errorMessageTemplate, @Nullable Object p1, int p2) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b, @Nullable String errorMessageTemplate, @Nullable Object p1, long p2) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b, @Nullable String errorMessageTemplate, @Nullable Object p1, @Nullable Object p2) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b,
            @Nullable String errorMessageTemplate,
            @Nullable Object p1,
            @Nullable Object p2,
            @Nullable Object p3) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1, p2, p3));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkParameter(String, boolean, String, Object...)} for details.
     */
    public static void checkParameter(
            String paramName,
            boolean b,
            @Nullable String errorMessageTemplate,
            @Nullable Object p1,
            @Nullable Object p2,
            @Nullable Object p3,
            @Nullable Object p4) {
        if (!b) {
            throw BadParameterException.invalidParameter(paramName, lenientFormat(errorMessageTemplate, p1, p2, p3, p4));
        }
    }

    public static <T extends Object> T checkNotNull(String paramName, @Nullable T obj) {
        if (obj == null) {
            throw BadParameterException.requiredParameter(paramName);
        }

        return obj;
    }
}
