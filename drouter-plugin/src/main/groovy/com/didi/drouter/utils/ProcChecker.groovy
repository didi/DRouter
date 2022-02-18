package com.didi.drouter.utils

import com.android.build.api.transform.TransformInput
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import org.apache.commons.compress.archivers.zip.ZipUtil
import org.apache.commons.io.FileUtils;

/**
 * Created by gaowei on 2022/02/15
 */
class ProcChecker {

    static def findCustomApplication(project) {
        project.extensions.android.applicationVariants.all { variant ->
            String variantName = variant.name.capitalize()
            def processManifestTask = project.tasks.getByName("process${variantName}Manifest")
            processManifestTask.doLast { manifestTask ->
                String manifestPath = "${manifestTask.manifestOutputDirectory}" + File.separator + "AndroidManifest.xml"
                if (!TextUtil.isEmpty(manifestPath)) {
                    def manifest = new XmlParser().parse(manifestPath)
                    if (manifest.application != null && manifest.application.size() > 0) {
                        String APPLICATION_LABEL = '{http://schemas.android.com/apk/res/android}name'
                        String childPath = "intermediates" + File.separator + "drouter" + File.separator + "application.txt"
                        def applicationMap = manifest.application[0].attributes()
                        def applicationClassName = ""
                        for (key in applicationMap.keySet()) {
                            // key 的类型为 groovy.xml.QName
                            if (APPLICATION_LABEL == key.toString().trim()) {
                                applicationClassName = applicationMap[key].toString().trim()
                                // 适配 AndroidManifest.xml 中  android:name=".RemoteApplication" 的配置
                                if (!TextUtil.isEmpty(applicationClassName) && applicationClassName.startsWith(".")) {
                                    applicationClassName = manifest.attribute('package') + applicationClassName
                                }
                                FileUtils.write(new File(project.buildDir, childPath),
                                        applicationClassName, "UTF-8")
                                break
                            }
                        }
                        FileUtils.write(new File(project.buildDir, childPath), applicationClassName,
                                "UTF-8")
                    }
                }
            }
        }
    }

    static def handleCustomApplication(project, invocation) {
        if (project == null || invocation == null) {
            return
        }
        def applicationFile = new File(project.buildDir.absolutePath,
                "intermediates" + File.separator + "drouter" + File.separator + "application.txt")
        if (!applicationFile.exists() || TextUtil.isEmpty(applicationFile.text.trim())) {
            return
        }

        def tempCompilePath = []
        for (TransformInput transformInput : invocation.inputs) {
            transformInput.directoryInputs.each {
                tempCompilePath.add(it.file)
            }
            transformInput.jarInputs.each {
                tempCompilePath.add(it.file)
            }
        }

        String tempDirectoryRootPath = project.buildDir.absolutePath + File.separator + "classes" + File.separator
        ClassPool pool = new ClassPool()
        for (File file : tempCompilePath) {
            if (file.absolutePath.contains("drouter-api") && file.absolutePath.endsWith(".jar")) {
                // 解决 Caused by: java.util.zip.ZipException: invalid code lengths set
                String tempFilePath = file.absolutePath.replace(".jar", "_" + System.currentTimeMillis() + ".jar")
                def tempFile = new File(tempFilePath)
                FileUtils.copyFile(file, tempFile)
                pool.appendClassPath(tempFilePath)

                def tempDirectory = new File(tempDirectoryRootPath)
                ZipUtil.unpack(file, tempDirectory)
                CtClass ctClass = pool.get("com.didi.drouter.remote.RemoteApplicationManager");
                CtMethod ctMethod = ctClass.getMethod("hasCustomApplication", "()Z")
                ctMethod.setBody("return true;")
                ctClass.writeFile(tempDirectoryRootPath)
                ZipUtil.pack(new File(tempDirectoryRootPath), file)

                FileUtils.deleteDirectory(tempDirectory)
                FileUtils.deleteQuietly(tempFile)
            } else {
                pool.appendClassPath(file.absolutePath)
            }
        }

        CtClass ctClass = pool.get(applicationFile.text.trim())
        CtMethod onCreateMethod = ctClass.getMethod("onCreate", "()V");
        // 解决 Caused by: java.lang.RuntimeException: xxxxx class is frozen
        if (ctClass.isFrozen()) {
            ctClass.defrost()
        }
        onCreateMethod.insertAfter("com.didi.drouter.remote.RemoteApplicationManager.setsApplicationFinished(true);")
        String classPath = project.buildDir.absolutePath + "/intermediates/javac/debug/compileDebugJavaWithJavac/classes/"
        ctClass.writeFile(classPath)
    }
}
