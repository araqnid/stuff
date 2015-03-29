package org.araqnid.stuff.mvc;

import org.araqnid.stuff.config.ServerIdentity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.inject.Inject;

@Controller
public class HelloWorldController {
	@Inject
	@ServerIdentity
	private String serverIdentity;

	@RequestMapping("/helloworld")
	public String helloWorld(Model model) {
		model.addAttribute("message", "Hello World!");
		model.addAttribute("server", serverIdentity);
		return "helloWorld";
	}
}
