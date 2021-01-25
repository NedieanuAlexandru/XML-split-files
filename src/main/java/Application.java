import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Application {

    public static void main(String[] args) throws Exception {
        String inputFile = "E:\\SACOM\\order23.xml";

        File xmlFile = new File(inputFile);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(xmlFile);

        // Extragem din numele fisierului primit doar cifrele
        String fileName = xmlFile.getName();
        String fileOrderNumber = fileName.replaceAll("[^0-9]", "");


        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        XPathFactory xFactory = XPathFactory.newInstance();
        XPath xpath = xFactory.newXPath();
        XPathExpression allProductsPath = xpath.compile("//product/supplier/text()");
        NodeList productNodes = (NodeList) allProductsPath.evaluate(doc, XPathConstants.NODESET);

        // Pentru adaugarea id-ului unui order in numele fisierului
        XPathExpression id = xpath.compile("orders/order/@ID");
        NodeList ids = (NodeList) id.evaluate(doc, XPathConstants.NODESET);

        List<String> allIds = new ArrayList<>();
        for (int o = 0; o < ids.getLength(); o++) {
            Node idName = ids.item(o);
            allIds.add(idName.getTextContent());
        }


        // Salvam toate produsele
        List<String> suppliers = new ArrayList<>();
        for (int i = 0; i < productNodes.getLength(); ++i) {
            Node productName = productNodes.item(i);
            suppliers.add(productName.getTextContent());
        }

        //  Aici vom crea fisierele xml separate pe furnizori

        for (String supplier : suppliers) {
            String xpathQuery = "/orders/order/product[supplier='" + supplier + "']";

            xpath = xFactory.newXPath();
            XPathExpression query = xpath.compile(xpathQuery);
            NodeList remainingProductNodes = (NodeList) query.evaluate(doc, XPathConstants.NODESET);

            //  Salvam noul fisier xml in supplierName.xml = Sony.xml
            Document trimedXml = db.newDocument();

            //  Cream root-ul <products>
            Element root = trimedXml.createElement("products");
            trimedXml.appendChild(root);
            //  Iteram printre produse si cream cate un element product pentru fiecare
            for (int i = 0; i < remainingProductNodes.getLength(); ++i) {
                Element product = trimedXml.createElement("product");
                Node productNode = remainingProductNodes.item(i);

                //  Populam fiecare product
                if (productNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) productNode;

                    String description = element.getElementsByTagName("description").item(0).getTextContent();
                    Element descriptionEl = trimedXml.createElement("description");
                    descriptionEl.appendChild(trimedXml.createTextNode(description));
                    product.appendChild(descriptionEl);


                    String gtin = element.getElementsByTagName("gtin").item(0).getTextContent();
                    Element gtinEl = trimedXml.createElement("gtin");
                    gtinEl.appendChild(trimedXml.createTextNode(gtin));
                    product.appendChild(gtinEl);


                    String price = element.getElementsByTagName("price").item(0).getTextContent();
                    Element priceEl = trimedXml.createElement("price");
                    priceEl.appendChild(trimedXml.createTextNode(price));

                    String currency = element.getElementsByTagName("price").item(0).getAttributes().getNamedItem("currency").getTextContent();
                    Attr currencyAttr = trimedXml.createAttribute("currency");
                    currencyAttr.setValue(currency);
                    priceEl.setAttributeNode(currencyAttr);
                    product.appendChild(priceEl);


                    String orderId = element.getParentNode().getAttributes().getNamedItem("ID").getTextContent();
                    Element orderEl = trimedXml.createElement("orderid");
                    orderEl.appendChild(trimedXml.createTextNode(orderId));
                    product.appendChild(orderEl);
                }
                root.appendChild(product);
            }

            //  Salvam fisierele pe disk si le indentam. "Pretty print"
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            //  Daca trebuie folosit id-ul anumitei comenzi in numele fisierului
//          String idNumeOptional = allIds.stream().iterator().next()      // .substring(0,2); - pentru a folosi doar primele 2 cifre

            DOMSource source = new DOMSource(trimedXml);
            StreamResult result = new StreamResult(new File("E:\\SACOM\\" + supplier.trim() + fileOrderNumber + ".xml"));
            transformer.transform(source, result);
        }
    }

}