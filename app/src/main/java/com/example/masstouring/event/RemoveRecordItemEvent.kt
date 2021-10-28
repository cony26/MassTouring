package com.example.masstouring.event

import com.example.masstouring.mapactivity.RecordItem

class RemoveRecordItemEvent(recordItem: RecordItem): Event<RecordItem>(recordItem) {
}