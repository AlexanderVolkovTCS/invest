package dev.axaratox.invest.service;

import dev.axaratox.invest.config.CacheConfiguration;
import dev.axaratox.invest.model.InstrumentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.Currency;
import ru.tinkoff.piapi.contract.v1.InstrumentStatus;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvestInformationService {
    private final InvestApi investApi;

    @Cacheable(CacheConfiguration.INSTRUMENTS)
    public Optional<InstrumentDTO> getInstrumentInfo(final String searchStr) {
        final var instrumentsService = investApi.getInstrumentsService();
        final var sharesStream = instrumentsService
            .getShares(InstrumentStatus.INSTRUMENT_STATUS_BASE)
            .thenApply(list -> list.stream().map(InstrumentDTO::fromAPI));
        final var etfsStream = instrumentsService
            .getEtfs(InstrumentStatus.INSTRUMENT_STATUS_BASE)
            .thenApply(list -> list.stream().map(InstrumentDTO::fromAPI));
        final var combinedStream = sharesStream
            .thenCombine(etfsStream, Stream::concat);
        return combinedStream
            .join()
            .filter(filterByTicker(searchStr).or(filterByFigi(searchStr)).or(filterByName(searchStr)))
            .findAny();
    }

    @Cacheable(CacheConfiguration.LAST_PRICES)
    public double getLastPrice(final String figi) {
        final var response = investApi.getMarketDataService()
            .getLastPricesSync(List.of(figi))
            .get(0)
            .getPrice();
        return Double.parseDouble(response.getUnits() + "." + response.getNano());
    }

    public double convertToRub(final double price, final String currency) {
        return price * getCurrency(currency);
    }

    public double getCurrency(final String currencyName) {
        return getLastPrice(getCurrencyFigi(currencyName));
    }

    @Cacheable(CacheConfiguration.CURRENCY_FIGI)
    public String getCurrencyFigi(final String currencyName) {
        return investApi.getInstrumentsService()
            .getCurrenciesSync(InstrumentStatus.INSTRUMENT_STATUS_BASE)
            .stream()
            .filter(currency -> StringUtils.containsIgnoreCase(currency.getTicker(), currencyName))
            .map(Currency::getFigi)
            .findAny()
            .orElseThrow();
    }

    private Predicate<InstrumentDTO> filterByTicker(final String searchStr) {
        return instrument -> instrument.ticker().equals(searchStr);
    }

    private Predicate<InstrumentDTO> filterByName(final String searchStr) {
        return instrument -> instrument.name().contains(searchStr);
    }

    private Predicate<InstrumentDTO> filterByFigi(final String searchStr) {
        return instrument -> instrument.figi().equals(searchStr);
    }
}
