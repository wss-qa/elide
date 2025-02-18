// automatically generated by the FlatBuffers compiler, do not modify

package elide.base

import java.nio.*
import kotlin.math.sign
import com.google.flatbuffers.*

@Suppress("unused")
class LanguageSpec : Table() {

    fun __init(_i: Int, _bb: ByteBuffer)  {
        __reset(_i, _bb)
    }
    fun __assign(_i: Int, _bb: ByteBuffer) : LanguageSpec {
        __init(_i, _bb)
        return this
    }
    val selection : elide.base.LanguageSpec_.LanguageSelection? get() = selection(elide.base.LanguageSpec_.LanguageSelection())
    fun selection(obj: elide.base.LanguageSpec_.LanguageSelection) : elide.base.LanguageSpec_.LanguageSelection? {
        val o = __offset(4)
        return if (o != 0) {
            obj.__assign(__indirect(o + bb_pos), bb)
        } else {
            null
        }
    }
    val modifier : elide.base.LanguageSpec_.LanguageModifier? get() = modifier(elide.base.LanguageSpec_.LanguageModifier())
    fun modifier(obj: elide.base.LanguageSpec_.LanguageModifier) : elide.base.LanguageSpec_.LanguageModifier? {
        val o = __offset(6)
        return if (o != 0) {
            obj.__assign(__indirect(o + bb_pos), bb)
        } else {
            null
        }
    }
    companion object {
        fun validateVersion() = Constants.FLATBUFFERS_22_12_06()
        fun getRootAsLanguageSpec(_bb: ByteBuffer): LanguageSpec = getRootAsLanguageSpec(_bb, LanguageSpec())
        fun getRootAsLanguageSpec(_bb: ByteBuffer, obj: LanguageSpec): LanguageSpec {
            _bb.order(ByteOrder.LITTLE_ENDIAN)
            return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb))
        }
        fun createLanguageSpec(builder: FlatBufferBuilder, selectionOffset: Int, modifierOffset: Int) : Int {
            builder.startTable(2)
            addModifier(builder, modifierOffset)
            addSelection(builder, selectionOffset)
            return endLanguageSpec(builder)
        }
        fun startLanguageSpec(builder: FlatBufferBuilder) = builder.startTable(2)
        fun addSelection(builder: FlatBufferBuilder, selection: Int) = builder.addOffset(0, selection, 0)
        fun addModifier(builder: FlatBufferBuilder, modifier: Int) = builder.addOffset(1, modifier, 0)
        fun endLanguageSpec(builder: FlatBufferBuilder) : Int {
            val o = builder.endTable()
            return o
        }
    }
}
