package de.rwth.i9.palm.pdfextraction.service;

import java.util.ArrayList;
import java.util.List;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextRenderInfo;
import com.itextpdf.text.pdf.parser.Vector;

import de.rwth.i9.palm.utils.NumberUtils;

/**
 * This class is customized iTextPdf TextExtractionStrategy, designed for
 * detecting the structure of academic publication
 * 
 * @author sigit
 *
 */
public class PalmPdfExtractionStrategy implements TextExtractionStrategy
{
	// check whether pdf can be read or not
	private boolean isPdfReadable;

	// store the last font type from previous loop
	private String lastFontType;

	// store the last font height from previous loop
	private float lastFontHeight;

	// store last bounding box bottom right coordinate
	private int lastPageNumber;

	// store last bounding box bottom right coordinate
	private Vector lastCoordinateBottomRight;

	// the textResultant test
	private StringBuilder textResult;

	// store characters from previous loop
	private String lastTextCharacter;

	// document page number
	private int pageNumber;

	// store the pageSize
	private Rectangle pageSize;

	// page margin, determine whether the margin of a page
	private float pageMargin;

	// Container for academic pdf structure
	List<TextSection> textSections;
	TextSection textSection;

	private StringBuilder lastLine;

	private StringBuilder lastContentSection;
	/**
	 * Enumeration for font type, such as normal, bold, etc
	 */
	private enum TextRenderMode
	{
		FILLTEXT(0), 
		STROKETEXT(1), 
		FILLTHENSTROKETEXT(2), 
		INVISIBLE(3), 
		FILLTEXTANDADDTOPATHFORCLIPPING(4), 
		STROKETEXTANDADDTOPATHFORCLIPPING(5), 
		FILLTHENSTROKETEXTANDADDTOPATHFORCLIPPING(6), 
		ADDTEXTTOPADDFORCLIPPING(7);

		private int value;

		private TextRenderMode( int value )
		{
			this.value = value;
		}

		public int getValue()
		{
			return value;
		}
	}

	/**
	 * Custom constructor
	 */
	public PalmPdfExtractionStrategy()
	{
		this.textResult = new StringBuilder();
		this.lastLine = new StringBuilder();
		this.lastContentSection = new StringBuilder();
		this.lastCoordinateBottomRight = new Vector( 0f, 0f, 1f );
		this.isPdfReadable = true;
		this.textSections = new ArrayList<TextSection>();
		this.lastPageNumber = 0;
		this.lastTextCharacter = "";
	}

	@Override
	public void beginTextBlock()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void endTextBlock()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void renderImage( ImageRenderInfo arg0 )
	{
		// TODO Auto-generated method stub

	}

	/**
	 * Customize how the pdf have to be extracted
	 */
	@Override
	public void renderText( TextRenderInfo renderInfo )
	{
		// flag whether letter position inside margin
		boolean isInsidePageMargin = false;
		
		// get the font type
		String currentFontType = renderInfo.getFont().getPostscriptFontName();

		// starting new page, prepare new textSection
		if ( this.lastPageNumber != this.pageNumber )
			if ( textSection == null )
				textSection = new TextSection();

		// check whether the font type is recognized by the system
		if ( currentFontType.equals( "Unspecified Font Name" ) )
			isPdfReadable = false;

		// proceed only if pdf readable
		// reject blank input, since space is generated from text distance
		if ( isPdfReadable && !renderInfo.getText().equals( " " ) )
		{
			// IText read pdf file based on letter/chunk coordinate in rectangle
			Vector currentCoordinateBottomLeft = renderInfo.getBaseline().getStartPoint();
			Vector currentCoordinateTopRight = renderInfo.getAscentLine().getEndPoint();
			// get bottom right vector coordinate
			Vector currentCoordinateBottomRight = new Vector( currentCoordinateTopRight.get( 0 ), currentCoordinateBottomLeft.get( 1 ), 1.0f );
		
			// check whether the text is not in margin
			// (margin > x > document.width - margin)
			// ( margin > y > document.height - margin)
			if( currentCoordinateBottomLeft.get( 0 ) > this.pageMargin && currentCoordinateBottomRight.get( 0 ) < pageSize.getWidth() - pageMargin &&
				currentCoordinateBottomLeft.get( 1 ) > this.pageMargin && currentCoordinateBottomRight.get( 1 ) < pageSize.getHeight() - pageMargin ){
				isInsidePageMargin = true;
			}

			// only process text, if it's not lies in the page margin
			if( isInsidePageMargin ){
				// the current character
				StringBuilder currentCharacters = new StringBuilder();

				// flag whether a line is complete
				boolean isLastLineComplete = false;
				// flag section complete
				boolean isSectionComplete = false;
				// flag whether a word is complete
				boolean isWordSplitted = false;
				// Check if the bold font type is in used
				if ( ( renderInfo.getTextRenderMode() == (int) TextRenderMode.FILLTHENSTROKETEXT.getValue() ) )
					currentFontType += "+Bold";
				
				// get font height
				float currentFontHeight = NumberUtils.round( currentCoordinateTopRight.get( 1 ) - currentCoordinateBottomLeft.get( 1 ), 2 );
				// get font space between previous and current
				float curSpaceWidth = 0f;
				// if character previous and current on the same line
				if ( currentCoordinateBottomRight.get( 1 ) == this.lastCoordinateBottomRight.get( 1 ) )
				{
					curSpaceWidth = currentCoordinateBottomLeft.get( 0 ) - lastCoordinateBottomRight.get( 0 );
					// if detect spacing
					if ( curSpaceWidth > 1f && curSpaceWidth < 3 * currentFontHeight )
						currentCharacters.append( ' ' );
					// detect huge spacing on same line
					else if ( curSpaceWidth > currentFontHeight )
						isSectionComplete = true;

				}
				// in different line
				else{
					// y coordinate difference value
					float yPosDifference = lastCoordinateBottomRight.get( 1 ) - currentCoordinateBottomRight.get( 1 );
					
					// -positive value, indicating
					// -- new line or new section
					if( yPosDifference > 0){
						// check for new line within paragraph
						if( yPosDifference < 2 * currentFontHeight )
						{
							if ( currentFontHeight != this.lastFontHeight )
							{
								isSectionComplete = true;
							} else {
								isLastLineComplete = true;
								// check whether a word is not complete
								if ( this.lastLine.charAt( this.lastLine.length() - 1 ) == '-' )
									isWordSplitted = true;
								else
								{
									currentCharacters.append( ' ' );
								}
							}
						}
						else if ( yPosDifference > 2 * currentFontHeight )
							isSectionComplete = true;
					}
					// - negative value, indicating
					// -- subscript/superscript
					// -- new column
					else{
						// check for column change
						if ( Math.abs( yPosDifference ) > currentFontHeight )
							isSectionComplete = true;
					}
					
				}

				// if word is splitted, remove split sign "-"
				if ( isWordSplitted )
					this.lastLine.setLength( this.lastLine.length() - 1 );

				// if a line is complete or a section is complete
				if ( isLastLineComplete || isSectionComplete )
				{
					this.textResult.append( this.lastLine.toString() );
					this.lastContentSection.append( this.lastLine.toString() );
					if ( this.lastLine.length() > 0 )
					{
						this.textSection.addContentLine( this.lastLine.toString() );

						// update right most boundary ( find highest x)
						if ( this.textSection.getBottomRightBoundary().get( 0 ) < this.lastCoordinateBottomRight.get( 0 ) )
							this.textSection.setBottomRightBoundary( this.lastCoordinateBottomRight );

						// update left most boundary( find lowest x)
						if ( this.textSection.getTopLeftBoundary().get( 0 ) > currentCoordinateBottomLeft.get( 0 ) )
						{
							float topBoundary = this.textSection.getTopLeftBoundary().get( 1 );
							this.textSection.setTopLeftBoundary( new Vector( currentCoordinateBottomLeft.get( 0 ), topBoundary, 1f ) );
						}

					}
					// reset
					this.lastLine.setLength( 0 );

				}

				if ( isSectionComplete )
				{
					if ( this.lastContentSection.length() > 0 )
					{
						// assign content and other properties
						this.textSection.setContent( this.lastContentSection.toString() );
						this.textSection.setIndentEnd( this.lastCoordinateBottomRight.get( 0 ) );

						// update bottom right boundary ( find lowest y)
						if ( this.textSection.getBottomRightBoundary().get( 1 ) > this.lastCoordinateBottomRight.get( 1 ) )
						{
							float rightBoundary = this.textSection.getBottomRightBoundary().get( 0 );
							this.textSection.setBottomRightBoundary( new Vector( rightBoundary, this.lastCoordinateBottomRight.get( 1 ), 1f ) );
						}

						// reset
						this.lastContentSection.setLength( 0 );

						this.textSections.add( textSection );
					}
					// create new one and assign some properties
					this.textSection = new TextSection();
					this.textSection.setPageNumber( pageNumber );
					this.textSection.setTopLeftBoundary( new Vector( currentCoordinateBottomLeft.get( 0 ), currentCoordinateTopRight.get( 1 ), 1.0f ) );
					this.textSection.setFontHeight( currentFontHeight );
					this.textSection.setFontType( currentFontType );
					this.textSection.setIndentStart( currentCoordinateBottomLeft.get( 0 ) );
				}
				// Append the current text
				currentCharacters.append( renderInfo.getText() );
				// this.textResult.append( currentCharacters );
				this.lastLine.append( currentCharacters );
	
				// Set currently used properties
				this.lastFontHeight = currentFontHeight;
				this.lastFontType = currentFontType;
				this.lastTextCharacter = currentCharacters.toString();
				this.lastPageNumber = pageNumber;
	
				this.lastCoordinateBottomRight = currentCoordinateBottomRight;
			}
		}
	}

	@Override
	public String getResultantText()
	{
		return textResult.toString();
	}


	public void setPageNumber( int pageNumber )
	{
		this.pageNumber = pageNumber;
		// reset some properties
		this.lastTextCharacter = null;
		this.textResult.setLength( 0 );
	}

	public void setPageSize( Rectangle pageSize )
	{
		this.pageSize = pageSize;
	}

	public float getPageMargin()
	{
		return pageMargin;
	}

	public void setPageMargin( float pageMargin )
	{
		this.pageMargin = pageMargin;
	}

	public List<TextSection> getTextSection()
	{
		return this.textSections;
	}

}
