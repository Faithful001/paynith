package com.king.paysim.domain.wallet.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Assignment(
        @JsonProperty("assignee_id") Long assigneeId,
        @JsonProperty("assignee_type") String assigneeType
) {}
