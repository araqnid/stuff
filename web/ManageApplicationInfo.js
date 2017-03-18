import bus from "bus";

class ManageApplicationInfo {

    constructor(refreshInterval) {
        this.ongoing = null;
        this.running = true;
        this.refreshInterval = refreshInterval;
    }

    poll() {
        this.ongoing = jQuery.ajax({
            url: "/_api/info/version",
            type: "GET",
            headers: { "Accept": "application/json" },
            success: (data, status, xhr) => {
                bus.publish('AppInfo.Version.Received', data);
            },
            error: (xhr, status, e) => {
                bus.publish('AppInfo.Version.AjaxError', [xhr, status, e]);
            },
            complete: () => {
                this.ongoing = null;
                if (this.running) {
                    this._reschedule(this.refreshInterval);
                }
            }
        });
    }

    initialise() {
        this._reschedule(0);
    }

    refresh() {
        if (!this.ongoing && this.running)
            this._reschedule(0)
    }

    destroy() {
        bus.unsubscribeAll(this);
        if (this.ongoing) {
            this.ongoing.abort();
            this.running = false;
        }
    }

    _reschedule(delay) {
        setTimeout(() => this.poll(), delay);
    }
}

export default ManageApplicationInfo
