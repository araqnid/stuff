define(function () {
    return function hashFragmentSettings() {
        if (!window.location.hash) return { path: "/" };
        var content = window.location.hash.substring(1);
	var parts = content.split('?', 2);
        var path = parts[0];
	if (!path) {
	    path = "/";
	}
	else if (!path.startsWith("/")) {
	    path = "/" + path;
	}
        var result = { path: path };
        var query = parts[1];
        if (query) {
            var vars = query.split('&');
            for (var i = 0; i < vars.length; i++) {
		var pair = vars[i].split('=');
		result[decodeURIComponent(pair[0])] = decodeURIComponent(pair[1]);
            }
	}
        return result;
    };
});
