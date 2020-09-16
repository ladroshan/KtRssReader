package tw.ktrssreader.parser

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import tw.ktrssreader.constant.ParserConst
import tw.ktrssreader.model.channel.Cloud
import tw.ktrssreader.model.channel.Image
import tw.ktrssreader.model.channel.RssStandardChannel
import tw.ktrssreader.model.channel.TextInput
import tw.ktrssreader.model.item.Category
import tw.ktrssreader.model.item.Enclosure
import tw.ktrssreader.model.item.RssStandardItem
import tw.ktrssreader.constant.ParserConst.AUTHOR
import tw.ktrssreader.constant.ParserConst.CATEGORY
import tw.ktrssreader.constant.ParserConst.CHANNEL
import tw.ktrssreader.constant.ParserConst.CLOUD
import tw.ktrssreader.constant.ParserConst.COMMENTS
import tw.ktrssreader.constant.ParserConst.COPYRIGHT
import tw.ktrssreader.constant.ParserConst.DESCRIPTION
import tw.ktrssreader.constant.ParserConst.DOCS
import tw.ktrssreader.constant.ParserConst.DOMAIN
import tw.ktrssreader.constant.ParserConst.ENCLOSURE
import tw.ktrssreader.constant.ParserConst.GENERATOR
import tw.ktrssreader.constant.ParserConst.GUID
import tw.ktrssreader.constant.ParserConst.IMAGE
import tw.ktrssreader.constant.ParserConst.ITEM
import tw.ktrssreader.constant.ParserConst.LANGUAGE
import tw.ktrssreader.constant.ParserConst.LAST_BUILD_DATE
import tw.ktrssreader.constant.ParserConst.LENGTH
import tw.ktrssreader.constant.ParserConst.LINK
import tw.ktrssreader.constant.ParserConst.MANAGING_EDITOR
import tw.ktrssreader.constant.ParserConst.NAME
import tw.ktrssreader.constant.ParserConst.PATH
import tw.ktrssreader.constant.ParserConst.PORT
import tw.ktrssreader.constant.ParserConst.PROTOCOL
import tw.ktrssreader.constant.ParserConst.PUB_DATE
import tw.ktrssreader.constant.ParserConst.RATING
import tw.ktrssreader.constant.ParserConst.REGISTER_PROCEDURE
import tw.ktrssreader.constant.ParserConst.SKIP_DAYS
import tw.ktrssreader.constant.ParserConst.SKIP_HOURS
import tw.ktrssreader.constant.ParserConst.SOURCE
import tw.ktrssreader.constant.ParserConst.TEXT_INPUT
import tw.ktrssreader.constant.ParserConst.TITLE
import tw.ktrssreader.constant.ParserConst.TTL
import tw.ktrssreader.constant.ParserConst.TYPE
import tw.ktrssreader.constant.ParserConst.URL
import tw.ktrssreader.constant.ParserConst.WEB_MASTER
import java.io.ByteArrayInputStream
import java.io.IOException

abstract class ParserBase<out T : RssStandardChannel> : Parser<T> {

    protected fun parseStandardChannel(xml: String): RssStandardChannel {
        val parser = getXmlParser(xml)

        var result: RssStandardChannel? = null
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue

            if (parser.name == ParserConst.CHANNEL) {
                result = parser.readRssStandardChannel()
                break
            } else {
                parser.skip()
            }
        }
        return result ?: throw XmlPullParserException("No valid channel tag in the RSS feed.")
    }

    protected fun getXmlParser(xml: String): XmlPullParser {
        ByteArrayInputStream(xml.toByteArray()).use { inputStream ->
            return Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(inputStream, null)
                nextTag()
            }
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    protected fun XmlPullParser.readString(tagName: String): String? {
        require(XmlPullParser.START_TAG, null, tagName)
        var content: String? = null
        if (next() == XmlPullParser.TEXT) {
            content = text
            nextTag()
        }
        require(XmlPullParser.END_TAG, null, tagName)
        return content
    }

    @Throws(IOException::class, XmlPullParserException::class)
    protected fun XmlPullParser.readAttributes(
        tagName: String,
        attributes: List<String>,
        action: (String, String?) -> Unit
    ) {
        require(XmlPullParser.START_TAG, null, tagName)
        attributes.forEach { attr ->
            action(attr, getAttributeValue(null, attr))
        }
        nextTag()
        require(XmlPullParser.END_TAG, null, tagName)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    protected fun XmlPullParser.skip() {
        if (eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun XmlPullParser.readRssStandardChannel(): RssStandardChannel {
        require(XmlPullParser.START_TAG, null, ParserConst.CHANNEL)

        var title: String? = null
        var description: String? = null
        var image: Image? = null
        var language: String? = null
        val categories = mutableListOf<String>()
        var link: String? = null
        var copyright: String? = null
        var managingEditor: String? = null
        var webMaster: String? = null
        var pubDate: String? = null
        var lastBuildDate: String? = null
        var generator: String? = null
        var docs: String? = null
        var cloud: Cloud? = null
        var ttl: Int? = null
        var rating: Float? = null
        var textInput: TextInput? = null
        var skipHours: Int? = null
        var skipDays: String? = null
        val items = mutableListOf<RssStandardItem>()

        while (next() != XmlPullParser.END_TAG) {
            if (eventType != XmlPullParser.START_TAG) continue

            when (this.name) {
                TITLE -> title = readString(TITLE)
                DESCRIPTION -> description = readString(DESCRIPTION)
                LINK -> link = readString(LINK)
                IMAGE -> image = readImage()
                LANGUAGE -> language = readString(LANGUAGE)
                CATEGORY -> readString(CATEGORY)?.let { categories.add(it) }
                COPYRIGHT -> copyright = readString(COPYRIGHT)
                MANAGING_EDITOR -> managingEditor = readString(MANAGING_EDITOR)
                WEB_MASTER -> webMaster = readString(WEB_MASTER)
                PUB_DATE -> pubDate = readString(PUB_DATE)
                LAST_BUILD_DATE -> lastBuildDate = readString(LAST_BUILD_DATE)
                GENERATOR -> generator = readString(GENERATOR)
                DOCS -> docs = readString(DOCS)
                CLOUD -> cloud = readCloud()
                TTL -> ttl = readString(TTL)?.toIntOrNull()
                RATING -> rating = readString(RATING)?.toFloatOrNull()
                TEXT_INPUT -> textInput = readTextInput()
                SKIP_HOURS -> skipHours = readString(SKIP_HOURS)?.toIntOrNull()
                SKIP_DAYS -> skipDays = readString(SKIP_DAYS)
                ITEM -> items.add(readRssStandardItem())
                else -> skip()
            }
        }
        require(XmlPullParser.END_TAG, null, CHANNEL)
        return RssStandardChannel(
            title = title,
            description = description,
            image = image,
            language = language,
            categories = categories,
            link = link,
            copyright = copyright,
            managingEditor = managingEditor,
            webMaster = webMaster,
            pubDate = pubDate,
            lastBuildDate = lastBuildDate,
            generator = generator,
            docs = docs,
            cloud = cloud,
            ttl = ttl,
            rating = rating,
            textInput = textInput,
            skipHours = skipHours,
            skipDays = skipDays,
            items = items
        )
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun XmlPullParser.readRssStandardItem(): RssStandardItem {
        require(XmlPullParser.START_TAG, null, ITEM)
        var title: String? = null
        var enclosure: Enclosure? = null
        var guid: String? = null
        var pubDate: String? = null
        var description: String? = null
        var link: String? = null
        var author: String? = null
        val categories: MutableList<Category> = mutableListOf()
        var comments: String? = null
        var source: String? = null
        while (next() != XmlPullParser.END_TAG) {
            if (eventType != XmlPullParser.START_TAG) continue

            when (this.name) {
                TITLE -> title = readString(TITLE)
                ENCLOSURE -> enclosure = readEnclosure()
                GUID -> guid = readString(GUID)
                PUB_DATE -> pubDate = readString(PUB_DATE)
                DESCRIPTION -> description = readString(DESCRIPTION)
                LINK -> link = readString(LINK)
                AUTHOR -> author = readString(AUTHOR)
                CATEGORY -> readItemCategory().let { categories.add(it) }
                COMMENTS -> comments = readString(COMMENTS)
                SOURCE -> source = readString(SOURCE)
                else -> skip()
            }
        }
        require(XmlPullParser.END_TAG, null, ITEM)
        return RssStandardItem(
            title = title,
            enclosure = enclosure,
            guid = guid,
            pubDate = pubDate,
            description = description,
            link = link,
            author = author,
            categories = categories,
            comments = comments,
            source = source
        )
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun XmlPullParser.readImage(): Image? {
        require(XmlPullParser.START_TAG, null, IMAGE)
        var link: String? = null
        var title: String? = null
        var url: String? = null
        while (next() != XmlPullParser.END_TAG) {
            if (eventType != XmlPullParser.START_TAG) continue

            when (this.name) {
                LINK -> link = readString(LINK)
                TITLE -> title = readString(TITLE)
                URL -> url = readString(URL)
                else -> skip()
            }
        }
        require(XmlPullParser.END_TAG, null, IMAGE)
        return Image(link = link, title = title, url = url)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun XmlPullParser.readEnclosure(): Enclosure {
        var url: String? = null
        var length: Long? = null
        var type: String? = null
        readAttributes(ENCLOSURE, listOf(URL, LENGTH, TYPE)) { attr, value ->
            when (attr) {
                URL -> url = value
                LENGTH -> length = value?.toLongOrNull()
                TYPE -> type = value
            }
        }
        return Enclosure(url = url, length = length, type = type)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun XmlPullParser.readItemCategory(): Category {
        require(XmlPullParser.START_TAG, null, CATEGORY)
        val domain: String? = getAttributeValue(null, DOMAIN)
        val name: String? = readString(tagName = CATEGORY)
        require(XmlPullParser.END_TAG, null, CATEGORY)
        return Category(name = name, domain = domain)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun XmlPullParser.readCloud(): Cloud {
        var domain: String? = null
        var port: Int? = null
        var path: String? = null
        var registerProcedure: String? = null
        var protocol: String? = null
        readAttributes(CLOUD, listOf(DOMAIN, PORT, PATH, REGISTER_PROCEDURE, PROTOCOL)) { attr, value ->
            when (attr) {
                DOMAIN -> domain = value
                PORT -> port = value?.toIntOrNull()
                PATH -> path = value
                REGISTER_PROCEDURE -> registerProcedure = value
                PROTOCOL -> protocol = value
            }
        }
        return Cloud(
            domain = domain,
            port = port,
            path = path,
            registerProcedure = registerProcedure,
            protocol = protocol
        )
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun XmlPullParser.readTextInput(): TextInput {
        require(XmlPullParser.START_TAG, null, TEXT_INPUT)
        var title: String? = null
        var description: String? = null
        var name: String? = null
        var link: String? = null
        while (next() != XmlPullParser.END_TAG) {
            if (eventType != XmlPullParser.START_TAG) continue

            when (this.name) {
                TITLE -> title = readString(TITLE)
                DESCRIPTION -> description = readString(DESCRIPTION)
                NAME -> name = readString(NAME)
                LINK -> link = readString(LINK)
                else -> skip()
            }
        }
        require(XmlPullParser.END_TAG, null, TEXT_INPUT)
        return TextInput(title = title, description = description, name = name, link = link)
    }
}