package dev.jorgeab.cashdaq.engine;

import java.util.Objects;

public class Order {

    private final long id;
    private final Side side;
    private final OrderType type;
    private final long price;
    private final long quantity;
    private long remainingQuantity;

    /**
     * An order to buy or sell a quantity of shares.
     * <p>
     * Prices are expressed in cents (minor currency units) as long values,
     * so 101.50 is stored as "10150". The smallest price step is one cent.
     */
    public Order(long id, Side side, OrderType type, long quantity, long price) {

        Objects.requireNonNull(side, "Side must not be null");
        Objects.requireNonNull(type, "Type must not be null");

        if (quantity <= 0)
            throw new IllegalArgumentException("Invalid quantity");
        if (type == OrderType.LIMIT && price <= 0)
            throw new IllegalArgumentException("Limit order price must be positive");
        if (id < 0)
            throw new IllegalArgumentException("ID must be positive");

        this.id = id;
        this.side = side;
        this.type = type;
        this.quantity = quantity;
        remainingQuantity = quantity;
        this.price = price;
    }

    public static Order limit(long id, Side side, long quantity, long price) {
        return new Order(id, side, OrderType.LIMIT, quantity, price);
    }

    public static Order market(long id, Side side, long quantity) {
        return new Order(id, side, OrderType.MARKET, quantity, 0);
    }

    void fill(long amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Cannot fill " + amount + ", must be greater than 0");
        if (remainingQuantity - amount < 0)
            throw new IllegalArgumentException("Cannot fill " + amount + ", only " + remainingQuantity + " remaining");
        remainingQuantity -= amount;
    }

    public boolean isFilled() {
        return remainingQuantity == 0;
    }

    public long getId() {
        return id;
    }

    public Side getSide() {
        return side;
    }

    public OrderType getType() {
        return type;
    }

    public long getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }

    public long getRemainingQuantity() {
        return remainingQuantity;
    }

}
