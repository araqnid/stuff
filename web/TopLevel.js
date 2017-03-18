import React from "react";
import bus from "bus";

class TopLevel extends React.Component {
    constructor(props) {
        super(props);
        this.state = {googleUser: null, adminUser: null};
    }

    render() {
        if (this.state.versionInfo) {
            if (this.state.versionInfo.version) {
                return <div>
                    App version is <b>{this.state.versionInfo.version}</b>
                </div>;
            }
            else {
                return <div>
                    App is unversioned
                </div>;
            }
        }
        else {
            return <div> </div>;
        }
    }

    signedIn(googleUser) {
        this.setState({ googleUser: googleUser });
    }

    signedOut() {
        this.setState({ googleUser: null, adminUser: null });
    }

    tokenExchanged(adminUser) {
        this.setState({ adminUser: adminUser });
    }

    appInfoVersion(versionInfo) {
        this.setState({ versionInfo: versionInfo });
    }

    componentWillMount() {
        bus.subscribe('GoogleAuth.SignedIn', this.signedIn, this);
        bus.subscribe('GoogleAuth.SignedOut', this.signedOut, this);
        bus.subscribe('GoogleAuth.TokenExchanged', this.tokenExchanged, this);
        bus.subscribe('AppInfo.Version.Received', this.appInfoVersion, this);
    }

    componentWillUnmount() {
        bus.unsubscribeAll(this);
    }
}

export default TopLevel
