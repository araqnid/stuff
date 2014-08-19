require.config({
	map: {
		'*': { jquery: "jquery-noconflict" },
		'jquery-noconflict': { jquery: "jquery" }
	},
	paths: {
		jquery: "http://code.jquery.com/jquery-1.11.1.min",
		lodash: "//cdnjs.cloudflare.com/ajax/libs/lodash.js/2.4.1/lodash.compat.min"
	}
});
define("jquery-noconflict", ["jquery"], function(jQuery) {
	jQuery.noConflict(true);
	return jQuery;
});
require(["jquery", "lodash"], function($, _) {
	var completed = { state: false, version: false };
	function markCompleted(key) {
		delete completed[key];
		if (_.keys(completed).length == 0)
			$("#info").addClass("completed");
	}
	$.ajax({
		url: "_api/info/state",
		success: function(data) {
			$("#info .info-state").html("App state is <b>" + data + "</b>").addClass("loaded");
		},
		error: function() {
			$("#info .info-state").html("Failed to get app state").addClass("error");
		},
		complete: function() {
			markCompleted('state');
		}
	});
	$.ajax({
		url: "_api/info/version",
		success: function(data) {
			$("#info .info-version").html("App version is <b>" + data.version + "</b>").addClass("loaded");
		},
		error: function() {
			$("#info .info-version").html("Failed to get app version").addClass("error");
		},
		complete: function() {
			markCompleted('version');
		}
	});
});
