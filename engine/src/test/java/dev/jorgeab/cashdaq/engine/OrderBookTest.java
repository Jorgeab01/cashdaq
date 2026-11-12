package dev.jorgeab.cashdaq.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class OrderBookTest {

    private OrderBook book;

    @BeforeEach
    void setUp() {
        book = new OrderBook();
    }

    // ============ NO CROSSES ============ //

    @Test
    void emptyBookHasNoBestPrices() {
        assertThat(book.bestBid()).isEmpty();
        assertThat(book.bestAsk()).isEmpty();
    }

    @Test
    void limitBuyInEmptyBookRestsAsBestBid() {
        List<Trade> trades = book.submit(limitBuy(10, 7, 100));

        assertThat(trades).isEmpty();
        assertThat(book.bestBid()).hasValue(100);
    }

    @Test
    void limitSellInEmptyBookRestsAsBestAsk() {
        List<Trade> trades = book.submit(limitSell(10, 4, 100));

        assertThat(trades).isEmpty();
        assertThat(book.bestAsk()).hasValue(100);
    }

    @Test
    void nonCrossingOrdersBothRest() {
        book.submit(limitBuy(1, 5, 99));
        List<Trade> trades = book.submit(limitSell(2, 5, 101));

        assertThat(trades).isEmpty();
        assertThat(book.bestBid()).hasValue(99);
        assertThat(book.bestAsk()).hasValue(101);
    }

    @Test
    void bestBidIsHighestBuyPrice() {
        book.submit(limitBuy(1, 5, 99));
        book.submit(limitBuy(2, 5, 102));
        List<Trade> trades = book.submit(limitBuy(3, 5, 101));

        assertThat(trades).isEmpty();
        assertThat(book.bestBid()).hasValue(102);
    }

    @Test
    void bestAskIsLowestSellPrice() {
        book.submit(limitSell(1, 5, 100));
        book.submit(limitSell(2, 5, 95));
        List<Trade> trades = book.submit(limitSell(3, 5, 102));

        assertThat(trades).isEmpty();
        assertThat(book.bestAsk()).hasValue(95);
    }

    // ============ EXECUTIONS (INCOMING BUY) ============ //

    @Test
    void incomingSellFullyMatchesRestingBuy() {
        book.submit(limitBuy(1, 5, 100));
        List<Trade> trades = book.submit(limitSell(2, 5, 100));

        assertThat(trades).containsExactly(new Trade(1, 2, 100, 5));
        assertThat(book.bestBid()).isEmpty();
        assertThat(book.bestAsk()).isEmpty();
    }

    @Test
    void tradeExecutesAtRestingOrderPrice() {
        book.submit(limitSell(2, 5, 100));
        List<Trade> trades = book.submit(limitBuy(1, 5, 105));

        assertThat(trades).containsExactly(new Trade(1, 2, 100, 5));
        assertThat(book.bestBid()).isEmpty();
        assertThat(book.bestAsk()).isEmpty();
    }

    @Test
    void incomingBuyLargerThanRestingSellRestsRemainder() {
        book.submit(limitSell(2, 5, 100));
        List<Trade> trades = book.submit(limitBuy(1, 8, 100));

        assertThat(trades).containsExactly(new Trade(1, 2, 100, 5));
        assertThat(book.bestBid()).hasValue(100);
        assertThat(book.quantityAt(Side.BUY, 100)).isEqualTo(3);
        assertThat(book.bestAsk()).isEmpty();
    }

    @Test
    void restingSellLargerThanIncomingBuyKeepsRemainder() {
        book.submit(limitSell(2, 10, 100));
        List<Trade> trades = book.submit(limitBuy(1, 4, 100));

        assertThat(trades).containsExactly(new Trade(1, 2, 100, 4));
        assertThat(book.quantityAt(Side.SELL, 100)).isEqualTo(6);
        assertThat(book.quantityAt(Side.BUY, 100)).isEqualTo(0);
    }

    @Test
    void incomingBuySweepsMultiplePriceLevels() {
        book.submit(limitSell(1, 5, 101));
        book.submit(limitSell(2, 5, 102));
        List<Trade> trades = book.submit(limitBuy(3, 8, 102));

        assertThat(trades).containsExactly(
                new Trade(3, 1, 101, 5),
                new Trade(3, 2, 102, 3));
        assertThat(book.quantityAt(Side.SELL, 102)).isEqualTo(2);
        assertThat(book.quantityAt(Side.SELL, 101)).isEqualTo(0);
        assertThat(book.quantityAt(Side.BUY, 102)).isEqualTo(0);
        assertThat(book.bestAsk()).hasValue(102);
    }

    @Test
    void ordersAtSamePriceMatchInArrivalOrder() {
        book.submit(limitSell(1, 5, 100));
        book.submit(limitSell(2, 5, 100));
        List<Trade> trades = book.submit(limitBuy(3, 5, 100));

        assertThat(trades).containsExactly(new Trade(3, 1, 100, 5));
        assertThat(book.quantityAt(Side.SELL, 100)).isEqualTo(5);
        assertThat(book.cancel(2)).isTrue();
    }

    // ============ EXECUTIONS (INCOMING SELL) ============ //

    @Test
    void incomingSellExecutesAtRestingBuyPrice() {
        book.submit(limitBuy(1, 5, 105));
        List<Trade> trades = book.submit(limitSell(2, 5, 100));

        assertThat(trades).containsExactly(new Trade(1, 2, 105, 5));
        assertThat(book.bestBid()).isEmpty();
        assertThat(book.bestAsk()).isEmpty();
    }

    @Test
    void incomingSellLargerThanRestingBuyRestsRemainder() {
        book.submit(limitBuy(1, 5, 100));
        List<Trade> trades = book.submit(limitSell(2, 8, 100));

        assertThat(trades).containsExactly(new Trade(1, 2, 100, 5));
        assertThat(book.bestAsk()).hasValue(100);
        assertThat(book.quantityAt(Side.SELL, 100)).isEqualTo(3);
        assertThat(book.bestBid()).isEmpty();
    }

    @Test
    void restingBuyLargerThanIncomingSellKeepsRemainder() {
        book.submit(limitBuy(1, 10, 100));
        List<Trade> trades = book.submit(limitSell(2, 4, 100));

        assertThat(trades).containsExactly(new Trade(1, 2, 100, 4));
        assertThat(book.quantityAt(Side.BUY, 100)).isEqualTo(6);
        assertThat(book.bestAsk()).isEmpty();
    }

    @Test
    void incomingSellSweepsMultiplePriceLevels() {
        book.submit(limitBuy(1, 5, 99));
        book.submit(limitBuy(2, 5, 98));
        List<Trade> trades = book.submit(limitSell(3, 8, 98));

        assertThat(trades).containsExactly(
                new Trade(1, 3, 99, 5),
                new Trade(2, 3, 98, 3));
        assertThat(book.quantityAt(Side.BUY, 99)).isEqualTo(0);
        assertThat(book.quantityAt(Side.BUY, 98)).isEqualTo(2);
        assertThat(book.bestBid()).hasValue(98);
        assertThat(book.bestAsk()).isEmpty();
    }

    @Test
    void restingBuysAtSamePriceMatchInArrivalOrder() {
        book.submit(limitBuy(1, 5, 100));
        book.submit(limitBuy(2, 5, 100));
        List<Trade> trades = book.submit(limitSell(3, 5, 100));

        assertThat(trades).containsExactly(new Trade(1, 3, 100, 5));
        assertThat(book.quantityAt(Side.BUY, 100)).isEqualTo(5);
        assertThat(book.cancel(2)).isTrue();
    }

    // ============ MARKET ORDERS ============ //

    @Test
    void marketBuyFillsAtBestAvailablePrices() {
        book.submit(limitSell(1, 5, 101));
        book.submit(limitSell(2, 5, 102));
        List<Trade> trades = book.submit(marketBuy(3, 8));

        assertThat(trades).containsExactly(
                new Trade(3, 1, 101, 5),
                new Trade(3, 2, 102, 3));
        assertThat(book.quantityAt(Side.SELL, 102)).isEqualTo(2);
        assertThat(book.quantityAt(Side.SELL, 101)).isEqualTo(0);
        assertThat(book.bestAsk()).hasValue(102);
        assertThat(book.bestBid()).isEmpty();
    }

    @Test
    void marketBuyRemainderIsNotRested() {
        book.submit(limitSell(1, 5, 100));
        List<Trade> trades = book.submit(marketBuy(2, 8));

        assertThat(trades).containsExactly(new Trade(2, 1, 100, 5));
        assertThat(book.bestBid()).isEmpty();
        assertThat(book.bestAsk()).isEmpty();
    }

    @Test
    void marketBuyInEmptyBookProducesNoTrades() {
        List<Trade> trades = book.submit(marketBuy(1, 5));

        assertThat(trades).isEmpty();
        assertThat(book.bestBid()).isEmpty();
        assertThat(book.bestAsk()).isEmpty();
    }

    @Test
    void marketSellFillsAtBestAvailablePrices() {
        book.submit(limitBuy(1, 5, 99));
        book.submit(limitBuy(2, 5, 98));
        List<Trade> trades = book.submit(marketSell(3, 8));

        assertThat(trades).containsExactly(
                new Trade(1, 3, 99, 5),
                new Trade(2, 3, 98, 3));
        assertThat(book.quantityAt(Side.BUY, 98)).isEqualTo(2);
        assertThat(book.bestBid()).hasValue(98);
        assertThat(book.bestAsk()).isEmpty();
    }

    // ============ CANCELLATIONS ============ //

    @Test
    void cancelRemovesRestingOrder() {
        book.submit(limitBuy(1, 5, 100));

        assertThat(book.cancel(1)).isTrue();
        assertThat(book.bestBid()).isEmpty();
        assertThat(book.quantityAt(Side.BUY, 100)).isEqualTo(0);
    }

    @Test
    void cancellingBestBidUpdatesBestBid() {
        book.submit(limitBuy(1, 5, 99));
        book.submit(limitBuy(2, 5, 98));

        assertThat(book.cancel(1)).isTrue();
        assertThat(book.bestBid()).hasValue(98);
    }

    @Test
    void cancelUnknownOrderReturnsFalse() {
        assertThat(book.cancel(42)).isFalse();
    }

    @Test
    void cancelFilledOrderReturnsFalse() {
        book.submit(limitSell(1, 5, 100));
        book.submit(limitBuy(2, 5, 100));

        assertThat(book.cancel(1)).isFalse();
    }

    @Test
    void cancelPartiallyFilledOrderRemovesRemainder() {
        book.submit(limitSell(1, 10, 100));
        book.submit(limitBuy(2, 4, 100));

        assertThat(book.cancel(1)).isTrue();
        assertThat(book.quantityAt(Side.SELL, 100)).isEqualTo(0);
        assertThat(book.bestAsk()).isEmpty();
    }

    // ============ VALIDATION ============ //

    @Test
    void orderWithNonPositiveQuantityIsRejected() {
        assertThatThrownBy(() -> limitBuy(1, 0, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limitBuy(1, -1, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void limitOrderWithNonPositivePriceIsRejected() {
        assertThatThrownBy(() -> limitBuy(1, 5, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limitBuy(1, 5, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duplicateOrderIdIsRejected() {
        book.submit(limitBuy(1, 5, 99));

        assertThatThrownBy(() -> book.submit(limitBuy(1, 5, 98)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(book.bestBid()).hasValue(99);
    }

    // ============ HELPERS ============ //

    private Order limitBuy(long id, long quantity, long price) {
        return Order.limit(id, Side.BUY, quantity, price);
    }

    private Order limitSell(long id, long quantity, long price) {
        return Order.limit(id, Side.SELL, quantity, price);
    }

    private Order marketBuy(long id, long quantity) {
        return Order.market(id, Side.BUY, quantity);
    }

    private Order marketSell(long id, long quantity) {
        return Order.market(id, Side.SELL, quantity);
    }
}