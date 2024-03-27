package top.cutexingluo.tools.utils.spring.security.pkg.ext;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * bearer标记器
 *
 * @author XingTian
 * @version 1.0.0
 * @date 2023/6/30 9:49
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class XTBearerTokenExtractor extends BearerTokenExtractor {

    private String headerName = "Authorization";

    /**
     * 是否解析Cookies
     * <p>false => headers , true => cookies</p>
     *
     * @since 1.0.4
     */
    private boolean useCookies = false;


    public XTBearerTokenExtractor() {
        this.useCookies = false;
    }

    public XTBearerTokenExtractor(boolean useCookies) {
        this.useCookies = useCookies;
    }


    /**
     * 提取为Authentication
     *
     * @param request    请求
     * @param headerName 请求头名称
     * @return {@link Authentication}
     */
    public Authentication extract(HttpServletRequest request, String headerName) {
        this.headerName = headerName;
        return super.extract(request);
    }

    @Override
    protected String extractHeaderToken(HttpServletRequest request) {
        if (useCookies) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals(headerName)) {
                        return getToken(request, cookie.getValue());
                    }
                }
            }
        } else {
            Enumeration<String> headers = request.getHeaders(headerName);
            while (headers.hasMoreElements()) { // typically there is only one (most servers enforce that)
                String value = headers.nextElement();
                return getToken(request, value);
            }
        }
        return null;
    }


    protected String getToken(HttpServletRequest request, String value) {
        if ((value.toLowerCase().startsWith(OAuth2AccessToken.BEARER_TYPE.toLowerCase()))) {
            String authHeaderValue = value.substring(OAuth2AccessToken.BEARER_TYPE.length()).trim();
            // Add this here for the auth details later. Would be better to change the signature of this method.
            request.setAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_TYPE,
                    value.substring(0, OAuth2AccessToken.BEARER_TYPE.length()).trim());
            int commaIndex = authHeaderValue.indexOf(',');
            if (commaIndex > 0) {
                authHeaderValue = authHeaderValue.substring(0, commaIndex);
            }
            return authHeaderValue;
        } else {
            String authHeaderValue = value.trim();
            int commaIndex = authHeaderValue.indexOf(',');
            if (commaIndex > 0) {
                authHeaderValue = authHeaderValue.substring(0, commaIndex);
            }
            return authHeaderValue;
        }
    }
}
