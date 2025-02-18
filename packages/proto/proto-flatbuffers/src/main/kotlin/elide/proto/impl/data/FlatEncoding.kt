@file:Suppress("RedundantVisibilityModifier")

package elide.proto.impl.data

import elide.proto.api.Symbolic
import elide.proto.api.Symbolic.Resolver
import elide.proto.api.Symbolic.SymbolUnresolved
import elide.core.encoding.Encoding

/** Maps a native enumeration to each symbolic encoding integer generated by Flatbuffers. */
public enum class FlatEncoding constructor (override val symbol: EncodingSymbol) : Symbolic<EncodingSymbol> {
  /** Encoding: UTF-8 (global default). */
  UTF8(symbol = Encoding.UTF_8.ordinal),

  /** Encoding: UTF-16. */
  UTF16(symbol = Encoding.UTF_16.ordinal),

  /** Encoding: UTF-32. */
  UTF32(symbol = Encoding.UTF_32.ordinal);

  public companion object : Resolver<EncodingSymbol, FlatEncoding> {
    /** @return [FlatEncoding] corresponding to the provided constant, or throw. */
    @Throws(SymbolUnresolved::class)
    override fun resoleSymbol(symbol: EncodingSymbol): FlatEncoding = when (symbol) {
      Encoding.UTF_8.ordinal -> UTF8
      Encoding.UTF_16.ordinal -> UTF16
      Encoding.UTF_32.ordinal -> UTF32
      else -> throw unresolved(symbol)
    }
  }
}
