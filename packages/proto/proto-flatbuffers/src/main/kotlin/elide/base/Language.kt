// automatically generated by the FlatBuffers compiler, do not modify

package elide.base

@Suppress("unused")
class Language private constructor() {
    companion object {
        const val ENGLISH: Int = 0
        const val FRENCH: Int = 1
        const val SPANISH: Int = 2
        const val CHINESE: Int = 3
        val names : Array<String> = arrayOf("ENGLISH", "FRENCH", "SPANISH", "CHINESE")
        fun name(e: Int) : String = names[e]
    }
}
