package top.cutexingluo.tools.utils.spring.security.pkg;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.security.oauth2.provider.authentication.TokenExtractor;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import top.cutexingluo.tools.designtools.builder.XTBuilder;
import top.cutexingluo.tools.utils.spring.security.pkg.ext.XTBearerTokenExtractor;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 认证转化工具类
 * <p><b>建议使用 Builder 模式</b>, 得到执行链Builder：</p>
 * <code>new XTAuthenticationUtil.AuthenticationBuilder(request) </code>
 *
 * @author XingTian
 * @version 1.0.0
 * @date 2023/6/28 16:38
 */
public class XTAuthenticationUtil {

    /**
     * 打印验证
     *
     * @param title          标题
     * @param authentication 身份验证
     */
    protected static void printAuthentication(String title, Authentication authentication) {
        System.out.println("");
        System.out.println("============== " + title + " ===============");
        System.out.println(authentication);
        System.out.println("-------------------------------------");
        System.out.println(authentication.getPrincipal());
        System.out.println(authentication.getCredentials());
        System.out.println(authentication.getAuthorities());
        System.out.println("======================================");
    }

    /**
     * 提取token，并添加"Bearer"前缀
     *
     * @param httpServletRequest http servlet请求
     * @return {@link String}
     */
    @Nullable
    public static String tokenAddBearerPrefix(HttpServletRequest httpServletRequest, String headerName) {
        String token = httpServletRequest.getHeader(headerName);
        if (StrUtil.isBlank(token)) {
            return null;
        }
        if (!token.startsWith("Bearer ")) {
            return "Bearer " + token;
        }
        return token;
    }

    /**
     * 提取token
     *
     * @param tokenExtractor     令牌器
     * @param httpServletRequest http servlet请求
     * @return {@link Authentication}
     */
    public static Authentication extract(TokenExtractor tokenExtractor, HttpServletRequest httpServletRequest) {
        return tokenExtractor.extract(httpServletRequest);
    }

    /**
     * 使用XTBearerTokenExtractor提取token，可以不加Bearer
     *
     * @param httpServletRequest http servlet请求
     * @param headerName         标题名称
     * @return {@link Authentication}
     */
    public static Authentication extract(HttpServletRequest httpServletRequest, String headerName) {
        return new XTBearerTokenExtractor().extract(httpServletRequest, headerName);
    }


    //*******************PreAuthenticatedAuthenticationToken*****************

    /**
     * 非验证得到前验证身份验证标记
     * 得到PreAuthenticatedAuthenticationToken
     * <p> setAuthenticated(false) </p>
     *
     * @param authentication 身份验证
     * @return {@link PreAuthenticatedAuthenticationToken}
     */
    public static PreAuthenticatedAuthenticationToken getPreAuthenticatedAuthenticationTokenNonAuthenticated(Authentication authentication) {
        return new PreAuthenticatedAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials());
    }


    /**
     * 得到PreAuthenticatedAuthenticationToken
     * <p> setAuthenticated(true) </p>
     *
     * @param authentication 身份验证
     * @return {@link PreAuthenticatedAuthenticationToken}
     */
    public static PreAuthenticatedAuthenticationToken getPreAuthenticatedAuthenticationToken(Authentication authentication, Collection<? extends GrantedAuthority> anAuthorities) {
        return new PreAuthenticatedAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), anAuthorities);
    }

    /**
     * 得到PreAuthenticatedAuthenticationToken
     * <p> setAuthenticated(true) </p>
     *
     * @param authentication 身份验证
     * @return {@link PreAuthenticatedAuthenticationToken}
     */
    public static PreAuthenticatedAuthenticationToken getPreAuthenticatedAuthenticationToken(Authentication authentication) {
        return new PreAuthenticatedAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), authentication.getAuthorities());
    }

    //**********************

    /**
     * 读身份验证 <br>
     * 得到oauth2身份认证
     *
     * @param tokenStore 令牌存储
     * @param token      令牌
     * @return {@link OAuth2Authentication}
     */
    public static OAuth2Authentication readAuthentication(TokenStore tokenStore, String token) {
        return tokenStore.readAuthentication(token);
    }

    //***********************

    /**
     * 进行身份验证
     *
     * @param authenticationManager 身份验证管理器
     * @param authentication        身份验证
     * @return {@link Authentication}
     */
    public static Authentication authenticate(AuthenticationManager authenticationManager, Authentication authentication) {
        return authenticationManager.authenticate(authentication);
    }

    //************************

    /**
     * 身份验证构建器
     * <p>执行链如下</p>
     * <ul>
     *     <li>1. tokenExtractor 展开器 (非必需set，默认Bear，设置 HttpServletRequest 则执行)</li>
     *     <li>2. preAuthenticatedAuthenticationTokenFilter  (非必需set，默认Bear)</li>
     *     <li>3. tokenConsumer  (非必需set，直接获取 token 字符串)</li>
     *     <li>3. accessTokenConsumer  (非必需set，消费 OAuth2AccessToken 对象)</li>
     *     <li>3. accessTokenAdditionalConverter  (非必需set，读取 OAuth2AccessToken 额外信息 返回新认证 （空则不覆盖原来认证）)</li>
     *     <li>3. authenticationFilter  (非必需set，获得Authentication和 OAuth2AccessToken 对象，返回新认证 （空则不覆盖原来认证）)</li>
     *     <li>4. authenticationManager  (非必需set，对Authentication进行认证)</li>
     *     <li>4. authenticationConsumer  (非必需set，最后对Authentication进行操作)</li>
     * </ul>
     *  <p>使用样例</p>
     * <code>
     *  <p>Authentication authentication = new XTAuthenticationUtil.AuthenticationBuilder(request) </p>
     *    <p> .setTokenExtractor(tokenExtractor)</p>
     *     <p>  .setTokenStore(tokenStore) </p>
     *     <p> .setAccessTokenConsumer ( accessToken -> {  // to do  }) </p>
     *      <p>.setAccessTokenAdditionalConverter((map,auth) ->  {// to do})  </p>
     *       <p> .repairCreate("").build();</p>
     * </code>
     *
     * @author XingTian
     * @date 2023/06/29
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    @Accessors(chain = true)
    public static class AuthenticationBuilder extends XTBuilder<Authentication> {

        public AuthenticationBuilder(Authentication authentication) {
            this.target = authentication;
        }

        private HttpServletRequest request; //请求


        public AuthenticationBuilder(HttpServletRequest request) {
            this.request = request;
        }

        /**
         * 令牌展开器
         */
        private TokenExtractor tokenExtractor; //展开

        /**
         * 前验证身份验证令牌
         */
        private PreAuthenticatedAuthenticationToken preAuthenticatedAuthenticationToken; //预认证
        /**
         * 前验证身份验证标记过滤器
         */
        private Function<Authentication, Authentication> preAuthenticatedAuthenticationTokenFilter;


        /**
         * 令牌消费者
         */
        private Consumer<String> tokenConsumer;
        //--------------- 解析token ---------------

        /**
         * 解析 token 生成 OAuth2AccessToken
         */
        private TokenStore tokenStore; // 解析 token 生成 OAuth2AccessToken

        /**
         * 令牌消费者
         */
        private Consumer<DefaultOAuth2AccessToken> accessTokenConsumer; // token消费者

        /**
         * 令牌额外信息转化器<br>
         * 例如可以适配 DefaultUserAuthenticationConverter::extractAuthentication
         */
        private Function<Map<String, ?>, Authentication> accessTokenAdditionalConverter;


        /**
         * 令牌认证转化器
         */
        private BiFunction<Authentication, DefaultOAuth2AccessToken, Authentication> authenticationFilter; // 令牌认证转化器


        //--------------- 认证中心 ----------------
        /**
         * 身份验证管理器
         */
        private AuthenticationManager authenticationManager; //认证中心
        /**
         * 身份验证消费者
         */
        private Consumer<Authentication> authenticationConsumer; //认证消费者

        private boolean isAuthenticated;
        private boolean isBuild;

        protected void setIsBuild(boolean isBuild) {
            this.isBuild = isBuild;
        }

        protected void setPreAuthenticatedAuthenticationToken(PreAuthenticatedAuthenticationToken preAuthenticatedAuthenticationToken) {
            this.preAuthenticatedAuthenticationToken = preAuthenticatedAuthenticationToken;
        }


        /**
         * 获得令牌
         * <p>需要设置 isAuthenticated = true 或者执行 preAuthenticatedAuthenticationWork()</p>
         * <p>HttpServletRequest 需要执行 preAuthenticatedAuthenticationWork() </p>
         *
         * @return {@link String}
         * @throws AuthenticationServiceException 身份验证服务异常
         */
        public String getToken() throws AuthenticationServiceException {
            if (!isAuthenticated) {
                throw new AuthenticationServiceException("未解析到token, 请先执行create");
            }
            if (isBuild) {
                Object principal = preAuthenticatedAuthenticationToken.getPrincipal();
                if (principal != null) {
                    return principal.toString();
                }
                return null;
            }
            if (target != null && target.getPrincipal() != null) {
                return target.getPrincipal().toString();
            }
            return null;
        }

        protected void initTokenExtractor() {
            if (tokenExtractor == null) {
                tokenExtractor = new BearerTokenExtractor();
            }
        }

        public Authentication extractRequest(String headerName) throws AuthenticationServiceException {
            if (request == null) {
                throw new AuthenticationServiceException("无 HttpServletRequest");
            }
            if (tokenExtractor == null) {
                throw new AuthenticationServiceException("无 TokenExtractor");
            }
            if (headerName == null) { //如果 null, 则默认执行自己设置的tokenExtractor
                return extract(tokenExtractor, request);
            }
            // 如果为空，默认Authorization，然后后续无需加Bearer
            if (StrUtil.isBlank(headerName)) headerName = "Authorization";
            return extract(request, headerName);
        }

        /**
         * 令牌器展开工作
         * <p>TokenExtractor 展开 HttpServletRequest 请求 获取 token 封装 Authentication</p>
         *
         * @param authorizationName 授权名字
         * @return {@link AuthenticationBuilder}
         */
        public AuthenticationBuilder tokenExtractorWork(String authorizationName) throws AuthenticationServiceException {
            initTokenExtractor();
            this.target = extractRequest(authorizationName);
            if (target == null) {
                throw new AuthenticationServiceException("token 解析错误, 可能是格式错误");
            }
            return this;
        }

        /**
         * 前验证身份验证工作
         * <p>preAuthenticatedAuthenticationTokenFilter 进行预处理操作</p>
         *
         * @return {@link AuthenticationBuilder}
         * @throws AuthenticationServiceException 身份验证服务异常
         */
        public AuthenticationBuilder preAuthenticatedAuthenticationWork() throws AuthenticationServiceException {
            if (preAuthenticatedAuthenticationTokenFilter == null) { // isAuthenticated
                preAuthenticatedAuthenticationToken = new PreAuthenticatedAuthenticationToken(target.getPrincipal(), target.getCredentials(), target.getAuthorities());
                target = preAuthenticatedAuthenticationToken;
            } else {
                Authentication applyR = preAuthenticatedAuthenticationTokenFilter.apply(target);
                if (applyR instanceof PreAuthenticatedAuthenticationToken) {
                    preAuthenticatedAuthenticationToken = (PreAuthenticatedAuthenticationToken) applyR;
                }
                target = applyR;
            }
            isAuthenticated = true;
            return this;
        }

        /**
         * 令牌附加信息工作
         * <p>可以从这里读取 token 信息, 生成认证</p>
         * <ul>
         *     <li> tokenConsumer 获取消费 token字符串</li>
         *     <li> tokenStore 读取 token信息，若没有提供则使用JWTUtil解析token</li>
         *     <li> accessTokenConsumer 读取 OAuth2AccessToken信息，并进行操作</li>
         *     <ul>
         *         <li> accessTokenAdditionalConverter 读取 token 额外信息并返回新认证（若为null则不覆盖认证）</li>
         *          <li> authenticationFilter 过滤 原认证authentication和OAuth2AccessToken信息，返回新认证（若为null则不覆盖认证） </li>
         *     </ul>
         * </ul>
         *
         * @return {@link AuthenticationBuilder}
         */
        public AuthenticationBuilder tokenAdditionalInformationWork() {
            Object principal = target.getPrincipal();
            if (principal instanceof String) {
                String token = (String) principal;
                if (tokenConsumer != null) tokenConsumer.accept(token);
                DefaultOAuth2AccessToken accessToken = null;
                if (tokenStore != null) { // tokenStore存在 则使用tokenStore
                    try {
                        accessToken = XTAccessTokenUtil.getAccessToken(token, tokenStore);
                        if (accessToken == null) {
                            throw new AuthenticationServiceException("token 解析失败");
                        }
                    } catch (Exception e) {
//                        e.printStackTrace();
                        throw new AuthenticationServiceException("token 解析错误");
                    }
                } else { // tokenStore不存在 则使用 hutool 的JWTUtil
                    try {
                        accessToken = XTAccessTokenUtil.getAccessToken(token);
                        if (accessToken == null) {
                            throw new AuthenticationServiceException("token 解析失败");
                        }
                    } catch (Exception e) {
//                        e.printStackTrace();
                        throw new AuthenticationServiceException("token 解析错误");
                    }
                }
                if (accessTokenConsumer != null) {
                    accessTokenConsumer.accept(accessToken);
                }
                if (accessTokenAdditionalConverter != null) {
                    Authentication apply = accessTokenAdditionalConverter.apply(accessToken.getAdditionalInformation());
                    if (apply != null) {
                        target = apply;
                    }
                }
                if (authenticationFilter != null) {
                    Authentication apply = authenticationFilter.apply(target, accessToken);
                    if (apply != null) {
                        target = apply;
                    }
                }
            }
            return this;
        }


        /**
         * 认证工作
         * <ul>
         *     <li> 执行 authenticationManager 认证 (没有则不执行)</li>
         *     <li> 执行 authenticationConsumer 消费 (没有则不执行)</li>
         * </ul>
         *
         * @return {@link AuthenticationBuilder}
         */
        public AuthenticationBuilder authenticationWork() {
            if (target != null) {
                if (authenticationManager != null) {
                    target = authenticationManager.authenticate(target);
                }
            }
            if (target != null) {
                if (authenticationConsumer != null) {
                    authenticationConsumer.accept(target);
                }
            }
            isBuild = true;
            return this;
        }

        /**
         * 创建 , 得到 target 为认证后的 Authentication 对象
         * <p> 默认读取请求头的 "Authorization" 并且需要加Bearer</p>
         * <p>等同于repairCreate(null)</p>
         *
         * @return {@link AuthenticationBuilder}
         * @throws AuthenticationServiceException 身份验证服务异常
         */
        public AuthenticationBuilder create() throws AuthenticationServiceException {
            if (request != null) tokenExtractorWork(null);
            preAuthenticatedAuthenticationWork();
            tokenAdditionalInformationWork();
            authenticationWork();
            return this;
        }


        /**
         * 修复创建 , 得到 target 为认证后的 Authentication 对象
         * <p>authorizationName 为 null  使用默认扩展器 读取请求头的 "Authorization" 并且需要加 "Bear" ,  等同于create()</p>
         * <p>authorizationName 为 ""  使用工具扩展器 读取请求头的 "Authorization"  会自动识别添加 "Bear"</p>
         *
         * @param authorizationName 授权名字 (header  名称)
         * @return {@link AuthenticationBuilder}
         * @throws AuthenticationServiceException 身份验证服务异常
         */
        public AuthenticationBuilder repairCreate(String authorizationName) throws AuthenticationServiceException {
            if (request != null) tokenExtractorWork(authorizationName);
            preAuthenticatedAuthenticationWork();
            tokenAdditionalInformationWork();
            authenticationWork();
            return this;
        }
    }

}
