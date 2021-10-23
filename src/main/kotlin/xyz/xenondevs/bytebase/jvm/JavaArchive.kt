package xyz.xenondevs.bytebase.jvm

import org.objectweb.asm.ClassReader.SKIP_FRAMES
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.zip.ZipInputStream

private val CLASS_MAGIC = byteArrayOf(-54, -2, -70, -66).asList() //0xCAFEBABE in signed bytes

class JavaArchive() {
    
    val directories = ArrayList<String>()
    val classes = ArrayList<ClassWrapper>()
    val resources = ArrayList<Resource>()
    
    constructor(file: File, parseOptions: Int = SKIP_FRAMES) : this(FileInputStream(file), parseOptions)
    
    constructor(inputStream: InputStream, parseOptions: Int = SKIP_FRAMES) : this() {
        readFile(if (inputStream is ZipInputStream) inputStream else ZipInputStream(inputStream), parseOptions)
    }
    
    private fun readFile(jis: ZipInputStream, parseOptions: Int) {
        generateSequence(jis::getNextEntry).forEach { entry ->
            println(entry.name)
            if (entry.isDirectory) { // TODO actually check if the file is a directory
                directories += entry.name
            } else {
                val content = jis.readAllBytes()
                if (entry.name.endsWith(".class") && content.take(4) == CLASS_MAGIC)
                    classes += ClassWrapper(entry.name, content, parseOptions)
                else
                    resources += Resource(entry.name, content)
            }
        }
    }
    
    fun writeFile(file: File) = write(file.outputStream())
    
    fun write(outputStream: OutputStream, close: Boolean = true, writtenEntries: HashSet<String> = HashSet()) {
        val jos = if (outputStream is JarOutputStream) outputStream else JarOutputStream(outputStream)
        
        directories.asSequence()
            .filterNot(writtenEntries::contains)
            .forEach { dir ->
                jos.putNextEntry(JarEntry(dir))
                jos.closeEntry()
                writtenEntries += dir
            }
        
        resources.asSequence()
            .filterNot { writtenEntries.contains(it.name) }
            .forEach { resource ->
                jos.putNextEntry(JarEntry(resource.name))
                jos.write(resource.content)
                jos.closeEntry()
                jos.flush()
                writtenEntries += resource.name
            }
        
        classes.asSequence()
            .filterNot { writtenEntries.contains(it.fileName) }
            .forEach { clazz ->
                jos.putNextEntry(JarEntry(clazz.fileName))
                jos.write(clazz.assemble())
                jos.closeEntry()
                jos.flush()
                writtenEntries += clazz.name
            }
        
        if(close) jos.close()
    }
    
}