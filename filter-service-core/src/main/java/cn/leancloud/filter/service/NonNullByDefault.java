package cn.leancloud.filter.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.meta.TypeQualifierDefault;
import java.lang.annotation.*;

/**
 * An annotation that signifies the return values, parameters and fields are non-nullable by default
 * leveraging the JSR-305 {@link Nonnull} annotation. Annotate a package with this annotation and
 * annotate nullable return values, parameters and fields with {@link Nullable}.
 */
@Nonnull
@Documented
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.RUNTIME)
@TypeQualifierDefault({ ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
public @interface NonNullByDefault {
}
