class AjaxRegister {
    constructor(owner, user) {
        this.owner = owner;
        this.user = user;
        this.ongoing = [];
    }

    begin(options) {
        if (!options.complete) {
            options.complete = (xhr, status) => this._ajaxComplete(xhr);
        }
        else if (_.isArray(options.complete)) {
            options.complete.push((xhr, status) => this._ajaxComplete(xhr));
        }
        else {
            options.complete = [options.complete, (xhr, status) => this._ajaxComplete(xhr)];
        }
        if (!options.headers) {
            options.headers = {};
        }
        options.headers["Authorization"] = "Bearer " + this.user.local.token;
        var req = jQuery.ajax(options);
        this.ongoing.push(req);
    }

    abort() {
        _.forEach(_.clone(this.ongoing), req => req.abort());
        this.ongoing = [];
    }

    _ajaxComplete(xhr) {
        _.remove(this.ongoing, xhr);
    }
}

export default AjaxRegister;
