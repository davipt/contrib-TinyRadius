/**
 * $Id: DictionaryParser.java,v 1.2 2005/09/06 16:38:40 wuttke Exp $
 * Created on 28.08.2005
 * @author mw
 * @version $Revision: 1.2 $
 */
package org.tinyradius.dictionary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import org.tinyradius.attribute.IntegerAttribute;
import org.tinyradius.attribute.IpAttribute;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.StringAttribute;
import org.tinyradius.attribute.VendorSpecificAttribute;

/**
 * Parses a dictionary in "Radiator format" and fills a
 * WritableDictionary.
 */
public class DictionaryParser {

	/**
	 * Returns a new dictionary filled with the contents
	 * from the given input stream.
	 * @param source input stream
	 * @return dictionary object
	 * @throws IOException
	 */
	public static Dictionary parseDictionary(InputStream source) 
	throws IOException {
		WritableDictionary d = new MemoryDictionary();
		parseDictionary(source, d);
		return d;
	}
	
	/**
	 * Parses the dictionary from the specified InputStream.
	 * @param source input stream
	 * @param dictionary dictionary data is written to
	 * @throws IOException syntax errors
	 * @throws RuntimeException syntax errors
	 */
	public static void parseDictionary(InputStream source, WritableDictionary dictionary) 
	throws IOException {
		// read each line separately
		BufferedReader in = new BufferedReader(new InputStreamReader(source));
		
		String line;
		int lineNum = -1;
		while ((line = in.readLine()) != null) {
			// ignore comments
			lineNum++;
			line = line.trim();
			if (line.startsWith("#") || line.length() == 0)
				continue;
			
			// tokenize line by whitespace
			StringTokenizer tok = new StringTokenizer(line);
			if (!tok.hasMoreTokens())
				continue;
			
			String lineType = tok.nextToken().trim();
			try {
    			if (lineType.equalsIgnoreCase("ATTRIBUTE"))
    				parseAttributeLine(dictionary, tok, lineNum);
    			else if (lineType.equalsIgnoreCase("VALUE"))
    				parseValueLine(dictionary, tok, lineNum);
    			else if (lineType.equalsIgnoreCase("$INCLUDE"))
    				includeDictionaryFile(dictionary, tok, lineNum);
    			else if (lineType.equalsIgnoreCase("VENDORATTR"))
    				parseVendorAttributeLine(dictionary, tok, lineNum);
    			else if (lineType.equals("VENDOR"))
    				parseVendorLine(dictionary, tok, lineNum);
    			else
                    //throw new IOException("unknown line type: " + lineType + " line: " + lineNum);
    			    System.err.println("[dictionary] unknown line type: " + lineType + ", line: " + lineNum);
			} catch ( IOException e ) {
                System.err.println("[dictionary] " + e.getMessage()+", line='" + line + "', line: " + lineNum);
			}
		}
	}

	/**
	 * Parse a line that declares an attribute.
	 */
	private static void parseAttributeLine(WritableDictionary dictionary, StringTokenizer tok, int lineNum) 
	throws IOException {
		if (tok.countTokens() < 3) // allow comments
			throw new IOException("syntax error on line " + lineNum);
		
		// read name, code, type
		String name = tok.nextToken().trim();
		int code = Integer.parseInt(tok.nextToken());
		String typeStr = tok.nextToken().trim();

		// translate type to class
		Class type;
		if (code == VendorSpecificAttribute.VENDOR_SPECIFIC)
			type = VendorSpecificAttribute.class;
		else
			type = getAttributeTypeClass(code, typeStr);
		
		// create and cache object
		try {
		    dictionary.addAttributeType(new AttributeType(code, name, type));
		} catch(IllegalArgumentException e ) {
		    System.err.println("[dictionary] ignored " + e.getMessage() + ", name=" + name 
		            + ", code=" + code + ", type=" + typeStr+", line: " + lineNum);
		}
	}

	/**
	 * Parses a VALUE line containing an enumeration value.
	 */
	private static void parseValueLine(WritableDictionary dictionary, StringTokenizer tok, int lineNum) 
	throws IOException {
		if (tok.countTokens() < 3) // allow comments
			throw new IOException("syntax error on line " + lineNum);

		String typeName = tok.nextToken().trim();
		String enumName = tok.nextToken().trim();
		String valStr = tok.nextToken().trim();
		
		AttributeType at = dictionary.getAttributeTypeByName(typeName);
		if (at == null) {
            System.err.println("[dictionary] unknown attribute, name=" + typeName + ", enum=" + enumName + ", val=" 
                + valStr + ", line: " + lineNum);
			//throw new IOException("unknown attribute type: " + typeName + ", line: " + lineNum);
            return;
		}
		//else
		//	at.addEnumerationValue(Integer.parseInt(valStr), enumName);
		// DAVI parse number dec or hex
		int code = valStr.startsWith("0x") ? Integer.parseInt(valStr.substring(2), 16) : Integer.parseInt(valStr); // DAVI
		at.addEnumerationValue(code, enumName);
	}

	/**
	 * Parses a line that declares a Vendor-Specific attribute.
	 */
	private static void parseVendorAttributeLine(WritableDictionary dictionary, StringTokenizer tok, int lineNum) 
	throws IOException {
		if (tok.countTokens() < 4) // allow comments
			throw new IOException("syntax error on line " + lineNum);
		
		String vendor = tok.nextToken().trim();
		String name = tok.nextToken().trim();
		// DAVI parse number dec or hex
		String t1 = tok.nextToken().trim();
		try {
		    int code = t1.startsWith("0x") ? Integer.parseInt(t1.substring(2), 16) : Integer.parseInt(t1); // DAVI
		    String typeStr = tok.nextToken().trim();

		    Class type = getAttributeTypeClass(code, typeStr);
    		AttributeType at = new AttributeType(Integer.parseInt(vendor), code, name, type);
    		dictionary.addAttributeType(at);
        } catch( RuntimeException e ) {
            System.err.println("[dictionary] ignored " + e.getMessage() + ", name=" + name 
                    + ", vendor=" + vendor + ", val=" + t1+", line: " + lineNum);
		}
	}

	/**
	 * Parses a line containing a vendor declaration.
	 */
	private static void parseVendorLine(WritableDictionary dictionary, StringTokenizer tok, int lineNum) 
	throws IOException {
		if (tok.countTokens() < 2) // allow comments
			throw new IOException("syntax error on line " + lineNum);
		
        String t1=tok.nextToken().trim(),t2=tok.nextToken().trim();
        /*
		String t1=tok.nextToken().trim(), t2=tok.nextToken().trim(), t3=null;
		int sizeAttrCode=1, sizeAttrLen=1;
		if(tok.hasMoreTokens())
		    t3=tok.nextToken().trim();
		if(t3!=null) {
		    if(t3.startsWith("format=")) {
		        String[] parts = t3.substring(7).split(",");
		        sizeAttrCode = Integer.parseInt(parts[0]);
		        if(parts.length>1)
		            sizeAttrLen = Integer.parseInt(parts[1]);
		    }
		    //System.out.println("t3='"+t3+"' sizeAttrCode="+sizeAttrCode+" sizeAttrLen="+sizeAttrLen);
		}
		*/
        
		// DAVI parse number dec or hex
		try {
			int vendorId = Integer.parseInt(t1);
			String vendorName = t2;

            //dictionary.addVendor(vendorId, vendorName, sizeAttrCode, sizeAttrLen);
            dictionary.addVendor(vendorId, vendorName);
		} catch( NumberFormatException e ) {
			int vendorId = Integer.parseInt(t2);
			String vendorName = t1;

            //dictionary.addVendor(vendorId, vendorName, sizeAttrCode, sizeAttrLen);
            dictionary.addVendor(vendorId, vendorName);
		}
	}
	
	/**
	 * Includes a dictionary file.
	 */
	private static void includeDictionaryFile(WritableDictionary dictionary, StringTokenizer tok, int lineNum) 
	throws IOException {
		if (tok.countTokens() != 1)
			throw new IOException("syntax error on line " + lineNum);
		String includeFile = tok.nextToken();
		
		File incf = new File(includeFile);
		if (!incf.exists())
			throw new IOException("inclueded file '" + includeFile + "' not found, line " + lineNum);
				
		FileInputStream fis = new FileInputStream(incf);
		parseDictionary(fis, dictionary);
		
		// line numbers begin with 0 again, but file name is
		// not mentioned in exceptions
		// furthermore, this method does not allow to include
		// classpath resources
	}
	
    /**
     * Returns the RadiusAttribute descendant class for the given
     * attribute type.
     * @param typeStr string|octets|integer|date|ipaddr
     * @return RadiusAttribute class or descendant
     */
    private static Class getAttributeTypeClass(int attributeType, String typeStr) {
		Class type = RadiusAttribute.class;
		if (typeStr.equalsIgnoreCase("string"))
			type = StringAttribute.class;
		else if (typeStr.equalsIgnoreCase("octets"))
			type = RadiusAttribute.class;
		// DAVI allow integer8 and integer16
		//else if (typeStr.equalsIgnoreCase("integer") || typeStr.equalsIgnoreCase("date"))
		else if (typeStr.equalsIgnoreCase("integer") || typeStr.equalsIgnoreCase("integer8") || typeStr.equalsIgnoreCase("integer16") || typeStr.equalsIgnoreCase("date"))
			type = IntegerAttribute.class;
		else if (typeStr.equalsIgnoreCase("ipaddr"))
			type = IpAttribute.class;
		return type;
    }    
	
}

