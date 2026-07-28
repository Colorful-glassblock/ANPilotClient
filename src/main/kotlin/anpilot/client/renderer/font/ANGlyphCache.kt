package anpilot.client.renderer.font

import java.io.InputStream

object ANGlyphCache {
    fun create(resource: InputStream): ANStbFontAtlas = ANStbFontAtlas.load(resource)
}
