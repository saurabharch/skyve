package org.skyve.impl.web;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.skyve.CORE;
import org.skyve.domain.messages.DomainException;
import org.skyve.impl.metadata.repository.router.Router;
import org.skyve.metadata.router.UxUi;
import org.skyve.metadata.router.UxUiSelector;
import org.skyve.web.UserAgentType;

import com.blueconic.browscap.BrowsCapField;
import com.blueconic.browscap.Capabilities;
import com.blueconic.browscap.UserAgentParser;
import com.blueconic.browscap.UserAgentService;

public class UserAgent {
	/**
	 * Prevent instantiation
	 */
	private UserAgent() {
		// nothing to see here
	}
	
	private static UserAgentParser parser = null;
	static {
		try {
//			parser = new UserAgentService().loadParser(Arrays.asList(BrowsCapField.values()));
			parser = new UserAgentService().loadParser(Collections.singleton(BrowsCapField.DEVICE_TYPE));
		}
		catch (Exception e) {
			throw new DomainException("Cannot initialise Browscap.", e);
		}
	}

	private static Map<String, UserAgentType> typeCache = new TreeMap<>();

	public static UserAgentType getType(HttpServletRequest request) {
		boolean touchEnabled = false;

		// See if UserAgentType is already set as a request attribute (from device.jsp)
		UserAgentType result = (UserAgentType) request.getAttribute(AbstractWebContext.USER_AGENT_TYPE_KEY);
		if (result == null) {
			// See if touch is enabled
			Cookie[] cookies = request.getCookies();
			if (cookies != null) {
				for (int i = 0, l = cookies.length; i < l; i++) {
					Cookie cookie = cookies[i];
					String cookieName = cookie.getName();
					if ("touch".equals(cookieName)) {
						if ("1".equals(cookie.getValue())) {
							touchEnabled = true;
						}
					}
				}
			}
			
			// Try the User-Agent header
			String agentString = request.getHeader("User-Agent");
			if (agentString == null) {
				agentString = "";
			}
	
			result = typeCache.get(agentString);
	
			if (result == null) {
				result = UserAgentType.other;
				
				Capabilities capabilities = parser.parse(agentString);
				if (capabilities != null) {
					String deviceType = capabilities.getDeviceType();
					if (deviceType != null) {
						if ("Desktop".equals(deviceType)) {
							result = UserAgentType.desktop;
						}
						else if ("Tablet".equals(deviceType)) {
							result = UserAgentType.tablet;
						}
						else if (deviceType.startsWith("Mobile")) {
							result = UserAgentType.phone;
						}
					}
				}
	
				typeCache.put(agentString, result);
			}
			
			if ((result == UserAgentType.desktop) && touchEnabled) {
				result = UserAgentType.tablet;
			}
			
			request.setAttribute(AbstractWebContext.USER_AGENT_TYPE_KEY, result);
		}
		
		return result;
	}
	
	public static UxUi getUxUi(HttpServletRequest request) throws Exception {
		UxUi result = (UxUi) request.getAttribute(AbstractWebContext.UXUI);
		if (result == null) {
			Router router = CORE.getRepository().getRouter();
			UxUiSelector uxuiSelector = (UxUiSelector) router.getUxuiSelector();
			UserAgentType type = UserAgent.getType(request);
			result = UserAgent.isEmulated(request) ? uxuiSelector.emulate(type, request) : uxuiSelector.select(type, request);
			request.setAttribute(AbstractWebContext.UXUI, result);
		}

		return result;
	}

	public static boolean isEmulated(HttpServletRequest request) {
		return (request.getAttribute(AbstractWebContext.EMULATED_USER_AGENT_TYPE_KEY) != null);
	}
}
