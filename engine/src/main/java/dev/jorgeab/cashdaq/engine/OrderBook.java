package dev.jorgeab.cashdaq.engine;

import java.util.*;

/**
 * Order book for a single instrument, with price-time priority matching.
 * <p>
 * Not thread-safe: all methods must be called from a single thread.
 */
public class OrderBook {

    private final TreeMap<Long, ArrayDeque<Order>> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Long, ArrayDeque<Order>> asks = new TreeMap<>();
    private final HashMap<Long, Order> ordersById = new HashMap<>();

    /**
     * Processes an incoming order: matches it against the opposite side of the book
     * and, if it is a limit order with remaining quantity, adds it to the book.
     *
     * @param order the incoming order
     * @return the trades generated, in execution order; empty if none
     * @throws IllegalArgumentException if an order with the same id is already in the book
     */
    public List<Trade> submit(Order order) {

        if (ordersById.containsKey(order.getId()))
            throw new IllegalArgumentException("Order with ID " + order.getId() + " is already in the book");

        List<Trade> trades = match(order);

        if (order.getType() == OrderType.LIMIT && !order.isFilled()) {
            rest(order);
        }

        return trades;
    }

    private List<Trade> match(Order order) {
        List<Trade> trades = new ArrayList<>();
        TreeMap<Long, ArrayDeque<Order>> oppositeBook = (order.getSide() == Side.SELL) ? bids : asks;

        while (!order.isFilled() && !oppositeBook.isEmpty() && crosses(order, oppositeBook.firstKey())) {

            long bestOpposite = oppositeBook.firstKey();
            ArrayDeque<Order> oppositeOrders = oppositeBook.get(bestOpposite);

            Order maker = oppositeOrders.getFirst();

            trades.add(executeFill(order, maker, bestOpposite));

            if (maker.isFilled()) {
                oppositeOrders.pollFirst();
                ordersById.remove(maker.getId());
            }

            if (oppositeOrders.isEmpty()) oppositeBook.remove(bestOpposite);
        }
        return trades;
    }

    private Trade executeFill(Order taker, Order maker, long price) {

        long fillQuantity = Math.min(taker.getRemainingQuantity(), maker.getRemainingQuantity());

        taker.fill(fillQuantity);
        maker.fill(fillQuantity);

        long buyOrderId = (taker.getSide() == Side.BUY) ? taker.getId() : maker.getId();
        long sellOrderId = (taker.getSide() == Side.SELL) ? taker.getId() : maker.getId();

        return new Trade(buyOrderId, sellOrderId, price, fillQuantity);
    }

    private void rest(Order order) {
        TreeMap<Long, ArrayDeque<Order>> ownBook = (order.getSide() == Side.BUY) ? bids : asks;
        ownBook.computeIfAbsent(order.getPrice(), price -> new ArrayDeque<>()).addLast(order);
        ordersById.put(order.getId(), order);
    }

    /**
     * Checks whether an order can trade at the given opposite price.
     *
     * @param order the incoming order
     * @param bestOppositePrice the best price on the opposite side of the book
     * @return true if the order accepts that price
     */
    private boolean crosses(Order order, long bestOppositePrice) {
        return  order.getType() == OrderType.MARKET ||
                (order.getSide() == Side.BUY && order.getPrice() >= bestOppositePrice) ||
                (order.getSide() == Side.SELL && order.getPrice() <= bestOppositePrice);
    }


    /**
     * Cancels an order that is resting in the book.
     *
     * @param orderId the id of the order to cancel
     * @return true if the order was found and removed; false otherwise
     */
    public boolean cancel(long orderId) {

        Order order = ordersById.remove(orderId);
        if (order == null) return false;

        TreeMap<Long, ArrayDeque<Order>> ownBook = (order.getSide() == Side.BUY) ? bids : asks;
        ArrayDeque<Order> orders = ownBook.get(order.getPrice());

        orders.remove(order);
        if (orders.isEmpty()) ownBook.remove(order.getPrice());

        return true;
    }

    /**
     * Returns the current best bid.
     * @return the highest buy price, or empty if there are no bids
     */
    public OptionalLong bestBid() {
        return bids.isEmpty() ? OptionalLong.empty() : OptionalLong.of(bids.firstKey());
    }

    /**
     * Returns the current best ask.
     * @return the lowest sell price, or empty if there are no asks
     */
    public OptionalLong bestAsk() {
        return asks.isEmpty() ? OptionalLong.empty() : OptionalLong.of(asks.firstKey());
    }

    /**
     * Returns the total remaining quantity resting at the given price on the given side,
     * or 0 if there are no orders at that price.
     * @param side Side to check
     * @param price Price to check
     * @return Remaining quantity
     */
     long quantityAt(Side side, long price) {

        TreeMap<Long, ArrayDeque<Order>> currentBook = (side == Side.SELL) ? asks : bids;

        ArrayDeque<Order> ordersAtPrice = currentBook.get(price);
        if (ordersAtPrice == null) return 0;

        long quantity = 0;
        for (Order currentOrder : ordersAtPrice) {
            quantity += currentOrder.getRemainingQuantity();
        }
        return quantity;

    }
}