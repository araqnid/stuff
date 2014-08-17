require.config({
	map: {
		'*': { jquery: "jquery-noconflict" },
		'jquery-noconflict': { jquery: "jquery" },
	},
	paths: {
		jquery: "http://code.jquery.com/jquery-2.1.1.min",
	},
});
define("jquery-noconflict", ["jquery"], function(jQuery) {
	jQuery.noConflict(true);
	return jQuery;
});
require(["jquery"], function($) {
	$.ajax({
		url: "_api/info/state",
		success: function(data) {
			$("#info .info-state").html("App state is <b>" + data + "</b>").addClass("loaded");
		},
		error: function() {
			$("#info .info-state").html("Failed to get app state").addClass("error");
		},
	});
	$.ajax({
		url: "_api/info/version",
		success: function(data) {
			$("#info .info-version").html("App version is <b>" + data.version + "</b>").addClass("loaded");
		},
		error: function() {
			$("#info .info-version").html("Failed to get app version").addClass("error");
		},
	});
});
