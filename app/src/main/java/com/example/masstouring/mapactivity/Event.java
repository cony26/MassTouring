package com.example.masstouring.mapactivity;

public abstract class Event<T> {
    private boolean oHasHandled = false;
    private T oContent;

    public Event(T aContent){
        oContent = aContent;
    }

    public T getContentIfNotHandled(){
        if(oHasHandled){
            return null;
        }else{
            oHasHandled = true;
            return oContent;
        }
    }

    public T peekContent(){
        return oContent;
    }
}