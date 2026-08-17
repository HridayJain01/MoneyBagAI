CREATE TABLE kyc_sessions (
    id VARCHAR(36) NOT NULL,
    cif_no VARCHAR(30) NOT NULL,
    purpose VARCHAR(100),
    document_type VARCHAR(30) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_kyc_sessions_cif_status (cif_no, status, created_at)
);

CREATE TABLE kyc_documents (
    id VARCHAR(36) NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT NOT NULL,
    content LONGBLOB NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_kyc_document_session_type UNIQUE (session_id, document_type),
    CONSTRAINT fk_kyc_document_session FOREIGN KEY (session_id) REFERENCES kyc_sessions(id)
);

CREATE TABLE kyc_frames (
    id VARCHAR(36) NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    frame_number INT NOT NULL,
    original_file_name VARCHAR(255),
    content_type VARCHAR(100),
    content LONGBLOB NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_kyc_frame_session_number UNIQUE (session_id, frame_number),
    CONSTRAINT fk_kyc_frame_session FOREIGN KEY (session_id) REFERENCES kyc_sessions(id)
);

CREATE TABLE kyc_verifications (
    id VARCHAR(36) NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    result VARCHAR(500) NOT NULL,
    reviewer_id VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_kyc_verification_session UNIQUE (session_id),
    CONSTRAINT fk_kyc_verification_session FOREIGN KEY (session_id) REFERENCES kyc_sessions(id)
);
