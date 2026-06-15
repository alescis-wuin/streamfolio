package dev.sey.streamfolio.transcoding;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/media")
public class TranscodeAdminController {
    private final TranscodeJobService jobs;

    public TranscodeAdminController(TranscodeJobService jobs) {
        this.jobs = jobs;
    }

    @GetMapping("/jobs")
    public List<TranscodeJobDto> jobs() {
        return jobs.recentJobs();
    }

    @GetMapping("/jobs/{jobId}")
    public TranscodeJobDto job(@PathVariable Long jobId) {
        return jobs.job(jobId);
    }

    @GetMapping("/assets")
    public List<MediaAssetDto> assets() {
        return jobs.assets();
    }

    @PostMapping("/videos/{videoId}/transcode")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TranscodeJobDto transcode(@PathVariable Long videoId, @RequestBody(required = false) TranscodeRequest request) {
        return jobs.submit(videoId, request != null && request.force());
    }
}
