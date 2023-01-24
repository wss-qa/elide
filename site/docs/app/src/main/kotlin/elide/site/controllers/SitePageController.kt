package elide.site.controllers

import com.aayushatharva.brotli4j.Brotli4jLoader
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream
import com.aayushatharva.brotli4j.encoder.Encoder as BrotliEncoder
import elide.core.encoding.hex.Hex
import elide.runtime.Logger
import elide.runtime.Logging
import elide.server.*
import elide.server.controller.PageController
import elide.server.controller.PageWithProps
import elide.site.AppServerProps
import elide.site.Assets
import elide.site.ElideSite
import elide.site.I18nPage
import elide.site.abstract.SitePage
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.cache.interceptor.CacheKeyGenerator
import io.micronaut.core.annotation.AnnotationMetadata
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import kotlinx.html.*
import kotlinx.html.tagext.body
import kotlinx.html.tagext.head
import reactor.core.publisher.Mono
import tools.elide.data.CompressionMode
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.security.MessageDigest
import java.time.Duration
import java.util.Locale
import java.util.SortedMap
import java.util.concurrent.ConcurrentSkipListMap
import java.util.zip.Deflater
import java.util.zip.GZIPOutputStream

/** Extend a [PageController] with access to a [SitePage] configuration. */
@Suppress("unused")
abstract class SitePageController protected constructor(val page: SitePage) : PageWithProps<AppServerProps>(
  AppServerProps.serializer(),
  AppServerProps(page = page.name),
) {
  companion object {
    const val enableSSR = true
    const val enableStreaming = true
  }

  /** Generate a cache key for an HTTP request. */
  internal class CachedResponseKeyGenerator : CacheKeyGenerator {
    /** @inheritDoc */
    override fun generateKey(annotationMetadata: AnnotationMetadata, vararg params: Any): Any {
      val req = params.first() as? HttpRequest<*> ?: error(
        "Cannot generate cache key for non-HTTP request: ${params.first()}"
      )
      return req.uri.toString()
    }
  }

  /** Compressed response body variant. */
  internal class CompressedResponseBody (
    private val data: ByteArray,
    private val length: Int = data.size,
  ) {
    /** @return Copied bytes for this response. */
    internal fun emit(): ByteArray {
      val copy = ByteArray(length)
      System.arraycopy(data, 0, copy, 0, length)
      return copy
    }
  }

  /** Cached response body. */
  internal class CachedResponseBody private constructor (
    private val raw: ByteArray,
    private val length: Int,
    private val compressed: SortedMap<CompressionMode, CompressedResponseBody>,
  ) {
    companion object {
      // Enabled compression modes for caching.
      private val enabledModes = if (Brotli4jLoader.isAvailable()) {
        Brotli4jLoader.ensureAvailability()
        listOf(
          CompressionMode.BROTLI,
          CompressionMode.GZIP,
        )
      } else listOf(
        CompressionMode.GZIP
      )

      // Brotli configuration.
      private val brotliParams = BrotliEncoder.Parameters()
        .setMode(BrotliEncoder.Mode.TEXT)
        .setQuality(8)
        .setWindow(22)

      /** Perform compression with the provided [mode] on the provided [body]. */
      @JvmStatic private fun compress(mode: CompressionMode, data: ByteArray): CompressedResponseBody? {
        val outbin = ByteArrayOutputStream()
        when (mode) {
          CompressionMode.GZIP -> BestGzip(outbin)
          CompressionMode.BROTLI -> BrotliOutputStream(outbin, brotliParams)
          else -> return null
        }.use {
          it.write(data)
          it.flush()
        }
        return CompressedResponseBody(outbin.toByteArray())
      }

      /** Create a [CachedResponseBody] from the provided [raw] body content. */
      @JvmStatic suspend fun from(raw: ByteArray?): CachedResponseBody? {
        if (raw == null) return null
        val length = raw.size
        val cache = ConcurrentSkipListMap<CompressionMode, CompressedResponseBody>()

        val jobs = enabledModes.map { mode ->
          withContext(Dispatchers.IO) {
            async {
              compress(mode, raw).let {
                if (it != null) cache[mode] = it
              }
            }
          }
        }
        jobs.awaitAll()

        return CachedResponseBody(
          raw,
          length,
          cache,
        )
      }
    }

    /** @return Raw bytes to serve for this response if no clear encoding can be resolved. */
    internal fun raw(): ByteArray {
      val copy = ByteArray(length)
      System.arraycopy(raw, 0, copy, 0, length)
      return copy
    }

    /** @return Indication of whether a body is present for the provided [mode]. */
    internal fun has(mode: CompressionMode): Boolean = compressed.contains(mode)

    /** @return Compressed response body for [mode], or an error is thrown. */
    internal fun serve(mode: CompressionMode): CompressedResponseBody = compressed[mode] ?: error(
      "Failed to locate request body for compression mode '$mode'"
    )
  }

  /** Gzip output stream which uses the [Deflater.BEST_COMPRESSION] ratio possible. */
  private class BestGzip (outbin: OutputStream): GZIPOutputStream(outbin) {
    init {
      def.setLevel(Deflater.BEST_COMPRESSION)
    }
  }

  /** Cached HTTP response. */
  internal data class CachedResponse(
    val uri: String,
    val status: HttpStatus,
    val encoding: Charset,
    val type: MediaType,
    val headers: Map<CharSequence, CharSequence>,
    val length: Long,
    val body: CachedResponseBody?,
    val fingerprint: String,
    val materialized: Instant = Clock.System.now(),
  ) {
    companion object {
      /** Create a cached response from a raw origin response and [body]. */
      @JvmStatic suspend fun from(request: HttpRequest<*>, response: HttpResponse<ByteArray>): CachedResponse {
        return CachedResponse(
          request.uri.toString(),
          response.status,
          response.characterEncoding,
          response.contentType.orElse(MediaType.TEXT_HTML_TYPE),
          response.headers.flatMap { it.value.map { value -> it.key to value } }.toMap(),
          response.contentLength,
          CachedResponseBody.from(response.body()),
          fingerprint(request, response),
        )
      }

      // Acquire a hasher for the provided algorithm.
      @JvmStatic private fun hasher() = MessageDigest.getInstance("SHA-256")

      // Generate a fingerprint for the provided response.
      @JvmStatic private fun fingerprint(req: HttpRequest<*>, res: HttpResponse<ByteArray>): String = hasher().apply {
        require(res.status == HttpStatus.OK) { "Cannot fingerprint non-OK response: $res" }
        update(req.uri.toString().toByteArray())
        update(res.status.code.toString().toByteArray())
        update(res.characterEncoding.name().toByteArray())
        update(res.contentType.orElse(MediaType.TEXT_HTML_TYPE).toString().toByteArray())
        update(res.headers.flatMap { it.value.map { value -> it.key to value } }.toMap().toString().toByteArray())
        update(res.contentLength.toString().toByteArray())
        val body = res.body()
        if (body != null) update(body)
      }.let { digest ->
        // encode an E-tag for the provided primed digest
        Hex.encodeToString(digest.digest()).takeLast(8)
      }
    }

    // Decide whether a response can be short-circuited with statuses like 304; otherwise, check the cache and determine
    // what compressed response to use (if data is found); if no options succeed, return `null` to synthesize from raw.
    private fun negotiate(req: HttpRequest<*>): MutableHttpResponse<ByteArray>? {
      // negotiation requires a body
      val body = this.body ?: return null

      // check for `If-None-Match` header
      val ifNoneMatch = req.headers.findFirst(HttpHeaders.IF_NONE_MATCH)
      if (ifNoneMatch.isPresent) {
        val nominated = ifNoneMatch.get().replace("\"", "")
        if (nominated.isNotBlank()) {
          if (fingerprint == nominated) {
            return HttpResponse.notModified<ByteArray>().apply {
              header(HttpHeaders.ETAG, "\"$fingerprint\"")
            }
          }
        }
      }

      // check for `If-Modified-Since` header
      val date = req.headers.findDate(HttpHeaders.IF_MODIFIED_SINCE)
      if (date.isPresent) {
        val ref = date.get().toInstant().toKotlinInstant()
        if (ref >= materialized) {
          return HttpResponse.notModified<ByteArray>().apply {
            header(HttpHeaders.ETAG, "\"$fingerprint\"")
          }
        }
      }

      // okay, we can't short-circuit, so we should begin negotiating which kind of body to send.
      val acceptEncoding = req.headers.findFirst(HttpHeaders.ACCEPT_ENCODING).orElse(null)
        ?.split(",")
        ?.map { it.trim().lowercase() }

      return when {
        // if we do not have an `Accept-Encoding` header at all, we should fall back to default behavior.
        acceptEncoding?.isEmpty() != false -> null

        // if `Accept-Encoding` includes `br` and we have a Brotli payload on-hand, we can use a present Brotli variant.
        body.has(CompressionMode.BROTLI) && acceptEncoding.contains("br") -> {
          // use a brotli variant
          synthesize("br" to body.serve(CompressionMode.BROTLI))
        }

        // if `Accept-Encoding` includes `gz` and we have a Gzip payload on-hand, we can use a present Gzip variant.
        body.has(CompressionMode.GZIP) && acceptEncoding.contains("gzip") -> {
          // use a gzip variant
          synthesize("gzip" to body.serve(CompressionMode.GZIP))
        }

        // we do not have a special negotiated variant to serve, so we should just fall back to default behavior.
        else -> null
      }
    }

    // Synthesize a request for a response.
    private fun synthesize(
      compressedBody: Pair<String, CompressedResponseBody>? = null,
    ): MutableHttpResponse<ByteArray> = HttpResponse.status<ByteArray>(
      status
    ).headers(
      headers
    ).header(
      HttpHeaders.ETAG,
      "\"${fingerprint}\"",
    ).characterEncoding(
      encoding
    ).contentLength(
      length
    ).contentType(
      type
    ).apply {
      if (status.code != 201 && status.code != 304) {
        if (compressedBody != null) {
          val (encoding, body) = compressedBody
          header(HttpHeaders.CONTENT_ENCODING, encoding)
          body(body.emit())
        } else {
          val body = this@CachedResponse.body?.raw()
          if (body?.isNotEmpty() == true) body(body)
        }
      }
    }

    /** @return HTTP response to emit for this cached response. */
    fun response(req: HttpRequest<*>): MutableHttpResponse<ByteArray> = when (val early = negotiate(req)) {
      null -> synthesize()
      else -> early
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as CachedResponse

      if (status != other.status) return false
      if (uri != other.uri) return false
      if (encoding != other.encoding) return false
      if (type != other.type) return false

      return true
    }

    override fun hashCode(): Int {
      var result = uri.hashCode()
      result = 32 * result + status.hashCode()
      result = 31 * result + encoding.hashCode()
      result = 31 * result + type.hashCode()
      return result
    }

    override fun toString(): String {
      return "CachedResponse(status='$status', uri='$uri', encoding=$encoding, type=$type, headers=$headers)"
    }
  }

  // Logger for all controllers.
  private val logging: Logger = Logging.named("SitePageController")

  // Response cache.
  private val responseCache: ConcurrentSkipListMap<String, CachedResponse> = ConcurrentSkipListMap()

  // Site-level info resolved for the current locale.
  @Inject internal lateinit var siteInfo: ElideSite.SiteInfo

  // Copy a response body and all metadata to a new response object.
  private fun <V, X> copyResponse(
    response: MutableHttpResponse<V>,
    base: HttpResponse<X>,
    getter: (HttpResponse<X>) -> V?,
  ): MutableHttpResponse<V> {
    return response.headers(
      base.headers.asMap().flatMap {
        it.value.map { value -> it.key to value }
      }.toMap()
    ).characterEncoding(
      base.characterEncoding
    ).cookies(
      try {
        base.cookies.all
      } catch (thr: Throwable) {
        emptySet()
      }
    ).status(
      base.status
    ).contentLength(
      base.contentLength
    ).contentType(
      base.contentType.orElse(MediaType.TEXT_HTML_TYPE)
    ).apply {
      val body = getter(base)
      if (body != null) body(body)
    }
  }

  // Check the cache for `request`, and serve it from the cache if possible.
  @Cacheable("ssrContent", keyGenerator = CachedResponseKeyGenerator::class)
  internal open suspend fun ssrGenerateResponse(
    request: HttpRequest<*>,
    response: MutableHttpResponse<ByteArray>,
    block: suspend HTML.() -> Unit
  ): CachedResponse {
    val subResponse = withContext(Dispatchers.IO) {
      val responsePublisher = ssr(
        request.mutate().apply {
          // disable conditional responses when generating an origin response body
          headers.remove(HttpHeaders.IF_NONE_MATCH)
          headers.remove(HttpHeaders.IF_MODIFIED_SINCE)
        },
        block = block,
      )
      responsePublisher.block(
        Duration.ofMinutes(3)
      ) ?: error(
        "Failed to render response for page: '${request.uri}'"
      )
    }

    val dataPublisher = subResponse.body()
    val data = if (dataPublisher != null) {
      withContext(Dispatchers.IO) {
        Mono.from(dataPublisher).block(Duration.ofMinutes(3))
      } ?: error(
        "Failed to render data stream for page: '${request.uri}'"
      )
    } else {
      null
    }

    // consume data into a static buffer
    val body = data?.use {
      it.toByteArray()
    }
    return CachedResponse.from(
      request,
      copyResponse(response, subResponse) {
        body
      }
    )
  }

  // Generate a baseline response to fill with content.
  protected open fun baseResponse(): MutableHttpResponse<ByteArray> = HttpResponse.ok()

  // Generate a baseline response to fill with content.
  protected open fun csp(): List<Pair<String, String>> = listOf(
      "default-src" to "'self'",
      "script-src" to "'self' https://www.googletagmanager.com",
      "style-src" to "'self' 'unsafe-inline'",
      "img-src" to "'self' data: https://www.googletagmanager.com https://www.google-analytics.com",
      "font-src" to "https://fonts.gstatic.com",
      "connect-src" to "'self' https://www.googletagmanager.com https://www.google-analytics.com https://analytics.google.com https://stats.g.doubleclick.net",
  )

  /** Finalize an HTTP response. */
  protected open fun finalize(request: HttpRequest<*>, locale: Locale, response: MutableHttpResponse<ByteArray>) {
    // set `Content-Language` header
    response.header(
      HttpHeaders.CONTENT_LANGUAGE,
      if (!locale.country.isNullOrBlank()) {
        "${locale.language}-${locale.country}"
      } else {
        (locale.language?.ifBlank { "en-US" } ?: "en-US")
      }
    )

    // set `Vary` header
    response.headers[HttpHeaders.VARY] = sortedSetOf(
      HttpHeaders.ACCEPT_LANGUAGE,
      HttpHeaders.ACCEPT_ENCODING,
      HttpHeaders.ACCEPT,
    ).joinToString(", ")

    // add security headers
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["X-XSS-Protection"] = "1; mode=block"
    response.headers[HttpHeaders.ACCEPT_CH] = "DPR"
    response.headers["Cross-Origin-Embedder-Policy"] = "require-corp"
    response.headers["Cross-Origin-Opener-Policy"] = "same-origin"
    response.headers["Referrer-Policy"] = "no-referrer, strict-origin-when-cross-origin"

    response.headers["Permissions-Policy"] = listOf(
     "ch-dpr=(self)",
    ).joinToString(", ")

    response.headers["Content-Security-Policy"] = csp().map {
      "${it.first} ${it.second}"
    }.joinToString("; ")

    // apex caching
    if (response.status.code == 200) response.headers[HttpHeaders.CACHE_CONTROL] = listOf(
      "public",
      "max-age=60",
      "s-max-age=300",
      "proxy-revalidate",
      "stale-while-revalidate=7200",
    ).joinToString(", ")

    // add `Link` headers
    if (response.status.code == 200) response.headers.apply {
      add(HttpHeaders.LINK, "<https://fonts.gstatic.com>; rel=preconnect")
      add(HttpHeaders.LINK, "</assets/base.min.css>; rel=preload; as=style")

      val ua = request.headers["sec-ch-ua"] ?: request.headers[HttpHeaders.USER_AGENT] ?: ""
      when {
        ua.contains("Google Chrome") || ua.contains("Chrome") || ua.contains("Chromium") -> {
          add(HttpHeaders.LINK, "<https://fonts.gstatic.com/s/jetbrainsmono/v13/tDbY2o-flEEny0FZhsfKu5WU4zr3E_BX0PnT8RD8yKxTOlOVk6OThhvA.woff2>; rel=preload; as=font; crossorigin=anonymous; type=font/woff2")
        }
        ua.contains("Safari") -> {
          add(HttpHeaders.LINK, "<https://fonts.gstatic.com/s/jetbrainsmono/v13/tDbY2o-flEEny0FZhsfKu5WU4zr3E_BX0PnT8RD8yKxTOlOTk6OThhvA.woff>; rel=preload; as=font; crossorigin=anonymous; type=font/woff")
        }
      }
    }
  }

  /** Finalize an HTTP response. */
  protected open fun finalizeAsset(locale: Locale, response: StreamedAssetResponse) {
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["X-XSS-Protection"] = "1; mode=block"
    response.headers["Cross-Origin-Resource-Policy"] = "same-origin"
    response.headers["Cache-Control"] = (
      "public, max-age=900, s-max-age=3600, proxy-revalidate, stale-while-revalidate=7200"
    )
  }

  // Consult the cache, returning any found result, otherwise populate the cache with an origin execution.
  @Suppress("ReactiveStreamsUnusedPublisher")
  private suspend fun ssrCached(
    locale: Locale,
    request: HttpRequest<*>,
    response: MutableHttpResponse<ByteArray> = baseResponse(),
    block: suspend HTML.() -> Unit
  ): Mono<HttpResponse<ByteArray>> = mono {
    ssrGenerateResponse(
      request,
      response,
      block,
    ).response(request).apply {
      finalize(request, locale, this)
    }
  }

  /** @return Rendered page title. */
  protected open fun renderTitle(): String {
    return if (page.title == "Elide") {
      siteInfo.title
    } else {
      "${page.title} | ${siteInfo.title}"
    }
  }

  protected open fun pageHead(): suspend HEAD.(request: HttpRequest<*>) -> Unit = {
    link {
      rel = "icon"
      href = "/images/favicon.png"
      type = "image/png"
    }
    link {
      rel = "icon"
      href = "/images/favicon.svg"
      sizes = "any"
      type = "image/svg+xml"
    }
  }

  protected open fun headMetadata(locale: Locale): suspend HEAD.(request: HttpRequest<*>) -> Unit = {
    when (page) {
      // if the page is an `I18nPage` subsidiary, it can be interrogated for SEO information to embed in the page.
      is I18nPage -> {
        val canonical = page.canonical(locale)
        val keywords = page.keywords(locale)
        val description = page.description(locale)
        val twitterInfo = page.twitterInfo(locale)
        val ogInfo = page.openGraph(locale)

        link {
          rel = "canonical"
          href = canonical.toString()
        }
        if (description.isNotBlank()) meta {
          name = "description"
          content = description
        }
        if (keywords.isNotEmpty()) meta {
          name = "keywords"
          content = keywords.joinToString(",")
        }
        ogInfo.head(locale).invoke(this)
        twitterInfo.head(locale).invoke(this)
      }
    }
  }

  protected open fun pageBody(): suspend BODY.(request: HttpRequest<*>) -> Unit = {
    if (enableSSR) {
      if (enableStreaming) streamSSR(this@SitePageController, it)
      else injectSSR(this@SitePageController, it)
    }
  }

  @Suppress("unused")
  protected open fun tailMetadata(locale: Locale): suspend BODY.(request: HttpRequest<*>) -> Unit = {
    when (page) {
      // if the page is an `I18nPage` subsidiary, it can be interrogated for SEO information to embed in the page.
      is I18nPage -> {
        val twitterInfo = page.twitterInfo(locale)
        val ogInfo = page.openGraph(locale)
         val builder = I18nPage.LinkedDataBuilder.create()
        ogInfo.linkedData(locale).invoke(builder)
        twitterInfo.linkedData(locale).invoke(builder)

        script {
          type = "application/json+ld"

          unsafe {
            +(builder.serializeJson())
          }
        }
      }
    }
  }

  protected open fun fonts(): List<String> = emptyList()

  protected open fun preStyles(locale: Locale): suspend HEAD.(request: HttpRequest<*>) -> Unit = {
    link {
      rel = "preconnect dns-prefetch"
      href = "https://fonts.gstatic.com"
      attributes["crossorigin"] = "true"
    }
  }

  protected open fun pageStyles(locale: Locale): suspend HEAD.(request: HttpRequest<*>) -> Unit = {
    stylesheet(Assets.Styles.base)
  }

  protected open suspend fun page(
    request: HttpRequest<*>,
    head: suspend HEAD.(HttpRequest<*>) -> Unit,
    block: suspend BODY.(HttpRequest<*>) -> Unit,
  ): Mono<HttpResponse<ByteArray>> {
    // locale selected
    val locale = request.locale.orElse(I18nPage.Defaults.locale)
    return ssrCached(locale, request) {
      // set HTML lang
      lang = if (!locale.country.isNullOrBlank()) {
        "${locale.language}-${locale.country}"
      } else {
        locale.language
      }

      head {
        meta {
          charset = "utf-8"
        }

        preStyles(locale).invoke(this@head, request)
        pageStyles(locale).invoke(this@head, request)
        fonts().forEach {
          link {
            href = it
            rel = "stylesheet"
          }
        }

        title { +renderTitle() }

        script(Assets.Scripts.ui, defer = true)
        script(Assets.Scripts.analytics, async = true)
        head.invoke(this@head, request)
        headMetadata(locale).invoke(this@head, request)
      }
      body {
        block.invoke(this@body, request)
        tailMetadata(locale).invoke(this@body, request)
      }
    }
  }

  protected open suspend fun page(
    request: HttpRequest<*>,
    block: suspend BODY.(HttpRequest<*>) -> Unit = pageBody(),
  ) = page(
    request,
    pageHead(),
    block,
  )
}
