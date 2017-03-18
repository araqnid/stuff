import bus from "bus";

class ManageUserSession {
    signedIn(googleUser) {
        jQuery.ajax({
            url: "/_api/sign-in",
            data: { gtoken: googleUser.getAuthResponse().id_token },
            type: "POST",
            success: function(data, status, xhr) {
                bus.publish('GoogleAuth.TokenExchanged', data);
            },
            error: function(xhr, status, e) {
                bus.publish('GoogleAuth.TokenExchangeError', [xhr, status, e]);
            }
        });
    }

    initialise() {
        bus.subscribe('GoogleAuth.SignedIn', this.signedIn, this);
    }

    destroy() {
        bus.unsubscribeAll(this)
    }
}

export default ManageUserSession
