// automatically generated by the FlatBuffers compiler, do not modify

package elide.vfs.GuestVFSPolicy_

import java.nio.*
import kotlin.math.sign
import com.google.flatbuffers.*

@Suppress("unused")
class HostIOPolicy : Table() {

    fun __init(_i: Int, _bb: ByteBuffer)  {
        __reset(_i, _bb)
    }
    fun __assign(_i: Int, _bb: ByteBuffer) : HostIOPolicy {
        __init(_i, _bb)
        return this
    }
    companion object {
        fun validateVersion() = Constants.FLATBUFFERS_22_12_06()
        fun getRootAsHostIOPolicy(_bb: ByteBuffer): HostIOPolicy = getRootAsHostIOPolicy(_bb, HostIOPolicy())
        fun getRootAsHostIOPolicy(_bb: ByteBuffer, obj: HostIOPolicy): HostIOPolicy {
            _bb.order(ByteOrder.LITTLE_ENDIAN)
            return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb))
        }
        fun startHostIOPolicy(builder: FlatBufferBuilder) = builder.startTable(0)
        fun endHostIOPolicy(builder: FlatBufferBuilder) : Int {
            val o = builder.endTable()
            return o
        }
    }
}
