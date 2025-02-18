// automatically generated by the FlatBuffers compiler, do not modify

package elide.assets.EmbeddedScriptMetadata_

import java.nio.*
import kotlin.math.sign
import com.google.flatbuffers.*

@Suppress("unused")
class LanguageSpecificMetadata : Table() {

    fun __init(_i: Int, _bb: ByteBuffer)  {
        __reset(_i, _bb)
    }
    fun __assign(_i: Int, _bb: ByteBuffer) : LanguageSpecificMetadata {
        __init(_i, _bb)
        return this
    }
    val javascript : elide.assets.EmbeddedScriptMetadata_.JsScriptMetadata? get() = javascript(elide.assets.EmbeddedScriptMetadata_.JsScriptMetadata())
    fun javascript(obj: elide.assets.EmbeddedScriptMetadata_.JsScriptMetadata) : elide.assets.EmbeddedScriptMetadata_.JsScriptMetadata? {
        val o = __offset(4)
        return if (o != 0) {
            obj.__assign(__indirect(o + bb_pos), bb)
        } else {
            null
        }
    }
    companion object {
        fun validateVersion() = Constants.FLATBUFFERS_22_12_06()
        fun getRootAsLanguageSpecificMetadata(_bb: ByteBuffer): LanguageSpecificMetadata = getRootAsLanguageSpecificMetadata(_bb, LanguageSpecificMetadata())
        fun getRootAsLanguageSpecificMetadata(_bb: ByteBuffer, obj: LanguageSpecificMetadata): LanguageSpecificMetadata {
            _bb.order(ByteOrder.LITTLE_ENDIAN)
            return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb))
        }
        fun createLanguageSpecificMetadata(builder: FlatBufferBuilder, javascriptOffset: Int) : Int {
            builder.startTable(1)
            addJavascript(builder, javascriptOffset)
            return endLanguageSpecificMetadata(builder)
        }
        fun startLanguageSpecificMetadata(builder: FlatBufferBuilder) = builder.startTable(1)
        fun addJavascript(builder: FlatBufferBuilder, javascript: Int) = builder.addOffset(0, javascript, 0)
        fun endLanguageSpecificMetadata(builder: FlatBufferBuilder) : Int {
            val o = builder.endTable()
            return o
        }
    }
}
