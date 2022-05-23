package dev.axaratox.invest.model;

import ru.tinkoff.piapi.contract.v1.Etf;
import ru.tinkoff.piapi.contract.v1.Share;

public record InstrumentDTO(String ticker,
                            String figi,
                            String classCode,
                            String name,
                            int lot,
                            String currency,
                            InstrumentType type) {

    public static InstrumentDTO fromAPI(final Share share) {
        return new InstrumentDTO(
            share.getTicker(),
            share.getFigi(),
            share.getClassCode(),
            share.getName(),
            share.getLot(),
            share.getCurrency(),
            InstrumentType.SHARE
        );
    }

    public static InstrumentDTO fromAPI(final Etf etf) {
        return new InstrumentDTO(
            etf.getTicker(),
            etf.getFigi(),
            etf.getClassCode(),
            etf.getName(),
            etf.getLot(),
            etf.getCurrency(),
            InstrumentType.ETF
        );
    }

    public enum InstrumentType {
        SHARE,
        ETF
    }
}
