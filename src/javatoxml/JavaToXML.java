/*
 * Copyright (C) 2014 gyankos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package javatoxml;


import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.ModifierSet;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.TypeDeclaration;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.File;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
 

import org.w3c.dom.Document;
import org.w3c.dom.Element;
 

/**
 *
 * @author gyankos
 */
public class JavaToXML {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        
        File dest;
        
        if (args.length == 1) {
            dest = new File(args[0]);
            String filename = dest.getName();
            if (dest.isFile()) {
                if (filename.indexOf(".") > 0) {
                    filename = filename.substring(0, filename.lastIndexOf("."));
                }
            }
            
            FileInputStream in = null;
            CompilationUnit cu = null;
            try {
                in = new FileInputStream(args[0]);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(JavaToXML.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }

            try {
                // parse the file
                cu = JavaParser.parse(in);
            } catch (ParseException ex) {
                Logger.getLogger(JavaToXML.class.getName()).log(Level.SEVERE, null, ex);
                try {
                    in.close();
                } catch (IOException ex1) {
                    Logger.getLogger(JavaToXML.class.getName()).log(Level.SEVERE, null, ex1);
                    return;
                }
            }
            
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder;
            try {
                docBuilder = docFactory.newDocumentBuilder();
            } catch (ParserConfigurationException ex) {
                Logger.getLogger(JavaToXML.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
 
            // root elements
            Document xml = docBuilder.newDocument();
            Element pack = xml.createElement("package");
            pack.setAttribute("name", cu.getPackage().getName().getName());
            xml.appendChild(pack);
 
            
            for (TypeDeclaration x:cu.getTypes()) {
                Element obj = xml.createElement("object");
                obj.setAttribute("name", x.getName());
                List<BodyDeclaration> members = x.getMembers();
                for (BodyDeclaration member : members) {
                    if (member instanceof MethodDeclaration) {
                        MethodDeclaration pmethod = (MethodDeclaration) member;
                        Element method = xml.createElement("method");
                        method.setAttribute("name", pmethod.getName());
                        method.setAttribute("returnType", pmethod.getType().toString());
                        switch(pmethod.getModifiers()) {
                            case ModifierSet.STATIC:
                                method.setAttribute("modifier", "static");
                                break;
                        }
                        if (pmethod.getParameters()!=null) {
                            for (Parameter y:pmethod.getParameters()) {
                                Element param = xml.createElement("param");
                                param.setAttribute("type", y.getType().toString());
                                param.setAttribute("name", y.getId().getName());
                                method.appendChild(param);
                            }
                        }
                        obj.appendChild(method);
                    }
                }
                pack.appendChild(obj);
            }
           
            //System.out.println(xml.toString());
            // write the content into xml file
            Transformer t;
            StreamResult sr ;
            try {
                t = TransformerFactory.newInstance().newTransformer();
                t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                t.setOutputProperty(OutputKeys.INDENT, "yes");
                t.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "8");
                sr = new StreamResult(new File(filename+".giws.xml"));
                
            } catch (TransformerConfigurationException ex) {
                Logger.getLogger(JavaToXML.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            try {
                t.transform(new DOMSource(xml), sr);
            } catch (TransformerException ex) {
                Logger.getLogger(JavaToXML.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }

            
        } else {
            System.err.println("JavaToXML : this program converts Java sources into giws xml format");
            System.err.println("Usage: java -jar JavaToXML.jar [input.java]");
            System.err.println("Output: returns an input.giws.xml in the current work directory");
        }
        
    }
    
}
