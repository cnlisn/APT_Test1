package com.lisn.annotationcompiler;

import com.google.auto.service.AutoService;
import com.lisn.annotationlib.AutoBundle;
import com.lisn.annotationlib.Route;
import com.lisn.annotationlib.Test;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

//注册APT
@AutoService(Processor.class)
//指定apt支持的java版本
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class RouteProcessor extends AbstractProcessor {
    private Messager messager;
    private Filer filer;
    private String moduleName;
    private Elements elementUtils;
    private Types typeUtils;

    /**
     * 初始化操作
     *
     *   工具方法	        功能
     *   getElementUtils()	返回实现Elements接口的对象，用于操作元素的工具类
     *   getFiler()	        返回实现Filer接口的对象，用于创建文件、类和辅助文件
     *   getMessager()	    返回实现Messager接口的对象，用于报告错误信息、警告提醒
     *   getOptions()	    返回指定的参数选项，可在Gradle文件配置
     *   getTypeUtils()	    返回实现Types接口的对象，用于操作类型的工具类
     *
     * @param processingEnvironment
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();
        elementUtils = processingEnvironment.getElementUtils();
        typeUtils = processingEnvironment.getTypeUtils();
        moduleName = processingEnvironment.getOptions().get("route_module_name");
        loggerInfo("moduleName = " + moduleName);
    }

    /**
     * 设置注解处理器需要处理的注解类型
     *
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotataions = new HashSet<String>();
        annotataions.add(Route.class.getCanonicalName());
        return annotataions;
    }

    public void loggerInfo(String msg) {
        messager.printMessage(Diagnostic.Kind.NOTE, msg);
    }

    /**
     * 指定java版本
     *
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 注解处理的核心方法
     *
     * @param set              返回所有当前注解处理器需要处理的Annotation.
     * @param roundEnvironment 表示当前或是之前的运行环境，可以通过该对象查找到注解。
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set != null && !set.isEmpty()) {

            parseTestAnnotation(set, roundEnvironment);
            processTest(set, roundEnvironment);
            processAutoBundle(set, roundEnvironment);

            loggerInfo("process start");
            StringBuilder printInfo = new StringBuilder();
            Set<? extends Element> routeElements = roundEnvironment.getElementsAnnotatedWith(Route.class);
            try {
                if (routeElements != null && routeElements.size() > 0) {
                    printInfo.append(routeElements.size() + "个文件加了@Route注解！");
                }
            } catch (Exception e) {
                loggerInfo(e.getMessage());
            }
            //构建参数
            ParameterSpec msg = ParameterSpec.builder(String.class, "msg")
                    .build();
            //构建方法
            MethodSpec method = MethodSpec.methodBuilder("inject")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(void.class)
                    .addParameter(msg)
                    .addStatement("$T.out.println($S+msg)", System.class, printInfo.toString())
                    .build();
            //构建类
            TypeSpec helloWorld = TypeSpec.classBuilder("InjectHelper")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(method)
                    .build();
            //构建文件并指定生成文件目录
            JavaFile javaFile = JavaFile.builder("com.lisn.annotation", helloWorld)
                    .build();
            loggerInfo("process end");
            try {
                //把类、方法、参数等信息写入文件
                javaFile.writeTo(filer);
            } catch (IOException e) {
                loggerInfo("process exception");
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private void processAutoBundle(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        String className = "";
        String packageName = "";
        List<FieldHolder> fields = new ArrayList();
        for (Element element : roundEnvironment.getElementsAnnotatedWith(AutoBundle.class)) {
            if (element.getKind() == ElementKind.FIELD) {
                // 可以安全地进行强转，将Element对象转换为一个VariableElement对象
                VariableElement variableElement = (VariableElement) element;
                // 获取变量所在类的信息TypeElement对象
                TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
                // 获取类名
                className = typeElement.getSimpleName().toString();
                // 获取包名
                packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
                // 获取变量上的注解信息
                AutoBundle autoBundle = variableElement.getAnnotation(AutoBundle.class);
                boolean require = autoBundle.require();
                // 获取变量名
                Name name = variableElement.getSimpleName();
                // 获取变量类型
                TypeMirror type = variableElement.asType();

                fields.add(new FieldHolder(name.toString(), type));
            }
        }

        //创建类
        TypeSpec.Builder contentBuilder = TypeSpec.classBuilder(className + "FastBundle");
        for (FieldHolder field : fields) {
            //创建变量
            FieldSpec f = FieldSpec.builder(ClassName.get(field.getType()), field.getName(), Modifier.PRIVATE).build();
            //添加变量
            contentBuilder.addField(f);
        }

        /**
         *  public Intent build(Context context) {
         *     Intent intent = new Intent(context, TestActivity.class);
         *     intent.putExtra("id", id);
         *     intent.putExtra("name", name);
         *     intent.putExtra("is", is);
         *     if (!(context instanceof Activity)) {
         *     intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         *     }
         *     return intent;
         *   }
         */
        MethodSpec.Builder buildMethodBuilder = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get("android.content", "Intent"));
        buildMethodBuilder.addParameter(ClassName.get("android.content", "Context"), "context");
        buildMethodBuilder.addStatement(String.format("Intent intent = new Intent(context, %s.class)", className));
        for (FieldHolder field : fields) {
            buildMethodBuilder.addStatement(String.format("intent.putExtra(\"%s\", %s)", field.getName(), field.getName()));
        }
        buildMethodBuilder.addCode("if (!(context instanceof $T)) {\n", ClassName.get("android.app", "Activity"));
        buildMethodBuilder.addStatement("intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)");
        buildMethodBuilder.addCode("}\n");
        buildMethodBuilder.addStatement("return intent");
        contentBuilder.addMethod(buildMethodBuilder.build());

        /**
         *  public void launch(Context context) {
         *     context.startActivity(build(context));
         *   }
         */
        MethodSpec.Builder launch = MethodSpec.methodBuilder("launch");
        launch.addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("android.content", "Context"), "context")
                .returns(TypeName.VOID);
        launch.addStatement("context.startActivity($N(context))",buildMethodBuilder.build() );
        contentBuilder.addMethod(launch.build());

        /**
         * public void launchForResult(int requestCode, Activity activity) {
         *     activity.startActivityForResult(build(activity),requestCode);
         *   }
         */
        MethodSpec.Builder launchForResult = MethodSpec.methodBuilder("launchForResult");
        launchForResult.addModifiers(Modifier.PUBLIC)
                .addParameter(int.class, "requestCode")
                .addParameter(ClassName.get("android.app", "Activity"), "activity")
                .returns(TypeName.VOID);
        launchForResult.addStatement("activity.startActivityForResult($N(activity),requestCode)",buildMethodBuilder.build() );
        contentBuilder.addMethod(launchForResult.build());

        JavaFile javaFile = JavaFile.builder(packageName, contentBuilder.build()).build();

        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 注: moduleName = app
     * 注: element--------------CLASS-------------------------------
     * 注: element:Interfaces = com.lisn.apt_test1.TestInterface
     * 注: element:TypeParameters = T
     * 注: element:packageElement = com.lisn.apt_test1
     * 注: element:value = this is class TestClass
     * 注: element:packageName = com.lisn.apt_test1
     * 注: element:SimpleName = TestClass
     * 注: element:asType = com.lisn.apt_test1.TestClass<T>
     * 注: element:KindName = CLASS
     * 注: element:EnclosingElementKindName = PACKAGE
     * 注: element:Modifiers = [public]
     * 注: element--------------FIELD-------------------------------
     * 注: element:field Name = name
     * 注: element:type = java.lang.String
     * 注: element:typeSimpleName = String
     * 注: element:value = this is local field name
     * 注: element:packageName = com.lisn.apt_test1
     * 注: element:SimpleName = name
     * 注: element:asType = java.lang.String
     * 注: element:KindName = FIELD
     * 注: element:EnclosingElementKindName = CLASS
     * 注: element:Modifiers = [private]
     * 注: element--------------METHOD-------------------------------
     * 注: element:Parameters =
     * 注: element:ReturnType = java.lang.String
     * 注: element:value = this is local method sayHello
     * 注: element:packageName = com.lisn.apt_test1
     * 注: element:SimpleName = sayHello
     * 注: element:asType = (java.lang.String)java.lang.String
     * 注: element:KindName = METHOD
     * 注: element:EnclosingElementKindName = CLASS
     * 注: element:Modifiers = [private]
     * 注: element--------------PARAMETER-------------------------------
     * 注: element:value = this is parameter msg
     * 注: element:packageName = com.lisn.apt_test1
     * 注: element:SimpleName = msg
     * 注: element:asType = java.lang.String
     * 注: element:KindName = PARAMETER
     * 注: element:EnclosingElementKindName = METHOD
     * 注: element:Modifiers = []
     * 注: process start
     * 注: process end
     *
     * @param set
     * @param roundEnv
     */
    private void parseTestAnnotation(Set<? extends TypeElement> set, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Test.class);
        for (Element element : elements) { //遍历所有元素
            if (element.getKind().equals(ElementKind.PACKAGE)) {
                loggerInfo("element--------------PACKAGE-------------------------------");
            } else if (element.getKind().equals(ElementKind.CLASS)) {
                //被注解的元素是类
                TypeElement typeElement = (TypeElement) element;
                loggerInfo("element--------------CLASS-------------------------------");
                //实现接口信息
                loggerInfo("element:Interfaces = " + typeElement.getInterfaces().toString());
                //泛型参数
                loggerInfo("element:TypeParameters = " + typeElement.getTypeParameters().toString());

                //element的父元素是包元素
                PackageElement packageElement = (PackageElement) element.getEnclosingElement();
                loggerInfo("element:packageElement = " + packageElement.getQualifiedName());

            } else if (element.getKind().equals(ElementKind.FIELD)) {
                //被注解的元素是全局变量
                loggerInfo("element--------------FIELD-------------------------------");
                VariableElement variableElement = (VariableElement) element;
                //获取变量名称
                loggerInfo("element:field Name = " + variableElement.getSimpleName());
                //获取变量类型
                loggerInfo("element:type = " + variableElement.asType());
                loggerInfo("element:typeSimpleName = " + typeUtils.asElement(variableElement.asType()).getSimpleName());
                loggerInfo("element:typeSimpleName = " + ClassName.get(variableElement.asType()));
            } else if (element.getKind().equals(ElementKind.PARAMETER)) {
                //被注解的元素是参数
                loggerInfo("element--------------PARAMETER-------------------------------");
            } else if (element.getKind().equals(ElementKind.METHOD)) {
                //被注解的元素是方法
                loggerInfo("element--------------METHOD-------------------------------");
                ExecutableElement executableElement = (ExecutableElement) element;
                //获取方法的参数名
                loggerInfo("element:Parameters = " + executableElement.getTypeParameters().toString());
                //获取方法的返回值类型
                loggerInfo("element:ReturnType = " + executableElement.getReturnType().toString());
            }
            //打印注解里面的值
            loggerInfo("element:value = " + element.getAnnotation(Test.class).value());
            //打印包名信息
            loggerInfo("element:packageName = " + processingEnv.getElementUtils().getPackageOf(element).getQualifiedName());
            //被注解元素的名称
            loggerInfo("element:SimpleName = " + element.getSimpleName());
            //被注解元素的类型（String/int/float...）
            loggerInfo("element:asType = " + element.asType().toString());
            //被注解元素的种类（PACKAGE、CLASS、METHOD、PARAMETER等）
            loggerInfo("element:KindName = " + element.getKind().name());
            //获取父元素的种类（局部变量的父元素是方法、方法及全局变量的父元素是类、类元素的父元素是包）
            loggerInfo("element:EnclosingElementKindName = " + element.getEnclosingElement().getKind().name());
            //被注解元素的修饰 如：public static 等
            loggerInfo("element:Modifiers = " + element.getModifiers().toString());
        }
    }


    /**
     * 打印信息
     * <p>
     * class Logger {
     * void test() {
     * String arg0=" TypeElement: this is class TestClass";
     * String arg1="=============================打印包信息================================";
     * String arg2="packageElement:  apt_test1";
     * String arg3="packageElement:  com.lisn.apt_test1";
     * String arg4="=============================打印泛型信息================================";
     * String arg5="T";
     * String arg6="=============================================================";
     * String arg7=" VariableElement: this is local field name";
     * String arg8="ExecutableElement: this is local method sayHello";
     * String arg9=" VariableElement: this is parameter msg";
     * }
     * }
     */

    public boolean processTest(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //扫描整个工程   找出含有Test注解的元素(包括类，)
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(Test.class);
        //由于编译器的输出无法打印到控制台，因此这里借助javapoet库把需要输出的信息写入到一个新的类
        //这个是我封装的一个简单的工具
        ProcessorTool builder = new ProcessorTool(processingEnv);
        for (Element element : elements) {
            Test aaaaa = element.getAnnotation(Test.class);
            if (element instanceof TypeElement) {
                builder.addArgs(" TypeElement: " + aaaaa.value());
                builder.addArgs("=============================打印包信息================================");
                PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(element);
                builder.addArgs("packageElement:  " + packageElement.getSimpleName().toString());
                builder.addArgs("packageElement:  " + packageElement.getQualifiedName());

                builder.addArgs("=============================打印泛型信息================================");
                List<? extends TypeParameterElement> typeParameters = ((TypeElement) element).getTypeParameters();
                for (TypeParameterElement typeParameter : typeParameters) {
                    builder.addArgs(typeParameter.getSimpleName().toString());
                }
                builder.addArgs("=============================================================");
            } else if (element instanceof ExecutableElement) {
                builder.addArgs("ExecutableElement: " + aaaaa.value());
            } else if (element instanceof VariableElement) {
                builder.addArgs(" VariableElement: " + aaaaa.value());
            }
        }
        builder.printLog();
        return true;
    }


}