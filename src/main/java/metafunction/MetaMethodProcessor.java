package metafunction;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
import java.util.List;
import java.util.Map;
import java.util.Set;


@SupportedAnnotationTypes("metafunction.MetaMethod")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MetaMethodProcessor extends AbstractProcessor {

    static class CompliationUnit {
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
            writer.annotation("SuppressWarnings(\"unchecked\")");
            writer.beginType(simpleName+"<R>", "abstract class", 0);

            for (MetaMethodDef methodDef : methods.values()) {
                methodDef.compile(writer);
            }

            writer.endType();
        }
    }

    static class MetaMethodDef {
        final ExecutableElement definition;
        final ProcessingEnvironment environment;

        MetaMethodDef(ExecutableElement definition, ProcessingEnvironment environment) {
            this.definition = definition;
            this.environment = environment;
        }

        public void compile(JavaWriter writer) throws IOException {
            List<? extends VariableElement> parameters = definition.getParameters();
            String[] paramDefs = new String[parameters.size() * 2];
            for(int i = 0; i < parameters.size(); i++) {
                VariableElement param = parameters.get(i);
                TypeMirror paramType = param.asType();
                Name paramName = param.getSimpleName();
                paramDefs[i] = paramType.toString();
                paramDefs[i + 1] = paramName.toString();
            }
            String methodName = definition.getSimpleName().toString();
            String returnType = definition.getReturnType().toString();
            writer.beginMethod(returnType, methodName, Modifier.ABSTRACT, "metafunction.MetaFunction<R>", "fn");
            writer.endMethod();

            List<String> genericFunctionParams = Lists.newArrayList();
            List<String> applyArgs = Lists.newArrayList();
            for(int i = 0; i <= 10; i++) {
                writer.beginMethod(genericFunctionParams.isEmpty()? returnType: ("<" + Joiner.on(",").join(genericFunctionParams) +"> " +returnType), methodName, Modifier.PUBLIC, "metafunction.Functions.F"+i+"<"+ (genericFunctionParams.isEmpty()?"":Joiner.on(",").join(genericFunctionParams) +",") +"R>", "fn");
                writer.statement("%s %s((metafunction.MetaFunction<R>) args -> fn.apply(" + Joiner.on(",").join(applyArgs) + "))", returnType.equals("void") ? "" : "return", methodName);
                writer.endMethod();
                genericFunctionParams.add("T"+i);
                applyArgs.add("(T"+i+") args[" + i +"]");
            }
        }
    }

    CompliationUnit compliationUnit = new CompliationUnit();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        try {
            for (TypeElement annotation : annotations) {
                Set<? extends Element> annotatedMethods = env.getElementsAnnotatedWith(MetaMethod.class);
                for (Element method : annotatedMethods) {
                    compliationUnit.addMethodDef(method, annotation, processingEnv);
                }
            }

            if(env.processingOver()) {
                compliationUnit.compile(processingEnv.getFiler());
            }
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
