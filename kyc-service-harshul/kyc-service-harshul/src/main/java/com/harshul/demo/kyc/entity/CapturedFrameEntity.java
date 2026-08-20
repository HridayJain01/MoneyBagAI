package com.harshul.demo.kyc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "KYC_FRAME")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CapturedFrameEntity extends AbstractEntity {

    @Column(name = "FRAME_NUMBER", nullable = false)
    private int frameNumber;

    @Column(name = "ORIGINAL_FILE_NAME", length = 255)
    private String originalFileName;

    @Column(name = "CONTENT_TYPE", length = 100)
    private String contentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SESSION_ID", nullable = false)
    private KycSessionEntity session;

    @Lob
    @Column(name = "CONTENT", nullable = false)
    private byte[] content;
}
