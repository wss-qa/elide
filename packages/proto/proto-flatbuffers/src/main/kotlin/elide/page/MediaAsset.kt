// automatically generated by the FlatBuffers compiler, do not modify

package elide.page

import java.nio.*
import kotlin.math.sign
import com.google.flatbuffers.*

@Suppress("unused")
class MediaAsset : Table() {

    fun __init(_i: Int, _bb: ByteBuffer)  {
        __reset(_i, _bb)
    }
    fun __assign(_i: Int, _bb: ByteBuffer) : MediaAsset {
        __init(_i, _bb)
        return this
    }
    val kind : Int
        get() {
            val o = __offset(4)
            return if(o != 0) bb.getInt(o + bb_pos) else 0
        }
    val media : elide.page.MediaAsset_.Anonymous1? get() = media(elide.page.MediaAsset_.Anonymous1())
    fun media(obj: elide.page.MediaAsset_.Anonymous1) : elide.page.MediaAsset_.Anonymous1? {
        val o = __offset(6)
        return if (o != 0) {
            obj.__assign(__indirect(o + bb_pos), bb)
        } else {
            null
        }
    }
    companion object {
        fun validateVersion() = Constants.FLATBUFFERS_22_12_06()
        fun getRootAsMediaAsset(_bb: ByteBuffer): MediaAsset = getRootAsMediaAsset(_bb, MediaAsset())
        fun getRootAsMediaAsset(_bb: ByteBuffer, obj: MediaAsset): MediaAsset {
            _bb.order(ByteOrder.LITTLE_ENDIAN)
            return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb))
        }
        fun createMediaAsset(builder: FlatBufferBuilder, kind: Int, mediaOffset: Int) : Int {
            builder.startTable(2)
            addMedia(builder, mediaOffset)
            addKind(builder, kind)
            return endMediaAsset(builder)
        }
        fun startMediaAsset(builder: FlatBufferBuilder) = builder.startTable(2)
        fun addKind(builder: FlatBufferBuilder, kind: Int) = builder.addInt(0, kind, 0)
        fun addMedia(builder: FlatBufferBuilder, media: Int) = builder.addOffset(1, media, 0)
        fun endMediaAsset(builder: FlatBufferBuilder) : Int {
            val o = builder.endTable()
            return o
        }
    }
}
