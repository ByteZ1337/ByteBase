package xyz.xenondevs.bytebase.analysis.stack

/**
 * Class containing information on the stack size when entering a label
 *
 * TODO: exitSize for different labels
 */
class LabelGateInfo {
    
    /**
     * The size of the stack when entering the label
     */
    var entrySize: Int = 0
        internal set
    
}