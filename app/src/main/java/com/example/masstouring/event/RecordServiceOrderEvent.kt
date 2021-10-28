package com.example.masstouring.event

class RecordServiceOrderEvent(order : Order) : Event<RecordServiceOrderEvent.Order>(order) {
    public enum class Order{
        START, END, BOUND
    }
}