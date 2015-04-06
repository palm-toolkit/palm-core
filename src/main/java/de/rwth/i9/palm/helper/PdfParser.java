package de.rwth.i9.palm.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

public class PdfParser
{
	public static void parsePdf( File pdfFile ) throws IOException, SAXException, TikaException
	{
	BodyContentHandler handler = new BodyContentHandler();
    Metadata metadata = new Metadata();
		FileInputStream inputstream = new FileInputStream( pdfFile );
    ParseContext pcontext = new ParseContext();
    
    //parsing the document using PDF parser
    PDFParser pdfparser = new PDFParser(); 
    pdfparser.parse(inputstream, handler, metadata,pcontext);
    
    //getting the content of the document
    System.out.println("Contents of the PDF :" + handler.toString());
    
    //getting metadata of the document
    System.out.println("Metadata of the PDF:");
    String[] metadataNames = metadata.names();
    
    for(String name : metadataNames) {
       System.out.println(name+ " : " + metadata.get(name));
    }
	}
}
