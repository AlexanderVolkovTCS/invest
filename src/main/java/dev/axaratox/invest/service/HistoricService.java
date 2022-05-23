package dev.axaratox.invest.service;

import dev.axaratox.invest.model.InstrumentDTO;
import dev.axaratox.invest.model.ProfitabilityDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.core.InvestApi;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;
import java.util.stream.IntStream;

import static java.time.temporal.ChronoUnit.DAYS;
import static ru.tinkoff.piapi.contract.v1.CandleInterval.CANDLE_INTERVAL_DAY;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricService {
    private final InvestInformationService investInformationService;
    private final InvestApi investApi;

    public ProfitabilityDTO calculateHistoricProfitability(final Map<InstrumentDTO, Integer> investmentPortfolio,
                                                           final int years) {
        final var datesOfBuy = IntStream.range(1, years * 12)
            .boxed()
            .sorted(Collections.reverseOrder())
            .map(this::getDateMinusMonthsFromDecember)
            .toList();
        final var prices = datesOfBuy
            .stream()
            .map(date -> getInvestmentPortfolioHistoricPrice(investmentPortfolio, date))
            .toList();
        final var boughtPackages = prices.size();
        final var initialPackagePrice = prices.get(0);
        final var finalPackagePrice = prices.get(prices.size() - 1);
        final var portfolioPricePercentageGain = ((finalPackagePrice - initialPackagePrice) / Math.abs(initialPackagePrice)) * 100;
        final var totalReplenishments = prices
            .stream()
            .mapToDouble(x -> x)
            .sum();
        final var totalCollected = finalPackagePrice * boughtPackages;
        return new ProfitabilityDTO(portfolioPricePercentageGain, totalReplenishments, totalCollected);
    }

    private double getInvestmentPortfolioHistoricPrice(final Map<InstrumentDTO, Integer> investmentPortfolio,
                                                       final Instant date) {
        return investmentPortfolio
            .entrySet()
            .stream()
            .map(entry -> getHistoricRublePrice(entry.getKey(), date) * entry.getValue())
            .mapToDouble(x -> x)
            .sum();
    }

    private double getHistoricRublePrice(final InstrumentDTO instrument, final Instant date) {
        final var priceQuotation = getHistoricPriceQuotation(instrument.figi(), date);
        final var price = Double
            .parseDouble(priceQuotation.getUnits() + "." + priceQuotation.getNano());
        return instrument.currency().equals("rub")
            ? price
            : convertToRuble(price, instrument.currency(), date);
    }

    private Quotation getHistoricPriceQuotation(final String figi, final Instant date) {
        final var priceQuotationList = investApi.getMarketDataService()
            .getCandlesSync(figi, date, date.plus(1, DAYS), CANDLE_INTERVAL_DAY);
        return !priceQuotationList.isEmpty()
            ? priceQuotationList.get(0).getOpen()
            : getHistoricPriceQuotation(figi, date.plus(1, DAYS));
    }

    private double convertToRuble(final double price, final String currencyName, final Instant date) {
        final var currencyFigi = investInformationService.getCurrencyFigi(currencyName);
        final var currencyPriceQuotation = getHistoricPriceQuotation(currencyFigi, date);
        final var currencyPrice = Double
            .parseDouble(currencyPriceQuotation.getUnits() + "." + currencyPriceQuotation.getNano());
        return price * currencyPrice;
    }

    private Instant getDateMinusMonthsFromDecember(final int months) {
        return Instant.now()
            .atZone(ZoneOffset.UTC)
            .withHour(12)
            .withMinute(0)
            .withSecond(0)
            .withDayOfMonth(1)
            .withYear(2021)
            .withMonth(12)
            .minusMonths(months)
            .toInstant();
    }
}
