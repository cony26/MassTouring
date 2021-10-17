package com.example.masstouring.mapactivity

class RecordServiceOrderEvent(order : Order) : Event<RecordServiceOrderEvent.Order>(order) {
    public enum class Order{
        START, END
    }
}