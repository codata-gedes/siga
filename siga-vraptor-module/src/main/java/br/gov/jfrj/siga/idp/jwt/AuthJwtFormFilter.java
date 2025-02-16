package br.gov.jfrj.siga.idp.jwt;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.unmodifiableSet;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.USER_AGENT;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import com.auth0.jwt.JWTExpiredException;
import com.auth0.jwt.JWTVerifyException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.caelum.vraptor.controller.HttpMethod;
import br.gov.jfrj.siga.base.Prop;
import br.gov.jfrj.siga.base.SigaMessages;
import br.gov.jfrj.siga.model.ContextoPersistencia;

public class AuthJwtFormFilter implements Filter {

	private static final Logger log = Logger.getLogger(AuthJwtFormFilter.class);

	private static ObjectMapper JSON_MAPPER = new ObjectMapper();
	private static final String X_REQUESTED_WITH = "X-Requested-With";
	private static final String SIGA_JWT_AUTH_COOKIE_NAME = "siga-jwt-auth";
	private static final Set<String> USER_AGENT_BROWSERS = unmodifiableSet(newHashSet(
			"Chrome",
			"Chromium",
			"Firefox",
			"Edg",
			"Edge",
			"Safari",
			"OPR",
			"Opera",
			"MSIE",
			"Trident",
			"Seamonkey"
	));

	private FilterConfig config;

	@Override
	public void init(FilterConfig config) throws ServletException {
		this.config = config;
		log.infov("Initializing Filter {0}", this.config.getFilterName());
	}

	@Override
	public void destroy() {
		log.infov("Destroying filter {0}", this.config.getFilterName());
	}

	public static Map<String, Object> validarToken(String token)
			throws IllegalArgumentException, SigaJwtInvalidException, SigaJwtProviderException, InvalidKeyException,
			NoSuchAlgorithmException, IllegalStateException, SignatureException, IOException, JWTVerifyException {
		if (token == null) {
			throw new SigaJwtInvalidException("Token inválido");
		}
		SigaJwtProvider provider = getProvider();
		return provider.validarToken(token);
	}

	public static String renovarToken(String token)
			throws IllegalArgumentException, SigaJwtProviderException, InvalidKeyException, NoSuchAlgorithmException,
			IllegalStateException, SignatureException, IOException, JWTVerifyException {
		if (token == null) {
			throw new RuntimeException("Token inválido");
		}
		SigaJwtProvider provider = getProvider();
		return provider.renovarToken(token, null);
	}

	public static SigaJwtProvider getProvider() throws SigaJwtProviderException {
		String password = Prop.get("/siga.jwt.secret");
		int ttl = Prop.getInt("/siga.jwt.token.ttl");
		
		SigaJwtOptions options = new SigaJwtOptionsBuilder().setPassword(password).setModulo(null).setTTL(ttl).build();
		SigaJwtProvider provider = SigaJwtProvider.getInstance(options);
		return provider;
	}

	public static String extrairAuthorization(HttpServletRequest request) {
		String auth = request.getHeader(AUTHORIZATION);
		if (auth != null) {
			return auth.replaceAll(".* ", "").trim();
		}
		Cookie[] cookies = request.getCookies();
		String token = null;
		List<String> tokens = new ArrayList<>();

		if (cookies != null) {
			// Percorre lista cookie e extrai tokens
			for (Cookie c : cookies) {
				if (getNameCookie().equals(c.getName())) {
					tokens.add(c.getValue());
				}
			}
			if (!tokens.isEmpty()) {
				// Se houver apenas 1, retorna para rotina principal validar
				if (tokens.size() == 1) {
					return tokens.get(0);
				} else {
					// Se houver mais de 1, tenta localizar algum token válido
					for (String t : tokens) {
						token = t;
						try {
							validarToken(token);
							return token; // Se houver algum Token Válido Retorna para Rotina Principal
						} catch (Exception e) {
							// Passa para Próximo Token.
						}
					}
					return token; // Se não há nenhum token válido na lista, retorna para rotina explorar o erro
				}
			}
		}
		return null; // Se não há Tokens
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		
		int ttl = Prop.getInt("/siga.jwt.token.ttl");

		try {
			if (!req.getRequestURI().equals("/sigaex/autenticar.action")) {
				String token = extrairAuthorization(req);
				Map<String, Object> decodedToken = validarToken(token);
				final long now = System.currentTimeMillis() / 1000L;
				
				if ((Integer) decodedToken.get("exp") < now + ttl) {
					// Seria bom incluir o attributo HttpOnly
					String tokenNew = renovarToken(token);
					Map<String, Object> decodedNewToken = validarToken(token);
					Cookie cookie = buildCookie(tokenNew);
					resp.addCookie(cookie);
				}
				ContextoPersistencia.setUserPrincipal((String) decodedToken.get("sub"));
			}
			chain.doFilter(request, response);
			/*
			 * Exceções de Sessão o erro não é exposto ao usuário e redireciona para Login
			 */
		} catch (AuthJwtException e) {
			redirecionarParaFormDeLogin(req, resp, e);
			return;
		} catch (SigaJwtProviderException e) {
			redirecionarParaFormDeLogin(req, resp, e);
			return;
		} catch (JWTVerifyException e) {
			if ("jwt expired".equals(e.getMessage()))
				redirecionarParaFormDeLogin(req, resp, e);
			else
				throw new ServletException(e);
		} catch (SigaJwtInvalidException e) {
			redirecionarParaFormDeLogin(req, resp, e);
			return;
		} catch (SignatureException e) {
			redirecionarParaFormDeLogin(req, resp, e);
			return;
		} catch (Exception e) {
			if (e.getCause() instanceof AuthJwtException) {
				redirecionarParaFormDeLogin(req, resp, e);
				return;
			} else {
				/* Erro não gerado pela sessão adiciona na stack */
				throw new ServletException(e);
			}
		} finally {
			ContextoPersistencia.removeUserPrincipal();
		}
	}

	public static Cookie buildCookie(String tokenNew) {

		Cookie cookie = new Cookie(getNameCookie(), tokenNew);
		cookie.setPath("/");

		if (SigaMessages.isSigaSP() && getCookieDomain() != null) {
			cookie.setDomain(getCookieDomain());
		}

		return cookie;
	}

	public static Cookie buildEraseCookie() {
		Cookie cookie = new Cookie(getNameCookie(), "");
		cookie.setPath("/");
		if (SigaMessages.isSigaSP() && getCookieDomain() != null) {
			cookie.setDomain(getCookieDomain());
		}
		cookie.setMaxAge(0);
		return cookie;
	}

	private void redirecionarParaFormDeLogin(HttpServletRequest req, HttpServletResponse resp, Exception e) throws IOException {

		if (req.getHeader(X_REQUESTED_WITH) != null) {
			informarAutenticacaoInvalida(resp, e);
			return;
		}
		if (!isBrowser(req)) {
			informarAutenticacaoInvalida(resp, e);
			return;
		}

		// Envia Mensagem para Tela de Login
		HttpSession session = req.getSession(false);
		if (session != null) {
			session.setAttribute("loginMensagem",
					(e.getClass() != SigaJwtInvalidException.class && e.getClass() != JWTExpiredException.class)
							? SigaMessages.getMessage("login.erro.jwt")
							: "");
		}

		final HttpMethod method = HttpMethod.of(req);
		if (method == HttpMethod.GET) {
			final String base = Prop.get("/siga.base.url");
			String cont = req.getRequestURL() + (req.getQueryString() != null ? "?" + req.getQueryString() : "");
			if (base != null && base.startsWith("https:") && cont.startsWith("http:")) {
				cont = "https" + cont.substring(4);
			}
			resp.sendRedirect("/siga/public/app/login?cont=" + URLEncoder.encode(cont, "UTF-8"));
		} else {
			resp.sendRedirect("/siga/public/app/login");
		}
	}

	private JsonErrorPayload preencherPayload(String message, Exception exception) {
		final JsonErrorPayload payload = new JsonErrorPayload(message);
		final List<String> details = new ArrayList<>();
		if (exception != null) {
			details.add(exception.getLocalizedMessage());
			if (exception.getCause() != null) {
				details.add(exception.getCause().getLocalizedMessage());
			}
		}
		payload.setDetails(details);
		return payload;
	}

	private void informarAutenticacaoInvalida(HttpServletResponse resp, Exception e) throws IOException {
		final JsonErrorPayload payload = preencherPayload("Não foi possível autenticar o usuário: efetue o login novamente para continuar no sistema.", e);
		resp.setContentType(MediaType.APPLICATION_JSON);
		resp.setStatus(SC_UNAUTHORIZED); // 401 Unauthorized - authentication is required and has failed or has not yet been provided.
		resp.getWriter().write(JSON_MAPPER.writeValueAsString(payload));
	}

	private void informarAutenticacaoProibida(HttpServletResponse resp, Exception e) throws IOException {
		final JsonErrorPayload payload = preencherPayload("O acesso a este recurso é restrito.", e);
		resp.setContentType(MediaType.APPLICATION_JSON);
		resp.setStatus(SC_FORBIDDEN); // 403 Forbidden
		resp.getWriter().write(JSON_MAPPER.writeValueAsString(payload));
	}

	/**
	 * Este cookie é utilizado na sessão do usuário. Gera o nome do cookie de acordo
	 * com o valor da variavel de name=ambiente que se encontra no standalone.xml do
	 * server. Este metodo é importante para resolver o problema de compatibilidade
	 * de sessao quando se tem mais de uma aplicação aberta no mesmo navegador.
	 * 
	 * @return String
	 */
	private static String getNameCookie() {
		final String NAME_ENVIRONMENT_PRODUCTION = "PROD";
		final String NAME_ENVIRONMENT = Prop.get("/siga.ambiente");
		String nameCookie = SIGA_JWT_AUTH_COOKIE_NAME;

		if (SigaMessages.isSigaSP()) {
			if (!NAME_ENVIRONMENT_PRODUCTION.equals(NAME_ENVIRONMENT.toUpperCase().trim()))
				nameCookie += "-" + NAME_ENVIRONMENT;
		}
		return nameCookie;
	}

	private static String getCookieDomain() {
		return Prop.get("/siga.jwt.cookie.domain");
	}

	private static boolean isBrowser(HttpServletRequest request) {
		final String userAgent = String.valueOf(request.getHeader(USER_AGENT));
		for (String browser : USER_AGENT_BROWSERS) {
			if (containsIgnoreCase(userAgent, browser)) {
				return true;
			}
		}
		return false;
	}

}
