package org.kie.guvnor.project.backend.server;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jboss.errai.bus.server.annotations.Service;
import org.kie.commons.io.IOService;
import org.kie.commons.java.nio.base.options.CommentedOption;
import org.kie.guvnor.datamodel.events.InvalidateDMOProjectCacheEvent;
import org.kie.guvnor.project.model.POM;
import org.kie.guvnor.project.service.POMService;
import org.kie.guvnor.services.metadata.MetadataService;
import org.kie.guvnor.services.metadata.model.Metadata;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;

@Service
@ApplicationScoped
public class POMServiceImpl
        implements POMService {


    private Event<InvalidateDMOProjectCacheEvent> invalidateDMOProjectCache;
    private IOService ioService;
    private Paths paths;
    private POMContentHandler pomContentHandler;
    private MetadataService metadataService;

    public POMServiceImpl() {
        // For Weld
    }

    @Inject
    public POMServiceImpl(Event<InvalidateDMOProjectCacheEvent> invalidateDMOProjectCache,
                          @Named("ioStrategy") IOService ioService,
                          MetadataService metadataService,
                          Paths paths,
                          POMContentHandler pomContentHandler) {
        this.invalidateDMOProjectCache = invalidateDMOProjectCache;
        this.ioService = ioService;
        this.metadataService = metadataService;
        this.paths = paths;
        this.pomContentHandler = pomContentHandler;
    }


    @Override
    public POM loadPOM(final Path path) {
        try {
            org.kie.commons.java.nio.file.Path convert = paths.convert(path);
            String propertiesString = ioService.readAllString(convert);
            return pomContentHandler.toModel(propertiesString);
        } catch (IOException e) {
            e.printStackTrace();  //TODO Need to use the Problems screen for these -Rikkola-
        } catch (XmlPullParserException e) {
            e.printStackTrace();  //TODO Need to use the Problems screen for these -Rikkola-
        }
        return null;

    }


    @Override
    public Path savePOM(final String commitMessage,
                        final Path pathToPOM,
                        final POM pomModel,
                        Metadata metadata) {
        try {
            Path result;
            if (metadata == null) {
                result = paths.convert(
                        ioService.write(
                                paths.convert(pathToPOM),
                                pomContentHandler.toString(pomModel),
                                metadataService.getCommentedOption(commitMessage)));
            } else {
                result = paths.convert(
                        ioService.write(
                                paths.convert(pathToPOM),
                                pomContentHandler.toString(pomModel),
                                metadataService.setUpAttributes(pathToPOM, metadata),
                                metadataService.getCommentedOption(commitMessage)));
            }

            invalidateDMOProjectCache.fire(new InvalidateDMOProjectCacheEvent(result));

            return result;

        } catch (IOException e) {
            e.printStackTrace();  //TODO Notify this in the Problems screen -Rikkola-
        }
        return null;
    }

    @Override
    public Path savePOM(final Path pathToPOM, final POM pomModel) {
        try {

            Path result = paths.convert(
                    ioService.write(
                            paths.convert(pathToPOM),
                            pomContentHandler.toString(pomModel)));

            invalidateDMOProjectCache.fire(new InvalidateDMOProjectCacheEvent(result));

            return result;

        } catch (IOException e) {
            e.printStackTrace();  //TODO Notify this in the Problems screen -Rikkola-
        }
        return null;
    }

}
