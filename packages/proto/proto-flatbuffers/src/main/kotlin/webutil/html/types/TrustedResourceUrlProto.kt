// automatically generated by the FlatBuffers compiler, do not modify

package webutil.html.types

import java.nio.*
import kotlin.math.sign
import com.google.flatbuffers.*

@Suppress("unused")
class TrustedResourceUrlProto : Table() {

    fun __init(_i: Int, _bb: ByteBuffer)  {
        __reset(_i, _bb)
    }
    fun __assign(_i: Int, _bb: ByteBuffer) : TrustedResourceUrlProto {
        __init(_i, _bb)
        return this
    }
    val privateDoNotAccessOrElseTrustedResourceUrlWrappedValue : String?
        get() {
            val o = __offset(4)
            return if (o != 0) __string(o + bb_pos) else null
        }
    val privateDoNotAccessOrElseTrustedResourceUrlWrappedValueAsByteBuffer : ByteBuffer get() = __vector_as_bytebuffer(4, 1)
    fun privateDoNotAccessOrElseTrustedResourceUrlWrappedValueInByteBuffer(_bb: ByteBuffer) : ByteBuffer = __vector_in_bytebuffer(_bb, 4, 1)
    companion object {
        fun validateVersion() = Constants.FLATBUFFERS_22_12_06()
        fun getRootAsTrustedResourceUrlProto(_bb: ByteBuffer): TrustedResourceUrlProto = getRootAsTrustedResourceUrlProto(_bb, TrustedResourceUrlProto())
        fun getRootAsTrustedResourceUrlProto(_bb: ByteBuffer, obj: TrustedResourceUrlProto): TrustedResourceUrlProto {
            _bb.order(ByteOrder.LITTLE_ENDIAN)
            return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb))
        }
        fun createTrustedResourceUrlProto(builder: FlatBufferBuilder, privateDoNotAccessOrElseTrustedResourceUrlWrappedValueOffset: Int) : Int {
            builder.startTable(1)
            addPrivateDoNotAccessOrElseTrustedResourceUrlWrappedValue(builder, privateDoNotAccessOrElseTrustedResourceUrlWrappedValueOffset)
            return endTrustedResourceUrlProto(builder)
        }
        fun startTrustedResourceUrlProto(builder: FlatBufferBuilder) = builder.startTable(1)
        fun addPrivateDoNotAccessOrElseTrustedResourceUrlWrappedValue(builder: FlatBufferBuilder, privateDoNotAccessOrElseTrustedResourceUrlWrappedValue: Int) = builder.addOffset(0, privateDoNotAccessOrElseTrustedResourceUrlWrappedValue, 0)
        fun endTrustedResourceUrlProto(builder: FlatBufferBuilder) : Int {
            val o = builder.endTable()
            return o
        }
    }
}
