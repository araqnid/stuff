define([], function() {
    var bus = { _registered: {} };
    bus.subscribe = function bus$$subscribe(eventType, handler, owner) {
        var listeners = this._registered[eventType];
        if (!listeners) {
            this._registered[eventType] = listeners = [];
        }
        listeners.push([owner, handler]);
    };
    bus.unsubscribeAll = function bus$$unsubscribeAll(owner) {
        for (var eventType in this._registered) {
            var listeners = this._registered[eventType];
            var remaining = [];
            for (var key in listeners) {
                var listener = listeners[key];
                if (listener[0] !== owner) {
                    remaining.push(listener);
                }
            }
            if (remaining.length > 0) {
                this._registered[eventType] = remaining;
            }
            else {
                delete this._registered[eventType];
            }
        }
    };
    bus.publish = function bus$$publish(eventType, data) {
        var listeners = this._registered[eventType];
        if (!listeners) {
            console.log(eventType + " (dead)", data);
            return;
        }
        console.log(eventType, data);
        for (var key in listeners) {
            var listener = listeners[key];
            listener[1].call(listener[0], data);
        }
    };
    return bus;
});
