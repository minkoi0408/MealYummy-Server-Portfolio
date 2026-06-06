package mealyummy.mealservice.model.enums;

import lombok.Getter;

@Getter
public enum IngredientUnit {

    G("g", "Gram"),
    KG("kg", "Kilogram"),

    ML("ml", "Mililit"),
    L("l", "Lít"),

    TSP("mcf", "Muỗng cà phê (Teaspoon - khoảng 5ml/5g)"),
    TBSP("muỗng canh", "Muỗng canh (Tablespoon - khoảng 15ml/15g)"),

    PIECE("trái/quả", "Trái / Quả / Củ"),
    CLOVE("tép/nhánh", "Tép / Nhánh (tỏi, sả, hành)"),
    BUNCH("bó", "Bó (rau, hành lá)"),
    BOWL("chén/bát", "Chén / Bát"),
    PINCH("ít/nhúm", "Một ít / Nhúm (muối, tiêu, đường)");

    private final String symbol;
    private final String description;

    IngredientUnit(String symbol, String description) {
        this.symbol = symbol;
        this.description = description;
    }

    @com.fasterxml.jackson.annotation.JsonCreator
    public static IngredientUnit fromString(String text) {
        for (IngredientUnit unit : IngredientUnit.values()) {
            if (unit.name().equalsIgnoreCase(text) || unit.symbol.equalsIgnoreCase(text)) {
                return unit;
            }
        }
        throw new IllegalArgumentException("Không hỗ trợ đơn vị đo lường: " + text);
    }
}