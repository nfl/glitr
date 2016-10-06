package com.nfl.glitr.data.mutation;

import com.nfl.glitr.data.query.Playable;

import java.time.Instant;
import java.time.LocalDate;

public class Bitrate implements Playable{

    private String id;
    private Integer kbps;
    private String url;
    private Integer[] frames;
    private Float gradeAverage;
    private Double grade;
    private Long durationNanos;
    private Boolean valid;
    private LocalDate createdDate;
    private Instant modifiedDateTime;

    public Integer getKbps() {
        return kbps;
    }

    public String getUrl() {
        return url;
    }

    public Long getDurationNanos() {
        return durationNanos;
    }

    public Integer[] getFrames() {
        return frames;
    }

    public Double getGrade() {
        return grade;
    }

    public Float getGradeAverage() {
        return gradeAverage;
    }

    public Boolean getValid() {
        return valid;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public Instant getModifiedDateTime() {
        return modifiedDateTime;
    }

    @Override
    public String getId() {
        return id;
    }
}
