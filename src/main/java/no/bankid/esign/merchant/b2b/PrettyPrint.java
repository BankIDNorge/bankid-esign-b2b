package no.bankid.esign.merchant.b2b;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Map;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class PrettyPrint {

    public static String prettyPrintXml(String xml) {
        if (xml == null) {
            return "";
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StreamSource source = new StreamSource(new java.io.StringReader(xml));
            StringWriter stringWriter = new StringWriter();
            StreamResult result = new StreamResult(stringWriter);

            transformer.transform(source, result);
            return stringWriter.toString();
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    final static ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writerWithDefaultPrettyPrinter();

    public static String prettyPrintJson(Map<String, Object> json) {
        if (json == null) {
            return "";
        }
        try {
            return OBJECT_WRITER.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String prettyPrintJWT(String jwtB64) {
        if (jwtB64 == null) {
            return "";
        }
        try {
            JWT parsed = JWTParser.parse(jwtB64);

            return prettyPrintJson(parsed.getHeader().toJSONObject()) + "\n" +
                prettyPrintJson(parsed.getJWTClaimsSet().toJSONObject());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
