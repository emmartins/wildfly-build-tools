package org.wildfly.build.server.model;

import org.jboss.staxmapper.XMLMapper;
import org.wildfly.build.util.PropertyResolver;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;

/**
 * @author Stuart Douglas
 */
public class ServerBuildDescriptionModelParser {

    private static final QName ROOT_1_0 = new QName(ServerBuildDescriptionModelParser10.NAMESPACE_1_0, "build");

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    private final XMLMapper mapper;

    public ServerBuildDescriptionModelParser(PropertyResolver properties) {
        mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(ROOT_1_0, new ServerBuildDescriptionModelParser10(properties));
    }

    public ServerBuildDescription parse(final InputStream input) throws XMLStreamException {

        final XMLInputFactory inputFactory = INPUT_FACTORY;
        setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(input);
        ServerBuildDescription build = new ServerBuildDescription();
        mapper.parseDocument(build, streamReader);
        return build;
    }

    private void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

}
