package com.harshul.demo.kyc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kyc_frames")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CapturedFrameEntity extends AbstractEntity {

    @Column(name = "frame_number", nullable = false)
    private int frameNumber;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private KycSessionEntity session;

    @Lob
    @Column(name = "content", nullable = false)
    private byte[] content;
}
