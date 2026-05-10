package com.king.paysim.domain.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Assignment(
        @JsonProperty("assignee_id") Long assigneeId,
        @JsonProperty("assignee_type") String assigneeType
) {}
