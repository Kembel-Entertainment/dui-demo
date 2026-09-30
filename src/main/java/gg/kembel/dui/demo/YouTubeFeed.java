package gg.kembel.dui.demo;

import gg.kembel.dui.core.*;
import java.io.*;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/** Atom / Media RSS parser, with fixed YouTube link and thumbnail origins. */
public final class YouTubeFeed {
  public static final String CHANNEL = "UC1sELGmy5jp5fQUugmuYlXQ";
  public static final URI URL =
      URI.create("https://www.youtube.com/feeds/videos.xml?channel_id=" + CHANNEL);
  private static final String ATOM = "http://www.w3.org/2005/Atom",
      YT = "http://www.youtube.com/xml/schemas/2015",
      MEDIA = "http://search.yahoo.com/mrss/";

  public record Video(String id, String title, Instant published, URI thumbnail) {
    public String watchUrl() {
      return "https://www.youtube.com/watch?v=" + id;
    }
  }

  public record Feed(String channel, List<Video> videos, Instant checkedAt) {
    public Feed {
      videos = List.copyOf(videos);
    }
  }

  private YouTubeFeed() {}

  public static Feed parse(byte[] bytes, Instant checkedAt) throws Exception {
    if (bytes.length > 512_000) throw new IOException("Feed too large");
    var factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    var root =
        factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes)).getDocumentElement();
    // YouTube's root channelId may omit the UC prefix; entries use the full ID.
    String channelId = direct(root, YT, "channelId");
    if (!root.getLocalName().equals("feed")
        || !ATOM.equals(root.getNamespaceURI())
        || !Set.of(CHANNEL, CHANNEL.substring(2)).contains(channelId))
      throw new IOException("Unexpected YouTube channel feed");
    String channel = direct(root, ATOM, "title");
    if (channel.isBlank() || channel.length() > 200) throw new IOException("Invalid channel title");
    var videos = new ArrayList<Video>();
    var seen = new HashSet<String>();
    var entries = root.getElementsByTagNameNS(ATOM, "entry");
    for (int i = 0; i < entries.getLength() && videos.size() < 15; i++) {
      var e = (Element) entries.item(i);
      String id = direct(e, YT, "videoId"), title = direct(e, ATOM, "title");
      String entryChannel = direct(e, YT, "channelId");
      if (!entryChannel.isEmpty() && !CHANNEL.equals(entryChannel))
        throw new IOException("Unexpected video channel");
      if (!id.matches("[A-Za-z0-9_-]{11}")
          || title.isBlank()
          || title.length() > 500
          || !seen.add(id)) continue;
      var thumbnails = e.getElementsByTagNameNS(MEDIA, "thumbnail");
      if (thumbnails.getLength() == 0) continue;
      URI thumbnail = URI.create(((Element) thumbnails.item(0)).getAttribute("url"));
      if (!allowedThumbnail(thumbnail, id)) throw new IOException("Unexpected thumbnail origin");
      videos.add(new Video(id, title, Instant.parse(direct(e, ATOM, "published")), thumbnail));
    }
    videos.sort(Comparator.comparing(Video::published).reversed());
    return new Feed(channel, videos, checkedAt);
  }

  public static boolean allowedThumbnail(URI uri, String id) {
    String host = uri.getHost();
    return "https".equals(uri.getScheme())
        && uri.getPort() == -1
        && uri.getUserInfo() == null
        && host != null
        && host.matches("(?:i[1-9]?|img)\\.ytimg\\.com")
        && uri.getPath().startsWith("/vi/" + id + "/")
        && uri.getQuery() == null
        && uri.getFragment() == null;
  }

  private static String direct(Element parent, String namespace, String tag) {
    for (var n = parent.getFirstChild(); n != null; n = n.getNextSibling())
      if (n instanceof Element e
          && namespace.equals(e.getNamespaceURI())
          && tag.equals(e.getLocalName())) return e.getTextContent().strip();
    return "";
  }
}
