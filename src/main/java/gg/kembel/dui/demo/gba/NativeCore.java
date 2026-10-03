package gg.kembel.dui.demo.gba;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.ZipInputStream;

/** Extracts small, already built binaries; no compiler, installation or runtime network request. */
final class NativeCore {
  static synchronized Path install(Path directory) throws Exception {
    String os=System.getProperty("os.name").toLowerCase(Locale.ROOT),arch=System.getProperty("os.arch");
    String platform=os.contains("mac")&&(arch.equals("aarch64")||arch.equals("arm64"))?"macos-arm64"
        :os.contains("linux")&&(arch.equals("amd64")||arch.equals("x86_64"))?"linux-x64":null;
    if(platform==null)throw new IllegalStateException("Bundled mGBA supports macOS ARM64 and Linux x64. Server platform: "+os+"/"+arch);
    JsonObject manifest;
    try(var in=NativeCore.class.getResourceAsStream("/gba/native/manifest.json")) {
      manifest=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
    }
    var artifact=manifest.getAsJsonObject("artifacts").getAsJsonObject(platform);
    byte[] archive;
    try(var in=NativeCore.class.getResourceAsStream("/gba/native/"+platform+".zip")) { archive=Objects.requireNonNull(in).readAllBytes(); }
    String expected=artifact.get("sha256").getAsString();
    if(!hash(archive).equals(expected))throw new IOException("Bundled core checksum failed");
    String binary=artifact.get("binary").getAsString();
    Path target=directory.resolve(expected).resolve(binary);
    byte[] bytes=null;
    try(var zip=new ZipInputStream(new ByteArrayInputStream(archive))) {
      for(var entry=zip.getNextEntry();entry!=null;entry=zip.getNextEntry())if(entry.getName().equals(binary)) {
        bytes=zip.readNBytes(16*1024*1024+1);break;
      }
    }
    if(bytes==null||bytes.length>16*1024*1024)throw new IOException("Invalid bundled core archive");
    Files.createDirectories(target.getParent());
    if(!Files.exists(target)||!hash(Files.readAllBytes(target)).equals(hash(bytes))) {
      Path tmp=target.resolveSibling(binary+".tmp");Files.write(tmp,bytes);
      Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING);
    }
    return target;
  }
  static String hash(byte[] bytes) throws NoSuchAlgorithmException { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
}
