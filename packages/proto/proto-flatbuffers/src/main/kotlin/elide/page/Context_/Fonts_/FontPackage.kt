// automatically generated by the FlatBuffers compiler, do not modify

package elide.page.Context_.Fonts_

import java.nio.*
import kotlin.math.sign
import com.google.flatbuffers.*

@Suppress("unused")
class FontPackage : Table() {

    fun __init(_i: Int, _bb: ByteBuffer)  {
        __reset(_i, _bb)
    }
    fun __assign(_i: Int, _bb: ByteBuffer) : FontPackage {
        __init(_i, _bb)
        return this
    }
    val name : String?
        get() {
            val o = __offset(4)
            return if (o != 0) __string(o + bb_pos) else null
        }
    val nameAsByteBuffer : ByteBuffer get() = __vector_as_bytebuffer(4, 1)
    fun nameInByteBuffer(_bb: ByteBuffer) : ByteBuffer = __vector_in_bytebuffer(_bb, 4, 1)
    fun reference(j: Int) : elide.page.Context_.Fonts_.FontReference? = reference(elide.page.Context_.Fonts_.FontReference(), j)
    fun reference(obj: elide.page.Context_.Fonts_.FontReference, j: Int) : elide.page.Context_.Fonts_.FontReference? {
        val o = __offset(6)
        return if (o != 0) {
            obj.__assign(__indirect(__vector(o) + j * 4), bb)
        } else {
            null
        }
    }
    val referenceLength : Int
        get() {
            val o = __offset(6); return if (o != 0) __vector_len(o) else 0
        }
    companion object {
        fun validateVersion() = Constants.FLATBUFFERS_22_12_06()
        fun getRootAsFontPackage(_bb: ByteBuffer): FontPackage = getRootAsFontPackage(_bb, FontPackage())
        fun getRootAsFontPackage(_bb: ByteBuffer, obj: FontPackage): FontPackage {
            _bb.order(ByteOrder.LITTLE_ENDIAN)
            return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb))
        }
        fun createFontPackage(builder: FlatBufferBuilder, nameOffset: Int, referenceOffset: Int) : Int {
            builder.startTable(2)
            addReference(builder, referenceOffset)
            addName(builder, nameOffset)
            return endFontPackage(builder)
        }
        fun startFontPackage(builder: FlatBufferBuilder) = builder.startTable(2)
        fun addName(builder: FlatBufferBuilder, name: Int) = builder.addOffset(0, name, 0)
        fun addReference(builder: FlatBufferBuilder, reference: Int) = builder.addOffset(1, reference, 0)
        fun createReferenceVector(builder: FlatBufferBuilder, data: IntArray) : Int {
            builder.startVector(4, data.size, 4)
            for (i in data.size - 1 downTo 0) {
                builder.addOffset(data[i])
            }
            return builder.endVector()
        }
        fun startReferenceVector(builder: FlatBufferBuilder, numElems: Int) = builder.startVector(4, numElems, 4)
        fun endFontPackage(builder: FlatBufferBuilder) : Int {
            val o = builder.endTable()
            return o
        }
    }
}
