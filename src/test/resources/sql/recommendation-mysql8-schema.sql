CREATE TABLE jobs (
    job_id BIGINT NOT NULL PRIMARY KEY,
    parent_job_id BIGINT NULL
);

CREATE TABLE maps (
    map_id BIGINT NOT NULL PRIMARY KEY
);

CREATE TABLE alrim (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(31) NOT NULL,
    date TIMESTAMP NOT NULL,
    INDEX idx_alrim_type_date (type, date)
);

CREATE TABLE recommendation_extracted_claims (
    extracted_claim_id BIGINT NOT NULL PRIMARY KEY,
    source_published_at TIMESTAMP NOT NULL
);

CREATE TABLE recommendation_reviewed_claims (
    reviewed_claim_id BIGINT NOT NULL PRIMARY KEY,
    extracted_claim_id BIGINT NOT NULL,
    review_status VARCHAR(20) NOT NULL,
    final_map_id BIGINT NULL,
    final_job_id BIGINT NULL,
    final_level_min INT NULL,
    final_level_max INT NULL,
    final_polarity VARCHAR(20) NULL,
    UNIQUE KEY uk_recommendation_reviewed_claims_extracted (
        extracted_claim_id,
        final_map_id,
        final_job_id
    ),
    INDEX idx_recommendation_reviewed_claims_scoring (review_status, final_job_id)
);

CREATE TABLE recommendation_reviewed_claim_reasons (
    reviewed_claim_reason_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reviewed_claim_id BIGINT NOT NULL,
    reason_axis VARCHAR(30) NOT NULL,
    reason_value VARCHAR(30) NOT NULL,
    display_order INT NULL,
    UNIQUE KEY uk_recommendation_reviewed_claim_reasons (
        reviewed_claim_id,
        reason_axis,
        reason_value
    )
);
