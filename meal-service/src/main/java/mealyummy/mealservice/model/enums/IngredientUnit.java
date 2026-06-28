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
        if (text == null || text.trim().isEmpty()) {
            return PIECE;
        }
        String lower = text.trim().toLowerCase();
        for (IngredientUnit unit : IngredientUnit.values()) {
            if (unit.name().equalsIgnoreCase(lower) || unit.symbol.equalsIgnoreCase(lower)) {
                return unit;
            }
        }
        // Fallback for English / Spoonacular units
        if (lower.contains("teaspoon") || lower.equals("tsp")) return TSP;
        if (lower.contains("tablespoon") || lower.equals("tbsp")) return TBSP;
        if (lower.contains("cup") || lower.equals("c")) return BOWL;
        if (lower.contains("clove")) return CLOVE;
        if (lower.contains("bunch") || lower.contains("stalk")) return BUNCH;
        if (lower.contains("pinch") || lower.contains("dash")) return PINCH;
        if (lower.contains("g") || lower.contains("gram") || lower.contains("ounce") || lower.contains("oz")) return G;
        if (lower.contains("kg") || lower.contains("kilogram") || lower.contains("pound") || lower.contains("lb")) return KG;
        if (lower.contains("ml") || lower.contains("milliliter")) return ML;
        if (lower.contains("l") || lower.contains("liter")) return L;
        
        // Default fallback instead of crashing
        return PIECE;
    }
}