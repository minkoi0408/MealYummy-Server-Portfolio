package mealyummy.mealservice.model.enums;

public enum TokenType {
    ACCESS("access:"),
    REFRESH("refresh:");

    private final String prefix;
    TokenType(String prefix) { this.prefix = prefix; }
    public String getPrefix() { return prefix; }
}
