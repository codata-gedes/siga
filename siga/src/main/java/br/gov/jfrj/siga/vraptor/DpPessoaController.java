/*******************************************************************************
* Copyright (c) 2006 - 2011 SJRJ.
 * 
 *     This file is part of SIGA.
 * 
 *     SIGA is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     SIGA is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with SIGA.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
/*
 * Criado em  13/09/2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package br.gov.jfrj.siga.vraptor; 

import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;

import com.google.common.collect.Iterables;

import br.com.caelum.vraptor.Consumes;
import br.com.caelum.vraptor.Controller;
import br.com.caelum.vraptor.Get;
import br.com.caelum.vraptor.Path;
import br.com.caelum.vraptor.Post;
import br.com.caelum.vraptor.Result;
import br.com.caelum.vraptor.observer.download.Download;
import br.com.caelum.vraptor.observer.download.InputStreamDownload;
import br.com.caelum.vraptor.observer.upload.UploadedFile;
import br.com.caelum.vraptor.view.Results;
import br.gov.jfrj.siga.base.AplicacaoException;
import br.gov.jfrj.siga.base.GeraMessageDigest;
import br.gov.jfrj.siga.base.RegraNegocioException;
import br.gov.jfrj.siga.base.SigaMessages;
import br.gov.jfrj.siga.base.SigaModal;
import br.gov.jfrj.siga.base.util.Texto;
import br.gov.jfrj.siga.cp.CpIdentidade;
import br.gov.jfrj.siga.cp.CpServico;
import br.gov.jfrj.siga.cp.bl.Cp;
import br.gov.jfrj.siga.cp.bl.CpBL;
import br.gov.jfrj.siga.cp.bl.CpConfiguracaoBL;
import br.gov.jfrj.siga.cp.model.DpLotacaoSelecao;
import br.gov.jfrj.siga.dp.CpOrgaoUsuario;
import br.gov.jfrj.siga.dp.CpUF;
import br.gov.jfrj.siga.dp.DpCargo;
import br.gov.jfrj.siga.dp.DpFuncaoConfianca;
import br.gov.jfrj.siga.dp.DpLotacao;
import br.gov.jfrj.siga.dp.DpPessoa;
import br.gov.jfrj.siga.dp.DpPessoaUsuarioDTO;
import br.gov.jfrj.siga.dp.dao.CpDao;
import br.gov.jfrj.siga.dp.dao.DpCargoDaoFiltro;
import br.gov.jfrj.siga.dp.dao.DpFuncaoConfiancaDaoFiltro;
import br.gov.jfrj.siga.dp.dao.DpLotacaoDaoFiltro;
import br.gov.jfrj.siga.dp.dao.DpPessoaDaoFiltro;
import br.gov.jfrj.siga.model.Selecionavel;

@Controller
public class DpPessoaController extends SigaSelecionavelControllerSupport<DpPessoa, DpPessoaDaoFiltro> {

	private static final Logger log = Logger.getLogger(DpPessoaController.class);
	
	private Long orgaoUsu;
	private DpLotacaoSelecao lotacaoSel;
	private boolean semLimiteOrgaoOrigem = true;

	/**
	 * @deprecated CDI eyes only
	 */
	public DpPessoaController() {
		super();
	}

	@Inject
	public DpPessoaController(HttpServletRequest request, Result result, CpDao dao,
			SigaObjects so, EntityManager em) {
		super(request, result, dao, so, em);
		setSel(new DpPessoa());
		setItemPagina(10);
	}
	
	@Get
	@Path({"/app/pessoa/buscar-json/{sigla}"})
	public void busca(String sigla) throws Exception{
		aBuscarJson(sigla);
	}

	@Get
	@Post
	@Path({ "/app/pessoa/buscar", "/app/cosignatario/buscar", "/pessoa/buscar.action", "/cosignatario/buscar.action" })
	public void buscar(String sigla, String postback, Integer paramoffset, Long idOrgaoUsu, DpLotacaoSelecao lotacaoSel) throws Exception {

		this.orgaoUsu = idOrgaoUsu;
		if (postback == null && getLotaTitular() != null) {
			this.orgaoUsu = getLotaTitular().getIdOrgaoUsuario();
		}
		if (lotacaoSel != null && lotacaoSel.getId() != null && lotacaoSel.getId() > 0) {
			this.lotacaoSel = lotacaoSel;
		}
		this.getP().setOffset(paramoffset);

		if (sigla == null) {
			sigla = EMPTY;
		}

	    super.aBuscar(sigla, postback);
		result.include("param", getRequest().getParameterMap());
		result.include("request", getRequest());
		result.include("itens", getItens());
		result.include("tamanho", getTamanho());
		result.include("orgaosUsu", getOrgaosPermitidosUsuarioCadastrante());
		result.include("lotacaoSel", lotacaoSel == null ? new DpLotacaoSelecao() : lotacaoSel);
		result.include("idOrgaoUsu", orgaoUsu);
		result.include("sigla", sigla);
		result.include("postbak", postback);
		result.include("offset", paramoffset);
	}
	
	@Get("/app/pessoa/exibir")
	public void exibi(String sigla) {
		if (sigla != null) {
			result.include("pessoa", dao().getPessoaPorPrincipal(sigla));
		}
	}
		
	@Override
	protected DpPessoaDaoFiltro createDaoFiltro() {
		final DpPessoaDaoFiltro flt = new DpPessoaDaoFiltro();

		flt.setNome(Texto.removeAcentoMaiusculas(getNome()));
		if (lotacaoSel != null) {
			flt.setLotacao(lotacaoSel.buscarObjeto());
		}

		flt.setBuscarSemLimitarOrgaoOrigem(this.semLimiteOrgaoOrigem);
		
		flt.setIdOrgaoUsu(orgaoUsu);
		if (flt.getIdOrgaoUsu() == null && !getTitular().isTramitarOutrosOrgaos()) {
			if (getLotaTitular() == null) {
				throw new AplicacaoException("Usuário limitado não pode buscar em outros órgãos");
			}
			flt.setIdOrgaoUsu(getLotaTitular().getOrgaoUsuario().getIdOrgaoUsu());
		}

		String buscarFechadas = param("buscarFechadas");
		flt.setBuscarFechadas(toBoolean(buscarFechadas));
		String buscarSubstitutos = param("buscarSubstitutos");
		if (buscarSubstitutos != null && toBoolean(buscarSubstitutos)) {
		    flt.setBuscarSubstitutos(true);
		    flt.setId(getTitular().getId());
		} else {
		    flt.setBuscarSubstitutos(false);
		}
		
		flt.setSituacaoFuncionalPessoa("");
		
		boolean buscarApenasUsuariosVisiveisParaTramitacao = toBoolean(param("buscarApenasUsuariosVisiveisParaTramitacao"));
		flt.setBuscarApenasUsuariosVisiveisParaTramitacao(toBooleanDefaultIfNull(buscarApenasUsuariosVisiveisParaTramitacao, false));
		
		boolean buscarApenasUsuariosDeUnidadesReceptoras = toBoolean(param("buscarApenasUsuariosDeUnidadesReceptoras"));
		flt.setBuscarApenasUsuariosDeUnidadesReceptoras(toBooleanDefaultIfNull(buscarApenasUsuariosDeUnidadesReceptoras, false));

		return flt;
	}

	@Override
	protected Selecionavel selecionarPorNome(final DpPessoaDaoFiltro flt) throws AplicacaoException {
		Selecionavel sel = null;

		// Acrescenta o sesb e repete a busca
		final String sigla = flt.getSigla();
		flt.setSigla(getTitular().getSesbPessoa() + sigla);
		sel = dao().consultarPorSigla(flt);
		if (sel != null)
			return sel;
		flt.setSigla(sigla);

		// Procura por nome
		flt.setIdOrgaoUsu(getLotaTitular().getOrgaoUsuario().getIdOrgaoUsu());
		flt.setNome(Texto.removeAcentoMaiusculas(flt.getSigla()));
		flt.setSigla(null);

		final List<DpPessoa> pessoas = dao().consultarPorFiltro(flt);
		if (CollectionUtils.isEmpty(pessoas)) {
			return null;
		}
		return Iterables.getFirst(pessoas, null);
	}

	private boolean temPermissaoParaExportarDados() {
		return toBoolean(Cp.getInstance().getConf().podeUtilizarServicoPorConfiguracao(getTitular(), getTitular().getLotacao(),"SIGA;GI;CAD_PESSOA;EXP_DADOS"));
	}

	@Get
	@Post
	@Path({ "/public/app/pessoa/selecionar", "/app/pessoa/selecionar", "/app/cosignatario/selecionar",
			"/pessoa/selecionar.action", "/cosignatario/selecionar.action" })
	public void selecionar(final String propriedade, final String sigla) {
		if (equalsIgnoreCase("destinatario", propriedade)) {
			this.semLimiteOrgaoOrigem = getTitular().isTramitarOutrosOrgaos();
		}

		final String resultado = super.aSelecionar(sigla);

		if (resultado == "ajax_retorno") {
			result.include("sel", getSel());
			result.use(Results.page()).forwardTo("/WEB-INF/jsp/ajax_retorno.jsp");
		} else {
			result.use(Results.page()).forwardTo("/WEB-INF/jsp/ajax_vazio.jsp");
		}
	}

	@Get("app/pessoa/listar")
	public void lista(Integer paramoffset, Long idOrgaoUsu, String nome, String cpfPesquisa, Long idCargoPesquisa, Long idFuncaoPesquisa, Long idLotacaoPesquisa, String emailPesquisa, String identidadePesquisa,
			boolean buscarInativos) throws Exception {

		assertAcesso(CpServico.VERIFICADOR_ACESSO_CADASTRO_PESSOA);;
		
		result.include("request",getRequest());
		List<CpOrgaoUsuario> list = new ArrayList<CpOrgaoUsuario>();
		CpOrgaoUsuario ou = new CpOrgaoUsuario();
				
		result.include("temPermissaoParaExportarDados", temPermissaoParaExportarDados());
		
		if (CpConfiguracaoBL.SIGLAS_ORGAOS_ADMINISTRADORES.contains(getTitular().getOrgaoUsuario().getSigla())) {
			list = dao().listarOrgaosUsuariosAtivosEVisiveis();
			result.include("orgaosUsu", list);
			if (idOrgaoUsu == null) {
				final CpOrgaoUsuario primeiroOrgaoUsuario = Iterables.getFirst(list, null);
				final Long primeiroIdOrgaoUsuario = primeiroOrgaoUsuario != null ? primeiroOrgaoUsuario.getId() : null;
				carregarCombos(null, primeiroIdOrgaoUsuario, null, null, null, null, null, 0, Boolean.FALSE);
			}
		} else {
			ou = CpDao.getInstance().consultarPorSigla(getTitular().getOrgaoUsuario());
			list.add(ou);
			result.include("orgaosUsu", list);
			if (idOrgaoUsu == null) {
				carregarCombos(null, ou.getId(), null, null, null, null, null, 0, Boolean.FALSE);
			}
		}
		if (idOrgaoUsu != null && (CpConfiguracaoBL.SIGLAS_ORGAOS_ADMINISTRADORES.contains(getTitular().getOrgaoUsuario().getSigla())
				|| CpDao.getInstance().consultarPorSigla(getTitular().getOrgaoUsuario()).getId().equals(idOrgaoUsu))) {
			DpPessoaDaoFiltro dpPessoaFiltro = setupDpPessoaDaoFiltro(idOrgaoUsu, nome, cpfPesquisa, idCargoPesquisa, idFuncaoPesquisa, idLotacaoPesquisa, emailPesquisa, identidadePesquisa, buscarInativos);
			setItens(CpDao.getInstance().consultarPorFiltro(dpPessoaFiltro, paramoffset, 15));
			result.include("itens", getItens());
			long tamanho = dao().consultarQuantidade(dpPessoaFiltro);
			result.include("tamanho", tamanho);

			result.include("idOrgaoUsu", idOrgaoUsu);
			result.include("nome", nome);
			result.include("emailPesquisa", emailPesquisa);
			result.include("identidadePesquisa", identidadePesquisa);
			result.include("cpfPesquisa", cpfPesquisa);
			result.include("idCargoPesquisa", idCargoPesquisa);
			result.include("idFuncaoPesquisa", idFuncaoPesquisa);
			result.include("idLotacaoPesquisa", idLotacaoPesquisa);
			if (getItens().size() == 0) result.include("mensagemPesquisa", "Nenhum resultado encontrado.");			

			carregarCombos(null, idOrgaoUsu, null, null, null, null, cpfPesquisa, paramoffset, Boolean.FALSE);
		}		
	}

	@Transacional
	@Get("/app/pessoa/ativarInativar")
	public void ativarInativar(final Long id, Integer offset, Long idOrgaoUsu, String nome, String cpfPesquisa, Long idCargoPesquisa, Long idFuncaoPesquisa, Long idLotacaoPesquisa, String emailPesquisa, String identidadePesquisa) throws Exception{
		
		assertAcesso(CpServico.VERIFICADOR_ACESSO_CADASTRO_PESSOA);;
		
		CpOrgaoUsuario ou = new CpOrgaoUsuario();
		DpPessoa pessoaAnt = dao().consultar(id, DpPessoa.class, false).getPessoaAtual();
		ou.setIdOrgaoUsu(pessoaAnt.getOrgaoUsuario().getId());
		ou = CpDao.getInstance().consultarPorId(ou);

		if (CpConfiguracaoBL.SIGLAS_ORGAOS_ADMINISTRADORES.contains(getTitular().getOrgaoUsuario().getSigla())
				|| CpDao.getInstance().consultarPorSigla(getTitular().getOrgaoUsuario()).getId().equals(ou.getId())) {
			pessoaAnt = dao().consultar(id, DpPessoa.class, false).getPessoaAtual();

			CpIdentidade identidadeCadastrante = getIdentidadeCadastrante();
			// inativar
			if (pessoaAnt.getDataFimPessoa() == null) {
				pessoaAnt.setDataFimPessoa(new Date());
				pessoaAnt.setHisIdcFim(identidadeCadastrante);

				try {
					dao().iniciarTransacao();
					dao().gravar(pessoaAnt);
					dao().commitTransacao();
				} catch (final Exception e) {
					if(e.getCause() instanceof ConstraintViolationException &&
	    					("CORPORATIVO.DP_PESSOA_UNIQUE_PESSOA_ATIVA".equalsIgnoreCase(((ConstraintViolationException)e.getCause()).getConstraintName()))) {
						result.include(SigaModal.ALERTA, SigaModal.mensagem("Ocorreu um problema no cadastro da pessoa"));
	    			} else {
	    				throw new AplicacaoException("Erro na gravação", 0, e);
	    			}
					dao().rollbackTransacao();	
				}

			} else {// ativar
				// não pode ativar caso já exista uma pessoa com mesmo órgão, cargo, função de
				// confiança, lotação e cpf

				DpPessoaDaoFiltro pessoaFiltro = new DpPessoaDaoFiltro();
				pessoaFiltro.setIdOrgaoUsu(pessoaAnt.getOrgaoUsuario().getIdOrgaoUsu());
				pessoaFiltro.setCargo(pessoaAnt.getCargo());
				pessoaFiltro.setFuncaoConfianca(pessoaAnt.getFuncaoConfianca());
				pessoaFiltro.setLotacao(pessoaAnt.getLotacao());
				pessoaFiltro.setCpf(pessoaAnt.getCpfPessoa());
				pessoaFiltro.setNome("");
				pessoaFiltro.setId(id);
				pessoaFiltro.setBuscarFechadas(Boolean.FALSE);

				long tamanho = dao().consultarQuantidade(pessoaFiltro);
				if (tamanho > 0) {
					throw new AplicacaoException("Já existe outro usuário ativo com estes dados: Órgão, Cargo, Função, Unidade e CPF");
				}

				final DpPessoa pessoa = DpPessoa.novaInstanciaBaseadaEm(pessoaAnt);
				try {
					/*
					 * FIXME
					 * 
					 * Correção exclusiva para pessoas inativadas erroneamente durante bugs
					 * ocorridos durante a migração.
					 * 
					 * Após correções, remover o IF abaixo e alterar o chamada subsequente para:
					 * Cp.getInstance().getBL().criarIdentidadeComHistorico(new Date(), pessoaAnt, pessoa, identidadeCadastrante);
					 */
					DpPessoa pessoaSalva = dao().gravarComHistorico(pessoa, pessoaAnt,dao().consultarDataEHoraDoServidor(), identidadeCadastrante);
					if (pessoaSalva == pessoaAnt) {
						pessoaSalva.setDataFim(null);
						pessoaSalva = dao().gravar(pessoaSalva);
					}

					Cp.getInstance().getBL().criarIdentidadeComHistorico(new Date(), pessoaAnt, pessoaSalva, identidadeCadastrante);
				} catch (Exception e) {
					if(e.getCause() instanceof ConstraintViolationException &&
	    					("CORPORATIVO.DP_PESSOA_UNIQUE_PESSOA_ATIVA".equalsIgnoreCase(((ConstraintViolationException)e.getCause()).getConstraintName()))) {
						result.include(SigaModal.ALERTA, SigaModal.mensagem("Ocorreu um problema no cadastro da pessoa"));
	    			} else {
	    				log.error("Erro ao ativar pessoa " + pessoa + ": " + e.getMessage(), e);
	    				throw new AplicacaoException("Erro na gravação", 0, e);
	    			}
				}
			}

			
			this.result.redirectTo(this).lista(offset, idOrgaoUsu, nome, cpfPesquisa, idCargoPesquisa, idFuncaoPesquisa, idLotacaoPesquisa, emailPesquisa, identidadePesquisa, false);
		}
	}

	@Get("/app/pessoa/editar")
	public void edita(final Long id) {
		
		assertAcesso(CpServico.VERIFICADOR_ACESSO_CADASTRO_PESSOA);;
		
		CpOrgaoUsuario ou = new CpOrgaoUsuario();
		
		/*Carrega lista UF*/
		List<CpUF> ufList = dao().consultarUF();
		result.include("ufList",ufList);

		if (id != null) {
			DpPessoa pessoa = dao().consultar(id, DpPessoa.class, false);
			ou.setIdOrgaoUsu(pessoa.getOrgaoUsuario().getId());
			ou = CpDao.getInstance().consultarPorId(ou);
			if (CpConfiguracaoBL.SIGLAS_ORGAOS_ADMINISTRADORES.contains(getTitular().getOrgaoUsuario().getSigla()) || CpDao.getInstance()
					.consultarPorSigla(getTitular().getOrgaoUsuario()).getId().equals(ou.getId())) {
				
				/*
				 * Envio da sigla do usuário para validação no front
				 * Referente ao cartão 859
				 */
				result.include("sigla", getUsuario().getOrgaoUsuario().getSigla());
				result.include("nmPessoa", pessoa.getNomePessoa());
				result.include("cpf", pessoa.getCpfFormatado());
				result.include("email", pessoa.getEmailPessoa());
				result.include("idOrgaoUsu", pessoa.getOrgaoUsuario().getId());
				result.include("nmOrgaousu", pessoa.getOrgaoUsuario().getNmOrgaoUsu());
				result.include("tramitarOutrosOrgaos", pessoa.isTramitarOutrosOrgaos());
				result.include("isUsuarioVisivelTramitacao", pessoa.isVisivelTramitacao());
				
				/*
				 * Adicao de campos RG
				 * Cartao 1057
				 */


				result.include("identidade",pessoa.getIdentidade());
				result.include("orgaoIdentidade",pessoa.getOrgaoIdentidade());
						
				
				if (pessoa.getDataNascimento() != null) {
					result.include("dtNascimento", pessoa.getDtNascimentoDDMMYYYY());
				}
				if (pessoa.getCargo() != null) {
					result.include("idCargo", pessoa.getCargo().getId());
				}
				if (pessoa.getNomeExibicao() != null) {
					result.include("nomeExibicao", pessoa.getNomeExibicao());
				}
				if (pessoa.getFuncaoConfianca() != null) {
					result.include("idFuncao", pessoa.getFuncaoConfianca().getId());
				}
				if (pessoa.getLotacao() != null) {
					result.include("idLotacao", pessoa.getLotacao().getId());
				}
				
				if(pessoa.getUfIdentidade() != null) {
					result.include("ufIdentidade",pessoa.getUfIdentidade());
				}
				
				if(pessoa.getDataExpedicaoIdentidade() != null) {
					result.include("dataExpedicaoIdentidade",pessoa.getDataExpedicaoIdentidadeDDMMYYYY());
				}	
			}
		}
		if (id == null || (ou.getId() != null && (CpConfiguracaoBL.SIGLAS_ORGAOS_ADMINISTRADORES.contains(getTitular().getOrgaoUsuario().getSigla())
				|| CpDao.getInstance().consultarPorSigla(getTitular().getOrgaoUsuario()).getId().equals(ou.getId())))) {
			if (ou.getId() == null) {
				ou = CpDao.getInstance().consultarPorSigla(getTitular().getOrgaoUsuario());
			}

			if (ou.getId() != null) {
				DpCargoDaoFiltro cargo = new DpCargoDaoFiltro();
				cargo.setNome("");
				cargo.setIdOrgaoUsu(ou.getId());
				List<DpCargo> lista = new ArrayList<DpCargo>();
				DpCargo c = new DpCargo();
				c.setId(0L);
				c.setDescricao("Selecione");
				lista.add(c);
				lista.addAll((List<DpCargo>) CpDao.getInstance().consultarPorFiltro(cargo));
				result.include("listaCargo", lista);

				DpLotacaoDaoFiltro lotacao = new DpLotacaoDaoFiltro();
				lotacao.setNome("");
				lotacao.setIdOrgaoUsu(ou.getId());
				lotacao.setBuscarParaCadastroDePessoa(true);
				List<DpLotacao> listaLotacao = new ArrayList<DpLotacao>();
				DpLotacao l = new DpLotacao();
				l.setNomeLotacao("Selecione");
				l.setId(0L);
				l.setSiglaLotacao("");
				CpOrgaoUsuario cpOrgaoUsuario = new CpOrgaoUsuario();
				cpOrgaoUsuario.setIdOrgaoUsu(0L);
				cpOrgaoUsuario.setSiglaOrgaoUsu("");
				l.setOrgaoUsuario(cpOrgaoUsuario);
				listaLotacao.add(l);
				listaLotacao.addAll(CpDao.getInstance().consultarPorFiltro(lotacao));
				result.include("listaLotacao", listaLotacao);

				DpFuncaoConfiancaDaoFiltro funcao = new DpFuncaoConfiancaDaoFiltro();
				funcao.setNome("");
				funcao.setIdOrgaoUsu(ou.getId());
				List<DpFuncaoConfianca> listaFuncao = new ArrayList<DpFuncaoConfianca>();
				DpFuncaoConfianca f = new DpFuncaoConfianca();
				f.setNomeFuncao("Selecione");
				f.setIdFuncao(0L);
				listaFuncao.add(f);
				listaFuncao.addAll(CpDao.getInstance().consultarPorFiltro(funcao));
				result.include("listaFuncao", listaFuncao);
				result.include("request", getRequest());
				result.include("id", id);
			}
		}
		List<CpOrgaoUsuario> list = new ArrayList<CpOrgaoUsuario>();
		if (CpConfiguracaoBL.SIGLAS_ORGAOS_ADMINISTRADORES.contains(getTitular().getOrgaoUsuario().getSigla())) {
			list = dao().listarOrgaosUsuariosAtivosEVisiveis();

			CpOrgaoUsuario org = new CpOrgaoUsuario();
			org.setNmOrgaoUsu("Selecione");
			org.setIdOrgaoUsu(0L);
			list.add(0, org);
			result.include("orgaosUsu", list);
		} else {
			ou = CpDao.getInstance().consultarPorSigla(getTitular().getOrgaoUsuario());
			list.add(ou);
			result.include("orgaosUsu", list);

		}
	}

	@Post("/app/pessoa/carregarCombos")
	public void carregarCombos(final Long id, final Long idOrgaoUsu, final String nmPessoa, final String dtNascimento,
			final String cpf, final String email, final String cpfPesquisa, final Integer paramoffset,
			Boolean retornarEnvioEmail) {
		result.include("request", getRequest());
		result.include("id", id);
		result.include("idOrgaoUsu", idOrgaoUsu);
		result.include("nmPessoa", nmPessoa);
		result.include("dtNascimento", dtNascimento);
		result.include("cpf", cpf);
		result.include("email", email);
		result.include("cpfPesquisa", cpfPesquisa);
		result.include("temPermissaoParaExportarDados", temPermissaoParaExportarDados());
		setItemPagina(15);
		result.include("currentPageNumber", calculaPaginaAtual(paramoffset));
		List<CpOrgaoUsuario> list = new ArrayList<CpOrgaoUsuario>();
		if (CpConfiguracaoBL.SIGLAS_ORGAOS_ADMINISTRADORES.contains(getTitular().getOrgaoUsuario().getSigla())) {
			list = dao().listarOrgaosUsuariosAtivosEVisiveis();
			result.include("orgaosUsu", list);
		} else {
			CpOrgaoUsuario ou = CpDao.getInstance().consultarPorSigla(getTitular().getOrgaoUsuario());
			list.add(ou);
			result.include("orgaosUsu", list);
		}
		
		if (retornarEnvioEmail == null || !retornarEnvioEmail) {
			DpCargoDaoFiltro cargo = new DpCargoDaoFiltro();
			cargo.setNome("");
			cargo.setIdOrgaoUsu(idOrgaoUsu);
			List<DpCargo> lista = new ArrayList<DpCargo>();
			DpCargo c = new DpCargo();
			c.setId(0L);
			c.setDescricao("Selecione");
			lista.add(c);
			lista.addAll((List<DpCargo>) CpDao.getInstance().consultarPorFiltro(cargo));
			result.include("listaCargo", lista);
		}	
		
		DpLotacaoDaoFiltro lotacao = new DpLotacaoDaoFiltro();
		lotacao.setNome("");
		lotacao.setIdOrgaoUsu(idOrgaoUsu);
		lotacao.setBuscarParaCadastroDePessoa(true);
		List<DpLotacao> listaLotacao = new ArrayList<DpLotacao>();
		DpLotacao l = new DpLotacao();
		l.setNomeLotacao("Selecione");
		l.setId(0L);
		l.setSiglaLotacao("");
		CpOrgaoUsuario cpOrgaoUsuario = new CpOrgaoUsuario();
		cpOrgaoUsuario.setIdOrgaoUsu(0L);
		cpOrgaoUsuario.setSiglaOrgaoUsu("");
		l.setOrgaoUsuario(cpOrgaoUsuario);
		listaLotacao.add(l);
		if (idOrgaoUsu != null && idOrgaoUsu.longValue() != CpConfiguracaoBL.ID_ORGAO_ROOT) {
			listaLotacao.addAll(CpDao.getInstance().consultarPorFiltro(lotacao));
		}
		result.include("listaLotacao", listaLotacao);

		if (retornarEnvioEmail == null || !retornarEnvioEmail) {
			DpFuncaoConfiancaDaoFiltro funcao = new DpFuncaoConfiancaDaoFiltro();
			funcao.setNome("");
			funcao.setIdOrgaoUsu(idOrgaoUsu);
			List<DpFuncaoConfianca> listaFuncao = new ArrayList<DpFuncaoConfianca>();
			DpFuncaoConfianca f = new DpFuncaoConfianca();
			f.setNomeFuncao("Selecione");
			f.setIdFuncao(0L);
			listaFuncao.add(f);
			listaFuncao.addAll(CpDao.getInstance().consultarPorFiltro(funcao));
			result.include("listaFuncao", listaFuncao);
		}
		
		if (retornarEnvioEmail == null || !retornarEnvioEmail) {
			/*Carrega lista UF*/
			List<CpUF> ufList = dao().consultarUF();
			result.include("ufList",ufList);
		}

		if (paramoffset == null) {
			result.use(Results.page()).forwardTo("/WEB-INF/page/dpPessoa/edita.jsp");
		} else if (retornarEnvioEmail != null && retornarEnvioEmail) {
			result.use(Results.page()).forwardTo("/WEB-INF/page/dpPessoa/enviaEmail.jsp");
		} else {
			result.use(Results.page()).forwardTo("/WEB-INF/page/dpPessoa/lista.jsp");
		}
	}

	@Transacional
	@Post("/app/pessoa/gravar")
	public void editarGravar(final Long id, final Long idOrgaoUsu, final Long idCargo, final Long idFuncao,
			final Long idLotacao, final String nmPessoa, final String dtNascimento, final String cpf,
			final String email, final String identidade, final String orgaoIdentidade, final String ufIdentidade,
			final String dataExpedicaoIdentidade, final String nomeExibicao, final String enviarEmail,
			final Boolean tramitarOutrosOrgaos, final Boolean isUsuarioVisivelTramitacao) throws Exception {

		assertAcesso(CpServico.VERIFICADOR_ACESSO_CADASTRO_PESSOA);;

		try {
			DpPessoa pes = new CpBL().criarUsuario(id, getIdentidadeCadastrante(), idOrgaoUsu, idCargo, idFuncao,
					idLotacao, nmPessoa, dtNascimento, cpf, email, identidade, orgaoIdentidade, ufIdentidade,
					dataExpedicaoIdentidade, nomeExibicao, enviarEmail, toBooleanDefaultIfNull(tramitarOutrosOrgaos, false), toBooleanDefaultIfNull(isUsuarioVisivelTramitacao, true));
			
			if (id == null) {
				String mensagem = String.format("%s\n%s", SigaMessages.getMessage("usuario.cadastro.sucesso"), SigaMessages.getMessage("usuario.cadastro.envioemail"));
				result.include(SigaModal.ALERTA, SigaModal.mensagem(mensagem).titulo("Sucesso"));
			} else {
				result.include("mensagem", "Operação realizada com sucesso!");	
			}
		} catch (RegraNegocioException e) {
			result.include(SigaModal.ALERTA, e.getMessage());
		}

		lista(0, null, "", "", null, null, null, "", null, false);
	}

	
	@Get({"/app/pessoa/check_nome_por_cpf"})
	public void checkNome(String nome, String cpf, String id) throws AplicacaoException {
		Long idd = 0L;
		if(id != null && !"".equals(id.trim())) {
			idd = Long.valueOf(id.toString());
		}
		List<DpPessoa> lista = new ArrayList<DpPessoa>();
		if(cpf != null && !"".equals(cpf.trim())) {
			lista = dao().listarCpfAtivoInativo(Long.valueOf(cpf.replace(".", "").replace("-", "")));
		}
		
		List<DpPessoa> pessoas = new ArrayList<DpPessoa>();
		for (DpPessoa dpPessoa : lista) {
			if(!dpPessoa.getNomePessoa().equalsIgnoreCase(nome.trim()) && !idd.equals(dpPessoa.getId())) {
				pessoas.add(dpPessoa);
			}
		}
		if(pessoas.isEmpty()) {
			result.use(Results.page()).forwardTo("/WEB-INF/jsp/ajax_vazio.jsp");
		} else {
			result.include("pessoas",pessoas);
			result.use(Results.page()).forwardTo("/WEB-INF/page/dpPessoa/pessoas.jsp");	
		}
	}
	

	@Get("/app/pessoa/carregarExcel")
	public void carregarExcel() {
		
		assertAcesso(CpServico.VERIFICADOR_ACESSO_CADASTRO_PESSOA);;
		
		if (CpConfiguracaoBL.SIGLAS_ORGAOS_ADMINISTRADORES.contains(getTitular().getOrgaoUsuario().getSigla())) {
			result.include("orgaosUsu", dao().listarOrgaosUsuarios());
		} else {
			result.include("nmOrgaousu", getTitular().getOrgaoUsuario().getNmOrgaoUsu());
		}

		result.use(Results.page()).forwardTo("/WEB-INF/page/dpPessoa/cargaPessoa.jsp");
	}

	@Transacional
	@Post("/app/pessoa/carga")
	public Download carga(final UploadedFile arquivo, Long idOrgaoUsu) throws Exception {
		
		assertAcesso(CpServico.VERIFICADOR_ACESSO_CADASTRO_PESSOA);;
		
		InputStream inputStream = null;
		try {
			String nomeArquivo = arquivo.getFileName();
			String extensao = nomeArquivo.substring(nomeArquivo.lastIndexOf("."), nomeArquivo.length());

			java.nio.file.Path tempFilePath = Files.createTempFile(UUID.randomUUID().toString(), extensao);
			Files.copy(arquivo.getFile(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);

			CpOrgaoUsuario orgaoUsuario = new CpOrgaoUsuario();
			if (idOrgaoUsu != null) {
				orgaoUsuario.setIdOrgaoUsu(idOrgaoUsu);
			} else {
				orgaoUsuario = getTitular().getOrgaoUsuario();
			}

			CpBL cpbl = new CpBL();
			inputStream = cpbl.uploadPessoa(tempFilePath.toFile(), orgaoUsuario, extensao, getIdentidadeCadastrante());
		} catch (Exception e) {
			throw new AplicacaoException("Problemas ao salvar pessoa(s)", 0, e);	
		}
		if (inputStream == null) {
			String mensagem = String.format("%s\n%s", SigaMessages.getMessage("usuario.cadastro.sucesso"), SigaMessages.getMessage("usuario.cadastro.envioemail"));
			result.include(SigaModal.ALERTA, SigaModal.mensagem(mensagem).titulo("Sucesso"));
			carregarExcel();
		} else {			
			return new InputStreamDownload(inputStream, "application/text", "inconsistencias.txt");
		}
		return null;
	}
	
	@Consumes("application/json")
	@Post("/app/pessoa/usuarios/envioDeEmailPendente")
	public void buscarUsuariosComEnvioDeEmailPendente(DpPessoaUsuarioDTO dados) {
		
		assertAcesso(CpServico.VERIFICADOR_ACESSO_CADASTRO_PESSOA);;
		
		DpPessoaDaoFiltro dpPessoa = new DpPessoaDaoFiltro();
		
		dpPessoa.setIdOrgaoUsu(dados.getIdOrgaoUsu());		
		dpPessoa.prepararLotacao(dados.getIdLotacaoSelecao());
		
		List<DpPessoaUsuarioDTO> usuarios = dao().consultarUsuariosComEnvioDeEmailPendenteFiltrandoPorLotacao(dpPessoa);
		
		result.use(Results.json()).from(usuarios).serialize();
	}
	
	@Get
	@Post
	@Path({"app/pessoa/enviarEmail", "/pessoa/enviarEmail.action"})
	public void enviaEmail(Integer paramoffset, Long idOrgaoUsu, String nome, String cpfPesquisa,
			String idLotacaoPesquisa, String idUsuarioPesquisa, Integer paramTamanho) throws Exception {
		
		assertAcesso(CpServico.VERIFICADOR_ACESSO_CADASTRO_PESSOA);;
		
		result.include("request", getRequest());
		List<CpOrgaoUsuario> list = new ArrayList<CpOrgaoUsuario>();
		CpOrgaoUsuario ou = new CpOrgaoUsuario();
		if (CpConfiguracaoBL.SIGLAS_ORGAOS_ADMINISTRADORES.contains(getTitular().getOrgaoUsuario().getSigla())) {
			list = dao().listarOrgaosUsuarios();
			list.remove(0);
			result.include("orgaosUsu", list);
			if (idOrgaoUsu == null) {
				carregarCombos(null, idOrgaoUsu, null, null, null, null, null, 0, Boolean.TRUE);
			}
		} else {
			ou = CpDao.getInstance().consultarPorSigla(getTitular().getOrgaoUsuario());
			list.add(ou);
			result.include("orgaosUsu", list);
			if (idOrgaoUsu == null) {
				carregarCombos(null, ou.getId(), null, null, null, null, null, 0, Boolean.TRUE);
			}
		}
		if (idOrgaoUsu != null && (CpConfiguracaoBL.SIGLAS_ORGAOS_ADMINISTRADORES.contains(getTitular().getOrgaoUsuario().getSigla())
				|| CpDao.getInstance().consultarPorSigla(getTitular().getOrgaoUsuario()).getId().equals(idOrgaoUsu))) {
			DpPessoaDaoFiltro dpPessoa = new DpPessoaDaoFiltro();
			if (paramoffset == null) {
				paramoffset = 0;
			}
			dpPessoa.setIdOrgaoUsu(idOrgaoUsu);
			dpPessoa.setNome(Texto.removeAcento(nome != null ? nome : ""));
			dpPessoa.prepararLotacao(idLotacaoPesquisa);	
			dpPessoa.prepararPessoa(idUsuarioPesquisa);
						
			if (cpfPesquisa != null && !"".equals(cpfPesquisa)) {
				dpPessoa.setCpf(Long.valueOf(cpfPesquisa.replace(".", "").replace("-", "")));
			}
			dpPessoa.setBuscarFechadas(Boolean.TRUE);
			setItens(CpDao.getInstance().consultarPorFiltroSemIdentidade(dpPessoa, paramoffset, 15));
			result.include("itens", getItens());
			
			Integer tamanho = 0;
			if (paramTamanho == null) {
				tamanho = dao().consultarQuantidadeDpPessoaSemIdentidade(dpPessoa);
			} else {
				tamanho = paramTamanho;
			}
			result.include("tamanho", tamanho);

			result.include("idOrgaoUsu", idOrgaoUsu);
			result.include("nome", nome);
			result.include("cpfPesquisa", cpfPesquisa);			
			result.include("idLotacaoPesquisa", idLotacaoPesquisa);
			result.include("idUsuarioPesquisa", idUsuarioPesquisa);
			this.result.include("mensagem", "Operação realizada com sucesso!");
			carregarCombos(null, idOrgaoUsu, null, null, null, null, cpfPesquisa, paramoffset, Boolean.TRUE);
		}
	}
	
	@Transacional
	@Post("app/pessoa/enviar")
	public void enviar(Long idOrgaoUsu, String nome, String cpfPesquisa, String idLotacaoPesquisa, String idUsuarioPesquisa) throws Exception {
		
		assertAcesso(CpServico.VERIFICADOR_ACESSO_CADASTRO_PESSOA);;
		
		String[] senhaGerada = new String[1];

		if (idOrgaoUsu == null || idOrgaoUsu == 0)
			throw new AplicacaoException("Órgão não informado");

		DpPessoaDaoFiltro dpPessoa = new DpPessoaDaoFiltro();

		dpPessoa.setIdOrgaoUsu(idOrgaoUsu);
		dpPessoa.setNome(Texto.removeAcento(nome != null ? nome : ""));
		dpPessoa.prepararLotacao(idLotacaoPesquisa);
		dpPessoa.prepararPessoa(idUsuarioPesquisa);
		
		if (cpfPesquisa != null && !"".equals(cpfPesquisa)) {
			dpPessoa.setCpf(Long.valueOf(cpfPesquisa.replace(".", "").replace("-", "")));
		}

		List<DpPessoa> lista = CpDao.getInstance().consultarPorFiltroSemIdentidade(dpPessoa, 0, 0);
		String cpfAnterior = "";
		for (DpPessoa dpPessoa2 : lista) {

			if (!cpfAnterior.equals(dpPessoa2.getCpfPessoa().toString())) {
				senhaGerada[0] = GeraMessageDigest.geraSenha();
			}
			Cp.getInstance().getBL().criarIdentidade(dpPessoa2.getSesbPessoa() + dpPessoa2.getMatricula(),
					dpPessoa2.getCpfFormatado(), getIdentidadeCadastrante(), null, senhaGerada, Boolean.FALSE, Boolean.TRUE);
			cpfAnterior = dpPessoa2.getCpfPessoa().toString();
		}
		this.result.redirectTo(this).enviaEmail(0, idOrgaoUsu, nome, cpfPesquisa, idLotacaoPesquisa, idUsuarioPesquisa,0);
	}

	@Post
	@Path("app/pessoa/exportarCsv")
	public Download exportarCsv(Long idOrgaoUsu, String nome, String cpfPesquisa, Long idCargoPesquisa,

    Long idFuncaoPesquisa, Long idLotacaoPesquisa, String emailPesquisa, String identidadePesquisa, boolean buscarInativos) throws IOException {
		assertAcesso(CpServico.VERIFICADOR_ACESSO_CADASTRO_PESSOA);

		if (idOrgaoUsu != null && (CpConfiguracaoBL.SIGLAS_ORGAOS_ADMINISTRADORES.contains(getTitular().getOrgaoUsuario().getSigla())
				|| CpDao.getInstance().consultarPorSigla(getTitular().getOrgaoUsuario()).getId().equals(idOrgaoUsu))) {
			
			DpPessoaDaoFiltro dpPessoaFiltro = setupDpPessoaDaoFiltro(idOrgaoUsu, nome, cpfPesquisa, idCargoPesquisa, idFuncaoPesquisa, idLotacaoPesquisa, emailPesquisa, identidadePesquisa, buscarInativos);
			
			List<DpPessoa> lista = CpDao.getInstance().consultarPorFiltro(dpPessoaFiltro, 0, 0);
			
			if (lista.size() > 0) {
				StringBuilder texto = new StringBuilder()
						.append("Sigla do Órgão;")
						.append("Cargo;")
						.append("Função de Confiança;")
						.append("Sigla da Unidade;")
						.append("Nome;")
						.append("Data de Nascimento;")
						.append("CPF;")
						.append("E-mail;")
						.append("Usuário;")
						.append("RG;")
						.append("Órgão Expedidor;")
						.append("UF;")
						.append("Data de Expedição;")
						.append("Status")
						.append(System.lineSeparator());

				for (DpPessoa p : lista) {
					texto.append(p.getOrgaoUsuario().getSiglaOrgaoUsu() + ";");
					texto.append(p.getCargo().getNomeCargo() + ";");
					texto.append(p.getFuncaoConfianca() != null ? p.getFuncaoConfianca().getNomeFuncao() + ";" : ";");
					texto.append(p.getLotacao().getSiglaLotacao() + ";");
					texto.append(p.getNomePessoa() + ";");
					texto.append(p.getDataNascimento() == null ? ";" : p.getDataNascimento() + ";");
					texto.append(p.getCpfFormatado() + ";");
					texto.append(p.getEmailPessoa() + ";");
					texto.append(p + ";");
					texto.append(p.getIdentidade() != null ? p.getIdentidade() + ";" : ";");
					texto.append(p.getOrgaoIdentidade() != null ? p.getOrgaoIdentidade() + ";" : ";");
					texto.append(p.getUfIdentidade() != null ? p.getUfIdentidade() + ";" : ";");
					texto.append(p.getDataExpedicaoIdentidadeDDMMYYYY() != null ? p.getDataExpedicaoIdentidadeDDMMYYYY() + ";" : ";");
					texto.append((p.getDataFimPessoa() == null ? "Ativo" : "Inativo") + ";");
					texto.append(System.lineSeparator());
				}

				try (InputStream inputStream = new ByteArrayInputStream(texto.toString().getBytes())) {
					return new InputStreamDownload(inputStream, "text/csv", "pessoas.csv");
				}
			} else {
				if (CpConfiguracaoBL.SIGLAS_ORGAOS_ADMINISTRADORES.contains(getTitular().getOrgaoUsuario().getSigla())) {					
					result.include("orgaosUsu", dao().listarOrgaosUsuarios());					
				} else {					
					List<CpOrgaoUsuario> list = new ArrayList<CpOrgaoUsuario>();					
					result.include("orgaosUsu", list.add(CpDao.getInstance().consultarPorSigla(getTitular().getOrgaoUsuario())));					
				}														
					result.include("idOrgaoUsu", idOrgaoUsu);
					result.include("nome", nome);
					result.include("cpfPesquisa", cpfPesquisa);
					result.include("idCargoPesquisa", idCargoPesquisa);
					result.include("idFuncaoPesquisa", idFuncaoPesquisa);
					result.include("idLotacaoPesquisa", idLotacaoPesquisa);					
					result.include("emailPesquisa", emailPesquisa);			
					result.include("identidadePesquisa", identidadePesquisa);
					result.include("mensagemPesquisa", "Nenhum resultado encontrado.");
					result.include("temPermissaoParaExportarDados", temPermissaoParaExportarDados());

					carregarCombos(null, idOrgaoUsu, null, null, null, null, cpfPesquisa, 0, Boolean.FALSE);																						
			}					
		}

		return null;
	}

	
	private DpPessoaDaoFiltro setupDpPessoaDaoFiltro(Long idOrgaoUsu, String nome, String cpfPesquisa, Long idCargoPesquisa,
			Long idFuncaoPesquisa, Long idLotacaoPesquisa, String emailPesquisa, String identidadePesquisa, boolean buscarInativos) {
		
		DpPessoaDaoFiltro dpPessoaFiltro = new DpPessoaDaoFiltro();
		
		dpPessoaFiltro.setBuscarFechadas(buscarInativos);
		dpPessoaFiltro.setIdOrgaoUsu(idOrgaoUsu);
		dpPessoaFiltro.setNome(isNotBlank(nome) ? Texto.removeAcento(nome) : EMPTY);
		dpPessoaFiltro.setEmail(isNotBlank(emailPesquisa) ? Texto.removeAcento(emailPesquisa) : EMPTY);
		dpPessoaFiltro.setIdentidade(identidadePesquisa);
		if(idCargoPesquisa != null) {
			DpCargo cargo = new DpCargo();
			cargo.setId(idCargoPesquisa);
			dpPessoaFiltro.setCargo(cargo);
		}
		if (idLotacaoPesquisa != null) {
			DpLotacao lotacao = new DpLotacao();
			lotacao.setId(idLotacaoPesquisa);
			dpPessoaFiltro.setLotacao(lotacao);
		}
		if (idFuncaoPesquisa != null) {
			DpFuncaoConfianca funcao = new DpFuncaoConfianca();
			funcao.setIdFuncao(idFuncaoPesquisa);
			dpPessoaFiltro.setFuncaoConfianca(funcao);
		}
		if (isNotBlank(cpfPesquisa)) {
			dpPessoaFiltro.setCpf(Long.valueOf(cpfPesquisa.replace(".", EMPTY).replace("-", EMPTY)));
		}
		dpPessoaFiltro.setId(Long.valueOf(0));
		dpPessoaFiltro.setBuscarParaCadastroDePessoas(true);
		
		return dpPessoaFiltro;
	}
	
	/*
	 * Pega informações do usuário logado
	 * Referente ao cartão 859
	 */
	protected DpPessoa getUsuario() {
		return so.getCadastrante();
	}
}
