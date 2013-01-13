package metafunction;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;


@SupportedAnnotationTypes("metafunction.MetaMethod")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MetaMethodProcessor extends AbstractProcessor {

    static class CompilationUnit {
        final Map<Name, MetaClassDef> classDefs = Maps.newHashMap();

        public void addMethodDef(Element method, TypeElement origin, ProcessingEnvironment environment) {
            TypeElement clazz = (TypeElement) method.getEnclosingElement();
            Name className = clazz.getQualifiedName();
            Name methodName = method.getSimpleName();
            if(!classDefs.containsKey(className)) {
                classDefs.put(className, new MetaClassDef(clazz, origin));
            }
            classDefs.get(className).methods.put(methodName, new MetaMethodDef((ExecutableElement) method, environment));
        }

        public void compile(Filer filer) throws IOException {
            for (Map.Entry<Name, MetaClassDef> classDefEntry : classDefs.entrySet()) {
                MetaClassDef classDef = classDefEntry.getValue();
                JavaFileObject sourceFile = filer.createSourceFile(classDef.fullyQualifiedName, classDef.annotation);
                try(JavaWriter writer = new JavaWriter(sourceFile.openWriter())) {
                    classDef.compile(writer);
                }
            }
        }
    }

    static class MetaClassDef {
        final TypeElement type;
        final TypeElement annotation;
        final String simpleName;
        final String packageName;
        final String fullyQualifiedName;
        final Map<Name, MetaMethodDef> methods = Maps.newHashMap();

        MetaClassDef(TypeElement type, TypeElement annotation) {
            this.type = type;
            this.annotation = annotation;
            Element classPackage = type.getEnclosingElement();
            while(classPackage.getKind().equals(ElementKind.CLASS)) {
                classPackage = classPackage.getEnclosingElement();
            }
            this.simpleName = type.getSimpleName().toString() + "_MetaFunction";
            this.fullyQualifiedName = ""+classPackage +"."+ this.simpleName;
            this.packageName = classPackage.toString();
        }

        public void compile(JavaWriter writer) throws IOException {
            writer.addPackage(packageName);
            writer.addImport(MetaFunction.class);
            writer.annotation("SuppressWarnings(\"unchecked\")");
            writer.beginType(simpleName+"<R>", "abstract class", 0);
            for (MetaMethodDef methodDef : methods.values()) {
                methodDef.compile(writer);
            }
            writer.endType();
        }
    }

    static class MetaMethodDef {
        static final Joiner JOINER = Joiner.on(",");
        final ExecutableElement definition;
        final ProcessingEnvironment environment;

        MetaMethodDef(ExecutableElement definition, ProcessingEnvironment environment) {
            this.definition = definition;
            this.environment = environment;
        }

        public void compile(JavaWriter writer) throws IOException {
            List<? extends VariableElement> parameters = definition.getParameters();
            String[] paramDefs = new String[parameters.size() * 2];
            List<String>delegateArgs = Lists.newArrayList();
            int metaParam = -1;
            int currentParamIndex = 0;
            Set<String> paramNames = Sets.newHashSet();
            for(VariableElement param : parameters) {
                TypeMirror paramType = param.asType();
                Name paramName = param.getSimpleName();
                paramNames.add(paramName.toString());
                if(paramType.toString().startsWith("metafunction.MetaFunction")) {
                    paramDefs[currentParamIndex] = "MetaFunction<R>";
                    metaParam = currentParamIndex;
                } else {
                    paramDefs[currentParamIndex] = paramType.toString();
                }
                paramDefs[currentParamIndex + 1] = paramName.toString();
                delegateArgs.add(paramName.toString());
                currentParamIndex += 2;
            }
            if(definition.isVarArgs()) {
                paramDefs[paramDefs.length -2] = paramDefs[paramDefs.length -2].replaceAll("\\[]$", "...");
            }
            String methodName = definition.getSimpleName().toString();
            String returnType = definition.getReturnType().toString();
            writer.beginMethod(returnType, methodName, Modifier.ABSTRACT, paramDefs);
            writer.endMethod();

            List<String> genericFunctionParams = Lists.newArrayList();
            List<String> applyArgs = Lists.newArrayList();
            String argsVariableName = "args";
            int variableNameSuffix = 0;
            while(paramNames.contains(argsVariableName)) {
                argsVariableName = "args" + variableNameSuffix++;
            }
            for(int i = 0; i <= 10; i++) {
                String functionResultGenerics = "<"+ (genericFunctionParams.isEmpty()?"": JOINER.join(genericFunctionParams) +",") +"R>";
                String methodResultGenerics = genericFunctionParams.isEmpty()?"" : ("<" + JOINER.join(genericFunctionParams) +"> ");
                paramDefs[metaParam] =  "Functions.F"+i+functionResultGenerics;
                delegateArgs.set(metaParam / 2, String.format("MetaFunction.of(%s -> %s.apply(%s))",
                        argsVariableName,
                        paramDefs[metaParam + 1],
                        JOINER.join(applyArgs)));

                writer.beginMethod(methodResultGenerics + " " + returnType, methodName, Modifier.PUBLIC, paramDefs);
                writer.statement("%s %s(%s)",
                        returnType.equals("void") ? "" : "return",
                        methodName,
                        JOINER.join(delegateArgs));
                writer.endMethod();
                genericFunctionParams.add("T"+i);
                applyArgs.add("(T"+i+") " + argsVariableName + "[" + i +"]");
            }
        }
    }

    CompilationUnit compilationUnit = new CompilationUnit();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        try {
            for (TypeElement annotation : annotations) {
                Set<? extends Element> annotatedMethods = env.getElementsAnnotatedWith(MetaMethod.class);
                for (Element method : annotatedMethods) {
                    compilationUnit.addMethodDef(method, annotation, processingEnv);
                }
            }

            if(env.processingOver()) {
                compilationUnit.compile(processingEnv.getFiler());
            }
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
