// automatically generated by the FlatBuffers compiler, do not modify

package elide.crypto

@Suppress("unused")
class HashAlgorithm private constructor() {
    companion object {
        const val IDENTITY: Int = 0
        const val MD5: Int = 1
        const val SHA1: Int = 2
        const val SHA256: Int = 3
        const val SHA512: Int = 4
        const val SHA3224: Int = 5
        const val SHA3256: Int = 6
        const val SHA3512: Int = 7
        val names : Array<String> = arrayOf("IDENTITY", "MD5", "SHA1", "SHA256", "SHA512", "SHA3_224", "SHA3_256", "SHA3_512")
        fun name(e: Int) : String = names[e]
    }
}
