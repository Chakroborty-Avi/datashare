package org.icij.datashare.web;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import liquibase.pro.packaged.E;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Options;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;
import org.apache.commons.compress.archivers.ArchiveException;
import org.icij.datashare.DeliverablePackage;
import org.icij.datashare.DeliverableRegistry;
import org.icij.datashare.PluginService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.Set;

import static java.util.Optional.ofNullable;
import static net.codestory.http.payload.Payload.ok;

@Singleton
@Prefix("/api/plugins")
public class PluginResource {
    private final PluginService pluginService;

    @Inject
    public PluginResource(PluginService pluginService) { this.pluginService = pluginService; }

    /**
     * Gets the plugins set in JSON
     *
     * If a request parameter "filter" is provided, the regular expression will be applied to the list.
     *
     * see https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html
     * for pattern syntax.
     *
     * Example:
     * $(curl localhost:8080/api/plugins?filter=.*paginator)
     */
    @Get()
    public Set<DeliverablePackage> getPluginList(Context context) {
        return pluginService.list(ofNullable(context.request().query().get("filter")).orElse(".*"));
    }

    /**
     * Preflight request
     *
     * @return OPTIONS,PUT
     */
    @Options("/install")
    public Payload installPluginPreflight() {
        return ok().withAllowMethods("OPTIONS", "PUT");
    }

    /**
     * Download (if necessary) and install plugin specified by its id or url
     *
     * request parameter `id` or `url` must be present.
     *
     * @return  200 if the plugin is installed
     * @return  404 if the plugin is not found by the provided id or url
     * @return  400 if neither id nor url is provided
     *
     * @throws IOException
     * @throws ArchiveException
     *
     * Example:
     * $(curl -i -XPUT localhost:8080/api/plugins/install?id=datashare-plugin-site-alert)
     */
    @Put("/install")
    public Payload installPlugin(Context context) throws IOException, ArchiveException {
        String pluginUrlString = context.request().query().get("url");
        try {
            pluginService.downloadAndInstall(new URL(pluginUrlString));
            return Payload.ok();
        } catch (MalformedURLException not_url) {
            String pluginId = context.request().query().get("id");
            if (pluginId == null) {
                return Payload.badRequest();
            }
            try {
                pluginService.downloadAndInstall(pluginId);
                return Payload.ok();
            } catch (DeliverableRegistry.UnknownDeliverableException unknownDeliverableException) {
                return Payload.notFound();
            }
        }
    }

    /**
     * Preflight request
     *
     * @return OPTIONS,DELETE
     */
    @Options("/uninstall")
    public Payload uninstallPluginPreflight() { return ok().withAllowMethods("OPTIONS", "DELETE");}

    /**
     * Uninstall plugin specified by its id
     * Always returns 204 or error 500.
     *
     * @param pluginId
     * @return 204
     *
     * @throws IOException if there is a filesystem error
     *
     * Example:
     * $(curl -i -XDELETE localhost:8080/api/plugins/uninstall?id=datashare-plugin-site-alert)
     */
    @Delete("/uninstall?id=:pluginId")
    public Payload uninstallPlugin(String pluginId) throws IOException {
        try {
            pluginService.delete(pluginId);
        } catch (DeliverableRegistry.UnknownDeliverableException|NoSuchElementException unknownDeliverableException) {
            return new Payload(204);
        }
        return new Payload(204);
    }
}
