package uk.co.palmr.gennaker.layout;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the on-the-wire name for a type or a field. When absent the
 * Java identifier is used.
 *
 * <p>Applicable scopes:
 * <ul>
 *   <li>On a record — overrides the type's wire name (e.g. SBE composite
 *       name).</li>
 *   <li>On a record component — overrides a field's wire name.</li>
 *   <li>On a parameter referenced inside a Layout body — overrides that
 *       field's wire name.</li>
 * </ul>
 *
 * <p>Useful for renaming a Java identifier without breaking the wire
 * contract.
 */
@Target({ElementType.TYPE, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.SOURCE)
public @interface WireName {
    /**
     * The wire name to use in place of the Java identifier.
     *
     * @return the override wire name
     */
    String value();
}
