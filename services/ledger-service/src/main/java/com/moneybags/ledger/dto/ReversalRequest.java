package com.moneybags.ledger.dto;

import jakarta.validation.constraints.Size;

public record ReversalRequest(
        @Size(max = 100) String journalReference,
        @Size(max = 500) String description,
        @Size(max = 100) String createdBy
) {}
