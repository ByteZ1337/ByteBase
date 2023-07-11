package xyz.xenondevs.bytebase.patch.logging

interface PatchLogger {
    
    fun debug(message: String, vararg args: Any)
    
    fun debugLine() = debug("---------------------------------------------------------------------------------")
    
    fun info(message: String, vararg args: Any)
    
    fun warn(message: String, vararg args: Any)
    
    fun error(message: String, vararg args: Any)
    
    fun critical(message: String, vararg args: Any)
    
}

object VoidingLogger : PatchLogger {
    
    override fun debug(message: String, vararg args: Any) = Unit
    
    override fun info(message: String, vararg args: Any) = Unit
    
    override fun warn(message: String, vararg args: Any) = Unit
    
    override fun error(message: String, vararg args: Any) = Unit
    
    override fun critical(message: String, vararg args: Any) = Unit
    
}

object SimpleLogger : PatchLogger {
    
    override fun debug(message: String, vararg args: Any) = println("[DEBUG] $message".format(args = args))
    
    override fun info(message: String, vararg args: Any) = println("[INFO] $message".format(args = args))
    
    override fun warn(message: String, vararg args: Any) = println("[WARN] $message".format(args = args))
    
    override fun error(message: String, vararg args: Any) = println("[ERROR] $message".format(args = args))
    
    override fun critical(message: String, vararg args: Any) = println("[CRITICAL] $message".format(args = args))
    
}