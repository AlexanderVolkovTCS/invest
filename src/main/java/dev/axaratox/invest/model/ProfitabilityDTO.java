package dev.axaratox.invest.model;

public record ProfitabilityDTO(double portfolioPricePercentageGain,
                               double totalReplenishments,
                               double totalCollected) {
}
