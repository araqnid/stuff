import React from "react";
import bus from "bus";

class SignIn extends React.Component {
    render() {
        return <div ref="button" className="my-signin2"></div>;
    }

    componentDidMount() {
        gapi.signin2.render(this.refs.button, {
            scope: 'profile email',
            width: 240,
            height: 50,
            longtitle: true,
            theme: 'light',
            onsuccess: this.whenSignedIn,
            onfailure: this.whenSigninFails
        });
    }

    whenSignedIn(googleUser) {
        bus.publish("GoogleAuth.SignedIn", googleUser);
    }

    whenSigninFails(error) {
        bus.publish("GoogleAuth.SignInFailed", error);
    }

    componentWillUnmount() {
    }

    shouldComponentUpdate() {
        return false;
    }
}

export default SignIn;
