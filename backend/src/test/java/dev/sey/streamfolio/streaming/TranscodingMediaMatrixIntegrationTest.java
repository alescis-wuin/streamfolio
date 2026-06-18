package dev.sey.streamfolio.streaming;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sey.streamfolio.catalog.CatalogService;
import dev.sey.streamfolio.domain.CatalogVideo;
import dev.sey.streamfolio.transcoding.FfmpegService;
import dev.sey.streamfolio.transcoding.HlsTranscodeResult;
import dev.sey.streamfolio.transcoding.TranscodingService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TranscodingMediaMatrixIntegrationTest {
    private static final Long VIDEO_ID = 777L;
    private static final int EXPECTED_THUMBNAILS = 2;
    private static final String VARIANTS = "144p:256:144:150k:200000,240p:426:240:350k:450000";
    private static final String FFMPEG = System.getenv().getOrDefault("STREAMFOLIO_TEST_FFMPEG", "ffmpeg");
    private static final String FFPROBE = System.getenv().getOrDefault("STREAMFOLIO_TEST_FFPROBE", "ffprobe");

    @ParameterizedTest(name = "local CPU transcodes {0}")
    @MethodSource("acceptedFormats")
    void localCpuTranscodesAcceptedFormats(FormatSpec format, @TempDir Path root) throws Exception {
        assumeMatrixEnabled();
        Path source = generateSource(format, root);
        TranscodingService service = service(root, localStorage(root), "none", "h264_nvenc", false);

        HlsTranscodeResult result = service.transcodeToHls(VIDEO_ID, true);

        assertThat(result.generated()).isTrue();
        assertThat(result.playlist()).exists();
        assertOutputTree(root, VIDEO_ID);
        assertThat(source).exists();
    }

    @Test
    void minioCpuTranscodesAndPublishesDerivedOutputs(@TempDir Path root) throws Exception {
        assumeMatrixEnabled();
        generateSource(FormatSpec.mp4(), root);
        MinioMediaGateway minio = minioMock();
        MediaStorageService storage = new MediaStorageService("minio", root.toString(), minio, false);
        TranscodingService service = service(root, storage, "none", "h264_nvenc", false);

        HlsTranscodeResult result = service.transcodeToHls(VIDEO_ID, true);

        assertThat(result.generated()).isTrue();
        assertOutputTree(root, VIDEO_ID);
        verify(minio).uploadTree(root.resolve("hls").resolve(VIDEO_ID.toString()), "hls/" + VIDEO_ID);
        verify(minio).uploadTree(root.resolve("thumbnails").resolve(VIDEO_ID.toString()), "thumbnails/" + VIDEO_ID);
    }

    @Test
    void localGpuProfileTranscodesWhenGpuMatrixIsEnabled(@TempDir Path root) throws Exception {
        assumeGpuMatrixEnabled();
        String gpuEncoder = gpuEncoder();
        assumeEncoderAvailable(gpuEncoder);
        generateSource(FormatSpec.mp4(), root);
        TranscodingService service = service(root, localStorage(root), "nvidia", gpuEncoder, false);

        HlsTranscodeResult result = service.transcodeToHls(VIDEO_ID, true);

        assertThat(result.generated()).isTrue();
        assertOutputTree(root, VIDEO_ID);
    }

    @Test
    void minioGpuProfileTranscodesAndPublishesWhenGpuMatrixIsEnabled(@TempDir Path root) throws Exception {
        assumeGpuMatrixEnabled();
        String gpuEncoder = gpuEncoder();
        assumeEncoderAvailable(gpuEncoder);
        generateSource(FormatSpec.mp4(), root);
        MinioMediaGateway minio = minioMock();
        MediaStorageService storage = new MediaStorageService("minio", root.toString(), minio, false);
        TranscodingService service = service(root, storage, "nvidia", gpuEncoder, false);

        HlsTranscodeResult result = service.transcodeToHls(VIDEO_ID, true);

        assertThat(result.generated()).isTrue();
        assertOutputTree(root, VIDEO_ID);
        verify(minio).uploadTree(root.resolve("hls").resolve(VIDEO_ID.toString()), "hls/" + VIDEO_ID);
        verify(minio).uploadTree(root.resolve("thumbnails").resolve(VIDEO_ID.toString()), "thumbnails/" + VIDEO_ID);
    }

    private static Stream<FormatSpec> acceptedFormats() {
        return Stream.of(
            new FormatSpec("mp4", "mp4", h264()),
            new FormatSpec("m4v", "mp4", h264()),
            new FormatSpec("mov", "mov", h264()),
            new FormatSpec("qt", "mov", h264()),
            new FormatSpec("mkv", "matroska", h264()),
            new FormatSpec("webm", "webm", List.of("-c:v", "libvpx-vp9", "-b:v", "220k", "-an")),
            new FormatSpec("avi", "avi", List.of("-c:v", "mpeg4", "-q:v", "5", "-an")),
            new FormatSpec("divx", "avi", List.of("-c:v", "mpeg4", "-q:v", "5", "-an")),
            new FormatSpec("wmv", "asf", List.of("-c:v", "wmv2", "-an")),
            new FormatSpec("asf", "asf", List.of("-c:v", "wmv2", "-an")),
            new FormatSpec("flv", "flv", List.of("-c:v", "flv1", "-an")),
            new FormatSpec("f4v", "flv", List.of("-c:v", "flv1", "-an")),
            new FormatSpec("ts", "mpegts", mpeg2()),
            new FormatSpec("m2ts", "mpegts", mpeg2()),
            new FormatSpec("mts", "mpegts", mpeg2()),
            new FormatSpec("m2t", "mpegts", mpeg2()),
            new FormatSpec("mpeg", "mpeg", mpeg2()),
            new FormatSpec("mpg", "mpeg", mpeg2()),
            new FormatSpec("mpe", "mpeg", mpeg2()),
            new FormatSpec("m1v", "mpeg", mpeg2()),
            new FormatSpec("m2v", "mpeg", mpeg2()),
            new FormatSpec("m2p", "mpeg", mpeg2()),
            new FormatSpec("ps", "mpeg", mpeg2()),
            new FormatSpec("vob", "mpeg", mpeg2()),
            new FormatSpec("ogv", "ogg", List.of("-c:v", "libtheora", "-q:v", "5", "-an")),
            new FormatSpec("ogg", "ogg", List.of("-c:v", "libtheora", "-q:v", "5", "-an")),
            new FormatSpec("3gp", "3gp", List.of("-c:v", "mpeg4", "-q:v", "5", "-an")),
            new FormatSpec("3g2", "3gp", List.of("-c:v", "mpeg4", "-q:v", "5", "-an")),
            new FormatSpec("mxf", "mxf", List.of("-c:v", "mpeg2video", "-pix_fmt", "yuv422p", "-b:v", "1M", "-an")),
            new FormatSpec("swf", "swf", List.of("-c:v", "flv1", "-an")),
            new FormatSpec("rm", "rm", List.of("-c:v", "rv10", "-an")),
            new FormatSpec("rmvb", "rm", List.of("-c:v", "rv10", "-an")),
            new FormatSpec("mod", "mpeg", mpeg2()),
            new FormatSpec("tod", "mpeg", mpeg2()),
            new FormatSpec("dat", "mpeg", mpeg2())
        );
    }

    private static List<String> h264() {
        return List.of("-c:v", "libx264", "-pix_fmt", "yuv420p", "-preset", "ultrafast", "-an");
    }

    private static List<String> mpeg2() {
        return List.of("-c:v", "mpeg2video", "-b:v", "500k", "-an");
    }

    private Path generateSource(FormatSpec format, Path root) throws Exception {
        Files.createDirectories(root.resolve("originals"));
        Path source = root.resolve("originals").resolve("matrix-" + format.extension() + "." + format.extension());
        List<String> command = new ArrayList<>();
        command.add(FFMPEG);
        command.addAll(List.of("-hide_banner", "-loglevel", "error", "-y", "-f", "lavfi", "-i", "testsrc2=size=320x180:rate=12:duration=1"));
        command.addAll(format.codecArgs());
        command.addAll(List.of("-t", "1", "-f", format.muxer(), source.toString()));
        CommandOutput output = run(command);
        Assumptions.assumeTrue(output.exitCode() == 0, "FFmpeg cannot generate " + format + ": " + output.output());
        return source;
    }

    private TranscodingService service(Path root, MediaStorageService storage, String acceleration, String encoder, boolean classpathFallback) {
        CatalogService catalog = mock(CatalogService.class);
        String filename = Files.exists(root.resolve("originals"))
            ? root.resolve("originals").toFile().list()[0]
            : "matrix.mp4";
        CatalogVideo video = new CatalogVideo(0, 0, "Matrix", "Matrix", 1, filename, "empty.vtt");
        when(catalog.findVideo(VIDEO_ID)).thenReturn(video);
        return new TranscodingService(
            catalog,
            storage,
            new FfmpegService(FFMPEG, Duration.ofSeconds(10)),
            Duration.ofSeconds(45),
            1,
            "independent_segments+temp_file",
            "vod",
            EXPECTED_THUMBNAILS,
            acceleration,
            encoder,
            "fast",
            "ultrafast",
            VARIANTS
        );
    }

    private MediaStorageService localStorage(Path root) {
        return new MediaStorageService("local", root.toString(), null, false);
    }

    private MinioMediaGateway minioMock() {
        MinioMediaGateway minio = mock(MinioMediaGateway.class);
        when(minio.exists(anyString())).thenReturn(false);
        doAnswer(invocation -> invocation.getArgument(1) + "/" + invocation.getArgument(0)).when(minio).videoObjectPrefix(anyLong(), anyString());
        doAnswer(invocation -> invocation.getArgument(0) + "/" + invocation.getArgument(1)).when(minio).objectName(anyString(), anyString());
        return minio;
    }

    private void assertOutputTree(Path root, Long videoId) throws Exception {
        Path hlsRoot = root.resolve("hls").resolve(videoId.toString());
        Path master = hlsRoot.resolve("master.m3u8");
        assertThat(master).exists();
        assertThat(Files.readString(master)).contains("#EXTM3U", "144p/playlist.m3u8", "240p/playlist.m3u8", "RESOLUTION=256x144", "RESOLUTION=426x240");
        assertVariant(hlsRoot, "144p", "256,144");
        assertVariant(hlsRoot, "240p", "426,240");

        Path thumbnailRoot = root.resolve("thumbnails").resolve(videoId.toString());
        assertThat(thumbnailRoot.resolve("manifest.json")).exists();
        assertThat(Files.readString(thumbnailRoot.resolve("manifest.json"))).contains("thumb_000.jpg", "thumb_001.jpg");
        try (var thumbnails = Files.list(thumbnailRoot)) {
            assertThat(thumbnails.filter(path -> path.getFileName().toString().endsWith(".jpg")).count()).isEqualTo(EXPECTED_THUMBNAILS);
        }
    }

    private void assertVariant(Path hlsRoot, String variant, String dimensions) throws Exception {
        Path directory = hlsRoot.resolve(variant);
        Path playlist = directory.resolve("playlist.m3u8");
        assertThat(playlist).exists();
        assertThat(Files.readString(playlist)).contains("#EXTM3U", "segment_");
        List<Path> segments;
        try (var stream = Files.list(directory)) {
            segments = stream.filter(path -> path.getFileName().toString().endsWith(".ts")).toList();
        }
        assertThat(segments).isNotEmpty();
        CommandOutput probe = run(List.of(FFPROBE, "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=width,height", "-of", "csv=p=0", segments.get(0).toString()));
        Assumptions.assumeTrue(probe.exitCode() == 0, "ffprobe cannot inspect generated segment: " + probe.output());
        assertThat(probe.output().trim()).contains(dimensions);
    }

    private void assumeMatrixEnabled() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getenv().getOrDefault("STREAMFOLIO_RUN_TRANSCODE_MATRIX", "false")),
            "Set STREAMFOLIO_RUN_TRANSCODE_MATRIX=true to run the heavy FFmpeg matrix.");
        Assumptions.assumeTrue(commandAvailable(FFMPEG), "FFmpeg binary not available: " + FFMPEG);
        Assumptions.assumeTrue(commandAvailable(FFPROBE), "FFprobe binary not available: " + FFPROBE);
    }

    private void assumeGpuMatrixEnabled() {
        assumeMatrixEnabled();
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getenv().getOrDefault("STREAMFOLIO_RUN_TRANSCODE_GPU", "false")),
            "Set STREAMFOLIO_RUN_TRANSCODE_GPU=true to run GPU-profile transcode tests.");
    }

    private void assumeEncoderAvailable(String encoder) {
        FfmpegService ffmpeg = new FfmpegService(FFMPEG, Duration.ofSeconds(10));
        Assumptions.assumeTrue(ffmpeg.hasEncoder(encoder), "Encoder unavailable in FFmpeg build: " + encoder);
    }

    private String gpuEncoder() {
        return System.getenv().getOrDefault("STREAMFOLIO_TEST_GPU_ENCODER", "h264_nvenc");
    }

    private boolean commandAvailable(String command) {
        try {
            return new ProcessBuilder(command, "-version").start().waitFor() == 0;
        } catch (Exception exception) {
            return false;
        }
    }

    private CommandOutput run(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        byte[] output = process.getInputStream().readAllBytes();
        int exitCode = process.waitFor();
        return new CommandOutput(exitCode, new String(output, StandardCharsets.UTF_8));
    }

    private record FormatSpec(String extension, String muxer, List<String> codecArgs) {
        static FormatSpec mp4() {
            return new FormatSpec("mp4", "mp4", h264());
        }

        @Override
        public String toString() {
            return extension.toUpperCase(Locale.ROOT) + " via " + muxer;
        }
    }

    private record CommandOutput(int exitCode, String output) {
    }
}
