package uk.co.palmr.gennaker.annotation.processor.shape;

import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;
import uk.co.palmr.gennaker.codec.FieldKind;
import uk.co.palmr.gennaker.codec.FieldShape;
import uk.co.palmr.gennaker.codec.TypeShape;
import uk.co.palmr.gennaker.layout.WireName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extracts {@link TypeShape} descriptions from records and {@code @LayoutSpec}
 * annotated classes. Used by the annotation processor to build the
 * reachable-type map that codecs need for generating layout helpers and
 * schema entries.
 */
public final class ShapeExtractor {

    private static final Set<String> PRIMITIVE_TYPES = Set.of(
            "boolean", "byte", "short", "int", "long", "float", "double", "char"
    );

    private static final Set<String> BOXED_TYPES = Set.of(
            "java.lang.Boolean", "java.lang.Byte", "java.lang.Short",
            "java.lang.Integer", "java.lang.Long", "java.lang.Float",
            "java.lang.Double", "java.lang.Character"
    );

    private static final Map<String, String> WRITE_METHOD_TO_TYPE = Map.ofEntries(
            Map.entry("writeBoolean", "boolean"),
            Map.entry("writeByte", "byte"),
            Map.entry("writeShort", "short"),
            Map.entry("writeInt", "int"),
            Map.entry("writeLong", "long"),
            Map.entry("writeFloat", "float"),
            Map.entry("writeDouble", "double"),
            Map.entry("writeChar", "char"),
            Map.entry("writeString", "java.lang.String"),
            Map.entry("writeObject", "object"),
            Map.entry("writeList", "list"),
            Map.entry("writeMap", "map")
    );

    private static final Map<String, String> READ_METHOD_TO_TYPE = Map.ofEntries(
            Map.entry("readBoolean", "boolean"),
            Map.entry("readByte", "byte"),
            Map.entry("readShort", "short"),
            Map.entry("readInt", "int"),
            Map.entry("readLong", "long"),
            Map.entry("readFloat", "float"),
            Map.entry("readDouble", "double"),
            Map.entry("readChar", "char"),
            Map.entry("readString", "java.lang.String"),
            Map.entry("readObject", "object"),
            Map.entry("readList", "list"),
            Map.entry("readMap", "map")
    );

    private final ProcessingEnvironment processingEnv;
    private final Trees trees;

    /**
     * LayoutSpec class element keyed by the fully-qualified Java type it
     * describes.
     */
    private final Map<String, TypeElement> layoutsByType = new HashMap<>();

    /** Cached shapes keyed by fully-qualified Java type. */
    private final Map<String, TypeShape> shapeCache = new LinkedHashMap<>();

    public ShapeExtractor(final ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        Trees treesOrNull;
        try {
            treesOrNull = Trees.instance(processingEnv);
        } catch (final IllegalArgumentException e) {
            treesOrNull = null;
        }
        this.trees = treesOrNull;
    }

    /**
     * Scan for {@code @LayoutSpec} classes in this round and register them.
     */
    public void discoverLayouts(final RoundEnvironment roundEnv) {
        final var layoutSpecFqn = "uk.co.palmr.gennaker.layout.LayoutSpec";
        final var layoutSpecElement = processingEnv.getElementUtils().getTypeElement(layoutSpecFqn);
        if (layoutSpecElement == null) {
            return;
        }
        for (final var element : roundEnv.getElementsAnnotatedWith(layoutSpecElement)) {
            if (element.getKind() != ElementKind.CLASS) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "@LayoutSpec can only be applied to classes", element);
                continue;
            }
            final var typeElement = (TypeElement) element;
            final var targetType = resolveLayoutTargetType(typeElement);
            if (targetType == null) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "@LayoutSpec class must implement Layout<T> for some type T", typeElement);
                continue;
            }
            layoutsByType.put(targetType, typeElement);
        }
    }

    /**
     * Collect all reachable types from the given topic method parameters.
     * Returns a map keyed by fully-qualified Java type name.
     */
    public Map<String, TypeShape> collectReachableTypes(final List<ExecutableElement> methods) {
        final Map<String, TypeShape> result = new LinkedHashMap<>();
        for (final var method : methods) {
            for (final var param : method.getParameters()) {
                collectFromType(param.asType(), result);
            }
        }
        return result;
    }

    /**
     * Returns true if the type string names a Java primitive.
     */
    public static boolean isPrimitive(final String type) {
        return PRIMITIVE_TYPES.contains(type);
    }

    /**
     * Returns true if the type string is java.lang.String.
     */
    public static boolean isString(final String type) {
        return "java.lang.String".equals(type);
    }

    /**
     * Returns true if the type is a primitive or String.
     */
    public static boolean isPrimitiveOrString(final String type) {
        return isPrimitive(type) || isString(type);
    }

    private void collectFromType(final TypeMirror type, final Map<String, TypeShape> result) {
        if (!type.getKind().isPrimitive() && !isPrimitiveOrString(type.toString())) {
            if (!(type instanceof DeclaredType dt) || !collectFromDeclaredType(dt, result)) {
                collectUserType(type, result);
            }
        }
    }

    private boolean collectFromDeclaredType(final DeclaredType dt, final Map<String, TypeShape> result) {
        final var erasure = processingEnv.getTypeUtils().erasure(dt).toString();
        if ("java.util.List".equals(erasure)) {
            if (!dt.getTypeArguments().isEmpty()) {
                collectFromType(dt.getTypeArguments().get(0), result);
            }
            return true;
        }
        if ("java.util.Map".equals(erasure)) {
            if (dt.getTypeArguments().size() == 2) {
                collectFromType(dt.getTypeArguments().get(0), result);
                collectFromType(dt.getTypeArguments().get(1), result);
            }
            return true;
        }
        return false;
    }

    private void collectUserType(final TypeMirror type, final Map<String, TypeShape> result) {
        final var typeStr = type.toString();
        if (result.containsKey(typeStr)) {
            // already collected
        } else if (shapeCache.containsKey(typeStr)) {
            result.put(typeStr, shapeCache.get(typeStr));
        } else {
            final var shape = extractShape(type);
            if (shape != null) {
                shapeCache.put(typeStr, shape);
                result.put(typeStr, shape);
                for (final var field : shape.fields()) {
                    if (field.kind() == FieldKind.OBJECT && field.nestedShape() != null) {
                        result.put(field.javaType(), field.nestedShape());
                    }
                }
            }
        }
    }

    private TypeShape extractShape(final TypeMirror type) {
        if (!(type instanceof DeclaredType dt)) {
            return null;
        }
        return extractShapeFromElement((TypeElement) dt.asElement());
    }

    private TypeShape extractShapeFromElement(final TypeElement element) {
        final var fqn = element.getQualifiedName().toString();
        final var layoutClass = layoutsByType.get(fqn);

        if (layoutClass != null) {
            return extractShapeFromLayout(layoutClass, element);
        } else if (element.getKind() == ElementKind.RECORD) {
            return extractShapeFromRecord(element);
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "Type '" + fqn + "' is used in a @Topic method but is not a record and has no " +
                        "@LayoutSpec implementation. Either make it a record or write a " +
                        "@LayoutSpec class implementing Layout<" + element.getSimpleName() + ">.",
                element);
        return null;
    }

    /**
     * Auto-derive shape from a record's components.
     */
    private TypeShape extractShapeFromRecord(final TypeElement recordElement) {
        final var fqn = recordElement.getQualifiedName().toString();
        if (shapeCache.containsKey(fqn)) {
            return shapeCache.get(fqn);
        }

        final var wireName = resolveWireName(recordElement);
        final var components = recordElement.getRecordComponents();
        final List<FieldShape> fields = new ArrayList<>(components.size());

        for (final var comp : components) {
            fields.add(fieldShapeFromComponent(comp));
        }

        final var shape = new TypeShape(wireName, fqn, fields);
        shapeCache.put(fqn, shape);
        return shape;
    }

    private FieldShape fieldShapeFromComponent(final RecordComponentElement comp) {
        final var wireName = resolveComponentWireName(comp);
        final var javaName = comp.getSimpleName().toString();
        final var typeMirror = comp.asType();
        return buildFieldShape(wireName, javaName, typeMirror);
    }

    private FieldShape buildFieldShape(final String wireName, final String javaName, final TypeMirror typeMirror) {
        final var typeStr = typeMirror.toString();

        if (typeMirror.getKind().isPrimitive() || !(typeMirror instanceof DeclaredType dt)) {
            return new FieldShape(wireName, javaName, FieldKind.PRIMITIVE, typeStr, null, null, null);
        } else if (isString(typeStr)) {
            return new FieldShape(wireName, javaName, FieldKind.STRING, typeStr, null, null, null);
        }
        return buildFieldShapeFromDeclared(wireName, javaName, dt, typeStr);
    }

    private FieldShape buildFieldShapeFromDeclared(final String wireName,
                                                   final String javaName,
                                                   final DeclaredType dt,
                                                   final String typeStr) {
        final var erasure = processingEnv.getTypeUtils().erasure(dt).toString();

        if ("java.util.List".equals(erasure) && !dt.getTypeArguments().isEmpty()) {
            return buildListFieldShape(wireName, javaName, dt);
        }
        if ("java.util.Map".equals(erasure) && dt.getTypeArguments().size() == 2) {
            return buildMapFieldShape(wireName, javaName, dt);
        }
        final var nestedShape = extractShape(dt);
        return new FieldShape(wireName, javaName, FieldKind.OBJECT, typeStr, nestedShape, null, null);
    }

    private FieldShape buildListFieldShape(final String wireName, final String javaName, final DeclaredType dt) {
        final var elemType = dt.getTypeArguments().get(0);
        final var elemTypeStr = elemType.toString();
        TypeShape nestedShape = null;
        if (!isPrimitiveOrString(elemTypeStr) && !BOXED_TYPES.contains(elemTypeStr)) {
            nestedShape = extractShape(elemType);
        }
        return new FieldShape(wireName, javaName, FieldKind.LIST, elemTypeStr, nestedShape, null, null);
    }

    private FieldShape buildMapFieldShape(final String wireName, final String javaName, final DeclaredType dt) {
        final var keyType = dt.getTypeArguments().get(0);
        final var valType = dt.getTypeArguments().get(1);
        final var keyTypeStr = keyType.toString();
        final var valTypeStr = valType.toString();

        if (!isPrimitiveOrString(keyTypeStr) && !BOXED_TYPES.contains(keyTypeStr)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Map key type must be a primitive or String, but found '" + keyTypeStr +
                            "'. Model composite keys as List<EntryRecord> instead.");
        }

        TypeShape valShape = null;
        if (!isPrimitiveOrString(valTypeStr) && !BOXED_TYPES.contains(valTypeStr)) {
            valShape = extractShape(valType);
        }
        return new FieldShape(wireName, javaName, FieldKind.MAP, valTypeStr, valShape, keyTypeStr, null);
    }

    /**
     * Extract shape from a hand-written Layout class by walking its encode
     * method's AST.
     */
    private TypeShape extractShapeFromLayout(final TypeElement layoutClass, final TypeElement targetType) {
        if (trees == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@LayoutSpec requires the javac compiler (com.sun.source.util.Trees not available). " +
                            "Hand-written Layouts are not supported on this compiler.",
                    layoutClass);
            return null;
        }

        final var fqn = targetType.getQualifiedName().toString();
        if (shapeCache.containsKey(fqn)) {
            return shapeCache.get(fqn);
        }

        return doExtractShapeFromLayout(layoutClass, targetType, fqn);
    }

    private TypeShape doExtractShapeFromLayout(final TypeElement layoutClass,
                                               final TypeElement targetType,
                                               final String fqn) {
        final var wireName = resolveWireName(targetType);
        final var encodeFields = extractEncodeFields(layoutClass);
        final var decodeFields = (encodeFields != null) ? extractDecodeFields(layoutClass) : null;

        if (encodeFields == null || decodeFields == null
                || !validateFieldsMatch(encodeFields, decodeFields, layoutClass)) {
            return null;
        }

        final var shape = new TypeShape(wireName, fqn, encodeFields);
        shapeCache.put(fqn, shape);
        return shape;
    }

    private List<FieldShape> extractEncodeFields(final TypeElement layoutClass) {
        final var encodeMethods = ElementFilter.methodsIn(layoutClass.getEnclosedElements()).stream()
                .filter(m -> "encode".equals(m.getSimpleName().toString()))
                .toList();
        if (encodeMethods.isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@LayoutSpec class must have an 'encode' method", layoutClass);
            return null;
        }
        return extractFieldsFromEncodeAST(encodeMethods.get(0), layoutClass);
    }

    private List<FieldShape> extractDecodeFields(final TypeElement layoutClass) {
        final var decodeMethods = ElementFilter.methodsIn(layoutClass.getEnclosedElements()).stream()
                .filter(m -> "decode".equals(m.getSimpleName().toString()))
                .toList();
        if (decodeMethods.isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@LayoutSpec class must have a 'decode' method", layoutClass);
            return null;
        }
        return extractFieldsFromDecodeAST(decodeMethods.get(0), layoutClass);
    }

    private List<FieldShape> extractFieldsFromEncodeAST(final ExecutableElement method,
                                                        final TypeElement layoutClass) {
        final var methodTree = trees.getTree(method);
        if (methodTree == null || methodTree.getBody() == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Cannot read AST of encode method", layoutClass);
            return null;
        }

        final List<FieldShape> fields = new ArrayList<>();
        for (final var stmt : methodTree.getBody().getStatements()) {
            final var field = parseEncodeStatement(stmt, layoutClass);
            if (field == null) {
                return null;
            }
            fields.add(field);
        }
        return fields;
    }

    private FieldShape parseEncodeStatement(final com.sun.source.tree.StatementTree stmt,
                                            final TypeElement layoutClass) {
        if (!(stmt instanceof ExpressionStatementTree exprStmt)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Layout encode body must be a linear sequence of sink.writeX() calls. " +
                            "Unsupported statement: " + stmt.getKind(),
                    layoutClass);
            return null;
        }
        final var expr = exprStmt.getExpression();
        if (!(expr instanceof MethodInvocationTree invocation)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Layout encode body must be a linear sequence of sink.writeX() calls. " +
                            "Unsupported expression: " + expr.getKind(),
                    layoutClass);
            return null;
        }
        return parseWriteInvocation(invocation, layoutClass);
    }

    private FieldShape parseWriteInvocation(final MethodInvocationTree invocation,
                                            final TypeElement layoutClass) {
        final var methodName = extractMethodName(invocation);
        if (methodName == null || !WRITE_METHOD_TO_TYPE.containsKey(methodName)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Layout encode body: unrecognised call '" + methodName + "'. " +
                            "Only sink.writeX() calls are allowed.",
                    layoutClass);
            return null;
        }

        final var fieldName = extractStringLiteral(invocation.getArguments().get(0));
        if (fieldName == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Layout encode body: first argument to sink." + methodName +
                            "() must be a string literal (the field name).",
                    layoutClass);
            return null;
        }

        return buildFieldShapeFromMethodType(fieldName, WRITE_METHOD_TO_TYPE.get(methodName));
    }

    private List<FieldShape> extractFieldsFromDecodeAST(final ExecutableElement method,
                                                        final TypeElement layoutClass) {
        final var methodTree = trees.getTree(method);
        if (methodTree == null || methodTree.getBody() == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Cannot read AST of decode method", layoutClass);
            return null;
        }

        final List<FieldShape> fields = new ArrayList<>();
        for (final var stmt : methodTree.getBody().getStatements()) {
            if (stmt instanceof ReturnTree) {
                continue;
            }
            final var field = parseDecodeStatement(stmt, layoutClass);
            if (field == null) {
                return null;
            }
            fields.add(field);
        }
        return fields;
    }

    private FieldShape parseDecodeStatement(final com.sun.source.tree.StatementTree stmt,
                                            final TypeElement layoutClass) {
        if (!(stmt instanceof VariableTree varTree)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Layout decode body must be local variable declarations " +
                            "with source.readX() initialisers and a return statement. " +
                            "Unsupported statement: " + stmt.getKind(),
                    layoutClass);
            return null;
        }

        final var init = varTree.getInitializer();
        if (!(init instanceof MethodInvocationTree invocation)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Layout decode body: each variable must be initialised with a " +
                            "source.readX() call.",
                    layoutClass);
            return null;
        }
        return parseReadInvocation(invocation, layoutClass);
    }

    private FieldShape parseReadInvocation(final MethodInvocationTree invocation,
                                           final TypeElement layoutClass) {
        final var methodName = extractMethodName(invocation);
        if (methodName == null || !READ_METHOD_TO_TYPE.containsKey(methodName)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Layout decode body: unrecognised call '" + methodName + "'. " +
                            "Only source.readX() calls are allowed.",
                    layoutClass);
            return null;
        }

        final var fieldName = extractStringLiteral(invocation.getArguments().get(0));
        if (fieldName == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Layout decode body: first argument to source." + methodName +
                            "() must be a string literal (the field name).",
                    layoutClass);
            return null;
        }

        return buildFieldShapeFromMethodType(fieldName, READ_METHOD_TO_TYPE.get(methodName));
    }

    private static FieldShape buildFieldShapeFromMethodType(final String fieldName, final String mappedType) {
        final FieldKind kind = switch (mappedType) {
            case "object" -> FieldKind.OBJECT;
            case "list" -> FieldKind.LIST;
            case "map" -> FieldKind.MAP;
            case "java.lang.String" -> FieldKind.STRING;
            default -> FieldKind.PRIMITIVE;
        };
        final var javaType = (kind == FieldKind.OBJECT || kind == FieldKind.LIST || kind == FieldKind.MAP)
                ? "java.lang.Object" : mappedType;
        return new FieldShape(fieldName, kind, javaType, null);
    }

    private boolean validateFieldsMatch(final List<FieldShape> encodeFields,
                                        final List<FieldShape> decodeFields,
                                        final TypeElement layoutClass) {
        if (encodeFields.size() != decodeFields.size()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@LayoutSpec encode has " + encodeFields.size() + " fields but decode has " +
                            decodeFields.size() + " fields. They must agree on the same fields in the same order.",
                    layoutClass);
            return false;
        }
        for (int i = 0; i < encodeFields.size(); i++) {
            final var enc = encodeFields.get(i);
            final var dec = decodeFields.get(i);
            if (!enc.name().equals(dec.name()) || enc.kind() != dec.kind()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "@LayoutSpec field mismatch at position " + i + ": encode has '" +
                                enc.name() + "' (" + enc.kind() + ") but decode has '" +
                                dec.name() + "' (" + dec.kind() + "). " +
                                "Fields must appear in the same order with the same names and types.",
                        layoutClass);
                return false;
            }
        }
        return true;
    }

    private static String extractMethodName(final MethodInvocationTree invocation) {
        final var select = invocation.getMethodSelect();
        if (select instanceof MemberSelectTree memberSelect) {
            return memberSelect.getIdentifier().toString();
        }
        if (select instanceof IdentifierTree id) {
            return id.getName().toString();
        }
        return null;
    }

    private static String extractStringLiteral(final ExpressionTree expr) {
        if (expr instanceof LiteralTree literal && literal.getValue() instanceof String s) {
            return s;
        }
        return null;
    }

    private String resolveLayoutTargetType(final TypeElement layoutClass) {
        final var layoutInterfaceFqn = "uk.co.palmr.gennaker.layout.Layout";
        for (final var iface : layoutClass.getInterfaces()) {
            if (iface instanceof DeclaredType dt) {
                final var ifaceElement = (TypeElement) dt.asElement();
                if (layoutInterfaceFqn.equals(ifaceElement.getQualifiedName().toString())
                        && !dt.getTypeArguments().isEmpty()) {
                    return dt.getTypeArguments().get(0).toString();
                }
            }
        }
        return null;
    }

    private static String resolveWireName(final TypeElement element) {
        final var wireNameAnnotation = element.getAnnotation(WireName.class);
        if (wireNameAnnotation != null) {
            return wireNameAnnotation.value();
        }
        return element.getSimpleName().toString();
    }

    private static String resolveComponentWireName(final RecordComponentElement comp) {
        final var wireNameAnnotation = comp.getAnnotation(WireName.class);
        if (wireNameAnnotation != null) {
            return wireNameAnnotation.value();
        }
        return comp.getSimpleName().toString();
    }
}
