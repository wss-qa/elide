// automatically generated by the FlatBuffers compiler, do not modify

package elide.data.DataContainerRef_

import java.nio.*
import kotlin.math.sign
import com.google.flatbuffers.*

@Suppress("unused")
class ContainerReference : Table() {

    fun __init(_i: Int, _bb: ByteBuffer)  {
        __reset(_i, _bb)
    }
    fun __assign(_i: Int, _bb: ByteBuffer) : ContainerReference {
        __init(_i, _bb)
        return this
    }
    val data : elide.data.CompressedData? get() = data(elide.data.CompressedData())
    fun data(obj: elide.data.CompressedData) : elide.data.CompressedData? {
        val o = __offset(4)
        return if (o != 0) {
            obj.__assign(__indirect(o + bb_pos), bb)
        } else {
            null
        }
    }
    val resource : elide.data.DataContainerRef_.FingerprintPathPair? get() = resource(elide.data.DataContainerRef_.FingerprintPathPair())
    fun resource(obj: elide.data.DataContainerRef_.FingerprintPathPair) : elide.data.DataContainerRef_.FingerprintPathPair? {
        val o = __offset(6)
        return if (o != 0) {
            obj.__assign(__indirect(o + bb_pos), bb)
        } else {
            null
        }
    }
    val filesystem : elide.data.DataContainerRef_.FingerprintPathPair? get() = filesystem(elide.data.DataContainerRef_.FingerprintPathPair())
    fun filesystem(obj: elide.data.DataContainerRef_.FingerprintPathPair) : elide.data.DataContainerRef_.FingerprintPathPair? {
        val o = __offset(8)
        return if (o != 0) {
            obj.__assign(__indirect(o + bb_pos), bb)
        } else {
            null
        }
    }
    companion object {
        fun validateVersion() = Constants.FLATBUFFERS_22_12_06()
        fun getRootAsContainerReference(_bb: ByteBuffer): ContainerReference = getRootAsContainerReference(_bb, ContainerReference())
        fun getRootAsContainerReference(_bb: ByteBuffer, obj: ContainerReference): ContainerReference {
            _bb.order(ByteOrder.LITTLE_ENDIAN)
            return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb))
        }
        fun createContainerReference(builder: FlatBufferBuilder, dataOffset: Int, resourceOffset: Int, filesystemOffset: Int) : Int {
            builder.startTable(3)
            addFilesystem(builder, filesystemOffset)
            addResource(builder, resourceOffset)
            addData(builder, dataOffset)
            return endContainerReference(builder)
        }
        fun startContainerReference(builder: FlatBufferBuilder) = builder.startTable(3)
        fun addData(builder: FlatBufferBuilder, data: Int) = builder.addOffset(0, data, 0)
        fun addResource(builder: FlatBufferBuilder, resource: Int) = builder.addOffset(1, resource, 0)
        fun addFilesystem(builder: FlatBufferBuilder, filesystem: Int) = builder.addOffset(2, filesystem, 0)
        fun endContainerReference(builder: FlatBufferBuilder) : Int {
            val o = builder.endTable()
            return o
        }
    }
}
