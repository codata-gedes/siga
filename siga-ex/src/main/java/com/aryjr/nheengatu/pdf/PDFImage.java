/*
 * The Nheengatu Project : a free Java library for HTML  abstraction.
 *
 * Project Info:  http://www.aryjr.com/nheengatu/
 * Project Lead:  Ary Rodrigues Ferreira Junior
 *
 * (C) Copyright 2005, 2006 by Ary Junior
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.aryjr.nheengatu.pdf;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.lang3.math.NumberUtils;

import com.aryjr.nheengatu.html.Tag;
import com.aryjr.nheengatu.util.TagsManager;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Image;
import com.openhtmltopdf.css.parser.property.PrimitivePropertyBuilders.Height;

/**
 * 
 * A HTML image in a PDF document.
 * 
 * @version $Id: PDFImage.java,v 1.1 2007/12/26 15:57:41 tah Exp $
 * @author <a href="mailto:junior@aryjr.com">Ary Junior</a>
 * 
 */
public class PDFImage {

	public static Image createImage(final Tag htmlImage) throws BadElementException, IOException {
		final TagsManager gm = TagsManager.getInstance();
		// TODO the image path can't be static
		Image img;
		if (htmlImage.getPropertyValue("src").indexOf("http://") >= 0) {
			img = Image.getInstance(new URL(htmlImage.getPropertyValue("src")));
		} else {
			img = Image.getInstance(htmlImage.getPropertyValue("src"));
		}
		img.setWidthPercentage(0);// TODO without it, the image dimensions
									// will be the same of the cell
		img.setAlignment(gm.getAlign());
		if (htmlImage.getPropertyValue("width") != null) {
			final String width = htmlImage.getPropertyValue("width");
			if (NumberUtils.isParsable(width)) {
				img.scaleAbsoluteWidth(Integer.parseInt(width));
			}
		}
		if (htmlImage.getPropertyValue("height") != null) {
			final String height = htmlImage.getPropertyValue("height");
			if (NumberUtils.isParsable(height)) {
				img.scaleAbsoluteHeight(Integer.parseInt(height));
			}
		}
		return img;
	}

}
/**
 * 
 * $Log: PDFImage.java,v $
 * Revision 1.1  2007/12/26 15:57:41  tah
 * *** empty log message ***
 *
 * Revision 1.3  2006/07/05 16:00:47  nts
 * Refatorando para melhorar qualidade do código
 *
 * Revision 1.2  2006/04/11 19:43:46  tah
 * *** empty log message ***
 * Revision 1.1 2006/04/03 21:30:42 tah Utilizando o
 * nheengatu
 * 
 * Revision 1.3 2006/01/01 13:45:32 aryjr Feliz 2006!!!
 * 
 * Revision 1.2 2005/12/16 14:06:31 aryjr Problem with cell heights solved!!!
 * 
 * Revision 1.1 2005/11/14 12:17:29 aryjr Renomeando os pacotes.
 * 
 * Revision 1.2 2005/09/26 19:41:13 aryjr Aproveitando a greve para voltar a
 * atividade.
 * 
 * Revision 1.1 2005/09/10 23:43:40 aryjr Passando para o java.net.
 * 
 * Revision 1.6 2005/07/02 01:18:56 aryjunior Site do projeto.
 * 
 * Revision 1.5 2005/06/04 13:29:25 aryjunior LGPL.
 * 
 * Revision 1.4 2005/05/30 05:28:48 aryjunior Ajustando alguns javadocs.
 * 
 * Revision 1.3 2005/05/30 01:55:56 aryjunior Alguns detalhes no cabecalho dos
 * arquivos e fazendo alguns testes com tabelas ainhadas.
 * 
 * Revision 1.2 2005/05/28 23:21:41 aryjunior Corrigindo o cabecalho.
 * 
 * Revision 1.1.1.1 2005/05/28 21:10:32 aryjunior Initial import.
 * 
 * 
 */
