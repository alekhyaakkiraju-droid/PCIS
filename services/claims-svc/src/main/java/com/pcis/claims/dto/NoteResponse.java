package com.pcis.claims.dto;

import java.time.Instant;

public record NoteResponse(Long noteId, String claimNbr, String noteText, Instant createdAt) {}
