package dev.jorgeab.cashdaq.engine;

public record Trade(long buyOrderId, long sellOrderId, long price, long quantity) {

}