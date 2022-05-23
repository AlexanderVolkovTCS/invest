package dev.axaratox.invest.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import dev.axaratox.invest.model.InstrumentDTO;
import dev.axaratox.invest.service.HistoricService;
import dev.axaratox.invest.service.InvestInformationService;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Route
public class MainView extends VerticalLayout {

    private final InvestInformationService investInformationService;
    private final HistoricService historicService;
    private final TextField searchField;
    private final Button searchButton;
    private final Label totalPriceLabel;
    private final IntegerField yearsField;
    private final Button calculateButton;
    private final Label profitabilityLabel;

    private final Set<InstrumentDTO> instrumentCollection;
    private final Map<InstrumentDTO, IntegerField> instrumentLots;
    private final Grid<InstrumentDTO> instrumentsGrid;

    public MainView(final InvestInformationService investInformationService,
                    final HistoricService historicService) {
        this.investInformationService = investInformationService;
        this.historicService = historicService;
        searchField = createSearchField();
        searchButton = createSearchButton();
        instrumentCollection = new LinkedHashSet<>();
        instrumentLots = new HashMap<>();
        instrumentsGrid = createInstrumentsGrid();
        final var searchToolbar = getToolbar();
        totalPriceLabel = createTotalPriceLabel();
        yearsField = createYearsField();
        profitabilityLabel = createProfitabilityLabel();
        calculateButton = createCalculateButton();
        final var historicProfitabilityToolbar = getHistoricProfitabilityToolbar();
        add(searchToolbar, instrumentsGrid, totalPriceLabel, historicProfitabilityToolbar, profitabilityLabel);
    }

    private Div getToolbar() {
        return new Div(searchField, searchButton);
    }

    private Div getHistoricProfitabilityToolbar() {
        return new Div(yearsField, calculateButton);
    }

    private TextField createSearchField() {
        final var field = new TextField();
        field.setPlaceholder("Enter ticker/name/FIGI");
        return field;
    }

    private Grid<InstrumentDTO> createInstrumentsGrid() {
        final var grid = new Grid<InstrumentDTO>();
        grid.addColumn(InstrumentDTO::ticker).setHeader("Ticker");
        grid.addColumn(InstrumentDTO::name).setHeader("Name");
        grid.addColumn(InstrumentDTO::figi).setHeader("FIGI");
        grid.addColumn(InstrumentDTO::type).setHeader("Type");
        grid.addColumn(this::getTextPrice).setHeader("Price");
        grid.addColumn(createLotFieldRenderer()).setHeader("Lots");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        grid.setItems(instrumentCollection);
        return grid;
    }

    private String getTextPrice(final InstrumentDTO instrument) {
        final var lastPrice = investInformationService.getLastPrice(instrument.figi());
        return switch (instrument.currency()) {
            case "usd" -> "$" + lastPrice;
            case "rub" -> lastPrice + " ₽";
            case "eur" -> "€" + lastPrice;
            default -> lastPrice + " " + instrument.currency();
        };
    }

    private Button createSearchButton() {
        final var button = new Button("Search");
        button.addClickListener(event -> updateGridOrSendNotification());
        button.setDisableOnClick(true);
        return button;
    }

    private void updateGridOrSendNotification() {
        final var searchStr = searchField.getValue();
        investInformationService.getInstrumentInfo(searchStr)
            .ifPresentOrElse(this::updateGrid, () -> sendNotFoundNotification(searchStr));
        searchButton.setEnabled(true);
    }

    private void updateGrid(final InstrumentDTO instrument) {
        instrumentCollection.add(instrument);
        instrumentsGrid.getDataProvider().refreshAll();
    }

    private void sendNotFoundNotification(final String searchStr) {
        Notification.show(String.format("Instrument %s was not found", searchStr));
    }

    private ComponentRenderer<IntegerField, InstrumentDTO> createLotFieldRenderer() {
        return new ComponentRenderer<>(this::createLotField);
    }

    private IntegerField createLotField(final InstrumentDTO instrument) {
        final var field = new IntegerField();
        field.setValue(0);
        instrumentLots.put(instrument, field);
        field.addValueChangeListener(event -> updateTotalPrice());
        return field;
    }

    private Label createTotalPriceLabel() {
        final var label = new Label();
        label.setText("Total Price: 0.0 ₽");
        return label;
    }

    private IntegerField createYearsField() {
        final var field = new IntegerField();
        field.setPlaceholder("Enter years");
        field.setMax(3);
        return field;
    }

    private Label createProfitabilityLabel() {
        final var label = new Label();
        label.setText("Profitability: not calculated");
        return label;
    }

    private Button createCalculateButton() {
        final var button = new Button("Calculate");
        button.addClickListener(event -> updateProfitability());
        button.setDisableOnClick(true);
        return button;
    }

    private void updateProfitability() {
        final var years = yearsField.getValue();
        final var result = historicService.calculateHistoricProfitability(convertLotMap(instrumentLots), years);
        profitabilityLabel.setText(String.format(
            """
            Portfolio Price Percentage Gain: %,.2f%%
            Total Replenishments: %,.2f ₽
            Total Collected: %,.2f ₽
            """,
            result.portfolioPricePercentageGain(),
            result.totalReplenishments(),
            result.totalCollected()
        ));
        searchButton.setEnabled(true);
    }

    private void updateTotalPrice() {
        final var sum = instrumentLots.entrySet().stream()
            .mapToDouble(this::getPrice)
            .sum();
        totalPriceLabel.setText(String.format("Total Price: %,.2f ₽", sum));
    }

    private double getPrice(final Map.Entry<InstrumentDTO, IntegerField> entry) {
        final var instrument = entry.getKey();
        final var lot = entry.getValue().getValue();
        final var price = investInformationService.getLastPrice(instrument.figi());
        return instrument.currency().equals("rub")
            ? price * lot
            : investInformationService.convertToRub(price, instrument.currency()) * lot;
    }

    private Map<InstrumentDTO, Integer> convertLotMap(final Map<InstrumentDTO, IntegerField> map) {
        return map.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getValue()));
    }
}
