package com.cliniq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class QueueDisplayResponse {

    private LocalDate date;
    private Integer currentToken;
    private Integer totalTokens;
    private List<TokenDisplay> waitingTokens;
    private List<TokenDisplay> completedTokens;

    @Getter
    @Setter
    @AllArgsConstructor
    @Builder
    public static class TokenDisplay {
        private Integer tokenNumber;
        private String status;
    }
}
