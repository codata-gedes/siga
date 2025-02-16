<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page language="java" contentType="text/html; charset=UTF-8"
	buffer="64kb"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<%@ taglib uri="http://localhost/customtag" prefix="tags"%>
<%@ taglib uri="http://localhost/jeetags" prefix="siga"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<siga:pagina titulo="Inclusão de Cópia">

	<c:if test="${not mob.doc.eletronico}">
		<script type="text/javascript">
			$("html").addClass("fisico");
			$("body").addClass("fisico");
		</script>
	</c:if>

	<script type="text/javascript" language="Javascript1.1">
		function sbmt() {
			ExMovimentacaoForm.page.value = '';
			ExMovimentacaoForm.acao.value = '${request.contextPath}/app/expediente/mov/copiar_gravar';
			ExMovimentacaoForm.submit();
		}
	</script>

	<div class="container-fluid">
		<div class="card bg-light mb-3">
			<div class="card-header">
				<h5>Inclusão de Cópia de Documento -
					${mob.siglaEDescricaoCompleta}</h5>
			</div>
			<div class="card-body">
				<form
					action="${request.contextPath}/app/expediente/mov/copiar_gravar"
					enctype="multipart/form-data" method="post">
					<input type="hidden" name="postback" value="1" /> <input
						type="hidden" name="sigla" value="${sigla}" />
					<c:choose>
						<c:when test="${!doc.eletronico}">
							<input type="hidden" name="postback" value="1" />
							<input type="hidden" name="sigla" value="${sigla}" />
							<div class="row">
								<div class="col-md-2 col-sm-3">
									<div class="form-group">
										<label for="dtMovString">Data</label> <input
											class="form-control" type="text" name="dtMovString"
											value="${dtMovString}"
											onblur="javascript:verifica_data(this,0);" />
									</div>
								</div>
								<div class="col-sm-6">
									<div class="form-group">
										<label>Responsável</label>
										<siga:selecao tema="simple" propriedade="subscritor"
											modulo="siga" />
									</div>
								</div>
								<div class="col-sm-2 mt-4">
									<div class="form-check form-check-inline">
										<input class="form-check-input" type="checkbox" theme="simple"
											name="substituicao" value="${substituicao}"
											onclick="javascript:displayTitular(this);" /> <label
											class="form-check-label">Substituto</label>
									</div>
								</div>
							</div>
							<div class="row">
								<div class="col-12">
									<div class="form-group">
										<c:choose>
											<c:when test="${!substituicao}">
												<div id="tr_titular" style="display: none">
											</c:when>
											<c:otherwise>
												<div id="tr_titular" style="">
											</c:otherwise>
										</c:choose>
										<label>Titular</label> <input class="form-control"
											type="hidden" name="campos" value="titularSel.id" />
										<siga:selecao propriedade="titular" tema="simple"
											modulo="siga" />
									</div>
								</div>
							</div>
							<div class="row">
								<div class="col-12">
									<div class="form-group">
										<label>Função do Responsável</label> <input
											class="form-control" type="hidden" name="campos"
											value="nmFuncaoSubscritor" /> <input class="form-control"
											type="text" name="nmFuncaoSubscritor"
											value="${nmFuncaoSubscritor}" size="50" maxLength="128"
											theme="simple" /> <small class="form-text text-muted">(opcional)</small>
									</div>
								</div>
							</div>
						</c:when>
					</c:choose>
					<div class="row">
						<div class="col col-12">
							<siga:selecao
							  titulo="Documento"
							  propriedade="documentoRef"
							  urlAcao="expediente/buscar"
							  urlSelecionar="expediente/selecionar"
							  modulo="sigaex"
							  primeiraVez="sim"
							 />
						</div>
					</div>
					<div class="row">
						<div class="col-12">
							<input type="submit" value="Ok" class="btn btn-primary" /> <input
								type="button" value="<fmt:message key="botao.voltar"/>"
								onclick="javascript:history.back();" class="btn btn-cancel ml-2" />
						</div>
					</div>
				</form>
			</div>
		</div>
	</div>
</siga:pagina>
