package uk.co.palmr.gennaker.layout;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation on a {@link Layout} implementation class so that the
 * annotation processor can discover it at compile time.
 *
 * <p>The processor reads the generic type argument of
 * {@code implements Layout<T>} to learn which type the Layout describes.
 *
 * <pre>{@code
 * @LayoutSpec
 * public final class OrderLayout implements Layout<Order> { ... }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface LayoutSpec {
}
